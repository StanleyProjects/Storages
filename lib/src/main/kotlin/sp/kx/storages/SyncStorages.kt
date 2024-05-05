package sp.kx.storages

import java.util.UUID

class SyncStorages private constructor(
    private val map: Map<Class<out Any>, SyncStorage<out Any>>,
) {
    class Builder {
        private val list = mutableListOf<Pair<Class<out Any>, SyncStorage<out Any>>>()

        fun <T : Any> add(storage: SyncStorage<T>, type: Class<T>): Builder {
            list.add(type to storage)
            return this
        }

        inline fun <reified T : Any> add(storage: SyncStorage<T>): Builder {
            return add(storage, T::class.java)
        }

        fun build(): SyncStorages {
            if (list.isEmpty()) error("Empty storages!")
            for (i in list.indices) {
                val (type, storage) = list[i]
                for (j in list.indices) {
                    if (i == j) continue
                    val pair = list[j]
                    if (type == pair.first) error("Type \"${type.name}\" is repeated!")
                    if (storage.id == pair.second.id) error("ID \"${storage.id}\" is repeated!")
                }
            }
            return SyncStorages(list.toMap())
        }
    }

    operator fun get(id: UUID): SyncStorage<out Any>? {
        return map.values.firstOrNull { it.id == id }
    }

    fun require(id: UUID): SyncStorage<out Any> {
        return map.values.firstOrNull { it.id == id } ?: error("No storage by ID: \"$id\"!")
    }

    fun <T : Any> get(type: Class<T>): SyncStorage<T>? {
        val entry = map.entries.firstOrNull { it.key == type } ?: return null
        return entry.value as SyncStorage<T>
    }

    inline fun <reified T : Any> get(): SyncStorage<T>? {
        return get(T::class.java)
    }

    inline fun <reified T : Any> require(): SyncStorage<T> {
        return get(T::class.java) ?: error("No storage by type: \"${T::class.java.name}\"!")
    }

    fun hashes(): Map<UUID, String> {
        TODO("SyncStorages:hashes")
    }

    companion object {
        fun <T : Any> create(storage: SyncStorage<T>, type: Class<T>): SyncStorages {
            return SyncStorages(map = mapOf(type to storage))
        }

        inline fun <reified T : Any> create(storage: SyncStorage<T>): SyncStorages {
            return create(storage, T::class.java)
        }
    }
}
