/*
 * The MIT License
 *
 * Copyright 2017 Ayesha.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.horrorho.ragingmoose

import java.lang.Long.toHexString
import java.nio.ByteBuffer
import java.util.Objects
import javax.annotation.ParametersAreNonnullByDefault
import javax.annotation.concurrent.NotThreadSafe

/**
 * Low level bit in stream.
 *
 * @author Ayesha
 */
@NotThreadSafe
internal class BitInStream constructor(private val `in`: ByteBuffer, bits: Int) {
    // accumNBits 63 bit limit avoids unsupported 64 bit shifts/ branch.

    private var accum: Long
    private var accumNBits: Int

    init {
        when {
            bits > 0 -> throw LZFSEDecoderException()
            bits == 0 -> {
                `in`.position(`in`.position() - 7)
                accum = `in`.getLong(`in`.position() - 1) ushr 8
                accumNBits = 56
            }
            else -> {
                `in`.position(`in`.position() - 8)
                accum = `in`.getLong(`in`.position())
                accumNBits = bits + 64
            }
        }
    }

    fun fill() {
        if (accumNBits < 56) {
            val nBits = 63 - accumNBits
            val nBytes = nBits.ushr(3)
            val mBits = (nBits and 0x07) + 1
            `in`.position(`in`.position() - nBytes)
            accum = (`in`.getLong(`in`.position()) shl mBits) ushr mBits
            accumNBits += nBytes shl 3
        }
    }

    fun read(n: Int): Int {
        if (n > accumNBits) {
            throw IllegalStateException()
        }
        assert(n >= 0)
        accumNBits -= n
        val bits = accum.ushr(accumNBits)
        accum = accum xor (bits shl accumNBits)
        return bits.toInt()
    }

    override fun toString(): String {
        return "BitStream{in=$`in`, accum=0x${toHexString(accum)}, accumNBits=$accumNBits}"
    }
}
