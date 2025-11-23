package sp.service.sample

import sp.kx.storages.MutableStorage
import sp.kx.storages.MutableStorages
import sp.kx.storages.Payload
import sp.kx.storages.Storage
import sp.kx.storages.Transaction
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private class FinalStorages : MutableStorages {
    private val k0 = Storage.Key(id = UUID(42, 0), type = String::class.java)
    private val k1 = Storage.Key(id = UUID(42, 1), type = Duration::class.java)
    private val p0 = mutableListOf<Payload<String>>()
    private val p1 = mutableListOf<Payload<Duration>>()
    private val s0 = FinalStorage(k0.id, p0)
    private val s1 = FinalStorage(k1.id, p1)

    override fun <T : Any> get(key: Storage.Key<T>): MutableStorage<T>? {
        return when (key) {
            k0 -> s0 as MutableStorage<T>
            k1 -> s1 as MutableStorage<T>
            else -> null
        }
    }

    private fun <T : Any> deleteFirst(
        payloads: MutableList<Payload<T>>,
        operation: Transaction.Operation.DeleteFirst<*>,
    ) {
        val condition: (Payload<T>) -> Boolean = operation.condition as (Payload<T>) -> Boolean
        for (index in payloads.indices) {
            val payload = payloads[index]
            if (condition(payload)) {
                payloads.removeAt(index)
                break
            }
        }
    }

    override fun commit(transaction: Transaction) {
        val _p0 = p0.toMutableList()
        val _p1 = p1.toMutableList()
        for (operation in transaction.operations) {
            when (operation) {
                is Transaction.Operation.Add<*> -> {
                    val created = System.currentTimeMillis().milliseconds
                    when (operation.key) {
                        k0 -> {
                            val payload = Payload(
                                id = UUID.randomUUID(),
                                created = created,
                                updated = created,
                                value = operation.value as String,
                            )
                            _p0.add(payload)
                        }
                        k1 -> {
                            val payload = Payload(
                                id = UUID.randomUUID(),
                                created = created,
                                updated = created,
                                value = operation.value as Duration,
                            )
                            _p1.add(payload)
                        }
                        else -> error("No storage!")
                    }
                }
                is Transaction.Operation.Delete<*> -> {
                    when (operation.key) {
                        k0 -> {
                            for (index in _p0.indices) {
                                val it = _p0[index]
                                if (it.id == operation.id) {
                                    _p0.removeAt(index)
                                    break
                                }
                            }
                        }
                        k1 -> {
                            for (index in _p1.indices) {
                                val it = _p1[index]
                                if (it.id == operation.id) {
                                    _p1.removeAt(index)
                                    break
                                }
                            }
                        }
                        else -> error("No storage!")
                    }
                }
                is Transaction.Operation.DeleteFirst<*> -> {
                    when (operation.key) {
                        k0 -> deleteFirst(_p0, operation)
                        k1 -> deleteFirst(_p1, operation)
                        else -> error("No storage!")
                    }
                }
                is Transaction.Operation.Update<*> -> TODO()
                is Transaction.Operation.UpdateFirst<*> -> TODO()
            }
        }
        p0.clear()
        p0.addAll(_p0)
        p1.clear()
        p1.addAll(_p1)
    }
}

private class FinalStorage<T : Any>(
    override val id: UUID,
    override val payloads: MutableList<Payload<T>>,
) : MutableStorage<T> {
    private fun write(payloads: List<Payload<T>>) {
        this.payloads.clear()
        this.payloads.addAll(payloads)
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

    override fun add(value: T): Payload<T> {
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

    override fun addAll(values: List<T>): List<Payload<T>> {
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

    override fun update(id: UUID, value: T): Duration? {
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

    override fun get(id: UUID): Payload<T>? {
        return payloads.firstOrNull { it.id == id }
    }
}

fun main() {
    val storages: MutableStorages = FinalStorages()
    val strings = Storage.Key(UUID(42, 0), String::class.java)
    val durations = Storage.Key(UUID(42, 1), Duration::class.java)
    var transaction = Transaction.Builder()
        .add(strings, "foo")
        .add(strings, "bar")
        .add(durations, 42.seconds)
        .add(durations, 43.seconds)
        .build()
    check(storages[strings]!!.payloads.isEmpty())
    check(storages[durations]!!.payloads.isEmpty())
    storages.commit(transaction = transaction)
    check(storages[strings]!!.payloads.size == 2)
    check(storages[strings]!!.payloads.map { it.value } == listOf("foo", "bar"))
    check(storages[durations]!!.payloads.size == 2)
    check(storages[durations]!!.payloads.map { it.value } == listOf(42.seconds, 43.seconds))
    val p00 = storages[strings]!!.payloads.firstOrNull { it.value == "foo" } ?: error("No payload!")
    val p10 = storages[durations]!!.payloads.firstOrNull { it.value == 42.seconds } ?: error("No payload!")
    transaction = Transaction.Builder()
        .delete(strings, p00.id)
        .add(strings, "baz")
        .delete(durations, p10.id)
        .build()
    storages.commit(transaction = transaction)
    check(storages[strings]!!.payloads.size == 2)
    check(storages[strings]!!.payloads.map { it.value } == listOf("bar", "baz"))
    check(storages[durations]!!.payloads.size == 1)
    check(storages[durations]!!.payloads.map { it.value } == listOf(43.seconds))
    transaction = Transaction.Builder()
        .deleteFirst(strings) { it.value == "bar" }
        .build()
    storages.commit(transaction = transaction)
    check(storages[strings]!!.payloads.size == 1)
    check(storages[strings]!!.payloads.map { it.value } == listOf("baz"))
}

fun main0() {
    val storages: MutableStorages = FinalStorages()
    val storage = storages[Storage.Key(UUID(42, 0), String::class.java)] ?: error("No storage!")
    println("storage: ${storage.id}")
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
