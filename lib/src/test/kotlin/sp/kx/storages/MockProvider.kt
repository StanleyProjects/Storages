package sp.kx.storages

internal fun interface MockProvider<T : Any> {
    fun provide(): T
}
