package top.kkoishi.json

import top.kkoishi.json.exceptions.JsonCastException
import java.math.BigDecimal
import java.math.BigInteger

abstract class JsonPrimitive : JsonElement(PRIMITIVE) {
    companion object {
        @JvmStatic
        fun create(inst: Any): JsonPrimitive {
            return object : JsonPrimitive() {
                override fun getAsString(): String = inst.toString()

                override fun getAsAny(): Any = inst
            }
        }

        @JvmStatic
        fun <T> create(inst: T, toStr: (T) -> String): JsonPrimitive {
            return object : JsonPrimitive() {
                override fun getAsString(): String = toStr(inst)

                override fun getAsAny(): Any = inst as Any
            }
        }

        @JvmStatic
        fun createActual(v: Any): JsonPrimitive {
            return when (v.javaClass) {
                Integer.TYPE, Integer::class.java -> JsonInt(v as Int)
                java.lang.Long.TYPE, java.lang.Long::class.java -> JsonLong(v as Long)
                java.lang.Byte.TYPE, java.lang.Byte::class.java -> JsonByte(v as Byte)
                java.lang.Short.TYPE, java.lang.Short::class.java -> JsonShort(v as Short)
                Character.TYPE, Character::class.java -> JsonChar(v as Char)
                java.lang.Float.TYPE, java.lang.Float::class.java -> JsonFloat(v as Float)
                java.lang.Double.TYPE, java.lang.Double::class.java -> JsonDouble(v as Double)
                java.lang.Boolean.TYPE, java.lang.Boolean::class.java -> JsonBool(v as Boolean)
                String::class.java -> JsonString("\"$v\"")
                else -> {
                    if (v is BigInteger)
                        JsonBigInteger(v)
                    else
                        JsonBigDecimal(v as BigDecimal)
                }
            }
        }

        @JvmStatic
        fun JsonPrimitive.toPrimitive(clz: Class<*>): Any {
            return when (clz) {
                Integer.TYPE, Integer::class.java -> getAsNumber().toInt()
                java.lang.Long.TYPE, java.lang.Long::class.java -> getAsNumber().toLong()
                java.lang.Byte.TYPE, java.lang.Byte::class.java -> getAsNumber().toByte()
                java.lang.Short.TYPE, java.lang.Short::class.java -> getAsNumber().toShort()
                Character.TYPE, Character::class.java -> charValue
                java.lang.Float.TYPE, java.lang.Float::class.java -> getAsNumber().toFloat()
                java.lang.Double.TYPE, java.lang.Double::class.java -> getAsNumber().toDouble()
                java.lang.Boolean.TYPE, java.lang.Boolean::class.java -> boolValue
                String::class.java -> getAsString()
                else -> clz.cast(this.getAsAny())
            }
        }
    }

    final override fun isJsonPrimitive(): Boolean = true

    final override fun toJsonPrimitive(): JsonPrimitive = this

    open var intValue: Int
        get() = Utils.uoe("top.kkoishi.json.JsonPrimitive")
        set(value) = Utils.uoe("top.kkoishi.json.JsonPrimitive")
    open var longValue: Long
        get() = Utils.uoe("top.kkoishi.json.JsonPrimitive")
        set(value) = Utils.uoe("top.kkoishi.json.JsonPrimitive")
    open var shortValue: Short
        get() = Utils.uoe("top.kkoishi.json.JsonPrimitive")
        set(value) = Utils.uoe("top.kkoishi.json.JsonPrimitive")
    open var charValue: Char
        get() = Utils.uoe("top.kkoishi.json.JsonPrimitive")
        set(value) = Utils.uoe("top.kkoishi.json.JsonPrimitive")
    open var floatValue: Float
        get() = Utils.uoe("top.kkoishi.json.JsonPrimitive")
        set(value) = Utils.uoe("top.kkoishi.json.JsonPrimitive")
    open var doubleValue: Double
        get() = Utils.uoe("top.kkoishi.json.JsonPrimitive")
        set(value) = Utils.uoe("top.kkoishi.json.JsonPrimitive")
    open var byteValue: Byte
        get() = Utils.uoe("top.kkoishi.json.JsonPrimitive")
        set(value) = Utils.uoe("top.kkoishi.json.JsonPrimitive")
    open var stringValue: String
        get() = Utils.uoe("top.kkoishi.json.JsonPrimitive")
        set(value) = Utils.uoe("top.kkoishi.json.JsonPrimitive")
    open var boolValue: Boolean
        get() = Utils.uoe("top.kkoishi.json.JsonPrimitive")
        set(value) = Utils.uoe("top.kkoishi.json.JsonPrimitive")

    fun getAsBigInteger(): BigInteger {
        if (isIntegral())
            return if (this is JsonBigInteger) this.value else BigInteger(getAsString())
        else throw UnsupportedOperationException("This is not integral")
    }

    fun getAsBigDecimal(): BigDecimal {
        if (isFloatType() || isIntegral())
            return if (this is JsonBigDecimal) this.value else BigDecimal(getAsString())
        else throw UnsupportedOperationException("This is not number type")
    }

    abstract fun getAsString(): String

    open fun getAsNumber(): Number = Utils.uoe("Can not cast to number")

    abstract fun getAsAny(): Any

    fun isJsonInt(): Boolean = this is JsonInt

    fun isJsonLong() = this is JsonLong

    fun isJsonChar() = this is JsonChar

    fun isJsonShort() = this is JsonShort

    fun isJsonFloat() = this is JsonFloat

    fun isJsonDouble() = this is JsonDouble

    fun isJsonByte() = this is JsonByte

    fun isJsonString() = this is JsonString

    fun isJsonBool() = this is JsonBool

    private inline fun <reified T : JsonPrimitive> cast(): T {
        if (this is T) {
            return this
        } else {
            throw JsonCastException("$this is a ${T::class.simpleName} instance")
        }
    }

    open fun toJsonInt() = cast<JsonInt>()

    open fun toJsonLong() = cast<JsonLong>()

    open fun toJsonChar() = cast<JsonChar>()

    open fun toJsonShort() = cast<JsonShort>()

    open fun toJsonFloat() = cast<JsonFloat>()

    open fun toJsonDouble() = cast<JsonDouble>()

    open fun toJsonByte() = cast<JsonByte>()

    fun toJsonString() = cast<JsonString>()

    fun toJsonBool() = cast<JsonBool>()

    fun toBigInteger() = cast<JsonBigInteger>()

    fun toBigDecimal() = cast<JsonBigDecimal>()

    fun isIntegral(): Boolean =
        this is JsonInt || this is JsonShort || this is JsonLong || this is JsonByte || this is JsonBigInteger

    fun isFloatType(): Boolean = this is JsonFloat || this is JsonDouble || this is JsonBigDecimal

    override fun toString(): String {
        return getAsString()
    }
}