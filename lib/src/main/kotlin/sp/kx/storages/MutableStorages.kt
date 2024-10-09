package sp.kx.storages

import java.util.UUID

interface MutableStorages : Storages {
    override fun get(id: UUID): MutableStorage<out Any>?
    override fun require(id: UUID): MutableStorage<out Any> {
        return get(id = id) ?: error("No storage by ID: \"$id\"!")
    }
    override fun <T : Any> get(type: Class<T>): MutableStorage<T>?
    fun delete(ids: Map<UUID, Set<UUID>>): Map<UUID, Set<UUID>>
}

inline fun <reified T : Any> MutableStorages.get(): MutableStorage<T>? {
    return get(T::class.java)
}

inline fun <reified T : Any> MutableStorages.require(): MutableStorage<T> {
    return get(T::class.java) ?: error("No storage by type: \"${T::class.java.name}\"!")
}
