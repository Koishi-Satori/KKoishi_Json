package top.kkoishi.json.internal

import top.kkoishi.json.JsonElement
import top.kkoishi.json.JsonString
import top.kkoishi.json.Kson
import top.kkoishi.json.exceptions.UnsupportedException
import top.kkoishi.json.internal.InternalParserFactory.DateParser
import top.kkoishi.json.internal.InternalParserFactory.getFactory
import top.kkoishi.json.internal.io.UtilParsers
import top.kkoishi.json.io.TypeParser
import top.kkoishi.json.io.TypeParserFactory
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Type
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.time.ZoneId
import java.util.*
import kotlin.jvm.Throws

/**
 * This is an util class used to access some methods in sun.misc.Unsafe and get the [TypeParserFactory]
 * of some basic classes.
 *
 * Some JVM environment might lack of [jdk.unsupported] module, so the methods are acquired by
 * reflection.
 *
 * @author KKoishi_
 */
internal object Utils {
    /**
     * This method is used while initialize Kson instance.
     *
     * @param inst Kson instance.
     * @return a list contains pairs of java Types and their TypeParserFactory.
     */
    @JvmStatic
    internal fun KKoishiJsonInit(inst: Kson): List<Pair<Type, TypeParserFactory>> {
        // TODO: init some basic classes.
        return listOf(getFactory(Date::class.java, DateParser(inst)),
            getFactory(UUID::class.java, UtilParsers.UUID),
            getFactory(Calendar::class.java,
                object : TypeParser<Calendar>(top.kkoishi.json.reflect.Type(Calendar::class.java)),
                    InternalParserFactory.Conditional {
                    override fun fromJson(json: JsonElement): Calendar {
                        if (json.isJsonPrimitive()) {
                            val primitive = json.toJsonPrimitive()
                            if (primitive.isJsonString())
                                return Calendar.getInstance(TimeZone.getTimeZone(primitive.getAsString()),
                                    instance.locale)
                        }
                        throw IllegalArgumentException("Required JsonString to serialize to Calender")
                    }

                    override fun toJson(t: Calendar): JsonElement = JsonString(t.timeZone.toZoneId().toString())
                    override val instance: Kson
                        get() = inst
                }),
            getFactory(BitSet::class.java, UtilParsers.BITSET),
            getFactory(TimeZone::class.java, UtilParsers.TIME_ZONE),
            getFactory(ZoneId::class.java, UtilParsers.ZONE_ID),
            getFactory(Random::class.java, UtilParsers.RANDOM),
            getFactory(File::class.java, UtilParsers.FILE),
            getFactory(Path::class.java, UtilParsers.PATH),
            getFactory(URI::class.java, UtilParsers.URI),
            getFactory(URL::class.java, UtilParsers.URL),
            getFactory(InetAddress::class.java, UtilParsers.INET_ADDRESS)
        )
    }

    /**
     * This method is used to get the Unsafe instance.
     *
     * @return Unsafe instance.
     */
    @JvmStatic
    private fun accessUnsafe(): Any? {
        try {
            if (unsafeClass != null) {
                val f = unsafeClass.getDeclaredField("theUnsafe")
                f.isAccessible = true
                return f[null]
            }
        } catch (e: Exception) {
        }
        return null
    }

    /**
     * This method is used to get Unsafe class.
     * Some jvm environment might in lack of sun.misc package, so use reflection is needed.
     *
     * @return sun.misc.Unsafe.class
     */
    @JvmStatic
    private fun accessUnsafeClass(): Class<*>? {
        try {
            return Class.forName("sun.misc.Unsafe")
        } catch (e: Exception) {
        }
        return null
    }

    @JvmStatic
    private fun accessAllocateInstance(): Method? {
        try {
            if (unsafeClass != null) {
                val mth = unsafeClass.getDeclaredMethod("allocateInstance", Class::class.java)
                mth.isAccessible = true
                return mth
            }
        } catch (e: Exception) {
        }
        return null
    }

    @JvmStatic
    private fun accessCompareAndSwapObject(): Method? {
        try {
            if (unsafeClass != null) {
                val mth = unsafeClass.getDeclaredMethod("compareAndSwapObject",
                    Any::class.java,
                    java.lang.Long.TYPE,
                    Any::class.java,
                    Any::class.java)
                mth.isAccessible = true
                return mth
            }
        } catch (e: Exception) {
        }
        return null
    }

