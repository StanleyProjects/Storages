package sp.kx.storages

import java.util.Objects

class RawPayload(
    val meta: Metadata,
    val bytes: ByteArray,
) {
    fun <T : Any> map(transform: (ByteArray) -> T): Payload<T> {
        return Payload(
            meta = meta,
            value = transform(bytes),
        )
    }

    override fun toString(): String {
        return "{meta: $meta, bytes:size: ${bytes.size}}"
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is RawPayload -> meta == other.meta && bytes.contentEquals(other.bytes)
            else -> false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(
            meta,
            bytes.contentHashCode(),
        )
    }
}
