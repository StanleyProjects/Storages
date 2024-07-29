package sp.kx.storages

import java.util.UUID

interface Storages {
    fun get(id: UUID): Storage<out Any>?
    fun require(id: UUID): Storage<out Any> {
        return get(id = id) ?: error("No storage by ID: \"$id\"!")
    }
    fun <T : Any> get(type: Class<T>): Storage<T>?
}

inline fun <reified T : Any> Storages.get(): Storage<T>? {
    return get(T::class.java)
}

inline fun <reified T : Any> Storages.require(): Storage<T> {
    return get(T::class.java) ?: error("No storage by type: \"${T::class.java.name}\"!")
}
