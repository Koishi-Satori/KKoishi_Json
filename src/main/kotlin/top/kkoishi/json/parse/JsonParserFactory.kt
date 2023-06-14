package top.kkoishi.json.parse

import top.kkoishi.json.*
import java.math.BigDecimal
import java.math.BigInteger

class JsonParserFactory @JvmOverloads constructor(
    val platform: Platform = Platform.LINUX,
    val mode: NumberMode = NumberMode.ALL_TYPE,
    val processEscape: Boolean = true,
) {
    internal companion object {
        @JvmStatic
        private val DEC_FLOAT_MAX = BigDecimal(Float.MAX_VALUE.toDouble())

        @JvmStatic
        private val DEC_DOUBLE_MAX = BigDecimal(Double.MAX_VALUE)

        @JvmStatic
        private val INT_BYTE_MAX = BigInteger("7f", 16)

        @JvmStatic
        private val INT_BYTE_MIN = BigInteger("-80", 16)

        @JvmStatic
        private val INT_SHORT_MAX = BigInteger("7fff", 16)

        @JvmStatic
        private val INT_SHORT_MIN = BigInteger("-8000", 16)

        @JvmStatic
        private val INT_INT_MAX = BigInteger("7fffffff", 16)

        @JvmStatic
        private val INT_INT_MIN = BigInteger("-80000000", 16)

        @JvmStatic
        private val INT_LONG_MAX = BigInteger("7fffffffffffffff", 16)

        @JvmStatic
        private val INT_LONG_MIN = BigInteger("-8000000000000000", 16)

        class AllTypeJsonParser(iterator: Iterator<Char>, platform: Platform, processEscape: Boolean) :
            JsonParser(iterator, platform, processEscape) {
            override fun judgeNumber(num: String): JsonPrimitive {
                if (num.last() == 'f') {
                    val digit = BigDecimal(num.substring(0, num.length - 1))
                    val abs = digit.abs()
                    if (abs <= DEC_FLOAT_MAX)
                        return JsonFloat(digit.toFloat())
                    if (abs <= DEC_DOUBLE_MAX)
                        return JsonDouble(digit.toDouble())
                    return JsonBigDecimal(digit)
                }
                val digit = BigInteger(num)
                if (digit.signum() == -1) {
                    if (digit >= INT_BYTE_MIN)
                        return JsonByte(digit.toByte())
                    if (digit >= INT_SHORT_MIN)
                        return JsonShort(digit.toShort())
                    if (digit >= INT_INT_MIN)
                        return JsonInt(digit.toInt())
                    if (digit >= INT_LONG_MIN)
                        return JsonLong(digit.toLong())
                } else {
                    if (digit <= INT_BYTE_MAX)
                        return JsonByte(digit.toByte())
                    if (digit <= INT_SHORT_MAX)
                        return JsonShort(digit.toShort())
                    if (digit <= INT_INT_MAX)
                        return JsonInt(digit.toInt())
                    if (digit <= INT_LONG_MAX)
                        return JsonLong(digit.toLong())
                }
                return JsonBigInteger(digit)
            }
        }

        class UntilShortJsonParser(iterator: Iterator<Char>, platform: Platform, processEscape: Boolean) :
            JsonParser(iterator, platform, processEscape) {
            override fun judgeNumber(num: String): JsonPrimitive {
                if (num.last() == 'f') {
                    val digit = BigDecimal(num.substring(0, num.length - 1))
                    val abs = digit.abs()
                    if (abs <= DEC_FLOAT_MAX)
                        return JsonFloat(digit.toFloat())
                    if (abs <= DEC_DOUBLE_MAX)
                        return JsonDouble(digit.toDouble())
                    return JsonBigDecimal(digit)
                }
                val digit = BigInteger(num)
                if (digit.signum() == -1) {
                    if (digit >= INT_SHORT_MIN)
                        return JsonShort(digit.toShort())
                    if (digit >= INT_INT_MIN)
                        return JsonInt(digit.toInt())
                    if (digit >= INT_LONG_MIN)
                        return JsonLong(digit.toLong())
                } else {
                    if (digit <= INT_SHORT_MAX)
                        return JsonShort(digit.toShort())
                    if (digit <= INT_INT_MAX)
                        return JsonInt(digit.toInt())
                    if (digit <= INT_LONG_MAX)
                        return JsonLong(digit.toLong())
                }
                return JsonBigInteger(digit)
            }
        }

        class UntilIntJsonParser(iterator: Iterator<Char>, platform: Platform, processEscape: Boolean) :
            JsonParser(iterator, platform, processEscape) {
            override fun judgeNumber(num: String): JsonPrimitive {
                if (num.last() == 'f') {
                    val digit = BigDecimal(num.substring(0, num.length - 1))
                    val abs = digit.abs()
                    if (abs <= DEC_FLOAT_MAX)
                        return JsonFloat(digit.toFloat())
                    if (abs <= DEC_DOUBLE_MAX)
                        return JsonDouble(digit.toDouble())
                    return JsonBigDecimal(digit)
                }
                val digit = BigInteger(num)
                if (digit.signum() == -1) {
                    if (digit >= INT_INT_MIN)
                        return JsonInt(digit.toInt())
                    if (digit >= INT_LONG_MIN)
                        return JsonLong(digit.toLong())
                } else {
                    if (digit <= INT_INT_MAX)
                        return JsonInt(digit.toInt())
                    if (digit <= INT_LONG_MAX)
                        return JsonLong(digit.toLong())
                }
                return JsonBigInteger(digit)
            }
        }

        class UntilLongJsonParser(iterator: Iterator<Char>, platform: Platform, processEscape: Boolean) :
            JsonParser(iterator, platform, processEscape) {
            override fun judgeNumber(num: String): JsonPrimitive {
                if (num.last() == 'f') {
                    val digit = BigDecimal(num.substring(0, num.length - 1))
                    val abs = digit.abs()
                    if (abs <= DEC_DOUBLE_MAX)
                        return JsonDouble(digit.toDouble())
                    return JsonBigDecimal(digit)
                }
                val digit = BigInteger(num)
                if (digit.signum() == -1)
                    if (digit >= INT_LONG_MIN)
                        return JsonLong(digit.toLong())
                    else
                        if (digit <= INT_LONG_MAX)
                            return JsonLong(digit.toLong())
                return JsonBigInteger(digit)
            }
        }

        class BigNumberJsonParser(iterator: Iterator<Char>, platform: Platform, processEscape: Boolean) :
            JsonParser(iterator, platform, processEscape) {
            override fun judgeNumber(num: String): JsonPrimitive {
                if (num.last() == 'f')
                    return JsonBigDecimal(BigDecimal(num.substring(0, num.length - 1)))
                return JsonBigInteger(BigInteger(num))
            }
        }

        class BigDecOnlyJsonParser(iterator: Iterator<Char>, platform: Platform, processEscape: Boolean) :
            JsonParser(iterator, platform, processEscape) {
            override fun judgeNumber(num: String): JsonPrimitive {
                if (num.last() == 'f')
                    return JsonBigDecimal(BigDecimal(num.substring(0, num.length - 1)))
                return JsonBigDecimal(BigDecimal(num))
            }
        }
    }

    fun create(json: String) = create(json.iterator())

    fun create() = create(JsonLexerFactory.Companion.EmptyCharIterator())

    fun create(iterator: Iterator<Char>): JsonParser {
        when (mode) {
            NumberMode.ALL_TYPE -> return AllTypeJsonParser(iterator, platform, processEscape)
            NumberMode.BIG_NUMBER_ONLY -> return BigNumberJsonParser(iterator, platform, processEscape)
            NumberMode.UNTIL_INT -> return UntilIntJsonParser(iterator, platform, processEscape)
            NumberMode.UNTIL_SHORT -> return UntilShortJsonParser(iterator, platform, processEscape)
            NumberMode.UNTIL_LONG -> return UntilLongJsonParser(iterator, platform, processEscape)
            NumberMode.BIG_DECIMAL_ONLY -> return BigDecOnlyJsonParser(iterator, platform, processEscape)
            else -> throw IllegalArgumentException()
        }
    }
}