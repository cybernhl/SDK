package chat.im.trilivechat.chatedin.core

import chat.im.trilivechat.chatedin.state.SessionState
import idv.neo.entity.cs.trilivechat.response.BindTokenResponse
import kotlinx.coroutines.flow.StateFlow

/**
 * TriliveChat 客服會話資訊提供者介面 (Session Provider Interface)。
 *
 * 負責提供當前會話狀態、獲取或自動建立 [sessionId]，以及處理 Token 綁定。
 */
interface SessionProvider {
    /** 觀察當前會話連線狀態與身分資訊 */
    val sessionState: StateFlow<SessionState>

    /** 獲取目前記憶體中已活化的 Session ID，若無則傳回 null */
    fun getActiveSessionId(): String?

    /** 獲取當前有效的 Session ID；若尚未建立，將自動向後端 API 發起建立流程 */
    suspend fun requireSessionId(forceNew: Boolean = false): String

    /** 進行玩家 Token 綁定與身份升級 */
    suspend fun bindToken(token: String): BindTokenResponse

    /** 更新 SDK LiveChatConfig 組態 */
    fun updateConfig(config: LiveChatConfig)

    /** 讀取當前 LiveChatConfig */
    fun getConfig(): LiveChatConfig?

    /** 清除當前會話 */
    fun clearSession()
}
