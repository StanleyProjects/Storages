package sp.kx.storages

internal fun <T : Any> Transformer<T>.hashPair(described: Described<T>): Pair<ByteArray, ByteArray> {
    return encode(described.payload) to described.info.hash
}
