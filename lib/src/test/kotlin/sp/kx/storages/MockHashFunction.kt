package sp.kx.storages

internal class MockHashFunction(
    private val hashes: List<Pair<ByteArray, ByteArray>>,
) : HashFunction {
    override val size = _size

    override fun map(bytes: ByteArray): ByteArray {
        return hashes.firstOrNull { (key, _) -> key.contentEquals(bytes) }?.second ?: error("No hash!\n---\n${String(bytes)}\n---")
    }

    companion object {
        private const val _size = 16

        fun map(value: String): ByteArray {
            return String.format("%0${_size}d", value.hashCode()).toByteArray()
        }

        fun hash(list: List<Described<out Any>>): ByteArray {
            return list.flatMap { it.info.hash.toList() }.toByteArray()
        }

        fun hashes(
            first: Pair<List<Described<out Any>>, String>,
            vararg other: Pair<List<Described<out Any>>, String>,
        ): List<Pair<ByteArray, ByteArray>> {
            return other.map { (item, hash) ->
                hash(item) to map(hash)
            } + (hash(first.first) to map(first.second))
        }
    }
}
