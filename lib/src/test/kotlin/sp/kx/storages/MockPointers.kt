package sp.kx.storages

import java.util.UUID

internal class MockPointers(
    private val values: MutableMap<UUID, Int> = mutableMapOf(),
) : SyncStreamsStorages.Pointers {
    override fun get(id: UUID): Int {
        return values[id] ?: 0
    }

    override fun putAll(values: Map<UUID, Int>) {
        this.values.putAll(values)
    }
}
