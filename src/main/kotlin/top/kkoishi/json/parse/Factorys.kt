package top.kkoishi.json.parse

import top.kkoishi.json.io.FieldTypeParserFactory
import top.kkoishi.json.io.TypeParserFactory

object Factorys {
    private val spec: MutableMap<Class<*>, TypeParserFactory> = mutableMapOf()

    @JvmStatic
    fun getFieldTypeFactory(): TypeParserFactory = FieldTypeParserFactory.instance

    @JvmStatic
    fun getFactoryFromType(type: Class<*>): TypeParserFactory {
        if (spec.containsKey(type))
            return spec[type]!!
        return getFieldTypeFactory()
    }
}