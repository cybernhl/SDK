package chat.im.trilivechat.chatedin.core

import chat.im.trilivechat.chatedin.api.TriliveChatApi
import chat.im.trilivechat.chatedin.state.SessionState
import idv.neo.entity.cs.trilivechat.request.BindTokenRequest
import idv.neo.entity.cs.trilivechat.request.CreateSessionRequest
import idv.neo.entity.cs.trilivechat.response.BindTokenResponse
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SessionCoordinator(
    private val api: TriliveChatApi,
    private val stateHolder: SessionStateHolder
) : SessionProvider {

    override val sessionState: StateFlow<SessionState> = stateHolder.sessionState
    private val mutex = Mutex()
    private var currentConfig: LiveChatConfig? = null

    override fun getActiveSessionId(): String? = stateHolder.getActiveSessionId()

    override fun updateConfig(config: LiveChatConfig) {
        currentConfig = config
    }

    override fun getConfig(): LiveChatConfig? = currentConfig

    override suspend fun requireSessionId(forceNew: Boolean): String {
        if (!forceNew) {
            getActiveSessionId()?.let { return it }
        }

        val config = currentConfig
            ?: throw IllegalStateException("LiveChatConfig is not initialized. Please call updateConfig or provide siteId and agentId.")

        if (config.siteId.isBlank() || config.agentId.isBlank()) {
            throw IllegalArgumentException("siteId and agentId must not be blank.")
        }

        return mutex.withLock {
            if (!forceNew) {
                getActiveSessionId()?.let { return it }
            }

            stateHolder.updateState(SessionState.Initializing)
            try {
                val response = api.createSession(
                    CreateSessionRequest(
                        siteId = config.siteId,
                        agentId = config.agentId,
                        token = config.token
                    )
                )

                val sid = response.sessionId
                if (response.success && !sid.isNullOrBlank()) {
                    val isBound = !config.token.isNullOrBlank()
                    stateHolder.updateState(SessionState.Active(sessionId = sid, isBound = isBound))
                    sid
                } else {
                    val errorMsg = response.detail ?: "Failed to create session"
                    stateHolder.updateState(SessionState.Error(errorMsg))
                    throw IllegalStateException(errorMsg)
                }
            } catch (e: Exception) {
                stateHolder.updateState(SessionState.Error(e.message ?: "Network error"))
                throw e
            }
        }
    }

    override suspend fun bindToken(token: String): BindTokenResponse {
        val currentSid = requireSessionId()
        return try {
            val response = api.bindToken(
                sessionId = currentSid,
                request = BindTokenRequest(token = token)
            )
            if (response.success) {
                stateHolder.updateState(
                    SessionState.Active(
                        sessionId = currentSid,
                        isBound = true,
                        username = response.username
                    )
                )
                currentConfig = currentConfig?.copy(token = token)
            }
            response
        } catch (e: Exception) {
            BindTokenResponse(success = false, detail = e.message)
        }
    }

    override fun clearSession() {
        stateHolder.updateState(SessionState.Uninitialized)
    }
}
