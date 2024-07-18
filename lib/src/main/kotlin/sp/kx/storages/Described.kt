package sp.kx.storages

import java.util.Objects
import java.util.UUID
import kotlin.time.Duration

/**
 * A wrapper for storing data along with technical information about that data.
 *
 * @author [Stanley Wintergreen](https://github.com/kepocnhh)
 * @since 0.4.1
 */
class Described<T : Any>(
    val id: UUID,
    val info: ItemInfo,
    val item: T,
) {
    fun copy(
        updated: Duration,
        hash: ByteArray,
        item: T,
    ): Described<T> {
        return Described(
            id = id,
            info = info.copy(updated = updated, hash = hash),
            item = item,
        )
    }

    fun <U : Any> map(transform: (T) -> U): Described<U> {
        return Described(
            id = id,
            info = info,
            item = transform(item),
        )
    }

    override fun toString(): String {
        return "{id: $id, info: $info, item: ${item::class.java.name}}"
    }

    @Suppress("ReturnCount")
    override fun equals(other: Any?): Boolean {
        if (other !is Described<*>) return false
        if (other.id != id || other.info != info) return false
        if (item is ByteArray) {
            return other.item is ByteArray && item.contentEquals(other.item)
        }
        return other.item == item
    }

    override fun hashCode(): Int {
        return Objects.hash(
            id,
            info,
            if (item is ByteArray) item.contentHashCode() else item,
        )
    }
}
