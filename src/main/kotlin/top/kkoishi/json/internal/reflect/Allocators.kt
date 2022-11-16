package top.kkoishi.json.internal.reflect

import top.kkoishi.json.Utils
import top.kkoishi.json.exceptions.UnsupportedException
import top.kkoishi.json.reflect.Type
import java.io.ObjectInputStream
import java.io.ObjectStreamClass
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
    fun <T : Any> unsafe(tryUnsafe: Boolean): InstanceAllocator<T> {
        if (tryUnsafe) {
            try {
                val clz = Class.forName("sun.misc.Unsafe")
                val allocateInstance = clz.getDeclaredMethod("allocateInstance", Class::class.java)
                val unsafeField = clz.getDeclaredField("theUnsafe")
                unsafeField.isAccessible = true
                val unsafe = unsafeField[null]
                return object : InstanceAllocator<T> {
                    override fun allocateInstance(typeofT: Type<T>): T {
                        checkInstantiable(typeofT.rawType)
                        return allocateInstance(unsafe, typeofT.type) as T
                    }

                    override fun allocateInstance(clz: Class<T>): T {
                        checkInstantiable(clz)
                        return allocateInstance(unsafe, clz) as T
                    }
                }
            } catch (e: Exception) {
            }
        }

        try {
            val getConstructorId = ObjectStreamClass::class.java.getDeclaredMethod("getConstructorId", Class::class.java)
            getConstructorId.isAccessible = true
            val id = getConstructorId(null, Any::class.java) as Int
            val newInstance = ObjectInputStream::class.java.getDeclaredMethod("newInstance", Class::class.java, Integer.TYPE)
            newInstance.isAccessible = true
            return object : InstanceAllocator<T> {
                override fun allocateInstance(typeofT: Type<T>): T {
                    with (typeofT.rawType) {
                        checkInstantiable(this)
                        return newInstance(null, this, id) as T
                    }
                }

                override fun allocateInstance(clz: Class<T>): T {
                    checkInstantiable(clz)
                    return newInstance(null, clz, id) as T
                }

            }
        } catch (e: Exception) {
        }

        return object : InstanceAllocator<T> {
            override fun allocateInstance(typeofT: Type<T>): T = Utils.uoe("Unsupported class.")
            override fun allocateInstance(clz: Class<T>): T = Utils.uoe("Unsupported class.")
        }
    }
}