package com.example.core.result

/**
 * Standardized result wrapper for asynchronous operations across Clean Architecture layers.
 *
 * Mapped to 13_API_CONTRACT.md and 14_STATE_AND_EDGE_CASES.md.
 */
sealed class AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>()
    data class Error(val error: AppError) : AppResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data

    inline fun <R> map(transform: (T) -> R): AppResult<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }
    }
}

/**
 * Machine-readable and human-navigable error classification.
 */
sealed class AppError(open val message: String) {
    data class ValidationError(override val message: String, val field: String? = null) : AppError(message)
    data class AuthenticationError(override val message: String = "Authentication required or session expired") : AppError(message)
    data class AuthorizationError(override val message: String = "Access denied for this resource") : AppError(message)
    data class NotFoundError(override val message: String = "Requested resource was not found") : AppError(message)
    data class ConflictError(override val message: String = "Conflict or state mismatch occurred") : AppError(message)
    data class RateLimitedError(val retryAfterSeconds: Int? = null) : AppError("Rate limit exceeded. Please wait.")
    data class NetworkError(override val message: String = "Network connection failed", val isTimeout: Boolean = false) : AppError(message)
    data class ServerError(val code: Int, override val message: String = "Internal server error") : AppError(message)
    data class UnknownError(val throwable: Throwable? = null, override val message: String = throwable?.message ?: "An unexpected error occurred") : AppError(message)
}
