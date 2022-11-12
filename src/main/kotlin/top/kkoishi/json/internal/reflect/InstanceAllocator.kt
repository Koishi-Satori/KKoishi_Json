package top.kkoishi.json.internal.reflect

import top.kkoishi.json.reflect.Type

internal interface InstanceAllocator<T> {
    fun allocateInstance(typeofT: Type<T>): T

    fun allocateInstance(clz: Class<T>): T
}