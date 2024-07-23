package sp.kx.storages

import java.util.UUID

internal class MockPointers(
    private val values: MutableMap<UUID, Long> = mutableMapOf(),
) : SyncStreamsStorages.Pointers {
    override fun getAll(): Map<UUID, Long> {
        return values
    }

    override fun putAll(values: Map<UUID, Long>) {
        this.values.putAll(values)
    }
}
