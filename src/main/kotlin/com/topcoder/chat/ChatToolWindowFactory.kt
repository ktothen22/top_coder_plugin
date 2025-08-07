package com.topcoder.chat

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.swing.*
import javax.swing.SwingUtilities.invokeLater

/**
 * PyCharm 툴 윈도우에 챗봇 UI를 제공하는 팩토리 클래스입니다.
 * ToolWindowFactory 를 구현하여 IDE가 툴 윈도우를 생성할 때 호출됩니다.
 */
class ChatToolWindowFactory : ToolWindowFactory {

    /**
     * 툴 윈도우가 생성될 때 호출됩니다.
     * 여기서 UI 구성 요소를 만들고 연결합니다.
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // BorderLayout 을 사용해 영역을 세 부분(상단/중앙/하단)으로 나눕니다.
        val mainPanel = JPanel(BorderLayout())

        // ---- 모델 선택 콤보 박스 ----
        // 서버에서 지원하는 모델 이름들을 배열로 전달합니다.
        val modelSelector = JComboBox(arrayOf("gpt-neo", "finetuned-model"))

        // ---- 대화 내용 표시 영역 ----
        val chatArea = JTextArea()
        chatArea.isEditable = false // 사용자가 직접 수정할 수 없도록 합니다.
        val chatScroll = JScrollPane(chatArea)

        // ---- 사용자 입력 필드와 전송 버튼 ----
        val inputField = JTextField()
        val sendButton = JButton("Send")

        // 입력 필드와 버튼을 같은 줄에 배치하기 위한 패널
        val inputPanel = JPanel(BorderLayout())
        inputPanel.add(inputField, BorderLayout.CENTER)
        inputPanel.add(sendButton, BorderLayout.EAST)

        // 메인 패널에 각 요소 부착
        mainPanel.add(modelSelector, BorderLayout.NORTH)
        mainPanel.add(chatScroll, BorderLayout.CENTER)
        mainPanel.add(inputPanel, BorderLayout.SOUTH)

        // 생성한 패널을 툴 윈도우에 추가
        val content = ContentFactory.getInstance().createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)

        // ---- HTTP 클라이언트 준비 ----
        // Java 11 이상에서 제공되는 표준 HttpClient 를 사용합니다.
        val client = HttpClient.newBuilder().build()

        // ---- 버튼 클릭 시 동작 정의 ----
        sendButton.addActionListener {
            val userText = inputField.text.trim()
            if (userText.isEmpty()) return@addActionListener // 빈 문자열이면 무시

            // 사용자가 입력한 내용을 대화창에 출력
            chatArea.append("You: $userText\n")
            inputField.text = "" // 입력 필드 비우기

            // 선택된 모델 이름을 가져옵니다.
            val modelName = modelSelector.selectedItem as String

            // 서버에 전달할 JSON 본문을 구성합니다.
            // 실제 서버 스펙에 맞게 수정해야 합니다.
            val jsonBody = """
                {
                    "model": "$modelName",
                    "prompt": "$userText"
                }
            """.trimIndent()

            // HTTP POST 요청 생성
            val request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8000/generate")) // vLLM 서버 주소
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build()

            // 비동기 요청 전송
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply { response ->
                    // 응답 본문에서 메시지를 추출합니다.
                    // 여기서는 {"response": "..."} 형식의 JSON 을 가정합니다.
                    val body = response.body()
                    // org.json 라이브러리를 사용하여 간단히 파싱
                    val text = org.json.JSONObject(body).getString("response")
                    text
                }
                .thenAccept { aiText ->
                    // UI 업데이트는 EDT(Event Dispatch Thread) 에서 수행되어야 합니다.
                    invokeLater {
                        chatArea.append("AI: $aiText\n")
                    }
                }
                .exceptionally { ex ->
                    // 오류가 발생하면 메시지를 출력합니다.
                    invokeLater {
                        chatArea.append("Error: ${ex.message}\n")
                    }
                    null
                }
        }
    }
}
