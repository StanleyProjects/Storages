package sp.kx.storages

import java.util.UUID

interface Storage<T : Any> : Items<T> {
    val id: UUID
    val hash: String
}
