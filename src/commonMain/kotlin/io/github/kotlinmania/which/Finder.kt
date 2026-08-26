// port-lint: source finder.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.which

import kotlin.native.HiddenFromObjC

private fun String.hasSeparator(): Boolean = pathComponentCount(this) > 1

private fun String.toAbsolute(cwd: String): String = pathToAbsolute(this, cwd)

@HiddenFromObjC
class Finder(private val sys: Sys) {

    fun find(
        binaryName: String,
        paths: String?,
        cwd: String?,
        nonfatalErrorHandler: NonFatalErrorHandler,
    ): Result<Iterator<String>> {
        val path = binaryName

        val ret: Iterator<String> = when {
            cwd != null && path.hasSeparator() ->
                WhichFindIterator.newCwd(path, cwd, sys, nonfatalErrorHandler)

            else -> {
                // Search binary in PATHs (defined in environment variable).
                val pathsValue = paths
                    ?: return kotlin.Result.failure(Error.CannotGetCurrentDirAndPathListEmpty)
                val splitPaths = sys.envSplitPaths(pathsValue)
                if (splitPaths.isEmpty()) {
                    return kotlin.Result.failure(Error.CannotGetCurrentDirAndPathListEmpty)
                }
                WhichFindIterator.newPaths(path, splitPaths, sys, nonfatalErrorHandler)
            }
        }
        return kotlin.Result.success(ret)
    }

    fun findRe(
        binaryRegex: Regex,
        paths: String?,
        nonfatalErrorHandler: NonFatalErrorHandler,
    ): Result<Iterator<String>> =
        WhichFindRegexIter.create(sys, paths, binaryRegex, nonfatalErrorHandler)

    companion object {
        fun new(sys: Sys): Finder = Finder(sys)
    }
}

private class WhichFindIterator private constructor(
    private val sys: Sys,
    private val paths: PathsIter,
    private val nonfatalErrorHandler: NonFatalErrorHandler,
) : Iterator<String> {

    private var pending: String? = null
    private var fetched: Boolean = false

    override fun hasNext(): Boolean {
        if (!fetched) {
            pending = computeNext()
            fetched = true
        }
        return pending != null
    }

    override fun next(): String {
        if (!hasNext()) throw NoSuchElementException()
        val value = pending!!
        pending = null
        fetched = false
        return value
    }

    private fun computeNext(): String? {
        while (paths.hasNext()) {
            val path = paths.next()
            if (isValid(sys, path, nonfatalErrorHandler)) {
                return correctCasing(sys, path, nonfatalErrorHandler)
            }
        }
        return null
    }

    companion object {
        fun newCwd(
            binaryName: String,
            cwd: String,
            sys: Sys,
            nonfatalErrorHandler: NonFatalErrorHandler,
        ): WhichFindIterator {
            val pathExtensions = if (sys.isWindows()) sys.envWindowsPathExt() else emptyList()
            val pathsIter = PathsIter(
                paths = listOf(binaryName.toAbsolute(cwd)).iterator(),
                pathExtensions = pathExtensions,
            )
            return WhichFindIterator(sys, pathsIter, nonfatalErrorHandler)
        }

        fun newPaths(
            binaryName: String,
            paths: List<String>,
            sys: Sys,
            nonfatalErrorHandler: NonFatalErrorHandler,
        ): WhichFindIterator {
            val pathExtensions = if (sys.isWindows()) sys.envWindowsPathExt() else emptyList()
            val expanded = paths.map { p -> pathJoin(tildeExpansion(sys, p), binaryName) }
            val pathsIter = PathsIter(
                paths = expanded.iterator(),
                pathExtensions = pathExtensions,
            )
            return WhichFindIterator(sys, pathsIter, nonfatalErrorHandler)
        }
    }
}

