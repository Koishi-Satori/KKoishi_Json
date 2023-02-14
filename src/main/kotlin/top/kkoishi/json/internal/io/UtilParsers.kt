package top.kkoishi.json.internal.io

import top.kkoishi.json.*
import top.kkoishi.json.exceptions.JsonInternalException
import top.kkoishi.json.io.TypeParser
import top.kkoishi.json.reflect.Type
import top.kkoishi.json.reflect.TypeHelper.asType
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.text.DateFormat
import java.time.ZoneId
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.pathString
import java.lang.reflect.Type as JType
import kotlin.reflect.KClass

internal object UtilParsers {
    private const val DATEFORMAT_DEFAULT = 2

    internal open class DateTypeParser : TypeParser<Date>(Date::class.java.asType()) {
        override fun fromJson(json: JsonElement): Date =
            fromJson(json, DATEFORMAT_DEFAULT, DATEFORMAT_DEFAULT, Locale.getDefault(Locale.Category.FORMAT))

        internal fun fromJson(json: JsonElement, dateStyle: Int, timeStyle: Int, aLocale: Locale): Date {
            if (json.isJsonPrimitive()) {
                val primitive = json.toJsonPrimitive()
                if (primitive.isJsonString())
                    return DateFormat.getDateTimeInstance(dateStyle, timeStyle, aLocale).parse(primitive.getAsString())
            }
            return iae(json, "Date", "Require JsonString")
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
            BigInteger::class.java -> BIG_INTEGER
            // The else option can not be a class out of BigDecimal, this should not happen.
            else -> BIG_DECIMAL
        }
    }

    private abstract class PrimitiveTypeParser(type: Type<Any>) : TypeParser<Any>(type) {
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
            return iae(json, type.rawType().typeName, "Type cast failed")
        }

