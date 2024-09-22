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
    values: Map<Metadata, ByteArray> = emptyMap(),
) : MutableFileStorage {
    private val values: MutableMap<Metadata, ByteArray> = values.toMutableMap()

    override fun delete(id: UUID): Boolean {
        val raw = values.entries.firstOrNull { (raw, _) -> raw.id == id }?.key ?: return false
        check(values.remove(raw) != null)
        return true
    }

    override fun add(bytes: ByteArray): Metadata {
        val created = timeProvider.provide()
        val meta = Metadata(
            id = uuidProvider.provide(),
            created = created,
            info = mockItemInfo(
                updated = created,
                hash = hf.map(bytes = bytes),
                size = bytes.size,
            ),
        )
        values[meta] = bytes
        return meta
    }

    override fun update(id: UUID, bytes: ByteArray): ItemInfo? {
        val oldRaw = values.entries.firstOrNull { (raw, _) -> raw.id == id }?.key ?: return null
        check(values.remove(oldRaw) != null)
        val newRaw = oldRaw.copy(
            updated = timeProvider.provide(),
            hash = hf.map(bytes),
            size = bytes.size,
        )
        values[newRaw] = bytes
        return newRaw.info
    }

    override val hash: ByteArray
        get() {
            val bytes = values.keys.map { it.id.toByteArray() + it.info.hash }.flatMap { it.toList() }.toByteArray()
            return hf.map(bytes)
        }

    override val items: List<Metadata>
        get() {
            return values.keys.toList()
        }

    override fun getBytes(id: UUID): ByteArray {
        return values.entries.firstOrNull { (raw, _) -> raw.id == id }?.value ?: TODO("MockMutableFileStorage:getBytes($id)")
    }
}
