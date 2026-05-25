# port-lint Proposed Changes

**Generated:** 2026-05-24
**Source:** tmp/which/src
**Target:** src/commonMain/kotlin/io/github/kotlinmania/which

These are review proposals only. They are emitted when a Rust -> Kotlin pair matches only after fallback normalization, so the existing `port-lint` header is not an exact provenance match.

| Target file | Current header | Proposed header | Source path | Reason |
|-------------|----------------|-----------------|-------------|--------|
| `src/commonMain/kotlin/io/github/kotlinmania/which/Sys.kt` | `// port-lint: source src/sys.rs` | `// port-lint: source sys.rs` | `sys.rs` | `port-lint provenance header matched only after fallback normalization: 'src/sys.rs' vs expected 'sys.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/which/Finder.kt` | `// port-lint: source src/finder.rs` | `// port-lint: source finder.rs` | `finder.rs` | `port-lint provenance header matched only after fallback normalization: 'src/finder.rs' vs expected 'finder.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/which/Noop.kt` | `// port-lint: source src/lib.rs` | `// port-lint: source lib.rs` | `lib.rs` | `port-lint provenance header matched only after fallback normalization: 'src/lib.rs' vs expected 'lib.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/which/CanonicalPath.kt` | `// port-lint: source src/lib.rs` | `// port-lint: source lib.rs` | `lib.rs` | `port-lint provenance header matched only after fallback normalization: 'src/lib.rs' vs expected 'lib.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/which/CwdOption.kt` | `// port-lint: source src/lib.rs` | `// port-lint: source lib.rs` | `lib.rs` | `port-lint provenance header matched only after fallback normalization: 'src/lib.rs' vs expected 'lib.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/which/NonFatalErrorHandler.kt` | `// port-lint: source src/lib.rs` | `// port-lint: source lib.rs` | `lib.rs` | `port-lint provenance header matched only after fallback normalization: 'src/lib.rs' vs expected 'lib.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/which/Path.kt` | `// port-lint: source src/lib.rs` | `// port-lint: source lib.rs` | `lib.rs` | `port-lint provenance header matched only after fallback normalization: 'src/lib.rs' vs expected 'lib.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/which/Which.kt` | `// port-lint: source src/lib.rs` | `// port-lint: source lib.rs` | `lib.rs` | `port-lint provenance header matched only after fallback normalization: 'src/lib.rs' vs expected 'lib.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/which/WhichConfig.kt` | `// port-lint: source src/lib.rs` | `// port-lint: source lib.rs` | `lib.rs` | `port-lint provenance header matched only after fallback normalization: 'src/lib.rs' vs expected 'lib.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/which/Error.kt` | `// port-lint: source src/error.rs` | `// port-lint: source error.rs` | `error.rs` | `port-lint provenance header matched only after fallback normalization: 'src/error.rs' vs expected 'error.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/which/Helper.kt` | `// port-lint: source src/helper.rs` | `// port-lint: source helper.rs` | `helper.rs` | `port-lint provenance header matched only after fallback normalization: 'src/helper.rs' vs expected 'helper.rs'` |
| `src/commonTest/kotlin/io/github/kotlinmania/which/HelperTest.kt` | `// port-lint: source src/helper.rs` | `// port-lint: source helper.rs` | `helper.rs` | `port-lint provenance header matched only after fallback normalization: 'src/helper.rs' vs expected 'helper.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/which/Checker.kt` | `// port-lint: source src/checker.rs` | `// port-lint: source checker.rs` | `checker.rs` | `port-lint provenance header matched only after fallback normalization: 'src/checker.rs' vs expected 'checker.rs'` |
