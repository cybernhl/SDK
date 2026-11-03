package chat.im.trilivechat.chatedin.core

import chat.im.trilivechat.chatedin.state.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 專職維護記憶體中 Session 狀態與當前 LiveChatConfig 的純狀態容器 (Pure State Holder)。
 * 零 API 副作用，不直接發起網路請求。
 */
class SessionStateHolder {
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Uninitialized)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    var config: LiveChatConfig? = null

    fun getActiveSessionId(): String? {
        return (_sessionState.value as? SessionState.Active)?.sessionId
    }

    fun updateState(newState: SessionState) {
        _sessionState.value = newState
    }
}
