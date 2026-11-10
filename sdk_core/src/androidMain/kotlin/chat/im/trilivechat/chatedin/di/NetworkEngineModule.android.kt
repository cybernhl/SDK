package chat.im.trilivechat.chatedin.di

import android.annotation.SuppressLint
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.config
import org.koin.core.module.Module
import org.koin.dsl.module
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

actual val networkEngineModule: Module = module {
    single<HttpClientEngineFactory<*>> {
        CIO.config {
            https {
                trustManager = TrustAllCerts
            }
        }
    }
}

@SuppressLint("CustomX509TrustManager")
private object TrustAllCerts : X509TrustManager {
    @SuppressLint("TrustAllX509TrustManager")
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
    @SuppressLint("TrustAllX509TrustManager")
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
}