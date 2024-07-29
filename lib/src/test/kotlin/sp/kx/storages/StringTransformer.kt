package sp.kx.storages

internal object StringTransformer : Transformer<String> {
    override fun encode(decoded: String): ByteArray {
        return decoded.toByteArray()
    }

    override fun decode(encoded: ByteArray): String {
        return String(encoded)
    }
}
