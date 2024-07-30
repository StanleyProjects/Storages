package sp.kx.storages

import java.util.UUID

internal fun <T : Any> mockDescribed(
    id: UUID = mockUUID(1),
    info: ItemInfo = mockItemInfo(),
    payload: T = TODO("mock:described"),
): Described<T> {
    return Described(
        id = id,
        info = info,
        payload = payload,
    )
}

internal fun <T : Any> mockDescribed(pointer: Int, payload: T): Described<T> {
    return mockDescribed(
        id = mockUUID(pointer = pointer),
        info = mockItemInfo(pointer = pointer),
        payload = payload,
    )
}

internal fun mockDescribed(pointer: Int): Described<String> {
    return mockDescribed(pointer = pointer, payload = "payload:$pointer")
}
