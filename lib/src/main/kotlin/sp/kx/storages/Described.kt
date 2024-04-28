package sp.kx.storages

import java.util.Objects
import java.util.UUID

class Described<T : Any>(
    val id: UUID,
    val info: ItemInfo,
    val item: T,
) {
    override fun toString(): String {
        return "{id: $id, info: $info, item: ${item::class.java}}"
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
