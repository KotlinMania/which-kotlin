// port-lint: ignore
// Integration tests that exercise the native `RealSys` implementation against
// a real on-disk tempdir. The upstream `tests/basic.rs` runs a similar shape
// using the `tempfile` crate; here we drive POSIX directly because there is no
// tempfile-kotlin sibling port yet.
package io.github.kotlinmania.which

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import platform.posix.S_IRGRP
import platform.posix.S_IROTH
import platform.posix.S_IRUSR
import platform.posix.S_IWUSR
import platform.posix.S_IXGRP
import platform.posix.S_IXOTH
import platform.posix.S_IXUSR
import platform.posix.chmod
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs
import platform.posix.getenv
import platform.posix.mkdir
import platform.posix.remove
import platform.posix.rmdir

@OptIn(ExperimentalForeignApi::class)
class RealSysTest {

    private lateinit var root: String
    private val tracked = mutableListOf<String>()

    @BeforeTest
    fun setup() {
        val base = getenv("TMPDIR")?.toKString()?.trimEnd('/') ?: "/tmp"
        val unique = "which-test-${Random.nextLong().toULong().toString(16)}"
        root = "$base/$unique"
        require(mkdir(root, modeBits(read = true, write = true, exec = true)) == 0) {
            "mkdir($root) failed"
        }
    }

    @AfterTest
    fun teardown() {
        for (p in tracked.reversed()) {
            remove(p)
        }
        rmdir(root)
    }

    private fun writeExecutable(dir: String, name: String, executable: Boolean = true): String {
        val full = "$dir/$name"
        ensureDir(dir)
        val fh = fopen(full, "wb") ?: error("fopen($full) failed")
        try {
            fputs("#!/bin/sh\necho $name\n", fh)
        } finally {
            fclose(fh)
        }
        val mode = if (executable) {
            modeBits(read = true, write = true, exec = true)
        } else {
            modeBits(read = true, write = true, exec = false)
        }
        chmod(full, mode)
        tracked.add(full)
        return full
    }

    private fun ensureDir(path: String) {
        if (path == root || path in tracked) return
        if (!path.startsWith("$root/")) return
        mkdir(path, modeBits(read = true, write = true, exec = true))
        tracked.add(path)
    }

    @Test
    fun findsBinaryByNameInPath() {
        val binA = "$root/a"
        val binB = "$root/b"
        writeExecutable(binA, "rustc")
        writeExecutable(binB, "cargo")

        val sys = RealSys()
        val iter = Finder(sys).find("rustc", "$binA:$binB", root, Noop).getOrThrow()
        assertTrue(iter.hasNext())
        assertEquals("$binA/rustc", iter.next())
        assertFalse(iter.hasNext())
    }

    @Test
    fun skipsNonExecutableMatches() {
        val dir = "$root/d"
        writeExecutable(dir, "foo", executable = false)
        val good = "$root/e"
        writeExecutable(good, "foo")

        val sys = RealSys()
        val iter = Finder(sys).find("foo", "$dir:$good", root, Noop).getOrThrow()
        assertEquals("$good/foo", iter.next())
    }

    @Test
    fun returnsErrorWhenNoPathsAndNoSeparator() {
        val sys = RealSys()
        val result = Finder(sys).find("foo", null, root, Noop)
        assertTrue(result.isFailure)
        assertEquals(Error.CannotGetCurrentDirAndPathListEmpty, result.exceptionOrNull())
    }

    @Test
    fun resolvesRelativeNameWithSeparatorAgainstCwd() {
        val sub = "$root/work"
        writeExecutable(sub, "tool")
        val sys = RealSys()
        val iter = Finder(sys).find("work/tool", null, root, Noop).getOrThrow()
        assertEquals("$root/work/tool", iter.next())
    }

    @Test
    fun pathExtCorrectsCasing() {
        // The Windows correctCasing branch isn't exercised on POSIX; this is a
        // smoke test that `is_valid` plus directory listing still produces a
        // hit on a path that exactly matches.
        val dir = "$root/g"
        writeExecutable(dir, "exact")
        val sys = RealSys()
        val iter = Finder(sys).find("exact", dir, root, Noop).getOrThrow()
        assertEquals("$dir/exact", iter.next())
    }

    @Test
    fun findRegexMatchesFilenames() {
        val dir = "$root/r"
        writeExecutable(dir, "python2")
        writeExecutable(dir, "python3")
        writeExecutable(dir, "ruby")
        val sys = RealSys()
        val iter = Finder(sys).findRe(Regex("^python\\d$"), dir, Noop).getOrThrow()
        val all = iter.asSequence().toList().sorted()
        assertEquals(listOf("$dir/python2", "$dir/python3"), all)
    }

    private fun modeBits(read: Boolean, write: Boolean, exec: Boolean): UShort {
        var m = 0
        if (read) m = m or S_IRUSR or S_IRGRP or S_IROTH
        if (write) m = m or S_IWUSR
        if (exec) m = m or S_IXUSR or S_IXGRP or S_IXOTH
        return m.toUShort()
    }
}

