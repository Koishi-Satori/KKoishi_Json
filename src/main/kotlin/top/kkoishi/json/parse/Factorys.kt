package top.kkoishi.json.parse

import top.kkoishi.json.internal.io.UtilFactorys
import top.kkoishi.json.internal.reflect.Allocators
import top.kkoishi.json.internal.reflect.Reflection
import top.kkoishi.json.io.*
import top.kkoishi.json.reflect.Type
import top.kkoishi.json.reflect.TypeResolver
import top.kkoishi.json.Kson
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType

import java.lang.reflect.Type as JType

/**
 * A class used to get TypeParsers without using [Kson].
 *
 * Now this class is thread-safety.
 *
 * @author KKoishi_
 */
object Factorys {
    @JvmStatic
    private val cached: ThreadLocal<MutableMap<JType, TypeParserFactory>> = ThreadLocal()

    init {
        val stored = HashMap<JType, TypeParserFactory>()
        UtilFactorys.init(stored)
        cached.set(stored)
    }

    @JvmStatic
    @JvmName(" addType")
    fun register(type: JType, factory: TypeParserFactory) {
        cached.get()[type] = factory
    }

    @JvmStatic
    @JvmName(" get")
    internal fun get(type: JType): TypeParserFactory? {
        return cached.get().getOrDefault(type, null)
    }

    @JvmStatic
    fun getFieldTypeFactory(): TypeParserFactory = FieldTypeParserFactory.` instance`

    @JvmStatic
    fun getArrayTypeFactory(): ArrayTypeParserFactory = ArrayTypeParserFactory.` inst`

    @JvmStatic
    fun getMapTypeFactory(): MapTypeParserFactory = MapTypeParserFactory.` inst`

    @JvmStatic
    fun getCollectionTypeFactory(): CollectionTypeParserFactory = CollectionTypeParserFactory.` inst`

    @JvmStatic
    fun getFactoryFromClass(type: Class<*>): TypeParserFactory {
        with(cached.get()){
            if (containsKey(type))
                return this[type]!!
        }
        return getIfNotContainsFromClass(type)
    }

    @JvmStatic
    private fun getIfNotContainsFromClass(type: Class<*>): TypeParserFactory {
        if (type.isArray)
            return getArrayTypeFactory()
        else if (Reflection.isMap(type) || Reflection.isCollection(type))
            throw IllegalArgumentException("Please use getMapTypeFactory/getCollectionTypeFactory instead of this to get TypeParserFactory of map/collection")
        else if (Reflection.checkJsonPrimitive(type))
            return UtilFactorys.PRIMITIVE
        else
            return getFieldTypeFactory()
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun getFactory(type: JType): TypeParserFactory {
        if (type is ParameterizedType) {
            val parameters = type.actualTypeArguments
            val key = Reflection.ParameterizedTypeImpl(type.ownerType, type.rawType, *parameters)
            val stored = cached.get()
            if (stored.containsKey(key))
                return stored[key]!!
            val raw = Reflection.getRawType(type.rawType)
            if (parameters.size == 2 && raw == MutableMap::class.java)
                throw IllegalArgumentException()
            if (parameters.size == 1 && raw == Collection::class.java)
                throw IllegalArgumentException()
            if (raw != Any::class.java)
                return getIfNotContainsFromClass(raw)
        } else if (type is Class<*>) {
            if (Reflection.checkJsonPrimitive(type))
                return UtilFactorys.PRIMITIVE

            val getter = Reflection.checkFactoryGetter(type)
            if (getter != null) {
                getter.isAccessible = true
                if (0 != getter.parameterCount)
                    return getFactoryFromClass(type)
                val factory: TypeParserFactory =
                    getter(if (Reflection.isStatic(getter)) null else Allocators.unsafeAny(true)
                        .allocateInstance(type as Class<Any>)) as TypeParserFactory?
                        ?: return getFactoryFromClass(type)
                register(type, factory)
                return factory
            }
            return getFactoryFromClass(type)
        } else if (type is GenericArrayType) {
            // get generic component type and create top.kkoishi.reflect.Type
            val cmpType = type.genericComponentType
            if (cmpType is ParameterizedType) {
                val tp = Type<Any>(cmpType)
                return getFactoryFromClass(tp.rawType())
            }
        }
        throw IllegalStateException("Can not get the parser of $type")
    }

    fun <TYPE> getFactory(typeResolver: TypeResolver<TYPE>): TypeParserFactory {
        val type = typeResolver.resolve()
        if (type !is ParameterizedType)
            throw IllegalStateException("Can not get Parameters, please make sure that you fill in the complete generic parameters")
        val arguments = type.actualTypeArguments
        val key = Reflection.ParameterizedTypeImpl(type.ownerType, type.rawType, *arguments)
        val stored = cached.get()
        if (stored.containsKey(key)) {
            return stored[key]!!
        }
        val rawType = Reflection.getRawType(key.rawType)
        if ((arguments.size == 2 && rawType == MutableMap::class.java) ||
            (arguments.size == 1 && rawType == Collection::class.java)
        )
            throw IllegalArgumentException()
        return getIfNotContainsFromClass(rawType)
    }
}