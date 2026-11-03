@file:Suppress("NON_EXPORTABLE_TYPE")

package chat.im.trilivechat.chatedin.js

import chat.im.trilivechat.chatedin.state.TriliveChatUiState
import chat.im.trilivechat.chatedin.viewmodel.TriliveChatViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 給 Web 前端 (Vue, React, Angular, 純 JS/TS) 調用的 SDK JavaScript 橋接介面。
 * 封裝 ViewModel 並提供 JS Callback 訂閱機制以相容各大前端框架。
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
class TriliveChatSdkJs(
    private val viewModel: TriliveChatViewModel
) {
    /** 讀取當前最新的 UI 狀態快照 */
    val state: TriliveChatUiState
        get() = viewModel.uiState.value

    /**
     * 狀態變更訂閱器（供 Vue / React / Angular / 純 JS 監聽狀態）。
     *
     * @param callback 每次 UI 狀態更新時調用
     * @return 返回一個取消訂閱 (unsubscribe) 的函數
     */
    fun subscribe(callback: (TriliveChatUiState) -> Unit): () -> Unit {
        val job = CoroutineScope(Dispatchers.Main).launch {
            viewModel.uiState.collect { newState ->
                callback(newState)
            }
        }
        return { job.cancel() }
    }

    /** 初始化並啟動會話 */
    fun startSession(siteId: String, agentId: String, token: String? = null) {
        viewModel.startSession(siteId = siteId, agentId = agentId, token = token)
    }

    /** 發送文字訊息 */
    fun sendMessage(text: String) {
        viewModel.sendMessage(text)
    }

    /** 綁定玩家 Token */
    fun bindToken(token: String) {
        viewModel.bindToken(token)
    }
}
