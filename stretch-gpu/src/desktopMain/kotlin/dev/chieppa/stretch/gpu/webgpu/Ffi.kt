package dev.chieppa.stretch.gpu.webgpu

import dev.chieppa.stretch.gpu.ffi.WGPUChainedStruct
import dev.chieppa.stretch.gpu.ffi.WGPUStringView
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** webgpu.h's WGPU_STRLEN: a string view whose text ends with a zero byte. */
private const val STRLEN = -1L

/** Points the string view embedded at [view] at [text], allocated in [arena]; null leaves it the null view. */
internal fun Arena.setString(view: MemorySegment, text: String?) {
    if (text == null) {
        WGPUStringView.data(view, MemorySegment.NULL)
    } else {
        WGPUStringView.data(view, allocateFrom(text))
    }
    WGPUStringView.length(view, STRLEN)
}

/** The text of the string view at [view]; empty for the null view. */
internal fun readString(view: MemorySegment): String {
    val data = WGPUStringView.data(view)
    if (data == MemorySegment.NULL) return ""
    val length = WGPUStringView.length(view)
    return if (length == STRLEN) {
        data.reinterpret(Long.MAX_VALUE).getString(0)
    } else {
        String(data.reinterpret(length).toArray(ValueLayout.JAVA_BYTE), Charsets.UTF_8)
    }
}

/** Fills the chained-struct header at the start of [struct] with [sType] and no next link. */
internal fun chain(struct: MemorySegment, sType: Int) {
    val header = struct.asSlice(0, WGPUChainedStruct.layout())
    WGPUChainedStruct.next(header, MemorySegment.NULL)
    WGPUChainedStruct.sType(header, sType)
}

/** An array of [count] structs of [layout]'s size in [arena], each filled by [fill]. */
internal inline fun Arena.structs(count: Int, layout: java.lang.foreign.MemoryLayout, fill: (index: Int, struct: MemorySegment) -> Unit): MemorySegment {
    if (count == 0) return MemorySegment.NULL
    val all = allocate(layout, count.toLong())
    for (i in 0 until count) fill(i, all.asSlice(i * layout.byteSize(), layout.byteSize()))
    return all
}

/** webgpu.h's optional boolean, as WGPUOptionalBool. */
internal fun optional(value: Boolean): Int = if (value) 1 else 0
