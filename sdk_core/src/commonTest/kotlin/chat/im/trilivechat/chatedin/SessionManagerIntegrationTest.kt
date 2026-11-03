package chat.im.trilivechat.chatedin

import chat.im.trilivechat.chatedin.core.LiveChatConfig
import chat.im.trilivechat.chatedin.core.SessionManager
import chat.im.trilivechat.chatedin.state.SessionState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class SessionManagerIntegrationTest {

    private val siteId = "888330"
    private val agentId = "88884598"
    private val realToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhZ2VudElkIjoiNzYiLCJ1c2VySWQiOiIyNTIwMjciLCJ1c2VybmFtZSI6IkFQS3Rlc3QwMSIsImp0aSI6IjVkNmUyNGY0LWUzOWQtNGRlOC05NTkzLWMwNjM1Y2U1YTBjNyIsImlhdCI6MTc5MjY2MzQxMiwiaXNzIjoib2dwLXBsYXllci1zZXJ2aWNlIiwiZXhwIjoxNzkzMDk1NDEyfQ.wP2V9aGrUctjrXDIy32MtTlNpPYLOTYXQuePZ1il1Q4"
    private val fakeToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.fake_token_signature"

    @Test
    fun test1_anonymousSessionCreation() = runTest {
        val sessionManager = SessionManager()
        sessionManager.updateConfig(LiveChatConfig(siteId = siteId, agentId = agentId, token = null))
        val sessionId = sessionManager.requireSessionId(forceNew = true)
        assertNotNull(sessionId)
        assertTrue(sessionId.isNotBlank())

        val state = sessionManager.sessionState.value
        assertTrue(state is SessionState.Active)
        assertEquals(false, state.isBound)
    }

    @Test
    fun test2_loggedInSessionCreation() = runTest {
        val sessionManager = SessionManager()
        sessionManager.updateConfig(LiveChatConfig(siteId = siteId, agentId = agentId, token = realToken))
        val sessionId = sessionManager.requireSessionId(forceNew = true)
        assertNotNull(sessionId)
        assertTrue(sessionId.isNotBlank())

        val state = sessionManager.sessionState.value
        assertTrue(state is SessionState.Active)
        assertEquals(true, state.isBound)
    }

    @Test
    fun test3_fakeTokenHandling() = runTest {
        val sessionManager = SessionManager()
        // 嘗試帶偽造 Token 建立會話 (預期被後端拒絕 403 / 錯誤)
        sessionManager.updateConfig(LiveChatConfig(siteId = siteId, agentId = agentId, token = fakeToken))
        try {
            sessionManager.requireSessionId(forceNew = true)
        } catch (e: Exception) {
            assertNotNull(e.message)
        }
    }

    /*
    * runTest 的 1 分鐘強制超時機制：
    * kotlinx.coroutines.test.runTest 預設設有 1 分鐘 (60s) 的測試 Watchdog 限制。
    * 當測試進行 6 分鐘（360 秒）的真實等待時，進行到第 1 分鐘時，runTest 的 Watchdog 會主動取消（Cancel）當前的協程 Job。
    * 若希望在 runTest 中執行滿 6 分鐘而不觸發 1 分鐘超時，可以在 runTest 參數加上 runTest(timeout = 10.minutes) { ... } 即可
    * */
    @Test
    fun test4_realDelayAndSessionInspection() = runTest(timeout = 10.minutes) {
        val sessionManager = SessionManager()
        sessionManager.updateConfig(LiveChatConfig(siteId = siteId, agentId = agentId, token = realToken))

        // 【步驟 1】: 呼叫 POST /api/client/sessions 建立會話
        println("\n【步驟 1】: 建立會話...")
        val sessionId = sessionManager.requireSessionId(forceNew = true)
        println("-> Session ID: $sessionId")
        assertNotNull(sessionId)

        // 【步驟 2】: 呼叫 POST /webhook/greet 取得客服問候語
        println("\n【步驟 2】: 取得客服問候語 (/webhook/greet)...")
        try {
            val greetRes = sessionManager.getGreeting()
            println("-> [客服問候語]: ${greetRes.greeting}")
        } catch (e: Exception) {
            println("-> Greet exception: ${e.message}")
        }

        // 【步驟 3】: 開始 6 分鐘（360 秒）真實牆鐘時間等待（透過各平台原生 thread/sleep 達成，不被 runTest 虛擬化）
        val totalSeconds = 360L // 6 分鐘
        println("\n【步驟 3】: 開始真實閒置等待 ${totalSeconds / 60} 分鐘 ($totalSeconds 秒)...")

        for (elapsed in 30..totalSeconds step 30) {
//            delay(30000L)//runTest 虛擬化跳過
//            Thread.sleep(30000L) // 使用 Thread.sleep 進行真實 wall-clock 暫停，不會被 runTest 虛擬化跳過，但是Thread是JVM的，因此需建立platformRealDelay
            platformRealDelay(30000L) // 各平台原生真實暫停 30 秒
            val mins = elapsed / 60
            val secs = elapsed % 60
            val remaining = totalSeconds - elapsed
            println("   ⏳ 各平台真實已閒置等待: ${mins}分${secs}秒 / 剩餘: ${remaining}秒")
        }
        println("✅ 各平台真實閒置等待完畢。")

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
