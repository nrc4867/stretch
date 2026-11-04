@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.chieppa.stretch.document

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.khronos.webgl.Uint8Array
import kotlin.js.Promise

private const val DATABASE_PREFIX = "stretch-recovery/"
private const val LOCK_PREFIX = "stretch-recovery-lock/"
private const val DEFAULT_APP_ID = "stretch-app"

private external interface StoredCopy : JsAny {
    val key: String
    val path: String?
    val name: String?
    val time: Double
    val data: Uint8Array
}

private fun storedCopy(key: String, path: String?, name: String?, data: Uint8Array): StoredCopy =
    js("({ key: key, path: path, name: name, time: Date.now(), data: data })")

private fun newKey(): String = js("crypto.randomUUID()")

/** Holds the lock named [name] until the returned function is called. */
private fun holdLock(name: String): JsAny = js("""(() => {
    let release;
    const held = new Promise((resolve) => { release = resolve; });
    navigator.locks.request(name, () => held);
    return release;
})()""")

private fun releaseLock(release: JsAny): Unit = js("release()")

/** The names of the locks held now, in any tab of this site, joined with newlines. */
private fun heldLocks(): Promise<JsString> = js("""navigator.locks.query()
    .then((state) => state.held.map((lock) => lock.name).join('\n'))""")

/** [Recovery] in IndexedDB, with a Web Lock per slot, which the browser frees when the tab goes. */
private class BrowserRecovery(id: String) : Recovery {
    private val store = BrowserStore("$DATABASE_PREFIX$id")
    private val lockPrefix = "$LOCK_PREFIX$id/"

    override fun slot(): RecoverySlot = BrowserRecoverySlot(store, newKey(), lockPrefix)

    override suspend fun pending(): List<RecoveredDocument> {
        val held = heldLocks().await<JsString>().toString().split('\n').toSet()
        val copies = store.all()
        return (0 until copies.length).mapNotNull { copies[it]?.unsafeCast<StoredCopy>() }
            .filter { "$lockPrefix${it.key}" !in held }
            .sortedByDescending { it.time }
            .map { RecoveredDocument(it.key, it.path, it.name) }
    }

    override suspend fun read(item: RecoveredDocument): ByteArray =
        checkNotNull(store.get(item.key)) { "No recovered copy of ${item.key}" }.unsafeCast<StoredCopy>().data.toByteArray()

    override suspend fun discard(item: RecoveredDocument) = store.delete(item.key)
}

private class BrowserRecoverySlot(
    private val store: BrowserStore,
    private val key: String,
    private val lockPrefix: String,
) : RecoverySlot {
    private var lock: JsAny? = null

    override suspend fun write(bytes: ByteArray, path: String?, name: String?) {
        if (lock == null) lock = holdLock("$lockPrefix$key")
        store.put(key, storedCopy(key, path, name, bytes.toUint8Array()))
    }

    override suspend fun clear() = store.delete(key)

    override fun close() {
        CoroutineScope(Dispatchers.Default).launch { store.delete(key) }
        lock?.let(::releaseLock)
        lock = null
    }
}

/** Recovery in this site's browser storage, for this browser only. */
actual fun Recovery.Companion.forApp(id: String?): Recovery = BrowserRecovery(id ?: DEFAULT_APP_ID)

private fun guardLeaving(): JsAny = js("""(() => {
    const guard = (event) => event.preventDefault();
    window.addEventListener('beforeunload', guard);
    return () => window.removeEventListener('beforeunload', guard);
})()""")

private fun stopGuarding(stop: JsAny): Unit = js("stop()")

/** While [edited], the browser asks before the tab closes or leaves the page. */
@Composable
actual fun LeaveGuard(edited: Boolean) {
    DisposableEffect(edited) {
        val stop = if (edited) guardLeaving() else null
        onDispose { stop?.let(::stopGuarding) }
    }
}
