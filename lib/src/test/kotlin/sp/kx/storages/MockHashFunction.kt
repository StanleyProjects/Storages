package sp.kx.storages

import java.util.UUID
import kotlin.math.absoluteValue

internal class MockHashFunction(
    private val hashes: List<Pair<ByteArray, ByteArray>>,
) : HashFunction {
    override val size = _size

    override fun map(bytes: ByteArray): ByteArray {
        val hash = hashes.firstOrNull { (key, _) -> key.contentEquals(bytes) }?.second
        if (hash == null) {
            val message = """
                No hash!
                ---
                hashes: ${hashes.map { (key, _) -> String(key)}}
                -
                ${bytes.size}
                -
                ${String(bytes)}
                ---
            """.trimIndent()
            error(message)
        }
        return hash
    }

    companion object {
        private const val _size = 16

        fun map(value: String): ByteArray {
            return String.format("%0${_size}d", value.hashCode().absoluteValue).toByteArray()
        }

        fun <T : Any> hash(id: UUID, item: T, encode: (T) -> ByteArray): ByteArray {
            val encoded = encode(item)
            val bytes = ByteArray(16 + encoded.size)
            BytesUtil.writeBytes(bytes = bytes, index = 0, value = id)
            System.arraycopy(encoded, 0, bytes, 16, encoded.size)
            return bytes
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
