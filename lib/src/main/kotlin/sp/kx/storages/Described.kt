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
    val payload: T,
) {
    fun copy(
        updated: Duration,
        hash: ByteArray,
        payload: T,
    ): Described<T> {
        return Described(
            id = id,
            info = info.copy(updated = updated, hash = hash),
            payload = payload,
        )
    }

    fun <U : Any> map(transform: (T) -> U): Described<U> {
        return Described(
            id = id,
            info = info,
            payload = transform(payload),
        )
    }

    override fun toString(): String {
        return "{id: $id, info: $info, payload: ${payload::class.java.name}}"
    }

    @Suppress("ReturnCount")
    override fun equals(other: Any?): Boolean {
        if (other !is Described<*>) return false
        if (other.id != id || other.info != info) return false
        if (payload is ByteArray) {
            return other.payload is ByteArray && payload.contentEquals(other.payload)
        }
        return other.payload == payload
    }

    override fun hashCode(): Int {
        return Objects.hash(
            id,
            info,
            if (payload is ByteArray) payload.contentHashCode() else payload,
        )
    }
}
