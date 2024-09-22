package sp.kx.storages

internal fun <T : Any> mockPayload(
    meta: Metadata = mockMetadata(pointer = 1),
    value: T = TODO("mock:payload"),
): Payload<T> {
    return Payload(
        meta = meta,
        value = value,
    )
}

internal fun <T : Any> mockPayload(pointer: Int, value: T): Payload<T> {
    return mockPayload(
        meta = mockMetadata(pointer = pointer),
        value = value,
    )
}

internal fun mockPayload(pointer: Int): Payload<String> {
    return mockPayload(
        meta = mockMetadata(pointer = pointer),
        value = "payload:$pointer",
    )
}
