#!/usr/local/bin/python3
# SPDX-License-Identifier: 0BSD
# Copyright (c) 2024-2026 zodac.net

"""Patch a Chromedriver binary using undetected-chromedriver."""

import undetected_chromedriver as uc


def main() -> None:
    """Patch the Chromedriver executable in place."""
    driver_path = "/usr/local/chromium/chromedriver-linux64/chromedriver"

    patcher = uc.Patcher(executable_path=driver_path)
    patcher.patch_exe()


if __name__ == "__main__":
    main()
