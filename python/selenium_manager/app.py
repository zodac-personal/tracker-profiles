#!/usr/bin/env python
# SPDX-License-Identifier: 0BSD
# Copyright (c) 2024-2026 zodac.net

"""Application factory for the Flask Selenium session manager."""

from flask import Flask

from .logging_config import configure_logging
from .routes import register_routes


def create_app() -> Flask:
    """Create and configure the Flask application instance.

    This function:
    - Initialises the Flask app.
    - Sets up structured logging using `configure_logging()`.
    - Registers all route handlers with `register_routes()`.

    Returns:
        A fully configured Flask application instance.
    """
    app = Flask(__name__)
    configure_logging()
    register_routes(app)
    return app
