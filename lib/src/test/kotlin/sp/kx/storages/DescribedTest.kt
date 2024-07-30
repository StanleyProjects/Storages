package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.util.Objects
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal class DescribedTest {
    @Test
    fun createTest() {
        val id = UUID.fromString("43518ed6-cda2-48c3-bd28-fed6fab80196")
        val info = ItemInfo(
            created = 1.milliseconds,
            updated = 2.milliseconds,
            hash = "hash".toByteArray(),
        )
        val payload = "DescribedTest:createTest"
        val value = Described(
            id = id,
            info = info,
            payload = payload,
        )
        assertEquals(id, value.id)
        assertEquals(info, value.info)
        assertEquals(payload, value.payload)
    }

    @Test
    fun toStringTest() {
        val id = UUID.fromString("43518ed6-cda2-48c3-bd28-fed6fab80196")
        val info = ItemInfo(
            created = 1.milliseconds,
            updated = 2.milliseconds,
            hash = "hash".toByteArray(),
        )
        val payload = "DescribedTest:toStringTest"
        val value = Described(
            id = id,
            info = info,
            payload = payload,
        )
        assertEquals("{id: $id, info: $info, payload: ${payload::class.java.name}}", value.toString())
    }

    @Test
    fun equalsTest() {
        val id = UUID.fromString("43518ed6-cda2-48c3-bd28-fed6fab80196")
        val info = ItemInfo(
            created = 1.milliseconds,
            updated = 2.milliseconds,
            hash = "hash".toByteArray(),
        )
        val payload = "DescribedTest:equalsTest"
        val expected = Described(
            id = id,
            info = info,
            payload = payload,
        )
        val actual = Described(
            id = id,
            info = info,
            payload = payload,
        )
        assertEquals(expected, actual)
    }

    @Test
    fun equalsBytesTest() {
        val id = UUID.fromString("43518ed6-cda2-48c3-bd28-fed6fab80196")
        val info = ItemInfo(
            created = 1.milliseconds,
            updated = 2.milliseconds,
            hash = "hash".toByteArray(),
        )
        val payload = "DescribedTest:equalsBytesTest".toByteArray()
        val value = Described(
            id = id,
            info = info,
            payload = payload,
        )
        val equals = Described(
            id = id,
            info = info,
            payload = payload,
        )
        assertEquals(value, equals)
        val notEquals = Described(
            id = id,
            info = info,
            payload = "notEquals".toByteArray(),
        )
        assertNotEquals(value, notEquals)
    }

    @Test
    fun notEqualsTest() {
        val id = UUID.fromString("43518ed6-cda2-48c3-bd28-fed6fab80196")
        val info = ItemInfo(
            created = 1.milliseconds,
            updated = 2.milliseconds,
            hash = "hash".toByteArray(),
        )
        val payload = "DescribedTest:notEqualsTest"
        val value = Described(
            id = id,
            info = info,
            payload = payload,
        )
        listOf(
            Described(
                id = UUID.fromString("57374f5c-2b76-4239-bed5-87ba25597fcf"),
                info = value.info,
                payload = payload,
            ),
            Described(
                id = id,
                info = ItemInfo(
                    created = 1.milliseconds,
                    updated = 2.milliseconds,
                    hash = "hash:notEquals".toByteArray(),
                ),
                payload = payload,
            ),
            Described(
                id = id,
                info = value.info,
                payload = "notEquals",
            ),
        ).forEach { notEquals ->
            assertNotEquals(value, notEquals)
        }
    }

    @Test
    fun hashCodeTest() {
        val id = UUID.fromString("43518ed6-cda2-48c3-bd28-fed6fab80196")
        val info = ItemInfo(
            created = 1.milliseconds,
            updated = 2.milliseconds,
            hash = "hash".toByteArray(),
        )
        val payload = "DescribedTest:hashCodeTest"
        val value = Described(
            id = id,
            info = info,
            payload = payload,
        )
        val expected = Objects.hash(
            id,
            info,
            payload,
        )
        assertEquals(expected, value.hashCode())
    }

    @Test
    fun hashCodeByteArrayTest() {
        val id = UUID.fromString("43518ed6-cda2-48c3-bd28-fed6fab80196")
        val info = ItemInfo(
            created = 1.milliseconds,
            updated = 2.milliseconds,
            hash = "hash".toByteArray(),
        )
        val payload = "DescribedTest:hashCodeTest".toByteArray()
        val value = Described(
            id = id,
            info = info,
            payload = payload,
        )
        val expected = Objects.hash(
            id,
            info,
            payload.contentHashCode(),
        )
        assertEquals(expected, value.hashCode())
    }

    @Test
    fun copyTest() {
        val id = UUID.fromString("43518ed6-cda2-48c3-bd28-fed6fab80196")
        val info = ItemInfo(
            created = 1.milliseconds,
            updated = 2.milliseconds,
            hash = "hash".toByteArray(),
        )
        val payload = "DescribedTest:copyTest"
        val value = Described(
            id = id,
            info = info,
            payload = payload,
        )
        val updated = 3.milliseconds
        val copiedHash = "hash:copied".toByteArray()
        val copiedPayload = "copied"
        val copied = value.copy(
            updated = updated,
            hash = copiedHash,
            payload = copiedPayload,
        )
        assertNotEquals(value, copied)
        assertEquals(id, copied.id)
        assertEquals(value.info.created, copied.info.created)
        assertEquals(updated, copied.info.updated)
        assertEquals(copiedHash, copied.info.hash)
        assertEquals(copiedPayload, copied.payload)
    }

    @Test
    fun mapTest() {
        val info = ItemInfo(
            created = 1.milliseconds,
            updated = 2.milliseconds,
            hash = "hash".toByteArray(),
        )
        val payloadMapped = 42
        val value = Described(
            id = mockUUID(1),
            info = info,
            payload = "DescribedTest:mapTest",
        )
        val mapped = value.map { payloadMapped }
        assertEquals(value.id, mapped.id)
        assertEquals(value.info, mapped.info)
        assertEquals(mapped.payload, payloadMapped)
    }

    @Test
    fun bytesTest() {
        val expected = mockDescribed(payload = "f1".toByteArray())
        val actual = mockDescribed(payload = "f2")
        assertFalse(expected == actual)
    }

    @Test
    fun equalsNotTest() {
        val expected: Any = mockDescribed(payload = "f1".toByteArray())
        val actual: Any = "f2"
        assertFalse(expected == actual)
    }
}
