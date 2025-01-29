package com.keyiflerolsun

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class Watch2MoviesPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Watch2Movies())
        registerExtractorAPI(W2MExtractor(context))
    }
}
