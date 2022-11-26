package top.kkoishi.json.internal.io

import top.kkoishi.json.internal.reflect.Reflection
import top.kkoishi.json.internal.reflect.Reflection.isType
import top.kkoishi.json.io.TypeParser
import top.kkoishi.json.io.TypeParserFactory
import top.kkoishi.json.reflect.Type
import java.util.*

internal object UtilFactorys {
    @JvmStatic
    val DATE: TypeParserFactory = getFactory(Date::class.java, UtilParsers.DATE)

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    val PRIMITIVE: TypeParserFactory = object : TypeParserFactory {
        override fun <T : Any> create(type: Type<T>): TypeParser<T> {
            val clz = type.type()
            if (clz !is Class<*> || !Reflection.isWrapped(clz))
                throw IllegalArgumentException()
            return UtilParsers.getPrimitiveParser(clz) as TypeParser<T>
        }
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    private fun getFactory(require: Class<*>, inst: TypeParser<*>): TypeParserFactory {
        return object : TypeParserFactory {
            override fun <T : Any> create(type: Type<T>): TypeParser<T> {
                if (isType(type, require))
                    return inst as TypeParser<T>
                throw IllegalArgumentException("The type $type must be $require")
            }
        }
    }
}