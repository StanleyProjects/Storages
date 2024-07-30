package sp.kx.storages

import java.util.UUID

internal class MockStorage<T : Any>(
    override val id: UUID = mockUUID(),
    override val hash: ByteArray = MockHashFunction.map("$id"),
    override val items: List<Described<T>> = emptyList(),
) : Storage<T>
