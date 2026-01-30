# SPDX-License-Identifier: 0BSD
# Copyright (c) 2024-2026 zodac.net

"""Custom logging configuration for the application.

Defines and installs a TRACE log level, colored output using colorlog,
and timezone-aware timestamp formatting.
"""

import logging
import os
import typing
from datetime import datetime, timezone
from logging import LogRecord
from zoneinfo import ZoneInfo

from colorlog import ColoredFormatter


def configure_logging() -> None:
    """Configure the root logger to match the Java application logging output.

    This function:
    - Defines a custom TRACE log level (numerically lower than DEBUG).
    - Uses `colorlog` for colored console logging.
    - Formats timestamps according to the `TIMEZONE` environment variable if set.
    - Sets the root logger's level from the `LOG_LEVEL` environment variable (default: INFO).
    - Outputs logs to the console with millisecond resolution timestamps.
    """
    trace_level_index: int = 5
    logging.addLevelName(trace_level_index, "TRACE")

    def trace(self: logging.Logger, message: str, *args: object, **kwargs: object) -> None:
        if self.isEnabledFor(trace_level_index):
            self._log(trace_level_index, message, args, **kwargs)

    logging.Logger.trace = trace
    date_format = "%Y-%m-%d %H:%M:%S.%f"

    class CustomFormatter(ColoredFormatter):
        @staticmethod
        def formatTime(record: LogRecord, datefmt: str | None = None) -> str:  # NOQA: N802 - Overriding function in logging.Formatter
            tz_name = os.getenv("TIMEZONE")
            local_tz = ZoneInfo(tz_name) if tz_name else timezone.UTC
            t = datetime.fromtimestamp(record.created, tz=local_tz)
            s = t.strftime(datefmt or date_format)
            return s[:-3]  # milliseconds

    log_format: str = (
        "%(asctime)s "
        "[%(log_color)s%(levelname)-5s%(reset)s] "
        "%(message)s"
    )

    log_colours = {
        "TRACE":    "bold_purple",
        "DEBUG":    "bold_green",
        "INFO":     "bold_blue",
        "WARNING":  "bold_yellow",
        "ERROR":    "bold_red",
        "CRITICAL": "bold_red",
    }

    formatter = CustomFormatter(
        log_format,
        datefmt=date_format,
        log_colors=log_colours,
    )

    wanted_log_level = os.getenv("LOG_LEVEL", "INFO").upper()
    handler = logging.StreamHandler()
    handler.setLevel(wanted_log_level)
    handler.setFormatter(formatter)

    root_logger = logging.getLogger()
    root_logger.setLevel(wanted_log_level)
    root_logger.handlers = [handler]

    # Intercept the patching logs from undetected_chromedriver and forces them to TRACE level
    class OverridePatcherLogs(logging.Filter):
        @typing.override
        def filter(self, record: logging.LogRecord) -> bool:
            if record.levelno <= logging.INFO:
                record.levelno = trace_level_index
                record.levelname = "TRACE"
            return True
    uc_logger = logging.getLogger("undetected_chromedriver.patcher")
    uc_logger.addFilter(OverridePatcherLogs())

    logging.getLogger(__name__).trace("Logging is configured")
