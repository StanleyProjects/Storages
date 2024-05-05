package sp.kx.storages

import java.io.InputStream
import java.io.OutputStream
import java.util.Base64
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Suppress(
    "MagicNumber",
    "TooManyFunctions",
)
abstract class SyncStreamsStorage<T : Any>(override val id: UUID) : SyncStorage<T> {
    override val hash: String
        get() {
            val builder = StringBuilder()
            inputStream().use { stream ->
                val reader = stream.bufferedReader()
                reader.readLine() // 0) deleted
                val size = reader.readLine().toInt() // 1) items size
                for (ignored in 0 until size) {
                    reader.readLine() // 0) item id
                    val split = reader.readLine().split(",")
                    check(split.size == 3)
                    builder.append(split[2])
                    reader.readLine() // 2) item
                }
            }
            return hash(builder.toString().toByteArray())
        }
    override val items: List<Described<T>>
        get() {
            return inputStream().use { stream ->
                val reader = stream.bufferedReader()
                reader.readLine() // 0) deleted
                val size = reader.readLine().toInt() // 1) items size
                (0 until size).map { _ ->
                    val id = UUID.fromString(reader.readLine()) // 0) item id
                    val info = reader.readLine().split(",").let { split ->
                        check(split.size == 3)
                        ItemInfo(
                            created = split[0].toLong().milliseconds,
                            updated = split[1].toLong().milliseconds,
                            hash = split[2],
                        )
                    }
                    val item = decode(base64(reader.readLine())) // 2) item
                    Described(
                        id = id,
                        info = info,
                        item = item,
                    )
                }
            }
        }
    override val deleted: Set<UUID>
        get() {
            return inputStream().use { stream ->
                val reader = stream.bufferedReader()
                reader.readLine()
                    .split(",")
                    .filter { it.isNotBlank() }
                    .map(UUID::fromString)
                    .toSet()
            }
        }

    private fun ItemInfo.toLine(): String {
        return StringBuilder()
            .append(created.inWholeMilliseconds)
            .append(",")
            .append(updated.inWholeMilliseconds)
            .append(",")
            .append(hash)
            .toString()
    }

    private fun ByteArray.base64(): String {
        return Base64.getEncoder().encodeToString(this)
    }

    private fun base64(value: String): ByteArray {
        return Base64.getDecoder().decode(value)
    }

    private fun write(
        items: List<Described<T>>,
        deleted: Set<UUID> = this.deleted,
    ) {
        outputStream().use { stream ->
            val writer = stream.bufferedWriter()
            writer.write(deleted.joinToString(separator = ",") { it.toString() })
            writer.newLine()
            writer.write(items.size.toString())
            for (it in items) {
                writer.newLine()
                writer.write(it.id.toString())
                writer.newLine()
                writer.write(it.info.toLine())
                writer.newLine()
                writer.write(encode(it.item).base64())
            }
            writer.flush()
        }
    }

    protected abstract fun now(): Duration
    protected abstract fun randomUUID(): UUID
    protected abstract fun hash(bytes: ByteArray): String
    protected abstract fun encode(item: T): ByteArray
    protected abstract fun decode(bytes: ByteArray): T
    protected abstract fun inputStream(): InputStream
    protected abstract fun outputStream(): OutputStream

    override fun delete(id: UUID): Boolean {
        val items = items.toMutableList()
        for (index in items.indices) {
            val item = items[index]
            if (item.id == id) {
                val oldItem = items.removeAt(index)
                check(oldItem.id == id)
                write(
                    items = items,
                    deleted = deleted + id,
                )
                return true
            }
        }
        return false
    }

    override fun update(id: UUID, item: T): ItemInfo? {
        val items = items.toMutableList()
        for (index in items.indices) {
            val it = items[index]
            if (it.id == id) {
                val oldItem = items.removeAt(index)
                check(oldItem.id == id)
                val described = it.copy(
                    updated = now(),
                    hash = hash(encode(item)),
                    item = item,
                )
                items.add(described)
                write(items = items.sortedBy { it.info.created })
                return described.info
            }
        }
        return null
    }

    override fun add(item: T): Described<T> {
        val items = items.toMutableList()
        val created = now()
        val described = Described(
            id = randomUUID(),
            info = ItemInfo(
                created = created,
                updated = created,
                hash = hash(encode(item)),
            ),
            item = item,
        )
        items.add(described)
        write(items = items.sortedBy { it.info.created })
        return described
    }

    override fun merge(info: MergeInfo): CommitInfo {
        val download = mutableListOf<Described<ByteArray>>()
        val newItems = mutableListOf<Described<T>>()
        for (item in this.items) {
            if (info.deleted.contains(item.id)) continue
            if (info.items.any { it.id == item.id }) continue
            if (info.download.contains(item.id)) download.add(item.map(::encode))
            newItems += item
        }
        for (item in info.items) {
            newItems += item.map(::decode)
        }
        val deleted = this.deleted
        write(
            items = newItems.sortedBy { it.info.created },
            deleted = deleted + info.deleted,
        )
        return CommitInfo(
            hash = hash,
            items = download,
            deleted = deleted,
        )
    }

    override fun merge(info: CommitInfo) {
        val newItems = mutableListOf<Described<T>>()
        for (item in this.items) {
            if (info.deleted.contains(item.id)) continue
            if (info.items.any { it.id == item.id }) continue
            newItems += item
        }
        for (item in info.items) {
            newItems += item.map(::decode)
        }
        write(
            items = newItems.sortedBy { it.info.created },
            deleted = this.deleted + info.deleted,
        )
        check(hash == info.hash)
    }

    override fun getSyncInfo(): SyncInfo {
        val meta = mutableMapOf<UUID, ItemInfo>()
        val deleted: Set<UUID>
        inputStream().use { stream ->
            val reader = stream.bufferedReader()
            deleted = reader.readLine()
                .split(",")
                .filter { it.isNotBlank() }
                .map(UUID::fromString)
                .toSet()
            val size = reader.readLine().toInt() // 1) items size
            for (ignored in 0 until size) {
                val id = UUID.fromString(reader.readLine()) // 0) item id
                val split = reader.readLine().split(",")
                check(split.size == 3)
                @Suppress("IgnoredReturnValue")
                meta[id] = ItemInfo(
                    created = split[0].toLong().milliseconds,
                    updated = split[1].toLong().milliseconds,
                    hash = split[2],
                )
                reader.readLine() // 2) item
            }
        }
        return SyncInfo(
            meta = meta,
            deleted = deleted,
        )
    }

    override fun getMergeInfo(info: SyncInfo): MergeInfo {
        val download = mutableSetOf<UUID>()
        val upload = mutableListOf<Described<ByteArray>>()
        val items = items
        for (described in items) {
            if (info.meta.containsKey(described.id)) continue
            if (info.deleted.contains(described.id)) continue
            upload.add(described.map(::encode))
        }
        val deleted = deleted
        for ((itemId, itemInfo) in info.meta) {
            val described = items.firstOrNull { it.id == itemId }
            if (described == null) {
                if (deleted.contains(itemId)) continue
                download.add(itemId)
            } else if (itemInfo.hash != described.info.hash) {
                if (itemInfo.updated > described.info.updated) {
                    download.add(itemId)
                } else {
                    upload.add(described.map(::encode))
                }
            }
        }
        return MergeInfo(
            download = download,
            items = upload,
            deleted = deleted,
        )
    }
}
