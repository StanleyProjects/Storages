package sp.kx.storages

import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class MockSyncStreamsStorages(
    hashes: List<Pair<ByteArray, ByteArray>>,
    transformers: Map<UUID, Transformer<*>>,
    private val timeProvider: MockProvider<Duration> = MockProvider { 1.milliseconds },
    private val uuidProvider: MockProvider<UUID> = MockProvider { UUID.fromString("d2d7c21b-f99a-4f78-80d4-8bf05ee25f62") },
) : SyncStreamsStorages(
    hf = MockHashFunction(hashes = hashes),
    pointers = MockPointers(),
    transformers = transformers,
) {
    override fun getStreamer(id: UUID, inputPointer: Long, outputPointer: Long): Streamer {
        TODO("getStreamer")
    }

    override fun now(): Duration {
        return timeProvider.provide()
    }

    override fun randomUUID(): UUID {
        return uuidProvider.provide()
    }

    override fun onPointers(pointers: Map<UUID, Long>) {
        TODO("onPointers")
    }
}
