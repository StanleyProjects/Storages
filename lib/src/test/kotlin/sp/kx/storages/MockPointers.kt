package sp.kx.storages

import java.util.UUID

internal class MockPointers : SyncStreamsStorages.Pointers {
    override fun getAll(): Map<UUID, Long> {
        TODO("getAll")
    }

    override fun putAll(values: Map<UUID, Long>) {
        TODO("putAll")
    }
}
