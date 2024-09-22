package sp.kx.storages

import java.util.UUID

interface FileStorage {
    val id: UUID
    val hash: ByteArray
    val items: List<Metadata>

    fun getBytes(id: UUID): ByteArray
}
