package sp.kx.storages

import java.util.UUID
import kotlin.time.Duration

interface MutableStorage<T : Any> : Storage<T> {
    fun delete(id: UUID): Boolean
    fun add(value: T): Payload<T>
    fun addAll(values: List<T>): Payload<T>
    fun update(id: UUID, value: T): Duration?
}
