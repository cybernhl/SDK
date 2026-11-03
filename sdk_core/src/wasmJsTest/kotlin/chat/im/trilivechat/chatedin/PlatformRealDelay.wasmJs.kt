package chat.im.trilivechat.chatedin

actual suspend fun platformRealDelay(millis: Long) {
    val target = kotlin.time.Duration.parse("${millis}ms")
    val mark = kotlin.time.TimeSource.Monotonic.markNow()
    while (mark.elapsedNow() < target) {
        // Busy wait for WasmJs
    }
}