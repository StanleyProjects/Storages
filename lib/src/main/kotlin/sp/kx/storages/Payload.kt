package sp.kx.storages

import java.util.Objects
import kotlin.time.Duration

class Payload<T : Any>(
    val meta: Metadata,
    val value: T,
) {
    fun copy(
        updated: Duration,
        hash: ByteArray,
        value: T,
    ): Payload<T> {
        return Payload(
            meta = meta.copy(updated = updated, hash = hash),
            value = value,
        )
    }

    override fun toString(): String {
        return "{meta: $meta, value: ${value::class.java.name}}"
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Payload<*> -> meta == other.meta && value == other.value
            else -> false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(
            meta,
            value,
        )
    }
}
