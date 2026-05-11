// port-lint: ignore
// Integration tests for the public top-level which()/whichAll()/whichRe() free
// functions and their Path.* companion factories. Drives the native `RealSys`
// implementation against a real on-disk tempdir; the wrapped Finder layer is
// covered separately by RealSysTest, this file is specifically about the
// public API surface exposed to consumers of the crate.
@file:OptIn(ExperimentalForeignApi::class)

package io.github.kotlinmania.which

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.toKString
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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

class WhichTest {

    private lateinit var root: String
    private val tracked = mutableListOf<String>()

    @BeforeTest
    fun setup() {
        val base = getenv("TMPDIR")?.toKString()?.trimEnd('/') ?: "/tmp"
        val unique = "which-api-test-${Random.nextLong().toULong().toString(16)}"
        root = "$base/$unique"
        require(mkdir(root, modeBits(read = true, write = true, exec = true).convert()) == 0) {
            "mkdir($root) failed"
        }
    }

    @AfterTest
    fun teardown() {
        for (p in tracked.reversed()) remove(p)
        rmdir(root)
    }

    private fun writeExecutable(dir: String, name: String, executable: Boolean = true): String {
        ensureDir(dir)
        val full = "$dir/$name"
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
        chmod(full, mode.convert())
        tracked.add(full)
        return full
    }

    private fun ensureDir(path: String) {
        if (path == root || path in tracked) return
        if (!path.startsWith("$root/")) return
        mkdir(path, modeBits(read = true, write = true, exec = true).convert())
        tracked.add(path)
    }

    @Test
    fun whichIn_returns_first_executable_in_paths() {
        val binA = "$root/a"
        val binB = "$root/b"
        writeExecutable(binA, "rustc")
        writeExecutable(binB, "rustc")

        val result = whichIn("rustc", "$binA:$binB", root).getOrThrow()
        assertEquals("$binA/rustc", result)
    }

    @Test
    fun whichInAll_returns_iterator_of_all_matches() {
        val binA = "$root/a"
        val binB = "$root/b"
        writeExecutable(binA, "tool")
        writeExecutable(binB, "tool")

        val all = whichInAll("tool", "$binA:$binB", root).getOrThrow()
            .asSequence().toList()
        assertEquals(listOf("$binA/tool", "$binB/tool"), all)
    }

    @Test
    fun whichInGlobal_ignores_cwd_for_relative_resolution() {
        val sub = "$root/work"
        writeExecutable(sub, "tool")

        val global = whichInGlobal("work/tool", sub)
        assertTrue(global.isFailure || global.getOrThrow().asSequence().none())
    }

    @Test
    fun whichReIn_matches_regex_across_paths() {
        val dir = "$root/r"
        writeExecutable(dir, "python2")
        writeExecutable(dir, "python3")
        writeExecutable(dir, "ruby")

        val all = whichReIn(Regex("^python\\d$"), dir).getOrThrow()
            .asSequence().toList().sorted()
        assertEquals(listOf("$dir/python2", "$dir/python3"), all)
    }

    @Test
    fun whichIn_returns_CannotFindBinaryPath_when_no_match() {
        val dir = "$root/empty"
        ensureDir(dir)

        val result = whichIn("definitely-not-here-xyz", dir, root)
        assertTrue(result.isFailure)
        assertEquals(Error.CannotFindBinaryPath, result.exceptionOrNull())
    }

    @Test
    fun whichIn_returns_CannotGetCurrentDirAndPathListEmpty_when_paths_null() {
        val result = whichIn("tool", null, root)
        assertTrue(result.isFailure)
        assertEquals(Error.CannotGetCurrentDirAndPathListEmpty, result.exceptionOrNull())
    }

    @Test
    fun path_newIn_wraps_whichIn() {
        val bin = "$root/a"
        writeExecutable(bin, "rustc")

        val p = Path.newIn("rustc", bin, root).getOrThrow()
        assertEquals("$bin/rustc", p.asPath())
        assertEquals("$bin/rustc", p.intoPathBuf())
    }

    @Test
    fun path_allIn_wraps_whichInAll() {
        val binA = "$root/a"
        val binB = "$root/b"
        writeExecutable(binA, "tool")
        writeExecutable(binB, "tool")

        val all = Path.allIn("tool", "$binA:$binB", root).getOrThrow()
            .asSequence().toList().map { it.asPath() }
        assertEquals(listOf("$binA/tool", "$binB/tool"), all)
    }

    @Test
    fun path_equals_uses_inner_string() {
        val bin = "$root/a"
        writeExecutable(bin, "rustc")
        val p1 = Path.newIn("rustc", bin, root).getOrThrow()
        val p2 = Path.newIn("rustc", bin, root).getOrThrow()
        assertEquals(p1, p2)
        assertEquals(p1.hashCode(), p2.hashCode())
    }

    private fun modeBits(read: Boolean, write: Boolean, exec: Boolean): UShort {
        var m = 0
        if (read) m = m or S_IRUSR or S_IRGRP or S_IROTH
        if (write) m = m or S_IWUSR
        if (exec) m = m or S_IXUSR or S_IXGRP or S_IXOTH
        return m.toUShort()
    }
}
