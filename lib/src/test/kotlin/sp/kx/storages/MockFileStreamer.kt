package sp.kx.storages

import java.io.File
import java.util.UUID

internal fun mockFileStreamer(
    dir: File,
    id: UUID = mockUUID(),
    inputPointer: Int = 0,
    outputPointer: Int = 0,
): FileStreamer {
    return FileStreamer(
        dir = dir,
        id = id,
        inputPointer = inputPointer,
        outputPointer = outputPointer,
    )
}
