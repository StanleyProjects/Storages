package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import sp.kx.bytes.toByteArray
import sp.kx.bytes.toHEX
import kotlin.time.Duration.Companion.milliseconds

internal class SyncResponseTransformerTest {
    @Test
    fun encodeTest() {
        val hf = MockHashFunction()
        val transformer = SyncResponseTransformer(hf = hf)
        val src = MockHashFunction.map("1")
        val dst = MockHashFunction.map("2")
        val infos = mapOf(
            mockUUID(10) to mockSyncInfo(
                infos = mapOf(
                    mockUUID(11) to mockItemInfo(
                        created = 111.milliseconds,
                        updated = 112.milliseconds,
                        hash = MockHashFunction.map("1111"),
                    ),
                ),
                deleted = setOf(mockUUID(12)),
            ),
            mockUUID(20) to mockSyncInfo(
                infos = mapOf(
                    mockUUID(21) to mockItemInfo(
                        created = 211.milliseconds,
                        updated = 212.milliseconds,
                        hash = MockHashFunction.map("2111"),
                    ),
                    mockUUID(22) to mockItemInfo(
                        created = 221.milliseconds,
                        updated = 222.milliseconds,
                        hash = MockHashFunction.map("2211"),
                    ),
                )
            ),
        )
        val decoded = mockSyncResponse(
            session = mockSyncSession(src = src, dst = dst),
            infos = infos,
        )
        val expected = src + dst + infos.size.toByteArray() + infos.map { (id0, syncInfo) ->
            id0.toByteArray() +
                syncInfo.infos.size.toByteArray() +
                syncInfo.infos.map { (id1, itemInfo) ->
                    id1.toByteArray() +
                        itemInfo.created.inWholeMilliseconds.toByteArray() +
                        itemInfo.updated.inWholeMilliseconds.toByteArray() +
                        itemInfo.hash
                }.flatMap { it.toList() }.toByteArray() +
                syncInfo.deleted.size.toByteArray() +
                syncInfo.deleted.map { it.toByteArray() }.flatMap { it.toList() }.toByteArray()
        }.flatMap { it.toList() }.toByteArray()
        val actual = transformer.encode(decoded = decoded)
        assertEquals(expected.size, actual.size, "expected: ${expected.toHEX()}, actual: ${actual.toHEX()}")
        assertEquals(expected.toHEX(), actual.toHEX())
        assertTrue(expected.contentEquals(actual))
    }

    @Test
    fun decodeTest() {
        val hf = MockHashFunction()
        val transformer = SyncResponseTransformer(hf = hf)
        val src = MockHashFunction.map("1")
        val dst = MockHashFunction.map("2")
        val infos = mapOf(
            mockUUID(10) to mockSyncInfo(
                infos = mapOf(
                    mockUUID(11) to mockItemInfo(
                        created = 111.milliseconds,
                        updated = 112.milliseconds,
                        hash = MockHashFunction.map("1111"),
                    ),
                ),
                deleted = setOf(mockUUID(12)),
            ),
            mockUUID(20) to mockSyncInfo(
                infos = mapOf(
                    mockUUID(21) to mockItemInfo(
                        created = 211.milliseconds,
                        updated = 212.milliseconds,
                        hash = MockHashFunction.map("2111"),
                    ),
                    mockUUID(22) to mockItemInfo(
                        created = 221.milliseconds,
                        updated = 222.milliseconds,
                        hash = MockHashFunction.map("2211"),
                    ),
                )
            ),
        )
        val decoded = mockSyncResponse(
            session = mockSyncSession(src = src, dst = dst),
            infos = infos,
        )
        val encoded = src + dst + infos.size.toByteArray() + infos.map { (id0, syncInfo) ->
            id0.toByteArray() +
                syncInfo.infos.size.toByteArray() +
                syncInfo.infos.map { (id1, itemInfo) ->
                    id1.toByteArray() +
                        itemInfo.created.inWholeMilliseconds.toByteArray() +
                        itemInfo.updated.inWholeMilliseconds.toByteArray() +
                        itemInfo.hash
                }.flatMap { it.toList() }.toByteArray() +
                syncInfo.deleted.size.toByteArray() +
                syncInfo.deleted.map { it.toByteArray() }.flatMap { it.toList() }.toByteArray()
        }.flatMap { it.toList() }.toByteArray()
        val actual = transformer.decode(encoded = encoded)
        assertEquals(decoded, actual)
    }
}
