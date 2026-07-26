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
import net.dv8tion.jda.api.interactions.commands.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Handles the /radio slash command, providing 23 pre-configured German and international radio stations. */
public class RadioHandler {

    private static final Logger log = LoggerFactory.getLogger(RadioHandler.class);

    private final BotContext ctx;

    static final Map<String, String[]> RADIO_STATIONS = new LinkedHashMap<>() {{
        put("1live",       new String[]{"\uD83D\uDCFB 1LIVE",                   "https://wdr-1live-live.icecastssl.wdr.de/wdr/1live/live/mp3/128/stream.mp3"});
        put("wdr2",        new String[]{"\uD83D\uDCFB WDR 2",                   "https://wdr-wdr2-rheinland.icecastssl.wdr.de/wdr/wdr2/rheinland/mp3/128/stream.mp3"});
        put("swr3",        new String[]{"\uD83D\uDCFB SWR3",                    "https://liveradio.swr.de/sw282p3/swr3/play.mp3"});
        put("bayern3",     new String[]{"\uD83D\uDCFB Bayern 3",                "https://streams.br.de/bayern3_2.m3u"});
        put("bigfm",       new String[]{"\uD83D\uDCFB bigFM",                   "https://streams.bigfm.de/bigfm-deutschland-128-mp3"});
        put("radiobob",    new String[]{"\uD83C\uDFA4 Radio BOB!",              "https://streams.radiobob.de/bob-live/mp3-192/mediaplayer"});
        put("nrj",         new String[]{"\uD83D\uDCFB Energy/NRJ",              "https://frontend.streamonkey.net/energy-madeingermany/stream/mp3"});
        put("antenne",     new String[]{"\uD83D\uDCFB Antenne Bayern",          "https://stream.antenne.de/antenne/stream/mp3"});
        put("jump",        new String[]{"\uD83D\uDCFB MDR JUMP",                "https://mdr-284320-0.sslcast.mdr.de/mdr/284320/0/mp3/high/stream.mp3"});
        put("sunshine",    new String[]{"\u2600\uFE0F Sunshine Live",           "https://stream.sunshine-live.de/live/mp3-192/stream"});
        put("jamfm",       new String[]{"\uD83C\uDFB5 JAM FM",                 "https://stream.jam.fm/jamfm-live/mp3-192/mediaplayer"});
        put("hitrtl",      new String[]{"\uD83D\uDCFB HITRADIO RTL",            "https://web.radio.hitradio-rtl.de/hrrtl-sachsen/stream/mp3"});
        put("89rtl",       new String[]{"\uD83D\uDCFB 89.0 RTL",               "https://stream.89.0rtl.de/live/mp3-192/stream"});
        put("lausitz",     new String[]{"\uD83D\uDCFB Radio Lausitz",           "https://web.radio.radiolausitz.de/radiolausitz-live/stream/mp3"});
        put("radiopsr",    new String[]{"\uD83D\uDCFB Radio PSR",              "https://streams.radiopsr.de/psr-live/mp3-192/stream"});
        put("rsa",         new String[]{"\uD83D\uDCFB R.SA",                   "https://streams.rsa-sachsen.de/rsa-live/mp3-192/stream"});
        put("rtl",         new String[]{"\uD83D\uDCFB RTL Radio",              "https://stream.rtlradio.de/rtl-de/mp3-192/stream"});
        put("lofi",        new String[]{"\uD83C\uDFB5 Lofi Hip Hop",           "https://play.streamafrica.net/lofiradio"});
        put("chillhop",    new String[]{"\uD83C\uDFB6 Chillhop",               "http://streams.fluxfm.de/Chillhop/mp3-320/audio/"});
        put("rock",        new String[]{"\uD83E\uDD18 Rock Hits",               "https://streams.radiobob.de/bob-rockhits/mp3-192/mediaplayer"});
        put("schlager",    new String[]{"\uD83C\uDFB6 Schlager Radio",         "https://streams.radiobob.de/bob-schlager/mp3-192/mediaplayer"});
        put("80er",        new String[]{"\uD83D\uDD7A 80er Hits",              "https://streams.bigfm.de/bigfm-80er-128-mp3"});
        put("90er",        new String[]{"\uD83D\uDC83 90er Hits",              "https://streams.bigfm.de/bigfm-90er-128-mp3"});
        put("freshhappywave", new String[]{"\uD83C\uDF1F Fresh Happy Wave",   "https://laut.fm/freshhappywave"});
    }};

    public RadioHandler(BotContext ctx) {
        this.ctx = ctx;
    }

    /** Handles the /radio command: loads and plays the selected radio stream. */
    public void handleRadio(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildVoiceState voiceState = event.getMember().getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            event.reply(Lang.t(gid, "voice.required")).setEphemeral(true).queue();
            return;
        }

        var senderOpt = event.getOption("sender");
        if (senderOpt == null) {
            event.reply(Lang.t(gid, "radio.unknown")).setEphemeral(true).queue();
            return;
        }

        String key = senderOpt.getAsString().toLowerCase();
        String[] station = RADIO_STATIONS.get(key);
        if (station == null) {
            log.warn("[Radio] Unbekannter Sender: {}", key);
            event.reply(Lang.t(gid, "radio.unknown")).setEphemeral(true).queue();
            return;
        }

        event.deferReply().queue();
        AudioChannelUnion channel = voiceState.getChannel();
        Guild guild = event.getGuild();
        GuildMusicManager musicManager = ctx.getGuildMusic(guild);

        ctx.playerManager.loadItemOrdered(musicManager, station[1], new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                ctx.connectAndPlay(guild, channel, musicManager, track, true);
                event.getHook().sendMessageEmbeds(new EmbedBuilder()
                        .setDescription(Lang.t(gid, "radio.started", station[0]))
                        .setColor(0xEB459E).build()).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (!playlist.getTracks().isEmpty()) {
                    ctx.connectAndPlay(guild, channel, musicManager, playlist.getTracks().get(0), true);
                    event.getHook().sendMessageEmbeds(new EmbedBuilder()
                            .setDescription(Lang.t(gid, "radio.started", station[0]))
                            .setColor(0xEB459E).build()).queue();
                }
            }

            @Override
            public void noMatches() {
                log.warn("[Radio] Stream nicht erreichbar: {} ({})", station[0], station[1]);
                event.getHook().sendMessage(Lang.t(gid, "radio.unreachable")).queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                log.error("[Radio] Load failed fuer {}: {}", station[0], e.getMessage());
                event.getHook().sendMessage(Lang.t(gid, "error.generic", e.getMessage())).queue();
            }
        });
    }

    /**
     * Returns autocomplete choices for radio station names matching the user input.
     *
     * @return up to 25 matching station choices
     */
    public List<Command.Choice> autocomplete(String input) {
        List<Command.Choice> choices = new ArrayList<>();
        for (Map.Entry<String, String[]> entry : RADIO_STATIONS.entrySet()) {
            if (choices.size() >= 25) break;
            if (input.isBlank() || entry.getKey().contains(input) || entry.getValue()[0].toLowerCase().contains(input)) {
                choices.add(new Command.Choice(entry.getValue()[0], entry.getKey()));
            }
        }
        return choices;
    }
}
