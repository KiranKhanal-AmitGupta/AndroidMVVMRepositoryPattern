package com.constructivecoders.wowland.base

/**
 * A generic class that holds hopper value with its loading status.
 * @param <T>
 */
sealed class Result<out T : Any> {

    data class Success<out T : Any>(val data: T?, val code: Int = 0) : Result<T>()
    data class Error(
        val exception: Exception? = null,
        val errorMsg: String = "",
        val code: Int = -1
    ) : Result<Nothing>()

    data class Loading<out T : Any>(val data: T?, val percentage: Int? = 0) : Result<T>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[exception=$exception]"
            is Loading<*> -> "Loading[data=$data percantage=$percentage]"
        }
    }
}
