package sp.kx.storages

import java.util.UUID

interface MutableStorages : Storages {
    override operator fun get(id: UUID): MutableStorage<out Any>?
    override fun <T : Any> get(type: Class<T>): MutableStorage<T>?
}
