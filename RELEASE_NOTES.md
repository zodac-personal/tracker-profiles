## Parallel Execution

I've added the ability to run all HEADLESS in parallel. By default `NUMBER_OF_PARALLEL_THREADS` is set to **5**, with a max value of **32**.

This is only for HEADLESS browsers since multiple MANUAL trackers waiting for input might just get too messy. Might re-evaluate that if there are enough requests.
