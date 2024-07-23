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
            if (transformers.containsKey(id)) TODO()
            transformers[id] = type to transformer
            return this
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
            inputPointer: Long,
            outputPointer: Long,
        ): Streamer
    }

    interface Pointers {
        fun getAll(): Map<UUID, Long>
        fun putAll(values: Map<UUID, Long>)
    }

    private fun getStreamer(id: UUID): Streamer {
        val pointer = pointers.getAll()[id] ?: 0
        return streamerProvider.get(id = id, inputPointer = pointer, outputPointer = pointer)
    }

    private fun getStreamer(id: UUID, outputPointer: Long): Streamer {
        val inputPointer = pointers.getAll()[id] ?: 0
        return streamerProvider.get(id = id, inputPointer = inputPointer, outputPointer = outputPointer)
    }

    fun get(id: UUID): MutableStorage<out Any>? {
        val (_, transformer) = transformers[id] ?: return null
        return getSyncStorage(id = id, streamer = getStreamer(id = id), transformer = transformer)
    }

    fun <T : Any> get(type: Class<T>): MutableStorage<T>? {
        for ((id, value) in transformers) {
            if (type == value.first) {
                val transformer = value.second as Transformer<T>
                return getSyncStorage(id = id, streamer = getStreamer(id = id), transformer = transformer)
            }
        }
        return null
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

    fun merge(infos: Map<UUID, MergeInfo>): Map<UUID, CommitInfo> {
        val newPointers = mutableMapOf<UUID, Long>()
        val outputPointer = System.currentTimeMillis()
        val result = infos.mapValues { (id, info) ->
            val (_, transformer) = transformers[id] ?: TODO()
            val storage = getSyncStorage(
                id = id,
                streamer = getStreamer(id = id, outputPointer = outputPointer),
                transformer = transformer as Transformer<Any>,
            )
            newPointers[id] = outputPointer
            storage.merge(info)
        }
        pointers.putAll(newPointers)
        return result
    }

    fun commit(infos: Map<UUID, CommitInfo>) {
        val newPointers = mutableMapOf<UUID, Long>()
        val outputPointer = System.currentTimeMillis()
        for ((id, info) in infos) {
            val (_, transformer) = transformers[id] ?: TODO()
            val storage = getSyncStorage(
                id = id,
                streamer = getStreamer(id = id, outputPointer = outputPointer),
                transformer = transformer as Transformer<Any>,
            )
            newPointers[id] = outputPointer
            storage.commit(info)
        }
        pointers.putAll(newPointers)
    }
}
