package sp.kx.storages

import java.util.UUID

interface Storages {
    operator fun get(id: UUID): Storage<out Any>?
    fun <T : Any> get(type: Class<T>): Storage<T>?
}

/*
fun Storages.require(id: UUID): Storage<out Any> {
    return get(id = id) ?: error("No storage by ID: \"$id\"!")
}

inline fun <reified T : Any> Storages.get(): Storage<T>? {
    return get(T::class.java)
}

inline fun <reified T : Any> Storages.require(): Storage<T> {
    return get(T::class.java) ?: error("No storage by type: \"${T::class.java.name}\"!")
}
*/
