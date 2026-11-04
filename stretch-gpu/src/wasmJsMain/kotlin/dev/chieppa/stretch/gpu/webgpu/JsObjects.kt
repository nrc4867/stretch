@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.chieppa.stretch.gpu.webgpu

private fun newObject(): JsAny = js("({})")

private fun setProperty(target: JsAny, key: String, value: JsAny): Unit = js("{ target[key] = value; }")

/** A JavaScript object with the properties [fill] sets, as WebGPU's descriptors are. */
internal fun jsObject(fill: JsObjectScope.() -> Unit): JsAny = newObject().also { JsObjectScope(it).fill() }

/** Sets a JavaScript object's properties; a null value leaves the property out. */
internal class JsObjectScope(private val target: JsAny) {
    operator fun set(key: String, value: JsAny?) {
        if (value != null) setProperty(target, key, value)
    }

    operator fun set(key: String, value: String?) = set(key, value?.toJsString())

    operator fun set(key: String, value: Int?) = set(key, value?.toJsNumber())

    operator fun set(key: String, value: Long?) = set(key, value?.toDouble()?.toJsNumber())

    operator fun set(key: String, value: Double?) = set(key, value?.toJsNumber())

    operator fun set(key: String, value: Boolean?) = set(key, value?.toJsBoolean())
}

/** The list as a JavaScript array. */
internal fun <T> List<T>.toJs(item: (T) -> JsAny): JsArray<JsAny> {
    val array = JsArray<JsAny>()
    forEachIndexed { i, value -> array[i] = item(value) }
    return array
}
