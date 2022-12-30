package top.kkoishi.json.internal.io

import top.kkoishi.json.internal.reflect.Reflection.isType
import top.kkoishi.json.io.TypeParser
import top.kkoishi.json.io.TypeParserFactory
import top.kkoishi.json.reflect.Type
import java.lang.reflect.Type as JType
import java.util.*

internal object UtilFactorys {
    internal fun init(stored: MutableMap<JType, TypeParserFactory>) {
        stored[Date::class.java] = getFactory(Date::class.java, UtilParsers.DATE)
        stored[UUID::class.java] = getFactory(UUID::class.java, UtilParsers.UUID)
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    val PRIMITIVE: TypeParserFactory = object : TypeParserFactory {
        override fun <T : Any> create(type: Type<T>): TypeParser<T> {
            val clz = type.type()
            if (clz !is Class<*>)
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