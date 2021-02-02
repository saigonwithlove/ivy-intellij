# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v0.2.0] - 2021-02-02
### Added
- Support Ivy 8.

### Changed
- Use RxJava to manage State change.
- Implement the new way to manage Ivy Engine, the handling for each Ivy version is separated into different classes.
- Polish InitializationActivity.
- Clean up old Process Models when creating Ivy Engine, because it may not relevant for existing project.

### Fixed
- Cannot restore Server Properties in State.
- Make Ivy Engine directory setting fit the width of Setting Panel.

## [v0.1.12] - 2020-05-03
### Added
- Added Server Properties in Engine View.
- Download and update Ivy Devtool automatically.
- Show different color between default and modified configuration.

### Changed
- Support IntelliJ 2020.1, since v0.1.12 the IntelliJ version part is removed. Ivy Plugin supports IntelliJ 2019.3 and later.
- Use one observable to handle setting changes.
- Internal code clean up.

## [v0.1.11-2019.3] - 2020-04-09
### Changed
- Re-create Ivy's Global Libraries to prevent wrong libraries were created and stay forever.

### Fixed
- Global Variables are not displayed when Ivy Tool Window was not opened at the beginning.
- Deployed module directories were cached lead to wrong deployment detection.
- Ivy Modules's version and dependencies are not get updated when pom.xml had been changed.
- NullPointerException when checking Ivy Devtool exists in InitializationActivity.
- Jar files were not updated in Virtual File System, lead to Global Libraries were not created correctly.

## [v0.1.10-2019.3] - 2020-04-04
### Added
- CHANGELOG.md to keep track changes.
- EngineView with ability to change Global Variable's value.
- Deploy modules which are not exist in Axon.ivy Engine when Axon.ivy Engine started.
- Update modified Global Variables in Axon.ivy Engine when Axon.ivy Engine started (because the old data was lost when Axon.ivy Engine stopped, we need to restore the modified values).

### Changed
- Ignore *.git*, *target*, *node_modules* when deploy module to Axon.ivy Engine.
- Logic/Workflow could be deployed without restart Axon.ivy Engine, with the help of *ivy-devtool-0.2.2*.
- Refine border and size of ModuleView for consistent background in Bright theme.
- Reduce startup time by skipping created libraries.
- Start Engine and Config Actions moved from ModuleView to EngineView.
