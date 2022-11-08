package top.kkoishi.json.io

import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.bufferedReader

internal interface CharBuffer : Iterable<Char> {
    companion object {
        @JvmStatic
        @JvmOverloads
        @Deprecated("Not finished")
        fun getInstance(ins: InputStream, charset: Charset = Charsets.UTF_8): CharBuffer {
            return object : CharBuffer {
                private val buffer = ins.bufferedReader(charset)
                private var nval: Int = 0


                private fun check() {
                    nval = buffer.read()
                }

                override fun read(): Char {

                    val nxt = buffer.read()
                    return if (nxt >= 0)
                        Char(nxt)
                    else
                        throw IOException()
                }

                override fun hasNext(): Boolean {
                    check()
                    return false
                }
            }
        }

        @JvmStatic
        @JvmOverloads
        fun getInstance(filePath: String, charset: Charset = Charsets.UTF_8): CharBuffer {
            return object : CharBuffer {
                private val buf =
                    Path.of(filePath).bufferedReader(charset, DEFAULT_BUFFER_SIZE, StandardOpenOption.READ)
                private var nval: Int = -1
                private var flag = true

                override fun read(): Char {
                    if (flag && nval < 0) {

                    }
                    TODO("Not yet implemented")
                }

                override fun hasNext(): Boolean {
                    TODO("Not yet implemented")
                }
            }
        }
    }

    fun read(): Char

    fun hasNext(): Boolean

    fun read(tar: CharArray, startIndex: Int = 0, len: Int = tar.size): Int {
        var count = 0
        try {
            for (index in startIndex until startIndex + len)
                if (hasNext()) {
                    tar[index] = read()
                    ++count
                } else
                    return count
        } catch (e: Exception) {
            return -1
        }
        return count
    }

    override fun iterator(): CharIterator = object : CharIterator() {
        override fun hasNext() = this@CharBuffer.hasNext()

        override fun nextChar(): Char = read()
    }
}