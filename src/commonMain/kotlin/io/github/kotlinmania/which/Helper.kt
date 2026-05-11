// port-lint: source src/helper.rs
package io.github.kotlinmania.which

/**
 * Returns the extension portion of [path], following the same rules as
 * `std::path::Path::extension`: the substring after the final `.` of the path's
 * final component. Returns `null` when the final component has no extension
 * (no embedded dot, or only a leading dot as in `.bashrc`, or the special
 * `..` component).
 */
internal fun pathExtension(path: String): String? {
    val sep = maxOf(path.lastIndexOf('/'), path.lastIndexOf('\\'))
    val fileName = if (sep < 0) path else path.substring(sep + 1)
    if (fileName.isEmpty() || fileName == "..") return null
    val dotIndex = fileName.lastIndexOf('.')
    if (dotIndex <= 0) return null
    return fileName.substring(dotIndex + 1)
}

/** Check if given path has extension which in the given vector. */
fun hasExecutableExtension(path: String, pathext: List<String>): Boolean {
    val ext = pathExtension(path) ?: return false
    return pathext.any { e -> e.isNotEmpty() && ext.equals(e.substring(1), ignoreCase = true) }
}
