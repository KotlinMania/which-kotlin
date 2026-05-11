// port-lint: source src/sys.rs
package io.github.kotlinmania.which

actual class RealSys actual constructor() : Sys {

    override fun isWindows(): Boolean = nodeProcess().platform.unsafeCast<String>() == "win32"

    override fun currentDir(): Result<String> = runCatching {
        nodeProcess().cwd().unsafeCast<String>()
    }

    override fun homeDir(): String? = try {
        nodeOs().homedir().unsafeCast<String>().takeIf { it.isNotEmpty() }
    } catch (_: Throwable) {
        null
    }

    override fun envSplitPaths(paths: String): List<String> {
        val sep = if (isWindows()) ';' else ':'
        return paths.split(sep).filter { it.isNotEmpty() }
    }

    override fun envPath(): String? {
        val v = nodeProcess().env.PATH
        return if (v == undefined() || v == null) null else v.unsafeCast<String>().takeIf { it.isNotEmpty() }
    }

    override fun envPathExt(): String? {
        val v = nodeProcess().env.PATHEXT
        return if (v == undefined() || v == null) null else v.unsafeCast<String>().takeIf { it.isNotEmpty() }
    }

    override fun metadata(path: String): Result<SysMetadata> = runCatching {
        val stats = nodeFs().statSync(path)
        JsMetadata(
            isRegular = stats.isFile().unsafeCast<Boolean>(),
            isSymlink = false,
        ) as SysMetadata
    }.recoverCatching { e ->
        throw IoError("stat($path): ${e.message ?: e.toString()}", e)
    }

    override fun symlinkMetadata(path: String): Result<SysMetadata> = runCatching {
        val stats = nodeFs().lstatSync(path)
        JsMetadata(
            isRegular = stats.isFile().unsafeCast<Boolean>(),
            isSymlink = stats.isSymbolicLink().unsafeCast<Boolean>(),
        ) as SysMetadata
    }.recoverCatching { e ->
        throw IoError("lstat($path): ${e.message ?: e.toString()}", e)
    }

    override fun readDir(path: String): Result<Iterator<Result<SysReadDirEntry>>> = runCatching {
        val names = nodeFs().readdirSync(path).unsafeCast<Array<String>>()
        names.map { name ->
            Result.success<SysReadDirEntry>(JsDirEntry(name, pathJoin(path, name)))
        }.iterator()
    }.recoverCatching { e ->
        throw IoError("readDir($path): ${e.message ?: e.toString()}", e)
    }

    override fun isValidExecutable(path: String): Result<Boolean> = runCatching {
        val fs = nodeFs()
        val mode = if (isWindows()) {
            fs.constants.F_OK.unsafeCast<Int>()
        } else {
            fs.constants.X_OK.unsafeCast<Int>()
        }
        fs.accessSync(path, mode)
        true
    }.recover { false }
}

// Webpack's static analyzer would otherwise see literal `require('fs')` / `require('os')` and
// pull those modules into the browser bundle, where they're unresolvable — jsBrowserTest fails
// with `Module not found: Error: Can't resolve 'fs'`. Routing through `eval('require')` makes
// the require lookup opaque to webpack's static analyzer (it doesn't trace eval string contents)
// AND preserves Node's lexical scoping so the CommonJS-injected `require` is still visible to
// the eval expression. `new Function('return require')()` would NOT work here: the Function
// constructor runs its body in GLOBAL scope where Node's module-local `require` is invisible,
// and `typeof require` would evaluate to 'undefined', breaking every runtime fs call.
private fun nodeFs(): dynamic = js(
    "(function(){ try { var r = eval('typeof require === \"function\" ? require : null'); return r ? r('fs') : null; } catch (e) { return null; } })()"
)

private fun nodeOs(): dynamic = js(
    "(function(){ try { var r = eval('typeof require === \"function\" ? require : null'); return r ? r('os') : null; } catch (e) { return null; } })()"
)

private fun nodeProcess(): dynamic = js("process")

private fun undefined(): dynamic = js("undefined")

private class JsMetadata(
    private val isRegular: Boolean,
    private val isSymlink: Boolean,
) : SysMetadata {
    override fun isSymlink(): Boolean = isSymlink
    override fun isFile(): Boolean = isRegular
}

private class JsDirEntry(
    private val fileName: String,
    private val fullPath: String,
) : SysReadDirEntry {
    override fun fileName(): String = fileName
    override fun path(): String = fullPath
}
