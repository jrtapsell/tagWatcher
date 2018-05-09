import org.w3c.dom.*
import kotlin.browser.document
import kotlin.js.Date

fun currentTimeMillis() = Date().getTime().toLong()

external fun atob(a: String): String
external fun btoa(a: String): String

inline fun <reified T: HTMLElement> make(): T {
    val name = when (T::class) {
        HTMLDivElement::class -> "div"
        HTMLAnchorElement::class -> "a"
        HTMLTableCellElement::class -> "td"
        HTMLTableRowElement::class -> "tr"
        else -> throw AssertionError(T::class.simpleName)
    }

    return document.createElement(name).unsafeCast<T>()
}

inline fun <reified T: HTMLElement> get(id: String): Lazy<T> {
    return lazy {document.getElementById(id).unsafeCast<T>()}
}

data class Observation(
    val siteName: String,
    val tag: String
)

typealias ObservationSet = MutableList<Observation>

fun ObservationSet.toHash() = btoa(JSON.stringify(this))

fun String.toObservationSet(): ObservationSet = JSON.parse<Array<Observation>>(atob(this)).toMutableList()
