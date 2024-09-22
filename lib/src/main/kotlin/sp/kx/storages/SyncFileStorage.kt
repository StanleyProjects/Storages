package sp.kx.storages

interface SyncFileStorage : MutableFileStorage {
    fun getSyncInfo(): SyncInfo
}
