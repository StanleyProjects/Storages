package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import sp.kx.bytes.toByteArray
import sp.kx.bytes.toHEX

internal class HashesTransformerTest {
    @Test
    fun encodeTest() {
        val hf = MockHashFunction()
        val transformer = HashesTransformer(hf = hf)
        val decoded = mapOf(
            mockUUID(1) to MockHashFunction.map("1"),
            mockUUID(2) to MockHashFunction.map("2"),
        )
        val expected = decoded.size.toByteArray() + decoded.map { (id, hash) ->
            check(hash.size == hf.size)
            id.toByteArray() + hash
        }.flatMap { it.toList() }.toByteArray()
        val actual = transformer.encode(decoded = decoded)
        assertTrue(expected.contentEquals(actual), "expected: ${expected.toHEX()}, actual: ${actual.toHEX()}")
    }

    @Test
    fun decodeTest() {
        val hf = MockHashFunction()
        val expected = mapOf(
            mockUUID(1) to MockHashFunction.map("1"),
            mockUUID(2) to MockHashFunction.map("2"),
        )
        val encoded = expected.size.toByteArray() + expected.map { (id, hash) ->
            check(hash.size == hf.size)
            id.toByteArray() + hash
        }.flatMap { it.toList() }.toByteArray()
        val transformer = HashesTransformer(hf = hf)
        val actual = transformer.decode(encoded = encoded)
        assertEquals(expected.size, actual.size)
        expected.forEach { (id, eh) ->
            val ah = actual[id] ?: error("No hash!")
            assertTrue(eh.contentEquals(ah), "expected: ${eh.toHEX()}, actual: ${ah.toHEX()}")
        }
    }
}
