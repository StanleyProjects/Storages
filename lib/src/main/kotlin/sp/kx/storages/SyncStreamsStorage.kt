package sp.kx.storages

import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

abstract class SyncStreamsStorage<T : Any>(id: UUID) : StreamsStorage<T>(id), SyncStorage<T> {
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
            (0 until size).map { _ ->
                val id = UUID.fromString(reader.readLine()) // 0) item id
                meta[id] = reader.readLine().split(",").let { split ->
                    check(split.size == 3)
                    ItemInfo(
                        created = split[0].toLong().milliseconds,
                        updated = split[1].toLong().milliseconds,
                        hash = split[2],
                    )
                }
                reader.readLine() // 2) item
            }.toList()
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
