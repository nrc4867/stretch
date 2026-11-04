package dev.chieppa.stretch.document

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set

/** The bytes as a JavaScript array. */
internal fun ByteArray.toUint8Array(): Uint8Array {
    val array = Uint8Array(size)
    for (i in indices) array[i] = this[i]
    return array
}

internal fun Uint8Array.toByteArray(): ByteArray = ByteArray(length) { this[it] }

internal fun ArrayBuffer.toByteArray(): ByteArray = Uint8Array(this).toByteArray()
