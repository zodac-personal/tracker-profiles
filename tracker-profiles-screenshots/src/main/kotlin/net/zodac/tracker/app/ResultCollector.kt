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

package net.zodac.tracker.app

import net.zodac.tracker.framework.ExitState
import net.zodac.tracker.framework.TrackerType
import net.zodac.tracker.util.StringUtils
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.EnumMap
import java.util.TreeSet

/**
 * Collects and reports on the results of screenshot attempts.
 */
internal class ResultCollector {

    private val successfulTrackers: MutableMap<TrackerType, MutableCollection<String>> = EnumMap(TrackerType::class.java)
    private val unsuccessfulTrackers: MutableMap<TrackerType, MutableCollection<String>> = EnumMap(TrackerType::class.java)

    /**
     * Records the result of a screenshot attempt.
     *
     * @param trackerType   the type of tracker
     * @param trackerName   the name of the tracker
     * @param wasSuccessful whether the screenshot was successful
     */
    fun addResult(trackerType: TrackerType, trackerName: String, wasSuccessful: Boolean) {
        val targetMap = if (wasSuccessful) successfulTrackers else unsuccessfulTrackers
        targetMap.getOrPut(trackerType) { TreeSet() }.add(trackerName)
    }

    /**
     * Generates a summary of all results and returns the appropriate [ExitState].
     *
     * @param trackerExecutionOrder the execution order of the [TrackerType]s
     * @return the [ExitState] based on success/failure counts
     */
    fun generateSummary(trackerExecutionOrder: Set<TrackerType>): ExitState {
        val totalSuccessful = successfulTrackers.values.sumOf { it.size }
        val totalUnsuccessful = unsuccessfulTrackers.values.sumOf { it.size }

        if (totalSuccessful == 0 && totalUnsuccessful == 0) {
            LOGGER.error("")
            LOGGER.error("Unexpectedly had no trackers execute")
            return ExitState.FAILURE
        }

        if (totalSuccessful == 0) {
            LOGGER.error("")
            LOGGER.error("{} selected tracker{} failed:", totalUnsuccessful, StringUtils.pluralise(totalUnsuccessful))
            unsuccessfulTrackers.forEach { (type, trackers) ->
                LOGGER.error("- {}:", type.formattedName())
                trackers.forEach { LOGGER.error("\t- {}", it) }
            }
            return ExitState.FAILURE
        }

        if (totalUnsuccessful == 0) {
            LOGGER.info("")
            LOGGER.info("{} selected tracker{} successfully screenshot", totalSuccessful, StringUtils.pluralise(totalSuccessful))
            successfulTrackers.forEach { (type, trackers) ->
                LOGGER.debug("- {} ({}): {}", type.formattedName(), trackers.size, trackers)
            }
            return ExitState.SUCCESS
        }

        // TODO: Print the number of successful/failed
        LOGGER.warn("")
        LOGGER.warn("Failures for following tracker{}:", StringUtils.pluralise(totalUnsuccessful))
        for (trackerType in trackerExecutionOrder) {
            val trackers = unsuccessfulTrackers[trackerType]
            if (trackers.isNullOrEmpty()) continue
            LOGGER.warn("- {}:", trackerType.formattedName())
            trackers.forEach { LOGGER.warn("\t- {}", it) }
        }

        return ExitState.PARTIAL_FAILURE
    }

    private companion object {
        private val LOGGER: Logger = LogManager.getLogger()
    }
}
