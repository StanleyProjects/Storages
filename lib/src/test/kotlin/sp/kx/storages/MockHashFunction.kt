package sp.kx.storages

import sp.kx.bytes.write
import java.util.UUID
import kotlin.math.absoluteValue
import kotlin.time.Duration

internal class MockHashFunction(
    private val hashes: List<Pair<ByteArray, ByteArray>> = emptyList(),
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

        fun bytesOf(id: UUID, updated: Duration, encoded: ByteArray): ByteArray {
            val idBytes = ByteArray(16)
            idBytes.write(value = id)
            val updatedBytes = ByteArray(8)
            updatedBytes.write(value = updated.inWholeMilliseconds)
            return idBytes + updatedBytes + encoded
        }

        fun hash(list: List<Described<out Any>>): ByteArray {
            return list.flatMap {
                bytesOf(id = it.id, updated = it.info.updated, encoded = it.info.hash).toList()
            }.toByteArray()
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
