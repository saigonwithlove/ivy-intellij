# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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