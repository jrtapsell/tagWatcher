import org.w3c.dom.*
import org.w3c.notifications.GRANTED
import org.w3c.notifications.Notification
import org.w3c.notifications.NotificationOptions
import org.w3c.notifications.NotificationPermission
import kotlin.browser.document
import kotlin.browser.window
import kotlin.js.Date
import kotlin.js.Promise

const val SOCKET_ENDPOINT = "wss://qa.sockets.stackexchange.com/"
const val SOCKET_ACTION = "155-questions-active"
const val SO_ICON = "https://cdn.sstatic.net/Sites/stackoverflow/img/apple-touch-icon.png"

@Suppress("unused")
external class StackMessage {
    val action: String
    val data: String
}

@Suppress("unused")
external class UpdateMessage {
    val apiSiteParameter: String
    val bodySummary: String
    val id: Long
    val lastActivityDate: Long
    val ownerDisplayName: String
    val ownerUrl: String
    val siteBaseHostAddress: String
    val tags: Array<String>
    val titleEncodedFancy: String
    val url: String
}

class SEEventSource(private val block: UpdateMessage.()->Unit) {
    private var socket: WebSocket? = null

    init {
        renew()
    }

    fun renew() {
        socket?.close()
        socket = WebSocket(SOCKET_ENDPOINT).let {ws ->
            ws.onmessage = { onMessage(it as MessageEvent) }
            ws.onopen = {
                ws.send(SOCKET_ACTION)
            }
            ws
        }
    }

    private fun onMessage(it: MessageEvent) {
        val event = JSON.parse<StackMessage>(it.data as String)
        when (event.action) {
            "hb" -> socket!!.send("hb")
            SOCKET_ACTION -> block(JSON.parse(event.data))
        }
    }
}

object NotificationManager {
    private var active = false
    private val granted = NotificationPermission.GRANTED

    fun activate() {

        if (Notification.permission == granted) {
            Promise.resolve(granted)
        } else {
            Notification.requestPermission()
        }.then {
            if (it == granted) {
                active = true
                notify("Notifications active")
            }
        }
    }

    data class NotOpt (val body: String, val icon: String)

    fun notify(message: String, link: String?=null) {
        if (active) {
            NotificationOptions()
            val notification = Notification(
                    title = "Tag Watcher",
                    options = NotOpt(message, SO_ICON).unsafeCast<NotificationOptions>()
            )
            if (link != null) {
                notification.onclick = {
                    window.open(link)
                    notification.close()
                }
            }
        }
    }
}

@Suppress("unused")
object WatchController {
    private val watchdog by get<HTMLDivElement>("watchdog")
    private val title by get<HTMLHeadingElement>("pageHeader")
    private val observerData by get<HTMLTableElement>("watchDetails")

    @Suppress("unused")
    @JsName("run")
    fun run() {
        NotificationManager.activate()
        val hsh = window.location.hash.drop(1)
        val set = hsh.toObservationSet()

        set.forEach {
            val newRow = make<HTMLTableRowElement>()
            val siteCell = make<HTMLTableCellElement>()
            siteCell.textContent = it.siteName
            val tagCell = make<HTMLTableCellElement>()
            tagCell.textContent = it.tag
            newRow.append(siteCell, tagCell)
            observerData.append(newRow)
        }

        title.textContent = "Tag Watcher"
        var lastUpdate: Long = currentTimeMillis()

        val es = SEEventSource {
            lastUpdate = currentTimeMillis()
            if (set.any { apiSiteParameter == it.siteName && it.tag in tags }) {
                val topBar = make<HTMLDivElement>()
                val itemTitle = make<HTMLAnchorElement>()
                itemTitle.textContent = titleEncodedFancy
                itemTitle.href = url
                itemTitle.target = "_blank"
                topBar.append(itemTitle)
                document.body!!.append(topBar)
                NotificationManager.notify(titleEncodedFancy, url)
            }
        }

        window.setInterval({
            val now = currentTimeMillis()
            val delta = (now - lastUpdate) / 1000
            document.title = "${delta}s Watcher"
            val lastDate = Date(lastUpdate).toLocaleString()
            watchdog.textContent = "Last update: $delta seconds ago ($lastDate)"
            if (delta >= 120) {
                es.renew()
            }
        }, 1000)
    }
}