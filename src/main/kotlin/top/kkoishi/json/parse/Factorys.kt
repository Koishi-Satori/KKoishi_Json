package top.kkoishi.json.parse

import top.kkoishi.json.io.ArrayTypeParserFactory
import top.kkoishi.json.io.FieldTypeParserFactory
import top.kkoishi.json.io.TypeParserFactory

object Factorys {
    private val spec: MutableMap<Class<*>, TypeParserFactory> = mutableMapOf()

    @JvmName(" addSpec")
    internal fun addSpec(clz: Class<*>, factory: TypeParserFactory) {
        spec.put(clz, factory)
    }

    @JvmStatic
    fun getFieldTypeFactory(): TypeParserFactory = FieldTypeParserFactory.` instance`

    @JvmStatic
    fun getArrayTypeFactory(): ArrayTypeParserFactory = ArrayTypeParserFactory.` inst`

    @JvmStatic
    fun getFactoryFromType(type: Class<*>): TypeParserFactory {
        if (spec.containsKey(type))
            return spec[type]!!
        return getFieldTypeFactory()
    }
}