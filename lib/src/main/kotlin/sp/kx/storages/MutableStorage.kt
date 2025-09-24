package sp.kx.storages

import java.util.UUID

interface MutableStorage<T : Any> : Storage<T> {
    fun delete(id: UUID): Boolean
    fun add(value: T): Payload<T>
    operator fun set(id: UUID, value: T): ValueState?
}
