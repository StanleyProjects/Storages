package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
}
