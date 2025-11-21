package sp.kx.storages

interface MutableStorages : Storages {
    override operator fun <T : Any> get(key: Storage.Key<T>): MutableStorage<T>?

    fun commit(transaction: Transaction)
}
