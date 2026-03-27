#!/usr/local/bin/python3
import sys

try:
    import undetected_chromedriver as uc
except ImportError:
    print("undetected-chromedriver is not installed", file=sys.stderr)
    sys.exit(1)

def main():
    driver_path = sys.argv[1] if len(sys.argv) > 1 else "/usr/local/chromium/chromedriver-linux64/chromedriver"

    print(f"Patching chromedriver at: {driver_path}")

    patcher = uc.Patcher(executable_path=driver_path)
    patcher.patch_exe()

    print("Patched successfully")


if __name__ == "__main__":
    main()
