# SPDX-License-Identifier: 0BSD
# Copyright (c) 2024-2026 zodac.net

"""Configuration utilities for Chrome browser options.

This module defines helper functions to create and configure ChromeOptions
for use with undetected-chromedriver.
"""

import logging
import uuid
from pathlib import Path
from typing import Any

import undetected_chromedriver as uc

logger = logging.getLogger(__name__)


def create_chrome_options(browser_data_storage_path: str, browser_dimensions: str, enable_translation: bool) -> uc.ChromeOptions:
    """Create and configure ChromeOptions for launching an undetected Chrome browser.

    Args:
        browser_data_storage_path (str): Path to store user data and cache.
        browser_dimensions (str): Browser window size in the format 'WIDTH,HEIGHT'.
        enable_translation (bool): Whether to translate web pages into English
    Returns:
        uc.ChromeOptions: Configured Chrome options.
    """
    chrome_options = uc.ChromeOptions()

    browser_data_storage_path = Path(browser_data_storage_path)
    user_data_dir = browser_data_storage_path / f"session_{uuid.uuid4()}"
    disk_cache_dir = browser_data_storage_path / "selenium"

    chrome_options.add_argument(f"--window-size={browser_dimensions}")
    chrome_options.add_argument(f"--disk-cache-dir={disk_cache_dir}")
    chrome_options.add_argument(f"--user-data-dir={user_data_dir}")
    chrome_options.add_argument("--no-sandbox")
    chrome_options.add_argument("--disable-dev-shm-usage")
    chrome_options.add_argument("--disable-gpu")
    chrome_options.add_argument("--disable-notifications")
    chrome_options.add_argument("--disable-blink-features=AutomationControlled")
    chrome_options.add_argument("--ignore-certificate-errors")

    driver_preferences: dict[str, Any] = {
        # Disable password manager pop-ups
        "credentials_enable_service": False,
        "profile.password_manager_enabled": False
    }

    if enable_translation:  # Adjust to match your config method
        chrome_options.add_argument("--lang=en")
        driver_preferences["intl.accept_languages"] = "en,en_US"
        driver_preferences["translate_accepted_count"] = 1
        driver_preferences["translate.enabled"] = True

        # Always translate to English
        translate_whitelists = {}

        # Base languages
        base_languages = [
            "af", "sq", "am", "ar", "hy", "az", "eu", "be", "bn", "bs",
            "bg", "ca", "ceb", "ny", "zh", "co", "hr", "cs",
            "da", "nl", "eo", "et", "tl", "fi", "fr", "fy", "gl", "ka",
            "de", "el", "gu", "ht", "ha", "haw", "he", "iw", "hi", "hmn",
            "hu", "is", "ig", "id", "ga", "it", "ja", "jw", "kn", "kk",
            "km", "ko", "ku", "ky", "lo", "la", "lv", "lt", "lb", "mk",
            "mg", "ms", "ml", "mt", "mi", "mr", "mn", "my", "ne", "no",
            "ps", "fa", "pl", "pt", "pa", "ro", "ru", "sm", "gd", "sr",
            "st", "sn", "sd", "si", "sk", "sl", "so", "es", "su", "sw",
            "sv", "tg", "ta", "te", "th", "tr", "uk", "ur", "uz", "vi",
            "cy", "xh", "yi", "yo", "zu"
        ]

        for base_language in base_languages:
            translate_whitelists[base_language] = "en"

        driver_preferences["translate_whitelists"] = translate_whitelists

    chrome_options.add_experimental_option("prefs", driver_preferences)

    logger.debug("Creating driver with following options: %s", chrome_options)
    return chrome_options
