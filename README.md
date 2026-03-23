# Tracker Profiles

- [Overview](#overview)
- [Features](#features)
    - [Screenshots](#screenshots)
- [Trackers](#trackers)
    - [Headless](#headless)
    - [Non-Headless](#non-headless)
- [How To Use](#how-to-use)
    - [Tracker Definitions](#tracker-definitions)
    - [Running In Docker](#running-in-docker)
    - [Browser UI](#browser-ui)
        - [UI In Debian](#ui-in-debian)
        - [UI In Windows](#ui-in-windows)
        - [Disable UI](#disable-ui)
    - [Configuration Options](#configuration-options)
- [Versioning](#versioning)
- [Contributing](#contributing)
    - [Requirements](#requirements)
    - [Install Git Hooks](#install-git-hooks)
    - [Debugging Application](#debugging-application)
    - [Building And Developing In Docker](#building-and-developing-in-docker)
    - [Implementing Support For New Trackers](#implementing-support-for-new-trackers)
    - [Cloudflare Trackers](#cloudflare-trackers)

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

#### Text

![Text Redaction](./doc/images/MooKo_Text.png)

</td>
</tr>
</table>
</details>

## Trackers

There are currently **115** supported trackers listed below. The available trackers come in the following types:

- Headless: Can run with the browser in headless mode, meaning no UI browser is needed
- Manual: There is some user interaction needed (a Captcha or 2FA to log in, etc.), requiring a UI browser
- Cloudflare-Check: The tracker has a Cloudflare verification check this will need a UI browser to bypass (overrides the
  **Manual** option)

**Note:** Any tracker not listed in any section below has not been tested (most likely due to lack of an account).

### Headless

The following trackers do not require a UI (unless `FORCE_UI_BROWSER` has been set to **true**), and can be run in the
background:

<table>
<tr>
<td valign="top">

| Tracker Name (A–E)                                |
|---------------------------------------------------|
| [ABTorrents](https://usefultrash.net/)            |
| [Aither](https://aither.cc/)                      |
| [AlphaRatio](https://alpharatio.cc/)              |
| [AnimeBytes](https://animebytes.tv/)              |
| [Anthelion](https://anthelion.me/)                |
| [ArabicSource](https://arabicsource.net/)         |
| [ArabP2P](https://www.arabp2p.net/)               |
| [AsianCinema](https://eiga.moi/)                  |
| [Aura4K](https://aura4k.net/)                     |
| [BackUps](https://back-ups.me/)                   |
| [BakaBT](https://bakabt.me/)                      |
| [BitPorn](https://bitporn.eu/)                    |
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
| [Fappaizuri](https://fappaizuri.me/)            |
| [FearNoPeer](https://fearnopeer.com/)           |
| [FileList](https://filelist.io/)                |
| [FunFile](https://www.funfile.org/)             |
| [GreatPosterWall](https://greatposterwall.com/) |
| [HappyFappy](https://www.happyfappy.net/)       |
| [HDUnited](https://hd-united.vn/)               |
| [HDZone](https://hdzero.org/)                   |
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
| [PixelCove](https://www.pixelcove.me/)               |
| [PixelHD](https://pixelhd.me/)                       |
| [PolishTorrent](https://polishtorrent.top/)          |
| [PornBay](https://pornbay.org/)                      |
| [Rastastugan](https://rastastugan.org/)              |
| [Redacted](https://redacted.sh/)                     |
| [ReelFlix](https://reelflix.cc/)                     |
| [RocketHD](https://rocket-hd.cc/)                    |
| [RUTracker](https://rutracker.org/forum/tracker.php) |
| [SceneHD](https://scenehd.org/)                      |
| [SecretCinema](https://secret-cinema.pw/)            |
| [SeedPool](https://seedpool.org/)                    |
| [SexTorrent](https://sextorrent.myds.me/)            |
| [SportsCult](https://sportscult.org/)                |
| [T3nnis](https://t3nnis.tv/)                         |
| [Tasmanites](https://tasmanit.es/)                   |
| [TeamOS](https://teamos.xyz/)                        |
| [TheMixingBowl](https://themixingbowl.org/)          |
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

### Non-Headless

If the following trackers are enabled (either uncommented in `TRACKER_INPUT_FILE_PATH`, or their type is included in
`TRACKER_EXECUTION_ORDER`), then a UI must be enabled. Instructions for this in Docker can be seen [below](#browser-ui).

<table>
<tr>
<td valign="top">

| Manual                                              |
|-----------------------------------------------------|
| [52PT](https://52pt.site/)                          |
| [AnimeZ](https://animez.to/)                        |
| [BeyondHD](https://beyond-hd.me/)                   |
| [DigitalCore.Club](https://digitalcore.club/)       |
| [DocsPedia](https://docspedia.world/)               |
| [GazelleGames](https://gazellegames.net/)           |
| [HDFans](https://hdfans.org/)                       |
| [HD-Forever](https://hdf.world/)                    |
| [LemonHD](https://lemonhd.net/)                     |
| [LetSeed](https://letseed.org/)                     |
| [LST](https://lst.gg/)                              |
| [PassThePopcorn](https://passthepopcorn.me/)        |
| [PornoLab](https://pornolab.net/forum/tracker.php)  |
| [PT.GTK](https://pt.gtk.pw/)                        |
| [SocietyGlitch](https://stalker.societyglitch.com/) |
| [TheEmpire](https://theempire.click/)               |
| [TheGeeks](https://thegeeks.click/)                 |
| [TheKitchen](https://thekitchen.click/)             |
| [TheOccult](https://theoccult.click/)               |
| [ThePlace](https://theplace.click/)                 |
| [TheShow](https://theshow.click/)                   |
| [TheVault](https://thevault.click/)                 |

</td>
<td valign="top">

| Cloudflare-Check                            |
|---------------------------------------------|
| [AvistaZ](https://avistaz.to/)              |
| [BroadcasThe.Net](https://broadcasthe.net/) |
| [CGPeers](https://cgpeers.to/)              |
| [CinemaZ](https://cinemaz.to/)              |
| [ExoticaZ](https://exoticaz.to/)            |
| [Hawke-Uno](https://hawke.uno/)             |
| [HDBits](https://hdbits.org/)               |
| [Immortal-S](https://immortal-s.me/)        |
| [PrivateHD](https://privatehd.to/)          |
| [RoTorrent](https://rotorrent.info/)        |
| [SceneTime](https://www.scenetime.com/)     |
| [Speed.CD](https://speed.cd/)               |
| [Torrenting](https://torrenting.com/)       |
| [UploadCX](https://upload.cx/)              |

</td>
</tr>
</table>

## How To Use

### Tracker Definitions

First, copy the [trackers_example.csv](./docker/trackers_example.csv) file. This file needs to be updated with your
user's login information for each tracker. Any unwanted trackers can be deleted, or prefixed by the `CSV_COMMENT_SYMBOL`
environment variable so they are excluded. The tracker names are case-insensitive.

The file can be saved anywhere, and it will be referenced by the `TRACKER_INPUT_FILE_PATH` environment variable when
running the application, so remember where it is saved and what it is named.

### Running In Docker

The application is run using Docker, and below are the commands to run the `latest` docker image.

<details>
<summary>Docker Commands</summary>
<table>

<tr>
<td valign="top">

#### Debian

```bash
docker run \
    --env DISPLAY="${DISPLAY}" \
    --env BROWSER_HEIGHT=1050 \
    --env BROWSER_WIDTH=1680 \
    --env CSV_COMMENT_SYMBOL='#' \
    --env ENABLE_ADULT_TRACKERS=true \
    --env ENABLE_TRANSLATION_TO_ENGLISH=true \
    --env FORCE_UI_BROWSER=false \
    --env INPUT_TIMEOUT_ENABLED=false \
    --env INPUT_TIMEOUT_SECONDS=300 \
    --env LOG_LEVEL=INFO \
    --env NUMBER_OF_TRACKER_ATTEMPTS=1 \
    --env OUTPUT_DIRECTORY_NAME_FORMAT=yyyy-MM-dd \
    --env OUTPUT_DIRECTORY_PARENT_PATH=/app/screenshots \
    --env REDACTION_TYPE=BOX \
    --env SCREENSHOT_EXISTS_ACTION=CREATE_ANOTHER \
    --env TAKE_SCREENSHOT_ON_ERROR=false \
    --env TIMEZONE=UTC \
    --env TRACKER_EXECUTION_ORDER=headless,manual,cloudflare-check \
    --env TRACKER_INPUT_FILE_PATH=/app/screenshots/trackers.csv \
    -v /tmp/.X11-unix:/tmp/.X11-unix \
    -v /tmp/screenshots:/app/screenshots \
    --name tracker-profiles \
    --rm zodac/tracker-profiles:latest
```

</td>
<td valign="top">

#### Windows

```bash
MSYS_NO_PATHCONV=1 docker run \
    --env DISPLAY=host.docker.internal:0 \
    --env BROWSER_HEIGHT=1050 \
    --env BROWSER_WIDTH=1680 \
    --env CSV_COMMENT_SYMBOL='#' \
    --env ENABLE_ADULT_TRACKERS=true \
    --env ENABLE_TRANSLATION_TO_ENGLISH=true \
    --env FORCE_UI_BROWSER=false \
    --env INPUT_TIMEOUT_ENABLED=false \
    --env INPUT_TIMEOUT_SECONDS=300 \
    --env LOG_LEVEL=INFO \
    --env NUMBER_OF_TRACKER_ATTEMPTS=1 \
    --env OUTPUT_DIRECTORY_NAME_FORMAT=yyyy-MM-dd \
    --env OUTPUT_DIRECTORY_PARENT_PATH=/app/screenshots \
    --env REDACTION_TYPE=BOX \
    --env SCREENSHOT_EXISTS_ACTION=CREATE_ANOTHER \
    --env TAKE_SCREENSHOT_ON_ERROR=false \
    --env TIMEZONE=UTC \
    --env TRACKER_EXECUTION_ORDER=headless,manual,cloudflare-check \
    --env TRACKER_INPUT_FILE_PATH=/app/screenshots/trackers.csv \
    -v /c/tmp/screenshots:/app/screenshots \
    --name tracker-profiles \
    --rm zodac/tracker-profiles:latest
```

</td>
</tr>
</table>
</details>

### Browser UI

There are two ways to execute the application - with a UI browser and without. The default commands will
execute [trackers that require a UI](#non-headless), so the UI will need to be configured to run through Docker. A UI
browser is needed for trackers that:

- Require some user input during login, like a Captcha or 2FA (if `TRACKER_EXECUTION_ORDER` includes **manual**)
- Have a Cloudflare verification check (if `TRACKER_EXECUTION_ORDER` includes **cloudflare-check**)

Below will define how to do this for your host system.

#### UI in Debian

To run through Docker with a UI, local connections to the host display must be enabled:

```bash
# This seems to be reset upon reboot and may need to be reapplied
xhost +local:
```

#### UI in Windows

I use [VcXsrv](https://vcxsrv.com/) as the X server for UI. When configuring VcXsrv, make sure to set the following in
the configuration:

- Multiple windows
- Display number 0
- Disable access control

#### Disable UI

To disable the UI and run the browser in headless mode only, ensure `FORCE_UI_BROWSER` and
`ENABLE_TRANSLATION_TO_ENGLISH` are set to **false**, and exclude **manual** and **cloudflare-check** from
`TRACKER_EXECUTION_ORDER`. You can also remove `--env DISPLAY` and/or `-v /tmp/.X11-unix:/tmp/.X11-unix` from the
`docker run` command.

### Configuration Options

The following are all possible configuration options, defined as environment variables for the docker image:

| Environment Variable            | Description                                                                                                                                                                                              | Default Value                    |
|---------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------|
| *BROWSER_HEIGHT*                | The height (in pixels) of the web browser used to take screenshots                                                                                                                                       | 1050                             |
| *BROWSER_WIDTH*                 | The width (in pixels) of the web browser used to take screenshots                                                                                                                                        | 1680                             |
| *CSV_COMMENT_SYMBOL*            | If this character is the first in a CSV row, the CSV row is considered a comment and not processed                                                                                                       | #                                |
| *ENABLE_ADULT_TRACKERS*         | Whether to take screenshots of trackers that primarily host adult content                                                                                                                                | true                             |
| *ENABLE_TRANSLATION_TO_ENGLISH* | Whether to translate non-English trackers to English                                                                                                                                                     | true                             |
| *FORCE_UI_BROWSER*              | Forces a browser with UI for each tracker (even for headless trackers)                                                                                                                                   | false                            |
| *INPUT_TIMEOUT_ENABLED*         | Whether to add a timeout for when a user-input is required, otherwise waits                                                                                                                              | false                            |
| *INPUT_TIMEOUT_SECONDS*         | If *INPUT_TIMEOUT_ENABLED* is enabled, how long to wait for a user-input (in seconds)                                                                                                                    | 300                              |
| *LOG_LEVEL*                     | The logging level for console output [TRACE, DEBUG, INFO, WARN, ERROR]                                                                                                                                   | INFO                             |
| *NUMBER_OF_TRACKER_ATTEMPTS*    | The number of times to attempt to screenshot a tracker (with retries if it fails or the wrong manual input was selected) (max of 5)                                                                      | 1                                |
| *OUTPUT_DIRECTORY_NAME_FORMAT*  | The name of the output directory to be created for the of the screenshots (see [Patterns for Formatting and Parsing](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html)) | yyyy-MM-dd                       |
| *OUTPUT_DIRECTORY_PARENT_PATH*  | The output location of the new directory created for the screenshots, relative to the project root                                                                                                       | /tmp/screenshots                 |
| *REDACTION_TYPE*                | Comma-separated list of redaction types to apply (if more than one is selected then multiple screenshots will be taken) [NONE, BLUR, BOX, TEXT]                                                          | BOX                              |
| *SCREENSHOT_EXISTS_ACTION*      | What to do when a screenshot for the tracker for the given date already exists [CREATE_ANOTHER, OVERWRITE, SKIP]                                                                                         | CREATE_ANOTHER                   |
| *TAKE_SCREENSHOT_ON_ERROR*      | Whether to take a screenshot of the current tracker page if any failure occurs (in a subdirectory called `errors`)                                                                                       | false                            |
| *TIMEZONE*                      | The local timezone, used to retrieve the current date to name the output directory                                                                                                                       | UTC                              |
| *TRACKER_EXECUTION_ORDER*       | The order in which different tracker types should be executed, at least one must be selected (case-insensitive)                                                                                          | headless,manual,cloudflare-check |
| *TRACKER_INPUT_FILE_PATH*       | The path to the input tracker definition CSV file (inside the docker container)                                                                                                                          | /tmp/screenshots/trackers.csv    |

## Versioning

This project follows [Semantic Versioning](https://semver.org/) (`MAJOR.MINOR.PATCH`):

- **MAJOR**: incompatible changes to the public API (e.g. removed or renamed configuration options, changed CSV format)
- **MINOR**: backwards-compatible new functionality added to the public API (e.g. new configuration options)
- **PATCH**: backwards-compatible bug fixes or code changes

Adding or removing tracker support is **not** considered a `MINOR` change. Trackers are external to the application and
not part of its public API, as their availability depends on third-party sites that can change or disappear at any time.
Tracker additions and removals will be released as `PATCH` versions.

## Contributing

### Requirements

- [Apache Maven](https://maven.apache.org/download.cgi)
- [Docker](https://docs.docker.com/engine/install/)
- [Google Chrome](https://www.google.com/chrome/)
- [Java](https://jdk.java.net/25/)
- [Python](https://www.python.org/downloads/release/python-314/)

### Install Git Hooks

Run the following command to run git hooks for the project:

```bash
bash ./ci/hooks/setup-hooks.sh
```

### Debugging Application

If `TRACKER_EXECUTION_ORDER` contains **cloudflare-check**, then Python must be configured for your environment. From
the root directory, execute the following:

```bash
source venv/bin/activate
pip install -r ./python/requirements.txt
./venv/bin/python ./python/selenium_manager.py
```

Using IntelliJ, and click on **Run**> **Edit Configurations** and add the environment variables for the application.
Once done, open
the [ApplicationLauncher.java](./tracker-profiles-screenshots/src/main/java/net/zodac/tracker/ApplicationLauncher.java)
and run the `main` method from the IDE.
The [AbstractTrackerHandler.java](./tracker-profiles-screenshots/src/main/java/net/zodac/tracker/handler/AbstractTrackerHandler.java)
implementation for each tracker is retrieved by the *trackerName* field within the CSV file.

[Selenium WebDriver](https://www.selenium.dev/documentation/webdriver/) is used to leverage the Chromium web browser to
take screenshots. While the application usually runs in headless mode, this can be changed by updating the
`FORCE_UI_BROWSER` value in the [configuration](#configuration-options). This will cause a new browser instance to
launch when taking a screenshot, and can be used for debugging a new implementation.

### Building And Developing In Docker

Below is the command to build and run the development docker image with everything enabled (requires [the UI to be
defined](#browser-ui)):

```bash
docker build -f ./docker/Dockerfile -t tracker-profiles-dev . &&
docker run \
    --env DISPLAY="${DISPLAY}" \
    --env BROWSER_HEIGHT=1050 \
    --env BROWSER_WIDTH=1680 \
    --env CSV_COMMENT_SYMBOL='#' \
    --env ENABLE_ADULT_TRACKERS=true \
    --env ENABLE_TRANSLATION_TO_ENGLISH=true \
    --env FORCE_UI_BROWSER=true \
    --env INPUT_TIMEOUT_ENABLED=true \
    --env INPUT_TIMEOUT_SECONDS=300 \
    --env LOG_LEVEL=TRACE \
    --env NUMBER_OF_TRACKER_ATTEMPTS=5 \
    --env OUTPUT_DIRECTORY_NAME_FORMAT=yyyy-MM-dd \
    --env OUTPUT_DIRECTORY_PARENT_PATH=/app/screenshots \
    --env REDACTION_TYPE=NONE,BLUR,BOX,TEXT \
    --env SCREENSHOT_EXISTS_ACTION=CREATE_ANOTHER \
    --env TAKE_SCREENSHOT_ON_ERROR=true \
    --env TIMEZONE=UTC \
    --env TRACKER_EXECUTION_ORDER=headless,manual,cloudflare-check \
    --env TRACKER_INPUT_FILE_PATH=/app/screenshots/trackers.csv \
    -v /tmp/.X11-unix:/tmp/.X11-unix \
    -v /tmp/screenshots:/app/screenshots \
    --name tracker-profiles-dev \
    --rm tracker-profiles-dev
```

### Implementing Support For New Trackers

All supported private trackers have an implementation found in
the [handler](./tracker-profiles-screenshots/src/main/java/net/zodac/tracker/handler) package. To add a new one,
extend [AbstractTrackerHandler.java](./tracker-profiles-screenshots/src/main/java/net/zodac/tracker/handler/AbstractTrackerHandler.java),
following the convention from an existing implementation
like [AbTorrents.java](./tracker-profiles-screenshots/src/main/java/net/zodac/tracker/handler/AbTorrents.java).

Ensure the [TrackerType](./tracker-profiles-screenshots/src/main/java/net/zodac/tracker/framework/TrackerType.java) is
set correctly for your tracker.

### Cloudflare Trackers

The `Cloudflare-check` trackers listed in [Trackers> Non-Headless](#non-headless) are implemented differently from the
other trackers, since this verification check cannot be passed using stock
Selenium. [undetected-chromedriver](https://github.com/ultrafunkamsterdam/undetected-chromedriver) is used to create a
web browser that is capable of bypassing Cloudflare detection.

Unfortunately, this is a Python-only package. While a *reasonable* person would migrate the project to Python, I'd
prefer to keep writing this in Java. So a [Python web-server](./python/selenium_manager/server.py) is spun up that
exposes endpoints to open/close a Selenium web browser that can bypass detection. There is a Java implementation of
the [Selenium WebDriver class](./tracker-profiles-screenshots/src/main/java/net/zodac/tracker/framework/driver/python/AttachedRemoteWebDriver.java)
which can attach to the Selenium browser that was launched by Python.

This is all handled by the framework, so an implementation of a tracker can be done following
like [any other tracker](#implementing-support-for-new-trackers), without needing to worry about whether the browser is
launched by Java or Python.
