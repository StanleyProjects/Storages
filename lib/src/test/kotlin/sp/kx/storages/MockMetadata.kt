package sp.kx.storages

import java.util.UUID
import kotlin.time.Duration

internal fun mockMetadata(
    id: UUID = mockUUID(pointer = 1),
    created: Duration = mockDuration(pointer = 1),
    info: ItemInfo = mockItemInfo(pointer = 1),
): Metadata {
    return Metadata(
        id = id,
        created = created,
        info = info,
    )
}

internal fun mockMetadata(pointer: Int): Metadata {
    return mockMetadata(
        id = mockUUID(pointer = pointer),
        created = mockDuration(pointer = pointer),
        info = mockItemInfo(pointer = pointer),
    )
}
