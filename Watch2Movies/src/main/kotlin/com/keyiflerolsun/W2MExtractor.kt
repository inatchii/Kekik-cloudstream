// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.
package com.keyiflerolsun

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

open class W2MExtractor(override val mainUrl: String, val context: Context) : ExtractorApi() {
    override val name = "W2MExtractor"
    override val requiresReferer = true
    private lateinit var webView: WebView

    // Override the getUrl function
    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        withContext(Dispatchers.Main) {
            // Create a WebView in the background (it will be hidden)
            webView = WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    javaScriptCanOpenWindowsAutomatically = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    allowFileAccess = true
                    databaseEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    allowContentAccess = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString =
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36"
                }
                evaluateJavascript(
                    """
Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
Object.defineProperty(navigator, 'platform', { get: () => 'Win32' });
Object.defineProperty(navigator, 'language', { get: () => 'en-US' });
window.chrome = { runtime: {} };
""".trimIndent(), {}
                )
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        // Get the URL from the request
                        val url = request?.url.toString()
                        val headers = request?.requestHeaders
                        // Run a background thread to fetch the content manually
                        Thread {
                            fetchAndCheckResponse(url,headers) { sourceUrl, headers ->
                                callback.invoke(
                                    ExtractorLink(
                                        source = this@W2MExtractor.name,
                                        name = this@W2MExtractor.name,
                                        url = sourceUrl,
                                        referer = headers["Referer"] ?: headers["referer"] ?: mainUrl,
                                        quality = Qualities.Unknown.value,
                                        type = ExtractorLinkType.M3U8,
                                        headers = headers
                                    )
                                )
                            }
                        }.start()
                        return super.shouldInterceptRequest(view, request)
                    }
                }
                // Load the URL in the WebView (you can customize the URL here)
                loadUrl(url)
            }
        }
        // Wait for 10 seconds
        delay(10_000)
    }

    private fun fetchAndCheckResponse(
        url: String,
        headers: Map<String, String>?,
        onResponseCaptured: (url: String, headers: Map<String, String>) -> Unit
    ) {
        try {
            // Create URL connection
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            if(headers != null){
                for (header in headers){
                    connection.headerFields[header.key] = listOf(header.value)
                }
            }
            connection.connect()
            // Read the response content
            val inputStream = connection.inputStream
            val response = BufferedReader(InputStreamReader(inputStream))
                .lineSequence()
                .joinToString("\n")
            // Check if the content starts with #EXTM3U
            if (response.startsWith("#EXTM3U")) {
                // If the response starts with "#EXTM3U", pass it to the callback
                Log.d("W2M", response)
                onResponseCaptured(connection.url.toString(),headers ?: mapOf())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
