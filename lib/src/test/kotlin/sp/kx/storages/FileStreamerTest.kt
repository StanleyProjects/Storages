package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

internal class FileStreamerTest {
    @Test
    fun existsTest() {
        val dateFormat = SimpleDateFormat("yyyyMMdd")
        val dir = File("/tmp/${dateFormat.format(Date())}")
        dir.deleteRecursively()
        dir.mkdirs()
        val id = mockUUID(1)
        val inputPointer = 1
        assertFalse(File(dir, "$id-$inputPointer").exists())
        val streamer = mockFileStreamer(id = id, dir = dir, inputPointer = inputPointer)
        val bytes = streamer.inputStream().readBytes()
        assertEquals(bytes.size, 12)
        assertTrue(bytes.all { it == 0.toByte() })
    }

    @Test
    fun lengthTest() {
        val dateFormat = SimpleDateFormat("yyyyMMdd")
        val dir = File("/tmp/${dateFormat.format(Date())}")
        dir.deleteRecursively()
        dir.mkdirs()
        val id = mockUUID(1)
        val inputPointer = 1
        val file = File(dir, "$id-$inputPointer")
        file.createNewFile()
        assertTrue(file.exists())
        assertEquals(0L, file.length())
        val streamer = mockFileStreamer(id = id, dir = dir, inputPointer = inputPointer)
        val bytes = streamer.inputStream().readBytes()
        assertEquals(bytes.size, 12)
        assertTrue(bytes.all { it == 0.toByte() })
    }
}
