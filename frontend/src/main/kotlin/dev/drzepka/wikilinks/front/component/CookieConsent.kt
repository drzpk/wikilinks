package dev.drzepka.wikilinks.front.component

import io.kvision.html.ButtonStyle
import io.kvision.html.Div
import io.kvision.html.button
import io.kvision.html.div
import kotlinx.browser.window
import org.w3c.dom.set

class CookieConsent : Div() {
    init {
        id = "cookie-consent"
        if (window.asDynamic().analyticsLoaded == true && getConsentStatus() == CookieConsentStatus.UNKNOWN)
            initialize()
    }

    private fun initialize() {
        div (className = "content") {
            val contentDiv = this

            div(CONSENT_TEXT, rich = true, className = "text")
            div(className = "controls") {
                button("Accept", style = ButtonStyle.PRIMARY) {
                    onClick {
                        this@CookieConsent.updateCookieConsentStatus(CookieConsentStatus.GRANTED)
                        contentDiv.parent?.remove(contentDiv)
                    }
                }
                button("Reject", style = ButtonStyle.LINK) {
                    onClick {
                        this@CookieConsent.updateCookieConsentStatus(CookieConsentStatus.DENIED)
                        contentDiv.parent?.remove(contentDiv)
                    }
                }
            }
        }
    }

    private fun getConsentStatus(): CookieConsentStatus {
        val raw = window.localStorage.getItem(COOKIE_CONSENT_STATUS_KEY) ?: ""
        return try {
            CookieConsentStatus.valueOf(raw)
        } catch (e: Exception) {
            CookieConsentStatus.UNKNOWN
        }
    }

    private fun updateCookieConsentStatus(newStatus: CookieConsentStatus) {
        val gtag = window.asDynamic().gtag ?: return

        val statusValue = when (newStatus) {
            CookieConsentStatus.GRANTED -> "granted"
            CookieConsentStatus.DENIED -> "denied"
            else -> null
        }

        if (statusValue != null) {
            val data = js("{}")
            data["analytics_storage"] = statusValue
            gtag("consent", "update", data)

            window.localStorage[COOKIE_CONSENT_STATUS_KEY] = newStatus.name
        }
    }

    private enum class CookieConsentStatus {
        UNKNOWN, GRANTED, DENIED
    }

    companion object {
        private const val COOKIE_CONSENT_STATUS_KEY = "cookieConsent"
        private val CONSENT_TEXT = """
            This website uses cookies to improve collection of traffic data.
            Data is only collected for analytical purposes and is <strong>not</strong>
            used for advertisement personalization (because there are no ads).
            By clicking <i>Accept</i> you consent to use of cookies on this website.
        """.trimIndent()
    }
}
