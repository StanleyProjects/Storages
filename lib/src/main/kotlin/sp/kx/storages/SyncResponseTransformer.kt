package sp.kx.storages

import sp.kx.bytes.readInt
import sp.kx.bytes.write
import java.util.HashMap
import java.util.UUID

class SyncResponseTransformer(
    private val hf: HashFunction,
) : Transformer<SyncResponse> {
    override fun encode(decoded: SyncResponse): ByteArray {
        val encoded = ByteArray(hf.size * 2 + 4 + decoded.infos.size)
        System.arraycopy(decoded.session.src, 0, encoded, 0, hf.size)
        System.arraycopy(decoded.session.dst, 0, encoded, hf.size, hf.size)
        encoded.write(index = hf.size * 2, value = decoded.infos.size)
        decoded.infos.entries.forEachIndexed { index, (id, info) ->
            encoded.write(index = hf.size * 2 + 4 + index * 16, value = id)
            // todo
        }
        return encoded
    }

    private fun ByteArray.readSyncSession(index: Int = 0, hf: HashFunction): SyncSession {
        val src = ByteArray(hf.size)
        System.arraycopy(this, index, src, 0, hf.size)
        val dst = ByteArray(hf.size)
        System.arraycopy(this, index + hf.size, dst, 0, hf.size)
        return SyncSession(
            src = src,
            dst = dst,
        )
    }

    override fun decode(encoded: ByteArray): SyncResponse {
        val session = encoded.readSyncSession(hf = hf)
        val infos = HashMap<UUID, SyncInfo>(encoded.readInt(index = hf.size * 2))
        // todo
        return SyncResponse(
            session = session,
            infos = infos,
        )
    }
}
