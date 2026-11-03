package chat.im.trilivechat.chatedin.repository

import chat.im.trilivechat.chatedin.core.SessionProvider
import chat.im.trilivechat.chatedin.api.TriliveChatApi
import idv.neo.entity.cs.trilivechat.request.ChatRequest
import idv.neo.entity.cs.trilivechat.request.GreetRequest
import idv.neo.entity.cs.trilivechat.request.TypingRequest
import idv.neo.entity.cs.trilivechat.response.BindTokenResponse
import idv.neo.entity.cs.trilivechat.response.ChatMessageItem
import idv.neo.entity.cs.trilivechat.response.ChatResponse
import idv.neo.entity.cs.trilivechat.response.GreetResponse
import idv.neo.entity.cs.trilivechat.response.SessionDetailResponse
import idv.neo.entity.cs.trilivechat.response.UploadResponse
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TriliveChatRepository(
    private val api: TriliveChatApi,
    private val sessionProvider: SessionProvider
) {

    suspend fun getSessionDetail(): SessionDetailResponse {
        var sessionId = sessionProvider.requireSessionId()
        return try {
            val response = api.getSessionDetail(sessionId = sessionId)
            if (!response.success) {
                // If session is expired or invalid, recreate session
                println("[TriliveChatSDK] Session $sessionId detail invalid, recreating session...")
                sessionId = sessionProvider.requireSessionId(forceNew = true)
                api.getSessionDetail(sessionId = sessionId)
            } else {
                response
            }
        } catch (e: Exception) {
            println("[TriliveChatSDK] Session $sessionId detail exception (${e.message}), recreating session...")
            sessionId = sessionProvider.requireSessionId(forceNew = true)
            api.getSessionDetail(sessionId = sessionId)
        }
    }

    suspend fun getGreeting(): GreetResponse {
        val sessionId = sessionProvider.requireSessionId()
        return api.getGreeting(GreetRequest(sessionId = sessionId))
    }

    suspend fun sendChatMessage(messageText: String): ChatResponse {
        var sessionId = sessionProvider.requireSessionId()
        var response = api.sendChat(
            ChatRequest(
                chatInput = messageText,
                sessionId = sessionId,
                metadata = mapOf("userAgent" to "TriliveChatSDK KMP")
            )
        )

        // 當捕獲 session_expired 錯誤碼時，自動重建 Session 並重試
        if (response.errorCode == "session_expired") {
            println("[TriliveChatSDK] Received 'session_expired' for session $sessionId. Auto-recreating session...")
            sessionId = sessionProvider.requireSessionId(forceNew = true)
            response = api.sendChat(
                ChatRequest(
                    chatInput = messageText,
                    sessionId = sessionId,
                    metadata = mapOf("userAgent" to "TriliveChatSDK KMP")
                )
            )
        }

        return response
    }

    suspend fun bindUserToken(token: String): BindTokenResponse {
        return sessionProvider.bindToken(token)
    }

    suspend fun sendTyping(content: String) {
        val sessionId = sessionProvider.requireSessionId()
        api.sendTyping(TypingRequest(sessionId = sessionId, content = content))
    }

    suspend fun uploadImage(fileBytes: ByteArray, fileName: String): UploadResponse {
        return try {
            api.uploadImage(
                MultiPartFormDataContent(
                    formData {
                        append("file", fileBytes, Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        })
                    }
                )
            )
        } catch (e: Exception) {
            UploadResponse(success = false, detail = e.message)
        }
    }

    fun pollMessages(pollIntervalMs: Long = 3000L): Flow<List<ChatMessageItem>> = flow {
        var lastMsgId: Long? = null
        while (true) {
            try {
                val sessionId = sessionProvider.requireSessionId()
                val response = api.getMessages(sessionId = sessionId, sinceId = lastMsgId)
                if (response.success && response.messages.isNotEmpty()) {
                    lastMsgId = response.messages.last().id ?: lastMsgId
                    emit(response.messages)
                }
            } catch (e: Exception) {
                // Ignore transient network errors during polling
            }
            delay(pollIntervalMs)
        }
    }
}
