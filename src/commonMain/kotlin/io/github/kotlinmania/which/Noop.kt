// port-lint: source src/lib.rs
package io.github.kotlinmania.which

/** A handler for non-fatal errors which does nothing with them. */
data object Noop : NonFatalErrorHandler {
    override fun handle(e: NonFatalError) {
        // Do nothing
    }
}
