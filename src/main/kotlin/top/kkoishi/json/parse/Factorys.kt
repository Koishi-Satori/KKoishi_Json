package top.kkoishi.json.parse

import top.kkoishi.json.internal.io.UtilFactorys
import top.kkoishi.json.internal.reflect.Reflection
import top.kkoishi.json.io.*
import top.kkoishi.json.reflect.TypeResolver
import java.lang.reflect.ParameterizedType

import java.lang.reflect.Type as JType

object Factorys {
    @JvmStatic
    private val stored: MutableMap<JType, TypeParserFactory> = mutableMapOf()

    @JvmStatic
    private val spec: MutableMap<Class<*>, TypeParserFactory> = mutableMapOf()

    @JvmStatic
    @JvmName(" addType")
    internal fun addType(type: JType, factory: TypeParserFactory) {
        stored[type] = factory
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
        if (stored.containsKey(type))
            return stored[type]!!
        return getIfNotContainsFromClass(type)
    }

    @JvmStatic
    private fun getIfNotContainsFromClass(type: Class<*>): TypeParserFactory {
        if (type.isArray)
            return getArrayTypeFactory()
        else if (Reflection.isMapType(type) || Reflection.isCollection(type))
            throw IllegalArgumentException()
        else if (Reflection.checkJsonPrimitive(type))
            return UtilFactorys.PRIMITIVE
        else
            return getFieldTypeFactory()
    }

    @JvmStatic
    fun getFactory(type: JType): TypeParserFactory {
        TODO()
    }

    fun <TYPE> getFactory(typeResolver: TypeResolver<TYPE>): TypeParserFactory {
        val parameterizedType = typeResolver.resolve() as ParameterizedType
        val arguments = parameterizedType.actualTypeArguments
        val key = Reflection.ParameterizedTypeImpl(parameterizedType.ownerType, parameterizedType.rawType, *arguments)
        if (stored.containsKey(key)) {
            return stored[key]!!
        }
        val rawType = Reflection.getRawType(key.rawType)
        var owner = key.ownerType
        if (rawType == Any::class.java) {
            if (owner == null) {
                throw IllegalStateException()
            }
            owner = Reflection.getRawType(owner)
            if (owner == Any::class.java) {
                throw IllegalStateException()
            }
            return getIfNotContainsFromClass(owner)
        }
        return getIfNotContainsFromClass(rawType)
    }
}