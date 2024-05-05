package sp.kx.storages

interface SyncStorage<T : Any> : MutableStorage<T> {
    // todo deleted
    fun merge(info: MergeInfo): CommitInfo
    fun merge(info: CommitInfo)
    fun getSyncInfo(): SyncInfo
    fun getMergeInfo(info: SyncInfo): MergeInfo
}
