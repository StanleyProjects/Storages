package sp.service.sample

import sp.kx.storages.HashFunction
import java.security.MessageDigest

internal class MD5HashFunction : HashFunction {
    private val md = MessageDigest.getInstance("MD5")
    override val size = md.digestLength
    override fun map(bytes: ByteArray): ByteArray {
        return md.digest(bytes)
    }
}
