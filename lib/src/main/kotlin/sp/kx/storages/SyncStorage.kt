package sp.kx.storages

import java.util.UUID

interface SyncStorage<T : Any> : MutableStorage<T> {
    val deleted: Set<UUID>

    fun merge(info: MergeInfo): CommitInfo
    fun merge(info: CommitInfo)
    fun getSyncInfo(): SyncInfo
    fun getMergeInfo(info: SyncInfo): MergeInfo
}
