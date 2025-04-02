# Contributing to Discord Whitelister

Thank you for considering contributing to Discord Whitelister! This document outlines the process for contributing to the project.

## Code of Conduct

By participating in this project, you agree to abide by our Code of Conduct. Please be respectful and considerate of others.

## How Can I Contribute?

### Reporting Bugs

If you find a bug, please create an issue on GitHub with the following information:
- A clear, descriptive title
- Steps to reproduce the bug
- Expected behavior
- Actual behavior
- Screenshots (if applicable)
- Your environment (OS, Java version, Minecraft version, server platform)

### Suggesting Features

If you have an idea for a new feature, please create an issue on GitHub with the following information:
- A clear, descriptive title
- A detailed description of the feature
- Why this feature would be useful
- Any implementation ideas you have

### Pull Requests

1. Fork the repository
2. Create a new branch for your feature or bug fix
3. Make your changes
4. Run tests to ensure your changes don't break existing functionality
5. Submit a pull request

## Development Setup

1. Clone the repository
2. Run `./gradlew build` to build the project
3. Import the project into your IDE of choice

## Project Structure

- `common/` - Common code shared across all platforms
- `spigot/` - Spigot/Paper plugin implementation
- `bungeecord/` - BungeeCord plugin implementation
- `velocity/` - Velocity plugin implementation
- `fabric/` - Fabric mod implementation
- `forge/` - Forge mod implementation
- `neoforge/` - NeoForge mod implementation

## Coding Guidelines

- Follow Java coding conventions
- Write clear, descriptive commit messages
- Add comments to explain complex logic
- Write unit tests for new functionality

## Release Process

1. Update the version number in `build.gradle`
2. Update the CHANGELOG.md file
3. Create a new tag with the version number (e.g., `v1.0.0`)
4. Push the tag to GitHub
5. GitHub Actions will automatically build and release the new version

## Questions?

If you have any questions, feel free to create an issue on GitHub or reach out to the maintainers.
