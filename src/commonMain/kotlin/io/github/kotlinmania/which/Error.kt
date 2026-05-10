// port-lint: source src/error.rs
package io.github.kotlinmania.which

/**
 * Result type used throughout this crate. Wraps a value of type [T] on success
 * or an [Error] on failure.
 */
typealias Result<T> = kotlin.Result<T>

/** Reasons a [which] lookup can fail. */
sealed class Error(message: String) : RuntimeException(message) {
    /** An executable binary with that name was not found. */
    data object CannotFindBinaryPath : Error("cannot find binary path")

    /** There was nowhere to search and the provided name wasn't an absolute path. */
    data object CannotGetCurrentDirAndPathListEmpty :
        Error("no path to search and provided name is not an absolute path")

    /** Failed to canonicalize the path found. */
    data object CannotCanonicalize : Error("cannot canonicalize path")
}

/**
 * Reasons a non-fatal problem may surface during a [which] lookup. A non-fatal
 * error represents a problem but does not necessarily require [which] to stop
 * its search.
 *
 * This type is not exhaustive: new variants may be added in future versions.
 */
sealed class NonFatalError {
    /** The underlying I/O operation failed. */
    data class Io(val cause: IoError) : NonFatalError()

    override fun toString(): String = when (this) {
        is Io -> cause.message ?: cause::class.simpleName.orEmpty()
    }
}

/**
 * Lightweight stand-in for `std::io::Error`. Wraps an error message and an
 * optional platform-level cause so the [NonFatalError.Io] variant can travel
 * through common code without depending on any particular I/O framework.
 */
open class IoError(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
