# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 6/6 (100.0%)
- **Function parity:** 46/66 matched (target 83) — 69.7%
- **Class/type parity:** 17/23 matched (target 27) — 73.9%
- **Combined symbol parity:** 63/89 matched (target 110) — 70.8%
- **Average inline-code cosine:** 0.42 (function body across 6 matched files)
- **Average documentation cosine:** 0.35 (doc text across 6 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 5 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. sys

- **Target:** `which.Sys`
- **Similarity:** 0.08
- **Dependents:** 3
- **Priority Score:** 3162309.2
- **Functions:** 3/17 matched (target 3)
- **Missing functions:** `file_name`, `path`, `is_symlink`, `is_file`, `is_windows`, `current_dir`, `home_dir`, `env_split_paths`, `env_path`, `env_path_ext`, `read_dir`, `metadata`, `symlink_metadata`, `is_valid_executable`
- **Types:** 4/6 matched (target 4)
- **Missing types:** `ReadDirEntry`, `Metadata`

### 2. finder

- **Target:** `which.Finder`
- **Similarity:** 0.51
- **Dependents:** 1
- **Priority Score:** 1021604.9
- **Functions:** 10/10 matched (target 20)
- **Missing functions:** _none_
- **Types:** 4/6 matched (target 4)
- **Missing types:** `PathExt`, `Item`

### 3. lib

- **Target:** `which.Noop`
- **Similarity:** 0.54
- **Dependents:** 0
- **Priority Score:** 73904.6
- **Functions:** 26/31 matched (target 50)
- **Missing functions:** `default`, `fmt`, `deref`, `as_ref`, `eq`
- **Types:** 6/8 matched (target 10)
- **Missing types:** `Regex`, `Target`
- **Lint issues:** 1

### 4. error

- **Target:** `which.Error`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10410.0
- **Functions:** 0/1 matched
- **Missing functions:** `fmt`
- **Types:** 3/3 matched (target 8)
- **Missing types:** _none_

### 5. helper

- **Target:** `which.Helper`
- **Similarity:** 0.56
- **Dependents:** 0
- **Priority Score:** 404.4
- **Functions:** 4/4 matched (target 5)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Tests:** 3/3 matched

### 6. checker

- **Target:** `which.Checker`
- **Similarity:** 0.86
- **Dependents:** 0
- **Priority Score:** 301.4
- **Functions:** 3/3 matched (target 4)
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

