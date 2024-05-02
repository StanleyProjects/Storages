package sp.kx.storages

import java.util.UUID

data class SyncInfo(
    val meta: Map<UUID, ItemInfo>,
    val deleted: Set<UUID>,
)
