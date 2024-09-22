package sp.kx.storages

import sp.kx.bytes.toByteArray
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class MockMutableFileStorage(
    override val id: UUID = mockUUID(),
    private val hf: HashFunction = MockHashFunction(),
    private val uuidProvider: MockProvider<UUID> = MockProvider { mockUUID(1) },
    private val timeProvider: MockProvider<Duration> = MockProvider { 1.milliseconds },
    values: Map<Raw, ByteArray> = emptyMap(),
) : MutableFileStorage {
    private val values: MutableMap<Raw, ByteArray> = values.toMutableMap()

    override fun delete(id: UUID): Boolean {
        val raw = values.entries.firstOrNull { (raw, _) -> raw.id == id }?.key ?: return false
        check(values.remove(raw) != null)
        return true
    }

    override fun add(bytes: ByteArray): Raw {
        val raw = Raw(
            id = uuidProvider.provide(),
            info = mockItemInfo(
                created = timeProvider.provide(),
                updated = timeProvider.provide(),
                hash = hf.map(bytes = bytes),
            ),
        )
        values[raw] = bytes
        return raw
    }

    override fun update(id: UUID, bytes: ByteArray): ItemInfo? {
        val oldRaw = values.entries.firstOrNull { (raw, _) -> raw.id == id }?.key ?: return null
        check(values.remove(oldRaw) != null)
        val newRaw = oldRaw.copy(updated = timeProvider.provide(), hash = hf.map(bytes))
        values[newRaw] = bytes
        return newRaw.info
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
        return values.entries.firstOrNull { (raw, _) -> raw.id == id }?.value ?: TODO("MockMutableFileStorage:getBytes($id)")
    }
}
