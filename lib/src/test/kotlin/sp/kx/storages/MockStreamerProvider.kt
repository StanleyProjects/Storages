package sp.kx.storages

import java.util.UUID

internal class MockStreamerProvider(ids: Set<UUID>) : SyncStreamsStorages.StreamerProvider {
    override fun getStreamer(id: UUID, inputPointer: Int, outputPointer: Int): Streamer {
        TODO("MockStreamerProvider:getStreamer($id, $inputPointer, $outputPointer)")
    }

    override fun getPointer(id: UUID): Int {
        TODO("MockStreamerProvider:getPointer($id)")
    }

    override fun putPointers(values: Map<UUID, Int>) {
        TODO("MockStreamerProvider:putPointers($values)")
    }
}

internal fun getStreamerProvider(streamers: Map<UUID, Streamer>): (Set<UUID>) -> SyncStreamsStorages.StreamerProvider {
    return { ids ->
        check(ids.sorted() == streamers.keys.sorted())
        mockStreamerProvider(streamers = streamers)
    }
}

internal fun mockStreamerProvider(id: UUID, streamer: Streamer): SyncStreamsStorages.StreamerProvider {
    return mockStreamerProvider(streamers = mapOf(id to streamer))
}

internal fun mockStreamerProvider(
    streamers: Map<UUID, Streamer> = emptyMap(),
    values: Map<UUID, Int> = streamers.mapValues { 0 },
): SyncStreamsStorages.StreamerProvider {
    return object : SyncStreamsStorages.StreamerProvider {
        private val values = values.toMutableMap()

        override fun getStreamer(id: UUID, inputPointer: Int, outputPointer: Int): Streamer {
            return streamers[id] ?: TODO()
        }

        override fun getPointer(id: UUID): Int {
            return values[id] ?: TODO()
        }

        override fun putPointers(values: Map<UUID, Int>) {
            this.values.putAll(values)
        }
    }
}
