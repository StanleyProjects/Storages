package sp.kx.storages

import sp.kx.bytes.hex
import java.util.Objects

class SyncSession(
    val src: ByteArray,
    val dst: ByteArray,
) {
    override fun toString(): String {
        return "SyncSession(src: ${src.hex()}, dst: ${dst.hex()})"
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is SyncSession -> src.contentEquals(other.src) && dst.contentEquals(other.dst)
            else -> false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(
            src.contentHashCode(),
            dst.contentHashCode(),
        )
    }
}
