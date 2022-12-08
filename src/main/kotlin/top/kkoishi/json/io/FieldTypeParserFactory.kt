package top.kkoishi.json.io

import top.kkoishi.json.JsonObject
import top.kkoishi.json.annotation.DeserializationIgnored
import top.kkoishi.json.annotation.FieldJsonName
import top.kkoishi.json.annotation.SerializationIgnored
import top.kkoishi.json.internal.reflect.Reflection
import top.kkoishi.json.reflect.Type
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class FieldTypeParserFactory private constructor() : TypeParserFactory {
    internal val ` defaults`: MutableMap<Type<*>, FieldTypeParser<*>> = mutableMapOf()
    internal val ` safety`: MutableMap<Type<*>, FieldTypeParser<*>> = mutableMapOf()

    internal companion object {
        @JvmStatic
        internal val ` instance` = FieldTypeParserFactory()

        open class ` DefaultFieldTypeParser`<T : Any>(type: Type<T>) : FieldTypeParser<T>(type) {
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
                } catch (e: NoSuchMethodException) { null }
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
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> create(type: Type<T>): FieldTypeParser<T> {
        var inst = ` defaults`[type]
        if (inst != null)
            return inst as FieldTypeParser<T>
        inst = ` DefaultFieldTypeParser`(type)
        ` defaults`[type] = inst
        return inst
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> createSafety(type: Type<T>, tryUnsafe: Boolean = true): FieldTypeParser<T> {
        var inst = ` safety`[type]
        if (inst != null)
            return inst as FieldTypeParser<T>
        inst = create(type).safe(tryUnsafe)
        ` safety`[type] = inst
        return inst
    }

    fun <T: Any> createByAllocator(type: Type<T>, allocator: ((Class<T>) -> T), replace: Boolean = true): FieldTypeParser<T> {
        val inst = object : ` DefaultFieldTypeParser`<T>(type) {
            @Suppress("UNCHECKED_CAST")
            override fun newInstance(clz: Class<*>): Any = allocator(type.rawType() as Class<T>)
        }
        if (replace)
            ` safety`[type] = inst
        return inst
    }

    override fun fieldParser(): FieldTypeParserFactory = this
}