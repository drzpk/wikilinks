package dev.drzepka.wikilinks.app.route

import dev.drzepka.wikilinks.app.BackendConfiguration
import dev.drzepka.wikilinks.app.KoinApp.frontendResourceService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*

private const val analyticsPlaceholder = "<!-- ANALYTICS PLACEHOLDER -->"
private const val noscriptAnalyticsPlaceholder = "<!-- NOSCRIPT ANALYTICS PLACEHOLDER -->"

private val googleTagManagerHtml = """
      <script>(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':
            new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],
        j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src=
        'https://www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);

        function gtag(){window.dataLayer.push(arguments);}
        window.gtag = gtag;
      })(window,document,'script','dataLayer','GTM-TR3QDDT');</script>
""".trimIndent()

private val noScriptGoogleTagManagerHtml = """
    <noscript><iframe src="https://www.googletagmanager.com/ns.html?id=GTM-TR3QDDT"
                  height="0" width="0" style="display:none;visibility:hidden"></iframe></noscript>
""".trimIndent()

private var indexContent: ByteArray? = null

suspend fun PipelineContext<*, ApplicationCall>.respondWithIndexHtml() {
    val content = getIndexContent()
    call.respondBytes(content, ContentType.Text.Html)
}

private fun getIndexContent(): ByteArray {
    if (indexContent == null) {
        var stringContent = frontendResourceService.getResource("index.html")!!.content.decodeToString()
        if (BackendConfiguration.enableAnalytics) {
            stringContent = stringContent
                .replace(analyticsPlaceholder, googleTagManagerHtml)
                .replace(noscriptAnalyticsPlaceholder, noScriptGoogleTagManagerHtml)
        }

        indexContent = stringContent.encodeToByteArray()
    }


    return indexContent!!
}