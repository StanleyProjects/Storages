package sp.kx.storages

interface MutableStorages : Storages {
    override operator fun <T : Any> get(type: Class<T>): MutableStorage<T>?
}
