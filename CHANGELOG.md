# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased] - 2026-04-22

### Changed
- **`docs/PHASE3_PLAN.md` comprehensively updated** — integrated 10 optimization improvements based on architectural review.
  - `TransactionType` (INCOME/EXPENSE) model change added across the full pipeline (core model → Entity → DAO → use cases).
  - Database schema upgrade strategy simplified: direct Entity modification + `fallbackToDestructiveMigration()` instead of `Migration_2_3` (pre-release phase, no production data).
  - Week 4 scope reduced: bank SMS parsing and Meituan/JD notification support moved to Phase 4 (Appendix A).
  - `BudgetAlertChecker` relocated from `:feature:analytics` to `:feature:capture` to eliminate circular dependency.
  - DAO-level indexed queries added (`searchByKeyword`, `getByCategoryAndDateRange`, `getByMerchantName`, `getTotalByTypeAndDateRange`, `getCategorySummary`).
  - Vico alpha risk mitigated with MPAndroidChart fallback and abstracted chart component interfaces.
  - All placeholder test bodies replaced with full assertions and edge case coverage.
  - Bottom navigation design completed with NavHost routing map (账本/统计/设置).
  - Data consistency strategy defined (Flow for reactive search, suspend for one-shot reports).
  - Migration versions coordinated into single schema update.

## [Unreleased] - 2026-04-21

### Added
- **Phase 2 MVP productization fully implemented** — automatic classification, AI learning loop, keep-alive, onboarding, and privacy compliance.
  - `:feature:classification` — three-layer classification engine (`Exact → Contains → Unclassified`); `ClassificationEngine` with confidence-ordered matching.
  - `:ai` — `LlmClient` interface + `DeepSeekLlmClient` (Ktor + `bodyAsText()` manual parsing); `LlmPrompts` with JSON-only output contract; `LlmResponseParser` with validation.
  - `seed_rules.json` — 140 common merchant-category seed rules covering 9 categories (餐饮, 饮品, 交通, 购物, 娱乐, 日用, 医疗, 教育, 其他); auto-loaded on first launch via `SeedRuleLoader`.
  - `LlmLearningUseCase` — async learning loop: unclassified merchant → quota check → LLM call → `ContainsRule` (confidence capped at 0.9) → backfill historical `category == null` transactions.
  - `DailyQuotaLimiter` — DataStore-based daily quota (default 10 calls/day), auto-resets on calendar day change.
  - `LearningQueue` — in-memory queue for merchants deferred due to quota exhaustion.
  - `KeepAliveService` — foreground service with `specialUse` type and `START_STICKY`; persistent notification "自动记账运行中".
  - `KeepAliveNotificationHelper` — `IMPORTANCE_LOW` notification channel with tap-to-open `MainActivity`.
  - `ServiceAliveChecker` — detects missing notification listener, battery optimization, and foreground service; drives `ServiceStatusBanner` in Compose.
  - Onboarding flow — 3-page Compose guide (Welcome → Notification Permission → Battery Optimization); persisted via DataStore; skips on subsequent launches.
  - Privacy settings — `PrivacySettingsScreen` with policy text, CSV export (UTF-8 BOM for Excel), and clear-all-data with confirmation dialog.
  - `DataExportUseCase` — exports all transactions to timestamped CSV in `Documents/`.
  - `ClearDataUseCase` — wipes transactions, rules, and parse failure logs.
  - `docs/privacy_policy.md` — privacy policy documenting local-only storage, minimal LLM upload (merchant name only), and user rights.
  - `docs/PHASE2_TECH_SPEC.md` — comprehensive Phase 2 technical specification.
- `TransactionDetailViewModel.save()` — auto-generates `ExactRule` (confidence = 1.0, `MatchType.EXACT`) when user changes a transaction's category, giving user feedback highest priority.
- `Migration_1_2` — adds `matchType` column to `category_rules` with default `CONTAINS`.
- `OnboardingPreferences` — DataStore wrapper for onboarding completion state.
- `ic_notification.xml` — vector drawable for foreground service notification.

