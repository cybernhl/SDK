package chat.im.trilivechat.chatedin.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.js.Promise

/**
 * 供純 JS / TS / Web 前端 (Vue, React, Angular, 傳統 HTML) 調用的 SessionManager 橋接類別。
 *
 * <h3>JavaScript 呼叫範例：</h3>
 * ```javascript
 * import { SessionManagerJs } from 'TriliveChatSDK-common'; // 或對應打包模組名
 *
 * const sessionMgr = new SessionManagerJs("https://trilivechat.chat/");
 *
 * // 1. 取得 H5 聊天頁網址 (自動處理閒置過期與自動重建 Session)
 * sessionMgr.getChatSessionURL("78505330", "18524598", null, null).then(url => {
 *     console.log("Chat Session URL:", url);
 *     window.open(url, "_blank");
 * }).catch(err => {
 *     console.error("Failed to get chat session URL:", err);
 * });
 *
 * // 2. 中途綁定玩家 Token (升級為已登入狀態)
 * sessionMgr.bindToken("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...").then(response => {
 *     if (response.success) {
 *         console.log("Token bound successfully for user:", response.username);
 *     }
 * });
 *
 * // 3. 清除會話
 * sessionMgr.clearSession();
 * ```
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
class SessionManagerJs(
    baseUrl: String = "https://trilivechat.chat/"
) {
    private val sessionManager = SessionManager(baseUrl = baseUrl)

    /**
     * 取得供瀏覽器開啟的客服對話完整網址。
     * 自動檢查現有 Session 是否過期（支援 404 / session_expired 檢測並自動重建）。
     *
     * @param siteId 商戶站台 ID（必填）
     * @param agentId 客服專員 ID（必填）
     * @param token 玩家驗證 Token（選填）
     * @param existingSessionId 現有的 Session ID（選填）
     * @return Promise<String> 完整的聊天頁面網址 (如 https://trilivechat.chat/chat.html?id=...)
     */
    fun getChatSessionURL(
        siteId: String,
        agentId: String,
        token: String? = null,
        existingSessionId: String? = null
    ): Promise<String> {
        return GlobalScope.promise {
            sessionManager.getChatSessionURL(
                siteId = siteId,
                agentId = agentId,
                token = token,
                existingSessionId = existingSessionId
            )
        }
    }

    /**
     * 進行玩家 Token 綁定與身分升級。
     *
     * @param token 玩家驗證 JWT Token
     * @return Promise<Any> 綁定結果回應物件 (包含 success 及 username)
     */
    fun bindToken(token: String): Promise<Any> {
        return GlobalScope.promise {
            sessionManager.bindToken(token)
        }
    }

    /**
     * 清除當前記憶體中的會話狀態。
     */
    fun clearSession() {
        sessionManager.clearSession()
    }
}
