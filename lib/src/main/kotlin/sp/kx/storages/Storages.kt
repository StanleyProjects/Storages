package sp.kx.storages

interface Storages {
    operator fun <T : Any> get(type: Class<T>): Storage<T>?
}
