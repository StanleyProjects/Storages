package sp.kx.storages

import java.util.Objects

class Payload<T : Any>(
    val value: T,
    val valueInfo: ValueInfo,
    val valueState: ValueState,
) {
    override fun toString(): String {
        return "Payload(value: ${value::class.java.simpleName}, valueInfo: $valueInfo, valueState: $valueState)"
    }

    companion object {
        fun hashCode(payload: Payload<ByteArray>): Int {
            return Objects.hash(
                payload.value.contentHashCode(),
                payload.valueInfo,
                payload.valueState,
            )
        }
    }
}
