package sp.kx.storages

import sp.kx.bytes.toByteArray
import java.util.UUID

internal class MockFileStorage(
    override val id: UUID = mockUUID(),
    private val hf: HashFunction = MockHashFunction(),
    private val values: Map<Metadata, ByteArray> = emptyMap(),
) : FileStorage {
    override val hash: ByteArray
        get() {
            val bytes = values.keys.map { it.id.toByteArray() + it.info.hash }.flatMap { it.toList() }.toByteArray()
            return hf.map(bytes)
        }
    override val items: List<Metadata> = values.keys.toList()

    override fun getBytes(id: UUID): ByteArray {
        return values.entries.firstOrNull { (raw, _) -> raw.id == id }?.value ?: TODO()
    }
}
