package sp.kx.storages

import java.util.UUID

internal class MockStorages(
    private val storages: List<Storage<out Any>> = emptyList(),
) : Storages {
    override fun get(id: UUID): Storage<out Any>? {
        return storages.firstOrNull { it.id == id }
    }

    override fun <T : Any> get(type: Class<T>): Storage<T>? {
        TODO("MockStorages:get")
    }
}
