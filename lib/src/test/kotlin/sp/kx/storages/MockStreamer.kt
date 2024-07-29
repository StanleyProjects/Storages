package sp.kx.storages

import java.io.InputStream
import java.io.OutputStream

internal class MockStreamer : Streamer {
    override fun inputStream(): InputStream {
        TODO("MockStreamer:inputStream")
    }

    override fun outputStream(): OutputStream {
        TODO("MockStreamer:outputStream")
    }
}
