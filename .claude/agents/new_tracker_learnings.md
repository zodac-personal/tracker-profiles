# New Tracker Handler Learnings

This file captures mistakes and discoveries made while building new `TrackerHandler` implementations,
to avoid repeating them across sessions.

---

## Key Patterns (from codebase review)

### Minimal required overrides (extending `AbstractTrackerHandler` directly)
- `usernameFieldSelector()` — defaults to `By.id("username")`; override only if different
- `passwordFieldSelector()` — defaults to `By.id("password")`; override only if different
- `loginButtonSelector()` — defaults to `By.id("login-button")`; override only if different
- `postLoginSelector()` — defaults to `profileLinkSelector()`; **must override** if `profileLinkSelector()`
  performs any action (e.g. opening a dropdown) — provide a simple presence-only selector instead
- `profileLinkSelector()` — **required**, no default; element clicked to navigate to profile page
- `profilePageElementSelector()` — **required**, no default; element that confirms profile page loaded
- `logoutButtonSelector()` — **required**, no default; element clicked to log out

### Common optional overrides
- `loginPageSelector()` — return non-null if the homepage doesn't auto-redirect to login
- `ipAddressElements()` — returns `List.of()` by default; override to redact IP fields
- `emailElements()` — returns `List.of()` by default; override to redact email fields
- `torrentPasskeyElements()` — returns `List.of()` by default; override to redact passkeys
- `sensitiveElements()` — returns `List.of()` by default; override for other sensitive data
- `hasFixedHeader()` / `headerSelector()` — implement `HasFixedHeader` interface if there's a sticky header
- `dismissBanner()` — implement `HasDismissibleElement` interface if there's a cookie/warning banner
- `manualCheckBeforeLoginClick()` / `manualCheckAfterLoginClick()` — for captcha/2FA (MANUAL type)

### Selector construction
Always use `XpathBuilder` — never raw XPath strings. Exceptions: prefer Selenium's built-in `By` methods
when a single stable attribute uniquely identifies the element — they are simpler and faster than an
equivalent XpathBuilder expression. This applies to **all** selectors in **all** methods.

- Use `By.id("stable-id")` when the element has a stable (non-dynamic) `id` attribute.
- Use `By.name("stable-name")` when the element has a stable `name` attribute and no stable `id`.
- Use `By.className("unique-class")` when the element has a **single, unique** CSS class and no stable
  `id` or `name`. If the class appears on multiple elements on the page, fall back to `XpathBuilder`.
- Fall back to `XpathBuilder` when `id`/`name` is dynamic or absent, the class is not unique, or when
  additional predicates (class, type, position) are needed to uniquely identify the element.

```java
// Prefer By.id() when the id is stable (applies everywhere, not just specific methods):
By.id("login-button")
By.id("user-view")

// Prefer By.name() when the name is stable and no id exists:
By.name("username")
By.name("password")

// Prefer By.className() when the class is unique on the page and no id/name exists:
By.className("auth-button")

// Fall back to XpathBuilder when id/name/unique-class is absent or additional predicates are needed:
XpathBuilder.from(input, withName("username"), withType("text")).build();
XpathBuilder.from(button, withType("submit")).build();
```

Available predicates: `withClass`, `withId`, `withName`, `withType`, `withAttribute`, `containsHref`,
`atIndex`, `atLastIndex`
Available axes: `followingSibling`, `descendant`, `child`, `parent`, `precedingSibling`, etc.
For non-enum HTML tags: `NamedHtmlElement.of("article")`

### Common base handlers
If the tracker uses a known platform, extend the appropriate base:
- `Unit3dHandler` — UNIT3D platform; handles login, dropdown nav, cookie banners, fixed header
- `GazelleHandler` — Gazelle platform
- `NexusPhpHandler` — NexusPHP platform
- `LuminanceHandler` — Luminance platform
- `TorrentPier` — TorrentPier platform
Check existing handlers before writing from scratch.

### TrackerType
- `HEADLESS` (default, omit `type =` in annotation) — no UI needed
- `MANUAL` — requires browser UI for captcha/2FA
Annotation: `@TrackerHandler(name = "Name", url = "https://...")`
For multiple fallback URLs: `url = {"https://url1", "https://url2"}`

### Tracker display name vs Java class name
The Java class name follows PascalCase (e.g. `AsianDvdClub`), but the `name =` value in
`@TrackerHandler`, the CSV entry, and the README row must use the tracker's **actual display name**
(e.g. `AsianDVDClub`). Check the site's title or logo for the canonical capitalisation before writing.

