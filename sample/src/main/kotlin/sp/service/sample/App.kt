package sp.service.sample

import sp.kx.storages.SyncStorages
import sp.kx.storages.SyncStreamsStorage
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private data class Foo(val text: String)

private data class Bar(val number: Int)

private class FileStorage<T : Any>(
    id: UUID,
    pointers: SyncStorages.Pointers,
    private val toBytes: (T) -> ByteArray,
    private val fromBytes: (ByteArray) -> T,
) : SyncStreamsStorage<T>(
    id = id,
    hf = MD5HashFunction(),
    pointers = pointers,
) {
    private val now = System.currentTimeMillis().milliseconds

    private fun getFile(pointer: Int): File {
        val file = File("/tmp", "storage-$id-$pointer-${now.inWholeMilliseconds}")
        if (!file.exists() || file.length() == 0L) {
            file.writeBytes(ByteArray(8))
        }
        return file
    }

    override fun now(): Duration {
        return System.currentTimeMillis().milliseconds
    }

    override fun randomUUID(): UUID {
        return UUID.randomUUID()
    }

    override fun encode(item: T): ByteArray {
        return toBytes(item)
    }

    override fun decode(bytes: ByteArray): T {
        return fromBytes(bytes)
    }

    override fun inputStream(pointer: Int): InputStream {
        return getFile(pointer = pointer).inputStream()
    }

    override fun outputStream(pointer: Int): OutputStream {
        return getFile(pointer = pointer).outputStream()
    }
}

private fun SyncStorages.println() {
    val message = hashes().toList().joinToString { (id, hash) ->
        val items = require(id).items
        """
            >
            id: $id
            hash: ${hash.toList()}
            items(${items.size}): $items
            <
        """.trimIndent()
    }
    println(message)
}

private fun Map<UUID, ByteArray>.assert(actual: Map<UUID, ByteArray>) {
    check(keys.sorted() == actual.keys.sorted())
    for (key in keys) {
        check(get(key).contentEquals(actual[key]!!))
    }
}

fun main() {
    val tPointers = object : SyncStorages.Pointers {
        private var values: Map<UUID, Int> = emptyMap()

        override fun getAll(): Map<UUID, Int> {
            return values
        }

        override fun setAll(values: Map<UUID, Int>) {
            this.values = values
        }
    }
    val tStorages = SyncStorages.Builder()
        .add(
            FileStorage(
                id = UUID.fromString("dbb81949-54c9-42e7-91b4-7be1a84bc875"),
                pointers = tPointers,
                fromBytes = {
                    Foo(text = String(it))
                },
                toBytes = {
                    it.text.toByteArray()
                },
            ),
        )
        .add(
            FileStorage(
                id = UUID.fromString("ad113e72-247e-4799-bb32-cd53d8330a86"),
                pointers = tPointers,
                fromBytes = {
                    Bar(number = it[0].toInt())
                },
                toBytes = {
                    byteArrayOf(it.number.toByte())
                },
            ),
        )
        .build(pointers = tPointers)
    val rPointers = object : SyncStorages.Pointers {
        private var values: Map<UUID, Int> = emptyMap()

        override fun getAll(): Map<UUID, Int> {
            return values
        }

        override fun setAll(values: Map<UUID, Int>) {
            this.values = values
        }
    }
    val rStorages = SyncStorages.Builder()
        .add(
            FileStorage(
                id = UUID.fromString("dbb81949-54c9-42e7-91b4-7be1a84bc875"),
                pointers = rPointers,
                fromBytes = {
                    Foo(text = String(it))
                },
                toBytes = {
                    it.text.toByteArray()
                },
            ),
        )
        .add(
            FileStorage(
                id = UUID.fromString("ad113e72-247e-4799-bb32-cd53d8330a86"),
                pointers = rPointers,
                fromBytes = {
                    Bar(number = it[0].toInt())
                },
                toBytes = {
                    byteArrayOf(it.number.toByte())
                },
            ),
        )
        .build(pointers = rPointers)
    tStorages.println()
    val e1 = tStorages.hashes()
    println("---")
    Foo(text = "42").also { item ->
        println("item: $item")
        tStorages.require<Foo>().add(item)
        tStorages.println()
    }
    val e2 = tStorages.hashes()
    println("---")
    Foo(text = "128").also { item ->
        println("item: $item")
        tStorages.require<Foo>().add(item)
        tStorages.println()
    }
    check(e1 != tStorages.hashes())
    check(e2 != tStorages.hashes())
//    println("---")
//    tStorages.require<Foo>().items.lastOrNull()!!.also { item ->
//        println("delete: ${item.id}")
//        tStorages.require<Foo>().delete(item.id)
//        tStorages.println()
//    }
//    e2.assert(tStorages.hashes())
//    println("---")
//    tStorages.require<Foo>().items.lastOrNull()!!.also { item ->
//        println("delete: ${item.id}")
//        tStorages.require<Foo>().delete(item.id)
//        tStorages.println()
//    }
//    e1.assert(tStorages.hashes())
    println("---")
    Bar(number = 21).also { item ->
        println("item: $item")
        tStorages.require<Bar>().add(item)
        tStorages.println()
    }
    println("---")
    Foo(text = "11").also { item ->
        println("item: $item")
        rStorages.require<Foo>().add(item)
        rStorages.println()
    }
    val rSyncInfo = rStorages.getSyncInfo(tStorages.hashes())
    val tMergeInfo = tStorages.getMergeInfo(rSyncInfo)
    tStorages.commit(rStorages.merge(tMergeInfo))
    println("\n\t---")
    tStorages.println()
    println("\n\t---")
    rStorages.println()
}
