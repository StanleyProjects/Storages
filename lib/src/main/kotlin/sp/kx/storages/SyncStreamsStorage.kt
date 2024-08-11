package sp.kx.storages

import sp.kx.bytes.write
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

    override val hash: ByteArray
        get() {
            return streamer.inputStream().use { stream ->
                stream.skip(BytesUtil.readInt(stream).toLong() * 16) // skip deleted
                stream.skip(BytesUtil.readInt(stream).toLong() * 16) // skip locals
                val itemsSize = BytesUtil.readInt(stream)
                val size = 16 + 8 + hf.size
                val bytes = ByteArray(itemsSize * size)
                for (index in 0 until itemsSize) {
                    stream.read(bytes, index * size, 16)
                    stream.skip(8) // skip created
                    stream.read(bytes, index * size + 16, 8)
                    stream.read(bytes, index * size + 16 + 8, hf.size)
                    stream.skip(BytesUtil.readInt(stream).toLong()) // skip encoded
                }
                hf.map(bytes)
            }
        }
    override val items: List<Described<T>>
        get() {
            return streamer.inputStream().use { stream ->
                stream.skip(BytesUtil.readInt(stream).toLong() * 16) // skip deleted
                stream.skip(BytesUtil.readInt(stream).toLong() * 16) // skip locals
                List(BytesUtil.readInt(stream)) { _ ->
                    val id = BytesUtil.readUUID(stream)
                    val info = ItemInfo(
                        created = BytesUtil.readLong(stream).milliseconds,
                        updated = BytesUtil.readLong(stream).milliseconds,
                        hash = BytesUtil.readBytes(stream, hf.size),
                    )
                    Described(
                        id = id,
                        info = info,
                        payload = transformer.decode(BytesUtil.readBytes(stream, BytesUtil.readInt(stream))),
                    )
                }
            }
        }
    private val deleted: Set<UUID>
        get() {
            val set = HashSet<UUID>()
            streamer.inputStream().use { stream ->
                val deletedSize = BytesUtil.readInt(stream)
                for (index in 0 until deletedSize) {
                    set.add(BytesUtil.readUUID(stream))
                }
            }
            return set
        }

    private val locals: Set<UUID>
        get() {
            val set = HashSet<UUID>()
            streamer.inputStream().use { stream ->
                stream.skip(BytesUtil.readInt(stream).toLong() * 16) // skip deleted
                val localsSize = BytesUtil.readInt(stream)
                for (index in 0 until localsSize) {
                    set.add(BytesUtil.readUUID(stream))
                }
            }
            return set
        }

    private fun write(
        deleted: Set<UUID> = this.deleted,
        locals: Set<UUID> = this.locals,
        items: List<Described<T>>,
    ) {
        streamer.outputStream().use { stream ->
            BytesUtil.writeBytes(stream, deleted.size)
            for (it in deleted) {
                BytesUtil.writeBytes(stream, it)
            }
            BytesUtil.writeBytes(stream, locals.size)
            for (it in locals) {
                BytesUtil.writeBytes(stream, it)
            }
            BytesUtil.writeBytes(stream, items.size)
            for (it in items) {
                BytesUtil.writeBytes(stream, it.id)
                BytesUtil.writeBytes(stream, it.info.created.inWholeMilliseconds)
                BytesUtil.writeBytes(stream, it.info.updated.inWholeMilliseconds)
                stream.write(it.info.hash)
                val encoded = transformer.encode(it.payload)
                BytesUtil.writeBytes(stream, encoded.size)
                stream.write(encoded)
            }
            stream.flush()
        }
    }

    override fun delete(id: UUID): Boolean {
        val items = items.toMutableList()
        for (index in items.indices) {
            if (items[index].id == id) {
                val oldItem = items.removeAt(index)
                check(oldItem.id == id)
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

    override fun update(id: UUID, payload: T): ItemInfo? {
        val items = items.toMutableList()
        for (index in items.indices) {
            val oldItem = items[index]
            if (oldItem.id == id) {
                val newItem = oldItem.copy(
                    updated = env.now(),
                    hash = hf.map(transformer.encode(payload)),
                    payload = payload,
                )
                items[index] = newItem
                write(items = items)
                return newItem.info
            }
        }
        return null
    }

    override fun add(payload: T): Described<T> {
        val items = items.toMutableList()
        val created = env.now()
        val described = Described(
            id = env.randomUUID(),
            info = ItemInfo(
                created = created,
                updated = created,
                hash = hf.map(transformer.encode(payload)),
            ),
            payload = payload,
        )
        items.add(described)
        write(
            items = items.sortedBy { it.info.created },
            locals = locals + described.id,
        )
        return described
    }

    private fun bytesOf(items: List<Described<out Any>>): ByteArray {
        val size = 16 + 8 + hf.size
        val bytes = ByteArray(items.size * size)
        for (index in items.indices) {
            val item = items[index]
            bytes.write(index = index * size, value = item.id)
            bytes.write(index = index * size + 16, value = item.info.updated.inWholeMilliseconds)
            System.arraycopy(item.info.hash, 0, bytes, index * size + 16 + 8, hf.size)
        }
        return bytes
    }

    override fun merge(info: MergeInfo): CommitInfo {
        val downloaded = mutableListOf<Described<ByteArray>>()
        val newItems = mutableListOf<Described<T>>()
        for (item in this.items) {
            if (info.deleted.contains(item.id)) continue
            if (info.items.any { it.id == item.id }) continue
            if (info.downloaded.contains(item.id)) downloaded.add(item.map(transformer::encode))
            newItems += item
        }
        for (item in info.items) {
            newItems += item.map(transformer::decode)
        }
        val deleted = this.deleted
        val sorted = newItems.sortedBy { it.info.created }
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
        val newItems = mutableListOf<Described<T>>()
        for (item in this.items) {
            if (info.deleted.contains(item.id)) continue
            if (info.items.any { it.id == item.id }) continue
            newItems += item
        }
        for (item in info.items) {
            newItems += item.map(transformer::decode)
        }
        val sorted = newItems.sortedBy { it.info.created }
        check(hf.map(bytesOf(items = sorted)).contentEquals(info.hash)) { "Wrong hash!" }
        write(
            items = sorted,
            deleted = oldDeleted + info.deleted,
            locals = emptySet(),
        )
        return true
    }

    override fun getSyncInfo(): SyncInfo {
        return streamer.inputStream().use { stream ->
            val deletedSize = BytesUtil.readInt(stream)
            val deleted = HashSet<UUID>(deletedSize)
            for (index in 0 until deletedSize) {
                deleted.add(BytesUtil.readUUID(stream))
            }
            stream.skip(BytesUtil.readInt(stream).toLong() * 16) // skip locals
            val itemsSize = BytesUtil.readInt(stream)
            val infos = HashMap<UUID, ItemInfo>(itemsSize)
            for (ignored in 0 until itemsSize) {
                infos[BytesUtil.readUUID(stream)] = ItemInfo(
                    created = BytesUtil.readLong(stream).milliseconds,
                    updated = BytesUtil.readLong(stream).milliseconds,
                    hash = BytesUtil.readBytes(stream, hf.size),
                )
                stream.skip(BytesUtil.readInt(stream).toLong()) // skip encoded
            }
            SyncInfo(
                infos = infos,
                deleted = deleted,
            )
        }
    }

    override fun getMergeInfo(info: SyncInfo): MergeInfo {
        val downloaded = mutableSetOf<UUID>()
        val uploaded = mutableListOf<Described<ByteArray>>()
        val items = items
        for (described in items) {
            if (info.infos.containsKey(described.id)) continue
            if (info.deleted.contains(described.id)) continue
            uploaded.add(described.map(transformer::encode))
        }
        val deleted = deleted
        for ((itemId, itemInfo) in info.infos) {
            val described = items.firstOrNull { it.id == itemId }
            if (described == null) {
                if (deleted.contains(itemId)) continue
                downloaded.add(itemId)
            } else if (itemInfo.updated > described.info.updated) {
                downloaded.add(itemId)
            } else if (!itemInfo.hash.contentEquals(described.info.hash)) {
                uploaded.add(described.map(transformer::encode))
            }
        }
        return MergeInfo(
            downloaded = downloaded,
            items = uploaded,
            deleted = deleted,
        )
    }
}
