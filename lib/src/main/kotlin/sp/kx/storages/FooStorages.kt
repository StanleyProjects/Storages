package sp.kx.storages

import java.util.UUID
import kotlin.time.Duration

abstract class FooStorages(
    private val hf: HashFunction,
    private val pointers: Pointers,
    private val transformers: Map<UUID, Transformer<*>>,
) {
    interface Pointers {
        fun getAll(): Map<UUID, Long>
        fun putAll(values: Map<UUID, Long>)
    }

    private fun getStreamer(id: UUID): Streamer {
        val pointer = pointers.getAll()[id] ?: 0
        return getStreamer(id = id, inputPointer = pointer, outputPointer = pointer)
    }

    private fun getStreamer(id: UUID, outputPointer: Long): Streamer {
        val inputPointer = pointers.getAll()[id] ?: 0
        return getStreamer(id = id, inputPointer = inputPointer, outputPointer = outputPointer)
    }

    protected abstract fun getStreamer(
        id: UUID,
        inputPointer: Long,
        outputPointer: Long,
    ): Streamer

    protected abstract fun now(): Duration
    protected abstract fun randomUUID(): UUID
    protected abstract fun onPointers(pointers: Map<UUID, Long>)

    fun get(id: UUID): MutableStorage<out Any>? {
        return getSyncStorage(id = id, getStreamer(id = id))
    }

    fun <T : Any> get(type: Class<T>): MutableStorage<T>? {
        for ((id, transformer) in transformers) {
            if (transformer as? Transformer<T> != null) {
                return getSyncStorage(id = id, getStreamer(id = id))
            }
        }
        TODO("${this::class.java.name}:get($type)")
    }

    private fun <T : Any> getSyncStorage(id: UUID, streamer: Streamer): SyncStorage<T>? {
        return object : SyncStreamsStorage<T>(
            id = id,
            hf = hf,
            streamer = streamer,
            transformer = transformers[id] as Transformer<T>,
        ) {
            override fun now(): Duration {
                return this@FooStorages.now()
            }

            override fun randomUUID(): UUID {
                return this@FooStorages.randomUUID()
            }
        }
    }

    fun merge(infos: Map<UUID, MergeInfo>): Map<UUID, CommitInfo> {
        val newPointers = mutableMapOf<UUID, Long>()
        val outputPointer = System.currentTimeMillis()
        val result = infos.mapValues { (id, info) ->
            val storage = getSyncStorage<Any>(
                id = id,
                streamer = getStreamer(id = id, outputPointer = outputPointer),
            ) ?: TODO()
            newPointers[id] = outputPointer
            storage.merge(info)
        }
        pointers.putAll(newPointers)
        onPointers(pointers.getAll())
        return result
    }

    fun commit(infos: Map<UUID, CommitInfo>) {
        val newPointers = mutableMapOf<UUID, Long>()
        val outputPointer = System.currentTimeMillis()
        for ((id, info) in infos) {
            val storage = getSyncStorage<Any>(
                id = id,
                streamer = getStreamer(id = id, outputPointer = outputPointer),
            ) ?: TODO()
            newPointers[id] = outputPointer
            storage.commit(info)
        }
        pointers.putAll(newPointers)
        onPointers(pointers.getAll())
    }
}
