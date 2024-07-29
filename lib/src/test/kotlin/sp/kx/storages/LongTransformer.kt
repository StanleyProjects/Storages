package sp.kx.storages

internal object LongTransformer : Transformer<Long> {
    override fun encode(decoded: Long): ByteArray {
        val byte = decoded.toByte()
        if (decoded != byte.toLong()) TODO()
        return byteArrayOf(byte)
    }

    override fun decode(encoded: ByteArray): Long {
        return encoded[0].toLong()
    }
}
