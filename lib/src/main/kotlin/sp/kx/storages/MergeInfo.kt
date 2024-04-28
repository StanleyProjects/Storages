package sp.kx.storages

import java.util.UUID

data class MergeInfo(
    val download: Set<UUID>,
    val items: List<Described<ByteArray>>,
    val deleted: Set<UUID>,
)
