package com.amverge.android.core.result

sealed interface OperationResult<out T> {
    data class Success<T>(val data: T) : OperationResult<T>
    data class Failure(val error: AppError) : OperationResult<Nothing>

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw error
    }
}

inline fun <T, R> OperationResult<T>.map(transform: (T) -> R): OperationResult<R> = when (this) {
    is OperationResult.Success -> OperationResult.Success(transform(data))
    is OperationResult.Failure -> this
}

inline fun <T> OperationResult<T>.onSuccess(action: (T) -> Unit): OperationResult<T> {
    if (this is OperationResult.Success) action(data)
    return this
}

inline fun <T> OperationResult<T>.onFailure(action: (AppError) -> Unit): OperationResult<T> {
    if (this is OperationResult.Failure) action(error)
    return this
}
