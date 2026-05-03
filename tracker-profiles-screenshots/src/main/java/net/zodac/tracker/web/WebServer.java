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

package net.zodac.tracker.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.zodac.tracker.framework.ExitState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Embedded HTTP server providing the web UI on port {@value PORT}.
 *
 * <p>
 * Three endpoints are served:
 * <ul>
 *   <li>{@code GET /} — the HTML control page</li>
 *   <li>{@code POST /api/start} — triggers screenshot execution</li>
 *   <li>{@code GET /api/log} — Server-Sent Events stream of log output</li>
 * </ul>
 */
public final class WebServer {

    private static final int PORT = 8080;
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final Logger LOGGER = LogManager.getLogger();

    private WebServer() {
    }

    /**
     * Starts the HTTP server on port {@value PORT} and blocks the calling thread until the JVM is shut down.
     *
     * @param execution the {@link Runnable} called in a virtual thread when the user clicks Start
     */
    public static void start(final Runnable execution) {
        try {
            final HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/", exchange -> handleRoot(exchange));
            server.createContext("/api/start", exchange -> handleStart(exchange, execution));
            server.createContext("/api/log", WebServer::handleLog);
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.start();
            LOGGER.info("Web UI available at http://localhost:{}", PORT);
            Thread.currentThread().join();  // block until JVM is shut down
        } catch (final IOException e) {
            LOGGER.error("Failed to start web server on port {}: {}", PORT, e.getMessage());
            System.exit(ExitState.FAILURE.exitCode());  // NOPMD: DoNotTerminateVM - web server is required for operation
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void handleRoot(final HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }
        if (!"/".equals(exchange.getRequestURI().getPath())) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }
        final byte[] body = loadResource("web/index.html");
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static void handleStart(final HttpExchange exchange, final Runnable execution) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }
        if (!RUNNING.compareAndSet(false, true)) {
            sendJson(exchange, 409, "{\"error\":\"Execution already running\"}");
            return;
        }
        LogBroadcaster.clear();
        Thread.ofVirtual().name("execution").start(() -> {
            try {
                execution.run();
            } finally {
                RUNNING.set(false);
                LogBroadcaster.signalDone();
            }
        });
        sendJson(exchange, 202, "{\"status\":\"started\"}");
    }

    private static void handleLog(final HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.sendResponseHeaders(200, 0);
        final LogBroadcaster.Subscription sub = LogBroadcaster.subscribe();
        try (OutputStream os = exchange.getResponseBody()) {
            streamLogs(os, sub);
        } finally {
            sub.unsubscribe();
        }
    }

    private static void streamLogs(final OutputStream os, final LogBroadcaster.Subscription sub) throws IOException {
        while (true) {
            final String message;
            try {
                message = sub.queue().poll(1L, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (message == null) {
                continue;
            }
            if (LogBroadcaster.DONE_SENTINEL.equals(message)) {
                os.write("event: done\ndata: \n\n".getBytes(StandardCharsets.UTF_8));
                os.flush();
                return;
            }
            os.write(toSseMessage(message));
            os.flush();
        }
    }

    private static byte[] toSseMessage(final String message) {
        final String trimmed = message.endsWith("\n") ? message.substring(0, message.length() - 1) : message;
        return ("data: " + trimmed.replace("\n", "\ndata: ") + "\n\n").getBytes(StandardCharsets.UTF_8);
    }

    private static void sendJson(final HttpExchange exchange, final int status, final String json) throws IOException {
        final byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static byte[] loadResource(final String path) {
        final ClassLoader cl = WebServer.class.getClassLoader();
        if (cl == null) {
            throw new IllegalStateException("No classloader available to load: '%s'".formatted(path));
        }
        try (InputStream is = cl.getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalStateException("Resource not found: '%s'".formatted(path));
            }
            return is.readAllBytes();
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to load resource: '%s'".formatted(path), e);
        }
    }
}
