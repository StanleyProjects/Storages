package sp.kx.storages

import sp.kx.bytes.Transformer
import sp.kx.bytes.write
import sp.kx.hashes.HashFunction
import sp.kx.streamers.MutableStreamer
import java.io.File
import java.util.UUID

class SyncStreamsStorages private constructor(
    private val hf: HashFunction,
    private val transformers: Map<UUID, Pair<Class<out Any>, Transformer<out Any>>>,
    private val env: SyncStreamsStorage.Environment,
    private val streamers: StreamerProvider,
    private var session: SyncSession?,
) : MutableStorages {
    class Builder {
        private val transformers = mutableMapOf<UUID, Pair<Class<out Any>, Transformer<out Any>>>()
        private var session: SyncSession?

        constructor() {
            session = null
        }

        internal constructor(session: SyncSession) {
            this.session = session
        }

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
            env: SyncStreamsStorage.Environment,
            getStreamers: (Set<UUID>) -> StreamerProvider,
        ): SyncStreamsStorages {
            if (transformers.isEmpty()) error("Empty storages!")
            return SyncStreamsStorages(
                hf = hf,
                env = env,
                streamers = getStreamers(transformers.keys),
                transformers = transformers,
                session = session,
            )
        }

        fun build(
            hf: HashFunction,
            env: SyncStreamsStorage.Environment,
            dir: File,
        ): SyncStreamsStorages {
            if (transformers.isEmpty()) error("Empty storages!")
            return SyncStreamsStorages(
                hf = hf,
                env = env,
                streamers = FileStreamerProvider(dir = dir, ids = transformers.keys),
                transformers = transformers,
                session = session,
            )
        }
    }

    interface StreamerProvider {
        fun getStreamer(
            id: UUID,
            inputPointer: Int,
            outputPointer: Int,
        ): MutableStreamer
        fun getPointer(id: UUID): Int
        fun putPointers(values: Map<UUID, Int>)
    }

    override fun get(id: UUID): MutableStorage<out Any>? {
        val (_, transformer) = transformers[id] ?: return null
        val pointer = streamers.getPointer(id = id)
        val streamer = streamers.getStreamer(id = id, inputPointer = pointer, outputPointer = pointer)
        return getSyncStorage(
            id = id,
            streamer = streamer,
            transformer = transformer,
        )
    }

    override fun <T : Any> get(type: Class<T>): MutableStorage<T>? {
        for ((id, value) in transformers) {
            if (type != value.first) continue
            val transformer = value.second as Transformer<T>
            val pointer = streamers.getPointer(id = id)
            val streamer = streamers.getStreamer(id = id, inputPointer = pointer, outputPointer = pointer)
            return getSyncStorage(
                id = id,
                streamer = streamer,
                transformer = transformer,
            )
        }
        return null
    }

    private fun <T : Any> getSyncStorage(id: UUID, streamer: MutableStreamer, transformer: Transformer<T>): SyncStorage<T> {
        return SyncStreamsStorage(
            id = id,
            hf = hf,
            streamer = streamer,
            transformer = transformer,
            env = env,
        )
    }

    fun hashes(): Map<UUID, ByteArray> {
        return transformers.mapValues { (id, _) ->
            val pointer = streamers.getPointer(id = id)
            val streamer = streamers.getStreamer(id = id, inputPointer = pointer, outputPointer = pointer)
            SyncStreamsStorage.getHash(streamer = streamer, hf = hf)
        }
    }

    private fun getHash(hashes: Map<UUID, ByteArray>): ByteArray {
        val bytes = ByteArray(hashes.size * (16 + hf.size))
        hashes.entries.sortedBy { (id, _) -> id }.forEachIndexed { index, (id, hash) ->
            bytes.write(index = index * (16 + hf.size), value = id)
            System.arraycopy(hash, 0, bytes, index * (16 + hf.size) + 16, hash.size)
        }
        return hf.map(bytes)
    }

    private fun getHash(ids: Set<UUID>): ByteArray {
        val bytes = ByteArray(ids.size * (16 + hf.size))
        ids.sorted().forEachIndexed { index, id ->
            bytes.write(index = index * (16 + hf.size), value = id)
            val pointer = streamers.getPointer(id = id)
            val streamer = streamers.getStreamer(id = id, inputPointer = pointer, outputPointer = pointer)
            val hash = SyncStreamsStorage.getHash(streamer = streamer, hf = hf)
            System.arraycopy(hash, 0, bytes, index * (16 + hf.size) + 16, hash.size)
        }
        return hf.map(bytes)
    }

    fun getSyncInfo(hashes: Map<UUID, ByteArray>): SyncResponse {
        val ids = transformers.keys
        val session = SyncSession(
            src = getHash(hashes = hashes),
            dst = getHash(ids = ids),
        )
        val result = mutableMapOf<UUID, SyncInfo>()
        for ((id, hash) in hashes) {
            if (!ids.contains(id)) continue
            val pointer = streamers.getPointer(id = id)
            val streamer = streamers.getStreamer(id = id, inputPointer = pointer, outputPointer = pointer)
            if (SyncStreamsStorage.getHash(streamer = streamer, hf = hf).contentEquals(hash)) continue
            result[id] = SyncStreamsStorage.getSyncInfo(streamer = streamer, hf = hf)
        }
        this.session = session
        return SyncResponse(
            session = session,
            infos = result,
        )
    }

    fun getMergeInfo(session: SyncSession, infos: Map<UUID, SyncInfo>): Map<UUID, MergeInfo> {
        val ids = transformers.keys
        if (!session.src.contentEquals(getHash(ids = ids))) error("Session expired!")
        this.session = session
        return infos.mapValues { (id, info) ->
            if (!ids.contains(id)) error("No storage by ID: \"$id\"!")
            val pointer = streamers.getPointer(id = id)
            val streamer = streamers.getStreamer(id = id, inputPointer = pointer, outputPointer = pointer)
            SyncStreamsStorage.getMergeInfo(streamer = streamer, hf = hf, info = info)
        }
    }

    fun merge(session: SyncSession, infos: Map<UUID, MergeInfo>): Map<UUID, CommitInfo> {
        val ids = transformers.keys
        if (!session.dst.contentEquals(getHash(ids = ids))) error("Session expired!")
        val dstSession = this.session ?: error("No session!")
        if (!dstSession.dst.contentEquals(session.dst)) error("Destination session error!")
        if (!dstSession.src.contentEquals(session.src)) error("Source session error!")
        this.session = null
        val newPointers = mutableMapOf<UUID, Int>()
        val result = mutableMapOf<UUID, CommitInfo>()
        for ((id, info) in infos) {
            val (_, transformer) = transformers[id] ?: error("No storage by ID: \"$id\"!")
            val inputPointer = streamers.getPointer(id = id)
            val outputPointer = inputPointer + 1
            val storage = getSyncStorage(
                id = id,
                streamer = streamers.getStreamer(
                    id = id,
                    inputPointer = inputPointer,
                    outputPointer = outputPointer,
                ),
                transformer = transformer,
            )
            result[id] = storage.merge(info)
            newPointers[id] = outputPointer
        }
        streamers.putPointers(newPointers)
        return result
    }

    fun commit(session: SyncSession, infos: Map<UUID, CommitInfo>): Set<UUID> {
        val ids = transformers.keys
        if (!session.src.contentEquals(getHash(ids = ids))) error("Session expired!")
        val srcSession = this.session ?: error("No session!")
        if (!srcSession.dst.contentEquals(session.dst)) TODO()
        if (!srcSession.src.contentEquals(session.src)) TODO()
        this.session = null
        if (infos.isEmpty()) return emptySet()
        val newPointers = mutableMapOf<UUID, Int>()
        for ((id, info) in infos) {
            val (_, transformer) = transformers[id] ?: error("No storage by ID: \"$id\"!")
            val inputPointer = streamers.getPointer(id = id)
            val outputPointer = inputPointer + 1
            val storage = getSyncStorage(
                id = id,
                streamer = streamers.getStreamer(
                    id = id,
                    inputPointer = inputPointer,
                    outputPointer = outputPointer,
                ),
                transformer = transformer,
            )
            if (!storage.commit(info)) continue
            newPointers[id] = outputPointer
        }
        if (newPointers.isNotEmpty()) {
            streamers.putPointers(newPointers)
        }
        return newPointers.keys
    }

    override fun delete(ids: Map<UUID, Set<UUID>>): Map<UUID, Set<UUID>> {
        if (session != null) TODO("Session exists!")
        val newPointers = mutableMapOf<UUID, Int>()
        val result = mutableMapOf<UUID, Set<UUID>>()
        for ((storageId, payloadIds) in ids) {
            val (_, transformer) = transformers[storageId] ?: error("No storage by ID: \"$storageId\"!")
            val inputPointer = streamers.getPointer(id = storageId)
            val outputPointer = inputPointer + 1
            val deleted = getSyncStorage(
                id = storageId,
                streamer = streamers.getStreamer(
                    id = storageId,
                    inputPointer = inputPointer,
                    outputPointer = outputPointer,
                ),
                transformer = transformer,
            ).deleteAll(payloadIds)
            if (deleted.isNotEmpty()) {
                result[storageId] = deleted
                newPointers[storageId] = outputPointer
            }
        }
        streamers.putPointers(newPointers)
        return result
    }
}
