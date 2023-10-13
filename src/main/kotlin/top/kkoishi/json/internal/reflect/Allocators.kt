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
    fun unsafeAny(tryUnsafe: Boolean): InstanceAllocator<Any> {
        if (tryUnsafe) {
            try {
                val clz = Class.forName("sun.misc.Unsafe")
                val allocateInstance = clz.getDeclaredMethod("allocateInstance", Class::class.java)
                val unsafeField = clz.getDeclaredField("theUnsafe")
                unsafeField.isAccessible = true
                val unsafe = unsafeField[null]
                return object : InstanceAllocator<Any> {
                    override fun allocateInstance(typeofT: Type<Any>): Any {
                        checkInstantiable(typeofT.rawType())
                        return allocateInstance(unsafe, typeofT.type())
                    }

                    override fun allocateInstance(clz: Class<Any>): Any {
                        checkInstantiable(clz)
                        return allocateInstance(unsafe, clz)
                    }
                }
            } catch (e: Exception) {
            }
        }

        try {
            val getConstructorId =
                ObjectStreamClass::class.java.getDeclaredMethod("getConstructorId", Class::class.java)
            getConstructorId.isAccessible = true
            val newInstance =
                ObjectStreamClass::class.java.getDeclaredMethod("newInstance", Integer.TYPE)
            newInstance.isAccessible = true
            return object : InstanceAllocator<Any> {
                override fun allocateInstance(typeofT: Type<Any>): Any {
                    with(typeofT.rawType()) {
                        checkInstantiable(this)
                        return newInstance(null, getConstructorId(null, this))
                    }
                }

                override fun allocateInstance(clz: Class<Any>): Any {
                    checkInstantiable(clz)
                    return newInstance(null, getConstructorId(null, clz))
                }

            }
        } catch (e: Exception) {
        }

        try {
            val newInstance = ObjectStreamClass::class.java.getDeclaredMethod("newInstance", Class::class.java)
            newInstance.isAccessible = true
            return object : InstanceAllocator<Any> {
                override fun allocateInstance(clz: Class<Any>): Any {
                    checkInstantiable(clz)
                    return newInstance(null, clz)
                }

                override fun allocateInstance(typeofT: Type<Any>): Any {
                    with(typeofT.rawType()) {
                        checkInstantiable(this)
                        return newInstance(null, this)
                    }
                }
            }
        } catch (e: Exception) {
        }

        return object : InstanceAllocator<Any> {
            override fun allocateInstance(typeofT: Type<Any>): Any = Utils.uoe("Unsupported class.")
            override fun allocateInstance(clz: Class<Any>): Any = Utils.uoe("Unsupported class.")
        }
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
                        checkInstantiable(typeofT.rawType())
                        return allocateInstance(unsafe, typeofT.type()) as T
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
            val getConstructorId =
                ObjectStreamClass::class.java.getDeclaredMethod("getConstructorId", Class::class.java)
            getConstructorId.isAccessible = true
            val newInstance =
                ObjectStreamClass::class.java.getDeclaredMethod("newInstance", Integer.TYPE)
            newInstance.isAccessible = true
            return object : InstanceAllocator<T> {
                override fun allocateInstance(typeofT: Type<T>): T {
                    with(typeofT.rawType()) {
                        checkInstantiable(this)
                        return newInstance(null, getConstructorId(null, this)) as T
                    }
                }

                override fun allocateInstance(clz: Class<T>): T {
                    checkInstantiable(clz)
                    return newInstance(null, getConstructorId(null, clz)) as T
                }

            }
        } catch (e: Exception) {
        }

        try {
            val newInstance = ObjectStreamClass::class.java.getDeclaredMethod("newInstance", Class::class.java)
            newInstance.isAccessible = true
            return object : InstanceAllocator<T> {
                override fun allocateInstance(clz: Class<T>): T {
                    checkInstantiable(clz)
                    return newInstance(null, clz) as T
                }

                override fun allocateInstance(typeofT: Type<T>): T {
                    with(typeofT.rawType()) {
                        checkInstantiable(this)
                        return newInstance(null, this) as T
                    }
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