package sp.kx.storages

interface HashFunction {
    val size: Int
    fun map(bytes: ByteArray): ByteArray
}
