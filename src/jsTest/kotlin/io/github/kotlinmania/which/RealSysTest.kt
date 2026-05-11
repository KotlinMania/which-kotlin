// port-lint: ignore
// Integration tests that drive the Node-backed `RealSys` against a real
// on-disk tempdir created with `fs.mkdtempSync`.
package io.github.kotlinmania.which

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RealSysTest {

    private lateinit var root: String

    @BeforeTest
    fun setup() {
        if (!runsInNode()) return
        root = mkTempRoot()
    }

    @AfterTest
    fun teardown() {
        if (!runsInNode()) return
        rmRecursive(root)
    }

    private fun writeExecutable(dir: String, name: String, executable: Boolean = true): String {
        nodeMkdir(dir)
        val full = "$dir/$name"
        nodeWriteFile(full, "#!/bin/sh\necho $name\n")
        if (executable) {
            nodeChmod(full, 0b111101101)
        } else {
            nodeChmod(full, 0b110100100)
        }
        return full
    }

    @Test
    fun findsBinaryByNameInPath() {
        if (!runsInNode()) return
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
        if (!runsInNode()) return
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
        if (!runsInNode()) return
        val sys = RealSys()
        val result = Finder(sys).find("foo", null, root, Noop)
        assertTrue(result.isFailure)
        assertEquals(Error.CannotGetCurrentDirAndPathListEmpty, result.exceptionOrNull())
    }

    @Test
    fun resolvesRelativeNameWithSeparatorAgainstCwd() {
        if (!runsInNode()) return
        val sub = "$root/work"
        writeExecutable(sub, "tool")
        val sys = RealSys()
        val iter = Finder(sys).find("work/tool", null, root, Noop).getOrThrow()
        assertEquals("$root/work/tool", iter.next())
    }

    @Test
    fun findRegexMatchesFilenames() {
        if (!runsInNode()) return
        val dir = "$root/r"
        writeExecutable(dir, "python2")
        writeExecutable(dir, "python3")
        writeExecutable(dir, "ruby")
        val sys = RealSys()
        val iter = Finder(sys).findRe(Regex("^python\\d$"), dir, Noop).getOrThrow()
        val all = iter.asSequence().toList().sorted()
        assertEquals(listOf("$dir/python2", "$dir/python3"), all)
    }
}

private fun runsInNode(): Boolean =
    js("typeof require !== 'undefined' && typeof process !== 'undefined'").unsafeCast<Boolean>()

private fun mkTempRoot(): String {
    val os: dynamic = js("require('os')")
    val fs: dynamic = js("require('fs')")
    val prefix = "${os.tmpdir()}/which-test-"
    return (fs.mkdtempSync(prefix) as String)
}

private fun rmRecursive(path: String) {
    val fs: dynamic = js("require('fs')")
    fs.rmSync(path, js("({ recursive: true, force: true })"))
}

private fun nodeMkdir(path: String) {
    val fs: dynamic = js("require('fs')")
    fs.mkdirSync(path, js("({ recursive: true })"))
}

private fun nodeWriteFile(path: String, content: String) {
    val fs: dynamic = js("require('fs')")
    fs.writeFileSync(path, content)
}

private fun nodeChmod(path: String, mode: Int) {
    val fs: dynamic = js("require('fs')")
    fs.chmodSync(path, mode)
}
