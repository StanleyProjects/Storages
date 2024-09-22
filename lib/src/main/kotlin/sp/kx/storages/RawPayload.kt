package sp.kx.storages

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
}
