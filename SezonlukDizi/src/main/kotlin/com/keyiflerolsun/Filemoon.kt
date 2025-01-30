package com.lagradost.cloudstream3.extractors

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

open class Filemoon(
    override val name: String = "Filemoon",
    override val mainUrl: String = "https://filemoon.in",
    override val requiresReferer: Boolean = true
) : ExtractorApi() {
    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val client = OkHttpClient()

        val filemoonRequest = Request.Builder().url(url).addHeader("User-Agent", USER_AGENT).addHeader("Referer", referer!!).build()
        client.newCall(filemoonRequest).execute().use { filemoonResponse ->
            val filemoonResponseBody = filemoonResponse.body.string()
            println("filemoon response: $filemoonResponseBody")

            val filemoonRegex = Regex("""<iframe src="(.*?)"""")
            val filemoonRegexResult = filemoonRegex.find(filemoonResponseBody,0)
            if(filemoonRegexResult != null){
                val firstUrl = filemoonRegexResult.groupValues[1]
                println("filemoon firsturl: $firstUrl")

                val firstUrlRequest = Request.Builder()
                    .url(firstUrl)
                    .addHeader(
                        "User-Agent",
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:134.0) Gecko/20100101 Firefox/134.0"
                    )
                    .addHeader("Accept-Language", "en-US,en;q=0.5")
                    .build()

                client.newCall(firstUrlRequest).execute().use { firstUrlResponse ->
                    if (!firstUrlResponse.isSuccessful) throw IOException("Unexpected code $firstUrlResponse")
                    val firstUrlBody = firstUrlResponse.body.string()

                    val evalRegex = Regex("""eval(.*;?)""")
                    val evalRegexResult = evalRegex.find(firstUrlBody, 0)

                    if (evalRegexResult != null) {
                        val jsCode = evalRegexResult.groupValues[1]
                        println("Extracted Value: $jsCode")

                        val cx = Context.enter()
                        try {
                            val scope: Scriptable = cx.initStandardObjects()
                            val result: Any = cx.evaluateString(scope, jsCode, "script", 1, null)
                            val resultString = Context.toString(result)
                            println("Result of the JavaScript function: $resultString")

                            val sourceRegex = Regex("""file:.*?"(.*?)"""")
                            val sourceRegexResult = sourceRegex.find(resultString, 0)
                            if (sourceRegexResult != null) {
                                val source = sourceRegexResult.groupValues[1]
                                println("Extracted source: $source")

                                callback(
                                    ExtractorLink(
                                        source = this.name,
                                        name = this.name,
                                        url = source,
                                        referer = "",
                                        quality = Qualities.Unknown.value,
                                        INFER_TYPE
                                    )
                                )
                            }
                        } finally {
                            Context.exit()
                        }
                    } else {
                        println("No match found")
                    }
                }
            }
        }
    }
}