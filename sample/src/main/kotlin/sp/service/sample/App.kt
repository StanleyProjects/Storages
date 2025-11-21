package sp.service.sample

import sp.kx.storages.MutableStorage
import sp.kx.storages.MutableStorages
import sp.kx.storages.Payload
import sp.kx.storages.Storage
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private class FinalStorages : MutableStorages {
    private val storages = listOf(
        FinalStorage(id = UUID(42, 0)),
    )

    override fun <T : Any> get(key: Storage.Key<T>): MutableStorage<T>? {
        val storage = storages.firstOrNull { it.key == key } ?: return null
        return storage as MutableStorage<T>
    }
}

private class FinalStorage(id: UUID) : MutableStorage<String> {
    private var _payloads = emptyList<Payload<String>>()

    private fun write(payloads: List<Payload<String>>) {
        _payloads = payloads
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

    override val key = Storage.Key(id = id, type = String::class.java)
    override val payloads: List<Payload<String>>
        get() { return _payloads }

    override fun get(id: UUID): Payload<String>? {
        return payloads.firstOrNull { it.id == id }
    }
}

fun main() {
    val storages: MutableStorages = FinalStorages()
    val storage = storages[Storage.Key(UUID(42, 0), String::class.java)] ?: error("No storage!")
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
