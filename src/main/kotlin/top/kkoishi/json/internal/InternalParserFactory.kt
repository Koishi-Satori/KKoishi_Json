package top.kkoishi.json.internal

import top.kkoishi.json.JsonElement
import top.kkoishi.json.Kson
import top.kkoishi.json.internal.io.UtilParsers
import top.kkoishi.json.internal.reflect.Reflection
import top.kkoishi.json.io.TypeParser
import top.kkoishi.json.io.TypeParserFactory
import top.kkoishi.json.reflect.Type
import java.util.*
import java.lang.reflect.Type as JType

internal object InternalParserFactory {
    /**
     * Some internal parser/factory might use some properties in Kson, so they need to implement
     * this interface to get Kson instance.
     */
    internal interface Conditional {
        val instance: Kson
    }

    /**
     * This interface is used to avoid repeated initialization when construct KsonBuilder
     * instance using Kson instance.
     *
     * @see top.kkoishi.json.KsonBuilder
     */
    internal interface InitFactory

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    internal fun getFactory(require: Class<*>, inst: TypeParser<*>): Pair<JType, TypeParserFactory> {
        // get anonymous class implements InitFactory and extends TypeParserFactory.
        return (require to object : TypeParserFactory, InitFactory {
            override fun <T : Any> create(type: Type<T>): TypeParser<T> {
                if (Reflection.isType(type, require))
                    return inst as TypeParser<T>
                throw IllegalArgumentException("The type $type must be $require")
            }
        })
    }

    class DateParser(override val instance: Kson) : UtilParsers.DateTypeParser(), Conditional {
        override fun fromJson(json: JsonElement): Date =
            super.fromJson(json, instance.dateStyle, instance.timeStyle, instance.locale)

        override fun toJson(t: Date): JsonElement =
            super.toJson(t, instance.dateStyle, instance.timeStyle, instance.locale)
    }
}