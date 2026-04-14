# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

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
- Added `PHASE1_PLAN.md` with detailed 3-week implementation plan, reviewed and approved.
