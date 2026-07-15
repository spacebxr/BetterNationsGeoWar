package com.geowar.discord;

import com.geowar.GeoWarPlugin;
import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class DiscordWebhookService {

    private final String warWebhookUrl;
    private final String peaceWebhookUrl;
    private final String treatyWebhookUrl;
    private final String serverName;

    public DiscordWebhookService() {
        warWebhookUrl = GeoWarPlugin.getInstance().getConfig().getString("discord.war-webhook-url", "");
        peaceWebhookUrl = GeoWarPlugin.getInstance().getConfig().getString("discord.peace-webhook-url", "");
        treatyWebhookUrl = GeoWarPlugin.getInstance().getConfig().getString("discord.treaty-webhook-url", "");
        serverName = GeoWarPlugin.getInstance().getConfig().getString("discord.server-name", "Minecraft Server");
    }

    public void sendWarDeclaration(String attacker, String defender, String reason, String demands, String declarerName) {
        if (warWebhookUrl.isEmpty() || warWebhookUrl.equals("YOUR_WEBHOOK_URL_HERE")) return;

        String threadName = "War: " + attacker + " vs " + defender;
        String timestamp = Instant.now().toString();

        String json = "{"
            + "\"thread_name\": \"" + escapeJson(threadName) + "\","
            + "\"embeds\": [{"
            + "\"title\": \"War Declaration\","
            + "\"description\": \"**" + escapeJson(attacker) + "** has declared war on **" + escapeJson(defender) + "**.\","
            + "\"color\": 16711680,"
            + "\"fields\": ["
            + "{\"name\": \"Declaring Nation\", \"value\": \"" + escapeJson(attacker) + "\", \"inline\": true},"
            + "{\"name\": \"Target Nation\", \"value\": \"" + escapeJson(defender) + "\", \"inline\": true},"
            + "{\"name\": \"Declared By\", \"value\": \"" + escapeJson(declarerName) + "\", \"inline\": true},"
            + "{\"name\": \"Reason\", \"value\": \"" + escapeJson(reason) + "\", \"inline\": false},"
            + "{\"name\": \"Demands\", \"value\": \"" + escapeJson(demands) + "\", \"inline\": false}"
            + "],"
            + "\"footer\": {\"text\": \"" + escapeJson(serverName) + "\"},"
            + "\"timestamp\": \"" + timestamp + "\""
            + "}]"
            + "}";

        sendAsync(warWebhookUrl, json, "war declaration");
    }

    public void sendPeaceAgreement(String nationA, String nationB, String signerName) {
        if (peaceWebhookUrl.isEmpty() || peaceWebhookUrl.equals("YOUR_WEBHOOK_URL_HERE")) return;

        String threadName = "Peace: " + nationA + " & " + nationB;
        String timestamp = Instant.now().toString();

        String json = "{"
            + "\"thread_name\": \"" + escapeJson(threadName) + "\","
            + "\"embeds\": [{"
            + "\"title\": \"Peace Agreement Signed\","
            + "\"description\": \"**" + escapeJson(nationA) + "** and **" + escapeJson(nationB) + "** have ended their war.\","
            + "\"color\": 65280,"
            + "\"fields\": ["
            + "{\"name\": \"Nations\", \"value\": \"" + escapeJson(nationA) + " and " + escapeJson(nationB) + "\", \"inline\": true},"
            + "{\"name\": \"Signed By\", \"value\": \"" + escapeJson(signerName) + "\", \"inline\": true}"
            + "],"
            + "\"footer\": {\"text\": \"" + escapeJson(serverName) + "\"},"
            + "\"timestamp\": \"" + timestamp + "\""
            + "}]"
            + "}";

        sendAsync(peaceWebhookUrl, json, "peace agreement");
    }

    public void sendTreatyLogged(String nationA, String nationB, String type, String terms, String signerName) {
        if (treatyWebhookUrl.isEmpty() || treatyWebhookUrl.equals("YOUR_WEBHOOK_URL_HERE")) return;

        String threadName = "Treaty: " + nationA + " & " + nationB + " [" + type + "]";
        String timestamp = Instant.now().toString();

        int color = switch (type) {
            case "Alliance" -> 5763719;
            case "Non-Aggression Pact" -> 3447003;
            case "Trade Agreement" -> 16776960;
            case "Vassalage" -> 10181046;
            default -> 9807270;
        };

        String json = "{"
            + "\"thread_name\": \"" + escapeJson(threadName) + "\","
            + "\"embeds\": [{"
            + "\"title\": \"Treaty Signed: " + escapeJson(type) + "\","
            + "\"description\": \"**" + escapeJson(nationA) + "** and **" + escapeJson(nationB) + "** have signed a " + escapeJson(type) + ".\","
            + "\"color\": " + color + ","
            + "\"fields\": ["
            + "{\"name\": \"Nations\", \"value\": \"" + escapeJson(nationA) + " & " + escapeJson(nationB) + "\", \"inline\": true},"
            + "{\"name\": \"Type\", \"value\": \"" + escapeJson(type) + "\", \"inline\": true},"
            + "{\"name\": \"Signed By\", \"value\": \"" + escapeJson(signerName) + "\", \"inline\": true},"
            + "{\"name\": \"Terms\", \"value\": \"" + escapeJson(terms) + "\", \"inline\": false}"
            + "],"
            + "\"footer\": {\"text\": \"" + escapeJson(serverName) + "\"},"
            + "\"timestamp\": \"" + timestamp + "\""
            + "}]"
            + "}";

        sendAsync(treatyWebhookUrl, json, "treaty");
    }

    public void sendTreatyRevoked(String nationA, String nationB, String revokerName) {
        if (treatyWebhookUrl.isEmpty() || treatyWebhookUrl.equals("YOUR_WEBHOOK_URL_HERE")) return;

        String threadName = "Treaty Revoked: " + nationA + " & " + nationB;
        String timestamp = Instant.now().toString();

        String json = "{"
            + "\"thread_name\": \"" + escapeJson(threadName) + "\","
            + "\"embeds\": [{"
            + "\"title\": \"Treaty Revoked\","
            + "\"description\": \"The treaty between **" + escapeJson(nationA) + "** and **" + escapeJson(nationB) + "** has been dissolved.\","
            + "\"color\": 16744272,"
            + "\"fields\": ["
            + "{\"name\": \"Nations\", \"value\": \"" + escapeJson(nationA) + " & " + escapeJson(nationB) + "\", \"inline\": true},"
            + "{\"name\": \"Revoked By\", \"value\": \"" + escapeJson(revokerName) + "\", \"inline\": true}"
            + "],"
            + "\"footer\": {\"text\": \"" + escapeJson(serverName) + "\"},"
            + "\"timestamp\": \"" + timestamp + "\""
            + "}]"
            + "}";

        sendAsync(treatyWebhookUrl, json, "treaty revocation");
    }

    private void sendAsync(String webhookUrl, String json, String eventType) {
        Bukkit.getScheduler().runTaskAsynchronously(GeoWarPlugin.getInstance(), () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "GeoWarPlugin/1.0");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    GeoWarPlugin.getInstance().getLogger().warning(
                        "Discord webhook for " + eventType + " returned HTTP " + responseCode
                    );
                }
                connection.disconnect();
            } catch (Exception e) {
                GeoWarPlugin.getInstance().getLogger().warning(
                    "Failed to send Discord webhook for " + eventType + ": " + e.getMessage()
                );
            }
        });
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
