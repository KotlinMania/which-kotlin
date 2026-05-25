# which-kotlin in Kotlin

[![GitHub link](https://img.shields.io/badge/GitHub-KotlinMania%2Fwhich--kotlin-blue.svg)](https://github.com/KotlinMania/which-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/which-kotlin)](https://central.sonatype.com/artifact/io.github.kotlinmania/which-kotlin)
[![Build status](https://img.shields.io/github/actions/workflow/status/KotlinMania/which-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/which-kotlin/actions)

This is a Kotlin Multiplatform line-by-line transliteration port of [`harryfei/which-rs`](https://github.com/harryfei/which-rs.git).

**Original Project:** This port is based on [`harryfei/which-rs`](https://github.com/harryfei/which-rs.git). All design credit and project intent belong to the upstream authors; this repository is a faithful port to Kotlin Multiplatform with no behavioural changes intended.

### Porting status

This is an **in-progress port**. The goal is feature parity with the upstream Rust crate while providing a native Kotlin Multiplatform API. Every Kotlin file carries a `// port-lint: source <path>` header naming its upstream Rust counterpart so the AST-distance tool can track provenance.

---

## Upstream README — `harryfei/which-rs`

> The text below is reproduced and lightly edited from [`https://github.com/harryfei/which-rs.git`](https://github.com/harryfei/which-rs.git). It is the upstream project's own description and remains under the upstream authors' authorship; links have been rewritten to absolute upstream URLs so they continue to resolve from this repository.

[![Build Status](https://github.com/harryfei/which-rs/actions/workflows/rust.yml/badge.svg)](https://github.com/harryfei/which-rs/actions/workflows/rust.yml)
[![Actively Maintained](https://img.shields.io/badge/Maintenance%20Level-Actively%20Maintained-green.svg)](https://gist.github.com/cheerfulstoic/d107229326a01ff0f333a1d3476e068d)

## which

A Rust equivalent of Unix command "which". Locate installed executable in cross platforms.

## Support platforms

* Linux
* Windows
* macOS
* wasm32-wasi*

### A note on WebAssembly

This project aims to support WebAssembly with the [WASI](https://wasi.dev/) extension. All `wasm32-wasi*` targets are officially supported.

If you need to add a conditional dependency on `which` please refer to [the relevant Cargo documentation for platform specific dependencies.](https://doc.rust-lang.org/cargo/reference/specifying-dependencies.html#platform-specific-dependencies)

Here's an example of how to conditionally add `which`. You should tweak this to your needs.

```toml
[target.'cfg(not(all(target_family = "wasm", target_os = "unknown")))'.dependencies]
which = "8.0.0"
```

Note that non-WASI environments have no access to the system. Using this in that situation requires disabling the default features of this crate and providing a custom `which::sys::Sys` implementation to `which::WhichConfig`.

## Examples

1) To find which rustc executable binary is using.

    ``` rust
    use which::which;

    let result = which("rustc").unwrap();
    assert_eq!(result, PathBuf::from("/usr/bin/rustc"));
    ```

2. After enabling the `regex` feature, find all Cargo subcommand executables on the path:

    ``` rust
    use which::which_re;

    which_re(Regex::new("^cargo-.*").unwrap()).unwrap()
        .for_each(|pth| println!("{}", pth.to_string_lossy()));
    ```

## MSRV

This crate currently has an MSRV of Rust 1.70. Increasing the MSRV is considered a breaking change and thus requires a major version bump.

We cannot make any guarantees about the MSRV of our dependencies. You may be required to pin one of our dependencies to a lower version in your own Cargo.toml in order to compile
with the minimum supported Rust version. Eventually Cargo will handle this automatically. See [rust-lang/cargo#9930](https://github.com/rust-lang/cargo/issues/9930) for more.

## Documentation

The documentation is [available online](https://docs.rs/which/).

---

## About this Kotlin port

### Installation

```kotlin
dependencies {
    implementation("io.github.kotlinmania:which-kotlin:0.1.2")
}
```

### Building

```bash
./gradlew build
./gradlew test
```

### Targets

- macOS arm64
- Linux x64
- Windows mingw-x64
- iOS arm64 / simulator-arm64 (Swift export + XCFramework)
- JS (browser + Node.js)
- Wasm-JS (browser + Node.js)
- Android (API 24+)

### Porting guidelines

See [AGENTS.md](AGENTS.md) for translator discipline, port-lint header convention, and Rust -> Kotlin idiom mapping.

### License

This Kotlin port is distributed under the same MIT license as the upstream [`harryfei/which-rs`](https://github.com/harryfei/which-rs.git). See [LICENSE](LICENSE) (and any sibling `LICENSE-*` / `NOTICE` files mirrored from upstream) for the full text.

Original work copyrighted by the which-rs authors.  
Kotlin port: Copyright (c) 2026 Sydney Renee and The Solace Project.

### Acknowledgments

Thanks to the [`harryfei/which-rs`](https://github.com/harryfei/which-rs.git) maintainers and contributors for the original Rust implementation. This port reproduces their work in Kotlin Multiplatform; bug reports about upstream design or behavior should go to the upstream repository.
