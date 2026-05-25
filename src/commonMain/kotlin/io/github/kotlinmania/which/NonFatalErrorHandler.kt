// port-lint: source src/lib.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.which

import kotlin.native.HiddenFromObjC

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
    @HiddenFromObjC
    fun handle(e: NonFatalError)

    companion object {
        /**
         * Adapts a plain function reference into a [NonFatalErrorHandler],
         * matching the upstream blanket impl for any `FnMut(NonFatalError)`.
         */
        @HiddenFromObjC
        fun forFunction(f: (NonFatalError) -> Unit): NonFatalErrorHandler =
            object : NonFatalErrorHandler {
                override fun handle(e: NonFatalError) {
                    f(e)
                }
            }
    }
}
