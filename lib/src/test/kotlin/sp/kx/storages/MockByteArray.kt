package sp.kx.storages

internal fun mockByteArray(size: Int = 0): ByteArray {
    return ByteArray(size) { index ->
        (size - index).toByte()
    }
}
