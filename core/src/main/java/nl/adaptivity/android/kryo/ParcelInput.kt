package nl.adaptivity.android.kryo

import android.os.Parcel
import com.esotericsoftware.kryo.io.Input
import java.io.InputStream

/**
 * Input class that uses a parcel to serialize. Perhaps not sustainable and ByteArrayStreams are better.
 */
class ParcelInput : Input {

    private val parcel: Parcel

    @JvmOverloads
    constructor(parcel: Parcel, bufferSize: Int = DEFAULT_BUFFER) : super(bufferSize) { this.parcel = parcel }
    constructor(parcel: Parcel, buffer: ByteArray?) : super(buffer) { this.parcel = parcel }
    constructor(parcel: Parcel, buffer: ByteArray?, offset: Int, count: Int) : super(buffer, offset, count) { this.parcel = parcel }


    override fun setInputStream(inputStream: InputStream?) {
        throw UnsupportedOperationException("ParcelInput reads from parcels, not streams")
    }

    override fun fill(buffer: ByteArray, offset: Int, count: Int): Int {
        val realCount = minOf(count, parcel.dataAvail())
        for (i in offset until realCount) {
            buffer[i] = parcel.readByte()
        }
        return realCount
    }

    override fun available(): Int {
        return limit - position + parcel.dataAvail()
    }

    override fun close() {
        // Don't do anything for now. Don't do our own recycling here.
    }

    companion object {
        const val DEFAULT_BUFFER = 1024
    }
}