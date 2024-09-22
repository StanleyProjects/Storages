package sp.kx.storages

import sp.kx.bytes.toByteArray
import java.util.UUID

internal class MockMutableFileStorage(
    override val id: UUID = mockUUID(),
    private val hf: HashFunction = MockHashFunction(),
    values: Map<Raw, ByteArray> = emptyMap(),
) : MutableFileStorage {
    private val values: MutableMap<Raw, ByteArray> = values.toMutableMap()

    override fun delete(id: UUID): Boolean {
        val raw = values.entries.firstOrNull { (raw, _) -> raw.id == id }?.key ?: return false
        check(values.remove(raw) != null)
        return true
    }

    override fun add(bytes: ByteArray): Raw {
        TODO("add")
    }

    override fun update(id: UUID, bytes: ByteArray): ItemInfo? {
        TODO("update")
    }

    override val hash: ByteArray
        get() {
            val bytes = values.keys.map { it.id.toByteArray() + it.info.hash }.flatMap { it.toList() }.toByteArray()
            return hf.map(bytes)
        }

    override val items: List<Raw>
        get() {
            return values.keys.toList()
        }

    override fun getBytes(id: UUID): ByteArray {
        return values.entries.firstOrNull { (raw, _) -> raw.id == id }?.value ?: TODO()
    }
}
