package sp.kx.storages

import java.util.UUID

internal fun mockMergeInfo(
    download: Set<UUID> = emptySet(),
    items: List<Described<ByteArray>> = emptyList(),
    deleted: Set<UUID> = emptySet(),
): MergeInfo {
    return MergeInfo(
        download = download,
        items = items,
        deleted = deleted,
    )
}