### Changed
- `PersistCapturedTransactionUseCase` — now injects `ClassificationEngine` and `LlmLearningUseCase`; sets `category` from classification result; triggers async LLM learning for unclassified merchants.
- `app/build.gradle.kts` — added `implementation(project(":feature:classification"))` and `implementation(project(":ai"))`.
- `feature/ledger/build.gradle.kts` — added dependencies on `:feature:capture` and `androidx-datastore-preferences`.
- `feature/classification/build.gradle.kts` — added `androidx-datastore-preferences` and `junit-vintage-engine` for Robolectric tests.
- `settings.gradle.kts` — included `:feature:classification` and `:ai`.
- `appModules` in `AppModule.kt` — added `classificationModule` and `aiModule`.
- `AndroidManifest.xml` — added `FOREGROUND_SERVICE_SPECIAL_USE` permission; declared `KeepAliveService` with `foregroundServiceType="specialUse"`.
- `MainActivity` — integrated onboarding, keep-alive auto-start, and `ServiceStatusBanner` driven by `ServiceAliveChecker` in `onResume()`.
- `CategoryRule` model — added `matchType: MatchType` enum (`EXACT`, `CONTAINS`).
- `CategoryRuleRepository` / `TransactionRepository` — added `count()`, `deleteAll()`, `hasRuleCovering()`, `getUnclassifiedByMerchantKeyword()`, `getAll()`.

### Fixed
- **Ktor MockEngine JSON parsing failures** in `DeepSeekLlmClientTest` — root cause was `body()` ContentNegotiation mismatch with nested escaped JSON. Fixed by switching to `bodyAsText()` + manual `Json.decodeFromString` in production and test code.
- **`koin-android` incompatible with JVM-only `:ai` module** — fixed by adding `koin-core` to `libs.versions.toml` and using it in `:ai/build.gradle.kts`.
- **JUnit 4 Robolectric tests not discovered by JUnit Platform** — fixed by adding `junit-vintage-engine` runtime dependency to `:feature:classification`.
- **`LlmPrompts.SYSTEM` `const val` compilation error** — `trimIndent()` is not compile-time constant; fixed by removing `const`.
- **`const` keyword not applicable to `trimIndent()` result** — same fix as above.
- **`HttpClient` "Fail to prepare request body" in tests** — missing ContentNegotiation plugin for request serialization; fixed by adding `install(ContentNegotiation) { json(...) }` to test `HttpClient` configuration.
- **`:app` compilation failure** — `Unresolved reference` for `classificationModule` and `aiModule` due to missing module dependencies in `app/build.gradle.kts`.
- **`feature:ledger` compilation failure** — `ServiceStatusSnackbar` referenced `:feature:capture` classes but module had no dependency; fixed by adding `implementation(project(":feature:capture"))`.

### Tests
- 48+ unit tests across all modules, all passing (`testDebugUnitTest`).
- New test classes:
  - `:feature:classification` — `ClassificationEngineTest`, `SeedRuleLoaderTest`, `DailyQuotaLimiterTest`, `LlmLearningUseCaseTest`
  - `:ai` — `LlmResponseParserTest`, `DeepSeekLlmClientTest`
  - `:feature:capture` — updated `PersistCapturedTransactionUseCaseTest` with classification and LLM learning triggers
  - `:feature:ledger` — updated `TransactionDetailViewModelTest` with user-rule generation tests; `DataExportUseCaseTest`

## [0.3.0] - 2026-04-19

### Added
- **Phase 1 core engine fully implemented** — all planned modules built from scratch with TDD.
  - `:core:model` — pure Kotlin domain models (`Transaction`, `CategoryRule`, `ParseFailureLog`, `NotificationData`).
  - `:core:database` — Room database (`JizhangDatabase` v1) with DAOs, Flow queries, and Repository implementations mapping Entity ↔ Domain Model.
  - `:feature:parser` — `TransactionParser` migrated from PoC; input changed to `NotificationData`; `isPaymentText` now checks `title + text + bigText`.
  - `:feature:capture` — production `CaptureNotificationService` + `CaptureNotificationHandler` + `NotificationDeduplicator`.
  - `:feature:ledger` — Compose UI with `LedgerReducer` + `StateFlow` pattern; `TransactionListScreen` and `TransactionDetailScreen` with category FilterChips (`FlowRow`).
  - `:app` — new `MainActivity` replacing PoC launcher; conditional rendering between list and detail; Koin DI aggregation.
