@file:JvmName("UtilsKt")

package top.kkoishi.json

import java.math.BigDecimal
import java.math.BigInteger

object Utils {
    @JvmStatic
    @JvmOverloads
    internal fun <T : Any> uoe(s: String = ""): T = throw UnsupportedOperationException(s)
}

class JsonInt(override var intValue: Int) : JsonPrimitive() {
    override fun getAsString(): String = intValue.toString()
    override fun getAsAny(): Any = intValue
    override fun getAsNumber(): Number = intValue
}

class JsonLong(override var longValue: Long) : JsonPrimitive() {
    override fun getAsAny(): Any = longValue
    override fun getAsString(): String = longValue.toString()
    override fun getAsNumber(): Number = longValue
}

class JsonChar(override var charValue: Char) : JsonPrimitive() {
    override fun getAsAny(): Any = charValue
    override fun getAsString(): String = "\"$charValue\""
}

class JsonShort(override var shortValue: Short) : JsonPrimitive() {
    override fun getAsAny(): Any = shortValue
    override fun getAsString(): String = shortValue.toString()
    override fun getAsNumber(): Number = shortValue
}

class JsonFloat(override var floatValue: Float) : JsonPrimitive() {
    override fun getAsAny(): Any = floatValue
    override fun getAsString(): String = floatValue.toString()
    override fun getAsNumber(): Number = floatValue
}

class JsonDouble(override var doubleValue: Double) : JsonPrimitive() {
    override fun getAsAny(): Any = doubleValue
    override fun getAsString(): String = doubleValue.toString()
    override fun getAsNumber(): Number = doubleValue
}

class JsonByte(override var byteValue: Byte) : JsonPrimitive() {
    override fun getAsAny(): Any = byteValue
    override fun getAsString(): String = byteValue.toString()
    override fun getAsNumber(): Number = byteValue
}

class JsonString(override var stringValue: String) : JsonPrimitive() {
    override fun getAsAny(): Any = stringValue
    override fun getAsString(): String = stringValue

    override fun toJsonChar(): JsonChar {
        if (stringValue.isEmpty())
            throw UnsupportedOperationException("String value is empty")
        return JsonChar(stringValue[0])
    }
}

class JsonBool(override var boolValue: Boolean) : JsonPrimitive() {
    override fun getAsAny(): Any = boolValue
    override fun getAsString(): String = boolValue.toString()
}

class JsonBigInteger(var value: BigInteger) : JsonPrimitive() {
    override fun getAsAny(): Any = value
    override var intValue: Int
        get() = value.toInt()
        set(value) {
            this.value = BigInteger(value.toString())
        }
    override var longValue: Long
        get() = value.toLong()
        set(value) {
            this.value = BigInteger(value.toString())
        }
    override var shortValue: Short
        get() = value.toShort()
        set(value) {
            this.value = BigInteger(value.toString())
        }
    override var byteValue: Byte
        get() = value.toByte()
        set(value) {
            this.value = BigInteger(value.toString())
        }

    override fun toJsonInt(): JsonInt {
        return JsonInt(intValue)
    }

    override fun toJsonLong(): JsonLong {
        return JsonLong(longValue)
    }

    override fun toJsonShort(): JsonShort {
        return JsonShort(shortValue)
    }

    override fun toJsonByte(): JsonByte {
        return JsonByte(byteValue)
    }

    override fun getAsString(): String = value.toString()
    override fun getAsNumber(): Number = longValue
}

class JsonBigDecimal(var value: BigDecimal) : JsonPrimitive() {
    override fun getAsAny(): Any = value
    override var floatValue: Float
        get() = super.floatValue
        set(value) {}
    override var doubleValue: Double
        get() = super.doubleValue
        set(value) {}

    override fun getAsString(): String = value.toString()
    override fun getAsNumber(): Number = doubleValue
}