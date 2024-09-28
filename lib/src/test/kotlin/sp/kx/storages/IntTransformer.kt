package sp.kx.storages

import sp.kx.bytes.readInt
import sp.kx.bytes.toByteArray

internal object IntTransformer : Transformer<Int> {
    override fun encode(decoded: Int): ByteArray {
        return decoded.toByteArray()
    }

    override fun decode(encoded: ByteArray): Int {
        return encoded.readInt()
    }
}
