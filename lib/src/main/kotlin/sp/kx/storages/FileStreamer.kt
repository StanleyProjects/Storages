package sp.kx.storages

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

internal class FileStreamer(
    private val dir: File,
    private val id: UUID,
    private val inputPointer: Int,
    private val outputPointer: Int,
) : Streamer {
    override fun inputStream(): InputStream {
        val file = File(dir, "$id-$inputPointer")
        if (!file.exists() || file.length() == 0L) {
            file.writeBytes(ByteArray(12))
        }
        return file.inputStream()
    }

    override fun outputStream(): OutputStream {
        return File(dir, "$id-$outputPointer").outputStream()
    }
}
