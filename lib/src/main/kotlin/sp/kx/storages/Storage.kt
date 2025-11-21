package sp.kx.storages

import java.util.UUID

interface Storage<T : Any> {
    data class Key<T : Any>(val id: UUID, val type: Class<T>)

    val key: Key<T>
    val payloads: List<Payload<T>>

    operator fun get(id: UUID): Payload<T>?
}
