package top.kkoishi.json.io

import top.kkoishi.json.reflect.Type

class ArrayTypeParserFactory private constructor(): TypeParserFactory {
    internal val ` defaults`: MutableMap<Type<*>, ArrayTypeParser<*>> = mutableMapOf()

    internal companion object ` Utils` {
        @JvmStatic
        val ` inst`: ArrayTypeParserFactory = ArrayTypeParserFactory()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> create(type: Type<T>): ArrayTypeParser<T> {
        var inst: ArrayTypeParser<T>? = ` defaults`[type] as ArrayTypeParser<T>?
        if (inst == null) {
            inst = ArrayTypeParser.` getInstance`(type)
            ` defaults`[type] = inst
        }
        return inst
    }
}