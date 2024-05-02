package sp.kx.storages

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class MockSyncStreamsStorage<T : Any>(
    id: UUID = UUID.fromString("a523adfd-07c1-450b-834d-6c34dec9fa4f"),
    private val timeProvider: MockProvider<Duration> = mockProvider { 1.milliseconds },
    private val uuidProvider: MockProvider<UUID> = mockProvider { UUID.fromString("d2d7c21b-f99a-4f78-80d4-8bf05ee25f62") },
    defaultDeleted: Set<UUID> = emptySet(),
    private val hashes: List<Pair<ByteArray, String>>,
    private val transformer: List<Pair<ByteArray, T>> = emptyList(),
) : SyncStreamsStorage<T>(id = id) {
    private val stream = ByteArrayOutputStream().also { stream ->
        stream.write("${defaultDeleted.joinToString(separator = "") { it.toString() }}\n0".toByteArray())
    }

    override fun now(): Duration {
        return timeProvider.provide()
    }

    override fun randomUUID(): UUID {
        return uuidProvider.provide()
    }

    override fun hash(bytes: ByteArray): String {
        return hashes.firstOrNull { (key, _) -> key.contentEquals(bytes) }?.second ?: error("No hash!\n---\n${String(bytes)}\n---")
    }

    override fun encode(item: T): ByteArray {
        return transformer.firstOrNull { (_, value) -> value == item }?.first ?: error("No encoded!")
    }

    override fun decode(bytes: ByteArray): T {
        return transformer.firstOrNull { (key, _) -> key.contentEquals(bytes) }?.second ?: error("No decoded!")
    }

    override fun inputStream(): InputStream {
        return stream.toByteArray().inputStream()
    }

    override fun outputStream(): OutputStream {
        stream.reset()
        return stream
    }
}
