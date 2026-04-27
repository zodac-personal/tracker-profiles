# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build, Test, and Lint Commands

Maven profiles are activated by passing a property flag, not `-P`:

```bash
# Build JAR only (no tests, no lints)
mvn clean install

# Run everything (tests + lints)
mvn clean install -Dall
```

To run a single test class:

```bash
mvn test -Dtests -pl tracker-profiles-screenshots -Dtest=TextSearcherTest
```

## Architecture Overview

### Execution Flow

The application reads a CSV of tracker credentials, then for each tracker: opens a browser, logs in, navigates to the user profile page, redacts
sensitive information, takes a full-page screenshot, and logs out.

Entry point: `ApplicationLauncher` → `TrackerRetriever` (load CSV) → `DisplayValidator` (AWT/display check) → `ScreenshotOrchestrator` →
`ProfileScreenshotExecutor` (per tracker) → `AbstractTrackerHandler` subclass.

### Retry and Screenshot Execution

`ProfileScreenshotExecutor` has three layers:

- **`takeScreenshot`** (package-private) — the entry point called by `ScreenshotOrchestrator`. Handles pre-flight logging and delegates to the loop.
- **`takeScreenshotWithAttempts`** (private) — the retry loop. Calls `isSuccessfullyScreenshot` up to `numberOfTrackerAttempts` times.
    - On any `Exception`, waits 1 second via `BrowserInteractionHelper.explicitWait` then retries.
- **`isSuccessfullyScreenshot`** (private) — a single attempt. Contains all exception handling with per-exception log messages.
    - Returns `true` on success
    - Returns `false` on any handled failure.
        - Exceptions that should propagate to trigger a retry are left uncaught (they bubble up to the `catch (Exception e)` in the retry loop).

### Tracker Handler Pattern

All tracker implementations live in `handler/` and extend `AbstractTrackerHandler`. The base class implements the full screenshot workflow as a
template method; subclasses override selectors and optional hooks.

Handlers are discovered at startup via reflection: `TrackerHandlerFactory` scans the `net.zodac.tracker.handler` package for classes annotated with
`@TrackerHandler`, then instantiates them on demand by name match.

To add a new tracker:

1. Create a class in `handler/` extending `AbstractTrackerHandler`
2. Annotate with `@TrackerHandler(name = "...", type = TrackerType.HEADLESS, url = "https://...")`
3. Override the required selectors — at minimum: `usernameFieldSelector()`, `passwordFieldSelector()`,
   `logoutButtonSelector()`, `profileLinkSelector()`
4. Override optional hooks for redaction (`ipAddressElements()`, `emailElements()`, `torrentPasskeyElements()`,
   `ircPasskeyElements()`), implement fixed element removal interfaces (`HasFixedHeader`, `HasFixedSidebar`),
   manual interaction (`preLoginClickAction()`), etc.
5. Add the tracker's credentials to `docker/trackers_example.csv` and details to `README.md`

Use `XpathBuilder` (with `HtmlElement` constants and `XpathAttributePredicate`) to construct selectors rather than raw XPath strings.

### Tracker Types

Two types control how the browser is created and whether user interaction is needed:

- **`HEADLESS`** — Runs Chrome without a UI. Default for most trackers.
- **`MANUAL`** — Runs Chrome with a UI; pauses execution via `DisplayUtils.userInputConfirmation()` for captcha or 2FA entry.
    - Override `preLoginClickAction()` or `postLoginClickAction()` in the handler.

Default execution order: `HEADLESS` → `MANUAL`. Configurable via `TRACKER_EXECUTION_ORDER` env var.

### Counting Tracker Handlers

Run `grep -R --include='*.java' '^@TrackerHandler(' . | cut -d '"' -f2 | sort -u | wc -l`

### Configuration

All runtime configuration is loaded from environment variables in `ApplicationConfiguration`. `Configuration` is a lazy singleton that calls
`ApplicationConfiguration.load()` once. Any invalid value throws `IllegalArgumentException` at startup.

New env vars must be:

1. Added as a record component (alphabetical order among components)
2. Loaded via a `getXxx()` private static method using `getOrDefault()` or
   `parseIntegerInRange(input, envVarName, max)` (min is hardcoded to 1; use `Integer.MAX_VALUE` for unbounded positive integers)
3. Added to the `load()` constructor call and `print()` debug log
4. Documented in the README `### Configuration Options` table and both Docker run examples (Debian and Windows blocks), all in alphabetical order

### Redaction

