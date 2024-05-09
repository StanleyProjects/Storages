package sp.kx.storages

import java.util.UUID

internal fun mockUUID(pointer: Int = 0): UUID {
    return UUID.fromString("cbae2ba0-6be9-40f5-b565-d6152a1${20_000 + pointer % 1024}")
}
