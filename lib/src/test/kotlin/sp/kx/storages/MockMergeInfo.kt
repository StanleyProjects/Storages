package sp.kx.storages

import java.util.UUID

internal fun mockMergeInfo(
    downloaded: Set<UUID> = emptySet(),
    items: List<Described<ByteArray>> = emptyList(),
    deleted: Set<UUID> = emptySet(),
): MergeInfo {
    return MergeInfo(
        downloaded = downloaded,
        items = items,
        deleted = deleted,
    )
}
