package sp.kx.storages

internal data class Bar(val number: Int)

internal object BarTransformer : Transformer<Bar> {
    override fun encode(decoded: Bar): ByteArray {
        val byte = decoded.number.toByte()
        check(byte.toInt() == decoded.number)
        return byteArrayOf(byte)
    }

    override fun decode(encoded: ByteArray): Bar {
        return Bar(number = encoded[0].toInt())
    }
}
