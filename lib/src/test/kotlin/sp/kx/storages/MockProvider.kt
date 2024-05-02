package sp.kx.storages

internal interface MockProvider<T : Any> {
    fun provide(): T
}

internal fun <T : Any> mockProvider(producer: () -> T): MockProvider<T> {
    return object : MockProvider<T> {
        override fun provide(): T {
            return producer()
        }
    }
}
