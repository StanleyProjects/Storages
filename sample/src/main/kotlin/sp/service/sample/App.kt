package sp.service.sample

import sp.kx.bytes.readBytes
import sp.kx.bytes.readInt
import sp.kx.bytes.readLong
import sp.kx.bytes.readUUID
import sp.kx.bytes.toByteArray
import sp.kx.bytes.writeBytes
import sp.kx.storages.MutableStorage
import sp.kx.storages.Payload
import sp.kx.storages.Storage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private class FinalStorage(
    private val delegate: File,
) : MutableStorage<String> {
    init {
        delegate.writeBytes(0.toByteArray())
    }

    private fun write(payloads: List<Payload<String>>) {
        val bytes = ByteArrayOutputStream().use { stream ->
            stream.writeBytes(payloads.size)
            payloads.forEach { payload ->
                stream.writeBytes(payload.id)
                stream.writeBytes(payload.created.inWholeMilliseconds)
                stream.writeBytes(payload.updated.inWholeMilliseconds)
                val bytes = payload.value.toByteArray()
                stream.writeBytes(bytes.size)
                stream.writeBytes(bytes)
            }
            stream.toByteArray()
        }
        delegate.writeBytes(bytes)
    }

    override fun delete(id: UUID): Boolean {
        val payloads = payloads.toMutableList()
        for (index in payloads.indices) {
            val it = payloads[index]
            if (it.id == id) {
                payloads.removeAt(index)
                write(payloads = payloads)
                return true
            }
        }
        return false
    }

    override fun add(value: String): Payload<String> {
        val created = System.currentTimeMillis().milliseconds
        val payload = Payload(
            id = UUID.randomUUID(),
            created = created,
            updated = created,
            value = value,
        )
        write(payloads = payloads + payload)
        return payload
    }

    override fun addAll(values: List<String>): List<Payload<String>> {
        val created = System.currentTimeMillis().milliseconds
        val newPayloads = values.map { value ->
            Payload(
                id = UUID.randomUUID(),
                created = created,
                updated = created,
                value = value,
            )
        }
        write(payloads = payloads + newPayloads)
        return newPayloads
    }

    override fun update(id: UUID, value: String): Duration? {
        val payloads = payloads.toMutableList()
        for (index in payloads.indices) {
            val it = payloads[index]
            if (it.id == id) {
                payloads.removeAt(index)
                val payload = Payload(
                    id = it.id,
                    created = it.created,
                    updated = System.currentTimeMillis().milliseconds,
                    value = value,
                )
                write(payloads = payloads + payload)
                return payload.updated
            }
        }
        return null
    }

    override val key = Storage.Key(id = UUID.randomUUID(), type = String::class.java)
    override val payloads: List<Payload<String>>
        get() {
            return ByteArrayInputStream(delegate.readBytes()).use { stream ->
                (0 until stream.readInt()).map { _ ->
                    val id = stream.readUUID()
                    val created = stream.readLong().milliseconds
                    val updated = stream.readLong().milliseconds
                    val bytes = stream.readBytes(stream.readInt())
                    Payload(
                        id = id,
                        created = created,
                        updated = updated,
                        value = String(bytes),
                    )
                }
            }
        }

    override fun get(id: UUID): Payload<String>? {
        return payloads.firstOrNull { it.id == id }
    }
}

fun main() {
    val storage: MutableStorage<String> = FinalStorage(File.createTempFile("foo", "bar"))
    println("storage: ${storage.key.id}")
    check(storage.payloads.isEmpty())
    val p0 = storage.add("foo")
    check(storage.payloads.size == 1)
    println("item: ${p0.id}")
    val p1 = storage.add("bar")
    check(storage.payloads.size == 2)
    println("item: ${p1.id}")
    val p2 = storage.add("baz")
    check(storage.payloads.size == 3)
    println("item: ${p2.id}")
    check(storage[p0.id]!!.value == "foo")
    check(storage[p1.id]!!.value == "bar")
    check(storage[p2.id]!!.value == "baz")
    val updated = storage.update(p0.id, "qux") ?: TODO()
    storage.delete(p2.id)
    check(storage.payloads.size == 2)
    check(storage[p0.id]!!.value == "qux")
    check(storage[p0.id]!!.updated == updated)
    check(storage[p1.id]!!.value == "bar")
    check(storage[UUID(0, 0)] == null)
    val newPayloads = storage.addAll(values = listOf("v1", "v2", "v3"))
    check(newPayloads.size == 3)
    newPayloads.forEach { expected ->
        val actual = storage[expected.id] ?: error("No payload!")
        check(expected.id == actual.id)
        check(expected.created == actual.created)
        check(expected.updated == actual.updated)
        check(expected.updated == actual.created)
        check(expected.value == actual.value)
    }
}