- `docs/PHASE1_TECH_SPEC.md` — comprehensive technical specification documenting architecture, data flow, testing strategy, issue log (12 items), and Phase 2 extension points.

### Changed
- `AndroidManifest.xml` — replaced PoC `PocNotificationService` and `PocMainActivity` with production `CaptureNotificationService` and `MainActivity`; removed obsolete `PocAccessibilityService` declaration.
- `JizhangApplication` — added `GlobalContext.getOrNull() == null` guard to prevent `KoinAppAlreadyStartedException` during Robolectric multi-test runs.
- `SharingStarted.WhileSubscribed(5000)` → `SharingStarted.Lazily` in `LedgerViewModel` for test stability under `UnconfinedTestDispatcher`.

### Fixed
- **App crash on startup** (`IllegalArgumentException: Service not registered`) caused by stale PoC `PocNotificationService` in manifest. Replaced with clean `CaptureNotificationService` that delegates to `CaptureNotificationHandler` inside a `CoroutineScope`.
- Cross-module null smart-cast failure in `PersistCapturedTransactionUseCase` — fixed by extracting local variables before null checks.
- `NotificationDeduplicator` false-positive when both `text` and `bigText` were `null` — fixed to require non-null equality.
- `TransactionDetailScreen` `FlowRow` experimental API compilation error — added `@OptIn(ExperimentalLayoutApi::class)`.
- Missing `parametersOf` import in `MainActivity` causing unresolved reference.
- Missing `:core:database` dependency in `:feature:capture` build script.
- Missing `koin-android` dependency in `:feature:parser` build script.

### Tests
- 33 unit tests across all modules, all passing (`testDebugUnitTest`).
- New test classes: `CaptureNotificationHandlerTest`, `PersistCapturedTransactionUseCaseTest`, `NotificationDeduplicatorTest`, `LedgerReducerTest`, `LedgerViewModelTest`, `TransactionDetailViewModelTest`, `TransactionParserTest`, `AppModuleTest`.

## [0.2.0] - 2026-04-14

### Added
- `PHASE1_PLAN.md` — detailed 3-week Phase 1 core-engine implementation plan.
  - **Goal:** Transform the Phase 0 PoC into a production-grade modular clean-architecture app.
  - **New modules:** `:core:model`, `:core:database`, `:core:common`, `:feature:capture`, `:feature:parser`, `:feature:ledger`.
  - **Key deliverables:**
    - Room database for `Transaction`, `CategoryRule`, and `ParseFailureLog`.
    - Koin dependency injection across modules.
    - Production `CaptureNotificationService` with deduplication logic.
    - Regex-based `TransactionParser` migrated from PoC.
    - Compose ledger UI (list + detail/edit) using `Reducer + StateFlow`.
    - New `MainActivity` replacing the PoC launcher.
  - **Testing strategy:** JUnit 5 + MockK + Turbine for new business logic; JUnit 4 + Robolectric retained for Room DAO and existing PoC tests.
- `CLAUDE.md` — project architecture, common commands, tech stack, and testing strategy guidelines.

## [0.1.0] - 2026-04-14

### Added
- Phase 0 PoC implementation for automatic bookkeeping on Android.
- `NotificationListenerService` to capture WeChat and Alipay payment notifications.
- `AccessibilityService` as a fallback when notifications are folded or encrypted.
- `TransactionParser` with regex-based extraction for amount and merchant from notification text.
- `AccessibilityExtractor` and `NotificationExtractor` to isolate testable logic from Android framework classes.
- Unit tests covering notification extraction, accessibility parsing, transaction parsing, and file I/O via Robolectric.
- Compose-based launcher UI with shortcuts to system notification and accessibility settings.

### Changed
- Upgraded build toolchain: Gradle 8.11.1, AGP 8.9.1, Kotlin 2.0.21, Compose Compiler plugin.
- Updated `AndroidManifest.xml` with required permissions and service declarations.
- Configured `gradle.properties` with `android.useAndroidX=true` and `android.nonTransitiveRClass=true`.

### Project
- Added `DESIGN.md`, `IMPLEMENTATION.md`, and `PHASE0_PLAN.md` documenting architecture, implementation plan, and Phase 0 details.
