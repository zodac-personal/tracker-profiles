/*
 * BSD Zero Clause License
 *
 * Copyright (c) 2024-2026 zodac.net
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package net.zodac.tracker.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import javax.imageio.ImageIO;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;
import ru.yandex.qatools.ashot.shooting.ShootingStrategy;

/**
 * Utility class used to take a screenshot of a website.
 */
// TODO: Should this exist as a field variable inside the AbstractTrackerHandler?
public final class ScreenshotTaker {

    private static final Duration TIME_BETWEEN_SCROLLS = Duration.ofMillis(500L);

    private ScreenshotTaker() {

    }

    /**
     * Checks how many screenshots already exist for the given base name in the given directory. The base name is either the tracker name alone (for
     * {@link net.zodac.tracker.framework.config.RedactionType#NONE}) or the tracker name with the redaction type appended (e.g.
     * {@code trackerName_Text}).
     *
     * <p>
     * A file is counted if it matches exactly {@code baseName.png} or {@code baseName_N.png} where {@code N} is a positive integer. This avoids
     * counting files for other redaction types that share the same tracker name prefix.
     *
     * @param baseName  the base file name to match against (tracker name, with optional redaction type suffix)
     * @param directory the directory in which to check for existing screenshots
     * @return the number of screenshots for the base name that already exist in the given directory
     */
    public static int howManyScreenshotsAlreadyExist(final String baseName, final Path directory) {
        final File outputDir = directory.toAbsolutePath().toFile();
        final File[] matchingFiles = outputDir.listFiles((_, name) -> {
            if (!name.endsWith(".png")) {
                return false;
            }
            final String nameWithoutExtension = name.substring(0, name.length() - ".png".length());
            if (nameWithoutExtension.equals(baseName)) {
                return true;
            }
            if (!nameWithoutExtension.startsWith(baseName + "_")) {
                return false;
            }
            final String indexSuffix = nameWithoutExtension.substring(baseName.length() + 1);
            return !indexSuffix.isEmpty() && indexSuffix.chars().allMatch(Character::isDigit);
        });

        return matchingFiles == null ? 0 : matchingFiles.length;
    }

    /**
     * Takes a screenshot of the current web page loaded by the {@link RemoteWebDriver}. The browser viewport is then saved as a {@code .png} file in
     * the provided {@code outputDirectory}. The file name will be {@code trackerName.png}.
     *
     * <p>
     * Once the screenshot is saved, the page is scrolled back to the top. This is to ensure that any elements at the top of the page are clickable
     * after scrolling.
     *
     * @param driver                 the {@link RemoteWebDriver} with the loaded web page
     * @param outputDirectory        the directory in which the screenshot should be saved
     * @param baseName               the base file name for the screenshot (tracker name, with optional redaction type suffix)
     * @param scrollDuringScreenshot whether to scroll the profile page during the screenshot
     * @param index                  how many screenshots already exist for this base name
     * @return the {@link File} instance of the saved screenshot
     * @throws IOException thrown if an error occurs saving the screenshot to the file system
     * @see BrowserInteractionHelper#scrollToTheTop()
     */
    public static File takeScreenshot(final RemoteWebDriver driver,
                                      final Path outputDirectory,
                                      final String baseName,
                                      final boolean scrollDuringScreenshot,
                                      final int index
    ) throws IOException {
        final BrowserInteractionHelper browserInteractionHelper = new BrowserInteractionHelper(driver);
        final BufferedImage screenshotImage = takeScreenshotOfEntirePage(driver, browserInteractionHelper, scrollDuringScreenshot);
        final File screenshot = createOutputFileHandle(outputDirectory.toAbsolutePath(), baseName, index);
        ImageIO.write(screenshotImage, "PNG", screenshot);
        browserInteractionHelper.scrollToTheTop();
        return screenshot;
    }

    private static File createOutputFileHandle(final Path outputDirectory, final String baseName, final int index) {
        if (index == 0) {
            return new File(outputDirectory + File.separator + baseName + ".png");
        }

        return new File(outputDirectory + File.separator + baseName + "_" + index + ".png");
    }

    private static BufferedImage takeScreenshotOfEntirePage(final RemoteWebDriver driver,
                                                            final BrowserInteractionHelper browserInteractionHelper,
                                                            final boolean scrollDuringScreenshot
    ) {
        browserInteractionHelper.disableScrolling();
        final BufferedImage screenshot = new AShot()
            .shootingStrategy(shootingStrategy(scrollDuringScreenshot))
            .takeScreenshot(driver)
            .getImage();

        browserInteractionHelper.enableScrolling("body");
        return screenshot;
    }

    private static ShootingStrategy shootingStrategy(final boolean scrollDuringScreenshot) {
        return scrollDuringScreenshot
            ? ShootingStrategies.viewportPasting(((Long) TIME_BETWEEN_SCROLLS.toMillis()).intValue())
            : ShootingStrategies.simple();
    }
}
