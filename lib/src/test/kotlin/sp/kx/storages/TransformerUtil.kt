package sp.kx.storages

internal fun <T : Any> Transformer<T>.hashPair(payload: Payload<T>): Pair<ByteArray, ByteArray> {
    return encode(payload.value) to payload.meta.info.hash
}
