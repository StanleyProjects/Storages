package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
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
            hash = "hash",
        )
        val item = "DescribedTest:createTest"
        val value = Described(
            id = id,
            info = info,
            item = item,
        )
        assertEquals(id, value.id)
        assertEquals(info, value.info)
        assertEquals(item, value.item)
    }

    @Test
    fun toStringTest() {
        val id = UUID.fromString("43518ed6-cda2-48c3-bd28-fed6fab80196")
        val info = ItemInfo(
            created = 1.milliseconds,
            updated = 2.milliseconds,
            hash = "hash",
        )
        val item = "DescribedTest:toStringTest"
        val value = Described(
            id = id,
            info = info,
            item = item,
        )
        assertEquals("{id: $id, info: $info, item: ${item::class.java.name}}", value.toString())
    }

    @Test
    fun equalsTest() {
        val id = UUID.fromString("43518ed6-cda2-48c3-bd28-fed6fab80196")
        val info = ItemInfo(
            created = 1.milliseconds,
            updated = 2.milliseconds,
            hash = "hash",
        )
        val item = "DescribedTest:equalsTest"
        val expected = Described(
            id = id,
            info = info,
            item = item,
        )
        val actual = Described(
            id = id,
            info = info,
            item = item,
        )
        assertEquals(expected, actual)
    }

    @Test
    fun equalsBytesTest() {
        val id = UUID.fromString("43518ed6-cda2-48c3-bd28-fed6fab80196")
        val info = ItemInfo(
            created = 1.milliseconds,
            updated = 2.milliseconds,
            hash = "hash",
        )
        val item = "DescribedTest:equalsBytesTest".toByteArray()
        val value = Described(
            id = id,
            info = info,
            item = item,
        )
        val equals = Described(
            id = id,
            info = info,
            item = item,
        )
        assertEquals(value, equals)
        val notEquals = Described(
            id = id,
            info = info,
            item = "notEquals".toByteArray(),
        )
        assertNotEquals(value, notEquals)
    }

    @Test
    fun notEqualsTest() {
        val id = UUID.fromString("43518ed6-cda2-48c3-bd28-fed6fab80196")
        val info = ItemInfo(
            created = 1.milliseconds,
            updated = 2.milliseconds,
            hash = "hash",
        )
        val item = "DescribedTest:notEqualsTest"
        val value = Described(
            id = id,
            info = info,
            item = item,
        )
        listOf(
            Described(
                id = UUID.fromString("57374f5c-2b76-4239-bed5-87ba25597fcf"),
                info = value.info,
                item = item,
            ),
            Described(
                id = id,
                info = ItemInfo(
                    created = 1.milliseconds,
                    updated = 2.milliseconds,
                    hash = "hash:notEquals",
                ),
                item = item,
            ),
            Described(
                id = id,
                info = value.info,
                item = "notEquals",
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
            hash = "hash",
        )
        val item = "DescribedTest:hashCodeTest"
        val value = Described(
            id = id,
            info = info,
            item = item,
        )
        val expected = Objects.hash(
            id,
            info,
            item,
        )
        assertEquals(expected, value.hashCode())
    }

    @Test
    fun copyTest() {
        val id = UUID.fromString("43518ed6-cda2-48c3-bd28-fed6fab80196")
        val info = ItemInfo(
            created = 1.milliseconds,
            updated = 2.milliseconds,
            hash = "hash",
        )
        val item = "DescribedTest:copyTest"
        val value = Described(
            id = id,
            info = info,
            item = item,
        )
        val updated = 3.milliseconds
        val copiedHash = "hash:copied"
        val copiedItem = "copied"
        val copied = value.copy(
            updated = updated,
            hash = copiedHash,
            item = copiedItem,
        )
        assertNotEquals(value, copied)
        assertEquals(id, copied.id)
        assertEquals(value.info.created, copied.info.created)
        assertEquals(updated, copied.info.updated)
        assertEquals(copiedHash, copied.info.hash)
        assertEquals(copiedItem, copied.item)
    }

    @Test
    fun mapTest() {
        TODO("DescribedTest:mapTest")
    }
}
