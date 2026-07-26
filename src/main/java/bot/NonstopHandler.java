package bot;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class NonstopHandler {

    private static final Logger log = LoggerFactory.getLogger(NonstopHandler.class);

    private static final long AUTO_NONSTOP_DELAY_MS = 2 * 60 * 1000L;
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final int MAX_HISTORY_SIZE = 200;

    private static final Map<Long, Deque<String>> playedHistory = new ConcurrentHashMap<>();

    public static final String[] DEFAULT_GENRES = {
            "tekk", "hardtekk", "techno", "uptempo", "uptempo hardcore",
            "frenchcore", "hardstyle", "raw hardstyle", "rawstyle",
            "schranz", "hardcore", "minimal techno", "acid techno",
            "psytrance", "rave", "hard techno",
            "rock", "classic rock", "hard rock", "alternative rock",
            "rock hits", "deutschrock",
            "schlager", "schlager hits", "deutsche schlager", "schlager party",
            "disco fox", "schlager mix", "party mix",
            "dubstep", "drum and bass", "trance", "eurodance",
            "90er techno", "90er hits", "80er hits",
            "punk rock", "indie rock", "metal", "death metal",
            "pop rock", "dance pop", "electro pop",
            "house", "deep house", "tech house", "progressive house",
            "trap", "phonk", "vaporwave", "synthwave"
    };

    public static final String[] DEFAULT_MODIFIERS = {
            "official audio", "official video", "official mix",
            "music video", "lyrics", "live performance",
            "best of", "greatest hits", "top hit",
            "remix", "club mix", "extended mix", "radio edit",
            "year end mix", "megamix", "nonstop mix",
            "DJ mix", "set", "compilation",
            "new", "popular", "chart", "hit"
    };

    private static final String[] SEARCH_TEMPLATES = {
            "%s %s",
            "%s %s %s",
            "%s %s official",
            "%s best %s",
            "%s top %s",
            "%s mix %s",
    };

    private static final String[] EXTRA_TERMS = {
            "", "2024", "2025", "2026", "german", "dance", "club", "party",
            "energy", "bass", "hard", "raw", "classic", "festival"
    };

    private NonstopHandler() {}

    public static void installIdleHandler(BotContext ctx, long guildId, GuildMusicManager musicManager) {
        musicManager.scheduler.setOnIdle(() -> {
            if (ctx.nonstopGuilds.contains(guildId)) {
                queueRandomNonstopTrack(ctx, guildId, musicManager);
            } else {
                scheduleAutoNonstop(ctx, guildId, musicManager);
            }
        });
    }

    public static void scheduleAutoNonstop(BotContext ctx, long guildId, GuildMusicManager musicManager) {
        cancelAutoNonstop(ctx, guildId);
        if (ctx.autoNonstopDisabledGuilds.contains(guildId)) {
            log.debug("[AutoNonstop] Deaktiviert fuer guild {}", guildId);
            return;
        }
        log.info("[AutoNonstop] Idle timer (2min) gestartet fuer guild {}", guildId);
        ScheduledFuture<?> task = ctx.npScheduler.schedule(() -> {
            try {
                if (musicManager.player.getPlayingTrack() != null) return;
                if (!musicManager.scheduler.getQueue().isEmpty()) return;
                Guild guild = MusicBot.JDA != null ? MusicBot.JDA.getGuildById(guildId) : null;
                if (guild == null || !guild.getAudioManager().isConnected()) {
                    log.info("[AutoNonstop] Guild nicht verbunden -> cleanup");
                    ctx.cleanupGuild(guildId);
                    return;
                }
                if (ctx.autoNonstopDisabledGuilds.contains(guildId)) return;
                log.info("[AutoNonstop] 2min Idle - starte Nonstop fuer guild {}", guildId);
                ctx.nonstopGuilds.add(guildId);
                installIdleHandler(ctx, guildId, musicManager);
                queueRandomNonstopTrack(ctx, guildId, musicManager);
            } catch (Exception e) {
                log.error("[AutoNonstop] Fehler fuer guild {}", guildId, e);
            }
        }, AUTO_NONSTOP_DELAY_MS, TimeUnit.MILLISECONDS);
        ctx.autoNonstopTimers.put(guildId, task);
    }

    public static void cancelAutoNonstop(BotContext ctx, long guildId) {
        ScheduledFuture<?> t = ctx.autoNonstopTimers.remove(guildId);
        if (t != null) t.cancel(false);
    }

    public static void queueRandomNonstopTrack(BotContext ctx, long guildId, GuildMusicManager musicManager) {
        queueRandomNonstopTrack(ctx, guildId, musicManager, 0);
    }

    public static void queueRandomNonstopTrack(BotContext ctx, long guildId, GuildMusicManager musicManager, int attempt) {
        if (attempt >= MAX_RETRY_ATTEMPTS) {
            log.warn("[Nonstop] Konnte nach {} Versuchen keinen Track laden - retry in 2 Min", MAX_RETRY_ATTEMPTS);
            ctx.nonstopGuilds.remove(guildId);
            scheduleAutoNonstop(ctx, guildId, musicManager);
            return;
        }

        String query = buildSearchQuery(ctx);
        log.debug("[Nonstop] Suche: {} (attempt {})", query, attempt);

        ctx.playerManager.loadItemOrdered(musicManager, query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                if (!isDuplicate(guildId, track)) {
                    musicManager.scheduler.queue(track);
                    recordPlayed(guildId, track);
                } else {
                    log.debug("[Nonstop] Duplikat, versuche naechsten: {}", track.getInfo().title);
                    queueRandomNonstopTrack(ctx, guildId, musicManager, attempt + 1);
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                List<AudioTrack> filtered = new ArrayList<>();
                for (AudioTrack t : playlist.getTracks()) {
                    if (!isLiveOrRecording(t) && !isDuplicate(guildId, t)) {
                        filtered.add(t);
                    }
                    if (filtered.size() >= 15) break;
                }
                if (filtered.isEmpty()) {
                    log.debug("[Nonstop] Alle Ergebnisse duplikat/live, versuche neuen Query");
                    queueRandomNonstopTrack(ctx, guildId, musicManager, attempt + 1);
                    return;
                }
                AudioTrack pick = filtered.get(ctx.nonstopRandom.nextInt(filtered.size()));
                musicManager.scheduler.queue(pick);
                recordPlayed(guildId, pick);
            }

            @Override
            public void noMatches() {
                queueRandomNonstopTrack(ctx, guildId, musicManager, attempt + 1);
            }

            @Override
            public void loadFailed(FriendlyException e) {
                log.warn("[Nonstop] Load failed: {}", e.getMessage());
                queueRandomNonstopTrack(ctx, guildId, musicManager, attempt + 1);
            }
        });
    }

    private static String buildSearchQuery(BotContext ctx) {
        String genre = ctx.nonstopGenres[ctx.nonstopRandom.nextInt(ctx.nonstopGenres.length)];
        String modifier = ctx.nonstopModifiers[ctx.nonstopRandom.nextInt(ctx.nonstopModifiers.length)];
        String extra = EXTRA_TERMS[ctx.nonstopRandom.nextInt(EXTRA_TERMS.length)];
        String template = SEARCH_TEMPLATES[ctx.nonstopRandom.nextInt(SEARCH_TEMPLATES.length)];

        String query = String.format(template, genre, modifier);
        if (!extra.isEmpty() && template.contains("%s")) {
            try {
                query = String.format(template, genre, modifier, extra);
            } catch (Exception ignored) {
                query = genre + " " + modifier + " " + extra;
            }
        }
        return "ytsearch:" + query.trim();
    }

    private static boolean isDuplicate(long guildId, AudioTrack track) {
        Deque<String> history = playedHistory.get(guildId);
        if (history == null || history.isEmpty()) return false;
        String uri = track.getInfo().uri;
        if (uri == null) return false;
        return history.contains(uri);
    }

    private static void recordPlayed(long guildId, AudioTrack track) {
        Deque<String> history = playedHistory.computeIfAbsent(guildId, k -> new ArrayDeque<>());
        String uri = track.getInfo().uri;
        if (uri != null) {
            history.addLast(uri);
            while (history.size() > MAX_HISTORY_SIZE) {
                history.removeFirst();
            }
        }
    }

    public static void clearHistory(long guildId) {
        playedHistory.remove(guildId);
    }

    public static boolean isLiveOrRecording(AudioTrack track) {
        if (track.getInfo().isStream) return true;
        String title = track.getInfo().title;
        if (title == null) return false;
        String t = title.toLowerCase();
        return t.contains("live") || t.contains("aufnahme") || t.contains("bootleg")
                || t.contains("concert") || t.contains("konzert");
    }

    public static void handleNonstop(SlashCommandInteractionEvent event, BotContext ctx) {
        Guild guild = event.getGuild();
        long guildId = guild.getIdLong();
        GuildMusicManager musicManager = ctx.getGuildMusic(guild);

        var modusOpt = event.getOption("modus");
        if (modusOpt != null) {
            String modus = modusOpt.getAsString();
            if (modus.equals("auto-off")) {
                ctx.autoNonstopDisabledGuilds.add(guildId);
                cancelAutoNonstop(ctx, guildId);
                event.replyEmbeds(new EmbedBuilder()
                        .setDescription(Lang.t(guildId, "auto.nonstop.off"))
                        .setColor(0xED4245).build()).queue();
                return;
            } else if (modus.equals("auto-on")) {
                ctx.autoNonstopDisabledGuilds.remove(guildId);
                if (ctx.musicManagers.containsKey(guildId)) {
                    installIdleHandler(ctx, guildId, musicManager);
                    if (musicManager.player.getPlayingTrack() == null && musicManager.scheduler.getQueue().isEmpty()) {
                        scheduleAutoNonstop(ctx, guildId, musicManager);
                    }
                }
                event.replyEmbeds(new EmbedBuilder()
                        .setDescription(Lang.t(guildId, "auto.nonstop.on"))
                        .setColor(0x57F287).build()).queue();
                return;
            }
        }

        if (ctx.nonstopGuilds.contains(guildId)) {
            ctx.nonstopGuilds.remove(guildId);
            installIdleHandler(ctx, guildId, musicManager);
            event.replyEmbeds(new EmbedBuilder()
                    .setDescription(Lang.t(guildId, "nonstop.off"))
                    .setColor(0xED4245).build()).queue();
            return;
        }

        GuildVoiceState voiceState = event.getMember().getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            event.reply(Lang.t(guildId, "voice.required")).setEphemeral(true).queue();
            return;
        }
        AudioChannelUnion channel = voiceState.getChannel();

        ctx.nonstopGuilds.add(guildId);
        cancelAutoNonstop(ctx, guildId);
        installIdleHandler(ctx, guildId, musicManager);

        AudioManager audioManager = guild.getAudioManager();
        audioManager.setSendingHandler(musicManager.sendHandler);
        audioManager.setSelfDeafened(true);
        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(channel);
        }
        ctx.lastChannelIds.put(guildId, channel.getIdLong());

        event.replyEmbeds(new EmbedBuilder()
                .setDescription(Lang.t(guildId, "nonstop.on"))
                .setColor(0x57F287).build()).queue();

        if (musicManager.player.getPlayingTrack() == null) {
            queueRandomNonstopTrack(ctx, guildId, musicManager);
        }
    }
}
