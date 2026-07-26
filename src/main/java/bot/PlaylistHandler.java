package bot;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles the /playlist slash command with subcommands for managing per-user, per-guild playlists.
 */
public class PlaylistHandler {

    private static final Logger log = LoggerFactory.getLogger(PlaylistHandler.class);

    private final BotContext ctx;

    public PlaylistHandler(BotContext ctx) {
        this.ctx = ctx;
    }

    public void handle(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;
        long gid = event.getGuild().getIdLong();
        long uid = event.getUser().getIdLong();
        String sub = event.getSubcommandName();

        if (sub == null) {
            List<Playlist> playlists = PlaylistManager.load(gid, uid);
            if (playlists.isEmpty()) {
                event.reply(Lang.t(gid, "playlist.empty")).setEphemeral(true).queue();
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(Lang.t(gid, "playlist.list", playlists.size(), PlaylistManager.MAX_PLAYLISTS)).append("\n");
            for (int i = 0; i < playlists.size(); i++) {
                Playlist pl = playlists.get(i);
                sb.append("`").append(i + 1).append(".` **").append(pl.getName()).append("** \u2014 ")
                  .append(pl.getSongs().size()).append(" Songs\n");
            }
            event.replyEmbeds(new EmbedBuilder()
                    .setDescription(sb.toString())
                    .setColor(0x5865F2).build()).queue();
            return;
        }

        switch (sub) {
            case "create" -> handleCreate(event, gid, uid);
            case "add" -> handleAdd(event, gid, uid);
            case "remove" -> handleRemove(event, gid, uid);
            case "delete" -> handleDelete(event, gid, uid);
            case "view" -> handleView(event, gid, uid);
            case "play" -> handlePlay(event, gid, uid);
        }
    }

    private void handleCreate(SlashCommandInteractionEvent event, long gid, long uid) {
        var nameOpt = event.getOption("name");
        if (nameOpt == null) {
            event.reply(Lang.t(gid, "error.generic", "Missing playlist name")).setEphemeral(true).queue();
            return;
        }
        String name = nameOpt.getAsString().trim();
        if (name.length() > 100) {
            event.reply("Playlist-Name zu lang (max 100 Zeichen)").setEphemeral(true).queue();
            return;
        }
        List<Playlist> playlists = PlaylistManager.load(gid, uid);
        if (!PlaylistManager.canCreate(gid, uid)) {
            event.reply(Lang.t(gid, "playlist.create.limit", PlaylistManager.MAX_PLAYLISTS)).setEphemeral(true).queue();
            return;
        }
        if (PlaylistManager.find(playlists, name) != null) {
            event.reply(Lang.t(gid, "playlist.exists", name)).setEphemeral(true).queue();
            return;
        }
        playlists.add(new Playlist(name));
        PlaylistManager.save(gid, uid, playlists);
        event.replyEmbeds(new EmbedBuilder()
                .setDescription(Lang.t(gid, "playlist.created", name))
                .setColor(0x57F287).build()).queue();
    }

    private void handleAdd(SlashCommandInteractionEvent event, long gid, long uid) {
        var nameOpt = event.getOption("name");
        if (nameOpt == null) {
            event.reply(Lang.t(gid, "error.generic", "Missing playlist name")).setEphemeral(true).queue();
            return;
        }
        String name = nameOpt.getAsString().trim();
        List<Playlist> playlists = PlaylistManager.load(gid, uid);
        Playlist pl = PlaylistManager.find(playlists, name);
        if (pl == null) {
            event.reply(Lang.t(gid, "playlist.not.found", name)).setEphemeral(true).queue();
            return;
        }
        var queryOpt = event.getOption("query");
        if (queryOpt == null) {
            event.reply(Lang.t(gid, "error.generic", "Missing query")).setEphemeral(true).queue();
            return;
        }
        String query = queryOpt.getAsString().trim();

        boolean isPlaylistUrl = query.contains("playlist?list=") || query.contains("&list=")
                || query.contains("/sets/") || query.contains("/playlists/")
                || query.contains("album/");

        if (ctx.spotify.isSpotifyUrl(query)) {
            handleAddSpotify(event, gid, uid, pl, playlists, query);
        } else if (isPlaylistUrl) {
            handleAddUrl(event, gid, uid, pl, playlists, query);
        } else {
            pl.getSongs().add(new Playlist.Song(query, query));
            PlaylistManager.save(gid, uid, playlists);
            event.replyEmbeds(new EmbedBuilder()
                    .setDescription(Lang.t(gid, "playlist.added", query, pl.getName()))
                    .setColor(0x57F287).build()).queue();
        }
    }

    private void handleAddSpotify(SlashCommandInteractionEvent event, long gid, long uid,
                                  Playlist pl, List<Playlist> playlists, String query) {
        if (!ctx.spotify.isConfigured()) {
            event.reply(Lang.t(gid, "spotify.not.configured")).setEphemeral(true).queue();
            return;
        }
        event.deferReply().queue();
        ctx.spotify.resolveAsync(query).thenAcceptAsync(searches -> {
            if (searches.isEmpty()) {
                event.getHook().sendMessage(Lang.t(gid, "spotify.no.songs")).queue();
                return;
            }
            for (String s : searches) {
                String title = s.replace("ytsearch:", "");
                pl.getSongs().add(new Playlist.Song(title, s));
            }
            PlaylistManager.save(gid, uid, playlists);
            event.getHook().sendMessageEmbeds(new EmbedBuilder()
                    .setDescription(Lang.t(gid, "playlist.added",
                            searches.size() + " Songs", pl.getName()))
                    .setColor(0x1DB954).build()).queue();
        }, ctx.npScheduler).exceptionally(e -> {
            event.getHook().sendMessage(Lang.t(gid, "spotify.error",
                    e.getCause().getMessage())).queue();
            return null;
        });
    }

    private void handleAddUrl(SlashCommandInteractionEvent event, long gid, long uid,
                              Playlist pl, List<Playlist> playlists, String query) {
        event.deferReply().queue();
        Guild guild = event.getGuild();
        GuildMusicManager musicManager = ctx.getGuildMusic(guild);
        ctx.playerManager.loadItemOrdered(musicManager, query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                pl.getSongs().add(new Playlist.Song(track.getInfo().title, track.getInfo().uri));
                PlaylistManager.save(gid, uid, playlists);
                event.getHook().sendMessageEmbeds(new EmbedBuilder()
                        .setDescription(Lang.t(gid, "playlist.added", track.getInfo().title, pl.getName()))
                        .setColor(0x57F287).build()).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                for (AudioTrack track : playlist.getTracks()) {
                    pl.getSongs().add(new Playlist.Song(track.getInfo().title, track.getInfo().uri));
                }
                PlaylistManager.save(gid, uid, playlists);
                event.getHook().sendMessageEmbeds(new EmbedBuilder()
                        .setDescription(Lang.t(gid, "playlist.added",
                                playlist.getTracks().size() + " Songs", pl.getName()))
                        .setColor(0x57F287).build()).queue();
            }

            @Override
            public void noMatches() {
                event.getHook().sendMessage(Lang.t(gid, "nothing.found", query)).queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                event.getHook().sendMessage(Lang.t(gid, "load.error", e.getMessage())).queue();
            }
        });
    }

    private void handleRemove(SlashCommandInteractionEvent event, long gid, long uid) {
        var nameOpt = event.getOption("name");
        var indexOpt = event.getOption("index");
        if (nameOpt == null || indexOpt == null) {
            event.reply(Lang.t(gid, "error.generic", "Missing parameters")).setEphemeral(true).queue();
            return;
        }
        String name = nameOpt.getAsString().trim();
        int index = indexOpt.getAsInt();
        List<Playlist> playlists = PlaylistManager.load(gid, uid);
        Playlist pl = PlaylistManager.find(playlists, name);
        if (pl == null) {
            event.reply(Lang.t(gid, "playlist.not.found", name)).setEphemeral(true).queue();
            return;
        }
        if (index < 1 || index > pl.getSongs().size()) {
            event.reply("Ungueltiger Index. Playlist hat " + pl.getSongs().size() + " Songs.").setEphemeral(true).queue();
            return;
        }
        Playlist.Song removed = pl.getSongs().remove(index - 1);
        PlaylistManager.save(gid, uid, playlists);
        event.replyEmbeds(new EmbedBuilder()
                .setDescription(Lang.t(gid, "playlist.removed", removed.getTitle(), pl.getName()))
                .setColor(0xED4245).build()).queue();
    }

    private void handleDelete(SlashCommandInteractionEvent event, long gid, long uid) {
        var nameOpt = event.getOption("name");
        if (nameOpt == null) {
            event.reply(Lang.t(gid, "error.generic", "Missing playlist name")).setEphemeral(true).queue();
            return;
        }
        String name = nameOpt.getAsString().trim();
        List<Playlist> playlists = PlaylistManager.load(gid, uid);
        Playlist pl = PlaylistManager.find(playlists, name);
        if (pl == null) {
            event.reply(Lang.t(gid, "playlist.not.found", name)).setEphemeral(true).queue();
            return;
        }
        playlists.remove(pl);
        PlaylistManager.save(gid, uid, playlists);
        event.replyEmbeds(new EmbedBuilder()
                .setDescription(Lang.t(gid, "playlist.deleted", name))
                .setColor(0xED4245).build()).queue();
    }

    private void handleView(SlashCommandInteractionEvent event, long gid, long uid) {
        var nameOpt = event.getOption("name");
        if (nameOpt == null) {
            event.reply(Lang.t(gid, "error.generic", "Missing playlist name")).setEphemeral(true).queue();
            return;
        }
        String name = nameOpt.getAsString().trim();
        List<Playlist> playlists = PlaylistManager.load(gid, uid);
        Playlist pl = PlaylistManager.find(playlists, name);
        if (pl == null) {
            event.reply(Lang.t(gid, "playlist.not.found", name)).setEphemeral(true).queue();
            return;
        }
        List<Playlist.Song> songs = pl.getSongs();
        if (songs.isEmpty()) {
            event.reply(Lang.t(gid, "playlist.songs", pl.getName(), 0) + "\nPlaylist ist leer.").queue();
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(Lang.t(gid, "playlist.songs", pl.getName(), songs.size())).append("\n");
        for (int i = 0; i < songs.size() && i < 20; i++) {
            Playlist.Song s = songs.get(i);
            sb.append("`").append(String.format("%2d", i + 1)).append(".` **").append(s.getTitle()).append("**\n");
        }
        if (songs.size() > 20) {
            sb.append("... und ").append(songs.size() - 20).append(" weitere Songs");
        }
        event.replyEmbeds(new EmbedBuilder()
                .setDescription(sb.toString())
                .setColor(0x5865F2).build()).queue();
    }

    private void handlePlay(SlashCommandInteractionEvent event, long gid, long uid) {
        GuildVoiceState voiceState = event.getMember().getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            event.reply(Lang.t(gid, "voice.required")).setEphemeral(true).queue();
            return;
        }
        var nameOpt = event.getOption("name");
        if (nameOpt == null) {
            event.reply(Lang.t(gid, "error.generic", "Missing playlist name")).setEphemeral(true).queue();
            return;
        }
        String name = nameOpt.getAsString().trim();
        List<Playlist> playlists = PlaylistManager.load(gid, uid);
        Playlist pl = PlaylistManager.find(playlists, name);
        if (pl == null) {
            event.reply(Lang.t(gid, "playlist.not.found", name)).setEphemeral(true).queue();
            return;
        }
        List<Playlist.Song> songs = pl.getSongs();
        if (songs.isEmpty()) {
            event.reply("Playlist **" + name + "** ist leer.").setEphemeral(true).queue();
            return;
        }

        event.deferReply().queue();

        AudioChannelUnion channel = voiceState.getChannel();
        Guild guild = event.getGuild();
        GuildMusicManager musicManager = ctx.getGuildMusic(guild);

        ctx.nonstopGuilds.remove(gid);
        musicManager.scheduler.clearQueue();
        musicManager.player.stopTrack();

        AtomicInteger loaded = new AtomicInteger(0);
        for (Playlist.Song song : songs) {
            String q = song.getUrl();
            if (!q.startsWith("http://") && !q.startsWith("https://")) {
                q = "ytsearch:" + q;
            }
            String finalQ = q;
            ctx.playerManager.loadItemOrdered(musicManager, q, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    if (loaded.get() == 0) {
                        ctx.connectAndPlay(guild, channel, musicManager, track);
                    } else {
                        musicManager.scheduler.queue(track);
                    }
                    loaded.incrementAndGet();
                    checkDone();
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    if (!playlist.isSearchResult() && !playlist.getTracks().isEmpty()) {
                        for (AudioTrack t : playlist.getTracks()) {
                            if (loaded.get() == 0) {
                                ctx.connectAndPlay(guild, channel, musicManager, t);
                                loaded.incrementAndGet();
                            } else {
                                musicManager.scheduler.queue(t);
                                loaded.incrementAndGet();
                            }
                        }
                    } else if (!playlist.getTracks().isEmpty()) {
                        AudioTrack first = playlist.getTracks().get(0);
                        if (loaded.get() == 0) {
                            ctx.connectAndPlay(guild, channel, musicManager, first);
                        } else {
                            musicManager.scheduler.queue(first);
                        }
                        loaded.incrementAndGet();
                    }
                    checkDone();
                }

                @Override
                public void noMatches() {
                    checkDone();
                }

                @Override
                public void loadFailed(FriendlyException e) {
                    log.error("[Playlist] Load failed: {} - {}", finalQ, e.getMessage());
                    checkDone();
                }

                private void checkDone() {
                    if (loaded.get() >= songs.size()) {
                        event.getHook().sendMessageEmbeds(new EmbedBuilder()
                                .setDescription(Lang.t(gid, "playlist.playing.started", pl.getName(), loaded.get()))
                                .setColor(0x5865F2).build()).queue();
                    }
                }
            });
        }
    }

    public void handleAutocomplete(CommandAutoCompleteInteractionEvent event) {
        String input = event.getFocusedOption().getValue().toLowerCase();
        List<Command.Choice> choices = new ArrayList<>();
        long uid = event.getUser().getIdLong();
        long gid = event.getGuild().getIdLong();
        List<Playlist> playlists = PlaylistManager.load(gid, uid);
        for (Playlist pl : playlists) {
            if (choices.size() >= 25) break;
            String plName = pl.getName();
            if (input.isBlank() || plName.toLowerCase().contains(input)) {
                String label = plName.length() > 100 ? plName.substring(0, 97) + "..." : plName;
                choices.add(new Command.Choice(label, plName));
            }
        }
        event.replyChoices(choices).queue();
    }
}
