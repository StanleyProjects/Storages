package sp.kx.storages

import java.util.UUID

interface MutableFileStorage : FileStorage {
    fun delete(id: UUID): Boolean
    fun add(bytes: ByteArray): Metadata
    fun update(id: UUID, bytes: ByteArray): ItemInfo?
}
