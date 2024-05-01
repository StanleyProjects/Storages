package sp.kx.storages

import java.io.InputStream
import java.io.OutputStream
import java.util.Base64
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

abstract class BytesStorage<T : Any>(
    override val id: UUID,
) : MutableStorage<T> {
    override val items: List<Described<T>>
        get() {
            return inputStream().use { stream ->
                val reader = stream.bufferedReader()
                reader.readLine()
                reader.readLine()
                val size = reader.readLine().toInt()
                (0 until size).map { _ ->
                    val line = reader.readLine()
                    val split = line.split(",")
                    check(split.size == 3)
                    val id = UUID.fromString(split[0])
                    val info = split[1].split("|").let {
                        check(it.size == 3)
                        ItemInfo(
                            created = it[0].toLong().milliseconds,
                            updated = it[1].toLong().milliseconds,
                            hash = it[2],
                        )
                    }
                    val item = decode(split[2].base64())
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
                reader.readLine()
                    .split(",")
                    .filter { it.isNotBlank() }
                    .map(UUID::fromString)
                    .toSet()
            }
        }

    override fun delete(id: UUID): Boolean {
        val items = items.toMutableList()
        for (index in items.indices) {
            val item = items[index]
            if (item.id != id) continue
            items.removeAt(index)
            write(
                items = items,
                deleted = deleted + id,
            )
            return true
        }
        return false
    }

    override fun update(id: UUID, item: T): Described<T> {
        TODO("update")
    }

    protected abstract fun now(): Duration
    protected abstract fun randomUUID(): UUID
    protected abstract fun itemHash(item: T): String
    protected abstract fun encode(item: T): ByteArray
    protected abstract fun decode(bytes: ByteArray): T
    protected abstract fun inputStream(): InputStream
    protected abstract fun outputStream(): OutputStream

    private fun ItemInfo.toLine(): String {
        return StringBuilder()
            .append(created.inWholeMilliseconds)
            .append("|")
            .append(updated.inWholeMilliseconds)
            .append("|")
            .append(hash)
            .toString()
    }

    private fun Described<T>.toLine(): String {
        return StringBuilder()
            .append(id.toString())
            .append(",")
            .append(info.toLine())
            .append(",")
            .append(encode(item).base64())
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
            println("stream: ${stream::class.java.name}")
            val writer = stream.bufferedWriter()
            writer.write(id.toString())
            writer.newLine()
            writer.write(deleted.joinToString(separator = ",") { it.toString() })
            writer.newLine()
            val size = sorted.size
            println("size: $size")
            writer.write(size.toString())
            for (it in sorted) {
                writer.newLine()
                writer.write(it.toLine())
            }
            writer.flush()
        }
    }

    override fun add(item: T): Described<T> {
        val items = items.toMutableList()
        val created = now()
        val described = Described(
            id = randomUUID(),
            info = ItemInfo(
                created = created,
                updated = created,
                hash = itemHash(item),
            ),
            item = item,
        )
        items.add(described)
        write(items = items)
        return described
    }
}
