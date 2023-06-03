package top.kkoishi.json

import top.kkoishi.json.annotation.DeserializationIgnored
import top.kkoishi.json.annotation.FieldJsonName
import top.kkoishi.json.annotation.SerializationIgnored
import top.kkoishi.json.internal.reflect.Reflection
import top.kkoishi.json.io.FieldTypeParser
import top.kkoishi.json.io.FieldTypeParserFactory
import top.kkoishi.json.reflect.Type
import java.lang.reflect.Field
import java.lang.reflect.Method

abstract class AbstractFieldTypeParser<T : Any>(type: Type<T>) : FieldTypeParser<T>(type) {

    init {
        FieldTypeParserFactory.` instance`.` defaults`[type] = this
    }

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

    protected open fun checkGetter(getterName: String): Method? {
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

    abstract override fun newInstance(clz: Class<*>): Any
}