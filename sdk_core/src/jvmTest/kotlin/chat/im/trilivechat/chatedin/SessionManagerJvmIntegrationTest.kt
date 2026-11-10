package chat.im.trilivechat.chatedin

import chat.im.trilivechat.chatedin.core.LiveChatConfig
import chat.im.trilivechat.chatedin.core.SessionManager
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SessionManagerJvmIntegrationTest {

    private val siteId = "78505330"
    private val agentId = "18524598"
    private val realToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhZ2VudElkIjoiNzYiLCJ1c2VySWQiOiIyNTIwMjciLCJ1c2VybmFtZSI6IkFQS3Rlc3QwMSIsImp0aSI6IjVkNmUyNGY0LWUzOWQtNGRlOC05NTkzLWMwNjM1Y2U1YTBjNyIsImlhdCI6MTc5MjY2MzQxMiwiaXNzIjoib2dwLXBsYXllci1zZXJ2aWNlIiwiZXhwIjoxNzkzMDk1NDEyfQ.wP2V9aGrUctjrXDIy32MtTlNpPYLOTYXQuePZ1il1Q4"

    @Test
    fun test_realDelayAndSessionTimeoutInspection(): Unit = runBlocking {
        val sessionManager = SessionManager()
        sessionManager.updateConfig(LiveChatConfig(siteId = siteId, agentId = agentId, token = realToken))

        // 【步驟 1】: 建立會話
        println("\n【步驟 1】: 建立會話...")
        val sessionId = sessionManager.requireSessionId(forceNew = true)
        println("-> Session ID: $sessionId")
        assertNotNull(sessionId)

        // 【步驟 2】: 取得客服問候語
        println("\n【步驟 2】: 取得客服問候語 (/webhook/greet)...")
        try {
            val greetRes = sessionManager.getGreeting()
            println("-> [客服問候語]: ${greetRes.greeting}")
        } catch (e: Exception) {
            println("-> Greet exception: ${e.message}")
        }

        // 【步驟 3】: 開始 6 分鐘（360 秒）真實牆鐘時間等待（Thread.sleep），模仿 5 分鐘閒置自動過期，對照 poc_session_timeout.py
        val totalSeconds = 360L // 6 分鐘
        println("\n【步驟 3】: 開始真實閒置等待 ${totalSeconds / 60} 分鐘 ($totalSeconds 秒)...")

        for (elapsed in 30..totalSeconds step 30) {
            Thread.sleep(30000L) // 真實執行緒暫停 30 秒，不被虛擬化
            val mins = elapsed / 60
            val secs = elapsed % 60
            val remaining = totalSeconds - elapsed
            println("   ⏳ 真實已閒置等待: ${mins}分${secs}秒 / 剩餘: ${remaining}秒")
        }
        println("✅ 真實閒置等待完畢。")

        // 【步驟 4】: 嘗試使用閒置過期後的舊 sessionId 獲取會話詳情 (/api/client/sessions/{id})
        println("\n【步驟 4】: 嘗試使用過期的舊 sessionId 獲取會話詳情 (/api/client/sessions/{id})...")
        try {
            val detailRes = sessionManager.fetchSessionDetail(sessionId)
            println("-> Session Detail Response after idle: success=${detailRes.success}, status=${detailRes.status}, detail=${detailRes.detail}")
        } catch (e: Exception) {
            println("-> Expected exception or error response on expired session detail: ${e.message}")
        }

        println("\n【步驟 4.1】: 使用過期的舊 sessionId 嘗試發送訊息 (/webhook/chat)...")
        try {
            val chatRes = sessionManager.sendChat("請問還有人在嗎？")
            println("-> Chat Response after idle: $chatRes")
        } catch (e: Exception) {
            println("-> Expected exception or error response on expired session chat: ${e.message}")
        }

        // 【步驟 5】: 驗證 getChatSessionURL 的自動恢復與重建機制 (Recovery)
        println("\n【步驟 5】: 呼叫 getChatSessionURL 自動恢復並重建新 Session...")
        val recoveryUrl = sessionManager.getChatSessionURL(
            siteId = siteId,
            agentId = agentId,
            token = realToken,
            existingSessionId = sessionId
        )
        println("-> Recovery URL generated: $recoveryUrl")
        assertNotNull(recoveryUrl)
        assertTrue(recoveryUrl.contains("chat.html?id="))
    }
}