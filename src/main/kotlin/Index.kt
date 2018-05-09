import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLInputElement
import kotlin.browser.window

object IndexController {

    private val tagInput by get<HTMLInputElement>("tagInput")
    private val siteInput by get<HTMLInputElement>("siteInput")
    private val tagButton by get<HTMLButtonElement>("send")

    @Suppress("unused")
    @JsName("run")
    fun run() {
        tagButton.onclick = {
            val tagName = tagInput.value
            val siteName = siteInput.value
            val observationSet: ObservationSet = mutableListOf()
            observationSet.add(Observation(siteName, tagName))

            window.location.assign("watch.html#" + observationSet.toHash())
        }
    }
}