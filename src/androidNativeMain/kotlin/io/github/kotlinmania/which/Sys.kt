// port-lint: source src/sys.rs
package io.github.kotlinmania.which

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.S_IFLNK
import platform.posix.lstat
import platform.posix.stat

@OptIn(ExperimentalForeignApi::class)
internal actual fun posixLstat(path: String, sb: CPointer<stat>): Int = lstat(path, sb)

internal actual fun posixSymlinkBit(): Int = S_IFLNK
