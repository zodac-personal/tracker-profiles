## Added new option `LOG_TRACKER_NAME`

Previously execution was logged by prefixing each step with the tracker name then running steps sequentially:

```text
2026-04-23 04:37:15.138 [INFO ] >>> Executing Headless trackers <<<
2026-04-23 04:37:15.150 [INFO ] 
2026-04-23 04:37:16.140 [INFO ] [MooKo]
2026-04-23 04:37:16.163 [INFO ]         - Opening tracker
2026-04-23 04:37:16.164 [INFO ]                 - 'https://mooko.org/'
2026-04-23 04:37:17.553 [INFO ]         - Logging in as 'harmless1357'
2026-04-23 04:37:20.127 [INFO ]         - Opening user profile page
2026-04-23 04:37:23.788 [INFO ]         - Redaction: None
2026-04-23 04:37:23.788 [INFO ]                 - Performing updates to profile page, if needed
2026-04-23 04:37:23.809 [INFO ]                         - Header has been updated to not be fixed
2026-04-23 04:37:26.524 [INFO ]                 - Screenshot saved at: [/app/screenshots/2026-04-23/MooKo.png]
2026-04-23 04:37:27.046 [INFO ]         - Redaction: Blur
2026-04-23 04:37:27.046 [INFO ]                 - Performing updates to profile page, if needed
2026-04-23 04:37:27.078 [INFO ]                         - Header has been updated to not be fixed
2026-04-23 04:37:27.081 [INFO ]                 - Redacting elements with sensitive information
2026-04-23 04:37:27.266 [INFO ]                         - Found IP Address: 'We last checked this clients connectability 8 minutes ago, using '192.0.2.1:8080'.' in <span>
2026-04-23 04:37:27.283 [INFO ]                 - Redacted the text of 1 element
2026-04-23 04:37:29.926 [INFO ]                 - Screenshot saved at: [/app/screenshots/2026-04-23/MooKo_Blur.png]
2026-04-23 04:37:31.580 [INFO ]         - Logged out

```

Now this new option (enabled by default) will prefix each entry with the currently executing tracker:

```text
2026-04-23 04:39:34.659 [INFO ] >>> Executing Headless trackers <<<
2026-04-23 04:39:34.660 [INFO ] 
2026-04-23 04:39:35.630 [INFO ] [MooKo]       - Opening tracker
2026-04-23 04:39:35.632 [INFO ] [MooKo]               - 'https://mooko.org/'
2026-04-23 04:39:36.957 [INFO ] [MooKo]       - Logging in as 'harmless1357'
2026-04-23 04:39:39.544 [INFO ] [MooKo]       - Opening user profile page
2026-04-23 04:39:47.135 [INFO ] [MooKo]       - Redaction: None
2026-04-23 04:39:47.135 [INFO ] [MooKo]               - Performing updates to profile page, if needed
2026-04-23 04:39:47.159 [INFO ] [MooKo]                       - Header has been updated to not be fixed
2026-04-23 04:39:50.029 [INFO ] [MooKo]               - Screenshot saved at: [/app/screenshots/2026-04-23/MooKo.png]
2026-04-23 04:39:50.448 [INFO ] [MooKo]       - Redaction: Blur
2026-04-23 04:39:50.448 [INFO ] [MooKo]               - Performing updates to profile page, if needed
2026-04-23 04:39:50.472 [INFO ] [MooKo]                       - Header has been updated to not be fixed
2026-04-23 04:39:50.474 [INFO ] [MooKo]               - Redacting elements with sensitive information
2026-04-23 04:39:50.563 [INFO ] [MooKo]                       - Found IP Address: 'We last checked this clients connectability 10 minutes ago, using '192.0.2.1:8080'.' in <span>
2026-04-23 04:39:50.573 [INFO ] [MooKo]               - Redacted the text of 1 element
2026-04-23 04:39:53.193 [INFO ] [MooKo]               - Screenshot saved at: [/app/screenshots/2026-04-23/MooKo_Blur.png]
2026-04-23 04:39:54.848 [INFO ] [MooKo]       - Logged out
```

Making this change now so if/when I introduce parallel execution it is still possible to see which tracker is executing what.
