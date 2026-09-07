# GitHub Access and Build

## Purpose
This repository is designed so GitHub Actions can build the compatibility mod without
GitHub secrets or personal credentials.

## Repository structure
- `src/` — compatibility source/resources
- `libs/` — pinned local CEE/P&W runtime jars required by the build
- `gradlew` / `gradlew.bat` — Gradle 8.13 wrapper
- `.github/workflows/build.yml` — automatic Java 21 build

## GitHub Actions
A push or pull request to `main` runs the build automatically.

The workflow:
1. checks out the repository;
2. installs Temurin Java 21;
3. enables Gradle dependency caching;
4. runs the checked-in Gradle 8.13 wrapper;
5. uploads `build/libs/*.jar` as `ohecompat-build`.

No GitHub token is stored in this repository.

## Local Windows test
From the repository root:

    gradlew.bat clean build

For the client:

    gradlew.bat runClient

## Important access limitation
A GitHub repository being public/private does not itself grant ChatGPT repository access.
ChatGPT must have an active GitHub connection with permission to the repository.
This project contains no mechanism that can grant that permission from inside Minecraft,
Gradle, or GitHub Actions.

Do NOT commit GitHub passwords, personal access tokens, OAuth secrets, or private keys.
Use GitHub's connection/authorization UI for account access.

## Build dependency note
CEE and P&W are supplied as pinned local jars under `libs/`, while Create/Ponder/Flywheel/
Registrate and P&W runtime dependencies are resolved by Gradle. This avoids depending on
GitHub authentication for the Minecraft mod build itself.
