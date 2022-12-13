package top.kkoishi.json

import top.kkoishi.json.internal.Utils.KKoishiJsonInit
import top.kkoishi.json.internal.io.UtilFactorys
import top.kkoishi.json.internal.io.UtilParsers
import top.kkoishi.json.internal.reflect.Allocators
import top.kkoishi.json.internal.reflect.Reflection
import top.kkoishi.json.io.TypeParser
import top.kkoishi.json.io.TypeParserFactory
import top.kkoishi.json.parse.Factorys
import top.kkoishi.json.reflect.TypeResolver
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.util.*
import java.lang.reflect.Type as JType

class KKoishiJson {
    private val stored: MutableMap<JType, TypeParserFactory> = KKoishiJsonInit()
    val dateStyle: Int
    val timeStyle: Int
    val locale: Locale
    val useUnsafe: Boolean

    constructor(dateStyle: Int, timeStyle: Int, locale: Locale, useUnsafe: Boolean) {
        this.dateStyle = dateStyle
        this.timeStyle = timeStyle
        this.locale = locale
        this.useUnsafe = useUnsafe
    }

    @Suppress("UNCHECKED_CAST")
    private fun getParser(type: JType): TypeParser<*> {
        if (type is ParameterizedType) {
            val parameters = type.actualTypeArguments
            val raw = Reflection.getRawType(type.rawType)
            if (parameters.size == 2 && raw == MutableMap::class.java)
                return Factorys.getMapTypeFactory().create<Any, Any>(parameters[0], parameters[1])
            if (parameters.size == 1 && raw == Collection::class.java)
                return Factorys.getCollectionTypeFactory().create<Any>(parameters[0])

            if (type != Any::class.java)
                return getParser(raw)
        } else if (type is Class<*>) {
            if (Reflection.checkJsonPrimitive(type))
                return UtilParsers.getPrimitiveParser(type)
            fun getFromType() = Factorys.getFactoryFromClass(type).create(top.kkoishi.json.reflect.Type(type))

            val getter = Reflection.checkFactoryGetter(type)
            if (getter != null) {
                getter.isAccessible = true
                if (0 != getter.parameterCount)
                    return Factorys.getFactoryFromClass(type).create(top.kkoishi.json.reflect.Type(type))
                val factory: TypeParserFactory =
                    getter(if (Reflection.isStatic(getter)) null else Allocators.unsafeAny(useUnsafe)
                        .allocateInstance(type as Class<Any>)) as TypeParserFactory?
                        ?: return getFromType()
                Factorys.addType(type, factory)
                return factory.create(top.kkoishi.json.reflect.Type(type))
            }
            return Factorys.getFactoryFromClass(type).create(top.kkoishi.json.reflect.Type(type))
        } else if (type is GenericArrayType) {
            // TODO: may have bugs.
            val cmpType = type.genericComponentType
            if (cmpType is ParameterizedType) {
                val tp = top.kkoishi.json.reflect.Type<Any>(type)
                return Factorys.getFactoryFromClass(tp.rawType()).create(tp)
            }
        }
        throw IllegalStateException()
    }

    private fun getFactoryFromClass(type: Class<*>): TypeParserFactory {
        if (stored.containsKey(type))
            return stored[type]!!
        return getIfNotContainsFromClass(type)
    }

    private fun getIfNotContainsFromClass(type: Class<*>): TypeParserFactory {
        if (type.isArray)
            return Factorys.getArrayTypeFactory()
        else if (Reflection.isMapType(type) || Reflection.isCollection(type))
            throw IllegalArgumentException()
        else if (Reflection.checkJsonPrimitive(type))
            return UtilFactorys.PRIMITIVE
        else
            return Factorys.getFieldTypeFactory()
    }

    private fun <TYPE> getFactory(typeResolver: TypeResolver<TYPE>): TypeParserFactory {
        val parameterizedType = typeResolver.resolve() as ParameterizedType
        val arguments = parameterizedType.actualTypeArguments
        val key = Reflection.ParameterizedTypeImpl(parameterizedType.ownerType, parameterizedType.rawType, *arguments)
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