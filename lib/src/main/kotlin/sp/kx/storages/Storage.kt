package sp.kx.storages

import java.util.UUID

interface Storage<T : Any> {
    val id: UUID
    val hash: String
    val items: List<Described<T>>
    val deleted: Set<UUID>
}
