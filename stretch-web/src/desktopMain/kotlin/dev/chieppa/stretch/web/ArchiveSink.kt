package dev.chieppa.stretch.web

import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Flow

/**
 * Takes a download's bytes as they arrive, one batch at a time, into [file] and [digest], reporting the total so far
 * to [onProgress]. [done] completes when the last byte is written; [stop] ends it early and lets go of the file.
 */
internal class ArchiveSink(file: File, private val digest: MessageDigest, private val onProgress: (Long) -> Unit) : Flow.Subscriber<List<ByteBuffer>> {
    val done = CompletableFuture<Unit>()
    private val channel = FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
    private var subscription: Flow.Subscription? = null
    private var written = 0L
    private var stopped = false

    @Synchronized
    override fun onSubscribe(subscription: Flow.Subscription) {
        this.subscription = subscription
        if (stopped) subscription.cancel() else subscription.request(1)
    }

    @Synchronized
    override fun onNext(item: List<ByteBuffer>) {
        if (stopped) return
        for (buffer in item) {
            digest.update(buffer.duplicate())
            while (buffer.hasRemaining()) written += channel.write(buffer)
        }
        onProgress(written)
        subscription?.request(1)
    }

    @Synchronized
    override fun onError(throwable: Throwable) {
        finish()
        done.completeExceptionally(throwable)
    }

    @Synchronized
    override fun onComplete() {
        finish()
        done.complete(Unit)
    }

    /** Cancels the download if it is still coming and lets go of the file. */
    @Synchronized
    fun stop() {
        if (!stopped) subscription?.cancel()
        finish()
    }

    /** Lets go of the file once the download has ended, which must not cancel what has already finished. */
    private fun finish() {
        if (stopped) return
        stopped = true
        channel.close()
    }
}