### CSV entry
Add to `docker/trackers_example.csv` after implementing. Use the tracker's display name, not the Java
class name casing.

### README entry
Add to `README.md` after implementing: insert a row in the correct alphabetical position in the tracker
table, and increment the tracker count on the "There are currently **N** supported trackers" line. Use
the tracker's display name, not the Java class name casing.

---

## Mistakes / Learnings from Implementation Sessions

### 1. `profilePageElementSelector()` must be unique to the profile page

**Mistake:** Returned `XpathBuilder.from(main).build()` (i.e. `<main>`) as a placeholder.

**Why it's wrong:** `<main>` is present on every page of the site, so it does not confirm the profile page
has loaded. The selector must match an element that only exists on the profile page (e.g. a profile-specific
section, heading, or container).

**Rule:** Always ask "is this element unique to the profile page?" before using it as `profilePageElementSelector()`.
Additionally, prefer **non-user-configurable** elements (username display, stats container, join date) over
user-configurable ones (profile avatar, banner image). A user who has not set an avatar may have no avatar
element, so that selector would fail for them. Platform-rendered elements are always present regardless of
user settings.

---

### 2. Always override `profileLinkSelector()` — the default is rarely correct

**Mistake:** Left `profileLinkSelector()` as the inherited default (`<a class="username">`), which did not
match the site's actual nav structure.

**Why it's wrong:** The default assumes a simple clickable anchor with class `username`. Many modern sites
(especially React/Next.js SPAs) use a dropdown menu: a parent element (often `<div>`) is clicked to open
the menu, and then the profile link is the first `<a>` inside it.

**Rule:** Always inspect the nav HTML and override `profileLinkSelector()` explicitly. If a dropdown must be
opened first, call a private `openUserDropdownMenu()` helper (see `MooKo`, `C411`, `Zappateers`) that clicks
the parent element, then return the selector for the link inside the opened dropdown.

**Dropdown pattern:**
```java
@Override
protected By profileLinkSelector() {
    openUserDropdownMenu();
    return XpathBuilder
        .from(div, withClass("dropdown-menu"))
        .child(a, atIndex(1))
        .build();
}

private void openUserDropdownMenu() {
    LOGGER.debug("\t\t- Clicking user dropdown menu to make profile/logout button interactable");
    final By usernameDropdownSelector = XpathBuilder
        .from(div, withClass("username"))
        .build();
    final WebElement usernameDropdown = driver.findElement(usernameDropdownSelector);
    clickButton(usernameDropdown);
}
```

The same `openUserDropdownMenu()` call must be repeated in `logoutButtonSelector()`. Use `atLastIndex()` for
the logout link when it is the last item in the dropdown, or use the specific `atIndex(n)` if known.

**`onmouseover` vs `onclick` dropdowns:** Some dropdown triggers use `onmouseover` instead of `onclick`
to show the menu. For these, **do not call `clickButton()`** — clicking will also follow the element's
`href`, navigating away from the page before the logout link can be clicked. Instead, use
`browserInteractionHelper.moveTo(element)` to hover over the trigger without clicking:

```java
private void openUserDropdownMenu() {
    LOGGER.debug("\t\t- Hovering over dropdown trigger to make logout button interactable");
    final By triggerSelector = By.id("editout");
    final WebElement trigger = driver.findElement(triggerSelector);
    browserInteractionHelper.moveTo(trigger);  // hover only — do NOT call clickButton()
}
```

To tell them apart: if the element has `onmouseover="showMenu(...)"` and a non-`#` `href`, use `moveTo`.
If it has `onclick` or `href="#"`, use `clickButton`.

---

### 3. Check for a fixed header on every new tracker

**Mistake:** Did not implement `HasFixedHeader` even though the profile page has a sticky header that
obscures part of the screenshot.

**Why it's wrong:** Fixed/sticky headers overlap page content in full-page screenshots, cutting off
information at the top of the visible area.

**Rule:** Always check whether the tracker has a `position: fixed` or `position: sticky` header. If it
does, implement `HasFixedHeader` and provide the correct `headerSelector()`. Use
`XpathBuilder.from(header, atIndex(1)).build()` for a plain `<header>` element; only override
`unfixHeader()` when CSS alone is insufficient (see `Torrenting.java` for a JS-injection example).

---

