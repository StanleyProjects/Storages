package sp.kx.storages

import kotlin.time.Duration

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
        meta = mockMetadata(
            id = mockUUID(pointer = pointer),
            created = mockDuration(pointer = pointer),
            info = mockItemInfo(
                updated = mockDuration(pointer = pointer),
                hash = MockHashFunction.map("$value"),
            ),
        ),
        value = value,
    )
}

internal fun mockPayload(pointer: Int): Payload<String> {
    return mockPayload(pointer = pointer, time = mockDuration(pointer = pointer))
}

internal fun mockPayload(pointer: Int, time: Duration): Payload<String> {
    val value = "payload:$pointer"
    return mockPayload(
        meta = mockMetadata(
            id = mockUUID(pointer = pointer),
            created = time,
            info = mockItemInfo(
                updated = time,
                hash = MockHashFunction.map(value),
            ),
        ),
        value = value,
    )
}
