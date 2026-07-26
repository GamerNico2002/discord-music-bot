package bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UpdateChecker {

    private static final String REPO = "GamerNico2002/discord-music-bot";
    private static final String API_URL = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final String DOWNLOAD_URL = "https://github.com/" + REPO + "/releases/latest";
    private static final Path NOTIFIED_FILE = Path.of("last-notified-version.txt");
    private static final long CHECK_INTERVAL_HOURS = 6;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public void start() {
        scheduler.scheduleAtFixedRate(this::check, 30, CHECK_INTERVAL_HOURS * 60 * 60, TimeUnit.SECONDS);
        System.out.println("[UpdateChecker] Gestartet (alle " + CHECK_INTERVAL_HOURS + "h)");
    }

    private void check() {
        try {
            String currentVersion = loadCurrentVersion();
            if (currentVersion == null) {
                System.err.println("[UpdateChecker] Aktuelle Version unbekannt");
                return;
            }

            String latestTag = fetchLatestVersion();
            if (latestTag == null) {
                System.out.println("[UpdateChecker] Konnte GitHub API nicht erreichen");
                return;
            }

            String latestVersion = latestTag.startsWith("v") ? latestTag.substring(1) : latestTag;
            System.out.println("[UpdateChecker] Aktuell: " + currentVersion + " | Neu: " + latestVersion);

            if (isNewer(latestVersion, currentVersion)) {
                String lastNotified = loadLastNotified();
                if (latestVersion.equals(lastNotified)) {
                    System.out.println("[UpdateChecker] Bereits benachrichtigt fuer " + latestVersion);
                    return;
                }

                System.out.println("[UpdateChecker] Neues Update gefunden! Benachrichtige Server...");
                sendNotifications(currentVersion, latestVersion);
                saveLastNotified(latestVersion);
            } else {
                System.out.println("[UpdateChecker] Kein Update verfuegbar");
            }
        } catch (Exception e) {
            System.err.println("[UpdateChecker] Fehler: " + e.getMessage());
        }
    }

    private String loadCurrentVersion() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("version.properties")) {
            if (in == null) return null;
            Properties p = new Properties();
            p.load(in);
            return p.getProperty("bot.version");
        } catch (IOException e) {
            return null;
        }
    }

    private String fetchLatestVersion() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "DiscordMusicBot")
                    .GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode json = mapper.readTree(resp.body());
            return json.has("tag_name") ? json.get("tag_name").asText() : null;
        } catch (Exception e) {
            System.err.println("[UpdateChecker] GitHub API Fehler: " + e.getMessage());
            return null;
        }
    }

    private boolean isNewer(String latest, String current) {
        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");
        int len = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < len; i++) {
            int l = i < latestParts.length ? parseNum(latestParts[i]) : 0;
            int c = i < currentParts.length ? parseNum(currentParts[i]) : 0;
            if (l > c) return true;
            if (l < c) return false;
        }
        return false;
    }

    private int parseNum(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void sendNotifications(String currentVersion, String newVersion) {
        String releaseUrl = "https://github.com/" + REPO + "/releases/tag/v" + newVersion;
        for (Guild guild : MusicBot.JDA.getGuilds()) {
            try {
                TextChannel channel = findChannel(guild);
                if (channel == null) {
                    System.out.println("[UpdateChecker] Kein Kanal fuer: " + guild.getName());
                    continue;
                }

                String lang = Lang.getLang(guild.getIdLong());
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle(Lang.t(lang, "update.title"))
                        .setDescription(Lang.t(lang, "update.body", currentVersion, newVersion))
                        .setColor(0x5865F2)
                        .setFooter("Discord Music Bot")
                        .setTimestamp(java.time.Instant.now());

                channel.sendMessageEmbeds(embed.build())
                        .addComponents(net.dv8tion.jda.api.components.actionrow.ActionRow.of(
                                net.dv8tion.jda.api.components.buttons.Button.link(
                                        releaseUrl, Lang.t(lang, "update.download"))))
                        .queue(
                                success -> System.out.println("[UpdateChecker] Benachrichtigt: " + guild.getName()),
                                error -> System.err.println("[UpdateChecker] Fehler bei " + guild.getName() + ": " + error.getMessage())
                        );
            } catch (Exception e) {
                System.err.println("[UpdateChecker] Fehler bei " + guild.getName() + ": " + e.getMessage());
            }
        }
    }

    private TextChannel findChannel(Guild guild) {
        var channels = guild.getTextChannels();
        for (TextChannel ch : channels) {
            String name = ch.getName().toLowerCase();
            if (name.contains("bot") || name.contains("update") || name.contains("ankuendigung")
                    || name.contains("announce") || name.contains("general") || name.contains("chat")) {
                return ch;
            }
        }
        return channels.isEmpty() ? null : channels.get(0);
    }

    private String loadLastNotified() {
        try {
            if (Files.exists(NOTIFIED_FILE)) {
                return Files.readString(NOTIFIED_FILE).trim();
            }
        } catch (IOException ignored) {}
        return null;
    }

    private void saveLastNotified(String version) {
        try {
            Files.writeString(NOTIFIED_FILE, version);
        } catch (IOException e) {
            System.err.println("[UpdateChecker] Konnte last-notified nicht speichern: " + e.getMessage());
        }
    }
}
