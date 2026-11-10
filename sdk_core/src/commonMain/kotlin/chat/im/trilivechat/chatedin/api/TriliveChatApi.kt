package chat.im.trilivechat.chatedin.api

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query
import idv.neo.entity.cs.trilivechat.request.BindTokenRequest
import idv.neo.entity.cs.trilivechat.request.ChatRequest
import idv.neo.entity.cs.trilivechat.request.CreateSessionRequest
import idv.neo.entity.cs.trilivechat.request.GreetRequest
import idv.neo.entity.cs.trilivechat.request.RatingRequest
import idv.neo.entity.cs.trilivechat.request.TypingRequest
import idv.neo.entity.cs.trilivechat.response.BindTokenResponse
import idv.neo.entity.cs.trilivechat.response.ChatMessageListResponse
import idv.neo.entity.cs.trilivechat.response.ChatResponse
import idv.neo.entity.cs.trilivechat.response.CreateSessionResponse
import idv.neo.entity.cs.trilivechat.response.GreetResponse
import idv.neo.entity.cs.trilivechat.response.SessionDetailResponse
import idv.neo.entity.cs.trilivechat.response.UploadResponse
import io.ktor.client.request.forms.MultiPartFormDataContent

interface TriliveChatApi {

    @Headers("Content-Type: application/json")
    @POST("api/client/sessions")
    suspend fun createSession(
        @Body request: CreateSessionRequest
    ): CreateSessionResponse

    @GET("api/client/sessions/{sessionId}")
    suspend fun getSessionDetail(
        @Path("sessionId") sessionId: String
    ): SessionDetailResponse

    @Headers("Content-Type: application/json")
    @POST("api/client/sessions/{sessionId}/bind-token")
    suspend fun bindToken(
        @Path("sessionId") sessionId: String,
        @Body request: BindTokenRequest
    ): BindTokenResponse

    @Headers("Content-Type: application/json")
    @POST("webhook/greet")
    suspend fun getGreeting(
        @Body request: GreetRequest
    ): GreetResponse

    @Headers("Content-Type: application/json")
    @POST("webhook/chat")
    suspend fun sendChat(
        @Body request: ChatRequest
    ): ChatResponse

    @GET("api/client/sessions/{sessionId}/messages")
    suspend fun getMessages(
        @Path("sessionId") sessionId: String,
        @Query("since_id") sinceId: Long? = null
    ): ChatMessageListResponse

    @POST("api/client/upload")
    suspend fun uploadImage(
        @Body formData: MultiPartFormDataContent
    ): UploadResponse

    @Headers("Content-Type: application/json")
    @POST("webhook/typing")
    suspend fun sendTyping(
        @Body request: TypingRequest
    )

    @Headers("Content-Type: application/json")
    @POST("api/client/ratings")
    suspend fun sendRating(
        @Body request: RatingRequest
    )
}
