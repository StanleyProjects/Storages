package sp.kx.storages

import java.util.Objects
import kotlin.time.Duration

class ValueState(
    val updated: Duration,
    val hash: ByteArray,
) {
    override fun toString(): String {
        return "ValueState(updated: ${updated.inWholeMilliseconds}, hash: ${hash.size})"
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is ValueState -> other.updated == updated && other.hash.contentEquals(hash)
            else -> false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(updated, hash.contentHashCode())
    }
}
