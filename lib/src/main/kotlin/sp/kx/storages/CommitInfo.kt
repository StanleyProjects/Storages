package sp.kx.storages

import java.util.UUID

data class CommitInfo(
    val hash: String,
    val items: List<Described<ByteArray>>,
    val deleted: Set<UUID>,
)
