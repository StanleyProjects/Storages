package sp.kx.storages

import sp.kx.bytes.readBytes
import sp.kx.bytes.readInt
import sp.kx.bytes.readLong
import sp.kx.bytes.readUUID
import sp.kx.bytes.write
import sp.kx.bytes.writeBytes
import java.io.InputStream
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * An implementation of [SyncStorage] that uses input and output streams to write and read data.
 *
 * Usage:
 * ```
 * class FooStorage : SyncStreamsStorage<Foo>(
 *     id = UUID.fromString("43518ed6-cda2-48c3-bd28-fed6fab80101"),
 * ) {
 * ...
 * }
 * val storage: SyncStorage<Foo> = FooStorage()
 * ```
 * @author [Stanley Wintergreen](https://github.com/kepocnhh)
 * @since 0.3.1
 */
@Suppress(
    "MagicNumber",
    "TooManyFunctions",
)
class SyncStreamsStorage<T : Any>(
    override val id: UUID,
    private val hf: HashFunction,
    private val streamer: Streamer,
    private val transformer: Transformer<T>,
    private val env: Environment,
) : SyncStorage<T> {
    interface Environment {
        fun now(): Duration
        fun randomUUID(): UUID
    }

    override val hash: ByteArray get() = getHash(streamer = streamer, hf = hf)
    override val items: List<Payload<T>>
        get() {
            return streamer.inputStream().use { stream ->
                stream.skip(stream.readInt().toLong() * 16) // skip deleted
                stream.skip(stream.readInt().toLong() * 16) // skip locals
                List(stream.readInt()) { _ ->
                    Payload(
                        meta = Metadata(
                            id = stream.readUUID(),
                            created = stream.readLong().milliseconds,
                            info = stream.readItemInfo(hf = hf),
                        ),
                        value = transformer.decode(stream.readBytes(size = stream.readInt())),
                    )
                }
            }
        }
    private val deleted: Set<UUID> get() = getDeleted(streamer = streamer)

    private val locals: Set<UUID> get() = getLocals(streamer = streamer)

    private fun write(
        deleted: Set<UUID> = this.deleted,
        locals: Set<UUID> = this.locals,
        items: List<Payload<T>>,
    ) {
        streamer.outputStream().use { stream ->
            stream.writeBytes(value = deleted.size)
            for (it in deleted) {
                stream.writeBytes(value = it)
            }
            stream.writeBytes(value = locals.size)
            for (it in locals) {
                stream.writeBytes(value = it)
            }
            stream.writeBytes(value = items.size)
            for (it in items) {
                stream.writeBytes(value = it.meta.id)
                stream.writeBytes(value = it.meta.created.inWholeMilliseconds)
                stream.writeBytes(value = it.meta.info)
                val encoded = transformer.encode(it.value)
                stream.writeBytes(value = encoded.size)
                stream.write(encoded)
            }
            stream.flush()
        }
    }

    override fun delete(id: UUID): Boolean {
        val items = items.toMutableList()
        for (index in items.indices) {
            if (items[index].meta.id == id) {
                val oldItem = items.removeAt(index)
                check(oldItem.meta.id == id)
                val newDeleted = deleted.toMutableSet()
                val newLocals = locals.toMutableSet()
                if (newLocals.contains(id)) {
                    newLocals -= id
                } else {
                    newDeleted += id
                }
                write(
                    items = items,
                    deleted = newDeleted,
                    locals = newLocals,
                )
                return true
            }
        }
        return false
    }

    override fun update(id: UUID, value: T): ItemInfo? {
        val items = items.toMutableList()
        for (index in items.indices) {
            val oldItem = items[index]
            if (oldItem.meta.id == id) {
                val newItem = oldItem.copy(
                    updated = env.now(),
                    hash = hf.map(transformer.encode(value)),
                    value = value,
                )
                items[index] = newItem
                write(items = items)
                return newItem.meta.info
            }
        }
        return null
    }

    override fun add(value: T): Payload<T> {
        val items = items.toMutableList()
        val created = env.now()
        val payload = Payload(
            meta = Metadata(
                id = env.randomUUID(),
                created = created,
                info = ItemInfo(
                    updated = created,
                    hash = hf.map(transformer.encode(value)),
                ),
            ),
            value = value,
        )
        items.add(payload)
        write(
            items = items.sortedBy { it.meta.created },
            locals = locals + payload.meta.id,
        )
        return payload
    }

    private fun bytesOf(items: List<Payload<out Any>>): ByteArray {
        val size = 16 + 8 + hf.size
        val bytes = ByteArray(items.size * size)
        for (index in items.indices) {
            val item = items[index]
            bytes.write(index = index * size, value = item.meta.id)
            bytes.write(index = index * size + 16, value = item.meta.info.updated.inWholeMilliseconds)
            System.arraycopy(item.meta.info.hash, 0, bytes, index * size + 16 + 8, hf.size)
        }
        return bytes
    }

    override fun merge(info: MergeInfo): CommitInfo {
        val downloaded = mutableListOf<RawPayload>()
        val newItems = mutableListOf<Payload<T>>()
        for (item in getEncoded(streamer = streamer, hf = hf)) {
            if (info.deleted.contains(item.meta.id)) continue
            if (info.items.any { it.meta.id == item.meta.id }) continue
            if (info.downloaded.contains(item.meta.id)) downloaded.add(item)
            newItems += item.map(transformer::decode)
        }
        for (item in info.items) {
            newItems += item.map(transformer::decode)
        }
        val deleted = this.deleted
        val sorted = newItems.sortedBy { it.meta.created }
        write(
            items = sorted,
            deleted = deleted + info.deleted,
            locals = emptySet(),
        )
        return CommitInfo(
            hash = hf.map(bytesOf(items = sorted)),
            items = downloaded,
            deleted = deleted,
        )
    }

    override fun commit(info: CommitInfo): Boolean {
        val oldDeleted = deleted
        if (info.items.isEmpty() && oldDeleted.containsAll(info.deleted) && locals.isEmpty()) {
            check(hash.contentEquals(info.hash)) { "Wrong hash!" }
            return false
        }
        val newItems = mutableListOf<Payload<T>>()
        for (item in this.items) {
            if (info.deleted.contains(item.meta.id)) continue
            if (info.items.any { it.meta.id == item.meta.id }) continue
            newItems += item
        }
        for (item in info.items) {
            newItems += item.map(transformer::decode)
        }
        val sorted = newItems.sortedBy { it.meta.created }
        check(hf.map(bytesOf(items = sorted)).contentEquals(info.hash)) { "Wrong hash!" }
        write(
            items = sorted,
            deleted = oldDeleted + info.deleted,
            locals = emptySet(),
        )
        return true
    }

    override fun getSyncInfo(): SyncInfo = getSyncInfo(streamer = streamer, hf = hf)

    override fun getMergeInfo(info: SyncInfo): MergeInfo = getMergeInfo(streamer = streamer, hf = hf, info = info)

    private fun get(id: UUID, stream: InputStream): Payload<T>? {
        stream.skip(stream.readInt().toLong() * 16) // skip deleted
        stream.skip(stream.readInt().toLong() * 16) // skip locals
        for (i in 0 until stream.readInt()) {
            val actual = stream.readUUID()
            if (actual != id) {
                stream.skip(8) // skip created
                stream.skipItemInfo(hf = hf)
                stream.skip(stream.readInt().toLong()) // skip encoded
                continue
            }
            return Payload(
                meta = Metadata(
                    id = actual,
                    created = stream.readLong().milliseconds,
                    info = stream.readItemInfo(hf = hf),
                ),
                value = transformer.decode(encoded = stream.readBytes(size = stream.readInt())),
            )
        }
        return null
    }

    override fun get(id: UUID): Payload<T>? {
        return streamer.inputStream().use { stream ->
            get(id = id, stream = stream)
        }
    }

    companion object {
        internal fun getHash(
            streamer: Streamer,
            hf: HashFunction,
        ): ByteArray {
            return streamer.inputStream().use { stream ->
                stream.skip(stream.readInt().toLong() * 16) // skip deleted
                stream.skip(stream.readInt().toLong() * 16) // skip locals
                val itemsSize = stream.readInt()
                val size = 16 + 8 + hf.size
                val bytes = ByteArray(itemsSize * size)
                for (index in 0 until itemsSize) {
                    stream.read(bytes, index * size, 16)
                    stream.skip(8) // skip created
                    stream.read(bytes, index * size + 16, 8)
                    stream.read(bytes, index * size + 16 + 8, hf.size)
                    stream.skip(stream.readInt().toLong()) // skip encoded
                }
                hf.map(bytes)
            }
        }

        internal fun getSyncInfo(
            streamer: Streamer,
            hf: HashFunction,
        ): SyncInfo {
            return streamer.inputStream().use { stream ->
                val deletedSize = stream.readInt()
                val deleted = HashSet<UUID>(deletedSize)
                for (index in 0 until deletedSize) {
                    deleted.add(stream.readUUID())
                }
                stream.skip(stream.readInt().toLong() * 16) // skip locals
                val itemsSize = stream.readInt()
                val infos = HashMap<UUID, ItemInfo>(itemsSize)
                for (ignored in 0 until itemsSize) {
                    val id = stream.readUUID()
                    stream.skip(8) // skip created
                    infos[id] = stream.readItemInfo(hf = hf)
                    stream.skip(stream.readInt().toLong()) // skip encoded
                }
                SyncInfo(
                    infos = infos,
                    deleted = deleted,
                )
            }
        }

        private fun getEncoded(
            streamer: Streamer,
            hf: HashFunction,
        ): List<RawPayload> {
            return streamer.inputStream().use { stream ->
                stream.skip(stream.readInt().toLong() * 16) // skip deleted
                stream.skip(stream.readInt().toLong() * 16) // skip locals
                List(stream.readInt()) { _ ->
                    RawPayload(
                        meta = Metadata(
                            id = stream.readUUID(),
                            created = stream.readLong().milliseconds,
                            info = stream.readItemInfo(hf = hf),
                        ),
                        bytes = stream.readBytes(size = stream.readInt()),
                    )
                }
            }
        }

        private fun getDeleted(streamer: Streamer): Set<UUID> {
            val set = HashSet<UUID>()
            streamer.inputStream().use { stream ->
                for (index in 0 until stream.readInt()) {
                    set.add(stream.readUUID())
                }
            }
            return set
        }

        private fun getLocals(streamer: Streamer): Set<UUID> {
            val set = HashSet<UUID>()
            streamer.inputStream().use { stream ->
                stream.skip(stream.readInt().toLong() * 16) // skip deleted
                for (index in 0 until stream.readInt()) {
                    set.add(stream.readUUID())
                }
            }
            return set
        }

        internal fun getMergeInfo(
            streamer: Streamer,
            hf: HashFunction,
            info: SyncInfo,
        ): MergeInfo {
            val downloaded = mutableSetOf<UUID>()
            val uploaded = mutableListOf<RawPayload>()
            val items = getEncoded(streamer = streamer, hf = hf)
            for (payload in items) {
                if (info.infos.containsKey(payload.meta.id)) continue
                if (info.deleted.contains(payload.meta.id)) continue
                uploaded.add(payload)
            }
            val deleted = getDeleted(streamer = streamer)
            for ((itemId, itemInfo) in info.infos) {
                val payload = items.firstOrNull { it.meta.id == itemId }
                if (payload == null) {
                    if (deleted.contains(itemId)) continue
                    downloaded.add(itemId)
                } else if (itemInfo.updated > payload.meta.info.updated) {
                    downloaded.add(itemId)
                } else if (!itemInfo.hash.contentEquals(payload.meta.info.hash)) {
                    uploaded.add(payload)
                }
            }
            return MergeInfo(
                downloaded = downloaded,
                items = uploaded,
                deleted = deleted,
            )
        }
    }
}
