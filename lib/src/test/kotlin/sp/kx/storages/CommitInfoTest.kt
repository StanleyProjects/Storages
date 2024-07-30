package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Objects

internal class CommitInfoTest {
    @Test
    fun toStringTest() {
        val item = Described(
            id = mockUUID(pointer = 1),
            info = mockItemInfo(),
            payload = byteArrayOf(
                0x05.toByte(),
                0x18.toByte(),
                0x9e.toByte(),
                0xe5.toByte(),
            ),
        )
        val commitInfo = CommitInfo(
            hash = byteArrayOf(
                0x01.toByte(),
                0x6e.toByte(),
                0x9e.toByte(),
                0xd6.toByte(),
            ),
            items = listOf(item),
            deleted = setOf(mockUUID(pointer = 2)),
        )
        val expected = "{" +
            "items: [$item], " +
            "deleted: [cbae2ba0-6be9-40f5-b565-d6152a120002], " +
            "hash: \"016e9ed6\"" +
            "}"
        assertEquals(expected, commitInfo.toString())
    }

    @Test
    fun equalsTest() {
        val i11 = CommitInfo(
            hash = MockHashFunction.map("113"),
            items = listOf(mockDescribed(pointer = 111).map { it.toByteArray() }),
            deleted = setOf(mockUUID(pointer = 112)),
        )
        val i12 = CommitInfo(
            hash = MockHashFunction.map("113"),
            items = listOf(mockDescribed(pointer = 111).map { it.toByteArray() }),
            deleted = setOf(mockUUID(pointer = 112)),
        )
        assertEquals(i11, i12)
        assertTrue(i11 == i12)
        val i21 = CommitInfo(
            hash = MockHashFunction.map("213"),
            items = listOf(mockDescribed(pointer = 211).map { it.toByteArray() }),
            deleted = setOf(mockUUID(pointer = 212)),
        )
        assertNotEquals(i11, i21)
        assertNotEquals(i12, i21)
        assertFalse(i11 == i21)
        assertFalse(i12 == i21)
        val i22 = CommitInfo(
            hash = MockHashFunction.map("113"),
            items = listOf(mockDescribed(pointer = 211).map { it.toByteArray() }),
            deleted = setOf(mockUUID(pointer = 212)),
        )
        assertNotEquals(i11, i22)
        assertNotEquals(i12, i22)
        assertFalse(i11 == i22)
        assertFalse(i12 == i22)
        val i23 = CommitInfo(
            hash = MockHashFunction.map("113"),
            items = listOf(mockDescribed(pointer = 111).map { it.toByteArray() }),
            deleted = setOf(mockUUID(pointer = 212)),
        )
        assertNotEquals(i11, i23)
        assertNotEquals(i12, i23)
        assertFalse(i11 == i23)
        assertFalse(i12 == i23)
        val i24 = CommitInfo(
            hash = MockHashFunction.map("113"),
            items = listOf(mockDescribed(pointer = 211).map { it.toByteArray() }),
            deleted = setOf(mockUUID(pointer = 112)),
        )
        assertNotEquals(i11, i24)
        assertNotEquals(i12, i24)
        assertFalse(i11 == i24)
        assertFalse(i12 == i24)
        assertNotEquals(i11, "foobar")
    }

    @Test
    fun hashCodeTest() {
        val commitInfo = CommitInfo(
            hash = MockHashFunction.map("113"),
            items = listOf(mockDescribed(pointer = 111).map { it.toByteArray() }),
            deleted = setOf(mockUUID(pointer = 112)),
        )
        val expected = Objects.hash(
            MockHashFunction.map("113").contentHashCode(),
            listOf(mockDescribed(pointer = 111).map { it.toByteArray() }),
            setOf(mockUUID(pointer = 112)),
        )
        assertEquals(expected, commitInfo.hashCode())
    }
}
