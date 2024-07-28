package sp.service.sample

import sp.kx.storages.Streamer
import sp.kx.storages.SyncStreamsStorage
import sp.kx.storages.SyncStreamsStorages
import sp.kx.storages.Transformer
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.UUID
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private data class Foo(val text: String)

private object FooTransformer : Transformer<Foo> {
    override fun encode(decoded: Foo): ByteArray {
        return decoded.text.toByteArray()
    }

    override fun decode(encoded: ByteArray): Foo {
        return Foo(text = String(encoded))
    }
}

private object DefaultEnvironment : SyncStreamsStorage.Environment {
    override fun now(): Duration {
        return System.currentTimeMillis().milliseconds
    }

    override fun randomUUID(): UUID {
        return UUID.randomUUID()
    }
}

private class FileStreamerProvider(
    ids: Set<UUID>,
) : SyncStreamsStorages.StreamerProvider {
    private val dir = File("/tmp/sample")

    init {
        File(dir, "storages").mkdirs()
        val pointers = File(dir, "pointers")
        if (!pointers.exists() || pointers.length() == 0L) {
            val text = ids.joinToString(separator = ",") { id ->
                "$id:0"
            }
            File(dir, "pointers").writeText(text)
        }
    }

    override fun getStreamer(id: UUID, inputPointer: Int, outputPointer: Int): Streamer {
        return FileStreamer(
            dir = File(dir, "storages"),
            id = id,
            inputPointer = inputPointer,
            outputPointer = outputPointer,
        )
    }

    private fun getValues(): Map<UUID, Int> {
        val text = File(dir, "pointers").readText()
        check(text.isNotEmpty())
        return text.split(",").associate {
            val (_id, _pointer) = it.split(":")
            UUID.fromString(_id) to _pointer.toInt()
        }
    }

    override fun getPointer(id: UUID): Int {
        return getValues()[id] ?: error("No pointer by ID: \"$id\"!")
    }

    override fun putPointers(values: Map<UUID, Int>) {
        val newValues = getValues() + values
        val text = newValues.entries.joinToString(separator = ",") { (id, pointer) ->
            "$id:$pointer"
        }
        File(dir, "pointers").writeText(text)
        val files = File(dir, "storages").listFiles()!!
        for (file in files) {
            if (file.isDirectory) continue
            val exists = newValues.any { (id, pointer) -> file.name == "$id-$pointer"}
            if (!exists) file.delete()
        }
    }
}

private class FileStreamer(
    private val dir: File,
    private val id: UUID,
    private val inputPointer: Int,
    private val outputPointer: Int,
) : Streamer {
    override fun inputStream(): InputStream {
        check(dir.exists())
        check(dir.isDirectory)
        val file = File(dir, "$id-$inputPointer")
        if (!file.exists() || file.length() == 0L) {
            file.writeBytes(ByteArray(8))
        }
        return file.inputStream()
    }

    override fun outputStream(): OutputStream {
        check(dir.exists())
        check(dir.isDirectory)
        return File(dir, "$id-$outputPointer").outputStream()
    }
}

private fun Byte.toHEX(): String {
    return String.format(Locale.US, "%02x", toInt() and 0xff)
}

private fun ByteArray.toHEX(): String {
    return joinToString(separator = "") { it.toHEX() }
}

private fun SyncStreamsStorages.println() {
    val builder = StringBuilder()
    val hashes = hashes()
    builder.append("\n")
        .append("hashes:")
    hashes.forEach { (id, bytes) ->
        builder.append("\n").append("$id: ${bytes.toHEX()}")
    }
    hashes.forEach { (id, _) ->
        builder.append("\n")
            .append("items:")
            .append("\n")
            .append("$id")
        val storages = require(id = id)
        storages.items.forEachIndexed { index, item ->
            builder.append("\n")
                .append("$index] ${item.id}: ${item.item}")
        }
    }
    println(builder.toString())
}

fun main() {
    val dir = File("/tmp/sample")
    dir.deleteRecursively()
    val storages = SyncStreamsStorages.Builder()
        .add(UUID.fromString("548ba538-0ff1-43ba-8b36-4bdbe4c32aef"), FooTransformer)
        .build(
            hf = MDHashFunction("MD5"),
            env = DefaultEnvironment,
            getStreamerProvider = ::FileStreamerProvider,
        )
    storages.println()
    storages.require<Foo>().add(Foo(text = "foo:${UUID.randomUUID().hashCode().absoluteValue}"))
    storages.println()
}
