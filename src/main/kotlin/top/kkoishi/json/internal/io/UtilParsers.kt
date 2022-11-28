package top.kkoishi.json.internal.io

import top.kkoishi.json.*
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
            Byte::class.java, java.lang.Byte::class.java -> BYTE
            Float::class.java, java.lang.Float::class.java -> FLOAT
            Double::class.java, java.lang.Double::class.java -> DOUBLE
            Boolean::class.java, java.lang.Boolean::class.java -> BOOL
            Char::class.java, Character::class.java -> CHAR
            String::class.java -> STRING
            // This should not happen.
            else -> throw IllegalArgumentException()
        }
    }

    private abstract class PrimitiveTypeParser(type: Type<Any>): TypeParser<Any>(type) {
        abstract fun cast(str: String): Any
        abstract fun cast(number: Number): Any

        override fun fromJson(json: JsonElement): Any {
            if (json.isJsonPrimitive()) {
                val primitive = json.toJsonPrimitive()
                if (primitive.isIntegral()) {
                    if (primitive is JsonBigInteger)
                        return primitive.getAsBigInteger()
                    return cast(primitive.getAsNumber())
                }
                if (primitive.isFloatType()) {
                    if (primitive is JsonBigDecimal)
                        return primitive.getAsBigDecimal()
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
    private val INT: TypeParser<Any> = object : PrimitiveTypeParser(Type<Any>(Int::class.java)) {
        override fun cast(str: String): Int = Integer.valueOf(str)
        override fun cast(number: Number): Int = number.toInt()
    }

    @JvmStatic
    private val LONG: TypeParser<Any> = object : PrimitiveTypeParser(Type<Any>(Long::class.java)) {
        override fun cast(str: String): Long = java.lang.Long.valueOf(str)
        override fun cast(number: Number): Long = number.toLong()
    }

    @JvmStatic
    private val SHORT: TypeParser<Any> = object : PrimitiveTypeParser(Type<Any>(Short::class.java)) {
        override fun cast(str: String): Short = java.lang.Short.valueOf(str)
        override fun cast(number: Number): Short = number.toShort()
    }

    @JvmStatic
    private val BYTE: TypeParser<Any> = object : PrimitiveTypeParser(Type<Any>(Byte::class.java)) {
        override fun cast(str: String): Byte = java.lang.Byte.valueOf(str)
        override fun cast(number: Number): Byte = number.toInt().toByte()
    }

    @JvmStatic
    private val FLOAT: TypeParser<Any> = object : PrimitiveTypeParser(Type<Any>(Float::class.java)) {
        override fun cast(str: String): Float = java.lang.Float.valueOf(str)
        override fun cast(number: Number): Float = number.toFloat()
    }

    @JvmStatic
    private val DOUBLE: TypeParser<Any> = object : PrimitiveTypeParser(Type<Any>(Double::class.java)) {
        override fun cast(str: String): Double = java.lang.Double.valueOf(str)
        override fun cast(number: Number): Double = number.toDouble()
    }

    @JvmStatic
    private val CHAR: TypeParser<Any> = object : PrimitiveTypeParser(Type<Any>(Char::class.java)) {
        override fun cast(str: String): Char {
            if (str.isEmpty())
                return '\u0000'
            return str[0]
        }
        override fun cast(number: Number): Char = number.toChar()
    }

    @JvmStatic
    private val BOOL: TypeParser<Any> = object : PrimitiveTypeParser(Type<Any>(Boolean::class.java)) {
        override fun cast(str: String): Boolean = java.lang.Boolean.valueOf(str)
        override fun cast(number: Number): Boolean = (number == 1)
    }

    @JvmStatic
    private val STRING: TypeParser<Any> = object : PrimitiveTypeParser(Type<Any>(String::class.java)) {
        override fun cast(str: String): Any = str
        override fun cast(number: Number): Any = number.toString()
    }
}