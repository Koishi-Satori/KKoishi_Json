package top.kkoishi.json.io

import top.kkoishi.json.internal.reflect.Reflection
import top.kkoishi.json.reflect.Type
import top.kkoishi.json.reflect.TypeResolver
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type as JType

class MapTypeParserFactory private constructor() {
    private val instances = HashMap<ParameterizedType, MapTypeParser<*, *>>()

    internal companion object {
        @JvmStatic
        internal val ` inst` = MapTypeParserFactory()
    }

    @JvmOverloads
    @Suppress("UNCHECKED_CAST")
    fun <K, V> create(
        kType: JType,
        vType: JType,
        rawType: JType = MutableMap::class.java,
        ownerType: JType? = null,
    ): MapTypeParser<K, V> where K : Any, V : Any {
        val key = Reflection.ParameterizedTypeImpl(null, rawType, kType, vType)
        var inst: MapTypeParser<K, V>? = instances[key] as MapTypeParser<K, V>?
        if (inst == null) {
            inst = MapTypeParser.` getInstance`(Type(MutableMap::class.java), kType, vType)
            instances[key] = inst
        }
        return inst
    }

    fun <K, V> create(typeResolver: TypeResolver<out MutableMap<K, V>>): MapTypeParser<K, V> where K : Any, V : Any {
        val parameterizedType = typeResolver.resolve() as ParameterizedType
        val types = parameterizedType.actualTypeArguments
        return create(types[0], types[1], parameterizedType.rawType, parameterizedType.ownerType)
    }
}