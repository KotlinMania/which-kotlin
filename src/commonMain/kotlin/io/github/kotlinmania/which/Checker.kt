// port-lint: source src/checker.rs
package io.github.kotlinmania.which

fun isValid(
    sys: Sys,
    path: String,
    nonfatalErrorHandler: NonFatalErrorHandler,
): Boolean =
    exists(sys, path, nonfatalErrorHandler) && isExecutable(sys, path, nonfatalErrorHandler)

private fun isExecutable(
    sys: Sys,
    path: String,
    nonfatalErrorHandler: NonFatalErrorHandler,
): Boolean {
    return if (sys.isWindows() && pathExtension(path) != null) {
        true
    } else {
        sys.isValidExecutable(path)
            .onFailure { e -> nonfatalErrorHandler.handle(NonFatalError.Io(e.asIoError())) }
            .getOrDefault(false)
    }
}

private fun exists(
    sys: Sys,
    path: String,
    nonfatalErrorHandler: NonFatalErrorHandler,
): Boolean {
    return if (sys.isWindows()) {
        sys.symlinkMetadata(path)
            .map { metadata -> metadata.isFile() || metadata.isSymlink() }
            .onFailure { e -> nonfatalErrorHandler.handle(NonFatalError.Io(e.asIoError())) }
            .getOrDefault(false)
    } else {
        val result = sys.metadata(path).map { metadata -> metadata.isFile() }
        result.fold(
            onSuccess = { it },
            onFailure = { e ->
                nonfatalErrorHandler.handle(NonFatalError.Io(e.asIoError()))
                false
            },
        )
    }
}

private fun Throwable.asIoError(): IoError =
    this as? IoError ?: IoError(message ?: this::class.simpleName.orEmpty(), this)
