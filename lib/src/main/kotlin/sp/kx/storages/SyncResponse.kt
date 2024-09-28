package sp.kx.storages

import java.util.UUID

data class SyncResponse(
    val session: SyncSession,
    val infos: Map<UUID, SyncInfo>,
)
