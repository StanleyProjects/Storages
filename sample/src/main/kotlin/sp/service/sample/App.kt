package sp.service.sample

import sp.kx.storages.Storage
import sp.kx.storages.SyncStreamsStorage
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

private fun Storage<out Any>.println() {
    val message = """
        id: $id
        hash: ${hash.toList()}
        items: $items
    """.trimIndent()
    println(message)
}

fun main() {
    val storage = FileStorage()
    storage.println()
    val e1 = storage.hash
    println("---")
    Foo(text = "42").also { item ->
        println("item: $item")
        storage.add(item)
        storage.println()
    }
    val e2 = storage.hash
    println("---")
    Foo(text = "128").also { item ->
        println("item: $item")
        storage.add(item)
        storage.println()
    }
    println("---")
    storage.items.lastOrNull()!!.also { item ->
        println("delete: ${item.id}")
        storage.delete(item.id)
        storage.println()
    }
    check(e2.contentEquals(storage.hash))
    println("---")
    storage.items.lastOrNull()!!.also { item ->
        println("delete: ${item.id}")
        storage.delete(item.id)
        storage.println()
    }
    check(e1.contentEquals(storage.hash))
}
