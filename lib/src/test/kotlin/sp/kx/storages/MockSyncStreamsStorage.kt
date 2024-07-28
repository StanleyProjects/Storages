package sp.kx.storages

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal fun <T : Any> mockSyncStreamsStorage(
    timeProvider: MockProvider<Duration> = MockProvider { 1.milliseconds },
    uuidProvider: MockProvider<UUID> = MockProvider { mockUUID(1) },
    transformer: List<Pair<ByteArray, T>> = emptyList(),
    id: UUID = mockUUID(),
    defaultDeleted: Set<UUID> = emptySet(),
    defaultLocals: Set<UUID> = emptySet(),
    hashes: List<Pair<ByteArray, ByteArray>> = emptyList(),
) = SyncStreamsStorage(
    id = id,
    hf = MockHashFunction(hashes = hashes),
    streamer = object : Streamer {
        private val stream = ByteArrayOutputStream().also { stream ->
            BytesUtil.writeBytes(stream, defaultDeleted.size)
            defaultDeleted.forEach {
                BytesUtil.writeBytes(stream, it)
            }
            BytesUtil.writeBytes(stream, defaultLocals.size)
            defaultLocals.forEach {
                BytesUtil.writeBytes(stream, it)
            }
            val itemsSize: Int = 0
            BytesUtil.writeBytes(stream, itemsSize)
        }

        override fun inputStream(): InputStream {
            return stream.toByteArray().inputStream()
        }

        override fun outputStream(): OutputStream {
            stream.reset()
            return stream
        }
    },
    transformer = object : Transformer<T> {
        override fun encode(decoded: T): ByteArray {
            return transformer.firstOrNull { (_, value) -> value == decoded }?.first ?: error("No encoded!")
        }

        override fun decode(encoded: ByteArray): T {
            return transformer.firstOrNull { (key, _) -> key.contentEquals(encoded) }?.second ?: error("No decoded: ${encoded.toHEX()}(${encoded.size})(${String(encoded)})!")
        }
    },
    env = object : SyncStreamsStorage.Environment {
        override fun now(): Duration {
            return timeProvider.provide()
        }

        override fun randomUUID(): UUID {
            return uuidProvider.provide()
        }
    },
)
