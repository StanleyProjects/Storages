package sp.kx.storages

import java.util.Objects
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

    override fun toString(): String {
        return "{id: $id, info: $info}"
    }

    @Suppress("ReturnCount")
    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Raw -> other.id == id && other.info == info
            else -> false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(
            id,
            info,
        )
    }
}
