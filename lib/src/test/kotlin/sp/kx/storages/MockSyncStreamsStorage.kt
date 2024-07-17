package sp.kx.storages

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class MockSyncStreamsStorage<T : Any>(
    private val timeProvider: MockProvider<Duration> = mockProvider { 1.milliseconds },
    private val uuidProvider: MockProvider<UUID> = mockProvider { UUID.fromString("d2d7c21b-f99a-4f78-80d4-8bf05ee25f62") },
    private val transformer: List<Pair<ByteArray, T>> = emptyList(),
    id: UUID = mockUUID(),
    defaultDeleted: Set<UUID> = emptySet(),
    hashes: List<Pair<ByteArray, ByteArray>> = emptyList(),
) : SyncStreamsStorage<T>(
    id = id,
    hf = MockHashFunction(hashes = hashes),
) {
    private val stream = ByteArrayOutputStream().also { stream ->
        BytesUtil.writeBytes(stream, defaultDeleted.size)
        defaultDeleted.forEach {
            BytesUtil.writeBytes(stream, it)
        }
        val itemsSize: Int = 0
        BytesUtil.writeBytes(stream, itemsSize)
    }

    override fun now(): Duration {
        return timeProvider.provide()
    }

    override fun randomUUID(): UUID {
        return uuidProvider.provide()
    }

    override fun encode(item: T): ByteArray {
        return transformer.firstOrNull { (_, value) -> value == item }?.first ?: error("No encoded!")
    }

    override fun decode(bytes: ByteArray): T {
        return transformer.firstOrNull { (key, _) -> key.contentEquals(bytes) }?.second ?: error("No decoded: ${bytes.toHEX()}(${bytes.size})(${String(bytes)})!")
    }

    override fun inputStream(): InputStream {
        return stream.toByteArray().inputStream()
    }

    override fun outputStream(): OutputStream {
        stream.reset()
        return stream
    }
}
