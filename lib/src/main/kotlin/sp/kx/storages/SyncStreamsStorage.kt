package sp.kx.storages

import sp.kx.bytes.readBytes
import sp.kx.bytes.readInt
import sp.kx.bytes.readLong
import sp.kx.bytes.readUUID
import sp.kx.bytes.write
import sp.kx.bytes.writeBytes
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
    override val items: List<Described<T>>
        get() {
            return streamer.inputStream().use { stream ->
                stream.skip(stream.readInt().toLong() * 16) // skip deleted
                stream.skip(stream.readInt().toLong() * 16) // skip locals
                List(stream.readInt()) { _ ->
                    val id = stream.readUUID()
                    val info = ItemInfo(
                        created = stream.readLong().milliseconds,
                        updated = stream.readLong().milliseconds,
                        hash = stream.readBytes(size = hf.size),
                    )
                    Described(
                        id = id,
                        info = info,
                        payload = transformer.decode(stream.readBytes(size = stream.readInt())),
                    )
                }
            }
        }
    private val deleted: Set<UUID> get() = getDeleted(streamer = streamer)

    private val locals: Set<UUID> get() = getLocals(streamer = streamer)

    private fun write(
        deleted: Set<UUID> = this.deleted,
        locals: Set<UUID> = this.locals,
        items: List<Described<T>>,
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
                stream.writeBytes(value = it.id)
                stream.writeBytes(value = it.info.created.inWholeMilliseconds)
                stream.writeBytes(value = it.info.updated.inWholeMilliseconds)
                stream.write(it.info.hash)
                val encoded = transformer.encode(it.payload)
                stream.writeBytes(value = encoded.size)
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
        for (item in getEncoded(streamer = streamer, hf = hf)) {
            if (info.deleted.contains(item.id)) continue
            if (info.items.any { it.id == item.id }) continue
            if (info.downloaded.contains(item.id)) downloaded.add(item)
            newItems += item.map(transformer::decode)
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

    override fun getSyncInfo(): SyncInfo = getSyncInfo(streamer = streamer, hf = hf)

    override fun getMergeInfo(info: SyncInfo): MergeInfo = getMergeInfo(streamer = streamer, hf = hf, info = info)

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
                    infos[stream.readUUID()] = ItemInfo(
                        created = stream.readLong().milliseconds,
                        updated = stream.readLong().milliseconds,
                        hash = stream.readBytes(size = hf.size),
                    )
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
        ): List<Described<ByteArray>> {
            return streamer.inputStream().use { stream ->
                stream.skip(stream.readInt().toLong() * 16) // skip deleted
                stream.skip(stream.readInt().toLong() * 16) // skip locals
                List(stream.readInt()) { _ ->
                    val id = stream.readUUID()
                    val info = ItemInfo(
                        created = stream.readLong().milliseconds,
                        updated = stream.readLong().milliseconds,
                        hash = stream.readBytes(size = hf.size),
                    )
                    Described(
                        id = id,
                        info = info,
                        payload = stream.readBytes(size = stream.readInt()),
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
            val uploaded = mutableListOf<Described<ByteArray>>()
            val items = getEncoded(streamer = streamer, hf = hf)
            for (described in items) {
                if (info.infos.containsKey(described.id)) continue
                if (info.deleted.contains(described.id)) continue
                uploaded.add(described)
            }
            val deleted = getDeleted(streamer = streamer)
            for ((itemId, itemInfo) in info.infos) {
                val described = items.firstOrNull { it.id == itemId }
                if (described == null) {
                    if (deleted.contains(itemId)) continue
                    downloaded.add(itemId)
                } else if (itemInfo.updated > described.info.updated) {
                    downloaded.add(itemId)
                } else if (!itemInfo.hash.contentEquals(described.info.hash)) {
                    uploaded.add(described)
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
