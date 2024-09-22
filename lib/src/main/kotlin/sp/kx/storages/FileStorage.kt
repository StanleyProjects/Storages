package sp.kx.storages

import java.util.UUID

interface FileStorage {
    val id: UUID
    val hash: ByteArray
    val items: List<Raw>

    fun getBytes(id: UUID): ByteArray
}
