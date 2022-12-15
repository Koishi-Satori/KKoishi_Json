package top.kkoishi.json

import top.kkoishi.json.annotation.DeserializationIgnored
import top.kkoishi.json.annotation.FieldJsonName
import top.kkoishi.json.annotation.SerializationIgnored
import top.kkoishi.json.internal.InternalParserFactory
import top.kkoishi.json.internal.Utils.KKoishiJsonInit
import top.kkoishi.json.internal.io.UtilFactorys
import top.kkoishi.json.internal.io.UtilParsers
import top.kkoishi.json.internal.reflect.Allocators
import top.kkoishi.json.internal.reflect.Reflection
import top.kkoishi.json.io.*
import top.kkoishi.json.parse.Factorys
import top.kkoishi.json.parse.NumberMode
import top.kkoishi.json.parse.Platform
import top.kkoishi.json.reflect.Type
import top.kkoishi.json.reflect.TypeResolver
import java.io.Reader
import java.io.Writer
import java.lang.reflect.Field
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.util.*
import java.lang.reflect.Type as JType

class KKoishiJson {
    val dateStyle: Int
    val timeStyle: Int
    val locale: Locale
    val useUnsafe: Boolean

    private lateinit var stored: MutableMap<JType, TypeParserFactory>
    private lateinit var fieldParserFactory: InternalFieldParserFactory

    @Suppress("RemoveRedundantSpreadOperator")
    constructor(): this(*arrayOf())

    constructor(vararg initFactories: Pair<JType, TypeParserFactory>) : this(DEFAULT_DATE_STYLE,
        DEFAULT_TIME_STYLE,
        DEFAULT_LOCALE,
        DEFAULT_USE_UNSAFE,
        *initFactories)

    constructor(
        dateStyle: Int,
        timeStyle: Int,
        locale: Locale,
        useUnsafe: Boolean,
        vararg initFactories: Pair<JType, TypeParserFactory>,
    ) {
        this.dateStyle = dateStyle
        this.timeStyle = timeStyle
        this.locale = locale
        this.useUnsafe = useUnsafe

        for ((tp, factory) in initFactories) {
            stored[tp] = factory
        }

        fieldParserFactory = InternalFieldParserFactory(this)
        stored = KKoishiJsonInit(this)
    }

    private companion object {
        private const val DEFAULT_DATE_STYLE = 2
        private const val DEFAULT_TIME_STYLE = 2

        @JvmStatic
        private val DEFAULT_LOCALE = Locale.getDefault()
        private const val DEFAULT_USE_UNSAFE = true

        private class InternalFieldParserFactory(override val instance: KKoishiJson) : TypeParserFactory,
            InternalParserFactory.Conditional {
            override fun <T : Any> create(type: Type<T>): TypeParser<T> = InternalFieldTypeParser<T>(type, instance)
        }

        private open class InternalFieldTypeParser<T : Any>(type: Type<T>, override val instance: KKoishiJson) :
            FieldTypeParser<T>(type), InternalParserFactory.Conditional {
            override fun deserializeField(field: Field, o: JsonObject): FieldData {
                val annotation = field.getDeclaredAnnotation(FieldJsonName::class.java)
                if (annotation != null) {
                    if (o.contains(annotation.name))
                        return FieldData(annotation.name, field)
                    else {
                        for (name in annotation.alternate) {
                            if (o.contains(name))
                                return FieldData(name, field)
                        }
                    }
                }
                return FieldData(field)
            }

            override fun serializeField(field: Field): FieldData {
                val annotation = field.getDeclaredAnnotation(FieldJsonName::class.java)
                if (annotation != null)
                    return FieldData(annotation.name, field)
                return FieldData(field)
            }

            private fun checkGetter(getterName: String): Method? {
                return try {
                    type.rawType().getDeclaredMethod(getterName)
                } catch (e: NoSuchMethodException) {
                    null
                }
            }

            override fun defaultValue(field: Field, inst: Any, deserialization: Boolean): Any? {
                val getterName: String = if (deserialization)
                    field.getAnnotation(DeserializationIgnored::class.java)?.defaultValueGetter ?: ""
                else
                    field.getAnnotation(SerializationIgnored::class.java)?.defaultValueGetter ?: ""
                if (getterName.isNotEmpty()) {
                    val getter = checkGetter(getterName)
                    if (getter != null)
                        return getter.invoke(if (Reflection.isStatic(getter)) null else inst)
                }
                val clz = field.type
                if (clz.isPrimitive)
                    return primitiveDefaultValue(clz)
                return null
            }

            @Suppress("UNCHECKED_CAST")
            override fun newInstance(clz: Class<*>): Any {
                val allocator = Allocators.unsafeAny(instance.useUnsafe)
                return allocator.allocateInstance(clz as Class<Any>)
            }
        }
    }

