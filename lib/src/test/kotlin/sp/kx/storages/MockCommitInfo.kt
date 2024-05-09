package sp.kx.storages

import java.util.UUID

internal fun mockCommitInfo(
    hash: String = "mock:hash",
    items: List<Described<ByteArray>> = emptyList(),
    deleted: Set<UUID> = emptySet(),
): CommitInfo {
    return CommitInfo(
        hash = hash,
        items = items,
        deleted = deleted,
    )
}