        override fun toJson(t: Any): JsonElement = JsonPrimitive.createActual(t)
    }

    @JvmStatic
    private fun <T> iae(json: JsonElement, target: String, cause: String? = null): T =
        throw JsonInternalException("Can not serialize $json to $target${if (cause == null) "" else ": $cause"}")

    @JvmStatic
    private fun <T> iae(json: JsonElement, target: String, cause: Throwable): T =
        throw JsonInternalException("Can not serialize $json to $target$", cause)

    @JvmStatic
    @Suppress("NOTHING_TO_INLINE")
    private inline fun getArray(json: JsonElement, target: String): JsonArray {
        if (json.isJsonArray())
            return json.toJsonArray()
        return iae(json, target)
    }

    @JvmStatic
    internal val DATE: TypeParser<Date> = DateTypeParser()

    @JvmStatic
    internal val UUID: TypeParser<UUID> = object : TypeParser<UUID>(Type(java.util.UUID::class.java)) {
        override fun fromJson(json: JsonElement): UUID {
            if (json.isJsonPrimitive()) {
                val primitive = json.toJsonPrimitive()
                if (primitive.isJsonString())
                    return try {
                        java.util.UUID.fromString(primitive.getAsString())
                    } catch (e: Exception) {
                        iae(json, "UUID", e)
                    }
            }
            return iae(json, "UUID", "Require JsonString")
        }

        override fun toJson(t: UUID): JsonElement = JsonString(t.toString())
    }

    @JvmStatic
    internal val BITSET: TypeParser<BitSet> = object : TypeParser<BitSet>(Type(BitSet::class.java)) {
        override fun fromJson(json: JsonElement): BitSet = with(getArray(json, "BitSet")) {
            return try {
                BitSet.valueOf(LongArray(this.size()) { index ->
                    this[index].toJsonPrimitive().getAsNumber().toLong()
                })
            } catch (e: Exception) {
                iae(json, "BitSet", e)
            }
        }

        override fun toJson(t: BitSet): JsonElement {
            val arr = JsonArray()
            Arrays.stream(t.toLongArray()).forEach { arr.add(JsonLong(it)) }
            return arr
        }
    }

    @JvmStatic
    internal val CALENDER: TypeParser<Calendar> = object : TypeParser<Calendar>(Type(Calendar::class.java)) {
        override fun fromJson(json: JsonElement): Calendar {
            if (json.isJsonPrimitive()) {
                val primitive = json.toJsonPrimitive()
                if (primitive.isJsonString())
                    return Calendar.getInstance(TimeZone.getTimeZone(primitive.getAsString()))
            }
            return iae(json, "Calender", "Require JsonString")
        }

        override fun toJson(t: Calendar): JsonElement = JsonString(t.timeZone.toZoneId().toString())
    }

    @JvmStatic
    internal val TIME_ZONE: TypeParser<TimeZone> = object : TypeParser<TimeZone>(Type(TimeZone::class.java)) {
        override fun fromJson(json: JsonElement): TimeZone {
            if (json.isJsonPrimitive()) {
                val primitive = json.toJsonPrimitive()
                if (primitive.isJsonString())
                    return TimeZone.getTimeZone(primitive.getAsString())
            }
            return iae(json, "TimeZone", "Require JsonString")
        }

        override fun toJson(t: TimeZone): JsonElement = JsonString(t.toZoneId().toString())
    }

    @JvmStatic
    internal val ZONE_ID: TypeParser<ZoneId> = object : TypeParser<ZoneId>(Type(ZoneId::class.java)) {
        override fun fromJson(json: JsonElement): ZoneId {
            if (json.isJsonPrimitive()) {
                val primitive = json.toJsonPrimitive()
                if (primitive.isJsonString())
                    return ZoneId.of(primitive.getAsString())
            }
            return iae(json, "ZoneId", "Require JsonString")
        }

        override fun toJson(t: ZoneId): JsonElement = JsonString(t.toString())
    }

    @JvmStatic
    internal val RANDOM: TypeParser<Random> = object : TypeParser<Random>(Type(Random::class.java)) {
        private val OFFSET: Long = top.kkoishi.json.internal.Utils.objectFieldOffset(Random::class.java.getDeclaredField("seed"))

        override fun fromJson(json: JsonElement): Random {
            if (json.isJsonPrimitive()) {
                val primitive = json.toJsonPrimitive()
                if (primitive.isJsonLong())
                    return Random(primitive.getAsNumber().toLong())
            }
            return iae(json, "Random", "Require JsonLong")
        }

        override fun toJson(t: Random): JsonElement {
            return JsonLong((top.kkoishi.json.internal.Utils.getObject(t, OFFSET) as AtomicLong).get())
        }
    }

    @JvmStatic
    internal val FILE: TypeParser<File> = object : TypeParser<File>(Type(File::class.java)) {
        override fun fromJson(json: JsonElement): File {
            if (json.isJsonPrimitive()) {
                val primitive = json.toJsonPrimitive()
                return File(primitive.getAsString())
            }
            if (json.isJsonObject()) {
                val filename = json.toJsonObject()["filename"]
                if (filename != null)
                    return File(filename.toJsonPrimitive().getAsString())
            }
            return iae(json, "File", "Illegal format")
        }

        override fun toJson(t: File): JsonElement {
            return JsonString(t.path)
        }
    }

    @JvmStatic
    internal val PATH: TypeParser<Path> = object : TypeParser<Path>(Type(Path::class.java)) {
        override fun fromJson(json: JsonElement): Path {
            if (json.isJsonPrimitive()) {
                val primitive = json.toJsonPrimitive()
                return try {
                    Path.of(primitive.getAsString())
                } catch (e: Exception) {
                    iae(json, "Path", e)
                }
            }
            return with(getArray(json, "Path")) {
                if (this.isEmpty())
                    return iae(json, "Path", "Path can not be empty")
                if (this.size() == 1) try {
                    Path.of(this[0].toJsonPrimitive().getAsString())
                } catch (e: Exception) {
                    iae(json, "Path", e)
                }
                else try {
                    Path.of(this[0].toJsonPrimitive().getAsString(),
                        *Array(this.size() - 1) { index -> this[index + 1].toJsonPrimitive().getAsString() })
                } catch (e: Exception) {
                    iae(json, "Path", e)
                }
            }
        }

        override fun toJson(t: Path): JsonElement {
            return JsonString(t.pathString)
        }
    }

    @JvmStatic
    internal val URL: TypeParser<URL> = object : TypeParser<URL>(Type(java.net.URL::class.java)) {
        @Suppress("NOTHING_TO_INLINE")
        private inline fun fromJsonObject(obj: JsonObject): URL {
            val protocol = obj["protocol"] ?: return iae(obj, "URL")
            val host = obj["host"] ?: return iae(obj, "URL")
            val file = obj["file"] ?: return iae(obj, "URL")
            if (protocol.isJsonPrimitive() && host.isJsonPrimitive() && file.isJsonPrimitive()) {
                val protocolStr = protocol.toJsonPrimitive().getAsString()
                val hostStr = host.toJsonPrimitive().getAsString()
                val fileStr = file.toJsonPrimitive().getAsString()
                val port = obj["port"] ?: return URL(protocolStr, hostStr, fileStr)
                if (port.isJsonPrimitive()) {
                    val portInt = port.toJsonPrimitive()
                    if (portInt.isIntegral())
                        return URL(protocolStr, hostStr, portInt.getAsNumber().toInt(), fileStr)
                }
            }
            return iae(obj, "URL", "Illegal format")
        }

        override fun fromJson(json: JsonElement): URL {
            try {
                if (json.isJsonObject())
                    return fromJsonObject(json.toJsonObject())
                else if (json.isJsonPrimitive()) {
                    val primitive = json.toJsonPrimitive()
                    if (primitive.isJsonString())
                        return URL(primitive.getAsString())
                }
            } catch (e: Exception) {
                throw JsonInternalException(e)
            }
            return iae(json, "URL")
        }

        override fun toJson(t: URL): JsonElement {
            return JsonString(t.toExternalForm())
        }
    }

    @JvmStatic
    internal val URI: TypeParser<URI> = object : TypeParser<URI>(Type(java.net.URI::class.java)) {
        override fun fromJson(json: JsonElement): URI {
            TODO("Not yet implemented")
        }

        override fun toJson(t: URI): JsonElement {
            TODO("Not yet implemented")
        }
    }

    /*----------------------------- Primitive Parsers -----------------------------*/

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

    @JvmStatic
    private val BIG_INTEGER: TypeParser<Any> = object : PrimitiveTypeParser(Type<Any>(BigInteger::class.java)) {
        override fun cast(str: String): BigInteger = BigInteger(str)
        override fun cast(number: Number): BigInteger = BigInteger.valueOf(number.toLong())
    }

    @JvmStatic
    private val BIG_DECIMAL: TypeParser<Any> = object : PrimitiveTypeParser(Type<Any>(BigDecimal::class.java)) {
        override fun cast(str: String): BigDecimal = BigDecimal(str)
        override fun cast(number: Number): BigDecimal = BigDecimal.valueOf(number.toDouble())
    }
}