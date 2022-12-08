package top.kkoishi.json.internal.io

import top.kkoishi.json.internal.reflect.Allocators
import top.kkoishi.json.internal.reflect.Reflection
import top.kkoishi.json.io.TypeParser
import top.kkoishi.json.io.TypeParserFactory
import top.kkoishi.json.parse.Factorys
import top.kkoishi.json.reflect.TypeHelper
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import top.kkoishi.json.reflect.Type as KType

internal object ParserManager {
    @Suppress("UNCHECKED_CAST")
    fun getParser(type: Type): TypeParser<*> {
        if (type is ParameterizedType) {
            val parameters = type.actualTypeArguments
            if (parameters.size == 2)
                return Factorys.getMapTypeFactory().create<Any, Any>(parameters[0], parameters[1])
            if (parameters.size == 1) {
                return Factorys.getCollectionTypeFactory().create<Any>(parameters[0])
            }
            val owner = type.ownerType
            if (owner is Class<*>)
                return getParser(type.ownerType)
        } else if (type is Class<*>) {
            if (Reflection.checkJsonPrimitive(type))
                return jsonPrimitiveParser(type)
            fun getFromType() = Factorys.getFactoryFromClass(type).create(KType(type))

            val getter = Reflection.checkFactoryGetter(type)
            if (getter != null) {
                getter.isAccessible = true
                if (0 != getter.parameterCount)
                    return Factorys.getFactoryFromClass(type).create(KType(type))
                val factory: TypeParserFactory =
                    getter(if (Reflection.isStatic(getter)) null else Allocators.unsafeAny(true)
                        .allocateInstance(type as Class<Any>)) as TypeParserFactory?
                        ?: return getFromType()
                Factorys.addType(type, factory)
                return factory.create(KType(type))
            }
            return Factorys.getFactoryFromClass(type).create(KType(type))
        } else if (type is GenericArrayType) {
            // TODO: may have bugs.
            val cmpType = type.genericComponentType
            if (cmpType is ParameterizedType) {
                val tp = KType<Any>(type)
                return Factorys.getFactoryFromClass(tp.rawType()).create(tp)
            }
        }
        throw IllegalStateException()
    }

    @JvmStatic
    private fun jsonPrimitiveParser(clz: Class<*>): TypeParser<Any> = UtilParsers.getPrimitiveParser(clz)
}