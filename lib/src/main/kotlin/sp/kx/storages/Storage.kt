package sp.kx.storages

import java.util.UUID

interface Storage<T : Any> {
    val id: UUID
    val items: List<Payload<T>>

    operator fun get(id: UUID): Payload<T>?
}
