package sp.kx.storages

import java.util.UUID

internal class MockStreamerProvider : SyncStreamsStorages.StreamerProvider {
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

internal fun mockStreamerProvider(
    streamers: Map<UUID, Streamer> = emptyMap(),
    values: Map<UUID, Int> = emptyMap(),
) : SyncStreamsStorages.StreamerProvider {
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
