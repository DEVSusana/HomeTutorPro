import os
import sys

# Ensure package directory is in sys.path whether executed as module or script
pkg_dir = os.path.dirname(os.path.abspath(__file__))
if pkg_dir not in sys.path:
    sys.path.insert(0, pkg_dir)

try:
    from agents_dev.cli import main
except ModuleNotFoundError:
    from cli import main

if __name__ == "__main__":
    main()

