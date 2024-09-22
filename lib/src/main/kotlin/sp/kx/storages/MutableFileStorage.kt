package sp.kx.storages

import java.util.UUID

interface MutableFileStorage : FileStorage {
    fun delete(id: UUID): Boolean
    fun add(bytes: ByteArray): Raw
    fun update(id: UUID, bytes: ByteArray): ItemInfo?
}
