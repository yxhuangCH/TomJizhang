# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Common Commands

- Build the app: `./gradlew :app:assembleDebug`
- Run all unit tests: `./gradlew testDebugUnitTest`
- Run tests for a single module: `./gradlew :app:testDebugUnitTest` (or `:core:model:test`, `:feature:parser:test`, etc.)
- Run a specific test class: `./gradlew :app:testDebugUnitTest --tests "com.yxhuang.jizhang.poc.parser.TransactionParserTest"`

## Project Architecture

**TomFinancialAccounting** is an Android automatic bookkeeping app (自动记账) that captures WeChat/Alipay payment notifications and logs them as transactions.

### Current State (Phase 0 — PoC)
- Single `:app` module with proof-of-concept code under `com.yxhuang.jizhang.poc`.
- `NotificationListenerService` and `AccessibilityService` capture notifications.
- `TransactionParser` extracts amount/merchant via regex.
- 32 unit tests covering parser, extractors, and file I/O (Robolectric).

### Target State (Phase 1)
Modular clean architecture as detailed in `PHASE1_PLAN.md`:
- `:core:model` — pure Kotlin domain models.
- `:core:database` — Room database, DAOs, repositories, and Koin `DatabaseModule`.
- `:core:common` — shared utilities.
- `:feature:capture` — production `NotificationListenerService`, deduplication, and `PersistCapturedTransactionUseCase`.
- `:feature:parser` — regex-based `TransactionParser` migrated from PoC.
- `:feature:ledger` — Compose UI (list + detail/edit) with `Reducer` + `StateFlow` pattern.

## Tech Stack

- Gradle 8.11.1, AGP 8.9.1, Kotlin 2.0.21, `compileSdk = 34`, `minSdk = 31`.
- Compose (BOM 2024.02.00) with Material3.
- Room 2.6.1, Koin 3.5.6, Kotlinx Coroutines 1.8.1, Kotlinx Serialization 1.6.2.

## Testing Strategy

- **New code** (repositories, use cases, ViewModels, reducers): JUnit 5 (Jupiter) + MockK + Turbine.
- **Room DAO integration tests** and existing PoC tests: JUnit 4 + Robolectric due to `Context` requirement.
- When adding dependencies for new modules, declare versions in `gradle/libs.versions.toml`.

## Key Documents

- `DESIGN.md` — high-level architecture decisions.
- `IMPLEMENTATION.md` — implementation guidelines.
- `PHASE0_PLAN.md` — Phase 0 (PoC) plan.
- `PHASE1_PLAN.md` — detailed 3-week Phase 1 plan with step-by-step implementation order, test examples, and acceptance criteria.
- `CHANGELOG.md` — release notes.

## Git

- Default branch for active development: `develop`.
- Remote: `git@github.com:yxhuangCH/TomJizhang.git`.
