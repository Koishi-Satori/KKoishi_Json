package top.kkoishi.json.internal

import sun.misc.Unsafe

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