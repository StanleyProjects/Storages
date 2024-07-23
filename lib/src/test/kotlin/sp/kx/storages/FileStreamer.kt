package sp.kx.storages

import java.io.File
import java.io.InputStream
import java.io.OutputStream

internal class FileStreamer(
    private val file: File,
) : Streamer {
    init {
        if (!file.exists() || file.length() == 0L) {
            outputStream().use { it.write(ByteArray(8)) }
        }
    }

    override fun inputStream(): InputStream {
        return file.inputStream()
    }

    override fun outputStream(): OutputStream {
        return file.outputStream()
    }
}
