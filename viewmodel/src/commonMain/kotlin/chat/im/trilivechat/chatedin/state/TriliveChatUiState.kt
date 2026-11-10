package chat.im.trilivechat.chatedin.state

import idv.neo.entity.cs.trilivechat.response.ChatMessageItem
import idv.neo.entity.cs.trilivechat.response.FaqItem


data class TriliveChatUiState(
    val messages: List<ChatMessageItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val isUploadingImage: Boolean = false,
    val sessionState: SessionState = SessionState.Uninitialized,
    val siteName: String? = null,
    val agentName: String? = null,
    val agentAvatarUrl: String? = null,
    val faqs: List<FaqItem> = emptyList(),
    val errorMessage: String? = null,
    val bindingMessage: String? = null
)
