package sp.kx.storages

internal fun <T : Any> Transformer<T>.hashPair(described: Described<T>): Pair<ByteArray, ByteArray> {
    return MockHashFunction.bytesOf(id = described.id, item = described.item, encode = ::encode) to described.info.hash
}
