package pl.variant.itemblocker.log;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * Posts alerts to a Discord webhook without blocking the server.
 */
final class DiscordWebhook {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final long WARNING_INTERVAL_MILLIS = 60_000;

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final Logger logger;
    private volatile long lastWarning;

    DiscordWebhook(Logger logger) {
        this.logger = logger;
    }

    void send(String url, LogEntry entry) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload(entry)))
                    .build();
        } catch (IllegalArgumentException exception) {
            warn("The Discord webhook URL is not valid: " + exception.getMessage());
            return;
        }

        client.sendAsync(request, HttpResponse.BodyHandlers.discarding()).whenComplete((response, error) -> {
            if (error != null) {
                warn("Could not reach the Discord webhook: " + error.getMessage());
            } else if (response.statusCode() >= 300) {
                warn("The Discord webhook answered with HTTP " + response.statusCode());
            }
        });
    }

    /**
     * The message as webhook JSON. Mentions are turned off, so an item named {@code @everyone}
     * cannot ping anyone.
     */
    static String payload(LogEntry entry) {
        String content = "**" + markdown(entry.player()) + "** - " + markdown(entry.action()) + " **" + markdown(entry.subject())
                + "** in `" + code(entry.world()) + "` (preset `" + code(entry.preset()) + "`)";
        return "{\"content\":\"" + json(content) + "\",\"allowed_mentions\":{\"parse\":[]}}";
    }

    private static String markdown(String text) {
        return text.replaceAll("([\\\\*_~`|>#\\[\\]()])", "\\\\$1");
    }

    private static String code(String text) {
        return text.replace("`", "'");
    }

    private static String json(String text) {
        StringBuilder escaped = new StringBuilder();
        for (char character : text.toCharArray()) {
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private void warn(String message) {
        long now = System.currentTimeMillis();
        if (now - lastWarning > WARNING_INTERVAL_MILLIS) {
            lastWarning = now;
            logger.warning(message);
        }
    }
}
