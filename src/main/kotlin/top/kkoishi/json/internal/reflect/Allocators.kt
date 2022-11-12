package top.kkoishi.json.internal.reflect

import top.kkoishi.json.Utils
import top.kkoishi.json.exceptions.UnsupportedException
import top.kkoishi.json.reflect.Type
import java.io.ObjectInputStream
import java.lang.reflect.Modifier

internal object Allocators {
    @JvmStatic
    private fun checkInstantiable0(c: Class<*>): String? {
        val modifiers = c.modifiers
        if (Modifier.isInterface(modifiers))
            return "Interfaces can't be instantiated! Register an InstanceCreator or a TypeAdapter for this type. Interface name: ${c.name}"
        if (Modifier.isAbstract(modifiers))
            return "Interfaces can't be instantiated! Register an InstanceCreator or a TypeAdapter for this type. Interface name: ${c.name}"
        return null
    }

    @JvmStatic
    private fun checkInstantiable(c: Class<*>) {
        val msg = checkInstantiable0(c)
        if (msg != null)
            throw UnsupportedException("This allocator is used for non-instantiable type: $msg")
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> unsafe(): InstanceAllocator<T> {
        try {
            val clz = Class.forName("sun.misc.Unsafe")
            val allocator = clz.getDeclaredMethod("allocateInstance", Class::class.java)
            val unsafe = clz.getDeclaredField("theUnsafe")
            return object : InstanceAllocator<T> {
                override fun allocateInstance(typeofT: Type<T>): T {
                    checkInstantiable(typeofT.rawType)
                    return allocator(unsafe, typeofT.type) as T
                }

                override fun allocateInstance(clz: Class<T>): T {
                    checkInstantiable(clz)
                    return allocator(unsafe, clz) as T
                }
            }
        } catch (e: Exception) {
        }

        try {
            TODO()
        } catch (e: Exception) {
        }

        return object : InstanceAllocator<T> {
            override fun allocateInstance(typeofT: Type<T>): T = Utils.uoe("Unsupported class.")
            override fun allocateInstance(clz: Class<T>): T = Utils.uoe("Unsupported class.")
        }
    }
}