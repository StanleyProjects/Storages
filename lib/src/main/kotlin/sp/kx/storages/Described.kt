package sp.kx.storages

import java.util.Objects
import java.util.UUID
import kotlin.time.Duration

class Described<T : Any>(
    val id: UUID,
    val info: ItemInfo,
    val item: T,
) {
    fun copy(
        updated: Duration,
        hash: String,
        item: T,
    ): Described<T> {
        return Described(
            id = id,
            info = info.copy(updated = updated, hash = hash),
            item = item,
        )
    }

    override fun toString(): String {
        return "{id: $id, info: $info, item: ${item::class.java.name}}"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Described<*>) return false
        if (other.id != id) return false
        if (other.info != info) return false
        if (item is ByteArray) {
            if (other.item !is ByteArray) return false
            return item.contentEquals(other.item)
        }
        return other.item == item
    }

    override fun hashCode(): Int {
        return Objects.hash(
            id,
            info,
            item,
        )
    }
}
