package sp.kx.storages

import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class SyncStorages(
    private val pointers: MutablePointers,
    private val storages: Map<UUID, Transformer<*>>,
    private val hf: HashFunction,
    private val env: SyncStreamsStorage.Environment,
) : MutableStorages {
    init {
        if (storages.isEmpty()) error("Empty storages!")
        val entries = storages.entries.toList()
        for (i in entries.indices) {
            val (id, _) = entries[i]
            for (j in entries.indices) {
                if (i == j) continue
                if (id == entries[j].key) error("ID \"$id\" is repeated!")
            }
        }
    }

    interface Pointers {
        fun get(id: UUID): Int
    }

    interface MutablePointers : Pointers {
        fun putAll(values: Map<UUID, Int>)
    }

    private fun getStreamer(
        id: UUID,
        inputPointer: Int,
        outputPointer: Int,
    ): Streamer {
        return object : Streamer {
            override fun inputStream(): InputStream {
                TODO("inputStream")
            }

            override fun outputStream(): OutputStream {
                TODO("outputStream")
            }
        }
    }

    override fun <T : Any> get(type: Class<T>): SyncStorage<T>? {
        val (id, transformer) = storages.entries.firstOrNull { (_, value) -> value == type } ?: return null
        val pointer = pointers.get(id = id)
        return get(
            id = id,
            transformer = transformer as Transformer<T>,
            inputPointer = pointer,
            outputPointer = pointer,
        )
    }

    private fun <T : Any> get(
        id: UUID,
        transformer: Transformer<T>,
        inputPointer: Int,
        outputPointer: Int,
    ): SyncStorage<T> {
        return SyncStreamsStorage(
            id = id,
            hf = hf,
            env = env,
            streamer = getStreamer(id = id, inputPointer = inputPointer, outputPointer = outputPointer),
            transformer = transformer,
        )
    }

    override fun get(id: UUID): SyncStorage<out Any>? {
        val transformer = storages[id] ?: return null
        val pointer = pointers.get(id = id)
        return get(
            id = id,
            transformer = transformer,
            inputPointer = pointer,
            outputPointer = pointer,
        )
    }

    fun hashes(): Map<UUID, ByteArray> {
        return storages.mapValues { (id, _) ->
            val storage = get(id = id) ?: error("No storage by ID: \"$id\"!")
            storage.hash
        }
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
            val storage = get(id = id) ?: error("No storage by ID: \"$id\"!")
            storage.getMergeInfo(info)
        }
    }

    fun merge(infos: Map<UUID, MergeInfo>): Map<UUID, CommitInfo> {
        val newPointers = mutableMapOf<UUID, Int>()
        val result = infos.mapValues { (id, info) ->
            val transformer = storages[id] ?: error("No storage by ID: \"$id\"!")
            val oldPointer = pointers.get(id = id)
            val newPointer = oldPointer + 1
            newPointers[id] = newPointer
            get(
                id = id,
                transformer = transformer,
                inputPointer = oldPointer,
                outputPointer = newPointer,
            ).merge(info)
        }
        pointers.putAll(newPointers)
        return result
    }

    fun commit(infos: Map<UUID, CommitInfo>) {
        for ((id, info) in infos) {
            val storage = get(id = id) ?: continue // todo
            storage.commit(info)
        }
    }
}

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
class SyncStorages private constructor(
    private val map: Map<Class<out Any>, SyncStorage<out Any>>,
) : Storages {
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

    override operator fun get(id: UUID): SyncStorage<out Any>? {
        return map.values.firstOrNull { it.id == id }
    }

    override fun <T : Any> get(type: Class<T>): SyncStorage<T>? {
        val entry = map.entries.firstOrNull { it.key == type } ?: return null
        return entry.value as SyncStorage<T>
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
