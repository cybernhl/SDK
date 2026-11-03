package chat.im.trilivechat.chatedin

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform