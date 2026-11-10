package chat.im.trilivechat.chatedin

import androidx.compose.runtime.Composable
import chat.im.trilivechat.chatedin.ui.LiveChatScreen
import chat.im.trilivechat.chatedin.viewmodel.TriliveChatViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * TriliveChat 客服 SDK 進入點 Composable。
 *
 * 【資安保護說明】：
 * 本 SDK 介面不設置 `siteId` 與 `agentId` 的硬編碼預設值。
 * 宿主應用程式 (Host App) 調用本組件時，必須顯式傳入自應用層安全傳遞之 `siteId` 與 `agentId`。
 *
 * @param siteId 站台商戶識別碼（必填）。
 * @param agentId 客服專員/佇列識別碼（必填）。
 * @param initialToken 玩家驗證 JWT Token（選填，登入狀態傳入）。
 * @param onCloseClick 點擊關閉按鈕回調。
 * @param onPickImageClick 點擊圖片選擇/傳送按鈕回調。
 */
@Composable
fun App(
    siteId: String="",
    agentId: String="",
    initialToken: String? = null,
    onCloseClick: (() -> Unit)? = null,
    onPickImageClick: (() -> Unit)? = null
) {
    val viewModel: TriliveChatViewModel = koinViewModel()
    LiveChatScreen(
        viewModel = viewModel,
        siteId = siteId,
        agentId = agentId,
        initialToken = initialToken,
        onCloseClick = onCloseClick,
        onPickImageClick = onPickImageClick
    )
}
