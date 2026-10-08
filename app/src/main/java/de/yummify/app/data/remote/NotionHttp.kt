package de.yummify.app.data.remote

import de.yummify.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/** Shared HTTP setup for every Notion call: one connection pool, no secrets in logs, polite rate-limit retries. */
object NotionHttp {
    const val BASE_URL = "https://api.notion.com/v1"
    const val VERSION = "2025-09-03"

    val client: OkHttpClient by lazy { builder().build() }

    fun builder(): OkHttpClient.Builder = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .addInterceptor(RateLimitRetry())
        .apply {
            // Headers only, debug builds only, and never the bearer token.
            if (BuildConfig.DEBUG) addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
                redactHeader("Authorization")
            })
        }
}

/**
 * Notion allows about three requests per second and answers 429 (or 503 when busy) with Retry-After.
 * Such requests were not processed, so repeating them is safe, also for page creation.
 */
class RateLimitRetry(private val maxRetries: Int = 3, private val sleep: (Long) -> Unit = { Thread.sleep(it) }) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var response = chain.proceed(chain.request())
        var attempt = 0
        while ((response.code == 429 || response.code == 503) && attempt < maxRetries) {
            val waitSeconds = response.header("Retry-After")?.toLongOrNull()?.coerceIn(1, 10) ?: (1L shl attempt)
            response.close()
            sleep(waitSeconds * 1000)
            attempt++
            response = chain.proceed(chain.request())
        }
        return response
    }
}
