---
name: Orchestrator
description: Coordinates the login, profile, and redactor agents to produce a complete TrackerHandler implementation
type: project
---

# Orchestrator Agent

You are the orchestration agent for a new `TrackerHandler` implementation. Your job is to coordinate three
specialist agents, assemble their findings into a complete Java handler class, and complete the post-implementation
checklist.

## Workflow

### Step 1 — Determine base class

Before spawning agents, inspect existing handlers to determine whether this tracker uses a known platform:

- **UNIT3D** → extend `Unit3dHandler`
- **Gazelle** → extend `GazelleHandler`
- **NexusPHP** → extend `NexusPhpHandler`
- **Luminance** → extend `LuminanceHandler`
- **TorrentPier** → extend `TorrentPier`
- **Unknown** → extend `AbstractTrackerHandler`

Read a similar existing handler (same base class) as a structural reference before writing anything.

### Step 2 — Spawn specialist agents sequentially

Spawn each agent with its context file path and the tracker name/URL. Collect findings before proceeding.

1. **Login agent** → `.claude/agents/login_agent.md`
2. **Profile agent** → `.claude/agents/profile_agent.md`
3. **Redactor agent** → `.claude/agents/redactor_agent.md`

### Step 3 — Write the handler class

Write the final `.java` file to:
`tracker-profiles-screenshots/src/main/java/net/zodac/tracker/handler/<TrackerName>.java`

Rules:
- BSD Zero Clause License header (copy from any existing handler)
- Package: `net.zodac.tracker.handler`
- Annotation: `@TrackerHandler(name = "Name", url = "https://...")`
  - For multiple fallback URLs: `url = {"https://url1", "https://url2"}`
  - For MANUAL type: add `type = TrackerType.MANUAL`
- Only include overrides that differ from the base class defaults
- Use `XpathBuilder` exclusively — never raw XPath strings
- Imports must be sorted (static first, then standard, alphabetical within each group)
- Do not add comments unless the logic is genuinely non-obvious

### Step 4 — Complete the checklist

After writing the handler:

1. Add a CSV entry to `docker/trackers_example.csv` (alphabetical order)
2. Add a row to the tracker table in `README.md` (alphabetical order)
3. Increment the tracker count on the "There are currently **N** supported trackers" line in `README.md`
4. Run tests and lints

## Key defaults (only override if different)

| Method                    | Default                                                                                       |
|---------------------------|-----------------------------------------------------------------------------------------------|
| `usernameFieldSelector()` | `By.id("username")`                                                                           |
| `passwordFieldSelector()` | `By.id("password")`                                                                           |
| `loginButtonSelector()`   | `By.id("login-button")`                                                                       |
| `postLoginSelector()`     | delegates to `profilePageSelector()` — **must override** if profile selector has side effects |
| `profilePageSelector()`   | `<a class="username">` — almost always needs overriding                                       |
