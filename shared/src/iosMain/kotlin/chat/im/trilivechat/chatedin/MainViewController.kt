package chat.im.trilivechat.chatedin

import androidx.compose.ui.window.ComposeUIViewController
import chat.im.trilivechat.chatedin.di.commonModule
import chat.im.trilivechat.chatedin.di.viewModelModule
import org.koin.core.context.startKoin

fun MainViewController(
    siteId: String = "78505330",
    agentId: String = "18524598",
    initialToken: String? = null
) = ComposeUIViewController {
    try {
        startKoin {
            modules(commonModule, viewModelModule)
        }
    } catch (_: Exception) {
        // Koin already started
    }
    App(siteId = siteId, agentId = agentId, initialToken = initialToken)
}

fun MainViewController() = MainViewController(
    siteId = "78505330",
    agentId = "18524598",
    initialToken = null
)
