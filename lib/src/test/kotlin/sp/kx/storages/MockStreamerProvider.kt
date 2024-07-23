package sp.kx.storages

import java.util.UUID

internal class MockStreamerProvider : SyncStreamsStorages.StreamerProvider {
    override fun get(id: UUID, inputPointer: Long, outputPointer: Long): Streamer {
        TODO("MockStreamerProvider:get($id, $inputPointer, $outputPointer)")
    }
}

internal fun mockStreamerProvider(
    streamers: Map<UUID, Streamer>,
) : SyncStreamsStorages.StreamerProvider {
    return object : SyncStreamsStorages.StreamerProvider {
        override fun get(id: UUID, inputPointer: Long, outputPointer: Long): Streamer {
            return streamers[id] ?: TODO()
        }
    }
}