    @JvmStatic
    private fun accessCompareAndSwapInt(): Method? {
        try {
            if (unsafeClass != null) {
                val mth = unsafeClass.getDeclaredMethod("compareAndSwapInt",
                    Any::class.java,
                    java.lang.Long.TYPE,
                    Integer.TYPE,
                    Integer.TYPE)
                mth.isAccessible = true
                return mth
            }
        } catch (e: Exception) {
        }
        return null
    }

    @JvmStatic
    private fun accessCompareAndSwapLong(): Method? {
        try {
            if (unsafeClass != null) {
                val mth = unsafeClass.getDeclaredMethod("compareAndSwapLong",
                    Any::class.java,
                    java.lang.Long.TYPE,
                    java.lang.Long.TYPE,
                    java.lang.Long.TYPE)
                mth.isAccessible = true
                return mth
            }
        } catch (e: Exception) {
        }
        return null
    }

    @JvmStatic
    private fun accessObjectFieldOffset(): Method? {
        try {
            if (unsafeClass != null) {
                val mth = unsafeClass.getDeclaredMethod("objectFieldOffset", Field::class.java)
                mth.isAccessible = true
                return mth
            }
        } catch (e: Exception) {
        }
        return null
    }

    @JvmStatic
    private fun accessGetAndSetObject(): Method? {
        try {
            if (unsafeClass != null) {
                val mth = unsafeClass.getDeclaredMethod("getAndSetObject",
                    Any::class.java,
                    java.lang.Long.TYPE,
                    Any::class.java)
                mth.isAccessible = true
                return mth
            }
        } catch (e: Exception) {
        }
        return null
    }

    @JvmStatic
    private fun accessGetValue(expand: String): Method? {
        try {
            if (unsafeClass != null) {
                val mth = unsafeClass.getDeclaredMethod("get$expand", Any::class.java, java.lang.Long.TYPE)
                mth.isAccessible = true
                return mth
            }
        } catch (e: Exception) {
        }
        return null
    }

    @JvmStatic
    private val unsafeClass: Class<*>? = accessUnsafeClass()

    @JvmStatic
    private val allocateInstance: Method? = accessAllocateInstance()

    @JvmStatic
    private val compareAndSwapObject: Method? = accessCompareAndSwapObject()

    @JvmStatic
    private val compareAndSwapInt: Method? = accessCompareAndSwapInt()

    @JvmStatic
    private val compareAndSwapLong: Method? = accessCompareAndSwapLong()

    @JvmStatic
    private val objectFieldOffset = accessObjectFieldOffset()

    @JvmStatic
    private val getAndSetObject = accessGetAndSetObject()

    @JvmStatic
    private val getObject = accessGetValue("Object")

    @JvmStatic
    private val getInt = accessGetValue("Int")

    @JvmStatic
    private val getChar = accessGetValue("Char")

    @JvmStatic
    private val getLong = accessGetValue("Long")

    @JvmStatic
    private val getShort = accessGetValue("Short")

    @JvmStatic
    private val getByte = accessGetValue("Byte")

    @JvmStatic
    private val getFloat = accessGetValue("Float")

    @JvmStatic
    private val getDouble = accessGetValue("Double")

    @JvmStatic
    private val getBoolean = accessGetValue("Boolean")

    @JvmStatic
    internal val unsafe: Any? = accessUnsafe()


    @JvmStatic
    @Throws(UnsupportedException::class)
    private fun <T> ue(): T = throw UnsupportedException("Can not access class sun.misc.Unsafe")

    @JvmStatic
    @Throws(UnsupportedException::class)
    internal fun allocateInstance(clz: Class<*>): Any {
        try {
            if (allocateInstance != null) {
                return allocateInstance.invoke(unsafe, clz)
            }
        } catch (e: Exception) {
        }
        return ue()
    }

    @JvmStatic
    @Throws(UnsupportedException::class)
    internal fun compareAndSwapObject(o: Any, offset: Long, expect: Any?, value: Any?): Boolean {
        try {
            if (compareAndSwapObject != null) {
                return compareAndSwapObject.invoke(unsafe, o, offset, expect, value) as Boolean
            }
        } catch (e: Exception) {
        }
        return ue()
    }

