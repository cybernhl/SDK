package chat.im.trilivechat.chatedin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.im.trilivechat.chatedin.core.LiveChatConfig
import chat.im.trilivechat.chatedin.core.SessionProvider
import idv.neo.entity.cs.trilivechat.response.ChatMessageItem
import chat.im.trilivechat.chatedin.repository.TriliveChatRepository
import chat.im.trilivechat.chatedin.state.TriliveChatUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TriliveChatViewModel(
    private val repository: TriliveChatRepository,
    private val sessionProvider: SessionProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(TriliveChatUiState())
    val uiState: StateFlow<TriliveChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionProvider.sessionState.collect { state ->
                _uiState.update { it.copy(sessionState = state) }
            }
        }
    }

    /**
     * 初始化 LiveChat 客服對話 Session。
     *
     * @param siteId 商戶站台 ID（必填，由宿主 App 提供）。
     * @param agentId 客服專員/佇列 ID（必填，由宿主 App 提供）。
     * @param token 玩家 Auth JWT Token（選填，登入狀態傳入）。
     */
    fun startSession(
        siteId: String,
        agentId: String,
        token: String? = null
    ) {
        if (siteId.isBlank() || agentId.isBlank()) {
            println("[TriliveChatSDK] startSession skipped: siteId and agentId must not be blank.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                sessionProvider.updateConfig(
                    LiveChatConfig(
                        siteId = siteId,
                        agentId = agentId,
                        token = token
                    )
                )

                // Fetch Session Detail for Site Name & Agent Info
                try {
                    val detail = repository.getSessionDetail()
                    if (detail.success) {
                        _uiState.update {
                            it.copy(
                                siteName = detail.site?.name,
                                agentName = detail.agent?.name,
                                agentAvatarUrl = detail.agent?.avatarUrl,
                                faqs = detail.faqs ?: emptyList()
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Ignore transient errors on detail fetch
                }

                // Trigger greeting
                val greetRes = repository.getGreeting()
                val greetingMsg = greetRes.greeting
                if (!greetingMsg.isNullOrBlank()) {
                    val botMessage = ChatMessageItem(
                        id = currentTimeMillis(),
                        senderType = "ai",
                        senderName = _uiState.value.agentName ?: "客服",
                        message = greetingMsg
                    )
                    _uiState.update { it.copy(messages = listOf(botMessage)) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "無法建立客服會話") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 發送聊天訊息。
     */
    fun sendMessage(chatInput: String) {
        if (chatInput.isBlank()) return
        val currentMessages = _uiState.value.messages.toMutableList()

        // Append user message
        val userMsg = ChatMessageItem(
            id = currentTimeMillis(),
            senderType = "customer",
            message = chatInput
        )
        currentMessages.add(userMsg)
        _uiState.update { it.copy(messages = currentMessages, isSending = true) }

        viewModelScope.launch {
            try {
                val chatRes = repository.sendChatMessage(chatInput)
                val reply = chatRes.output
                if (!reply.isNullOrBlank()) {
                    val botMsg = ChatMessageItem(
                        id = currentTimeMillis(),
                        senderType = "ai",
                        senderName = _uiState.value.agentName ?: "客服",
                        message = reply
                    )
                    _uiState.update {
                        it.copy(messages = it.messages + botMsg, isSending = false)
                    }
                } else {
                    _uiState.update { it.copy(isSending = false) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "傳送失敗: ${e.message}",
                        isSending = false
                    )
                }
            }
        }
    }

    /**
     * 身分升級綁定玩家 Token。
     */
    fun bindToken(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, bindingMessage = null) }
            try {
                val response = sessionProvider.bindToken(token)
                if (response.success) {
                    _uiState.update {
                        it.copy(
                            bindingMessage = "身分綁定成功！歡迎 ${response.username ?: ""}",
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = response.detail ?: "身分綁定失敗",
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "綁定異常: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun currentTimeMillis(): Long {
        return kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds
    }
}
