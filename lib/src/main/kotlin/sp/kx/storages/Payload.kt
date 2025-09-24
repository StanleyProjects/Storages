package sp.kx.storages

class Payload<T : Any>(
    val value: T,
    val valueInfo: ValueInfo,
    val valueState: ValueState,
)
