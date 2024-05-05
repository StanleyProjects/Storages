package sp.kx.storages

import java.util.UUID

// todo storages
interface Storage<T : Any> {
    val id: UUID
    val hash: String
    val items: List<Described<T>>
}
