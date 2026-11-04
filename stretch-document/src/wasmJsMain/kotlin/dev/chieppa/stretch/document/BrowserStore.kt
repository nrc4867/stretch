@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.chieppa.stretch.document

import kotlinx.coroutines.await
import kotlin.js.Promise

private const val STORE_NAME = "values"

private fun storeRequest(database: String, store: String, write: Boolean, run: (JsAny) -> JsAny): Promise<JsAny?> = js("""new Promise((resolve, reject) => {
    const open = indexedDB.open(database, 1);
    open.onupgradeneeded = () => open.result.createObjectStore(store);
    open.onerror = () => reject(open.error);
    open.onsuccess = () => {
        const db = open.result;
        const transaction = db.transaction(store, write ? 'readwrite' : 'readonly');
        const request = run(transaction.objectStore(store));
        transaction.oncomplete = () => { db.close(); resolve(request.result === undefined ? null : request.result); };
        transaction.onabort = () => { db.close(); reject(transaction.error); };
    };
})""")

private fun storePut(store: JsAny, key: String, value: JsAny): JsAny = js("store.put(value, key)")

private fun storeGet(store: JsAny, key: String): JsAny = js("store.get(key)")

private fun storeDelete(store: JsAny, key: String): JsAny = js("store.delete(key)")

private fun storeAll(store: JsAny): JsAny = js("store.getAll()")

/** Values kept between visits in the browser's IndexedDB database [database]. */
internal class BrowserStore(private val database: String) {
    suspend fun put(key: String, value: JsAny) {
        storeRequest(database, STORE_NAME, true) { storePut(it, key, value) }.await<JsAny?>()
    }

    suspend fun get(key: String): JsAny? = storeRequest(database, STORE_NAME, false) { storeGet(it, key) }.await()

    suspend fun delete(key: String) {
        storeRequest(database, STORE_NAME, true) { storeDelete(it, key) }.await<JsAny?>()
    }

    /** Every value, in key order. */
    suspend fun all(): JsArray<JsAny> = checkNotNull(storeRequest(database, STORE_NAME, false, ::storeAll).await<JsAny?>()).unsafeCast()
}
