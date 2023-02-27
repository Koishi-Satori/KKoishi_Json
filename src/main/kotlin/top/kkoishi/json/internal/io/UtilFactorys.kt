package top.kkoishi.json.internal.io

import top.kkoishi.json.internal.reflect.Reflection.isType
import top.kkoishi.json.io.TypeParser
import top.kkoishi.json.io.TypeParserFactory
import top.kkoishi.json.reflect.Type
import java.io.File
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.lang.reflect.Type as JType
import java.util.*

internal object UtilFactorys {
    internal fun init(stored: MutableMap<JType, TypeParserFactory>) {
        stored[Date::class.java] = getFactory(Date::class.java, UtilParsers.DATE)
        stored[UUID::class.java] = getFactory(UUID::class.java, UtilParsers.UUID)
        stored[Calendar::class.java] = getFactory(Calendar::class.java, UtilParsers.CALENDER)
        stored[Random::class.java] = getFactory(Random::class.java, UtilParsers.RANDOM)
        stored[File::class.java] = getFactory(File::class.java, UtilParsers.FILE)
        stored[Path::class.java] = getFactory(Path::class.java, UtilParsers.PATH)
        stored[URL::class.java] = getFactory(URL::class.java, UtilParsers.URL)
        stored[URI::class.java] = getFactory(URI::class.java, UtilParsers.URI)
        stored[InetAddress::class.java] = getFactory(InetAddress::class.java, UtilParsers.INET_ADDRESS)
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