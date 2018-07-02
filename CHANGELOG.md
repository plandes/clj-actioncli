# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).


## [Unreleased]


## [0.0.27] - 2018-03-24
### Changed
- Fixed unit test for Clojure 1.9.0 dep.


## [0.0.26] - 2018-03-23
### Changed
- Fix `resource-path` clojure 1.9 issue
- Add 1.9 profile and fix tests for 1.9.


## [0.0.25] - 2018-02-21
### Added
- Move IO utility library from `clj-ml-model`.

### Changed
- Use zenbuild as submodule.


## [0.0.24] - 2017-12-03
### Changed
- Fixed default arguments not being applied.


## [0.0.23] - 2017-12-01
### Added
- Added namespace macros from old Clojure Contrib library.


## [0.0.22] - 2017-10-27
### Changed
- Bug fixes to prime and locking macros.

### Added
- Pooling function creation macro.


## [0.0.21] - 2017-10-20
### Changed
- Fixed CLI program error reporting format.


## [0.0.20] - 2017-10-17
### Removed
- All `defonce` obviated `def*` (i.e. `defa` macros).

### Added
- Thread resource creation protection functions.

### Changed
- Fix Javadoc warnings.


## [0.0.18] - 2017-09-01
### Added
- Utility namespace with timeout functionality and string truncation brought
  over from tools library.

### Changed
- Better log4j2 config resource handling.
- Better CLI exception handling and configuration.


## [0.0.17] - 2017-06-09
### Added
- Set log level on a per logger basis.
- With/temporary lexical semantics register/resolve path functionality.

### Changed
- Function resource resolve bug fixes with more testing.


## [0.0.16] - 2017-04-27
### Added
- Better CLI parsing error handing.
- Travis build.

### Changed
- Fix no help on startup.
- Open access to reading CLI options.


## [0.0.15] - 2017-01-30
### Added
- Changelog
- Accessible help function and featues (i.e. add usage etc).
- More configurable usage configuration.

### Changed
- Fixing help message printing.
- Readme documentation up to date.


## [0.0.14] - 2017-01-27
### Changed
- Custom help printing handling.
- Better help message printing


## [0.0.13] - 2017-01-20
First major release.

### Added
- Adding single command CLI functionality.
- Added easy help generation for single command CLI option.

### Changed
- Better help message printing


[Unreleased]: https://github.com/plandes/clj-actioncli/compare/v0.0.27...HEAD
[0.0.27]: https://github.com/plandes/clj-actioncli/compare/v0.0.26...v0.0.27
[0.0.26]: https://github.com/plandes/clj-actioncli/compare/v0.0.25...v0.0.26
[0.0.25]: https://github.com/plandes/clj-actioncli/compare/v0.0.24...v0.0.25
[0.0.24]: https://github.com/plandes/clj-actioncli/compare/v0.0.23...v0.0.24
[0.0.23]: https://github.com/plandes/clj-actioncli/compare/v0.0.22...v0.0.23
[0.0.22]: https://github.com/plandes/clj-actioncli/compare/v0.0.21...v0.0.22
[0.0.21]: https://github.com/plandes/clj-actioncli/compare/v0.0.20...v0.0.21
[0.0.20]: https://github.com/plandes/clj-actioncli/compare/v0.0.18...v0.0.20
[0.0.18]: https://github.com/plandes/clj-actioncli/compare/v0.0.17...v0.0.18
[0.0.17]: https://github.com/plandes/clj-actioncli/compare/v0.0.16...v0.0.17
[0.0.16]: https://github.com/plandes/clj-actioncli/compare/v0.0.15...v0.0.16
[0.0.15]: https://github.com/plandes/clj-actioncli/compare/v0.0.14...v0.0.15
[0.0.14]: https://github.com/plandes/clj-actioncli/compare/v0.0.13...v0.0.14
[0.0.13]: https://github.com/plandes/clj-actioncli/compare/v0.0.12...v0.0.13
