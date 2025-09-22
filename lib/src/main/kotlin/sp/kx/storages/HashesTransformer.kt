package sp.kx.storages

import sp.kx.bytes.Transformer
import sp.kx.bytes.readInt
import sp.kx.bytes.readUUID
import sp.kx.bytes.write
import java.util.HashMap
import java.util.UUID

class HashesTransformer(private val hf: HashFunction) : Transformer<Map<UUID, ByteArray>> {
    override fun encode(decoded: Map<UUID, ByteArray>): ByteArray {
        val encoded = ByteArray(4 + decoded.size * (16 + hf.size))
        encoded.write(value = decoded.size)
        decoded.entries.forEachIndexed { index, (id, hash) ->
            encoded.write(index = 4 + index * (16 + hf.size), value = id)
            System.arraycopy(hash, 0, encoded, 4 + index * (16 + hf.size) + 16, hf.size)
        }
        return encoded
    }

    override fun decode(encoded: ByteArray): Map<UUID, ByteArray> {
        val size = encoded.readInt()
        val result = HashMap<UUID, ByteArray>(size)
        for (index in 0 until size) {
            val id = encoded.readUUID(index = 4 + index * (16 + hf.size))
            val hash = ByteArray(hf.size)
            System.arraycopy(encoded, 4 + index * (16 + hf.size) + 16, hash, 0, hf.size)
            result[id] = hash
        }
        return result
    }
}
