package sp.kx.storages

import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

abstract class SyncStreamsStorage<T : Any>(id: UUID) : StreamsStorage<T>(id), SyncStorage<T> {
    override fun merge(info: MergeInfo): List<Described<ByteArray>> {
        TODO("merge")
    }

    override fun getSyncInfo(): SyncInfo {
        val meta = mutableMapOf<UUID, ItemInfo>()
        val deleted: Set<UUID>
        inputStream().use { stream ->
            val reader = stream.bufferedReader()
            reader.readLine() // 0) id
            deleted = reader.readLine()
                .split(",")
                .filter { it.isNotBlank() }
                .map(UUID::fromString)
                .toSet()
            val size = reader.readLine().toInt() // 2) items size
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
        TODO("getMergeInfo")
    }
}
