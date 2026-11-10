package chat.im.trilivechat.chatedin

actual suspend fun platformRealDelay(millis: Long) {
    Thread.sleep(millis)
}