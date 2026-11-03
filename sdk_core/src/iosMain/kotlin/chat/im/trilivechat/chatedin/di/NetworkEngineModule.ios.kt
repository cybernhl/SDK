package chat.im.trilivechat.chatedin.di

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.config
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.credentialForTrust
import platform.Foundation.serverTrust
import platform.Security.SecTrustRef

actual val networkEngineModule: Module = module {
    single<HttpClientEngineFactory<*>> {
        Darwin
        //TODO below is enable "ignore or TrustAllCerts"
//            .config {
//            @OptIn(ExperimentalForeignApi::class)
//            handleChallenge { _, _, challenge, completionHandler ->
//                val trust: SecTrustRef? = challenge.protectionSpace.serverTrust
//                if (trust != null) {
//                    completionHandler(
//                        NSURLSessionAuthChallengeUseCredential,
//                        NSURLCredential.credentialForTrust(trust)
//                    )
//                    return@handleChallenge
//                }
//                completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
//            }
//        }
    }
}