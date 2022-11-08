package top.kkoishi.json.annotation

@MustBeDocumented
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER)
annotation class FieldJsonName(
    /**
     * @return the desired name of the field when it is serialized or deserialized
     */
    val name: String,
    /**
     * @return the alternative names of the field when it is deserialized
     */
    val alternate: Array<String> = [],
)
