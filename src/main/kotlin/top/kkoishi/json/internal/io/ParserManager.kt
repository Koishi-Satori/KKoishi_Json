package top.kkoishi.json.internal.io

import top.kkoishi.json.internal.reflect.Allocators
import top.kkoishi.json.internal.reflect.Reflection
import top.kkoishi.json.io.TypeParser
import top.kkoishi.json.io.TypeParserFactory
import top.kkoishi.json.parse.Factorys
import java.lang.reflect.*
import top.kkoishi.json.reflect.Type as KType

internal object ParserManager {
    @Suppress("UNCHECKED_CAST")
    fun getParser(type: Type): TypeParser<*> {
        if (type is ParameterizedType) {
            val parameters = type.actualTypeArguments
            val raw = Reflection.getRawType(type.rawType)
            if (parameters.size == 2 && Reflection.isMap(raw))
                return Factorys.getMapTypeFactory().create<Any, Any>(parameters[0], parameters[1])
            if (parameters.size == 1 && Reflection.isCollection(raw))
                return Factorys.getCollectionTypeFactory().create<Any>(parameters[0])

            if (type != Any::class.java)
                return getParser(raw)
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