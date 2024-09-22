package sp.kx.storages

import java.util.UUID
import kotlin.time.Duration

class Raw(
    val id: UUID,
    val info: ItemInfo,
) {
    fun copy(
        updated: Duration,
        hash: ByteArray,
    ): Raw {
        return Raw(
            id = id,
            info = info.copy(updated = updated, hash = hash),
        )
    }
}
