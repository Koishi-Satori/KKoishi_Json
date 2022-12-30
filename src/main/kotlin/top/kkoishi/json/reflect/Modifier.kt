package top.kkoishi.json.reflect

import top.kkoishi.json.internal.reflect.Reflection
import top.kkoishi.json.internal.reflect.Reflection.ACC_ENUM
import top.kkoishi.json.internal.reflect.Reflection.ACC_FINAL
import top.kkoishi.json.internal.reflect.Reflection.ACC_PRIVATE
import top.kkoishi.json.internal.reflect.Reflection.ACC_PROTECTED
import top.kkoishi.json.internal.reflect.Reflection.ACC_PUBLIC
import top.kkoishi.json.internal.reflect.Reflection.ACC_STATIC
import top.kkoishi.json.internal.reflect.Reflection.ACC_SYNTHETIC
import top.kkoishi.json.internal.reflect.Reflection.ACC_TRANSIENT
import top.kkoishi.json.internal.reflect.Reflection.ACC_VOLATILE

enum class Modifier(val value: Int) {
    PUBLIC(ACC_PUBLIC),
    PRIVATE(ACC_PRIVATE),
    PROTECTED(ACC_PROTECTED),
    STATIC(ACC_STATIC),
    FINAL(ACC_FINAL),
    VOLATILE(ACC_VOLATILE),
    TRANSIENT(ACC_TRANSIENT),
    SYNTHETIC(ACC_SYNTHETIC),
    ENUM(ACC_ENUM);

    operator fun MutableList<Modifier>.plus(modifier: Modifier): MutableList<Modifier> {
        add(modifier)
        return this
    }

    operator fun invoke(modifier: Int): Boolean {
        return value <= modifier
    }

    companion object {
        @JvmStatic
        fun List<Modifier>.modifier(): Int {
            var modifier = 0x0000
            this.stream().distinct().forEach { modifier += it.value }
            return modifier
        }

        @JvmStatic
        fun Int.modifier(): MutableList<Modifier> {
            val modifiers = ArrayDeque<Modifier>(3)
            var copy = this
            for ((_value, modifier) in Reflection.FIELD_MODIFIERS) {
                if (copy >= _value) {
                    modifiers.addLast(modifier)
                    copy -= _value
                }
                else break
            }
            return modifiers
        }
    }

    override fun toString(): String {
        return name
    }
}