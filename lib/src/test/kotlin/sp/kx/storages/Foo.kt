package sp.kx.storages

internal data class Foo(val text: String)

internal object FooTransformer : Transformer<Foo> {
    override fun encode(decoded: Foo): ByteArray {
        return decoded.text.toByteArray()
    }

    override fun decode(encoded: ByteArray): Foo {
        return Foo(text = String(encoded))
    }
}
