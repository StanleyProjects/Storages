package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

internal class FileStreamerProviderTest {
    @Test
    fun existsTest() {
        val dateFormat = SimpleDateFormat("yyyyMMdd")
        val dir = File("/tmp/${dateFormat.format(Date())}")
        dir.deleteRecursively()
        dir.mkdirs()
        val id = mockUUID(1)
        val file = File(dir, "pointers")
        assertFalse(file.exists())
        val pointer = FileStreamerProvider(dir = dir, ids = setOf(id)).getPointer(id = id)
        assertEquals(0, pointer)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
    }

    @Test
    fun lengthTest() {
        val dateFormat = SimpleDateFormat("yyyyMMdd")
        val dir = File("/tmp/${dateFormat.format(Date())}")
        dir.deleteRecursively()
        dir.mkdirs()
        val id = mockUUID(1)
        val file = File(dir, "pointers")
        file.createNewFile()
        assertTrue(file.exists())
        assertEquals(0L, file.length())
        val pointer = FileStreamerProvider(dir = dir, ids = setOf(id)).getPointer(id = id)
        assertEquals(0, pointer)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
    }

    @Test
    fun emptyTest() {
        val dateFormat = SimpleDateFormat("yyyyMMdd")
        val dir = File("/tmp/${dateFormat.format(Date())}")
        dir.deleteRecursively()
        dir.mkdirs()
        val id = mockUUID(1)
        val file = File(dir, "pointers")
        file.createNewFile()
        assertTrue(file.exists())
        file.writeBytes(ByteArray(4))
        assertEquals(4L, file.length())
        val streamerProvider = FileStreamerProvider(dir = dir, ids = emptySet())
        val expected = 42
        streamerProvider.putPointers(values = mapOf(id to expected))
        val actual = streamerProvider.getPointer(id = id)
        assertEquals(expected, actual)
    }

    @Test
    fun getErrorTest() {
        val dateFormat = SimpleDateFormat("yyyyMMdd")
        val dir = File("/tmp/${dateFormat.format(Date())}")
        dir.deleteRecursively()
        dir.mkdirs()
        val id1 = mockUUID(1)
        val id2 = mockUUID(2)
        check(id1 != id2)
        val streamerProvider = FileStreamerProvider(dir = dir, ids = setOf(id1))
        assertThrows(IllegalStateException::class.java) {
            streamerProvider.getPointer(id = id2)
        }
    }

    @Test
    fun putPointersDirectoryTest() {
        val dateFormat = SimpleDateFormat("yyyyMMdd")
        val dir = File("/tmp/${dateFormat.format(Date())}")
        dir.deleteRecursively()
        dir.mkdirs()
        val file = File(File(dir, "storages"), "foo")
        file.mkdirs()
        check(file.exists())
        check(file.isDirectory)
        val id = mockUUID(1)
        val streamerProvider = FileStreamerProvider(dir = dir, ids = setOf(id))
        streamerProvider.putPointers(values = mapOf(id to 2))
        assertTrue(file.exists())
        assertTrue(file.isDirectory)
    }
}
