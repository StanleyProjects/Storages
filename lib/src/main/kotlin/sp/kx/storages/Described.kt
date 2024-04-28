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
        return when (other) {
            is Described<*> -> {
                other.id == id && other.info == info && other.item == item
            }
            else -> false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(
            id,
            info,
            item,
        )
    }
}
