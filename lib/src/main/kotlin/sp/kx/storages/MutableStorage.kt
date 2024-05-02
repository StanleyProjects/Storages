package sp.kx.storages

import java.util.UUID

interface MutableStorage<T : Any> : Storage<T> {
    fun delete(id: UUID): Boolean
    fun add(item: T): Described<T>
    fun update(id: UUID, item: T): ItemInfo?
}
