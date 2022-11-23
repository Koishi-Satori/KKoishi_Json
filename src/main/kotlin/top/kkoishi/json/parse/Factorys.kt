package top.kkoishi.json.parse

import top.kkoishi.json.internal.reflect.Reflection
import top.kkoishi.json.io.ArrayTypeParserFactory
import top.kkoishi.json.io.FieldTypeParserFactory
import top.kkoishi.json.io.MapTypeParserFactory
import top.kkoishi.json.io.TypeParserFactory

object Factorys {
    @JvmStatic
    private val spec: MutableMap<Class<*>, TypeParserFactory> = mutableMapOf()

    @JvmName(" addSpec")
    @JvmStatic
    internal fun addSpec(clz: Class<*>, factory: TypeParserFactory) {
        spec.put(clz, factory)
    }

    @JvmStatic
    fun getFieldTypeFactory(): TypeParserFactory = FieldTypeParserFactory.` instance`

    @JvmStatic
    fun getArrayTypeFactory(): ArrayTypeParserFactory = ArrayTypeParserFactory.` inst`

    @JvmStatic
    fun getMapTypeFactory(): MapTypeParserFactory = MapTypeParserFactory.` inst`

    @JvmStatic
    fun getFactoryFromType(type: Class<*>): TypeParserFactory {
        if (spec.containsKey(type))
            return spec[type]!!
        else if (type.isArray)
            return getArrayTypeFactory()
        else if (Reflection.isMapType(type))
            throw IllegalArgumentException()
        else
            return getFieldTypeFactory()
    }
}