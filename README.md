# Tracker Profiles

- [Overview](#overview)
- [Features](#features)
    - [Screenshots](#screenshots)
- [Trackers](#trackers)
    - [Headless](#headless)
    - [Manual Interaction](#manual-interaction)
- [How To Use](#how-to-use)
    - [Tracker Definitions](#tracker-definitions)
    - [Running In Docker](#running-in-docker)
    - [Web UI](#web-ui)
    - [Configuration Options](#configuration-options)
        - [Progress Bar](#progress-bar)
- [Versioning](#versioning)
- [AI Usage](#ai-usage)
    - [AI Agents](#ai-agents)
    - [Established Patterns](#established-patterns)
    - [Human Review](#human-review)
    - [Exceptions](#exceptions)
        - [JavaScript Redaction](#javascript-redaction)
        - [Progress Bar PrintStream](#progress-bar-printstream)
- [Contributing](#contributing)
    - [Requirements](#requirements)
    - [Install Git Hooks](#install-git-hooks)
    - [Debugging Application](#debugging-application)
    - [Building And Developing In Docker](#building-and-developing-in-docker)
    - [Implementing Support For New Trackers](#implementing-support-for-new-trackers)
    - [Cloudflare Verification](#cloudflare-verification)

## Overview

This is a tool used to log in to private torrent websites and take a screenshot of the user's profile page. This can be
used to showcase stats on your current tracker as part of an application to another site. It can also be used as a
historical record of your stats on a tracker in case it goes down or becomes otherwise unavailable.

## Features

- Opens the selected trackers and logs in, navigating to the user's profile page
- Requests user input for trackers with manual inputs (like Captcha, 2FA, etc.)
- Redacts the user's sensitive information
    - There are several types of redaction:
        - Blur (adds a Gaussian blur)
        - Box (draws a solid box)
        - Text (Replaces the text)
        - None (no redaction)
    - There following elements are currently redacted
        - Email address
        - IP address (including ISP)
        - Passkey (for torrents or for IRC)
- Takes a full-page screenshot of the redacted user profile

### Screenshots

Below are examples of the different types of redaction from the [MooKo](https://mooko.org/) tracker.

<details>
<summary>Example Screenshots</summary>
<table>
<tr>
<td valign="top">

#### None

![No Redaction](./doc/images/MooKo.png)

</td>
<td valign="top">

#### Blur

![Blur Redaction](./doc/images/MooKo_Blur.png)

</td>
<td valign="top">

#### Box

![Box Redaction](./doc/images/MooKo_Box.png)

</td>
<td valign="top">

#### Remove

![Remove Redaction](./doc/images/MooKo_Remove.png)

</td>
<td valign="top">

#### Text

![Text Redaction](./doc/images/MooKo_Text.png)

</td>
</tr>
</table>
</details>

## Trackers

There are currently **147** supported trackers listed below. The available trackers come in the following types:

- Headless: Can run with the browser in headless mode, meaning no UI browser is needed
- Manual: There is some user interaction needed (a Captcha or 2FA to log in, etc.), requiring a UI browser

The implementation for these trackers can be found in
the [handler](./tracker-profiles-screenshots/src/main/java/net/zodac/tracker/handler) package.

### Headless

The following trackers do not require a UI (unless `FORCE_UI_BROWSER` has been set to **true**), and can be run in the
background:

<table>
<tr>
<td valign="top">

| Tracker Name (A–E)                                |
|---------------------------------------------------|
| [ABTorrents](https://usefultrash.net/)            |
| [AcrossTheTasman](https://acrossthetasman.com/)   |
| [Aidoru!Online](https://aidoru-online.me/)        |
| [Aither](https://aither.cc/)                      |
| [Alexandria](https://alxdria.org/)                |
| [AlphaRatio](https://alpharatio.cc/)              |
| [AnimeBytes](https://animebytes.tv/)              |
| [Anthelion](https://anthelion.me/)                |
| [ArabicSource](https://arabicsource.net/)         |
| [ArabP2P](https://www.arabp2p.net/)               |
| [AsianCinema](https://eiga.moi/)                  |
| [AsianDVDClub](https://asiandvdclub.org/)         |
| [Aura4K](https://aura4k.net/)                     |
| [AussieRul.es](https://aussierul.es/)             |
| [BackUps](https://back-ups.me/)                   |
| [BakaBT](https://bakabt.me/)                      |
| [bitGAMER](https://bitgamer.ch/)                  |
| [BitPorn](https://bitporn.eu/)                    |
| [BlueTorrents](https://bluetorrents.com/)         |
| [Blutopia](https://blutopia.cc/)                  |
| [BootyTape](https://ssl.bootytape.com/)           |
| [BrokenStones](https://brokenstones.is/)          |
| [BwTorrents](https://bwtorrents.tv/)              |
| [C411](https://c411.org/)                         |
| [CanalStreet](https://canal-street.org/)          |
| [CapybaraBR](https://capybarabr.com/)             |
| [Cathode-Ray.Tube](https://www.cathode-ray.tube/) |
| [Concertos](https://concertos.live/)              |
| [D3Si.NET](https://d3si.net/)                     |
| [DarkPeers](https://darkpeers.org/)               |
| [DesiTorrents](https://desitorrents.tv/)          |
| [DICMusic](https://dicmusic.com/)                 |
| [DimeADozen](http://www.dimeadozen.org/)          |
| [Empornium](https://www.empornium.sx/)            |
| [ExtremeBits](https://extremebits.net/)           |

</td>
<td valign="top">

| Tracker Name (F–J)                              |
|-------------------------------------------------|
| [F1Carreras](https://f1carreras.xyz/)           |
| [F1GP](https://f1gp.site/)                      |
| [Fappaizuri](https://fappaizuri.me/)            |
| [FearNoPeer](https://fearnopeer.com/)           |
| [FileList](https://filelist.io/)                |
| [FunFile](https://www.funfile.org/)             |
| [GreatPosterWall](https://greatposterwall.com/) |
| [HappyFappy](https://www.happyfappy.net/)       |
| [HDUnited](https://hd-united.vn/)               |
| [Hellenic-HD](https://hellenic-hd.cc/)          |
| [ImmortalSeed](https://immortalseed.me/)        |
| [InfinityHD](https://infinityhd.net/)           |
| [InfinityLibrary](https://infinitylibrary.net/) |
| [IPTorrents](https://iptorrents.com/)           |
| [ItaTorrents](https://itatorrents.xyz/)         |
| [JPopsuki](https://jpopsuki.eu/)                |

</td>
<td valign="top">

| Tracker Name (K–O)                             |
|------------------------------------------------|
| [Kufirc](https://kufirc.com/)                  |
| [Lat-Team](https://lat-team.com/)              |
| [LDU](https://theldu.to/)                      |
| [Libble](https://libble.me/)                   |
| [Luminarr](https://luminarr.me/)               |
| [MalayaBits](https://malayabits.cc/)           |
| [Metal-Tracker](https://en.metal-tracker.com/) |
| [Milkie](https://milkie.cc/)                   |
| [MooKo](https://mooko.org/)                    |
| [MoreThanTV](https://www.morethantv.me/)       |
| [MyAnonaMouse](https://www.myanonamouse.net/)  |
| [Nebulance](https://nebulance.io/)             |
| [Nexum-Core](https://nexum-core.com/)          |
| [NordicBytes](https://nordicbytes.org/)        |
| [NordicHD](http://nordichd.org/)               |
| [NordicQuality](https://nordicq.org/)          |
| [OldToons.World](https://oldtoons.world/)      |
| [OnlyEncodes](https://onlyencodes.cc/)         |
| [Orpheus](https://orpheus.network/)            |

</td>
<td valign="top">

| Tracker Name (P–T)                                   |
|------------------------------------------------------|
| [P2PBG](https://www.p2pbg.com/)                      |
| [PixelCove](https://www.pixelcove.me/)               |
| [PixelHD](https://pixelhd.me/)                       |
| [Podzemlje](https://podzemlje.net/)                  |
| [PolishTorrent](https://polishtorrent.top/)          |
| [Portugas](https://portugas.org/)                    |
| [PussyTorrents](https://pussytorrents.org/)          |
| [Rastastugan](https://rastastugan.org/)              |
| [Redacted](https://redacted.sh/)                     |
| [ReelFlix](https://reelflix.cc/)                     |
| [RetroMoviesClub](https://retro-movies.club/)        |
| [RocketHD](https://rocket-hd.cc/)                    |
| [RUTracker](https://rutracker.org/forum/tracker.php) |
| [SceneHD](https://scenehd.org/)                      |
| [SceneTime](https://www.scenetime.com/)              |
| [SecretCinema](https://secret-cinema.pw/)            |
| [SeedPool](https://seedpool.org/)                    |
| [SexTorrent](https://sextorrent.myds.me/)            |
| [Shazbat](https://www.shazbat.tube/)                 |
| [SlobitMedia](https://media.slo-bitcloud.eu/)        |
| [SpeedApp](https://speedapp.io/)                     |
| [SportsCult](https://sportscult.org/)                |
| [T3nnis](https://t3nnis.tv/)                         |
| [Tasmanit.es](https://tasmanit.es/)                  |
| [TeamOS](https://teamos.xyz/)                        |
| [TheMixingBowl](https://themixingbowl.org/)          |
| [Torr9](https://torr9.net/)                          |
| [TorrentLeech](https://www.torrentleech.org/)        |
| [TranceTraffic](https://www.trancetraffic.com/)      |
| [TVChaosUK](https://tvchaosuk.com/)                  |

</td>
<td valign="top">

| Tracker Name (U–Z)                              |
|-------------------------------------------------|
| [Unwalled](https://unwalled.cc/)                |
| [VietMediaF](https://tracker.vietmediaf.store/) |
| [XSpeeds](https://www.xspeeds.eu/)              |
| [XWT-Classics](https://xwt-classics.net/)       |
| [YUSCENE](https://yu-scene.net/)                |
| [Zappateers](https://zappateers.com/)           |

</td>
</tr>
</table>

### Manual Interaction

If the following trackers are enabled (either uncommented in `TRACKER_INPUT_FILE_PATH`, or **MANUAL** is included in
`TRACKER_EXECUTION_ORDER`), then user input is required during login. These trackers are not supported in the default
Docker Compose setup — see [Web UI](#web-ui) for details.

<table>
<tr>
<td valign="top">

| Tracker Name                                        | Reason                                          |
|-----------------------------------------------------|-------------------------------------------------|
| [52PT](https://52pt.site/)                          | Captcha                                         |
| [AnimeZ](https://animez.to/)                        | Captcha                                         |
| [AvistaZ](https://avistaz.to/)                      | Captcha & Cloudflare verificaiton               |
| [BeyondHD](https://beyond-hd.me/)                   | Captcha                                         |
| [BroadcasThe.Net](https://broadcasthe.net/)         | Captcha & Cloudflare verification               |
| [BTArg](https://www.btarg.com.ar/tracker/)          | Needs to be explicitly translated               |
| [CGPeers](https://cgpeers.to/)                      | 2FA & Cloudflare verification                   |
| [CinemaZ](https://cinemaz.to/)                      | Captcha & Cloudflare verification               |
| [DigitalCore.Club](https://digitalcore.club/)       | Captcha                                         |
| [DocsPedia](https://docspedia.world/)               | Captcha                                         |
| [ExoticaZ](https://exoticaz.to/)                    | Captcha & Cloudflare verification               |
| [GazelleGames](https://gazellegames.net/)           | Question on login                               |
| [Hawke-Uno](https://hawke.uno/)                     | 2FA & Cloudflare verification                   |
| [HD-Forever](https://hdf.world/)                    | Needs to be explicitly translated               |
| [HDBits](https://hdbits.org/)                       | 2FA, Cloudflare verification, question on login |
| [HDFans](https://hdfans.org/)                       | Captcha                                         |
| [Immortal-S](https://immortal-s.me/)                | Cloudflare verification                         |
| [HDZero](https://hdzero.org/)                       | Cloudflare verification                         |
| [LastFiles](https://last-torrents.org/)             | Cloudflare verification                         |
| [Leech24](https://leech24.net/)                     | Captcha                                         |
| [LemonHD](https://lemonhd.net/)                     | Captcha                                         |
| [LetSeed](https://letseed.org/)                     | Captcha                                         |
| [LP-Bits](https://lp-bits.com/)                     | Captcha                                         |
| [LST](https://lst.gg/)                              | Question on login                               |
| [PassThePopcorn](https://passthepopcorn.me/)        | Question on login                               |
| [PornoLab](https://pornolab.net/forum/tracker.php)  | Needs to be explicitly translated               |
| [PrivateHD](https://privatehd.to/)                  | Captcha & Cloudflare verification               |
| [PT.GTK](https://pt.gtk.pw/)                        | Captcha                                         |
| [RoTorrent](https://rotorrent.info/)                | Cloudflare verification                         |
| [SkyeySnow](https://skyeysnow.com/)                 | Needs to be explicitly translated               |
| [SocietyGlitch](https://stalker.societyglitch.com/) | Captcha                                         |
| [Speed.CD](https://speed.cd/)                       | Cloudflare verification                         |
| [TheEmpire](https://theempire.click/)               | Captcha                                         |
| [TheGeeks](https://thegeeks.click/)                 | Captcha                                         |
| [TheKitchen](https://thekitchen.click/)             | Captcha                                         |
| [TheOccult](https://theoccult.click/)               | Captcha                                         |
| [ThePlace](https://theplace.click/)                 | Captcha                                         |
| [TheShow](https://theshow.click/)                   | Captcha                                         |
| [TheVault](https://thevault.click/)                 | Captcha                                         |
| [Torrenting](https://torrenting.com/)               | Cloudflare verification                         |
| [UploadCX](https://upload.cx/)                      | Captcha                                         |

</td>
</tr>
</table>

## How To Use

### Tracker Definitions

First, copy the [trackers_example.csv](./docker/trackers_example.csv) file. This file needs to be updated with your
user's login information for each tracker. Any unwanted trackers can be deleted, or prefixed by the `CSV_COMMENT_SYMBOL`
environment variable so they are excluded. The tracker names are case-insensitive.

The file can be saved anywhere but needs to be mounted into the Docker container, and it will be referenced by the
`TRACKER_INPUT_FILE_PATH` environment variable when running the application, so remember where it is saved and what it
is named.

### Running In Docker

The application is run using Docker Compose, which starts the app and a headless Selenium Chrome node together.

1. Download or copy the [docker-compose.yml](./docker/docker-compose.yml) file into a working directory.
2. In the same directory, create a `screenshots/` folder and place your `trackers.csv` file inside it.
3. Start the stack:

   ```bash
   docker compose up
   ```

4. Open a browser and navigate to [http://localhost:8080](http://localhost:8080).
5. Click **Start** to begin execution. Logs stream to the page in real-time.
6. Screenshots are saved to the `screenshots/` directory on the host.

To customise the application, add `environment` entries under the `app` service in `docker-compose.yml`. See
[Configuration Options](#configuration-options) for the full list.

### Web UI

The application exposes a web UI on port 8080. After starting the stack with `docker compose up`, open
[http://localhost:8080](http://localhost:8080) in any browser.

Click **Start** to launch the screenshot run. All log output streams to the page in real-time. The button is
re-enabled once execution completes, allowing another run to be started.

> **Note:** Trackers that require manual user interaction (Captcha, 2FA, Cloudflare verification, etc.) are not
> supported in the Docker Compose setup — the Selenium container runs fully headless with no interactive access.
> Set `TRACKER_EXECUTION_ORDER=HEADLESS` to skip those trackers.

### Configuration Options

The following are all possible configuration options, defined as environment variables for the docker image:

| Environment Variable                | Description                                                                                                                                                                                              | Default Value                 |
|-------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------|
| *BROWSER_HEIGHT*                    | The height (in pixels) of the web browser used to take screenshots                                                                                                                                       | 1050                          |
| *BROWSER_WIDTH*                     | The width (in pixels) of the web browser used to take screenshots                                                                                                                                        | 1680                          |
| *CSV_COMMENT_SYMBOL*                | If this character is the first in a CSV row, the CSV row is considered a comment and not processed                                                                                                       | #                             |
| *ENABLE_ADULT_TRACKERS*             | Whether to take screenshots of trackers that primarily host adult content                                                                                                                                | true                          |
| *ENABLE_TRANSLATION_TO_ENGLISH*     | Whether to translate non-English trackers to English                                                                                                                                                     | true                          |
| *FAIL_ON_UNSUPPORTED_TRACKER*       | Whether to fail if a tracker in the CSV file has no matching handler implementation                                                                                                                      | true                          |
| *FORCE_UI_BROWSER*                  | Forces a browser with UI for each tracker (even for headless trackers)                                                                                                                                   | false                         |
| *INPUT_TIMEOUT_ENABLED*             | Whether to add a timeout for when a user-input is required, otherwise waits                                                                                                                              | false                         |
| *INPUT_TIMEOUT_SECONDS*             | If *INPUT_TIMEOUT_ENABLED* is enabled, how long to wait for a user-input in [seconds]                                                                                                                    | 300                           |
| *JAVA_XMS*                          | The initial heap size for the Java process                                                                                                                                                               | 128m                          |
| *JAVA_XMX*                          | The maximum heap size for the Java process                                                                                                                                                               | 512m                          |
| *LOG_LEVEL*                         | The logging level for console output [TRACE, DEBUG, INFO, WARN, ERROR]                                                                                                                                   | INFO                          |
| *LOG_TRACKER_NAME*                  | Whether to prefix each log message with the name of the tracker being screenshot                                                                                                                         | true                          |
| *NUMBER_OF_TRACKER_ATTEMPTS*        | The number of times to attempt to screenshot a tracker (with retries if it fails or the wrong manual input was selected) [max of 5]                                                                      | 1                             |
| *OUTPUT_DIRECTORY_NAME_FORMAT*      | The name of the output directory to be created for the of the screenshots (see [Patterns for Formatting and Parsing](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html)) | yyyy-MM-dd                    |
| *OUTPUT_DIRECTORY_PARENT_PATH*      | The output location of the new directory created for the screenshots, relative to the project root                                                                                                       | /tmp/screenshots              |
| *PROGRESS_BAR_COMPLETE_CHARACTER*   | The character used to render the completed portion of the [progress bar](#progress-bar) (must differ from *PROGRESS_BAR_INCOMPLETE_CHARACTER*                                                            | █                             |
| *PROGRESS_BAR_ENABLED*              | Whether to render a progress bar at the bottom of the console output                                                                                                                                     | true                          |
| *PROGRESS_BAR_FORMAT*               | The format string for the [progress bar](#progress-bar) (must not be blank)                                                                                                                              | :bar :percent% \| [:elapsed]  |
| *PROGRESS_BAR_INCOMPLETE_CHARACTER* | The character used to render the incomplete portion of the [progress bar](#progress-bar) (must differ from *PROGRESS_BAR_COMPLETE_CHARACTER*                                                             | ░                             |
| *PROGRESS_BAR_LENGTH*               | The length (in characters) of the [progress bar](#progress-bar) [min: 10, max: 80]                                                                                                                       | 35                            |
| *REDACTION_TEXT*                    | The placeholder text used to replace sensitive information (only when using TEXT redaction, will be truncated if longer than the sensitive information)                                                  | ----                          |
| *REDACTION_TYPE*                    | Comma-separated list of redaction types to apply (if more than one is selected then multiple screenshots will be taken) [NONE, BLUR, BOX, REMOVE, TEXT]                                                  | BOX                           |
| *SCREENSHOT_EXISTS_ACTION*          | What to do when a screenshot for the tracker for the given date already exists [CREATE_ANOTHER, OVERWRITE, SKIP]                                                                                         | CREATE_ANOTHER                |
| *SELENIUM_REMOTE_URL*               | The URL of the Selenium Grid or standalone node to use for browser automation; if blank, a local ChromeDriver is used                                                                                    | (empty)                       |
| *TAKE_SCREENSHOT_ON_ERROR*          | Whether to take a screenshot of the current tracker page if any failure occurs (in a subdirectory called `errors`)                                                                                       | false                         |
| *TIMEZONE*                          | The local timezone, used to retrieve the current date to name the output directory                                                                                                                       | UTC                           |
| *TRACKER_EXECUTION_ORDER*           | The order in which different tracker types should be executed, at least one must be selected (case-insensitive)                                                                                          | HEADLESS,MANUAL               |
| *TRACKER_INPUT_FILE_PATH*           | The path to the input tracker definition CSV file (inside the docker container)                                                                                                                          | /tmp/screenshots/trackers.csv |

#### Progress Bar

The progress bar is rendered at the bottom of the console output during execution, showing overall screenshot progress.
It is powered by the [Clique](https://github.com/kusoroadeolu/Clique) library.

The bar advances in fine-grained steps (login, profile page, each screenshot, logout) so the fill and percentage move
smoothly. A tracker counter (`X/Y`) is always appended to the right of the rendered format string, showing how many
trackers have fully completed regardless of how the format string is configured.

The format string supports the following tokens, as defined in the
[Clique source](https://github.com/kusoroadeolu/Clique/blob/main/docs/progress-bars.md):

| Token       | Description                                                                        |
|-------------|------------------------------------------------------------------------------------|
| `:bar`      | The rendered progress bar itself                                                   |
| `:elapsed`  | Time elapsed since the bar was started                                             |
| `:percent`  | Completion percentage (0–100), based on workflow steps not tracker count           |
| `:progress` | Number of completed internal ticks (workflow steps, not trackers) — see note below |
| `:total`    | Total number of internal ticks (workflow steps, not trackers) — see note below     |

> **Note:** `:progress` and `:total` reflect the internal tick count used to drive the bar fill (workflow steps), not
> the number of trackers. For a tracker counter, use the `X/Y` suffix that is always appended to the bar.

> **Note:** When the JVM's native encoding is not UTF-8, multibyte characters passed via environment variable (such as
> `█` and `░`) may arrive as individual bytes rather than a single character. I try to convert these appropriately, but
> it doesn't always work. If you're having issues, try to use plain ASCII characters, or use the defaults. Feel free to
> raise an issue and I can look into it.

## Versioning

This project follows [Semantic Versioning](https://semver.org/) (`MAJOR.MINOR.PATCH`):

- **MAJOR**: incompatible changes to the public API (e.g. removed or renamed configuration options, changed CSV format)
- **MINOR**: backwards-compatible new functionality added to the public API (e.g. new configuration options or trackers)
- **PATCH**: backwards-compatible bug fixes or code changes

Changes to the [trackers_example.csv](./docker/trackers_example.csv) file are considered `MINOR` (new trackers) or
`MAJOR` (updates to the tracker name). However, removing a tracker due to the site no longer being available is **not**
considered a `MAJOR` change. Trackers are external to the application and not part of its public API, as their
availability depends on third-party sites that can change or disappear at any time. Tracker removals will be released as
`PATCH` versions.

## AI Usage

I use AI agents to perform the "first-pass" implementation for a new tracker site. The AI will investigate and evaluate
the generate structure of the site and build an initial handler. Based off of this, I'll refine and test until I'm
happy that it matches the same standards as the existing implementations.

Since there were over 100 implementations prior to using AI, there are established patterns and styles to follow,
hopefully guiding the AI to follow the same standards.

### AI Agents

[Claude Code](https://claude.com/claude-code) employs a multi-agent pipeline to implement each new tracker handler:

| Agent                    | Responsibility                                                                                                                                                             |
|--------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Orchestrator**         | Coordinates the full workflow, delegating to specialised sub-agents in sequence                                                                                            |
| **Login Agent**          | Inspects the tracker's login page to determine the correct selectors for the username field, password field, login button, and post-login confirmation element             |
| **Profile Agent**        | Navigates the user's profile page to identify the navigation selector that reaches it, the content selector that confirms it has loaded, and the logout button selector    |
| **Page Structure Agent** | Invoked when the profile agent flags structural elements and  detects cookie/consent banners, fixed headers, and fixed sidebars that must be handled before screenshotting |
| **Redactor Agent**       | Reviews the profile page for sensitive fields and determines the appropriate redaction selectors                                                                           |

### Established Patterns

Every implementation relies on:

- **Existing handler code**: The agents study the handlers already in
  the [handler/](./tracker-profiles-screenshots/src/main/java/net/zodac/tracker/handler) package to understand project
  conventions, selector construction patterns, and platform-specific base classes (`Unit3dHandler`, `GazelleHandler`,
  etc.)
- **[CLAUDE.md](./.claude/CLAUDE.md)**: This defines project-level standards covering structure, selector conventions,
  linting rules, and the mandatory post-implementation checklist
- **Agent definitions**: Each agent has a [Markdown](.claude/agents) file defining its scope, and how to parse the
  tracker to build the handler, which is refined over time

### Human Review

All AI-generated code is reviewed and approved before being merged, including manual testing of the new handler. Since
each handler needs to be tested against a real site, there's no value to automating much testing, so each implementation
will be verified manually.

### Exceptions

#### JavaScript Redaction

The [JavaScript redaction logic](./tracker-profiles-screenshots/src/main/resources/net/zodac/tracker/redaction) used in
[BlurRedactor](./tracker-profiles-screenshots/src/main/java/net/zodac/tracker/redaction/BlurRedactor.java) and
[BoxRedactor](./tracker-profiles-screenshots/src/main/java/net/zodac/tracker/redaction/BoxRedactor.java) is mostly AI
created with little input from me. I don't know JavaScript well enough to perform this overlay-based redaction, but I
verify against all tracker handlers when updates are made, to cover as many cases as possible.

#### Progress Bar PrintStream

The [ProgressBarPrintStream](tracker-profiles-screenshots/src/main/java/net/zodac/tracker/framework/progress/ProgressBarPrintStream.java)
contains some code for handling print streams to override the default console printing of
the [Clique progress bar](#progress-bar). I'm not super well-versed in handling streams, so I use AI to make updates and
then verify manually afterwards.

## Contributing

### Requirements

- [Apache Maven](https://maven.apache.org/download.cgi)
- [Docker](https://docs.docker.com/engine/install/)
- [Java](https://jdk.java.net/)

### Install Git Hooks

Run the following command to run git hooks for the project:

```bash
bash ./ci/hooks/setup-hooks.sh
```

### Debugging Application

Using IntelliJ, and click on **Run**> **Edit Configurations** and add the environment variables for the application.
Once done, open
the [ApplicationLauncher.java](./tracker-profiles-screenshots/src/main/java/net/zodac/tracker/ApplicationLauncher.java)
and run the `main` method from the IDE.
The [AbstractTrackerHandler.java](./tracker-profiles-screenshots/src/main/java/net/zodac/tracker/handler/AbstractTrackerHandler.java)
implementation for each tracker is retrieved by the *trackerName* field within the CSV file.

[Selenium WebDriver](https://www.selenium.dev/documentation/webdriver/) is used to control the Chromium browser running
in the `selenium/standalone-chrome` container. The `SELENIUM_REMOTE_URL` environment variable tells the app where to
connect.

### Building And Developing In Docker

Build a local image and start the full stack against it:

```bash
docker build -f ./docker/Dockerfile -t zodac/tracker-profiles:latest . &&
BROWSER_HEIGHT=1050 \
BROWSER_WIDTH=1680 \
CSV_COMMENT_SYMBOL='#' \
ENABLE_ADULT_TRACKERS=true \
ENABLE_TRANSLATION_TO_ENGLISH=true \
FAIL_ON_UNSUPPORTED_TRACKER=false \
INPUT_TIMEOUT_ENABLED=true \
INPUT_TIMEOUT_SECONDS=300 \
JAVA_XMS=128m \
JAVA_XMX=512m \
LOG_LEVEL=TRACE \
LOG_TRACKER_NAME=true \
NUMBER_OF_TRACKER_ATTEMPTS=5 \
OUTPUT_DIRECTORY_NAME_FORMAT=yyyy-MM-dd \
OUTPUT_DIRECTORY_PARENT_PATH=/app/screenshots \
PROGRESS_BAR_COMPLETE_CHARACTER='█' \
PROGRESS_BAR_ENABLED=true \
PROGRESS_BAR_FORMAT=":bar :percent% | :progress/:total | [:elapsed]" \
PROGRESS_BAR_INCOMPLETE_CHARACTER='░' \
PROGRESS_BAR_LENGTH=35 \
REDACTION_TEXT=---- \
REDACTION_TYPE=NONE,BLUR,BOX,REMOVE,TEXT \
SCREENSHOT_EXISTS_ACTION=CREATE_ANOTHER \
TAKE_SCREENSHOT_ON_ERROR=true \
TIMEZONE=UTC \
TRACKER_EXECUTION_ORDER=HEADLESS \
TRACKER_INPUT_FILE_PATH=/app/screenshots/trackers.csv \
docker compose -f ./docker/docker-compose.yml up
```

### Implementing Support For New Trackers

All supported private trackers have an implementation found in
the [handler](./tracker-profiles-screenshots/src/main/java/net/zodac/tracker/handler) package. To add a new one, extend
[AbstractTrackerHandler.java](./tracker-profiles-screenshots/src/main/java/net/zodac/tracker/handler/AbstractTrackerHandler.java),
following the convention from an existing implementation
like [AbTorrents.java](./tracker-profiles-screenshots/src/main/java/net/zodac/tracker/handler/AbTorrents.java).

Ensure the [TrackerType](./tracker-profiles-screenshots/src/main/java/net/zodac/tracker/framework/TrackerType.java) is
set correctly for your tracker.

### Cloudflare Verification

Some of the `Manual` trackers listed in [Trackers> Manual Interaction](#manual-interaction) contain a Cloudflare
verification check. This check cannot be passed simply by opening the browser UI like other `manual` trackers. Instead,
[undetected-chromedriver](https://github.com/ultrafunkamsterdam/undetected-chromedriver) is used to patch the Google
Chrome binary, making it possibly to successfully check the Cloudflare box (by a user's manual input), bypassing
Cloudflare detection.
