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
            if (Reflection.isType(type, TypeHelper.TypeParserFactoryGetter::class.java)) {
                val getter = type.getDeclaredMethod("getTypeParser")
                getter.isAccessible = true
                val parser =
                    getter(Allocators.unsafeAny(true).allocateInstance(type as Class<Any>)) as TypeParserFactory
                Factorys.addSpec(type, parser)
                return parser.create(top.kkoishi.json.reflect.Type(type))
            }
            return Factorys.getFactoryFromType(type).create(top.kkoishi.json.reflect.Type(type))
        } else if (type is GenericArrayType) {
            // TODO: may have bugs.
            val cmpType = type.genericComponentType
            if (cmpType is ParameterizedType) {
                val tp = top.kkoishi.json.reflect.Type<Any>(type)
                return Factorys.getFactoryFromType(tp.rawType()).create(tp)
            }
        }
        throw IllegalStateException()
    }

    private fun jsonPrimitiveParser(clz: Class<*>): TypeParser<Any> = UtilParsers.getPrimitiveParser(clz)
}