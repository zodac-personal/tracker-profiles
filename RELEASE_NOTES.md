# Backwards Incompatible Changes

## Changing `NUMBER_OF_TRACKER_ATTEMPTS` option to `NUMBER_OF_SCREENSHOT_ATTEMPTS`

I think the original variable name was a little ambiguous, so I'm making it a little clearer. The README still gives more details.

## Removing Tracker Support

Removing support for the following trackers:

- InfinityLibrary

# Other changes

## Parallel Execution

I've added the ability to run all HEADLESS in parallel. By default `NUMBER_OF_PARALLEL_THREADS` is set to **5**, with a max value of **32**.

This is only for HEADLESS browsers since multiple MANUAL trackers waiting for input might just get too messy. Might re-evaluate that if there are enough requests.
