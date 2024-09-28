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
            return map("${bytes.contentHashCode()}")
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

        fun hashOf(id: UUID, decoded: String): ByteArray {
            val encoded = map(decoded)
            val bytes = ByteArray(16 + encoded.size)
            bytes.write(value = id)
            System.arraycopy(encoded, 0, bytes, 16, encoded.size)
            return map("${bytes.contentHashCode()}")
        }

        fun hash(list: List<Payload<out Any>>): ByteArray {
            return list.flatMap {
                bytesOf(id = it.meta.id, updated = it.meta.info.updated, encoded = it.meta.info.hash).toList()
            }.toByteArray()
        }

        fun hashes(
            first: Pair<List<Payload<out Any>>, String>,
            vararg other: Pair<List<Payload<out Any>>, String>,
        ): List<Pair<ByteArray, ByteArray>> {
            return other.map { (item, hash) ->
                hash(item) to map(hash)
            } + (hash(first.first) to map(first.second))
        }
    }
}
