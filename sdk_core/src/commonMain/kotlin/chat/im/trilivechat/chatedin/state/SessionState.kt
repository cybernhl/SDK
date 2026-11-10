package chat.im.trilivechat.chatedin.state


sealed class SessionState {
    object Uninitialized : SessionState()
    object Initializing : SessionState()
    data class Active(val sessionId: String, val isBound: Boolean, val username: String? = null) : SessionState()
    data class Error(val message: String) : SessionState()
}