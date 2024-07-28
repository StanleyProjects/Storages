package sp.kx.storages

import java.util.UUID

class SyncStreamsStorages private constructor(
    private val hf: HashFunction,
    private val pointers: Pointers,
    private val transformers: Map<UUID, Pair<Class<*>, Transformer<*>>>,
    private val env: SyncStreamsStorage.Environment,
    private val streamerProvider: StreamerProvider,
) {
    class Builder {
        private val transformers = mutableMapOf<UUID, Pair<Class<*>, Transformer<*>>>()

        fun <T : Any> add(id: UUID, type: Class<T>, transformer: Transformer<T>): Builder {
            if (transformers.containsKey(id)) error("ID \"$id\" is repeated!")
            transformers[id] = type to transformer
            return this
        }

        inline fun <reified T : Any> add(id: UUID, transformer: Transformer<T>): Builder {
            return add(id = id, type = T::class.java, transformer = transformer)
        }

        fun build(
            hf: HashFunction,
            pointers: Pointers,
            env: SyncStreamsStorage.Environment,
            streamerProvider: StreamerProvider,
        ): SyncStreamsStorages {
            if (transformers.isEmpty()) error("Empty storages!")
            return SyncStreamsStorages(
                hf = hf,
                pointers = pointers,
                env = env,
                streamerProvider = streamerProvider,
                transformers = transformers,
            )
        }
    }

    interface StreamerProvider {
        fun get(
            id: UUID,
            inputPointer: Int,
            outputPointer: Int,
        ): Streamer
    }

    interface Pointers {
        fun get(id: UUID): Int
        fun putAll(values: Map<UUID, Int>)
    }

    fun get(id: UUID): MutableStorage<out Any>? {
        val (_, transformer) = transformers[id] ?: return null
        val pointer = pointers.get(id)
        val streamer = streamerProvider.get(id = id, inputPointer = pointer, outputPointer = pointer)
        return getSyncStorage(
            id = id,
            streamer = streamer,
            transformer = transformer,
        )
    }

    fun require(id: UUID): MutableStorage<out Any> {
        return get(id = id) ?: error("No storage by ID: \"$id\"!")
    }

    fun <T : Any> get(type: Class<T>): MutableStorage<T>? {
        for ((id, value) in transformers) {
            if (type != value.first) continue
            val transformer = value.second as Transformer<T>
            val pointer = pointers.get(id)
            val streamer = streamerProvider.get(id = id, inputPointer = pointer, outputPointer = pointer)
            return getSyncStorage(
                id = id,
                streamer = streamer,
                transformer = transformer,
            )
        }
        return null
    }

    inline fun <reified T : Any> get(): MutableStorage<T>? {
        return get(T::class.java)
    }

    inline fun <reified T : Any> require(): MutableStorage<T> {
        return get(T::class.java) ?: error("No storage by type: \"${T::class.java.name}\"!")
    }

    private fun <T : Any> getSyncStorage(id: UUID, streamer: Streamer, transformer: Transformer<T>): SyncStorage<T> {
        return SyncStreamsStorage(
            id = id,
            hf = hf,
            streamer = streamer,
            transformer = transformer,
            env = env,
        )
    }

    fun hashes(): Map<UUID, ByteArray> {
        return transformers.mapValues { (id, value) ->
            val (_, transformer) = value
            val pointer = pointers.get(id)
            val streamer = streamerProvider.get(id = id, inputPointer = pointer, outputPointer = pointer)
            getSyncStorage(id, streamer, transformer).hash
        }
    }

    fun getSyncInfo(hashes: Map<UUID, ByteArray>): Map<UUID, SyncInfo> {
        val result = mutableMapOf<UUID, SyncInfo>()
        for ((id, hash) in hashes) {
            val (_, transformer) = transformers[id] ?: continue
            val pointer = pointers.get(id)
            val streamer = streamerProvider.get(id = id, inputPointer = pointer, outputPointer = pointer)
            val storage = getSyncStorage(id, streamer, transformer)
            if (storage.hash.contentEquals(hash)) continue
            result[id] = storage.getSyncInfo()
        }
        return result
    }

    fun getMergeInfo(infos: Map<UUID, SyncInfo>): Map<UUID, MergeInfo> {
        return infos.mapValues { (id, info) ->
            val (_, transformer) = transformers[id] ?: error("No storage by ID: \"$id\"!")
            val pointer = pointers.get(id)
            val streamer = streamerProvider.get(id = id, inputPointer = pointer, outputPointer = pointer)
            val storage = getSyncStorage(id, streamer, transformer)
            storage.getMergeInfo(info)
        }
    }

    fun merge(infos: Map<UUID, MergeInfo>): Map<UUID, CommitInfo> {
        val newPointers = mutableMapOf<UUID, Int>()
        val result = infos.mapValues { (id, info) ->
            val (_, transformer) = transformers[id] ?: TODO()
            val inputPointer = pointers.get(id)
            val outputPointer = inputPointer + 1
            val storage = getSyncStorage(
                id = id,
                streamer = streamerProvider.get(id = id, inputPointer = inputPointer, outputPointer = outputPointer),
                transformer = transformer as Transformer<Any>,
            ) // todo SyncStorage only encoded
            newPointers[id] = outputPointer
            storage.merge(info)
        }
        pointers.putAll(newPointers)
        return result
    }

    fun commit(infos: Map<UUID, CommitInfo>) {
        val newPointers = mutableMapOf<UUID, Int>()
        for ((id, info) in infos) {
            val (_, transformer) = transformers[id] ?: TODO()
            val inputPointer = pointers.get(id)
            val outputPointer = inputPointer + 1
            val storage = getSyncStorage(
                id = id,
                streamer = streamerProvider.get(id = id, inputPointer = inputPointer, outputPointer = outputPointer),
                transformer = transformer as Transformer<Any>,
            )
            newPointers[id] = outputPointer
            storage.commit(info)
        }
        pointers.putAll(newPointers)
    }
}
