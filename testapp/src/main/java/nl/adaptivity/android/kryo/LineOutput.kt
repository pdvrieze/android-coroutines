package nl.adaptivity.android.kryo

import com.esotericsoftware.kryo.io.Output
import java.io.OutputStream

class LineOutput(outStream: OutputStream) : Output(outStream) {

    var writer = outStream.bufferedWriter()

    override fun writeShort(value: Int) {
        super.writeShort(value)
    }

    override fun clear() {
        throw UnsupportedOperationException()
    }

    fun rawByte(value: Byte) {
        writer.write(value.toInt())
    }

    override fun writeString(value: String?) {
        if (value == null) {
            rawByte(0)
        } else {
            writer.write(value)
        }
        writer.write('\n'.toInt())
    }

    override fun writeString(value: CharSequence?) {
        writeString(value?.toString())
    }

    override fun writeBytes(bytes: ByteArray) {
        writer.flush()
        outputStream.write(bytes)
    }

    override fun writeBytes(bytes: ByteArray, offset: Int, count: Int) {
        writer.flush()
        outputStream.write(bytes, offset, count)
    }

    override fun writeFloats(value: FloatArray?) {
        if (value == null) {
            rawByte(0)
        } else {
            value.joinTo(writer) { it.toString() }
        }
        writer.write('\n'.toInt())
    }

    override fun writeDoubles(value: DoubleArray?) {
        if (value == null) {
            rawByte(0)
        } else {
            value.joinTo(writer) { it.toString() }
        }
        writer.write('\n'.toInt())
    }

    override fun write(value: Int) {
        super.write(value)
    }

    override fun write(bytes: ByteArray?) {
        super.write(bytes)
    }

    override fun write(bytes: ByteArray?, offset: Int, length: Int) {
        super.write(bytes, offset, length)
    }

    override fun flush() {
        writer.flush()
        super.flush()
    }

    override fun writeChar(value: Char) {
        writer.write(value.toString())
        writer.write('\n'.toInt())
    }

    override fun writeBoolean(value: Boolean) {
        writer.write(value.toString())
        writer.write('\n'.toInt())
    }

    override fun writeInt(value: Int) {
        writer.write(value.toString())
        writer.write('\n'.toInt())
    }

    override fun writeInt(value: Int, optimizePositive: Boolean): Int {
        writer.write(value.toString())
        writer.write('\n'.toInt())
        return 1
    }

    override fun writeShorts(value: ShortArray?) {
        if (value == null) {
            rawByte(0)
        } else {
            value.joinTo(writer) { it.toString() }
        }
        writer.write('\n'.toInt())
    }

    override fun writeLongs(value: LongArray?, optimizePositive: Boolean) {
        if (value == null) {
            rawByte(0)
        } else {
            value.joinTo(writer) { it.toString() }
        }
        writer.write('\n'.toInt())
    }

    override fun writeLongs(value: LongArray?) {
        if (value == null) {
            rawByte(0)
        } else {
            value.joinTo(writer) { it.toString() }
        }
        writer.write('\n'.toInt())
    }

    override fun close() {
        writer.close()
        outputStream.close()
    }

    override fun writeInts(value: IntArray?, optimizePositive: Boolean) {
        if (value == null) {
            rawByte(0)
        } else {
            value.joinTo(writer) { it.toString() }
        }
        writer.write('\n'.toInt())
    }

    override fun writeInts(value: IntArray?) {
        if (value == null) {
            rawByte(0)
        } else {
            value.joinTo(writer) { it.toString() }
        }
        writer.write('\n'.toInt())
    }

    override fun writeLong(value: Long) {
        writer.write(value.toString())
        writer.write('\n'.toInt())
    }

    override fun writeLong(value: Long, optimizePositive: Boolean): Int {
        writeLong(value)
        return 1
    }

    override fun writeDouble(value: Double) {
        writer.write(value.toString())
        writer.write('\n'.toInt())
    }

    override fun writeDouble(value: Double, precision: Double, optimizePositive: Boolean): Int {
        writer.write(value.toString())
        writer.write('\n'.toInt())
        return 1
    }

    override fun writeByte(value: Byte) {
        writer.write(value.toString(16).padStart(2, '0'))
        writer.write('\n'.toInt())
    }

    override fun writeByte(value: Int) {
        writeByte(value.toByte())
    }

    override fun setOutputStream(outputStream: OutputStream) {
        writer.flush()
        writer = outputStream.bufferedWriter()
        super.setOutputStream(outputStream)
    }

    override fun writeFloat(value: Float) {
        writer.write(value.toString())
        writer.write('\n'.toInt())
    }

    override fun writeFloat(value: Float, precision: Float, optimizePositive: Boolean): Int {
        writer.write(value.toString())
        writer.write('\n'.toInt())
        return 1
    }

    override fun writeChars(value: CharArray?) {
        writeString(value?.let { String(it) })
    }

    override fun writeAscii(value: String?) {
        writeString(value)
    }

    override fun writeVarInt(value: Int, optimizePositive: Boolean): Int {
        writer.write(value.toString())
        writer.write('\n'.toInt())
        return 1
    }

    override fun writeVarLong(value: Long, optimizePositive: Boolean): Int {
        writer.write(value.toString())
        writer.write('\n'.toInt())
        return 1
    }
}