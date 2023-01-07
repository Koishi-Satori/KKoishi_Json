package top.kkoishi.json.internal

import top.kkoishi.json.JsonElement
import top.kkoishi.json.JsonObject
import top.kkoishi.json.JsonString
import top.kkoishi.json.Kson
import top.kkoishi.json.internal.io.ParserManager
import top.kkoishi.json.internal.io.UtilParsers
import top.kkoishi.json.internal.reflect.Allocators
import top.kkoishi.json.io.TypeParser
import top.kkoishi.json.io.TypeParserFactory
import top.kkoishi.json.reflect.Type
import java.util.*
import java.lang.reflect.Type as JType

internal object InternalParserFactory {
    /**
     * Some internal parser/factory might use some properties in Kson, so they need to implement
     * this interface to get Kson instance.
     */
    internal interface Conditional {
        val instance: Kson
    }

    /**
     * This interface is used to avoid repeated initialization when construct KsonBuilder
     * instance using Kson instance.
     *
     * @see top.kkoishi.json.KsonBuilder
     */
    internal interface InitFactory

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    internal fun getFactory(require: JType, inst: TypeParser<*>): Pair<JType, TypeParserFactory> {
        // get anonymous class implements InitFactory and extends TypeParserFactory.
        return (require to object : TypeParserFactory, InitFactory {
            override fun <T : Any> create(type: Type<T>): TypeParser<T> = inst as TypeParser<T>
        })
    }

    class DateParser(override val instance: Kson) : UtilParsers.DateTypeParser(), Conditional {
        override fun fromJson(json: JsonElement): Date =
            super.fromJson(json, instance.dateStyle, instance.timeStyle, instance.locale)

        override fun toJson(t: Date): JsonElement =
            super.toJson(t, instance.dateStyle, instance.timeStyle, instance.locale)
    }

    @Suppress("UNCHECKED_CAST")
    internal class DictionaryTypeParser<K, V, D>(
        type: Type<D>,
        private val kType: JType,
        private val vType: JType,
        override val instance: Kson,
    ) :
        TypeParser<D>(type), Conditional where D : Dictionary<K, V> {
        override fun fromJson(json: JsonElement): D {
            if (!json.isJsonObject())
                throw IllegalArgumentException()
            val obj = json.toJsonObject()
            val dictionary: D = allocateInstance()
            val kParser: TypeParser<K> = getParser(kType) as TypeParser<K>
            val vParser: TypeParser<V> = getParser(vType) as TypeParser<V>
            for ((k, v) in obj) {
                dictionary.put(kParser.fromJson(JsonString(k)), vParser.fromJson(v))
            }
            return dictionary
        }

        override fun toJson(t: D): JsonElement {
            val keyParser: TypeParser<K> = getParser(kType) as TypeParser<K>
            val valueParser: TypeParser<V> = getParser(vType) as TypeParser<V>
            val json = JsonObject()
            for (k in t.keys()) {
                json[keyParser.toJson(k).toString()] = valueParser.toJson(t[k])
            }
            return json
        }

        private fun allocateInstance(): D {
            val raw = type.rawType() as Class<D>
            return try {
                val constructor = raw.getDeclaredConstructor()
                constructor.isAccessible = true
                constructor.newInstance()
            } catch (e: Exception) {
                Allocators.unsafe<D>(instance.useUnsafe).allocateInstance(raw)
            }
        }

        private fun getParser(type: JType): TypeParser<*> = ParserManager.getParser(type)
    }
}