If `RedactionType` is not `NONE`, `AbstractTrackerHandler.redactElements()` finds elements by the selectors returned from `ipAddressElements()`,
`emailElements()`, `torrentPasskeyElements()`, `ircPasskeyElements()`, and `sensitiveElements()`, then applies the configured `RedactionType`
(`TEXT` replacement, `BOX` overlay, Gaussian `BLUR`, fully `REMOVE`). Redaction happens after profile page load, before the screenshot.

## Logging

### Log Levels

Log4j2 is used throughout. Apply levels as follows:

- **`ERROR`** — any error that ends execution entirely
- **`WARN`** — unexpected error that allows the next tracker to proceed
- **`INFO`** — user-facing status updates on overall execution progress
- **`DEBUG`** — detail on each discrete step (clicking buttons, filling fields, navigating pages)
- **`TRACE`** — fine-grained detail on waits, timeouts, and low-level state changes

### Exclusions

`logging.properties` is used to suppress irrelevant Selenium log entries.

### Progress Bar

The progress bar is managed by `ProgressBarManager` and kept pinned to the bottom of the console by `ProgressBarPrintStream`, which replaces
`System.out` at startup and intercepts every write. Any new code that produces console output must go through the standard logger — writing directly
to `System.out` or `System.err` will corrupt the bar rendering.

The Clique bar is driven by exactly multiple ticks per tracker (see `TrackerStep` for values). A separate tracker counter (`X/Y`) is appended by
`ProgressBarManager.getProgressBarContent()`. Call `tick()` at each of the tracker steps inside `ProfileScreenshotExecutor`, and `tickTracker()` in
`ScreenshotOrchestrator` after each tracker completes. The `:progress`/`:total` Clique tokens reflect raw tick counts, not tracker counts — users
should rely on the appended `X/Y` suffix for tracker-level progress.

The screenshot phase is always one tick regardless of how many redaction types are configured — this is intentional, because
`hasSensitiveInformation()` is determined at runtime per handler, making the actual screenshot count unknowable upfront.

## Coding Standards

### String Formatting

Always use `"".formatted()` — never `String.format()`:

```java
// Correct
"Hello, %s!".formatted(name)

// Wrong
String.format("Hello, %s!", name)
```

For multi-line strings, use a text block with `.formatted()`. Do **not** use a text block for a single-line string —
SpotBugs flags the trailing `\n` as `VA_FORMAT_STRING_USES_NEWLINE` when `.formatted()` is used:

```java
// Multi-line — text block is correct
"""
<div class="%s">
  <span>%s</span>
</div>
""".formatted(cssClass, content)

// Single-line — plain string, not a text block
"<div class=\"%s\">%s</div>".formatted(cssClass, content)
```

### Instantiation: Private Constructor + Static Factory

Classes that are explicitly instantiated must use a **private constructor** and a **`static` factory method** instead of a public constructor. This
applies to all non-abstract, non-utility classes, unless a private constructor is impossible (e.g., the canonical constructor of a public `record`
cannot be private).

The private constructor must only assign fields — no validation, no logic. All input validation and exceptions belong in the static factory method.
Other static factories (e.g. `ofDefault()`) must call the validating factory, not the private constructor directly.

```java
// Correct
public final class TrackerCredential {

    private final String username;
    private final String password;

    private TrackerCredential(final String username, final String password) {
        this.username = username;  // assignments only
        this.password = password;
    }

    public static TrackerCredential of(final String username, final String password) {
        if (username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        return new TrackerCredential(username, password);  // new only called here
    }

    public static TrackerCredential ofDefault() {
        return of("default", "");  // delegates to of(), not new TrackerCredential()
    }
}

// Wrong — public constructor, and validation in constructor
public final class TrackerCredential {
    public TrackerCredential(final String username, final String password) {
        if (username.isBlank()) { throw new IllegalArgumentException(...); }
        ...
    }
}
```

Name the factory method `of(...)` for value-type objects. Use a more descriptive verb (`create`, `from`, `forTracker`, etc.) when the method does
non-trivial work beyond field assignment.

### Linting

Linter configs live in `ci/`:

- `ci/java/checkstyle.xml` and `checkstyle-suppression.xml`
- `ci/java/pmd-ruleset.xml`
- `ci/java/spotbugs-include/exclude-filter-file.xml`
- `ci/docker/.hadolint.yaml`

Suppress only when genuinely necessary. The codebase has very few suppressions — keep it that way.

Markdown files follow `ci/doc/.markdownlint.json`: body lines max 150 characters, heading lines max 120 characters, code block lines max 200
characters (tables are exempt). Always run the IDE formatter (Ctrl+Shift+F) on any `.md` file after editing.
