package sp.kx.storages

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class MockSyncStreamsStorage<T : Any>(
    id: UUID,
    private val now: Duration = 1.milliseconds,
    private val randomUUID: UUID = UUID.fromString("a9971314-2b26-4704-b145-f2473a7e068c"),
    defaultDeleted: Set<UUID> = emptySet(),
    private val hashes: List<Pair<ByteArray, String>>,
    private val transformer: List<Pair<ByteArray, T>> = emptyList(),
) : SyncStreamsStorage<T>(id = id) {
    private val stream = ByteArrayOutputStream().also { stream ->
        stream.write("${defaultDeleted.joinToString(separator = "") { it.toString() }}\n0".toByteArray())
    }

    override fun now(): Duration {
        return now
    }

    override fun randomUUID(): UUID {
        return randomUUID
    }

    override fun hash(bytes: ByteArray): String {
        return hashes.firstOrNull { (key, _) -> key.contentEquals(bytes) }?.second ?: error("No hash!")
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
