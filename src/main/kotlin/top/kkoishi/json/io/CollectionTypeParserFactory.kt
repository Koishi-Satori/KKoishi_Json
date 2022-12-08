package top.kkoishi.json.io

import top.kkoishi.json.internal.reflect.Reflection
import top.kkoishi.json.reflect.Type
import top.kkoishi.json.reflect.TypeResolver
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type as JType

class CollectionTypeParserFactory private constructor() {
    private val instances = HashMap<ParameterizedType, CollectionTypeParser<*>>()

    internal companion object {
        @JvmStatic
        internal val ` inst` = CollectionTypeParserFactory()
    }

    @JvmOverloads
    @Suppress("UNCHECKED_CAST")
    fun <T> create(
        tType: JType,
        rawType: JType = Collection::class.java,
        ownerType: JType? = null,
    ): CollectionTypeParser<T> where T : Any {
        val key = Reflection.ParameterizedTypeImpl(ownerType, rawType, tType)
        var inst: CollectionTypeParser<T>? = instances[key] as CollectionTypeParser<T>?
        if (inst == null) {
            inst = CollectionTypeParser.` getInstance`(Type(Collection::class.java), tType)
            instances[key] = inst
        }
        return inst
    }

    fun <T> create(typeResolver: TypeResolver<out Collection<T>>): CollectionTypeParser<T> where T : Any {
        val parameterizedType = typeResolver.resolve() as ParameterizedType
        val types = parameterizedType.actualTypeArguments
        return create(types[0], parameterizedType.rawType, parameterizedType.ownerType)
    }
}