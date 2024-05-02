package sp.kx.storages

import java.io.InputStream
import java.io.OutputStream
import java.util.Base64
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

abstract class StreamsStorage<T : Any>(override val id: UUID) : MutableStorage<T> {
    override val hash: String
        get() {
            val hashes = inputStream().use { stream ->
                val reader = stream.bufferedReader()
                reader.readLine() // 0) deleted
                val size = reader.readLine().toInt() // 1) items size
                (0 until size).map { _ ->
                    reader.readLine() // 0) item id
                    val hash = reader.readLine().split(",").let { split ->
                        check(split.size == 3)
                        split[2]
                    }
                    reader.readLine() // 2) item
                    hash
                }
            }
            return hash(hashes.joinToString(separator = "").toByteArray())
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
                    val item = decode(reader.readLine().base64()) // 2) item
                    Described(
                        id = id,
                        info = info,
                        item = item,
                    )
                }.toList()
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

    private fun String.base64(): ByteArray {
        return Base64.getDecoder().decode(this)
    }

    private fun write(
        items: List<Described<T>>,
        deleted: Set<UUID> = this.deleted,
    ) {
        val sorted = items.sortedBy { it.info.created }
        outputStream().use { stream ->
            val writer = stream.bufferedWriter()
            writer.write(deleted.joinToString(separator = ",") { it.toString() })
            writer.newLine()
            writer.write(sorted.size.toString())
            for (it in sorted) {
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
        TODO("delete")
    }

    override fun update(id: UUID, item: T): Described<T> {
        TODO("update")
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
        write(items = items)
        return described
    }
}
