package chat.im.trilivechat.chatedin

actual suspend fun platformRealDelay(millis: Long) {
    platform.Foundation.NSThread.sleepForTimeInterval(millis / 1000.0)
}
