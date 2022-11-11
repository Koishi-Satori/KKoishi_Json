@file:Suppress("MemberVisibilityCanBePrivate")

package top.kkoishi.json

import java.lang.System.arraycopy as arrcpy

class JsonArray : JsonElement, Iterable<JsonElement> {
    internal companion object {
        private class Arr(vararg initElements: JsonElement) : Iterable<JsonElement>, MutableList<JsonElement> {
            private class ArrIterator @JvmOverloads constructor(val inst: Arr, var pos: Int = 0) :
                MutableListIterator<JsonElement> {
                private var right = inst.elements.size - 1

                /**
                 * The flag is used to show the last accessed element is accessed by
                 * *next()* or *previous()*.
                 *
                 * If *next()* method is invoked, this will be 0x00, or 0x01.
                 */
                private var flagSet: Byte = 0x00

                override fun hasPrevious(): Boolean = pos > 1

                override fun nextIndex() = pos + 1

                override fun previous(): JsonElement {
                    synchronized(lock = inst) {
                        flagSet = 0x01
                        return inst.elements[pos--]
                    }
                }

                override fun previousIndex() = pos - 1

                @Suppress("NOTHING_TO_INLINE")
                private inline fun applyChange() {
                    right = inst.elements.size - 1
                }

                override fun add(element: JsonElement) {
                    synchronized(lock = inst) {
                        inst.add(element)
                        applyChange()
                    }
                }

                override fun hasNext(): Boolean = pos < right

                override fun next(): JsonElement {
                    synchronized(lock = inst) {
                        flagSet = 0x00
                        return inst.elements[pos++]
                    }
                }

                override fun remove() {
                    synchronized(lock = inst) {
                        inst.removeLast()
                        applyChange()
                    }
                }

                override fun set(element: JsonElement) {
                    synchronized(lock = inst) {
                        if (flagSet.toInt() == 0x00)
                            inst.elements[pos - 1] = element
                        else
                            inst.elements[pos + 1] = element
                    }
                }
            }

            @Suppress("UNCHECKED_CAST")
            var elements: Array<JsonElement> = initElements as Array<JsonElement>
            override val size: Int
                get() = elements.size

            override fun iterator(): MutableIterator<JsonElement> = ArrIterator(this)

            override fun contains(element: JsonElement) = this[element] != -1

            override fun containsAll(elements: Collection<JsonElement>): Boolean {
                for (e in elements) if (!contains(e)) return false
                return true
            }

            override operator fun get(index: Int): JsonElement = elements[index]

            @JvmOverloads
            operator fun get(element: JsonElement, reserved: Boolean = false): Int {
                val range: IntRange = if (reserved) (elements.size - 1..0) else (elements.indices)
                for (index in range) {
                    if (elements[index] == element) {
                        return index
                    }
                }
                return -1
            }

            override fun indexOf(element: JsonElement): Int = this[element]

            override fun isEmpty(): Boolean = elements.isEmpty()

            override fun lastIndexOf(element: JsonElement) = this[element, true]

            private fun resize(dv: Int, desize: Boolean = false): Arr {
                assert(dv > 0) { throw IllegalArgumentException() }
                val old = elements
                val oldSize = old.size
                elements = Array(oldSize + dv * (if (desize) -1 else 1)) { if (it < oldSize) old[it] else JsonNull() }
                return this
            }

            private fun add0(element: JsonElement): Boolean {
                elements[elements.size - 1] = element
                return true
            }

            override fun add(element: JsonElement): Boolean = resize(1).add0(element)

            override fun add(index: Int, element: JsonElement): Unit = Utils.uoe("top.kkoishi.json.JsonArray")

            override fun addAll(index: Int, elements: Collection<JsonElement>): Boolean =
                Utils.uoe("top.kkoishi.json.JsonArray")

            override fun addAll(elements: Collection<JsonElement>): Boolean {
                for (e in elements) add(e)
                return true
            }

            override fun clear() {
                elements = Array(0) { JsonNull() }
            }

            override fun listIterator(): MutableListIterator<JsonElement> = ArrIterator(this)

            override fun listIterator(index: Int): MutableListIterator<JsonElement> = ArrIterator(this, index)

            override fun remove(element: JsonElement): Boolean {
                val index = indexOf(element)
                if (index == -1) return false
                removeAt(index)
                return true
            }

            override fun removeAll(elements: Collection<JsonElement>): Boolean {
                var res = false
                for (e in elements) if (remove(e)) res = true
                return res
            }

            override fun removeAt(index: Int): JsonElement {
                val e = elements[index]
                if (index != elements.size - 1)
                    arrcpy(elements, index + 1, elements, index, elements.size - index - 1)
                resize(1, true)
                return e
            }

            override fun retainAll(elements: Collection<JsonElement>): Boolean {
                val cpy: Array<JsonElement> = Array(this.elements.size) { this.elements[it] }
                var res = false
                for (e in cpy)
                    if (!elements.contains(e)) {
                        res = true
                        remove(e)
                    }
                return res
            }

            override fun set(index: Int, element: JsonElement): JsonElement {
                val oldValue = elements[index]
                elements[index] = element
                return oldValue
            }

            override fun subList(fromIndex: Int, toIndex: Int): MutableList<JsonElement> = Arr(*elements.sliceArray(fromIndex..toIndex))

            override fun toString(): String = elements.contentToString()
        }
    }

    constructor(elements: Collection<JsonElement>) : super(ARRAY) {
        array = Arr(*elements.toTypedArray())
    }

    constructor(): super(ARRAY) {
        array = Arr()
    }

    private val array: Arr

    fun size() = array.size

    override fun isJsonArray() = true

    override fun toJsonArray() = this

    override fun iterator(): Iterator<JsonElement> = array.iterator()

    operator fun get(index: Int): JsonElement = array[index]

    operator fun set(index: Int, element: JsonElement) = array.set(index, element)

    fun add(element: JsonElement) = array.add(element)

    fun add(index: Int, element: JsonElement) = array.add(index, element)

    fun isEmpty() = array.isEmpty()

    fun isNotEmpty() = array.isNotEmpty()

    fun reverse() = reverse(0, array.size - 1)

    fun reverse(fromIndex: Int, toIndex: Int) = array.elements.reverse(fromIndex, toIndex)

    fun reversed(): JsonArray = reversed(0, array.size - 1)

    fun reversed(fromIndex: Int, toIndex: Int): JsonArray {
        reverse(fromIndex, toIndex)
        return this
    }

    fun shuffle() = array.elements.shuffle()

    fun shuffled(): JsonArray {
        shuffle()
        return this
    }

    fun toList(): List<JsonElement> = array.toList()

    fun toCollection(): Collection<JsonElement> = array.toList()

    fun toArray(): Array<JsonElement> = Array(array.size) { array[it] }

    fun slice(indices: IntRange) = array.slice(indices)

    fun slice(indices: Iterable<Int>) = array.slice(indices)
    override fun toString(): String {
        return "JsonArray{$array}"
    }
}