### 4. Always identify sensitive fields before submitting the implementation

**Mistake:** Did not add `emailElements()` or `torrentPasskeyElements()` overrides despite the profile page
displaying both in plaintext.

**Why it's wrong:** The purpose of the handler is to produce a shareable screenshot; user email and passkey
are sensitive and must be redacted before the screenshot is taken.

**Rule:** Before considering the implementation complete, inspect the user's profile page and identify all
sensitive fields: email, IP address, torrent passkey, API key, or any other personal data. Add the
appropriate redaction override(s) for each.

### 4a. Use semantic HTML attributes to locate sensitive fields — don't guess CSS classes

**Mistake:** Used `withText("@")` on a `<td>` to find the email field, instead of inspecting the HTML for
a more reliable signal.

**Why it's wrong:** Many profile pages render the email in an `<input type="email">` field (even if it is
read-only). The `type="email"` attribute is an unambiguous HTML5 signal that is far more reliable than
pattern-matching on the value content.

**Rule:** Before writing a text- or class-based selector for a sensitive field, check whether the element
has a semantic `type` attribute (`type="email"`, `type="password"`, etc.) or a distinctive `name`/`id`.
These are always preferable to guessing class names or matching on value content.

```java
// Prefer this:
XpathBuilder.from(input, withType("email")).build()

// Over this:
XpathBuilder.from(td, withText("@")).build()
```

---

### 4b. Prefer DOM structure over CSS styling attributes when locating sensitive fields

**Mistake:** Used `withAttribute("align", "left")` to find an IP address cell, relying on a CSS
presentation attribute to identify the element.

**Why it's wrong:** CSS styling attributes (`align`, `style`, etc.) are fragile — they can change when
the site is restyled without any change to the underlying data. The element's position within the DOM
table structure is a more stable signal.

**Rule:** When a sensitive field has no semantic `id`, `name`, or `type`, navigate to it via table
structure (`table[n]/tbody[n]/tr[n]/td[n]`) anchored on a stable parent container, rather than relying
on CSS/styling attributes.

```java
// Prefer this — anchored on a stable container, uses DOM position:
XpathBuilder
    .from(td, withClass("embedded"))
    .child(table, atIndex(1))
    .child(tbody, atIndex(1))
    .child(tr, atIndex(3))
    .child(td, atIndex(2))
    .build()

// Over this — relies on a CSS presentation attribute that may change:
XpathBuilder.from(td, withAttribute("align", "left")).build()
```

---

### 5. Override `postLoginSelector()` whenever `profileLinkSelector()` performs an action

**Mistake:** Left `postLoginSelector()` delegating to `profileLinkSelector()`, which calls
`openUserDropdownMenu()` before returning the selector.

**Why it's wrong:** `postLoginSelector()` is called immediately after the login click, purely to confirm
the login succeeded. The default implementation calls `profileLinkSelector()`, which is fine for simple
link selectors — but if `profileLinkSelector()` opens a dropdown (or does any other interaction), that
action fires at the wrong time, against the wrong page state.

**Rule:** Any time `profileLinkSelector()` contains a side effect (clicking, hovering, scrolling), override
`postLoginSelector()` with a plain presence-only selector. Prefer a user-stats element (e.g. `div.user-stats`,
`ul.top-nav__ratio-bar`) over a generic nav element — the post-login page often shows the user's stats and
this provides a more meaningful confirmation of a successful login.

```java
// profileLinkSelector() clicks a dropdown — must not be reused for post-login check
@Override
protected By postLoginSelector() {
    return XpathBuilder
        .from(div, withClass("user-stats"))
        .build();
}
```

---

### 6. Always check the homepage for `loginPageSelector()` — don't assume direct navigation to `/login`

**Mistake:** Verified that `/login` renders a login form and concluded `loginPageSelector()` was not needed,
without checking whether the homepage itself redirects to login or requires clicking a link.

**Why it's wrong:** The framework always navigates to the tracker's root URL first. If the homepage shows a
landing page (e.g. "Please login to view torrents") rather than auto-redirecting to the login form,
`loginPageSelector()` must return the selector for the login link to click. The login form being available
at `/login` does not mean the homepage redirects there.

**Rule:** Always fetch the tracker's **homepage** (not `/login`) to determine the login flow. If it renders
the login form directly → `return null`. If it renders a landing page with a login link → return the
selector for that link. Prefer a nav-bar login anchor over inline paragraph links.

