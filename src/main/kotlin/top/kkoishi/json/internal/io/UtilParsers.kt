package top.kkoishi.json.internal.io

import top.kkoishi.json.JsonElement
import top.kkoishi.json.JsonPrimitive
import top.kkoishi.json.JsonString
import top.kkoishi.json.io.TypeParser
import top.kkoishi.json.reflect.Type
import top.kkoishi.json.reflect.TypeHelper.asType
import java.text.DateFormat
import java.util.*
import kotlin.reflect.KClass

internal object UtilParsers {
    private const val DATEFORMAT_DEFAULT = 2

    internal class DateTypeParser : TypeParser<Date>(Date::class.java.asType()) {
        override fun fromJson(json: JsonElement): Date =
            fromJson(json, DATEFORMAT_DEFAULT, DATEFORMAT_DEFAULT, Locale.getDefault(Locale.Category.FORMAT))

        internal fun fromJson(json: JsonElement, dateStyle: Int, timeStyle: Int, aLocale: Locale): Date {
            if (json.isJsonPrimitive()) {
                val primitive = json.toJsonPrimitive()
                if (primitive.isJsonString())
                    return DateFormat.getDateTimeInstance(dateStyle, timeStyle, aLocale).parse(primitive.getAsString())
            }
            return inputIllegal()
        }

        override fun toJson(t: Date): JsonElement =
            toJson(t, DATEFORMAT_DEFAULT, DATEFORMAT_DEFAULT, Locale.getDefault(Locale.Category.FORMAT))

        internal fun toJson(t: Date, dateStyle: Int, timeStyle: Int, aLocale: Locale): JsonElement {
            return JsonString(DateFormat.getDateTimeInstance(dateStyle, timeStyle, aLocale).format(t))
        }
    }

    @JvmStatic
    internal fun getPrimitiveParser(clz: Class<*>): TypeParser<Any> {
        return when (clz) {
            Int::class.java, Integer::class.java -> INT
            Long::class.java, java.lang.Long::class.java -> LONG
            Short::class.java, java.lang.Short::class.java -> SHORT
            String::class.java -> STRING
            // This should not happen.
            else -> throw IllegalArgumentException()
        }
    }

    private abstract class IntegralPrimitiveTypeParser(type: Type<Any>): TypeParser<Any>(type) {
        abstract fun cast(str: String): Any

        abstract fun cast(number: Number): Any

        override fun fromJson(json: JsonElement): Any {
            if (json.isJsonPrimitive()) {
                val primitive = json.toJsonPrimitive()
                if (primitive.isIntegral()) {
                    return cast(primitive.getAsNumber())
                }
                if (primitive.isFloatType()) {
                    return cast(primitive.getAsNumber())
                }
                if (primitive.isJsonString()) {
                    return cast(primitive.stringValue)
                }
                return primitive.getAsAny()
            }
            throw IllegalArgumentException()
        }

        override fun toJson(t: Any): JsonElement = JsonPrimitive.createActual(t)
    }

    @JvmStatic
    private fun <T> inputIllegal(type: KClass<*> = JsonPrimitive::class): T =
        throw IllegalArgumentException("The input JsonElement must be ${type.simpleName}")

    @JvmStatic
    internal val DATE: TypeParser<Date> = DateTypeParser()

    @JvmStatic
    private val INT: TypeParser<Any> = object : IntegralPrimitiveTypeParser(Type<Any>(Int::class.java)) {
        override fun cast(str: String): Int = Integer.valueOf(str)
        override fun cast(number: Number): Int = number.toInt()
    }

    @JvmStatic
    private val LONG: TypeParser<Any> = object : IntegralPrimitiveTypeParser(Type<Any>(Long::class.java)) {
        override fun cast(str: String): Long = java.lang.Long.valueOf(str)
        override fun cast(number: Number): Long = number.toLong()
    }

    @JvmStatic
    private val SHORT: TypeParser<Any> = object : IntegralPrimitiveTypeParser(Type<Any>(Short::class.java)) {
        override fun cast(str: String): Short = java.lang.Short.valueOf(str)
        override fun cast(number: Number): Short = number.toShort()
    }

    @JvmStatic
    private val STRING: TypeParser<Any> = object : IntegralPrimitiveTypeParser(Type<Any>(String::class.java)) {
        override fun cast(str: String): Any = str
        override fun cast(number: Number): Any = number.toString()
    }
}