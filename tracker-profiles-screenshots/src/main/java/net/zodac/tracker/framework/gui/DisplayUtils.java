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

package net.zodac.tracker.framework.gui;

import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import net.zodac.tracker.framework.exception.CancelledInputException;
import net.zodac.tracker.framework.exception.NoUserInputException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class used to display pop-up windows and confirmation boxes to the user.
 */
public final class DisplayUtils {

    /**
     * The {@link Duration} the program will wait for a user to enter an input.
     */
    // TODO: Make this optional, and just allow it to run indefinitely otherwise?
    public static final Duration INPUT_WAIT_DURATION = Duration.ofMinutes(5L);

    private static final Logger LOGGER = LogManager.getLogger();

    // UI element constants
    private static final String BUTTON_CONTINUE_TEXT = "Continue";
    private static final String BUTTON_EXIT_TEXT = "Exit";
    private static final String LABEL_SUFFIX = String.format(", then click '%s' below", BUTTON_CONTINUE_TEXT);
    private static final String TITLE_SUFFIX = " Manual Input";
    private static final int DIALOG_BOX_HEIGHT = 125;
    private static final int DIALOG_BOX_WIDTH = 500;

    private static final ScheduledExecutorService TIMEOUT_SCHEDULER = Executors.newSingleThreadScheduledExecutor(runnable -> {
        final Thread t = new Thread(runnable, "dialog-timeout");
        t.setDaemon(true);
        return t;
    });

    private DisplayUtils() {

    }

    /**
     * Creates a pop-up on the screen for the user to click to confirm a user input has been provided to the loaded tracker.
     *
     * @param titlePrefix the title for the pop-up
     * @param labelPrefix the text for the pop-up
     */
    public static void userInputConfirmation(final String titlePrefix, final String labelPrefix) {
        setStyleToSystemTheme();

        final AtomicBoolean userProvidedInput = new AtomicBoolean(false);
        final AtomicBoolean timedOut = new AtomicBoolean(false);

        final JDialog dialog = createDialog(titlePrefix, labelPrefix, userProvidedInput);
        showDialog(dialog, userProvidedInput, timedOut);
    }

    private static JDialog createDialog(final String titlePrefix, final String labelPrefix, final AtomicBoolean userProvidedInput) {
        final JDialog dialog = new JDialog((Frame) null, titlePrefix + TITLE_SUFFIX, true);
        dialog.setLayout(new BorderLayout());
        dialog.setAlwaysOnTop(true);  // Ensure the dialog remains on top of all windows when interacting with browser

        final JPanel panel = new JPanel(new GridLayout(2, 1));
        final String labelText = "<html>" + labelPrefix + LABEL_SUFFIX + "</html>"; // Wrap text as HTML so .pack() can resize dynamically
        panel.add(new JLabel(labelText, SwingConstants.CENTER));

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(createButtons(dialog, userProvidedInput), BorderLayout.PAGE_END);

        dialog.setPreferredSize(new Dimension(DIALOG_BOX_WIDTH, DIALOG_BOX_HEIGHT));
        dialog.pack();  // Respects preferredSize but allows it to be larger if needed based on the text
        dialog.setLocationRelativeTo(null);

        setDialogPosition(dialog);
        return dialog;
    }

    private static JPanel createButtons(final JDialog dialog, final AtomicBoolean userProvidedInput) {
        final JButton continueButton = new JButton(BUTTON_CONTINUE_TEXT);
        continueButton.addActionListener(_ -> {
            userProvidedInput.set(true);
            dialog.dispose();
        });

        final JButton exitButton = new JButton(BUTTON_EXIT_TEXT);
        exitButton.addActionListener(_ -> dialog.dispose());

        final JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(continueButton);
        buttonPanel.add(exitButton);
        return buttonPanel;
    }

    // TODO: Find old code that included a timer in the window
    private static void showDialog(final JDialog dialog, final AtomicBoolean userProvidedInput, final AtomicBoolean timedOut) {
        final var timeoutTask = TIMEOUT_SCHEDULER.schedule(
            () -> SwingUtilities.invokeLater(() -> {
                if (dialog.isShowing()) {
                    timedOut.set(true);
                    dialog.dispose();
                }
            }),
            INPUT_WAIT_DURATION.getSeconds(),
            TimeUnit.SECONDS
        );
        LOGGER.trace("Waiting {} for user input", INPUT_WAIT_DURATION);

        try {
            SwingUtilities.invokeAndWait(() -> dialog.setVisible(true));

            if (userProvidedInput.get()) {
                LOGGER.trace("Received an input");
                return;
            }

            if (timedOut.get()) {
                LOGGER.trace("Dialog timed out and no input received");
                throw new NoUserInputException(INPUT_WAIT_DURATION, new IllegalStateException());
            }

            LOGGER.trace("Dialog still running and no timeout received, assuming the user cancelled execution");
            throw new CancelledInputException();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NoUserInputException(INPUT_WAIT_DURATION, e);
        } catch (final InvocationTargetException e) {
            throw new NoUserInputException(INPUT_WAIT_DURATION, e.getCause() == null ? e : e.getCause());
        } finally {
            timeoutTask.cancel(true);
            dialog.dispose();
        }
    }

    private static void setDialogPosition(final JDialog dialog) {
        // Get the screen the dialog will appear on
        final GraphicsConfiguration gc = dialog.getGraphicsConfiguration();
        final Rectangle bounds = gc.getBounds();
        final int margin = 40;

        // X is left + margin, Y is vertically centered
        final int x = bounds.x + margin;
        final int y = bounds.y + (bounds.height - dialog.getHeight()) / 2;
        dialog.setLocation(x, y);
    }

    private static void setStyleToSystemTheme() {
        try {
            FlatDarkLaf.setup();
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (final UnsupportedLookAndFeelException e) {
            LOGGER.debug("Unexpected error setting UI style", e);
        }
    }
}