```java
// Homepage shows a nav-bar "Login" link — return its selector.
// Use containsHref() rather than withAttribute("href", ...) for href-based matching:
@Override
public By loginPageSelector() {
    return XpathBuilder
        .from(a, withClass("nav-link"), containsHref("/login"))
        .build();
}
```

---

### 7. Prefer semantic, user-focused elements for `profilePageElementSelector()`

**Mistake:** Used `div.col-lg-4` (a Bootstrap grid class) as `profilePageElementSelector()`, relying on
CSS layout structure to confirm the profile page.

**Why it's wrong:** Bootstrap grid classes (`col-lg-4`, `card-body`, `container`) appear on many pages
with different layouts. They confirm a layout, not a specific page. If the tracker reskins or adds
new pages with the same grid structure, the selector will produce false positives.

**Rule:** Look for an element whose class name describes the content concept, not its visual position.
Username displays, profile headings, or user-stats containers are ideal — their class names are tied
to the feature, not the layout.

```java
// Prefer this — class name describes the content:
XpathBuilder.from(span, withClass("username-user")).build()

// Over this — class name describes the layout:
XpathBuilder.from(div, withClass("col-lg-4")).child(div, atIndex(2)).child(div, withClass("card-header")).build()
```

---

### 8. Never use `withText()` or `withExactText()` in selectors

**Mistake:** Used `withText("Login")` to identify the login dropdown toggle button.

**Why it's wrong:** Text content is language-dependent. Non-English users will see "Connexion", "Anmelden",
etc. — the selector silently fails for them.

**Rule:** Always use structural HTML attributes (`id`, `name`, `class`, `type`, `href`, `action`, etc.) or
DOM position. If an element has no distinguishing attribute, navigate to it from a nearby element that does:

```java
// WRONG — breaks for non-English users:
XpathBuilder.from(button, withClass("dropdown-toggle"), withText("Login")).build()

// CORRECT — navigate from the login form's preceding sibling button:
XpathBuilder
    .from(form, withAttribute("action", "index.php?page=login"))
    .navigateTo(precedingSibling(button))
    .build()
```

---

### 9. Be consistent when selecting `<li>` children of the same parent element

**Mistake:** Used `withClass("logoutlink")` to select the logout `<li>` inside `ul.isuser`, while
`profileLinkSelector()` used `atIndex(3)` for the profile `<li>` in the same list.

**Why it's wrong:** When two selectors (`profileLinkSelector()` and `logoutButtonSelector()`) both navigate
into the same parent element, mixing `withClass` and `atIndex` for their respective `<li>` children is
inconsistent. It makes it harder to reason about their positions relative to each other and to verify
correctness at a glance.

**Rule:** When `profileLinkSelector()` and `logoutButtonSelector()` (or any two selectors) both descend
into the same parent element to select sibling `<li>` items, use the same predicate type for both —
either `atIndex` for both, or `withClass` for both. It is fine to use `withClass` on one and `atIndex`
on another only when they target entirely different parent elements.

```java
// ul.isuser: index 1 = Logout, index 2 = Settings, index 3 = My Profile

// CORRECT — both use atIndex, consistent within the same parent:
@Override
protected By profileLinkSelector() {
    return XpathBuilder.from(ul, withClass("isuser")).child(li, atIndex(3)).child(a, atIndex(1)).build();
}

@Override
protected By logoutButtonSelector() {
    return XpathBuilder.from(ul, withClass("isuser")).child(li, atIndex(1)).child(a, atIndex(1)).build();
}

// ALSO CORRECT — both use withClass, consistent within the same parent:
// profileLinkSelector:  .child(li, withClass("profilelink")).child(a, atIndex(1))
// logoutButtonSelector: .child(li, withClass("logoutlink")).child(a, atIndex(1))

// WRONG — inconsistent within the same parent:
// profileLinkSelector:  .child(li, atIndex(3)).child(a, atIndex(1))
// logoutButtonSelector: .child(li, withClass("logoutlink")).child(a, atIndex(1))
```

---

### 10. Always specify `atIndex(1)` on terminal `<a>` children — never use bare `.child(a)`

**Mistake:** Used `.child(a)` (no index) as the final step in a selector chain, e.g. to get the link
inside a `<li>`.

**Why it's wrong:** `.child(a)` without an index matches *all* `<a>` children, which can be ambiguous if
the `<li>` ever contains more than one anchor (e.g. an icon link alongside a text link). Being explicit
prevents silent breakage if the DOM gains extra elements.

