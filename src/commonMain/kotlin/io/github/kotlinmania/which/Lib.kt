// port-lint: source src/lib.rs
//
// which
//
// A Kotlin Multiplatform equivalent of the Unix command `which(1)`.
//
// # Example:
//
// To find which rustc executable binary is using:
//
// ```
// import io.github.kotlinmania.which.which
//
// val result = which("rustc").getOrThrow()
// assertEquals("/usr/bin/rustc", result)
// ```
package io.github.kotlinmania.which

/** A handler for non-fatal errors which does nothing with them. */
data object Noop : NonFatalErrorHandler {
    override fun handle(e: NonFatalError) {
        // Do nothing
    }
}

/**
 * Defines what should happen when a nonfatal error is encountered. A nonfatal
 * error may represent a problem, but it doesn't necessarily require [which] to
 * stop its search.
 *
 * Any function that takes a single [NonFatalError] argument can act as a
 * handler via the [forFunction] adapter. You may also implement this
 * interface for your own types.
 */
interface NonFatalErrorHandler {
    fun handle(e: NonFatalError)

    companion object {
        /**
         * Adapts a plain function reference into a [NonFatalErrorHandler],
         * matching the upstream blanket impl for any `FnMut(NonFatalError)`.
         */
        fun forFunction(f: (NonFatalError) -> Unit): NonFatalErrorHandler =
            object : NonFatalErrorHandler {
                override fun handle(e: NonFatalError) {
                    f(e)
                }
            }
    }
}
