package dev.chieppa.stretch.web

import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.concurrent.Flow
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArchiveSinkTest {
    private class Subscription : Flow.Subscription {
        var requested = 0L
        var cancelled = false
        override fun request(n: Long) {
            requested += n
        }
        override fun cancel() {
            cancelled = true
        }
    }

    @Test
    fun aFinishedDownloadIsNotCancelled() {
        // Over HTTP/2 a cancel after the last byte resets the stream and fails the whole response.
        val folder = createTempDirectory("archive-sink").toFile()
        val file = File(folder, "archive")
        val subscription = Subscription()
        var progress = 0L
        val sink = ArchiveSink(file, MessageDigest.getInstance("SHA-512")) { progress = it }
        sink.onSubscribe(subscription)
        sink.onNext(listOf(ByteBuffer.wrap(byteArrayOf(1, 2)), ByteBuffer.wrap(byteArrayOf(3))))
        sink.onComplete()
        sink.stop()
        assertFalse(subscription.cancelled)
        assertTrue(sink.done.isDone)
        assertEquals(3, progress)
        assertEquals(listOf<Byte>(1, 2, 3), file.readBytes().toList())
        folder.deleteRecursively()
    }

    @Test
    fun stoppingADownloadUnderWayCancelsIt() {
        val folder = createTempDirectory("archive-sink").toFile()
        val subscription = Subscription()
        val sink = ArchiveSink(File(folder, "archive"), MessageDigest.getInstance("SHA-512")) {}
        sink.onSubscribe(subscription)
        sink.stop()
        assertTrue(subscription.cancelled)
        folder.deleteRecursively()
    }
}