**Rule:** Always write `.child(a, atIndex(1))` (or the appropriate index) when targeting a specific anchor.
This applies to the final `<a>` in any selector chain, not just logout/profile links.

```java
// CORRECT:
XpathBuilder.from(ul, withClass("isuser")).child(li, atIndex(3)).child(a, atIndex(1)).build()

// WRONG — ambiguous if <li> contains multiple anchors:
XpathBuilder.from(ul, withClass("isuser")).child(li, atIndex(3)).child(a).build()
```

---

### 12. Use curl with a cookie jar to access authenticated profile pages

**Mistake:** Attempted to use `WebFetch` to access the authenticated profile page to inspect IP address,
email, and passkey fields — and when it returned the login page, spawned progressively more speculative
agents searching GitHub, Gitee, and Wayback Machine for the page's HTML structure.

**Why it's wrong:** `WebFetch` cannot authenticate. Any page that requires login will return the login
page instead. The correct approach is to follow the login → profile → logout flow as a real user would,
using `curl` with a cookie jar to maintain the session:

```bash
# 1. Fetch the homepage — if cookies from a previous step already have a valid session, you may
#    already be logged in (check for username in the response)
curl -s -c /tmp/cookies.txt -b /tmp/cookies.txt "https://tracker.site/" \
  -H "User-Agent: Mozilla/5.0 ..." -o /tmp/home.html
grep -i "username\|logout" /tmp/home.html | head -10

# 2. If not logged in, POST to the login endpoint with the form fields
curl -s -c /tmp/cookies.txt -b /tmp/cookies.txt "https://tracker.site/login.php" \
  --data "username=USER&password=PASS&..." -o /tmp/post_login.html

# 3. Fetch the profile page with the established session
curl -s -c /tmp/cookies.txt -b /tmp/cookies.txt \
  "https://tracker.site/profile.php?uid=X" -o /tmp/profile.html

# 4. Inspect the HTML for sensitive fields
grep -i "ip\|email\|passkey\|key" /tmp/profile.html
sed -n '100,150p' /tmp/profile.html
```

**Rule:** Before giving up on finding sensitive fields, always attempt to access the authenticated
profile page via `curl`. Do not fall back to source-code archaeology (GitHub, Gitee, Wayback Machine)
unless the `curl` approach fails. If `curl` also fails (e.g. CAPTCHA, JS-only auth), escalate to the
user with a clear description of what was tried and what additional information is needed.

---

### 11. Always check the logout link for a JavaScript confirmation alert

**Mistake:** Did not add `additionalActionAfterLogoutClick()` despite the logout anchor triggering a
JavaScript `confirm()` alert before completing the logout.

**Why it's wrong:** If the JS alert is not accepted, the logout never completes, leaving the session active
and causing the framework to fail when it checks for the post-logout state.

**Rule:** Inspect the logout anchor's `onclick` attribute (or nearby `<script>` blocks) for a `confirm(`
call. If present, override `additionalActionAfterLogoutClick()` to accept the alert. Some platforms
(vBulletin, TS Special Edition) use this pattern consistently — always check the logout link HTML even
when the site otherwise appears straightforward.

```java
// Logout anchor has onclick="return confirm('...')" — must accept the alert:
@Override
protected void additionalActionAfterLogoutClick() {
    LOGGER.debug("\t\t- Clicking JavaScript alert to confirm logout");
    browserInteractionHelper.acceptAlert();
}
```

---

### 13. Always add `{@inheritDoc}` Javadoc to Duration method overrides explaining why

**Mistake:** Overrode `pageTransitionsDuration()` with a longer value but left no explanation of why.

**Why it's wrong:** The default is 500ms. A reader seeing `Duration.ofSeconds(3L)` with no comment has no
way to know whether it was a rough guess or a measured requirement.

**Rule:** Any override of `pageTransitionsDuration()` (or `pageLoadDuration()`) must include a Javadoc
block starting with `{@inheritDoc}` followed by a site-specific explanation of what causes the delay:

```java
/**
 * {@inheritDoc}
 *
 * <p>
 * For {@link MyTracker}, after clicking logout the server issues a JavaScript {@code setTimeout}
 * redirect with a 2-second delay before reaching the login page. The default 500ms is
 * insufficient for this redirect chain.
 */
@Override
public Duration pageTransitionsDuration() {
    return Duration.ofSeconds(3L);
}
```
