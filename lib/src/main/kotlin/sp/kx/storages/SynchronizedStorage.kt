package sp.kx.storages

interface SynchronizedStorage<T : Any> : MutableStorage<T> {
    fun merge(info: MergeInfo): List<Described<ByteArray>>
    fun getMergeInfo(info: SyncInfo): MergeInfo
}
