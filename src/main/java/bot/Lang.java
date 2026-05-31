package bot;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple i18n manager. Per-guild language stored in languages.properties.
 * Supported: de (default), en, fr, es, it
 */
public final class Lang {

    public static final String DEFAULT = "de";

    public static final Map<String, String> SUPPORTED;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("de", "\uD83C\uDDE9\uD83C\uDDEA Deutsch");
        m.put("en", "\uD83C\uDDEC\uD83C\uDDE7 English");
        m.put("fr", "\uD83C\uDDEB\uD83C\uDDF7 Fran\u00e7ais");
        m.put("es", "\uD83C\uDDEA\uD83C\uDDF8 Espa\u00f1ol");
        m.put("it", "\uD83C\uDDEE\uD83C\uDDF9 Italiano");
        SUPPORTED = Collections.unmodifiableMap(m);
    }

    private static final Path FILE = Paths.get("languages.properties");
    private static final Map<Long, String> GUILD_LANG = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, String>> T = new ConcurrentHashMap<>();

    static {
        initTranslations();
        load();
    }

    private Lang() {}

    public static String getLang(long guildId) {
        return GUILD_LANG.getOrDefault(guildId, DEFAULT);
    }

    public static synchronized void setLang(long guildId, String code) {
        if (!SUPPORTED.containsKey(code)) return;
        GUILD_LANG.put(guildId, code);
        save();
    }

    public static String t(long guildId, String key, Object... args) {
        return t(getLang(guildId), key, args);
    }

    public static String t(String lang, String key, Object... args) {
        Map<String, String> map = T.getOrDefault(lang, T.get(DEFAULT));
        String value = map.get(key);
        if (value == null) value = T.get(DEFAULT).getOrDefault(key, key);
        if (args == null || args.length == 0) return value;
        try { return MessageFormat.format(value, args); }
        catch (Exception e) { return value; }
    }

    private static void load() {
        if (!Files.exists(FILE)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            p.load(in);
            for (String name : p.stringPropertyNames()) {
                try {
                    long id = Long.parseLong(name);
                    String code = p.getProperty(name);
                    if (SUPPORTED.containsKey(code)) GUILD_LANG.put(id, code);
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("[Lang] load failed: " + e.getMessage());
        }
    }

    private static synchronized void save() {
        Properties p = new Properties();
        GUILD_LANG.forEach((id, code) -> p.setProperty(String.valueOf(id), code));
        try (OutputStream out = Files.newOutputStream(FILE)) {
            p.store(out, "Per-guild language preferences");
        } catch (IOException e) {
            System.err.println("[Lang] save failed: " + e.getMessage());
        }
    }

    private static void put(String lang, String key, String value) {
        T.computeIfAbsent(lang, k -> new ConcurrentHashMap<>()).put(key, value);
    }

    private static void initTranslations() {
        // ===== DEUTSCH =====
        put("de", "voice.required", "Du musst in einem Voice-Channel sein!");
        put("de", "nothing.playing", "Gerade spielt nichts");
        put("de", "nothing.found", "Nichts gefunden fuer: {0}");
        put("de", "load.error", "Fehler beim Laden: {0}");
        put("de", "error.generic", "Fehler: {0}");
        put("de", "queue.empty", "Die Queue ist leer");
        put("de", "queue.already.empty", "Die Queue ist bereits leer");
        put("de", "spotify.not.configured", "Spotify ist nicht konfiguriert! Trage `spotify.client.id` und `spotify.client.secret` in config.properties ein.");
        put("de", "spotify.no.songs", "Keine Songs in diesem Spotify-Link gefunden.");
        put("de", "spotify.not.on.youtube", "Song nicht auf YouTube gefunden.");
        put("de", "spotify.loading", "\uD83C\uDFB5 Lade **{0} Songs** von Spotify...");
        put("de", "spotify.loaded", "\u2705 **{0}/{1} Songs** von Spotify geladen");
        put("de", "spotify.failed.count", " ({0} nicht gefunden)");
        put("de", "spotify.error", "Spotify-Fehler: {0}");
        put("de", "spotify.added", "\uD83C\uDFB5 **{0}** zur Queue hinzugefuegt (via Spotify)");
        put("de", "radio.unknown", "Unbekannter Sender!");
        put("de", "radio.unreachable", "Radio-Stream nicht erreichbar.");
        put("de", "radio.started", "\uD83D\uDCFB **{0}** gestartet");
        put("de", "track.added", "**{0}** zur Queue hinzugefuegt");
        put("de", "track.duplicate", "\u26A0\uFE0F **{0}** ist bereits in der Queue! Trotzdem hinzugefuegt.");
        put("de", "playlist.added", "**{0} Songs** aus Playlist **{1}** hinzugefuegt");
        put("de", "skip.next", "\u23ED Song uebersprungen\n\uD83C\uDFB6 Jetzt: **{0}**");
        put("de", "skip.nonstop", "\u23ED Song uebersprungen \u2014 \uD83D\uDD25 lade naechsten Random-Track...");
        put("de", "skip.empty", "\u23ED Song uebersprungen \u2014 Queue ist leer");
        put("de", "stop.full", "\u23F9\uFE0F Musik gestoppt und Queue geleert\n\uD83D\uDCA4 Nonstop startet in 2 Min wenn nichts gespielt wird");
        put("de", "stop.short", "\u23F9\uFE0F Musik gestoppt \u2014 \uD83D\uDCA4 Nonstop startet in 2 Min");
        put("de", "pause", "\u23F8\uFE0F Pausiert");
        put("de", "resume", "\u25B6\uFE0F Fortgesetzt");
        put("de", "now.playing.now", "\uD83C\uDFB6 **Spielt gerade:**");
        put("de", "queue.list", "**Warteschlange:**");
        put("de", "queue.title", "\uD83D\uDCCB Warteschlange");
        put("de", "queue.footer", "{0} Songs in der Queue  |  Gesamtdauer: {1}{2}");
        put("de", "repeat.song.full", "  |  \uD83D\uDD02 Song Repeat");
        put("de", "repeat.queue.full", "  |  \uD83D\uDD01 Queue Repeat");
        put("de", "repeat.song.short", "  |  \uD83D\uDD02 Song");
        put("de", "repeat.queue.short", "  |  \uD83D\uDD01 Queue");
        put("de", "now.playing.title", "\uD83C\uDFB5 Now Playing");
        put("de", "nonstop.loading", "\uD83D\uDD25 **Nonstop-Modus** \u2014 lade naechsten Random-Track...");
        put("de", "now.playing.status.vol", "\uD83D\uDD0A Vol: {0}%");
        put("de", "skipped.empty.short", "\u23ED Uebersprungen \u2014 Queue ist leer");
        put("de", "volume.range", "Lautstaerke muss zwischen 0 und 100 sein");
        put("de", "volume.set", "\uD83D\uDD0A Lautstaerke: **{0}%**\n{1}");
        put("de", "joined", "\uD83D\uDD0A Joined **{0}**");
        put("de", "bye", "\uD83D\uDC4B Tschuess!");
        put("de", "repeat.off", "\u27A1\uFE0F Repeat: **Aus**");
        put("de", "repeat.track.set", "\uD83D\uDD02 Repeat: **Song wiederholen**");
        put("de", "repeat.queue.set", "\uD83D\uDD01 Repeat: **Queue wiederholen**");
        put("de", "repeat.invalid", "Ungueltiger Modus! Nutze: `off`, `track` oder `queue`");
        put("de", "shuffled", "\uD83D\uDD00 Queue wurde gemischt! ({0} Songs)");
        put("de", "pos.invalid", "Ungueltige Position! Queue hat **{0}** Songs.");
        put("de", "moved", "\u2195\uFE0F **{0}** von Position {1} nach {2} verschoben");
        put("de", "move.failed", "Konnte Song nicht verschieben");
        put("de", "skipto.success", "\u23ED Uebersprungen zu **#{0}**: **{1}**");
        put("de", "skipto.failed", "Konnte nicht springen");
        put("de", "save.title", "\uD83D\uDCBE Gespeicherter Song");
        put("de", "save.open", "\uD83D\uDD17 Link oeffnen");
        put("de", "save.dm.sent", "\uD83D\uDC8C Song wurde dir per DM geschickt!");
        put("de", "save.dm.failed", "Konnte dir keine DM senden. Aktiviere DMs von Server-Mitgliedern!");
        put("de", "save.dm.failed.short", "Konnte dir keine DM senden.");
        put("de", "auto.nonstop.off", "\uD83D\uDEAB **Auto-Nonstop deaktiviert** \u2014 Bot startet keine Musik mehr nach 2 Min Idle");
        put("de", "auto.nonstop.on", "\u2705 **Auto-Nonstop aktiviert** \u2014 Bot spielt nach 2 Min Idle automatisch Musik");
        put("de", "nonstop.off", "\uD83D\uDCA4 **Nonstop-Modus deaktiviert**");
        put("de", "nonstop.on", "\uD83D\uDD25 **Nonstop-Modus aktiviert** \u2014 random Tekk, Techno, Uptempo & Co.");
        put("de", "filter.bassboost", "\uD83D\uDD0A **Bassboost** aktiviert");
        put("de", "filter.treble", "\uD83C\uDFB5 **Treble Boost** aktiviert");
        put("de", "filter.pop", "\uD83C\uDFA4 **Pop** Equalizer aktiviert");
        put("de", "filter.rock", "\uD83E\uDD18 **Rock** Equalizer aktiviert");
        put("de", "filter.off", "\u27A1\uFE0F Filter **deaktiviert**");
        put("de", "seek.invalid", "Ungueltiges Format! Nutze z.B. `1:30` oder `90` (Sekunden)");
        put("de", "seek.success", "\u23E9 Gesprungen zu **{0}** / {1}");
        put("de", "removed", "\uD83D\uDDD1\uFE0F **{0}** aus der Queue entfernt (Position {1})");
        put("de", "remove.failed", "Konnte Song nicht entfernen");
        put("de", "cleared", "\uD83D\uDDD1\uFE0F **{0} Songs** aus der Queue entfernt");
        put("de", "help.title", "\uD83C\uDFB5 Music Bot \u2014 Hilfe");
        put("de", "help.music.title", "\uD83C\uDFA7 Musik");
        put("de", "help.music.body",
                "`/play <URL/Suche>` \u2014 Song, Playlist oder Radio\n" +
                "`/skip` \u2014 Song ueberspringen\n" +
                "`/stop` \u2014 Musik stoppen & Queue leeren\n" +
                "`/pause` \u2014 Pausieren\n" +
                "`/resume` \u2014 Fortsetzen\n" +
                "`/volume <0-100>` \u2014 Lautstaerke aendern\n" +
                "`/radio <sender>` \u2014 Live-Radio hoeren");
        put("de", "help.queue.title", "\uD83D\uDCCB Queue & Wiedergabe");
        put("de", "help.queue.body",
                "`/queue` \u2014 Warteschlange anzeigen\n" +
                "`/playing` \u2014 Aktueller Song mit Steuerung\n" +
                "`/seek <Zeit>` \u2014 Im Song springen (z.B. 1:30)\n" +
                "`/skipto <Position>` \u2014 Zu Song in Queue springen\n" +
                "`/move <von> <nach>` \u2014 Song verschieben\n" +
                "`/repeat <off/track/queue>` \u2014 Repeat-Modus\n" +
                "`/shuffle` \u2014 Queue mischen\n" +
                "`/remove <Position>` \u2014 Song aus Queue entfernen\n" +
                "`/clear` \u2014 Queue leeren\n" +
                "`/save` \u2014 Song per DM speichern");
        put("de", "help.voice.title", "\uD83D\uDD0A Voice & Audio");
        put("de", "help.voice.body",
                "`/join` \u2014 Voice-Channel beitreten\n" +
                "`/leave` \u2014 Voice-Channel verlassen\n" +
                "`/filter <preset>` \u2014 Audio-Filter (Bassboost, etc.)\n" +
                "`/nonstop` \u2014 Nonstop-Modus: random Tekk/Techno/Uptempo");
        put("de", "help.other.title", "\u2139\uFE0F Sonstiges");
        put("de", "help.other.body",
                "`/invite` \u2014 Bot einladen\n" +
                "`/info` \u2014 Bot-Infos\n" +
                "`/uptime` \u2014 Online-Zeit\n" +
                "`/language` \u2014 Sprache aendern\n" +
                "`/help` \u2014 Diese Hilfe");
        put("de", "help.footer", "Quellen: YouTube, SoundCloud, Spotify, Radio  |  Playlists werden unterstuetzt!");
        put("de", "invite.title", "\uD83D\uDD17 Bot einladen");
        put("de", "invite.desc", "Klicke den Link um den Bot auf deinen Server einzuladen:\n\n**[Hier klicken]({0})**");
        put("de", "info.owner", "\uD83D\uDC51 Besitzer");
        put("de", "info.servers", "\uD83C\uDF10 Server");
        put("de", "info.members", "\uD83D\uDC65 Mitglieder");
        put("de", "info.uptime", "\u23F0 Uptime");
        put("de", "info.active.players", "\uD83C\uDFB5 Aktive Player");
        put("de", "info.support", "\uD83D\uDCE9 Support");
        put("de", "info.discord", "\uD83D\uDD17 Discord");
        put("de", "info.discord.value", "[Server beitreten](https://discord.gg/KqngYCVJqZ)");
        put("de", "info.footer", "Made with \u2764\uFE0F using JDA + LavaPlayer");
        put("de", "ping.title", "\uD83C\uDFD3 Pong!");
        put("de", "ping.gateway", "\uD83D\uDCE1 Gateway");
        put("de", "uptime.title", "\u23F0 Uptime & System");
        put("de", "uptime.online", "\u23F1\uFE0F Online seit");
        put("de", "uptime.cpu", "\uD83D\uDCBB CPU");
        put("de", "uptime.threads", "\uD83E\uDDE9 Threads");
        put("de", "uptime.bot.ram", "\uD83D\uDCBE Bot RAM");
        put("de", "uptime.sys.ram", "\uD83D\uDDA5\uFE0F System RAM");
        put("de", "dcleave.not.owner", "\u26D4 Nur der Bot-Owner darf diesen Befehl benutzen.");
        put("de", "dcleave.not.found", "\u274C Server nicht gefunden (ID: {0})");
        put("de", "dcleave.leaving", "\uD83D\uDC4B Verlasse Server **{0}** ({1})...");
        put("de", "page", "Seite");
        put("de", "lang.title", "\uD83C\uDF0D Sprache");
        put("de", "lang.changed", "\u2705 Sprache geaendert auf **{0}**");
        put("de", "lang.current", "Aktuelle Sprache: **{0}**\nWaehle eine neue Sprache mit `/language code:<de|en|fr|es|it>`");

        // ===== ENGLISH =====
        put("en", "voice.required", "You must be in a voice channel!");
        put("en", "nothing.playing", "Nothing is currently playing");
        put("en", "nothing.found", "Nothing found for: {0}");
        put("en", "load.error", "Loading error: {0}");
        put("en", "error.generic", "Error: {0}");
        put("en", "queue.empty", "The queue is empty");
        put("en", "queue.already.empty", "The queue is already empty");
        put("en", "spotify.not.configured", "Spotify is not configured! Add `spotify.client.id` and `spotify.client.secret` to config.properties.");
        put("en", "spotify.no.songs", "No songs found in this Spotify link.");
        put("en", "spotify.not.on.youtube", "Song not found on YouTube.");
        put("en", "spotify.loading", "\uD83C\uDFB5 Loading **{0} songs** from Spotify...");
        put("en", "spotify.loaded", "\u2705 **{0}/{1} songs** loaded from Spotify");
        put("en", "spotify.failed.count", " ({0} not found)");
        put("en", "spotify.error", "Spotify error: {0}");
        put("en", "spotify.added", "\uD83C\uDFB5 **{0}** added to queue (via Spotify)");
        put("en", "radio.unknown", "Unknown station!");
        put("en", "radio.unreachable", "Radio stream not reachable.");
        put("en", "radio.started", "\uD83D\uDCFB **{0}** started");
        put("en", "track.added", "**{0}** added to queue");
        put("en", "track.duplicate", "\u26A0\uFE0F **{0}** is already in the queue! Added anyway.");
        put("en", "playlist.added", "**{0} songs** from playlist **{1}** added");
        put("en", "skip.next", "\u23ED Song skipped\n\uD83C\uDFB6 Now: **{0}**");
        put("en", "skip.nonstop", "\u23ED Song skipped \u2014 \uD83D\uDD25 loading next random track...");
        put("en", "skip.empty", "\u23ED Song skipped \u2014 queue is empty");
        put("en", "stop.full", "\u23F9\uFE0F Music stopped and queue cleared\n\uD83D\uDCA4 Nonstop starts in 2 min if nothing plays");
        put("en", "stop.short", "\u23F9\uFE0F Music stopped \u2014 \uD83D\uDCA4 Nonstop starts in 2 min");
        put("en", "pause", "\u23F8\uFE0F Paused");
        put("en", "resume", "\u25B6\uFE0F Resumed");
        put("en", "now.playing.now", "\uD83C\uDFB6 **Now playing:**");
        put("en", "queue.list", "**Queue:**");
        put("en", "queue.title", "\uD83D\uDCCB Queue");
        put("en", "queue.footer", "{0} songs in queue  |  Total duration: {1}{2}");
        put("en", "repeat.song.full", "  |  \uD83D\uDD02 Song Repeat");
        put("en", "repeat.queue.full", "  |  \uD83D\uDD01 Queue Repeat");
        put("en", "repeat.song.short", "  |  \uD83D\uDD02 Song");
        put("en", "repeat.queue.short", "  |  \uD83D\uDD01 Queue");
        put("en", "now.playing.title", "\uD83C\uDFB5 Now Playing");
        put("en", "nonstop.loading", "\uD83D\uDD25 **Nonstop mode** \u2014 loading next random track...");
        put("en", "now.playing.status.vol", "\uD83D\uDD0A Vol: {0}%");
        put("en", "skipped.empty.short", "\u23ED Skipped \u2014 queue is empty");
        put("en", "volume.range", "Volume must be between 0 and 100");
        put("en", "volume.set", "\uD83D\uDD0A Volume: **{0}%**\n{1}");
        put("en", "joined", "\uD83D\uDD0A Joined **{0}**");
        put("en", "bye", "\uD83D\uDC4B Bye!");
        put("en", "repeat.off", "\u27A1\uFE0F Repeat: **Off**");
        put("en", "repeat.track.set", "\uD83D\uDD02 Repeat: **Track**");
        put("en", "repeat.queue.set", "\uD83D\uDD01 Repeat: **Queue**");
        put("en", "repeat.invalid", "Invalid mode! Use: `off`, `track` or `queue`");
        put("en", "shuffled", "\uD83D\uDD00 Queue shuffled! ({0} songs)");
        put("en", "pos.invalid", "Invalid position! Queue has **{0}** songs.");
        put("en", "moved", "\u2195\uFE0F **{0}** moved from position {1} to {2}");
        put("en", "move.failed", "Could not move song");
        put("en", "skipto.success", "\u23ED Skipped to **#{0}**: **{1}**");
        put("en", "skipto.failed", "Could not skip");
        put("en", "save.title", "\uD83D\uDCBE Saved song");
        put("en", "save.open", "\uD83D\uDD17 Open link");
        put("en", "save.dm.sent", "\uD83D\uDC8C Song sent to you via DM!");
        put("en", "save.dm.failed", "Could not send you a DM. Enable DMs from server members!");
        put("en", "save.dm.failed.short", "Could not send you a DM.");
        put("en", "auto.nonstop.off", "\uD83D\uDEAB **Auto-Nonstop disabled** \u2014 Bot will not start music after 2 min idle");
        put("en", "auto.nonstop.on", "\u2705 **Auto-Nonstop enabled** \u2014 Bot plays music automatically after 2 min idle");
        put("en", "nonstop.off", "\uD83D\uDCA4 **Nonstop mode disabled**");
        put("en", "nonstop.on", "\uD83D\uDD25 **Nonstop mode enabled** \u2014 random Tekk, Techno, Uptempo & Co.");
        put("en", "filter.bassboost", "\uD83D\uDD0A **Bassboost** enabled");
        put("en", "filter.treble", "\uD83C\uDFB5 **Treble Boost** enabled");
        put("en", "filter.pop", "\uD83C\uDFA4 **Pop** equalizer enabled");
        put("en", "filter.rock", "\uD83E\uDD18 **Rock** equalizer enabled");
        put("en", "filter.off", "\u27A1\uFE0F Filter **disabled**");
        put("en", "seek.invalid", "Invalid format! Use e.g. `1:30` or `90` (seconds)");
        put("en", "seek.success", "\u23E9 Jumped to **{0}** / {1}");
        put("en", "removed", "\uD83D\uDDD1\uFE0F **{0}** removed from queue (position {1})");
        put("en", "remove.failed", "Could not remove song");
        put("en", "cleared", "\uD83D\uDDD1\uFE0F **{0} songs** removed from queue");
        put("en", "help.title", "\uD83C\uDFB5 Music Bot \u2014 Help");
        put("en", "help.music.title", "\uD83C\uDFA7 Music");
        put("en", "help.music.body",
                "`/play <URL/search>` \u2014 Song, playlist or radio\n" +
                "`/skip` \u2014 Skip song\n" +
                "`/stop` \u2014 Stop music & clear queue\n" +
                "`/pause` \u2014 Pause\n" +
                "`/resume` \u2014 Resume\n" +
                "`/volume <0-100>` \u2014 Change volume\n" +
                "`/radio <station>` \u2014 Listen to live radio");
        put("en", "help.queue.title", "\uD83D\uDCCB Queue & Playback");
        put("en", "help.queue.body",
                "`/queue` \u2014 Show queue\n" +
                "`/playing` \u2014 Current song with controls\n" +
                "`/seek <time>` \u2014 Jump within song (e.g. 1:30)\n" +
                "`/skipto <position>` \u2014 Jump to song in queue\n" +
                "`/move <from> <to>` \u2014 Move song\n" +
                "`/repeat <off/track/queue>` \u2014 Repeat mode\n" +
                "`/shuffle` \u2014 Shuffle queue\n" +
                "`/remove <position>` \u2014 Remove song from queue\n" +
                "`/clear` \u2014 Clear queue\n" +
                "`/save` \u2014 Save song via DM");
        put("en", "help.voice.title", "\uD83D\uDD0A Voice & Audio");
        put("en", "help.voice.body",
                "`/join` \u2014 Join voice channel\n" +
                "`/leave` \u2014 Leave voice channel\n" +
                "`/filter <preset>` \u2014 Audio filter (bassboost, etc.)\n" +
                "`/nonstop` \u2014 Nonstop mode: random Tekk/Techno/Uptempo");
        put("en", "help.other.title", "\u2139\uFE0F Other");
        put("en", "help.other.body",
                "`/invite` \u2014 Invite bot\n" +
                "`/info` \u2014 Bot info\n" +
                "`/uptime` \u2014 Online time\n" +
                "`/language` \u2014 Change language\n" +
                "`/help` \u2014 This help");
        put("en", "help.footer", "Sources: YouTube, SoundCloud, Spotify, Radio  |  Playlists supported!");
        put("en", "invite.title", "\uD83D\uDD17 Invite bot");
        put("en", "invite.desc", "Click the link to invite the bot to your server:\n\n**[Click here]({0})**");
        put("en", "info.owner", "\uD83D\uDC51 Owner");
        put("en", "info.servers", "\uD83C\uDF10 Servers");
        put("en", "info.members", "\uD83D\uDC65 Members");
        put("en", "info.uptime", "\u23F0 Uptime");
        put("en", "info.active.players", "\uD83C\uDFB5 Active players");
        put("en", "info.support", "\uD83D\uDCE9 Support");
        put("en", "info.discord", "\uD83D\uDD17 Discord");
        put("en", "info.discord.value", "[Join server](https://discord.gg/KqngYCVJqZ)");
        put("en", "info.footer", "Made with \u2764\uFE0F using JDA + LavaPlayer");
        put("en", "ping.title", "\uD83C\uDFD3 Pong!");
        put("en", "ping.gateway", "\uD83D\uDCE1 Gateway");
        put("en", "uptime.title", "\u23F0 Uptime & System");
        put("en", "uptime.online", "\u23F1\uFE0F Online since");
        put("en", "uptime.cpu", "\uD83D\uDCBB CPU");
        put("en", "uptime.threads", "\uD83E\uDDE9 Threads");
        put("en", "uptime.bot.ram", "\uD83D\uDCBE Bot RAM");
        put("en", "uptime.sys.ram", "\uD83D\uDDA5\uFE0F System RAM");
        put("en", "dcleave.not.owner", "\u26D4 Only the bot owner can use this command.");
        put("en", "dcleave.not.found", "\u274C Server not found (ID: {0})");
        put("en", "dcleave.leaving", "\uD83D\uDC4B Leaving server **{0}** ({1})...");
        put("en", "page", "Page");
        put("en", "lang.title", "\uD83C\uDF0D Language");
        put("en", "lang.changed", "\u2705 Language changed to **{0}**");
        put("en", "lang.current", "Current language: **{0}**\nChoose a new language with `/language code:<de|en|fr|es|it>`");

        // ===== FRAN\u00c7AIS =====
        put("fr", "voice.required", "Tu dois \u00eatre dans un salon vocal !");
        put("fr", "nothing.playing", "Rien n'est en lecture");
        put("fr", "nothing.found", "Rien trouv\u00e9 pour : {0}");
        put("fr", "load.error", "Erreur de chargement : {0}");
        put("fr", "error.generic", "Erreur : {0}");
        put("fr", "queue.empty", "La file d'attente est vide");
        put("fr", "queue.already.empty", "La file est d\u00e9j\u00e0 vide");
        put("fr", "spotify.not.configured", "Spotify n'est pas configur\u00e9 ! Ajoute `spotify.client.id` et `spotify.client.secret` dans config.properties.");
        put("fr", "spotify.no.songs", "Aucune chanson trouv\u00e9e dans ce lien Spotify.");
        put("fr", "spotify.not.on.youtube", "Chanson introuvable sur YouTube.");
        put("fr", "spotify.loading", "\uD83C\uDFB5 Chargement de **{0} morceaux** depuis Spotify...");
        put("fr", "spotify.loaded", "\u2705 **{0}/{1} morceaux** charg\u00e9s depuis Spotify");
        put("fr", "spotify.failed.count", " ({0} non trouv\u00e9s)");
        put("fr", "spotify.error", "Erreur Spotify : {0}");
        put("fr", "spotify.added", "\uD83C\uDFB5 **{0}** ajout\u00e9 \u00e0 la file (via Spotify)");
        put("fr", "radio.unknown", "Station inconnue !");
        put("fr", "radio.unreachable", "Flux radio inaccessible.");
        put("fr", "radio.started", "\uD83D\uDCFB **{0}** d\u00e9marr\u00e9e");
        put("fr", "track.added", "**{0}** ajout\u00e9 \u00e0 la file");
        put("fr", "track.duplicate", "\u26A0\uFE0F **{0}** est d\u00e9j\u00e0 dans la file ! Ajout\u00e9 quand m\u00eame.");
        put("fr", "playlist.added", "**{0} morceaux** de la playlist **{1}** ajout\u00e9s");
        put("fr", "skip.next", "\u23ED Morceau pass\u00e9\n\uD83C\uDFB6 Maintenant : **{0}**");
        put("fr", "skip.nonstop", "\u23ED Morceau pass\u00e9 \u2014 \uD83D\uDD25 chargement al\u00e9atoire...");
        put("fr", "skip.empty", "\u23ED Morceau pass\u00e9 \u2014 la file est vide");
        put("fr", "stop.full", "\u23F9\uFE0F Musique arr\u00eat\u00e9e et file vid\u00e9e\n\uD83D\uDCA4 Nonstop d\u00e9marre dans 2 min si rien ne joue");
        put("fr", "stop.short", "\u23F9\uFE0F Musique arr\u00eat\u00e9e \u2014 \uD83D\uDCA4 Nonstop dans 2 min");
        put("fr", "pause", "\u23F8\uFE0F En pause");
        put("fr", "resume", "\u25B6\uFE0F Reprise");
        put("fr", "now.playing.now", "\uD83C\uDFB6 **Lecture en cours :**");
        put("fr", "queue.list", "**File d'attente :**");
        put("fr", "queue.title", "\uD83D\uDCCB File d'attente");
        put("fr", "queue.footer", "{0} morceaux dans la file  |  Dur\u00e9e totale : {1}{2}");
        put("fr", "repeat.song.full", "  |  \uD83D\uDD02 R\u00e9p\u00e9ter morceau");
        put("fr", "repeat.queue.full", "  |  \uD83D\uDD01 R\u00e9p\u00e9ter file");
        put("fr", "repeat.song.short", "  |  \uD83D\uDD02 Morceau");
        put("fr", "repeat.queue.short", "  |  \uD83D\uDD01 File");
        put("fr", "now.playing.title", "\uD83C\uDFB5 Lecture en cours");
        put("fr", "nonstop.loading", "\uD83D\uDD25 **Mode Nonstop** \u2014 chargement du prochain morceau al\u00e9atoire...");
        put("fr", "now.playing.status.vol", "\uD83D\uDD0A Vol : {0}%");
        put("fr", "skipped.empty.short", "\u23ED Pass\u00e9 \u2014 la file est vide");
        put("fr", "volume.range", "Le volume doit \u00eatre entre 0 et 100");
        put("fr", "volume.set", "\uD83D\uDD0A Volume : **{0}%**\n{1}");
        put("fr", "joined", "\uD83D\uDD0A Connect\u00e9 \u00e0 **{0}**");
        put("fr", "bye", "\uD83D\uDC4B Au revoir !");
        put("fr", "repeat.off", "\u27A1\uFE0F R\u00e9p\u00e9tition : **D\u00e9sactiv\u00e9e**");
        put("fr", "repeat.track.set", "\uD83D\uDD02 R\u00e9p\u00e9tition : **Morceau**");
        put("fr", "repeat.queue.set", "\uD83D\uDD01 R\u00e9p\u00e9tition : **File**");
        put("fr", "repeat.invalid", "Mode invalide ! Utilise : `off`, `track` ou `queue`");
        put("fr", "shuffled", "\uD83D\uDD00 File m\u00e9lang\u00e9e ! ({0} morceaux)");
        put("fr", "pos.invalid", "Position invalide ! La file a **{0}** morceaux.");
        put("fr", "moved", "\u2195\uFE0F **{0}** d\u00e9plac\u00e9 de la position {1} \u00e0 {2}");
        put("fr", "move.failed", "Impossible de d\u00e9placer le morceau");
        put("fr", "skipto.success", "\u23ED Saut\u00e9 \u00e0 **#{0}** : **{1}**");
        put("fr", "skipto.failed", "Impossible de sauter");
        put("fr", "save.title", "\uD83D\uDCBE Morceau sauvegard\u00e9");
        put("fr", "save.open", "\uD83D\uDD17 Ouvrir le lien");
        put("fr", "save.dm.sent", "\uD83D\uDC8C Morceau envoy\u00e9 en MP !");
        put("fr", "save.dm.failed", "Impossible d'envoyer un MP. Active les MP des membres du serveur !");
        put("fr", "save.dm.failed.short", "Impossible d'envoyer un MP.");
        put("fr", "auto.nonstop.off", "\uD83D\uDEAB **Auto-Nonstop d\u00e9sactiv\u00e9** \u2014 le bot ne lancera plus de musique apr\u00e8s 2 min d'inactivit\u00e9");
        put("fr", "auto.nonstop.on", "\u2705 **Auto-Nonstop activ\u00e9** \u2014 le bot joue automatiquement apr\u00e8s 2 min d'inactivit\u00e9");
        put("fr", "nonstop.off", "\uD83D\uDCA4 **Mode Nonstop d\u00e9sactiv\u00e9**");
        put("fr", "nonstop.on", "\uD83D\uDD25 **Mode Nonstop activ\u00e9** \u2014 Tekk, Techno, Uptempo & Co. al\u00e9atoires");
        put("fr", "filter.bassboost", "\uD83D\uDD0A **Bassboost** activ\u00e9");
        put("fr", "filter.treble", "\uD83C\uDFB5 **Treble Boost** activ\u00e9");
        put("fr", "filter.pop", "\uD83C\uDFA4 \u00c9galiseur **Pop** activ\u00e9");
        put("fr", "filter.rock", "\uD83E\uDD18 \u00c9galiseur **Rock** activ\u00e9");
        put("fr", "filter.off", "\u27A1\uFE0F Filtre **d\u00e9sactiv\u00e9**");
        put("fr", "seek.invalid", "Format invalide ! Utilise par ex. `1:30` ou `90` (secondes)");
        put("fr", "seek.success", "\u23E9 Saut \u00e0 **{0}** / {1}");
        put("fr", "removed", "\uD83D\uDDD1\uFE0F **{0}** retir\u00e9 de la file (position {1})");
        put("fr", "remove.failed", "Impossible de retirer le morceau");
        put("fr", "cleared", "\uD83D\uDDD1\uFE0F **{0} morceaux** retir\u00e9s de la file");
        put("fr", "help.title", "\uD83C\uDFB5 Music Bot \u2014 Aide");
        put("fr", "help.music.title", "\uD83C\uDFA7 Musique");
        put("fr", "help.music.body",
                "`/play <URL/recherche>` \u2014 Morceau, playlist ou radio\n" +
                "`/skip` \u2014 Passer le morceau\n" +
                "`/stop` \u2014 Arr\u00eater & vider la file\n" +
                "`/pause` \u2014 Mettre en pause\n" +
                "`/resume` \u2014 Reprendre\n" +
                "`/volume <0-100>` \u2014 Changer le volume\n" +
                "`/radio <station>` \u2014 \u00c9couter la radio en direct");
        put("fr", "help.queue.title", "\uD83D\uDCCB File & lecture");
        put("fr", "help.queue.body",
                "`/queue` \u2014 Afficher la file\n" +
                "`/playing` \u2014 Morceau actuel avec contr\u00f4les\n" +
                "`/seek <temps>` \u2014 Sauter dans le morceau (ex. 1:30)\n" +
                "`/skipto <position>` \u2014 Sauter \u00e0 un morceau\n" +
                "`/move <de> <\u00e0>` \u2014 D\u00e9placer un morceau\n" +
                "`/repeat <off/track/queue>` \u2014 Mode r\u00e9p\u00e9tition\n" +
                "`/shuffle` \u2014 M\u00e9langer la file\n" +
                "`/remove <position>` \u2014 Retirer de la file\n" +
                "`/clear` \u2014 Vider la file\n" +
                "`/save` \u2014 Sauvegarder en MP");
        put("fr", "help.voice.title", "\uD83D\uDD0A Voix & audio");
        put("fr", "help.voice.body",
                "`/join` \u2014 Rejoindre le salon vocal\n" +
                "`/leave` \u2014 Quitter le salon vocal\n" +
                "`/filter <preset>` \u2014 Filtre audio (bassboost, etc.)\n" +
                "`/nonstop` \u2014 Mode Nonstop : Tekk/Techno/Uptempo al\u00e9atoires");
        put("fr", "help.other.title", "\u2139\uFE0F Autres");
        put("fr", "help.other.body",
                "`/invite` \u2014 Inviter le bot\n" +
                "`/info` \u2014 Infos du bot\n" +
                "`/uptime` \u2014 Temps en ligne\n" +
                "`/language` \u2014 Changer la langue\n" +
                "`/help` \u2014 Cette aide");
        put("fr", "help.footer", "Sources : YouTube, SoundCloud, Spotify, Radio  |  Playlists support\u00e9es !");
        put("fr", "invite.title", "\uD83D\uDD17 Inviter le bot");
        put("fr", "invite.desc", "Clique sur le lien pour inviter le bot sur ton serveur :\n\n**[Cliquer ici]({0})**");
        put("fr", "info.owner", "\uD83D\uDC51 Propri\u00e9taire");
        put("fr", "info.servers", "\uD83C\uDF10 Serveurs");
        put("fr", "info.members", "\uD83D\uDC65 Membres");
        put("fr", "info.uptime", "\u23F0 Uptime");
        put("fr", "info.active.players", "\uD83C\uDFB5 Lecteurs actifs");
        put("fr", "info.support", "\uD83D\uDCE9 Support");
        put("fr", "info.discord", "\uD83D\uDD17 Discord");
        put("fr", "info.discord.value", "[Rejoindre le serveur](https://discord.gg/KqngYCVJqZ)");
        put("fr", "info.footer", "Fait avec \u2764\uFE0F gr\u00e2ce \u00e0 JDA + LavaPlayer");
        put("fr", "ping.title", "\uD83C\uDFD3 Pong !");
        put("fr", "ping.gateway", "\uD83D\uDCE1 Gateway");
        put("fr", "uptime.title", "\u23F0 Uptime & syst\u00e8me");
        put("fr", "uptime.online", "\u23F1\uFE0F En ligne depuis");
        put("fr", "uptime.cpu", "\uD83D\uDCBB CPU");
        put("fr", "uptime.threads", "\uD83E\uDDE9 Threads");
        put("fr", "uptime.bot.ram", "\uD83D\uDCBE RAM bot");
        put("fr", "uptime.sys.ram", "\uD83D\uDDA5\uFE0F RAM syst\u00e8me");
        put("fr", "dcleave.not.owner", "\u26D4 Seul le propri\u00e9taire du bot peut utiliser cette commande.");
        put("fr", "dcleave.not.found", "\u274C Serveur introuvable (ID : {0})");
        put("fr", "dcleave.leaving", "\uD83D\uDC4B Quitte le serveur **{0}** ({1})...");
        put("fr", "page", "Page");
        put("fr", "lang.title", "\uD83C\uDF0D Langue");
        put("fr", "lang.changed", "\u2705 Langue chang\u00e9e en **{0}**");
        put("fr", "lang.current", "Langue actuelle : **{0}**\nChoisis une nouvelle langue avec `/language code:<de|en|fr|es|it>`");

        // ===== ESPA\u00d1OL =====
        put("es", "voice.required", "\u00a1Debes estar en un canal de voz!");
        put("es", "nothing.playing", "No se est\u00e1 reproduciendo nada");
        put("es", "nothing.found", "Nada encontrado para: {0}");
        put("es", "load.error", "Error al cargar: {0}");
        put("es", "error.generic", "Error: {0}");
        put("es", "queue.empty", "La cola est\u00e1 vac\u00eda");
        put("es", "queue.already.empty", "La cola ya est\u00e1 vac\u00eda");
        put("es", "spotify.not.configured", "\u00a1Spotify no est\u00e1 configurado! A\u00f1ade `spotify.client.id` y `spotify.client.secret` en config.properties.");
        put("es", "spotify.no.songs", "No se encontraron canciones en este enlace de Spotify.");
        put("es", "spotify.not.on.youtube", "Canci\u00f3n no encontrada en YouTube.");
        put("es", "spotify.loading", "\uD83C\uDFB5 Cargando **{0} canciones** desde Spotify...");
        put("es", "spotify.loaded", "\u2705 **{0}/{1} canciones** cargadas desde Spotify");
        put("es", "spotify.failed.count", " ({0} no encontradas)");
        put("es", "spotify.error", "Error de Spotify: {0}");
        put("es", "spotify.added", "\uD83C\uDFB5 **{0}** a\u00f1adido a la cola (v\u00eda Spotify)");
        put("es", "radio.unknown", "\u00a1Emisora desconocida!");
        put("es", "radio.unreachable", "Stream de radio no disponible.");
        put("es", "radio.started", "\uD83D\uDCFB **{0}** iniciada");
        put("es", "track.added", "**{0}** a\u00f1adido a la cola");
        put("es", "track.duplicate", "\u26A0\uFE0F **{0}** ya est\u00e1 en la cola. A\u00f1adido igualmente.");
        put("es", "playlist.added", "**{0} canciones** de la playlist **{1}** a\u00f1adidas");
        put("es", "skip.next", "\u23ED Canci\u00f3n saltada\n\uD83C\uDFB6 Ahora: **{0}**");
        put("es", "skip.nonstop", "\u23ED Canci\u00f3n saltada \u2014 \uD83D\uDD25 cargando siguiente pista aleatoria...");
        put("es", "skip.empty", "\u23ED Canci\u00f3n saltada \u2014 cola vac\u00eda");
        put("es", "stop.full", "\u23F9\uFE0F M\u00fasica detenida y cola vaciada\n\uD83D\uDCA4 Nonstop arranca en 2 min si nada suena");
        put("es", "stop.short", "\u23F9\uFE0F M\u00fasica detenida \u2014 \uD83D\uDCA4 Nonstop en 2 min");
        put("es", "pause", "\u23F8\uFE0F En pausa");
        put("es", "resume", "\u25B6\uFE0F Reanudado");
        put("es", "now.playing.now", "\uD83C\uDFB6 **Reproduciendo ahora:**");
        put("es", "queue.list", "**Cola:**");
        put("es", "queue.title", "\uD83D\uDCCB Cola");
        put("es", "queue.footer", "{0} canciones en cola  |  Duraci\u00f3n total: {1}{2}");
        put("es", "repeat.song.full", "  |  \uD83D\uDD02 Repetir canci\u00f3n");
        put("es", "repeat.queue.full", "  |  \uD83D\uDD01 Repetir cola");
        put("es", "repeat.song.short", "  |  \uD83D\uDD02 Canci\u00f3n");
        put("es", "repeat.queue.short", "  |  \uD83D\uDD01 Cola");
        put("es", "now.playing.title", "\uD83C\uDFB5 Reproduciendo");
        put("es", "nonstop.loading", "\uD83D\uDD25 **Modo Nonstop** \u2014 cargando siguiente pista aleatoria...");
        put("es", "now.playing.status.vol", "\uD83D\uDD0A Vol: {0}%");
        put("es", "skipped.empty.short", "\u23ED Saltado \u2014 cola vac\u00eda");
        put("es", "volume.range", "El volumen debe estar entre 0 y 100");
        put("es", "volume.set", "\uD83D\uDD0A Volumen: **{0}%**\n{1}");
        put("es", "joined", "\uD83D\uDD0A Conectado a **{0}**");
        put("es", "bye", "\uD83D\uDC4B \u00a1Adi\u00f3s!");
        put("es", "repeat.off", "\u27A1\uFE0F Repetir: **Desactivado**");
        put("es", "repeat.track.set", "\uD83D\uDD02 Repetir: **Canci\u00f3n**");
        put("es", "repeat.queue.set", "\uD83D\uDD01 Repetir: **Cola**");
        put("es", "repeat.invalid", "\u00a1Modo inv\u00e1lido! Usa: `off`, `track` o `queue`");
        put("es", "shuffled", "\uD83D\uDD00 \u00a1Cola mezclada! ({0} canciones)");
        put("es", "pos.invalid", "\u00a1Posici\u00f3n inv\u00e1lida! La cola tiene **{0}** canciones.");
        put("es", "moved", "\u2195\uFE0F **{0}** movido de la posici\u00f3n {1} a {2}");
        put("es", "move.failed", "No se pudo mover la canci\u00f3n");
        put("es", "skipto.success", "\u23ED Saltado a **#{0}**: **{1}**");
        put("es", "skipto.failed", "No se pudo saltar");
        put("es", "save.title", "\uD83D\uDCBE Canci\u00f3n guardada");
        put("es", "save.open", "\uD83D\uDD17 Abrir enlace");
        put("es", "save.dm.sent", "\uD83D\uDC8C \u00a1Canci\u00f3n enviada por MD!");
        put("es", "save.dm.failed", "No se pudo enviar MD. \u00a1Activa los MD de miembros del servidor!");
        put("es", "save.dm.failed.short", "No se pudo enviar MD.");
        put("es", "auto.nonstop.off", "\uD83D\uDEAB **Auto-Nonstop desactivado** \u2014 el bot no iniciar\u00e1 m\u00fasica tras 2 min de inactividad");
        put("es", "auto.nonstop.on", "\u2705 **Auto-Nonstop activado** \u2014 el bot reproduce autom\u00e1ticamente tras 2 min de inactividad");
        put("es", "nonstop.off", "\uD83D\uDCA4 **Modo Nonstop desactivado**");
        put("es", "nonstop.on", "\uD83D\uDD25 **Modo Nonstop activado** \u2014 Tekk, Techno, Uptempo & Co. aleatorios");
        put("es", "filter.bassboost", "\uD83D\uDD0A **Bassboost** activado");
        put("es", "filter.treble", "\uD83C\uDFB5 **Treble Boost** activado");
        put("es", "filter.pop", "\uD83C\uDFA4 Ecualizador **Pop** activado");
        put("es", "filter.rock", "\uD83E\uDD18 Ecualizador **Rock** activado");
        put("es", "filter.off", "\u27A1\uFE0F Filtro **desactivado**");
        put("es", "seek.invalid", "\u00a1Formato inv\u00e1lido! Usa por ej. `1:30` o `90` (segundos)");
        put("es", "seek.success", "\u23E9 Saltado a **{0}** / {1}");
        put("es", "removed", "\uD83D\uDDD1\uFE0F **{0}** eliminado de la cola (posici\u00f3n {1})");
        put("es", "remove.failed", "No se pudo eliminar la canci\u00f3n");
        put("es", "cleared", "\uD83D\uDDD1\uFE0F **{0} canciones** eliminadas de la cola");
        put("es", "help.title", "\uD83C\uDFB5 Music Bot \u2014 Ayuda");
        put("es", "help.music.title", "\uD83C\uDFA7 M\u00fasica");
        put("es", "help.music.body",
                "`/play <URL/b\u00fasqueda>` \u2014 Canci\u00f3n, playlist o radio\n" +
                "`/skip` \u2014 Saltar canci\u00f3n\n" +
                "`/stop` \u2014 Detener m\u00fasica y vaciar cola\n" +
                "`/pause` \u2014 Pausar\n" +
                "`/resume` \u2014 Reanudar\n" +
                "`/volume <0-100>` \u2014 Cambiar volumen\n" +
                "`/radio <emisora>` \u2014 Radio en vivo");
        put("es", "help.queue.title", "\uD83D\uDCCB Cola & reproducci\u00f3n");
        put("es", "help.queue.body",
                "`/queue` \u2014 Mostrar cola\n" +
                "`/playing` \u2014 Canci\u00f3n actual con controles\n" +
                "`/seek <tiempo>` \u2014 Saltar en la canci\u00f3n (ej. 1:30)\n" +
                "`/skipto <posici\u00f3n>` \u2014 Saltar a canci\u00f3n en cola\n" +
                "`/move <de> <a>` \u2014 Mover canci\u00f3n\n" +
                "`/repeat <off/track/queue>` \u2014 Modo repetir\n" +
                "`/shuffle` \u2014 Mezclar cola\n" +
                "`/remove <posici\u00f3n>` \u2014 Quitar de la cola\n" +
                "`/clear` \u2014 Vaciar cola\n" +
                "`/save` \u2014 Guardar canci\u00f3n por MD");
        put("es", "help.voice.title", "\uD83D\uDD0A Voz & audio");
        put("es", "help.voice.body",
                "`/join` \u2014 Unirse al canal de voz\n" +
                "`/leave` \u2014 Salir del canal de voz\n" +
                "`/filter <preset>` \u2014 Filtro de audio (bassboost, etc.)\n" +
                "`/nonstop` \u2014 Modo Nonstop: Tekk/Techno/Uptempo aleatorios");
        put("es", "help.other.title", "\u2139\uFE0F Otros");
        put("es", "help.other.body",
                "`/invite` \u2014 Invitar bot\n" +
                "`/info` \u2014 Info del bot\n" +
                "`/uptime` \u2014 Tiempo en l\u00ednea\n" +
                "`/language` \u2014 Cambiar idioma\n" +
                "`/help` \u2014 Esta ayuda");
        put("es", "help.footer", "Fuentes: YouTube, SoundCloud, Spotify, Radio  |  \u00a1Playlists soportadas!");
        put("es", "invite.title", "\uD83D\uDD17 Invitar bot");
        put("es", "invite.desc", "Haz clic en el enlace para invitar el bot a tu servidor:\n\n**[Haz clic aqu\u00ed]({0})**");
        put("es", "info.owner", "\uD83D\uDC51 Due\u00f1o");
        put("es", "info.servers", "\uD83C\uDF10 Servidores");
        put("es", "info.members", "\uD83D\uDC65 Miembros");
        put("es", "info.uptime", "\u23F0 Uptime");
        put("es", "info.active.players", "\uD83C\uDFB5 Reproductores activos");
        put("es", "info.support", "\uD83D\uDCE9 Soporte");
        put("es", "info.discord", "\uD83D\uDD17 Discord");
        put("es", "info.discord.value", "[Unirse al servidor](https://discord.gg/KqngYCVJqZ)");
        put("es", "info.footer", "Hecho con \u2764\uFE0F usando JDA + LavaPlayer");
        put("es", "ping.title", "\uD83C\uDFD3 \u00a1Pong!");
        put("es", "ping.gateway", "\uD83D\uDCE1 Gateway");
        put("es", "uptime.title", "\u23F0 Uptime & sistema");
        put("es", "uptime.online", "\u23F1\uFE0F En l\u00ednea desde");
        put("es", "uptime.cpu", "\uD83D\uDCBB CPU");
        put("es", "uptime.threads", "\uD83E\uDDE9 Threads");
        put("es", "uptime.bot.ram", "\uD83D\uDCBE RAM bot");
        put("es", "uptime.sys.ram", "\uD83D\uDDA5\uFE0F RAM sistema");
        put("es", "dcleave.not.owner", "\u26D4 Solo el due\u00f1o del bot puede usar este comando.");
        put("es", "dcleave.not.found", "\u274C Servidor no encontrado (ID: {0})");
        put("es", "dcleave.leaving", "\uD83D\uDC4B Saliendo del servidor **{0}** ({1})...");
        put("es", "page", "P\u00e1gina");
        put("es", "lang.title", "\uD83C\uDF0D Idioma");
        put("es", "lang.changed", "\u2705 Idioma cambiado a **{0}**");
        put("es", "lang.current", "Idioma actual: **{0}**\nElige un nuevo idioma con `/language code:<de|en|fr|es|it>`");

        // ===== ITALIANO =====
        put("it", "voice.required", "Devi essere in un canale vocale!");
        put("it", "nothing.playing", "Niente in riproduzione");
        put("it", "nothing.found", "Nessun risultato per: {0}");
        put("it", "load.error", "Errore di caricamento: {0}");
        put("it", "error.generic", "Errore: {0}");
        put("it", "queue.empty", "La coda \u00e8 vuota");
        put("it", "queue.already.empty", "La coda \u00e8 gi\u00e0 vuota");
        put("it", "spotify.not.configured", "Spotify non configurato! Aggiungi `spotify.client.id` e `spotify.client.secret` in config.properties.");
        put("it", "spotify.no.songs", "Nessun brano trovato in questo link Spotify.");
        put("it", "spotify.not.on.youtube", "Brano non trovato su YouTube.");
        put("it", "spotify.loading", "\uD83C\uDFB5 Carico **{0} brani** da Spotify...");
        put("it", "spotify.loaded", "\u2705 **{0}/{1} brani** caricati da Spotify");
        put("it", "spotify.failed.count", " ({0} non trovati)");
        put("it", "spotify.error", "Errore Spotify: {0}");
        put("it", "spotify.added", "\uD83C\uDFB5 **{0}** aggiunto alla coda (via Spotify)");
        put("it", "radio.unknown", "Stazione sconosciuta!");
        put("it", "radio.unreachable", "Stream radio non raggiungibile.");
        put("it", "radio.started", "\uD83D\uDCFB **{0}** avviata");
        put("it", "track.added", "**{0}** aggiunto alla coda");
        put("it", "track.duplicate", "\u26A0\uFE0F **{0}** \u00e8 gi\u00e0 in coda! Aggiunto comunque.");
        put("it", "playlist.added", "**{0} brani** dalla playlist **{1}** aggiunti");
        put("it", "skip.next", "\u23ED Brano saltato\n\uD83C\uDFB6 Ora: **{0}**");
        put("it", "skip.nonstop", "\u23ED Brano saltato \u2014 \uD83D\uDD25 carico prossima traccia random...");
        put("it", "skip.empty", "\u23ED Brano saltato \u2014 coda vuota");
        put("it", "stop.full", "\u23F9\uFE0F Musica fermata e coda svuotata\n\uD83D\uDCA4 Nonstop parte in 2 min se non suona nulla");
        put("it", "stop.short", "\u23F9\uFE0F Musica fermata \u2014 \uD83D\uDCA4 Nonstop in 2 min");
        put("it", "pause", "\u23F8\uFE0F In pausa");
        put("it", "resume", "\u25B6\uFE0F Ripreso");
        put("it", "now.playing.now", "\uD83C\uDFB6 **In riproduzione:**");
        put("it", "queue.list", "**Coda:**");
        put("it", "queue.title", "\uD83D\uDCCB Coda");
        put("it", "queue.footer", "{0} brani in coda  |  Durata totale: {1}{2}");
        put("it", "repeat.song.full", "  |  \uD83D\uDD02 Ripeti brano");
        put("it", "repeat.queue.full", "  |  \uD83D\uDD01 Ripeti coda");
        put("it", "repeat.song.short", "  |  \uD83D\uDD02 Brano");
        put("it", "repeat.queue.short", "  |  \uD83D\uDD01 Coda");
        put("it", "now.playing.title", "\uD83C\uDFB5 In riproduzione");
        put("it", "nonstop.loading", "\uD83D\uDD25 **Modalit\u00e0 Nonstop** \u2014 carico prossima traccia random...");
        put("it", "now.playing.status.vol", "\uD83D\uDD0A Vol: {0}%");
        put("it", "skipped.empty.short", "\u23ED Saltato \u2014 coda vuota");
        put("it", "volume.range", "Il volume deve essere tra 0 e 100");
        put("it", "volume.set", "\uD83D\uDD0A Volume: **{0}%**\n{1}");
        put("it", "joined", "\uD83D\uDD0A Connesso a **{0}**");
        put("it", "bye", "\uD83D\uDC4B Ciao!");
        put("it", "repeat.off", "\u27A1\uFE0F Ripeti: **Disattivato**");
        put("it", "repeat.track.set", "\uD83D\uDD02 Ripeti: **Brano**");
        put("it", "repeat.queue.set", "\uD83D\uDD01 Ripeti: **Coda**");
        put("it", "repeat.invalid", "Modalit\u00e0 invalida! Usa: `off`, `track` o `queue`");
        put("it", "shuffled", "\uD83D\uDD00 Coda mescolata! ({0} brani)");
        put("it", "pos.invalid", "Posizione invalida! La coda ha **{0}** brani.");
        put("it", "moved", "\u2195\uFE0F **{0}** spostato dalla posizione {1} a {2}");
        put("it", "move.failed", "Impossibile spostare il brano");
        put("it", "skipto.success", "\u23ED Saltato a **#{0}**: **{1}**");
        put("it", "skipto.failed", "Impossibile saltare");
        put("it", "save.title", "\uD83D\uDCBE Brano salvato");
        put("it", "save.open", "\uD83D\uDD17 Apri link");
        put("it", "save.dm.sent", "\uD83D\uDC8C Brano inviato in DM!");
        put("it", "save.dm.failed", "Impossibile inviare DM. Attiva i DM dai membri del server!");
        put("it", "save.dm.failed.short", "Impossibile inviare DM.");
        put("it", "auto.nonstop.off", "\uD83D\uDEAB **Auto-Nonstop disattivato** \u2014 il bot non avvier\u00e0 musica dopo 2 min di inattivit\u00e0");
        put("it", "auto.nonstop.on", "\u2705 **Auto-Nonstop attivato** \u2014 il bot riproduce automaticamente dopo 2 min di inattivit\u00e0");
        put("it", "nonstop.off", "\uD83D\uDCA4 **Modalit\u00e0 Nonstop disattivata**");
        put("it", "nonstop.on", "\uD83D\uDD25 **Modalit\u00e0 Nonstop attivata** \u2014 Tekk, Techno, Uptempo & Co. casuali");
        put("it", "filter.bassboost", "\uD83D\uDD0A **Bassboost** attivato");
        put("it", "filter.treble", "\uD83C\uDFB5 **Treble Boost** attivato");
        put("it", "filter.pop", "\uD83C\uDFA4 Equalizzatore **Pop** attivato");
        put("it", "filter.rock", "\uD83E\uDD18 Equalizzatore **Rock** attivato");
        put("it", "filter.off", "\u27A1\uFE0F Filtro **disattivato**");
        put("it", "seek.invalid", "Formato invalido! Usa es. `1:30` o `90` (secondi)");
        put("it", "seek.success", "\u23E9 Saltato a **{0}** / {1}");
        put("it", "removed", "\uD83D\uDDD1\uFE0F **{0}** rimosso dalla coda (posizione {1})");
        put("it", "remove.failed", "Impossibile rimuovere il brano");
        put("it", "cleared", "\uD83D\uDDD1\uFE0F **{0} brani** rimossi dalla coda");
        put("it", "help.title", "\uD83C\uDFB5 Music Bot \u2014 Aiuto");
        put("it", "help.music.title", "\uD83C\uDFA7 Musica");
        put("it", "help.music.body",
                "`/play <URL/ricerca>` \u2014 Brano, playlist o radio\n" +
                "`/skip` \u2014 Salta brano\n" +
                "`/stop` \u2014 Ferma musica & svuota coda\n" +
                "`/pause` \u2014 Pausa\n" +
                "`/resume` \u2014 Riprendi\n" +
                "`/volume <0-100>` \u2014 Cambia volume\n" +
                "`/radio <stazione>` \u2014 Radio live");
        put("it", "help.queue.title", "\uD83D\uDCCB Coda & riproduzione");
        put("it", "help.queue.body",
                "`/queue` \u2014 Mostra coda\n" +
                "`/playing` \u2014 Brano attuale con controlli\n" +
                "`/seek <tempo>` \u2014 Salta nel brano (es. 1:30)\n" +
                "`/skipto <posizione>` \u2014 Salta a brano in coda\n" +
                "`/move <da> <a>` \u2014 Sposta brano\n" +
                "`/repeat <off/track/queue>` \u2014 Modalit\u00e0 ripeti\n" +
                "`/shuffle` \u2014 Mescola coda\n" +
                "`/remove <posizione>` \u2014 Rimuovi dalla coda\n" +
                "`/clear` \u2014 Svuota coda\n" +
                "`/save` \u2014 Salva brano in DM");
        put("it", "help.voice.title", "\uD83D\uDD0A Voce & audio");
        put("it", "help.voice.body",
                "`/join` \u2014 Entra nel canale vocale\n" +
                "`/leave` \u2014 Esci dal canale vocale\n" +
                "`/filter <preset>` \u2014 Filtro audio (bassboost, ecc.)\n" +
                "`/nonstop` \u2014 Modalit\u00e0 Nonstop: Tekk/Techno/Uptempo casuali");
        put("it", "help.other.title", "\u2139\uFE0F Altro");
        put("it", "help.other.body",
                "`/invite` \u2014 Invita bot\n" +
                "`/info` \u2014 Info bot\n" +
                "`/uptime` \u2014 Tempo online\n" +
                "`/language` \u2014 Cambia lingua\n" +
                "`/help` \u2014 Questo aiuto");
        put("it", "help.footer", "Fonti: YouTube, SoundCloud, Spotify, Radio  |  Playlist supportate!");
        put("it", "invite.title", "\uD83D\uDD17 Invita bot");
        put("it", "invite.desc", "Clicca il link per invitare il bot nel tuo server:\n\n**[Clicca qui]({0})**");
        put("it", "info.owner", "\uD83D\uDC51 Proprietario");
        put("it", "info.servers", "\uD83C\uDF10 Server");
        put("it", "info.members", "\uD83D\uDC65 Membri");
        put("it", "info.uptime", "\u23F0 Uptime");
        put("it", "info.active.players", "\uD83C\uDFB5 Player attivi");
        put("it", "info.support", "\uD83D\uDCE9 Supporto");
        put("it", "info.discord", "\uD83D\uDD17 Discord");
        put("it", "info.discord.value", "[Entra nel server](https://discord.gg/KqngYCVJqZ)");
        put("it", "info.footer", "Fatto con \u2764\uFE0F usando JDA + LavaPlayer");
        put("it", "ping.title", "\uD83C\uDFD3 Pong!");
        put("it", "ping.gateway", "\uD83D\uDCE1 Gateway");
        put("it", "uptime.title", "\u23F0 Uptime & sistema");
        put("it", "uptime.online", "\u23F1\uFE0F Online da");
        put("it", "uptime.cpu", "\uD83D\uDCBB CPU");
        put("it", "uptime.threads", "\uD83E\uDDE9 Threads");
        put("it", "uptime.bot.ram", "\uD83D\uDCBE RAM bot");
        put("it", "uptime.sys.ram", "\uD83D\uDDA5\uFE0F RAM sistema");
        put("it", "dcleave.not.owner", "\u26D4 Solo il proprietario del bot pu\u00f2 usare questo comando.");
        put("it", "dcleave.not.found", "\u274C Server non trovato (ID: {0})");
        put("it", "dcleave.leaving", "\uD83D\uDC4B Esco dal server **{0}** ({1})...");
        put("it", "page", "Pagina");
        put("it", "lang.title", "\uD83C\uDF0D Lingua");
        put("it", "lang.changed", "\u2705 Lingua cambiata in **{0}**");
        put("it", "lang.current", "Lingua attuale: **{0}**\nScegli una nuova lingua con `/language code:<de|en|fr|es|it>`");
    }
}
