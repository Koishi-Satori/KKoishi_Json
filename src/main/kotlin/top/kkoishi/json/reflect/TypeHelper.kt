package top.kkoishi.json.reflect

import top.kkoishi.json.io.TypeParserFactory
import kotlin.reflect.KClass

object TypeHelper {
    interface TypeToken<T> {
        fun type(): java.lang.reflect.Type
        fun rawType(): Class<in T>
    }

    interface TypeParserFactoryGetter {
        fun getFactory(): TypeParserFactory
    }

    @JvmStatic
    fun <T : Any> KClass<T>.asType(): Type<T> {
        return Type(this.java)
    }

    @JvmStatic
    fun <T: Any> Class<T>.asType(): Type<T> {
        return Type(this)
    }
}