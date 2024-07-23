package sp.kx.storages

internal object IntTransformer : Transformer<Int> {
    override fun encode(decoded: Int): ByteArray {
        val byte = decoded.toByte()
        if (decoded != byte.toInt()) TODO()
        return byteArrayOf(byte)
    }

    override fun decode(encoded: ByteArray): Int {
        return encoded[0].toInt()
    }
}
