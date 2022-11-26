package top.kkoishi.json.io

import java.lang.reflect.Type

class CollectionTypeParserFactory private constructor () {
    internal companion object {
        internal val ` inst` = CollectionTypeParserFactory()
    }

    fun <T> create(tType: Type): CollectionTypeParser<T> {
        TODO()
    }
}