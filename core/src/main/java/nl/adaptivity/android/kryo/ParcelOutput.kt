package nl.adaptivity.android.kryo

import android.os.Parcel
import com.esotericsoftware.kryo.io.Output
import java.io.OutputStream

/**
 * Output class that uses a parcel to serialize. Perhaps not sustainable and ByteArrayStreams are better.
 */
class ParcelOutput: Output {
    private val parcel: Parcel

    @JvmOverloads
    constructor(parcel: Parcel, bufferSize: Int = DEFAULT_BUFFER) : super(bufferSize) { this.parcel = parcel }
    constructor(parcel: Parcel, bufferSize: Int, maxBufferSize: Int) : super(bufferSize, maxBufferSize) { this.parcel = parcel }
    constructor(parcel: Parcel, buffer: ByteArray?) : super(buffer) { this.parcel = parcel }
    constructor(parcel: Parcel, buffer: ByteArray?, maxBufferSize: Int) : super(buffer, maxBufferSize) { this.parcel = parcel }

    override fun setOutputStream(outputStream: OutputStream) {
        throw UnsupportedOperationException("ParcelInput writes to parcels, not streams")
    }

    override fun flush() {
        for(i in 0 until position) {
            parcel.writeByte(buffer[i])
        }
    }

    override fun close() {
        // Do nothing
    }

    companion object {
        const val DEFAULT_BUFFER = 1024
    }
}