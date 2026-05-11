// port-lint: source src/lib.rs
package io.github.kotlinmania.which

/**
 * Defines what should happen when a nonfatal error is encountered. A nonfatal
 * error may represent a problem, but it doesn't necessarily require [which] to
 * stop its search.
 *
 * Any function that takes a single [NonFatalError] argument can act as a
 * handler via the [Companion.forFunction] adapter. You may also implement this
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
