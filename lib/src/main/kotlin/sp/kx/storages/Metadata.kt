package sp.kx.storages

import java.util.Date
import java.util.Objects
import java.util.UUID
import kotlin.time.Duration

class Metadata(
    val id: UUID,
    val created: Duration,
    val info: ItemInfo,
) {
    fun copy(
        updated: Duration,
        hash: ByteArray,
    ): Metadata {
        return Metadata(
            id = id,
            created = created,
            info = info.copy(updated = updated, hash = hash),
        )
    }

    override fun toString(): String {
        return "{" +
            "id: $id, " +
            "created: ${Date(created.inWholeMilliseconds)}, " +
            "info: $info" +
            "}"
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Metadata -> id == other.id && created == other.created && info == other.info
            else -> false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(
            id,
            created,
            info,
        )
    }
}
