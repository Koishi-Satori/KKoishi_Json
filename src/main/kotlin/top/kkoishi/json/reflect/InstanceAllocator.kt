package top.kkoishi.json.reflect

internal interface InstanceAllocator<T> {
    fun allocateInstance(typeofT: Type<T>): T

    fun allocateInstance(clz: Class<T>): T
}