    fun <T> fromJson(typeResolver: TypeResolver<T>, json: JsonElement): T? where T : Any {
        if (json.isJsonNull())
            return null
        val factory = getFactory(typeResolver)
        if (factory != null) {
            val tp = Type<T>(typeResolver.resolve())
            return factory.create(tp).fromJson(json)
        }
        TODO()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> fromJson(typeofT: JType, json: JsonElement): T? {
        if (json.isJsonNull())
            return null
        val parser = getParser(typeofT)
        if (parser == null) {
            if (typeofT == String::class.java && json.isJsonPrimitive()) {
                val primitive = json.toJsonPrimitive()
                if (primitive.isJsonString())
                    return primitive.getAsString() as T?
            }
            throw IllegalArgumentException()
        }
        return parser.fromJson(json) as T?
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> toJson(typeofT: JType, instance: T?): JsonElement {
        if (instance == null)
            return JsonNull()
        val parser = getParser(typeofT) as TypeParser<in T>?
        if (parser == null) {
            if (typeofT == String::class.java && instance is String) {
                return JsonString(instance)
            }
            throw IllegalArgumentException()
        }
        return parser.toJson(instance)
    }

    @JvmOverloads
    fun reader(
        reader: Reader,
        platform: Platform = Platform.LINUX,
        mode: NumberMode = NumberMode.ALL_TYPE,
    ): JsonReader = JsonReader(reader, platform, mode)

    @JvmOverloads
    fun writer(writer: Writer, lineSeparator: String = "\n"): JsonWriter {
        return BasicJsonWriter(writer, lineSeparator)
    }

    private fun getFieldTypeFactory(): TypeParserFactory = fieldParserFactory

    @Suppress("UNCHECKED_CAST")
    private fun getParser(type: JType): TypeParser<*>? {
        if (type is ParameterizedType) {
            val parameters = type.actualTypeArguments
            val raw = Reflection.getRawType(type.rawType)
            if (parameters.size == 2 && raw == MutableMap::class.java)
                return Factorys.getMapTypeFactory().create<Any, Any>(parameters[0], parameters[1])
            if (parameters.size == 1 && raw == Collection::class.java)
                return Factorys.getCollectionTypeFactory().create<Any>(parameters[0])

            if (type != Any::class.java)
                return getParser(raw)
        } else if (type is Class<*>) {
            if (Reflection.checkJsonPrimitive(type))
                return UtilParsers.getPrimitiveParser(type)
            fun getFromType() = getFactoryFromClass(type).create(Type(type))

            val getter = Reflection.checkFactoryGetter(type)
            if (getter != null) {
                getter.isAccessible = true
                if (0 != getter.parameterCount)
                    return getFromType()
                val factory: TypeParserFactory =
                    getter(if (Reflection.isStatic(getter)) null else Allocators.unsafeAny(useUnsafe)
                        .allocateInstance(type as Class<Any>)) as TypeParserFactory?
                        ?: return getFromType()
                stored[type] = factory
                return factory.create(Type(type))
            }
            return getFactoryFromClass(type).create(Type(type))
        } else if (type is GenericArrayType) {
            // TODO: may have bugs.
            val cmpType = type.genericComponentType
            if (cmpType is ParameterizedType) {
                val tp = Type<Any>(type)
                return getFactoryFromClass(tp.rawType()).create(tp)
            }
        }
        return null
    }

    private fun getFactoryFromClass(type: Class<*>): TypeParserFactory {
        if (stored.containsKey(type))
            return stored[type]!!
        return getIfNotContainsFromClass(type)
    }

    private fun getIfNotContainsFromClass(type: Class<*>): TypeParserFactory {
        if (type.isArray)
            return Factorys.getArrayTypeFactory()
        else if (Reflection.isMapType(type) || Reflection.isCollection(type))
            throw IllegalArgumentException()
        else if (Reflection.checkJsonPrimitive(type))
            return UtilFactorys.PRIMITIVE
        else
            return getFieldTypeFactory()
    }

    private fun <TYPE> getFactory(typeResolver: TypeResolver<TYPE>): TypeParserFactory? {
        val parameterizedType = typeResolver.resolve() as ParameterizedType
        val arguments = parameterizedType.actualTypeArguments
        val key = Reflection.ParameterizedTypeImpl(parameterizedType.ownerType, parameterizedType.rawType, *arguments)
        if (stored.containsKey(key)) {
            return stored[key]!!
        }
        val rawType = Reflection.getRawType(key.rawType)
        if ((arguments.size == 2 && rawType == MutableMap::class.java) ||
            (arguments.size == 1 && rawType == Collection::class.java)
        )
            return null
        return getIfNotContainsFromClass(rawType)
    }
}