package sp.kx.storages

import java.util.UUID

internal class MockStreamerProvider : SyncStreamsStorages.StreamerProvider {
    override fun get(id: UUID, inputPointer: Int, outputPointer: Int): Streamer {
        TODO("MockStreamerProvider:get($id, $inputPointer, $outputPointer)")
    }
}

internal fun mockStreamerProvider(
    streamers: Map<UUID, Streamer> = emptyMap(),
) : SyncStreamsStorages.StreamerProvider {
    return object : SyncStreamsStorages.StreamerProvider {
        override fun get(id: UUID, inputPointer: Int, outputPointer: Int): Streamer {
            return streamers[id] ?: TODO()
        }
    }
}
