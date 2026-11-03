package chat.im.trilivechat.chatedin.core

/**
 * TriliveChat 客服 SDK 組態設定。
 *
 * 【重要安全提醒】：
 * 本 SDK 不得在 SDK 程式碼內硬編碼特定商戶之 [siteId] 或 [agentId]，以避免資訊洩漏或商業授權濫用。
 * 宿主應用程式 (Host App) 必須在初始化或調用本 SDK 時，自應用層提供授權之參數。
 *
 * @property siteId 站台商戶識別碼（由 PlayMe8 客服系統後端分配，必填）。
 * @property agentId 客服專員/分派佇列識別碼（由 PlayMe8 客服系統後端分配，必填）。
 * @property token 玩家身分驗證 JWT Token（選填）。
 *                 - 若使用者於開啟客服前已登入 App，請傳入此 Token，系統將直接以已登入玩家身分建立對話；
 *                 - 若使用者未登入（或為訪客），可傳入 null，系統將以匿名訪客身分建立對話；
 *                 - 匿名使用者於對話中途在 App 內完成登入後，可透過 [SessionManager.bindToken] 動態升級為登入狀態。
 */
data class LiveChatConfig(
    val siteId: String,
    val agentId: String,
    val token: String? = null
)
