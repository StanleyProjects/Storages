package sp.service.sample

import sp.kx.storages.HashFunction
import java.security.MessageDigest

internal class MDHashFunction(algorithm: String) : HashFunction {
    private val md = MessageDigest.getInstance(algorithm)
    override val size = md.digestLength
    override fun map(bytes: ByteArray): ByteArray {
        return md.digest(bytes)
    }
}
