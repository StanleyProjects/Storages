package sp.kx.storages

import sp.kx.bytes.Transformer
import sp.kx.bytes.readBytes
import sp.kx.bytes.readInt
import sp.kx.bytes.readUUID
import sp.kx.bytes.writeBytes
import sp.kx.hashes.HashFunction
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID

class SyncResponseTransformer(
    private val hf: HashFunction,
) : Transformer<SyncResponse> {
    override fun encode(decoded: SyncResponse): ByteArray {
        val stream = ByteArrayOutputStream()
        stream.write(decoded.session.src)
        stream.write(decoded.session.dst)
        stream.writeBytes(value = decoded.infos.size)
        decoded.infos.forEach { (id, info) ->
            stream.writeBytes(value = id)
            stream.writeBytes(value = info)
        }
        return stream.toByteArray()
    }

    override fun decode(encoded: ByteArray): SyncResponse {
        val stream = ByteArrayInputStream(encoded)
        val session = SyncSession(
            src = stream.readBytes(hf.size),
            dst = stream.readBytes(hf.size),
        )
        val syncInfoSize = stream.readInt()
        val infos = HashMap<UUID, SyncInfo>(syncInfoSize)
        for (i in 0 until syncInfoSize) {
            infos[stream.readUUID()] = stream.readSyncInfo(hf = hf)
        }
        return SyncResponse(
            session = session,
            infos = infos,
        )
    }
}
