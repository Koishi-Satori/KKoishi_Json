package top.kkoishi.json.parse

import sun.misc.Unsafe

enum class Platform {
    WIN, LINUX, MACOS, UNSUPPORTED
}

enum class NumberMode {
    BIG_DECIMAL_ONLY, BIG_NUMBER_ONLY, UNTIL_LONG, UNTIL_INT, UNTIL_SHORT, ALL_TYPE
}

internal data class Token @JvmOverloads constructor(val type: Type, var content: String = "") {
    companion object {
        @JvmStatic
        private val offset: Long = Utils.unsafe.objectFieldOffset(Token::class.java.getDeclaredField("type"))

        @JvmStatic
        private fun createTypeOnlyToken(t: Type?): Token {
            with(Utils.unsafe) {
                val inst = allocateInstance(Token::class.java)
                compareAndSwapObject(inst, offset, null, t)
                return inst as Token
            }
        }

        @JvmStatic
        val blanket = createTypeOnlyToken(Type.BLANKET_BEGIN)

        @JvmStatic
        val blanketEnd = createTypeOnlyToken(Type.BLANKET_END)

        @JvmStatic
        val array = createTypeOnlyToken(Type.ARRAY_BLANKET_BEGIN)

        @JvmStatic
        val arrayEnd = createTypeOnlyToken(Type.ARRAY_BLANKET_END)

        @JvmStatic
        val quote = createTypeOnlyToken(Type.QUOTE)

        @JvmStatic
        val sep = createTypeOnlyToken(Type.SEPARATOR)

        @JvmStatic
        val colon = createTypeOnlyToken(Type.COLON)

        @JvmStatic
        val nullv = createTypeOnlyToken(Type.NULL)

        @JvmStatic
        val truev = Token(Type.BOOL, " ")

        @JvmStatic
        val falsev = Token(Type.BOOL, "")

        @JvmStatic
        val invalid = createTypeOnlyToken(null)
    }
}

internal enum class Type {
    BLANKET_BEGIN, ARRAY_BLANKET_BEGIN, QUOTE,
    SEPARATOR, COLON,
    NUMBER, STRING, NULL, BOOL,
    BLANKET_END, ARRAY_BLANKET_END
}

internal object Utils {
    @JvmStatic
    private fun accessUnsafe(): Unsafe {
        val f = Unsafe::class.java.getDeclaredField("theUnsafe")
        f.isAccessible = true
        return f[null] as Unsafe
    }

    @JvmStatic
    val unsafe = accessUnsafe()
}