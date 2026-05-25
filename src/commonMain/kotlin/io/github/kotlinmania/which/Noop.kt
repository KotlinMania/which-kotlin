// port-lint: source src/lib.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.which

import kotlin.native.HiddenFromObjC

/** A handler for non-fatal errors which does nothing with them. */
@HiddenFromObjC
data object Noop : NonFatalErrorHandler {
    override fun handle(e: NonFatalError) {
        // Do nothing
    }
}
