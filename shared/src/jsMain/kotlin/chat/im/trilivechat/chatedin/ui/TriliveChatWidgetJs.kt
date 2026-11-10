package chat.im.trilivechat.chatedin.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import chat.im.trilivechat.chatedin.App
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

/**
 * 供 Web 前端（Vue, React, Angular, 傳統 HTML）使用的 DOM 宿主掛載器 (Web Widget Bridge)。
 * 支援外部動態傳入 siteId, agentId, token，並可將 Compose 渲染的客服 UI 掛載到指定的 DOM 容器中。
 *
 * <h3>JavaScript / HTML 呼叫範例：</h3>
 * ```html
 * <!-- 1. 在 HTML 中放置掛載點容器 -->
 * <div id="trilive-chat-container" style="width: 420px; height: 650px; position: fixed; bottom: 24px; right: 24px; z-index: 999999;"></div>
 *
 * <!-- 2. 引入編譯後的 webApp.js -->
 * <script src="webApp.js"></script>
 * <script>
 *     // 3. 外部動態傳入 siteId, agentId, token 進行掛載初始化
 *     window.TriliveChatWidgetJs.mount({
 *         containerId: "trilive-chat-container",
 *         siteId: "78505330",
 *         agentId: "18524598",
 *         token: "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." // 選填：若已登入傳入 player token，未登入傳入 null
 *     });
 * </script>
 * ```
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalJsExport::class)
@JsExport
object TriliveChatWidgetJs {

    /**
     * 將 TriliveChat 客服 UI 掛載到指定的 DOM 元素中。
     *
     * @param options 配置物件，包含 containerId, siteId, agentId, token
     */
    fun mount(options: TriliveChatWidgetOptions) {
        val container = document.getElementById(options.containerId) as? HTMLElement
            ?: throw IllegalArgumentException("TriliveChatWidget Error: Container element with id '${options.containerId}' not found.")

        // 清空容器舊內容
        container.innerHTML = ""

        // 使用 ComposeViewport 掛載 App 到該 DOM 元素內
        ComposeViewport(container) {
            App(
                siteId = options.siteId,
                agentId = options.agentId,
                initialToken = options.token
            )
        }
    }
}

/**
 * 掛載選項 Data Class (供 JS 端傳遞配置參數)。
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
data class TriliveChatWidgetOptions(
    val containerId: String,
    val siteId: String,
    val agentId: String,
    val token: String? = null
)
