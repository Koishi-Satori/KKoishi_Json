package top.kkoishi.json.io

import top.kkoishi.json.Utils
import top.kkoishi.json.reflect.Type

interface TypeParserFactory {
    fun <T> create(type: Type<T>): TypeParser<T>

    fun fieldParser(): FieldTypeParserFactory = Utils.uoe()
}