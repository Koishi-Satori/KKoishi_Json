package top.kkoishi.json.io

import top.kkoishi.json.reflect.Type
import top.kkoishi.json.reflect.TypeResolver
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type as JType

class MapTypeParserFactory private constructor() {
    internal companion object {
        internal val ` inst` = MapTypeParserFactory()
    }

    fun <K, V> create(kType: JType, vType: JType): MapTypeParser<K, V> where K : Any, V : Any =
        MapTypeParser.` getInstance`(Type(MutableMap::class.java), kType, vType)

    fun <K, V> create(typeResolver: TypeResolver<in MutableMap<K, V>>): MapTypeParser<K, V> where K : Any, V : Any {
        val types = (typeResolver.resolve() as ParameterizedType).actualTypeArguments
        return MapTypeParser.` getInstance`(Type(MutableMap::class.java), types[0], types[1])
    }
}