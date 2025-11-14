package sp.kx.storages

import java.util.Objects
import java.util.UUID
import kotlin.time.Duration

class Payload<out T : Any>(
    val id: UUID,
    val created: Duration,
    val updated: Duration,
    val value: T,
) {
    override fun toString(): String {
        return "Payload(id: $id, created: $created, updated: $updated, value: ${value::class.java.simpleName})"
    }

    companion object {
        fun hashCode(payload: Payload<ByteArray>): Int {
            return Objects.hash(
                payload.id,
                payload.created,
                payload.updated,
                payload.value.contentHashCode(),
            )
        }

        fun equals(expected: Payload<ByteArray>, actual: Payload<ByteArray>): Boolean {
            return expected.id == actual.id &&
                expected.created == actual.created &&
                expected.updated == actual.updated &&
                expected.value.contentEquals(actual.value)
        }
    }
}
