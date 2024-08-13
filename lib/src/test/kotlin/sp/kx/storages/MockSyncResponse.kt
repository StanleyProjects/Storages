package sp.kx.storages

import java.util.UUID

internal fun mockSyncResponse(
    session: SyncSession = mockSyncSession(),
    infos: Map<UUID, SyncInfo> = emptyMap(),
): SyncResponse {
    return SyncResponse(
        session = session,
        infos = infos,
    )
}
