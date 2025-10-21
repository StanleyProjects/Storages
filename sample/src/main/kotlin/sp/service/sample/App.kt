package sp.service.sample

import sp.kx.bytes.readBytes
import sp.kx.bytes.readInt
import sp.kx.bytes.readLong
import sp.kx.bytes.readUUID
import sp.kx.bytes.toByteArray
import sp.kx.bytes.writeBytes
import sp.kx.storages.MutableStorage
import sp.kx.storages.Payload
import sp.kx.storages.ValueInfo
import sp.kx.storages.ValueState
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

private class FinalStorage(
    private val delegate: File,
) : MutableStorage<String> {
    private val md = MessageDigest.getInstance("MD5")

    init {
        delegate.writeBytes(0.toByteArray())
    }

    private fun write(payloads: List<Payload<String>>) {
        val bytes = ByteArrayOutputStream().use { stream ->
            stream.writeBytes(payloads.size)
            payloads.forEachIndexed { index, payload ->
                stream.writeBytes(payload.valueInfo.id)
                stream.writeBytes(payload.valueInfo.created.inWholeMilliseconds)
                stream.writeBytes(payload.valueState.updated.inWholeMilliseconds)
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
            if (it.valueInfo.id == id) {
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
            value = value,
            valueInfo = ValueInfo(
                id = UUID.randomUUID(),
                created = created,
            ),
            valueState = ValueState(
                updated = created,
                hash = md.digest(value.toByteArray()),
            ),
        )
        write(payloads = payloads + payload)
        return payload
    }

    override fun update(id: UUID, value: String): ValueState? {
        val payloads = payloads.toMutableList()
        for (index in payloads.indices) {
            val it = payloads[index]
            if (it.valueInfo.id == id) {
                payloads.removeAt(index)
                val valueState = ValueState(
                    updated = System.currentTimeMillis().milliseconds,
                    hash = md.digest(value.toByteArray()),
                )
                val payload = Payload(
                    value = value,
                    valueInfo = it.valueInfo,
                    valueState = valueState,
                )
                write(payloads = payloads + payload)
                return valueState
            }
        }
        return null
    }

    override val id: UUID = UUID.randomUUID()
    override val payloads: List<Payload<String>>
        get() {
            return ByteArrayInputStream(delegate.readBytes()).use { stream ->
                (0 until stream.readInt()).map { index ->
                    val valueInfo = ValueInfo(
                        id = stream.readUUID(),
                        created = stream.readLong().milliseconds,
                    )
                    val updated = stream.readLong().milliseconds
                    val bytes = stream.readBytes(stream.readInt())
                    Payload(
                        value = String(bytes),
                        valueInfo = valueInfo,
                        valueState = ValueState(
                            updated = updated,
                            hash = md.digest(bytes),
                        ),
                    )
                }
            }
        }

    override fun get(id: UUID): Payload<String>? {
        return payloads.firstOrNull { it.valueInfo.id == id }
    }
}

fun main() {
    val storage: MutableStorage<String> = FinalStorage(File.createTempFile("foo", "bar"))
    println("storage: ${storage.id}")
    check(storage.payloads.isEmpty())
    val p0 = storage.add("foo")
    check(storage.payloads.size == 1)
    println("item: ${p0.valueInfo}")
    val p1 = storage.add("bar")
    check(storage.payloads.size == 2)
    println("item: ${p1.valueInfo}")
    val p2 = storage.add("baz")
    check(storage.payloads.size == 3)
    println("item: ${p2.valueInfo}")
    check(storage[p0.valueInfo.id]!!.value == "foo")
    check(storage[p1.valueInfo.id]!!.value == "bar")
    check(storage[p2.valueInfo.id]!!.value == "baz")
    storage.update(p0.valueInfo.id, "qux") ?: TODO()
    storage.delete(p2.valueInfo.id)
    check(storage.payloads.size == 2)
    check(storage[p0.valueInfo.id]!!.value == "qux")
    check(storage[p1.valueInfo.id]!!.value == "bar")
    check(storage[UUID(0, 0)] == null)
}
