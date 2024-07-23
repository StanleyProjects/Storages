package sp.kx.storages

import java.io.InputStream
import java.io.OutputStream

interface Streamer {
    fun inputStream(): InputStream
    fun outputStream(): OutputStream
}
