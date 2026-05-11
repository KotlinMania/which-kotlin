// port-lint: ignore
// Finder unit tests driven by FakeSys. These exercise the port's logic
// without depending on platform filesystem APIs; the upstream integration
// tests in tests/basic.rs need a real `RealSys` and a writable tempfile root
// to run, which is left for the actual-typed platform layer.
package io.github.kotlinmania.which

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FinderTest {

    @Test
    fun findsBinaryByNameInPath() {
        val sys = FakeSys(
            files = setOf("/usr/bin/rustc", "/usr/local/bin/cargo"),
            executables = setOf("/usr/bin/rustc", "/usr/local/bin/cargo"),
        )
        val finder = Finder(sys)
        val iter = finder.find("rustc", "/usr/bin:/usr/local/bin", "/cwd", Noop).getOrThrow()
        assertTrue(iter.hasNext())
        assertEquals("/usr/bin/rustc", iter.next())
        assertFalse(iter.hasNext())
    }

    @Test
    fun findsAllMatchesAcrossPaths() {
        val sys = FakeSys(
            files = setOf("/a/python", "/b/python", "/c/python"),
            executables = setOf("/a/python", "/b/python", "/c/python"),
        )
        val finder = Finder(sys)
        val iter = finder.find("python", "/a:/b:/c", "/cwd", Noop).getOrThrow()
        val all = iter.asSequence().toList()
        assertEquals(listOf("/a/python", "/b/python", "/c/python"), all)
    }

    @Test
    fun skipsNonExecutableMatches() {
        val sys = FakeSys(
            files = setOf("/a/foo", "/b/foo"),
            executables = setOf("/b/foo"),
        )
        val finder = Finder(sys)
        val iter = finder.find("foo", "/a:/b", "/cwd", Noop).getOrThrow()
        val all = iter.asSequence().toList()
        assertEquals(listOf("/b/foo"), all)
    }

    @Test
    fun returnsErrorWhenNoPathsAndNoSeparator() {
        val sys = FakeSys()
        val finder = Finder(sys)
        val result = finder.find("foo", null, "/cwd", Noop)
        assertTrue(result.isFailure)
        assertEquals(Error.CannotGetCurrentDirAndPathListEmpty, result.exceptionOrNull())
    }

    @Test
    fun resolvesRelativeNameWithSeparatorAgainstCwd() {
        val sys = FakeSys(
            files = setOf("/cwd/sub/tool"),
            executables = setOf("/cwd/sub/tool"),
        )
        val finder = Finder(sys)
        val iter = finder.find("sub/tool", null, "/cwd", Noop).getOrThrow()
        assertEquals("/cwd/sub/tool", iter.next())
    }

    @Test
    fun tildeExpansionUsesHomeDir() {
        val sys = FakeSys(
            home = "/home/user",
            files = setOf("/home/user/bin/myapp"),
            executables = setOf("/home/user/bin/myapp"),
        )
        val finder = Finder(sys)
        val iter = finder.find("myapp", "~/bin", "/cwd", Noop).getOrThrow()
        assertEquals("/home/user/bin/myapp", iter.next())
    }

    @Test
    fun windowsAppendsPathExtExtensions() {
        val sys = FakeSys(
            isWindows = true,
            pathExtVar = ".COM;.EXE;.CMD",
            files = setOf("C:\\bin\\foo.EXE"),
            executables = setOf("C:\\bin\\foo.EXE"),
        )
        val finder = Finder(sys)
        val iter = finder.find("foo", "C:\\bin", "C:\\cwd", Noop).getOrThrow()
        assertTrue(iter.hasNext())
        val first = iter.next()
        assertTrue(first.endsWith(".EXE"), "expected .EXE suffix, got $first")
    }

    @Test
    fun findRegexMatchesFilenames() {
        val sys = FakeSys(
            files = setOf("/u/python2", "/u/python3", "/u/ruby"),
            executables = setOf("/u/python2", "/u/python3", "/u/ruby"),
        )
        val finder = Finder(sys)
        val iter = finder.findRe(Regex("^python\\d$"), "/u", Noop).getOrThrow()
        val all = iter.asSequence().toList()
        assertEquals(listOf("/u/python2", "/u/python3"), all)
    }
}
