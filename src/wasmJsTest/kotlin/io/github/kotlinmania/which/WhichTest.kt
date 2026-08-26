@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.github.kotlinmania.which

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WhichTest {

    private lateinit var root: String

    @BeforeTest
    fun setup() {
        if (!runsInNode()) return
        root = jsMkTempRoot()
    }

    @AfterTest
    fun teardown() {
        if (!runsInNode()) return
        jsRmRecursive(root)
    }

    private fun writeExecutable(dir: String, name: String, executable: Boolean = true): String {
        jsMkdirRecursive(dir)
        val full = "$dir/$name"
        jsWriteFile(full, "#!/bin/sh\necho $name\n")
        jsChmod(full, if (executable) 0b111101101 else 0b110100100)
        return full
    }

    @Test
    fun whichIn_returns_first_executable_in_paths() {
        if (!runsInNode()) return
        val binA = "$root/a"
        val binB = "$root/b"
        writeExecutable(binA, "rustc")
        writeExecutable(binB, "rustc")

        val result = whichIn("rustc", "$binA:$binB", root).getOrThrow()
        assertEquals("$binA/rustc", result)
    }

    @Test
    fun whichInAll_returns_iterator_of_all_matches() {
        if (!runsInNode()) return
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
        if (!runsInNode()) return
        val sub = "$root/work"
        writeExecutable(sub, "tool")

        val global = whichInGlobal("work/tool", sub)
        assertTrue(global.isFailure || global.getOrThrow().asSequence().none())
    }

    @Test
    fun whichReIn_matches_regex_across_paths() {
        if (!runsInNode()) return
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
        if (!runsInNode()) return
        val dir = "$root/empty"
        jsMkdirRecursive(dir)

        val result = whichIn("definitely-not-here-xyz", dir, root)
        assertTrue(result.isFailure)
        assertEquals(Error.CannotFindBinaryPath, result.exceptionOrNull())
    }

    @Test
    fun whichIn_returns_CannotGetCurrentDirAndPathListEmpty_when_paths_null() {
        if (!runsInNode()) return
        val result = whichIn("tool", null, root)
        assertTrue(result.isFailure)
        assertEquals(Error.CannotGetCurrentDirAndPathListEmpty, result.exceptionOrNull())
    }

    @Test
    fun path_newIn_wraps_whichIn() {
        if (!runsInNode()) return
        val bin = "$root/a"
        writeExecutable(bin, "rustc")

        val p = Path.newIn("rustc", bin, root).getOrThrow()
        assertEquals("$bin/rustc", p.asPath())
        assertEquals("$bin/rustc", p.intoPathBuf())
    }

    @Test
    fun path_allIn_wraps_whichInAll() {
        if (!runsInNode()) return
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
        if (!runsInNode()) return
        val bin = "$root/a"
        writeExecutable(bin, "rustc")
        val p1 = Path.newIn("rustc", bin, root).getOrThrow()
        val p2 = Path.newIn("rustc", bin, root).getOrThrow()
        assertEquals(p1, p2)
        assertEquals(p1.hashCode(), p2.hashCode())
    }
}

@JsFun("() => typeof require !== 'undefined' && typeof process !== 'undefined'")
private external fun runsInNode(): Boolean

@JsFun("() => require('fs').mkdtempSync(require('os').tmpdir() + '/which-api-test-')")
private external fun jsMkTempRoot(): String

@JsFun("(p) => require('fs').rmSync(p, { recursive: true, force: true })")
private external fun jsRmRecursive(path: String)

@JsFun("(p) => require('fs').mkdirSync(p, { recursive: true })")
private external fun jsMkdirRecursive(path: String)

@JsFun("(p, c) => require('fs').writeFileSync(p, c)")
private external fun jsWriteFile(path: String, content: String)

@JsFun("(p, m) => require('fs').chmodSync(p, m)")
private external fun jsChmod(path: String, mode: Int)
