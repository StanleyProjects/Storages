package sp.service.sample

import sp.kx.storages.Storage
import sp.kx.storages.StreamsStorage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private data class Foo(val text: String)

private class FooStorage : StreamsStorage<Foo>(UUID.fromString("dbb81949-54c9-42e7-91b4-7be1a84bc875")) {
    private val stream = ByteArrayOutputStream().also {
        it.write(
            StringBuilder()
                .append("$id")
                .append("\n")
                .append("")
                .append("\n")
                .append("0")
                .toString()
                .toByteArray(),
        )
    }

    override fun now(): Duration {
        return System.currentTimeMillis().milliseconds
    }

    override fun randomUUID(): UUID {
        return UUID.randomUUID()
    }

    override fun hash(bytes: ByteArray): String {
        return String(bytes).hashCode().toString()
    }

    override fun decode(bytes: ByteArray): Foo {
        return Foo(text = String(bytes))
    }

    override fun encode(item: Foo): ByteArray {
        return item.text.toByteArray()
    }

    override fun inputStream(): InputStream {
        return stream.toByteArray().inputStream()
    }

    override fun outputStream(): OutputStream {
        stream.reset()
        return stream
    }
}

private fun Storage<out Any>.println() {
    val message = """
        id: $id
        hash: $hash
        items: $items
        deleted: $deleted
    """.trimIndent()
    println(message)
}

fun main() {
    val storage = FooStorage()
    storage.println()
    println("---")
    Foo(text = "42").also { item ->
        println("item: $item")
        storage.add(item)
        storage.println()
    }
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
    println("---")
    storage.items.lastOrNull()!!.also { item ->
        println("delete: ${item.id}")
        storage.delete(item.id)
        storage.println()
    }
}
