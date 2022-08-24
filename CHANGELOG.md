# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## v0.2.4 (unreleased) - 2022-08-22
### Changed
- Support IntelliJ from 2021.3 to 2022.2.

## v0.2.3 - 2022-06-06
### Added
- Sort the modules for deployment based on their dependencies.
- Use ProGuard to optimize distribution size.

### Changed
- Support IntelliJ from 2021.3 to 2022.1.
- Remove deprecated API calls.
- Update Gradle to version 7.4.2.
- Update gradle-intellij-plugin to version 1.6.0.

## [v0.2.2] - 2022-04-12
### Changed
- Support IntelliJ 2021.3.

### Fixed
- Rule Engine classes not found, because the jars is inside a container jar, we need to extract the file "ch.ivyteam.ivy.rule.engine.libs" manually. Only for Axon.ivy Engine 7.0.
- Index of out bound because we assume all modules have at least one content root. It turned out some Gradle modules don't have it.
- Adjusted the pattern to extract Axon.ivy Engine port from console log. The current pattern will work correctly on Axon.ivy Engine 7.0 and 8.0.
- Remaining half number of the global variables / system properties, cause them duplicated in the EngineView.

## [v0.2.1] - 2021-02-03
### Changed
- Only support IntelliJ from 2020 and later.

### Fixed
- Fixed many compatible issues.

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
