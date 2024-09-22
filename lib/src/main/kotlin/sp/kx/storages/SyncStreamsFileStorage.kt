package sp.kx.storages

import java.util.UUID

class SyncStreamsFileStorage(
    override val id: UUID,
) : SyncFileStorage {
    override fun getSyncInfo(): SyncInfo {
        TODO("SyncStreamsFileStorage:getSyncInfo")
    }

    override fun delete(id: UUID): Boolean {
        TODO("SyncStreamsFileStorage:delete")
    }

    override fun add(bytes: ByteArray): Raw {
        TODO("SyncStreamsFileStorage:add")
    }

    override fun update(id: UUID, bytes: ByteArray): ItemInfo? {
        TODO("SyncStreamsFileStorage:update")
    }

    override val hash: ByteArray
        get() = TODO("SyncStreamsFileStorage:hash")
    override val items: List<Raw>
        get() = TODO("SyncStreamsFileStorage:items")

    override fun getBytes(id: UUID): ByteArray {
        TODO("SyncStreamsFileStorage:getBytes")
    }
}