    @JvmStatic
    @Throws(UnsupportedException::class)
    internal fun compareAndSwapInt(o: Any, offset: Long, expect: Int, value: Int): Boolean {
        try {
            if (compareAndSwapInt != null) {
                return compareAndSwapInt.invoke(unsafe, o, offset, expect, value) as Boolean
            }
        } catch (e: Exception) {
        }
        return ue()
    }

    @JvmStatic
    @Throws(UnsupportedException::class)
    internal fun compareAndSwapLong(o: Any, offset: Long, expect: Long, value: Long): Boolean {
        try {
            if (compareAndSwapLong != null) {
                return compareAndSwapLong.invoke(unsafe, o, offset, expect, value) as Boolean
            }
        } catch (e: Exception) {
        }
        return ue()
    }

    @JvmStatic
    @Throws(UnsupportedException::class)
    internal fun objectFieldOffset(field: Field): Long {
        try {
            if (objectFieldOffset != null) {
                return objectFieldOffset.invoke(unsafe, field) as Long
            }
        } catch (e: Exception) {
        }
        return ue()
    }

    @JvmStatic
    @Deprecated("Current useless")
    @Throws(UnsupportedException::class)
    internal fun getAndSetObject(o: Any, offset: Long, value: Any?): Any? {
        try {
            if (getAndSetObject != null) {
                return getAndSetObject.invoke(unsafe, o, offset, value)
            }
        } catch (e: Exception) {
        }
        return ue()
    }

    @JvmStatic
    @Throws(UnsupportedException::class)
    internal fun getObject(inst: Any, offset: Long): Any? {
        try {
            if (getObject != null) {
                return getObject.invoke(unsafe, inst, offset)
            }
        } catch (e: Exception) {
        }
        return ue()
    }

    @JvmStatic
    @Throws(UnsupportedException::class)
    internal fun getInt(inst: Any, offset: Long): Any? {
        try {
            if (getInt != null) {
                return getInt.invoke(unsafe, inst, offset)
            }
        } catch (e: Exception) {
        }
        return ue()
    }

    @JvmStatic
    @Throws(UnsupportedException::class)
    internal fun getChar(inst: Any, offset: Long): Any? {
        try {
            if (getChar != null) {
                return getChar.invoke(unsafe, inst, offset)
            }
        } catch (e: Exception) {
        }
        return ue()
    }

    @JvmStatic
    @Throws(UnsupportedException::class)
    internal fun getLong(inst: Any, offset: Long): Any? {
        try {
            if (getLong != null) {
                return getLong.invoke(unsafe, inst, offset)
            }
        } catch (e: Exception) {
        }
        return ue()
    }

    @JvmStatic
    @Throws(UnsupportedException::class)
    internal fun getShort(inst: Any, offset: Long): Any? {
        try {
            if (getShort != null) {
                return getShort.invoke(unsafe, inst, offset)
            }
        } catch (e: Exception) {
        }
        return ue()
    }

    @JvmStatic
    @Throws(UnsupportedException::class)
    internal fun getByte(inst: Any, offset: Long): Any? {
        try {
            if (getByte != null) {
                return getByte.invoke(unsafe, inst, offset)
            }
        } catch (e: Exception) {
        }
        return ue()
    }

    @JvmStatic
    @Throws(UnsupportedException::class)
    internal fun getFloat(inst: Any, offset: Long): Any? {
        try {
            if (getFloat != null) {
                return getFloat.invoke(unsafe, inst, offset)
            }
        } catch (e: Exception) {
        }
        return ue()
    }

    @JvmStatic
    @Throws(UnsupportedException::class)
    internal fun getDouble(inst: Any, offset: Long): Any? {
        try {
            if (getDouble != null) {
                return getDouble.invoke(unsafe, inst, offset)
            }
        } catch (e: Exception) {
        }
        return ue()
    }

    @JvmStatic
    @Throws(UnsupportedException::class)
    internal fun getBoolean(inst: Any, offset: Long): Any? {
        try {
            if (getBoolean != null) {
                return getBoolean.invoke(unsafe, inst, offset)
            }
        } catch (e: Exception) {
        }
        return ue()
    }
}