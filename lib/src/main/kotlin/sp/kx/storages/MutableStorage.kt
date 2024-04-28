package sp.kx.storages

import java.util.UUID

interface MutableStorage<T : Any> : Storage<T> {
    fun delete(id: UUID)
    fun add(item: T)
    fun update(id: UUID, item: T)
}
