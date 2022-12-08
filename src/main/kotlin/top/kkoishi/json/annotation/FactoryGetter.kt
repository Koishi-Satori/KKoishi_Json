package top.kkoishi.json.annotation

@MustBeDocumented
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class FactoryGetter(val getterDescriptor: String)
