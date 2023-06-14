package top.kkoishi.json.internal.io

import top.kkoishi.json.internal.reflect.Allocators
import top.kkoishi.json.internal.reflect.Reflection
import top.kkoishi.json.io.TypeParser
import top.kkoishi.json.io.TypeParserFactory
import top.kkoishi.json.parse.Factorys
import java.lang.reflect.*
import top.kkoishi.json.reflect.Type as KType

internal object ParserManager {
    @Suppress("UNCHECKED_CAST")
    fun getParser(type: Type): TypeParser<*> {
        if (type is ParameterizedType) {
            val factory = Factorys.get(type)
            if (factory != null)
                return factory.create(KType(type))

            val parameters = type.actualTypeArguments
            val raw = Reflection.getRawType(type.rawType)
            if (parameters.size == 2 && Reflection.isMap(raw))
                return Factorys.getMapTypeFactory().create<Any, Any>(parameters[0], parameters[1])
            if (parameters.size == 1 && Reflection.isCollection(raw))
                return Factorys.getCollectionTypeFactory().create<Any>(parameters[0])

            if (type != Any::class.java)
                return getParser(raw)
        } else if (type is Class<*>) {
            if (Reflection.checkJsonPrimitive(type))
                return jsonPrimitiveParser(type)
            fun getFromType() = Factorys.getFactoryFromClass(type).create(KType(type))

            val getter = Reflection.checkFactoryGetter(type)
            if (getter != null) {
                getter.isAccessible = true
                if (0 != getter.parameterCount)
                    return Factorys.getFactoryFromClass(type).create(KType(type))
                val factory: TypeParserFactory =
                    getter(if (Reflection.isStatic(getter)) null else Allocators.unsafeAny(true)
                        .allocateInstance(type as Class<Any>)) as TypeParserFactory?
                        ?: return getFromType()
                Factorys.register(type, factory)
                return factory.create(KType(type))
            }
            return Factorys.getFactoryFromClass(type).create(KType(type))
        } else if (type is GenericArrayType) {
            // TODO: may have bugs.
            val cmpType = type.genericComponentType
            if (cmpType is ParameterizedType) {
                val tp = KType<Any>(type)
                return Factorys.getFactoryFromClass(tp.rawType()).create(tp)
            }
        }
        throw IllegalStateException("Can not get the parser of $type")
    }

    @JvmStatic
    private fun jsonPrimitiveParser(clz: Class<*>): TypeParser<Any> = UtilParsers.getPrimitiveParser(clz)

    internal fun String.transEscapes(): String {
        if (isEmpty())
            return ""
        val chars: CharArray = toCharArray()
        val length = chars.size
        var from = 0
        var to = 0
        while (from < length) {
            var ch = chars[from++]
            if (ch == '\\') {
                ch = if (from < length) chars[from++] else '\u0000'
                when (ch) {
                    'b' -> ch = '\b'
                    'f' -> ch = 12.toChar()
                    'n' -> ch = '\n'
                    'r' -> ch = '\r'
                    's' -> ch = ' '
                    't' -> ch = '\t'
                    '\'', '\"', '\\' -> {
                    }
                    '0', '1', '2', '3', '4', '5', '6', '7' -> {
                        val limit = Integer.min(from + if (ch <= '3') 2 else 1, length)
                        var code = ch - '0'
                        while (from < limit) {
                            ch = chars[from]
                            if (ch < '0' || '7' < ch) {
                                break
                            }
                            from++
                            code = code shl 3 or ch - '0'
                        }
                        ch = code.toChar()
                    }
                    '\n' -> continue
                    '\r' -> {
                        if (from < length && chars[from] == '\n') {
                            from++
                        }
                        continue
                    }
                    else -> {
                        val msg = String.format(
                            "Invalid escape sequence: \\%c \\\\u%04X",
                            ch, ch.toInt())
                        throw IllegalArgumentException(msg)
                    }
                }
            }
            chars[to++] = ch
        }
        return String(chars, 0, to)
    }
}