private class PathsIter(
    private val paths: Iterator<String>,
    private val pathExtensions: List<String>,
) : Iterator<String> {

    private var currentPathWithIndex: Pair<String, Int>? = null
    private var pending: String? = null
    private var fetched: Boolean = false

    override fun hasNext(): Boolean {
        if (!fetched) {
            pending = computeNext()
            fetched = true
        }
        return pending != null
    }

    override fun next(): String {
        if (!hasNext()) throw NoSuchElementException()
        val value = pending!!
        pending = null
        fetched = false
        return value
    }

    private fun computeNext(): String? {
        if (pathExtensions.isEmpty()) {
            return if (paths.hasNext()) paths.next() else null
        }
        val cur = currentPathWithIndex
        return if (cur != null) {
            val (p, index) = cur
            val nextIndex = index + 1
            currentPathWithIndex = if (nextIndex < pathExtensions.size) p to nextIndex else null
            // Append the extension.
            pathConcat(p, pathExtensions[index])
        } else {
            if (!paths.hasNext()) return null
            val p = paths.next()
            if (!hasExecutableExtension(p, pathExtensions)) {
                // Appended paths with windows executable extensions.
                // For example, path `c:/windows/bin[.ext]` will expand to:
                // c:/windows/bin[.ext]
                // c:/windows/bin[.ext].COM
                // c:/windows/bin[.ext].EXE
                // c:/windows/bin[.ext].CMD
                // ...
                currentPathWithIndex = p to 0
            }
            p
        }
    }
}

private fun tildeExpansion(sys: Sys, p: String): String {
    val first = firstNormalComponent(p) ?: return p
    if (first != "~") return p
    val home = sys.homeDir() ?: return p
    val rest = pathDropFirstComponent(p)
    return if (rest.isEmpty()) home else pathJoin(home, rest)
}

private fun correctCasing(
    sys: Sys,
    p: String,
    nonfatalErrorHandler: NonFatalErrorHandler,
): String {
    if (!sys.isWindows()) return p
    val parent = pathParent(p) ?: return p
    val fileName = pathFileName(p) ?: return p
    val iter = sys.readDir(parent).getOrNull() ?: return p
    while (iter.hasNext()) {
        val entry = iter.next()
        entry.fold(
            onSuccess = { e ->
                if (e.fileName().equals(fileName, ignoreCase = true)) {
                    return pathJoin(parent, e.fileName())
                }
            },
            onFailure = { e ->
                nonfatalErrorHandler.handle(NonFatalError.Io(e.toIoError()))
            },
        )
    }
    return p
}

private class WhichFindRegexIter private constructor(
    private val sys: Sys,
    private val re: Regex,
    private val paths: Iterator<String>,
    private val nonfatalErrorHandler: NonFatalErrorHandler,
) : Iterator<String> {

    private var currentReadDirIter: Iterator<kotlin.Result<SysReadDirEntry>>? = null
    private var pending: String? = null
    private var fetched: Boolean = false

    override fun hasNext(): Boolean {
        if (!fetched) {
            pending = computeNext()
            fetched = true
        }
        return pending != null
    }

    override fun next(): String {
        if (!hasNext()) throw NoSuchElementException()
        val value = pending!!
        pending = null
        fetched = false
        return value
    }

    private fun computeNext(): String? {
        while (true) {
            val iter = currentReadDirIter
            if (iter != null) {
                if (iter.hasNext()) {
                    val entry = iter.next()
                    val matched = entry.fold(
                        onSuccess = { path ->
                            if (re.containsMatchIn(path.fileName())) path.path() else null
                        },
                        onFailure = { e ->
                            nonfatalErrorHandler.handle(NonFatalError.Io(e.toIoError()))
                            null
                        },
                    )
                    if (matched != null) return matched
                } else {
                    currentReadDirIter = null
                }
            } else {
                if (!paths.hasNext()) return null
                val path = paths.next()
                sys.readDir(path).fold(
                    onSuccess = { newIter -> currentReadDirIter = newIter },
                    onFailure = { e ->
                        nonfatalErrorHandler.handle(NonFatalError.Io(e.toIoError()))
                    },
                )
            }
        }
    }

    companion object {
        fun create(
            sys: Sys,
            paths: String?,
            re: Regex,
            nonfatalErrorHandler: NonFatalErrorHandler,
        ): Result<Iterator<String>> {
            val p = paths ?: return kotlin.Result.failure(Error.CannotGetCurrentDirAndPathListEmpty)
            val split = sys.envSplitPaths(p)
            return kotlin.Result.success(
                WhichFindRegexIter(sys, re, split.iterator(), nonfatalErrorHandler),
            )
        }
    }
}

private fun Throwable.toIoError(): IoError =
    this as? IoError ?: IoError(message ?: this::class.simpleName.orEmpty(), this)
