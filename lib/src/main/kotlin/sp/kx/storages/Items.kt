package sp.kx.storages

import java.util.UUID

interface Items<T : Any> {
    val list: List<Described<T>>
    val deleted: Set<UUID>
}
