package bot;

import club.minnced.discord.jdave.interop.JDaveSessionFactory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MusicBot extends ListenerAdapter {

    public static final Properties CONFIG = new Properties();
    public static final Instant START_TIME = Instant.now();

    private static final List<CommandData> COMMANDS = List.of(
            Commands.slash("play", "Spielt einen Song von YouTube, SoundCloud oder Spotify")
                    .addOption(OptionType.STRING, "query", "URL oder Suchbegriff", true),
            Commands.slash("skip", "Ueberspringt den aktuellen Song"),
            Commands.slash("stop", "Stoppt die Musik und leert die Queue"),
            Commands.slash("pause", "Pausiert den aktuellen Song"),
            Commands.slash("resume", "Setzt den pausierten Song fort"),
            Commands.slash("queue", "Zeigt die aktuelle Warteschlange"),
            Commands.slash("playing", "Zeigt den aktuell spielenden Song"),
            Commands.slash("volume", "Setzt die Lautstaerke (0-100)")
                    .addOption(OptionType.INTEGER, "vol", "Lautstaerke 0-100", true),
            Commands.slash("join", "Bot betritt deinen Voice-Channel"),
            Commands.slash("leave", "Bot verlaesst den Voice-Channel"),
            Commands.slash("repeat", "Repeat-Modus: off, track oder queue")
                    .addOption(OptionType.STRING, "mode", "off / track / queue", true),
            Commands.slash("shuffle", "Mischt die aktuelle Queue"),
            Commands.slash("radio", "Spielt einen Radio-Sender")
                    .addOption(OptionType.STRING, "sender", "Sender waehlen", true, true),
            Commands.slash("seek", "Springt zu einer Position im Song")
                    .addOption(OptionType.STRING, "time", "Zeitposition (z.B. 1:30 oder 90)", true),
            Commands.slash("remove", "Entfernt einen Song aus der Queue")
                    .addOption(OptionType.INTEGER, "position", "Position in der Queue (1, 2, 3...)", true),
            Commands.slash("clear", "Leert die Warteschlange ohne den aktuellen Song zu stoppen"),
            Commands.slash("move", "Verschiebt einen Song in der Queue")
                    .addOption(OptionType.INTEGER, "von", "Aktuelle Position (1, 2, 3...)", true)
                    .addOption(OptionType.INTEGER, "nach", "Neue Position (1, 2, 3...)", true),
            Commands.slash("skipto", "Springt direkt zu einem Song in der Queue")
                    .addOption(OptionType.INTEGER, "position", "Position in der Queue", true),
            Commands.slash("save", "Schickt dir den aktuellen Song per DM"),
            Commands.slash("nonstop", "Nonstop-Modus — random Tekk, Techno, Uptempo & Co.")
                    .addOptions(new net.dv8tion.jda.api.interactions.commands.build.OptionData(OptionType.STRING, "modus", "Optional: auto-on / auto-off", false)
                            .addChoice("auto: an (Standard)", "auto-on")
                            .addChoice("auto: aus", "auto-off")),
            Commands.slash("filter", "Audio-Filter / Equalizer Preset")
                    .addOption(OptionType.STRING, "preset", "Filter waehlen", true, true),
            Commands.slash("invite", "Einladungslink fuer den Bot"),
            Commands.slash("help", "Erhalte Hilfe"),
            Commands.slash("info", "Infos ueber den Bot"),
            Commands.slash("uptime", "Zeigt wie lange der Bot schon online ist"),
            Commands.slash("ping", "Zeigt die Latenz des Bots"),
            Commands.slash("dcleave", "Bot verlaesst einen ausgewaehlten Discord-Server (nur Bot-Owner)")
                    .addOption(OptionType.STRING, "server", "Discord-Server auswaehlen", true, true),
            Commands.slash("language", "Sprache des Bots aendern / Change bot language")
                    .addOptions(new net.dv8tion.jda.api.interactions.commands.build.OptionData(OptionType.STRING, "code", "Sprache / Language", false)
                            .addChoice("\uD83C\uDDE9\uD83C\uDDEA Deutsch", "de")
                            .addChoice("\uD83C\uDDEC\uD83C\uDDE7 English", "en")
                            .addChoice("\uD83C\uDDEB\uD83C\uDDF7 Fran\u00e7ais", "fr")
                            .addChoice("\uD83C\uDDEA\uD83C\uDDF8 Espa\u00f1ol", "es")
                            .addChoice("\uD83C\uDDEE\uD83C\uDDF9 Italiano", "it"))
    );

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        registerForGuild(event.getGuild());
    }

    private static void registerForGuild(Guild guild) {
        guild.updateCommands().addCommands(COMMANDS).queue(
                cmds -> System.out.println("Commands registriert fuer: " + guild.getName()),
                err -> System.err.println("Fehler bei " + guild.getName() + ": " + err.getMessage())
        );
    }

    public static void main(String[] args) throws InterruptedException {
        try (var in = new FileInputStream("config.properties")) {
            CONFIG.load(in);
        } catch (IOException e) {
            System.err.println("config.properties nicht gefunden! Erstelle die Datei neben der JAR.");
            System.exit(1);
        }

        String token = CONFIG.getProperty("bot.token");
        if (token == null || token.isBlank() || token.equals("DEIN_TOKEN_HIER")) {
            System.err.println("Trage deinen Bot-Token in config.properties ein!");
            System.exit(1);
        }

        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(
                        GatewayIntent.GUILD_VOICE_STATES,
                        GatewayIntent.GUILD_MESSAGES
                )
                .enableCache(CacheFlag.VOICE_STATE, CacheFlag.MEMBER_OVERRIDES)
                .setAudioModuleConfig(new AudioModuleConfig()
                        .withDaveSessionFactory(new JDaveSessionFactory()))
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.playing("Starte..."))
                .addEventListeners(new CommandHandler(), new MusicBot())
                .build();

        jda.awaitReady();
        System.out.println("Bot ist verbunden! Registriere Slash-Commands...");

        for (Guild guild : jda.getGuilds()) {
            registerForGuild(guild);
        }

        // Dynamischer Status: wechselt alle 15 Sekunden (immer Streaming-Modus)
        var scheduler = Executors.newSingleThreadScheduledExecutor();
        final int commandCount = COMMANDS.size();
        final String[] statusMessages = {
                "mit {members} Membern auf {servers} Servern",
                "mit /help",
                "mit " + commandCount + " Commands"
        };
        final int[] index = {0};
        scheduler.scheduleAtFixedRate(() -> {
            try {
                var guilds = jda.getGuilds();
                int servers = guilds.size();
                int members = guilds.stream()
                        .mapToInt(Guild::getMemberCount)
                        .sum();
                String msg = statusMessages[index[0] % statusMessages.length]
                        .replace("{members}", String.valueOf(members))
                        .replace("{servers}", String.valueOf(servers));
                jda.getPresence().setActivity(
                        Activity.streaming(msg, "https://www.twitch.tv/placeholder"));
                index[0]++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 15, TimeUnit.SECONDS);

        System.out.println("Music Bot ist online!");
    }
}
