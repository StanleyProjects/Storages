package sp.kx.storages

import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class MockSyncStreamsStorages(
    hashes: List<Pair<ByteArray, ByteArray>> = emptyList(),
    transformers: Map<UUID, Transformer<*>> = emptyMap(),
    private val timeProvider: MockProvider<Duration> = MockProvider { 1.milliseconds },
    private val uuidProvider: MockProvider<UUID> = MockProvider { UUID.fromString("d2d7c21b-f99a-4f78-80d4-8bf05ee25f62") },
    private val streamerProvider: (id: UUID, inputPointer: Long, outputPointer: Long) -> Streamer = { id: UUID, inputPointer: Long, outputPointer: Long ->
        TODO("MockSyncStreamsStorages:getStreamer($id, $inputPointer, $outputPointer)")
    },
) : SyncStreamsStorages(
    hf = MockHashFunction(hashes = hashes),
    pointers = MockPointers(),
    transformers = transformers,
    env = object : SyncStreamsStorage.Environment {
        override fun now(): Duration {
            return timeProvider.provide()
        }

        override fun randomUUID(): UUID {
            return uuidProvider.provide()
        }
    },
) {
    override fun getStreamer(id: UUID, inputPointer: Long, outputPointer: Long): Streamer {
        return streamerProvider(id, inputPointer, outputPointer)
    }

    override fun onPointers(pointers: Map<UUID, Long>) {
        TODO("MockSyncStreamsStorages:onPointers($pointers)")
    }
}
