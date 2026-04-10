# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build, Test, and Lint Commands

Maven profiles are activated by passing a property flag, not `-P`:

```bash
# Build JAR only (no tests, no lints)
mvn clean install

# Run unit tests only
mvn clean install -Dtests

# Run all linters only (Checkstyle, PMD, SpotBugs, Javadoc, Enforcer, License, SortPom)
mvn clean install -Dlint

# Run everything (tests + lints), skipping the shade JAR
mvn clean install -Dall
```

To run a single test class:

```bash
mvn test -Dtests -pl tracker-profiles-screenshots -Dtest=TextSearcherTest
```

## Architecture Overview

### Execution Flow

The application reads a CSV of tracker credentials, then for each tracker: opens a browser, logs in, navigates to the
user profile page, redacts sensitive information, takes a full-page screenshot, and logs out.

Entry point: `ApplicationLauncher` → `ScreenshotOrchestrator` → `ProfileScreenshotExecutor` (per tracker) →
`AbstractTrackerHandler` subclass.

### Retry and Screenshot Execution

`ProfileScreenshotExecutor` has two layers:

- **`canScreenshotTracker`** (package-private) — the retry loop. Calls `isSuccessfullyScreenshot` up to
  `numberOfTrackerAttempts` times. On any `Exception`, waits 1 second via `BrowserInteractionHelper.explicitWait` then
  retries. Called by `ScreenshotOrchestrator`.
- **`isSuccessfullyScreenshot`** (private) — a single attempt. Contains all exception handling with per-exception log
  messages. Returns `true` on success, `false` on any handled failure. Exceptions that should propagate to trigger a
  retry are left uncaught (they bubble up to the `catch (Exception e)` in the retry loop).

### Tracker Handler Pattern

All 75+ tracker implementations live in `handler/` and extend `AbstractTrackerHandler`. The base class implements the
full screenshot workflow as a template method; subclasses override selectors and optional hooks.

Handlers are discovered at startup via reflection: `TrackerHandlerFactory` scans the `net.zodac.tracker.handler` package
for classes annotated with `@TrackerHandler`, then instantiates them on demand by name match.

To add a new tracker:

1. Create a class in `handler/` extending `AbstractTrackerHandler`
2. Annotate with `@TrackerHandler(name = "...", type = TrackerType.HEADLESS, url = "https://...")`
3. Override the required selectors — at minimum: `usernameFieldSelector()`, `passwordFieldSelector()`,
   `logoutButtonSelector()`, `profileLinkSelector()`
4. Override optional hooks for redaction (`ipAddressElements()`, `emailElements()`, `passkeyElements()`), fixed header
   removal (`hasFixedHeader()`), manual interaction (`manualCheckBeforeLoginClick()`), etc.
5. Add the tracker's credentials to `docker/trackers_example.csv`

Use `XpathBuilder` (with `HtmlElement` constants and `XpathAttributePredicate`) to construct selectors rather than raw
XPath strings.

### Tracker Types

Three types control how the browser is created and whether user interaction is needed:

- **`HEADLESS`** — Runs Chrome without a UI. Default for most trackers.
- **`MANUAL`** — Runs Chrome with a UI; pauses execution via `DisplayUtils.userInputConfirmation()` for captcha or 2FA
  entry. Override `manualCheckBeforeLoginClick()` or `manualCheckAfterLoginClick()` in the handler.

Default execution order: `HEADLESS` → `MANUAL` → `CLOUDFLARE_CHECK`. Configurable via `TRACKER_EXECUTION_ORDER` env var.

### Counting Tracker Handlers

Run `grep -R --include='*.java' '^@TrackerHandler(' . | cut -d '"' -f2 | sort -u | wc -l`

### Configuration

All runtime configuration is loaded from environment variables in `ApplicationConfiguration` (a Java record).
`Configuration` is a lazy singleton that calls `ApplicationConfiguration.load()` once. Any invalid value throws
`IllegalArgumentException` at startup.

New env vars must be:

1. Added as a record component (alphabetical order among components)
2. Loaded via a `getXxx()` private static method using `getOrDefault()` or
   `parseIntegerInRange(input, envVarName, max)` (min is hardcoded to 1; use `Integer.MAX_VALUE` for unbounded positive
   integers)
3. Added to the `load()` constructor call and `print()` debug log
4. Documented in the README `### Configuration Options` table and both Docker run examples (Debian and Windows blocks),
   all in alphabetical order

### Redaction

If `RedactionType` is not `NONE`, `AbstractTrackerHandler.redactElements()` finds elements by the selectors returned
from `ipAddressElements()`, `emailElements()`, `passkeyElements()`, and `sensitiveElements()`, then applies the
configured `RedactionType` (`TEXT` replacement, `BOX` overlay, or Gaussian `BLUR`). Redaction happens after profile page
load, before the screenshot.

### Linting Rules

Linter configs live in `ci/`:

- `ci/java/checkstyle.xml` and `checkstyle-suppression.xml`
- `ci/java/pmd-ruleset.xml`
- `ci/java/spotbugs-include/exclude-filter-file.xml`
- `ci/docker/.hadolint.yaml`

Suppress only when genuinely necessary. The codebase has very few suppressions — keep it that way.

Markdown files have a maximum line length of 120 characters. Always run the IDE formatter (Ctrl+Shift+F) on any `.md`
file after editing.
