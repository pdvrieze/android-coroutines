package nl.adaptivity.android.coroutines


sealed class Maybe<out T> {

    data class Error(val e: Exception): Maybe<Nothing>() {
        override fun <R> flatMap(function: (Nothing) -> R): Nothing {
            throw e
        }

        override fun <T> select(ok: T, cancelled: T, error: T) = error
    }

    object Cancelled: Maybe<Nothing>() {
        override fun <R> flatMap(function: (Nothing) -> R) = null
        override fun <T> select(ok: T, cancelled: T, error: T) = cancelled
    }

    data class Ok<T>(val data: T): Maybe<T>() {
        override fun <R> flatMap(function: (T) -> R): R = function(data)
        override fun <U> select(ok: U, cancelled: U, error: U) = ok
    }

    abstract fun <R> flatMap(function: (T) -> R): R?

    /**
     * Flatmap the identity function. Basically this gives the value for Ok, null when cancelled or
     * throws the exception for an error state.
     */
    fun flatMap(): T? = flatMap { it }

    /**
     * Create a new maybe with the function applied to the data (on Ok values only).
     * @param The function for the mapping.
     */
    @Suppress("unused")
    fun <R> map(function: (T) -> R): Maybe<R> {
        @Suppress("UNCHECKED_CAST")
        return when(this) {
            is Ok -> Ok(function(data))
            else -> this as Maybe<R>
        }
    }

    /**
     * Helper to determine whether the maybe has a value.
     */
    val isOk get() = this is Ok

    interface ErrorCallback { fun onError(e: Exception) }
    interface CancellationCallback { fun onCancelled() }
    interface SuccessCallback<in T> { fun onOk(d: T) }

    fun onError(function: ErrorCallback) = if (this is Error) function.onError(e) else null
    fun onCancelled(function: CancellationCallback) = if (this is Cancelled) function.onCancelled() else null
    @Suppress("unused")
    fun onOk(function: SuccessCallback<T>) = if (this is Ok) function.onOk(data) else null

    inline fun <R> onError(function: Error.(Exception) -> R):R? = if (this is Error) function(e) else null
    inline fun <R> onCancelled(function: Cancelled.() -> R):R? = if (this is Cancelled) function() else null
    inline fun <R> onOk(function: Ok<*>.(T) -> R):R? = if (this is Ok) this.function(data) else null

    abstract fun <T> select(ok: T, cancelled:T, error: T):T

    @Suppress("NOTHING_TO_INLINE")
    companion object {
        inline fun <T> error(e: Exception): Maybe<T> = Error(e)

        inline fun <T> cancelled(): Maybe<T> = Cancelled

        @Suppress("FunctionName")
        @Deprecated("Use Maybe.Ok instead", ReplaceWith("Maybe.Ok<T>value)"))
        inline fun <T> Success(value: T) = Ok(value)
    }

}