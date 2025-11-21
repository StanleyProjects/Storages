package sp.kx.storages

interface Storages {
    operator fun <T : Any> get(key: Storage.Key<T>): Storage<T>?
}
