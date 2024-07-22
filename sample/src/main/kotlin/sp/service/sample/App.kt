package sp.service.sample

import sp.kx.storages.SyncStorages
import sp.kx.storages.SyncStreamsStorage
import sp.kx.storages.require
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private data class Foo(val text: String)

private class FileStorage : SyncStreamsStorage<Foo>(
    id = UUID.fromString("dbb81949-54c9-42e7-91b4-7be1a84bc875"),
    hf = MD5HashFunction(),
) {
    private val file = File.createTempFile("storage", id.toString()).also { file ->
        file.outputStream().use { stream ->
            stream.write(ByteArray(8))
        }
    }

    override fun now(): Duration {
        return System.currentTimeMillis().milliseconds
    }

    override fun randomUUID(): UUID {
        return UUID.randomUUID()
    }

    override fun encode(item: Foo): ByteArray {
        return item.text.toByteArray()
    }

    override fun decode(bytes: ByteArray): Foo {
        return Foo(text = String(bytes))
    }

    override fun inputStream(): InputStream {
        return file.inputStream()
    }

    override fun outputStream(): OutputStream {
        return file.outputStream()
    }
}

private fun SyncStorages.println() {
    val message = hashes().toList().joinToString { (id, hash) ->
        """
            >
            id: $id
            hash: ${hash.toList()}
            items: ${require(id).items}
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
    val storages = SyncStorages.create(FileStorage())
    storages.println()
    val e1 = storages.hashes()
    println("---")
    Foo(text = "42").also { item ->
        println("item: $item")
        storages.require<Foo>().add(item)
        storages.println()
    }
    val e2 = storages.hashes()
    println("---")
    Foo(text = "128").also { item ->
        println("item: $item")
        storages.require<Foo>().add(item)
        storages.println()
    }
    check(e1 != storages.hashes())
    check(e2 != storages.hashes())
    println("---")
    storages.require<Foo>().items.lastOrNull()!!.also { item ->
        println("delete: ${item.id}")
        storages.require<Foo>().delete(item.id)
        storages.println()
    }
    e2.assert(storages.hashes())
    println("---")
    storages.require<Foo>().items.lastOrNull()!!.also { item ->
        println("delete: ${item.id}")
        storages.require<Foo>().delete(item.id)
        storages.println()
    }
    e1.assert(storages.hashes())
}
