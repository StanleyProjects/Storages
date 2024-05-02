package sp.kx.storages

import java.util.UUID

interface SyncStorage<T : Any> : MutableStorage<T> {
    fun merge(info: MergeInfo): List<Described<ByteArray>>
    fun merge(
        items: List<Described<ByteArray>>,
        deleted: Set<UUID>,
    ) // todo commit
    fun getSyncInfo(): SyncInfo
    fun getMergeInfo(info: SyncInfo): MergeInfo
}
