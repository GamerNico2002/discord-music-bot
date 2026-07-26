package bot;

import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.ArrayList;
import java.util.List;

/** Central command router that dispatches slash commands, button interactions, and autocompletes to the appropriate handler. */
public class CommandHandler extends ListenerAdapter {

    private final BotContext ctx;
    private final PlaybackHandler playback;
    private final QueueHandler queue;
    private final PlaylistHandler playlist;
    private final RadioHandler radio;
    private final FilterHandler filter;
    private final VoiceHandler voice;
    private final InfoHandler info;

    public CommandHandler(BotContext ctx) {
        this.ctx = ctx;
        this.playback = new PlaybackHandler(ctx);
        this.queue = new QueueHandler(ctx);
        this.playlist = new PlaylistHandler(ctx);
        this.radio = new RadioHandler(ctx);
        this.filter = new FilterHandler(ctx);
        this.voice = new VoiceHandler(ctx);
        this.info = new InfoHandler(ctx);
    }

    /** Routes incoming slash command interactions to the matching handler method. */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;

        switch (event.getName()) {
            case "play" -> playback.handlePlay(event);
            case "skip" -> playback.handleSkip(event);
            case "stop" -> playback.handleStop(event);
            case "pause" -> playback.handlePause(event);
            case "resume" -> playback.handleResume(event);
            case "seek" -> playback.handleSeek(event);
            case "queue" -> queue.handleQueue(event);
            case "playing" -> queue.handleNowPlaying(event);
            case "volume" -> queue.handleVolume(event);
            case "move" -> queue.handleMove(event);
            case "remove" -> queue.handleRemove(event);
            case "clear" -> queue.handleClear(event);
            case "shuffle" -> queue.handleShuffle(event);
            case "repeat" -> queue.handleRepeat(event);
            case "skipto" -> queue.handleSkipTo(event);
            case "save" -> queue.handleSave(event);
            case "nonstop" -> NonstopHandler.handleNonstop(event, ctx);
            case "radio" -> radio.handleRadio(event);
            case "filter" -> filter.handleFilter(event);
            case "join" -> voice.handleJoin(event);
            case "leave" -> voice.handleLeave(event);
            case "playlist" -> playlist.handle(event);
            case "help" -> info.handleHelp(event);
            case "info" -> info.handleInfo(event);
            case "ping" -> info.handlePing(event);
            case "invite" -> info.handleInvite(event);
            case "dcleave" -> info.handleDcLeave(event);
            case "language" -> info.handleLanguage(event);
        }
    }

    /** Dispatches button interactions (queue pagination, now-playing controls) to the queue handler. */
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getGuild() == null) return;
        queue.onButtonInteraction(event);
    }

    /** Routes autocomplete requests for /radio, /filter, /dcleave and /playlist to the matching handler. */
    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("playlist") && event.getFocusedOption().getName().equals("name")) {
            playlist.handleAutocomplete(event);
            return;
        }

        String input = event.getFocusedOption().getValue().toLowerCase();
        List<Command.Choice> choices = new ArrayList<>();

        if (event.getName().equals("radio") && event.getFocusedOption().getName().equals("sender")) {
            choices = radio.autocomplete(input);
        } else if (event.getName().equals("filter") && event.getFocusedOption().getName().equals("preset")) {
            choices = filter.autocomplete(input);
        } else if (event.getName().equals("dcleave") && event.getFocusedOption().getName().equals("server")) {
            choices = info.autocompleteDcLeave(event, input);
        }

        event.replyChoices(choices).queue();
    }

    /** Forwards voice-channel update events to the voice handler for auto-reconnect logic. */
    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        voice.onGuildVoiceUpdate(event);
    }
}
