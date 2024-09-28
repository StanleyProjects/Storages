package sp.kx.storages

internal fun mockSyncSession(
    src: ByteArray = byteArrayOf(0x1f, 0x2e, 0x3d, 0x4c, 0x5b),
    dst: ByteArray = byteArrayOf(0x2e, 0x3d, 0x4c, 0x5b, 0x6a),
): SyncSession {
    return SyncSession(src = src, dst = dst)
}
