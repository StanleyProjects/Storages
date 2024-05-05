package sp.kx.storages

import java.util.UUID

class Storages private constructor(
    private val map: Map<Class<out Any>, Storage<out Any>>,
) {
    class Builder {
        private val list = mutableListOf<Pair<Class<out Any>, Storage<out Any>>>()

        fun <T : Any> add(storage: Storage<T>, type: Class<T>): Builder {
            list.add(type to storage)
            return this
        }

        inline fun <reified T : Any> add(storage: Storage<T>): Builder {
            return add(storage, T::class.java)
        }

        fun build(): Storages {
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
            return Storages(list.toMap())
        }
    }

    operator fun get(id: UUID): Storage<out Any>? {
        return map.values.firstOrNull { it.id == id }
    }

    fun <T : Any> get(type: Class<T>): Storage<T>? {
        val entry = map.entries.firstOrNull { it.key == type } ?: return null
        return entry.value as Storage<T>
    }

    inline fun <reified T : Any> get(): Storage<T>? {
        return get(T::class.java)
    }

    companion object {
        fun <T : Any> create(storage: Storage<T>, type: Class<T>): Storages {
            return Storages(map = mapOf(type to storage))
        }

        inline fun <reified T : Any> create(storage: Storage<T>): Storages {
            return create(storage, T::class.java)
        }
    }
}
