package sp.kx.storages

class Payload<T : Any>(
    val value: T,
    val valueInfo: ValueInfo,
    val valueState: ValueState,
) {
    override fun toString(): String {
        return "Payload(value: ${value::class.java.simpleName}, valueInfo: $valueInfo, valueState: $valueState)"
    }
}
