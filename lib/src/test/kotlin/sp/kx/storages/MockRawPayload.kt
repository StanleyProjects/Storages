package sp.kx.storages

internal fun mockRawPayload(
    meta: Metadata = mockMetadata(pointer = 1),
    bytes: ByteArray = mockByteArray(size = 1),
): RawPayload {
    return RawPayload(
        meta = meta,
        bytes = bytes,
    )
}

internal fun mockRawPayload(pointer: Int): RawPayload {
    return mockRawPayload(
        meta = mockMetadata(pointer = pointer),
        bytes = "payload:$pointer".toByteArray(),
    )
}
