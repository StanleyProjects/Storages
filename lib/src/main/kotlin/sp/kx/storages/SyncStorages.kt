package sp.kx.storages

import java.util.UUID

/**
 * A tool for working with synchronized data storages.
 *
 * Usage:
 * ```
 * val storages = SyncStorages.create(FooStorage())
 * val storage = storages.require(UUID.fromString("43518ed6-cda2-48c3-bd28-fed6fab80101"))
 * println("storage: ${storage.id}")
 * ```
 * @author [Stanley Wintergreen](https://github.com/kepocnhh)
 * @since 0.4.1
 */
@Deprecated(message = "sp.kx.storages.SyncStreamsStorages")
class SyncStorages private constructor(
    private val map: Map<Class<out Any>, SyncStorage<out Any>>,
) {
    /**
     * Builder class for creating [SyncStorages] with multiple [SyncStorage]s.
     *
     * Usage:
     * ```
     * val storages = SyncStorages.Builder()
     *     .add(FooStorage())
     *     .add(BarStorage())
     *     .build()
     * val foo = storages.require(UUID.fromString("43518ed6-cda2-48c3-bd28-fed6fab80101"))
     * println("storage: ${foo.id}")
     * val bar = storages.require<Bar>()
     * println("storage: ${bar.id}")
     * ```
     * @author [Stanley Wintergreen](https://github.com/kepocnhh)
     * @since 0.4.1
     */
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

    fun hashes(): Map<UUID, ByteArray> {
        return map.values.associate { it.id to it.hash }
    }

    fun getSyncInfo(hashes: Map<UUID, ByteArray>): Map<UUID, SyncInfo> {
        val result = mutableMapOf<UUID, SyncInfo>()
        for ((id, hash) in hashes) {
            val storage = get(id = id) ?: continue // todo
            if (storage.hash.contentEquals(hash)) continue
            result[id] = storage.getSyncInfo()
        }
        return result
    }

    fun getMergeInfo(infos: Map<UUID, SyncInfo>): Map<UUID, MergeInfo> {
        return infos.mapValues { (id, info) ->
            require(id = id).getMergeInfo(info)
        }
    }

    fun merge(infos: Map<UUID, MergeInfo>): Map<UUID, CommitInfo> {
        return infos.mapValues { (id, info) ->
            require(id = id).merge(info)
        }
    }

    fun commit(infos: Map<UUID, CommitInfo>) {
        for ((id, info) in infos) {
            val storage = get(id = id) ?: continue // todo
            storage.commit(info)
        }
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
