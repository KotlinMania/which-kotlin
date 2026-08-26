// Common path helpers used across the port.
package io.github.kotlinmania.which

internal fun isAbsolutePath(path: String): Boolean {
    if (path.isEmpty()) return false
    if (path[0] == '/' || path[0] == '\\') return true
    if (path.length >= 3 && path[1] == ':' && (path[2] == '/' || path[2] == '\\')) {
        val drive = path[0]
        if ((drive in 'A'..'Z') || (drive in 'a'..'z')) return true
    }
    return false
}

internal fun pathSeparators(): CharArray = charArrayOf('/', '\\')

internal fun pathHasSeparator(path: String): Boolean =
    path.any { it == '/' || it == '\\' }

internal fun pathFileName(path: String): String? {
    if (path.isEmpty()) return null
    var end = path.length
    while (end > 0 && (path[end - 1] == '/' || path[end - 1] == '\\')) end--
    if (end == 0) return null
    var start = end
    while (start > 0 && path[start - 1] != '/' && path[start - 1] != '\\') start--
    val name = path.substring(start, end)
    return if (name == ".." || name == ".") null else name.ifEmpty { null }
}

internal fun pathParent(path: String): String? {
    if (path.isEmpty()) return null
    var end = path.length
    while (end > 0 && (path[end - 1] == '/' || path[end - 1] == '\\')) end--
    if (end == 0) return null
    while (end > 0 && path[end - 1] != '/' && path[end - 1] != '\\') end--
    if (end == 0) return null
    while (end > 1 && (path[end - 1] == '/' || path[end - 1] == '\\')) end--
    return path.substring(0, end)
}

internal fun pathJoin(base: String, leaf: String): String {
    if (leaf.isEmpty()) return base
    if (isAbsolutePath(leaf)) return leaf
    if (base.isEmpty()) return leaf
    val last = base.last()
    return if (last == '/' || last == '\\') base + leaf else "$base/$leaf"
}

internal fun pathConcat(path: String, suffix: String): String = path + suffix

/**
 * Counts the number of meaningful path components. Used by `Finder` to detect
 * paths that contain a separator (`a/b`, `./b`, `/b`) versus a bare binary name
 * (`rustc`).
 */
internal fun pathComponentCount(path: String): Int {
    if (path.isEmpty()) return 0
    var count = 0
    var i = 0
    if (isAbsolutePath(path)) {
        count = 1
        while (i < path.length && (path[i] == '/' || path[i] == '\\')) i++
        if (path.length >= 3 && path[1] == ':' && (path[2] == '/' || path[2] == '\\')) {
            i = 3
        }
    }
    var inSeg = false
    while (i < path.length) {
        val c = path[i]
        if (c == '/' || c == '\\') {
            if (inSeg) {
                count++
                inSeg = false
            }
        } else {
            inSeg = true
        }
        i++
    }
    if (inSeg) count++
    return count
}

/**
 * Resolves [path] against [cwd]. If [path] is already absolute, it is returned
 * verbatim. Otherwise leading `.` components are stripped and the remainder is
 * appended onto [cwd]. Mirrors the upstream `PathExt::to_absolute` translation.
 */
internal fun pathToAbsolute(path: String, cwd: String): String {
    if (isAbsolutePath(path)) return path
    val segments = path.split('/', '\\').filter { it.isNotEmpty() }
    val pruned = segments.dropWhile { it == "." }
    var result = cwd
    for (seg in pruned) {
        result = pathJoin(result, seg)
    }
    return result
}

internal fun firstNormalComponent(path: String): String? {
    var i = 0
    if (path.isNotEmpty() && (path[0] == '/' || path[0] == '\\')) return null
    if (path.length >= 2 && path[1] == ':' && (path.length == 2 || path[2] == '/' || path[2] == '\\')) {
        return null
    }
    while (i < path.length && path[i] != '/' && path[i] != '\\') i++
    val seg = path.substring(0, i)
    return seg.ifEmpty { null }
}

internal fun pathDropFirstComponent(path: String): String {
    var i = 0
    while (i < path.length && path[i] != '/' && path[i] != '\\') i++
    while (i < path.length && (path[i] == '/' || path[i] == '\\')) i++
    return path.substring(i)
}
