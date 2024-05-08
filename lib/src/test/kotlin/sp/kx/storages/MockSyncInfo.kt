package sp.kx.storages

import java.util.UUID

internal fun mockSyncInfo(
    infos: Map<UUID, ItemInfo> = emptyMap(),
    deleted: Set<UUID> = emptySet(),
): SyncInfo {
    return SyncInfo(
        infos = infos,
        deleted = deleted,
    )
}
