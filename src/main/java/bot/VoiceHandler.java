package bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoiceHandler {

    private static final Logger log = LoggerFactory.getLogger(VoiceHandler.class);

    private final BotContext ctx;

    public VoiceHandler(BotContext ctx) {
        this.ctx = ctx;
    }

    public void handleJoin(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildVoiceState voiceState = event.getMember().getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            event.reply(Lang.t(gid, "voice.required")).setEphemeral(true).queue();
            return;
        }

        AudioChannelUnion channel = voiceState.getChannel();
        Guild guild = event.getGuild();
        long guildId = guild.getIdLong();
        GuildMusicManager musicManager = ctx.getGuildMusic(guild);

        ctx.lastChannelIds.put(guildId, channel.getIdLong());
        AudioManager audioManager = guild.getAudioManager();
        audioManager.setSendingHandler(musicManager.sendHandler);
        audioManager.setSelfDeafened(true);
        audioManager.openAudioConnection(channel);

        NonstopHandler.installIdleHandler(ctx, guildId, musicManager);
        NonstopHandler.scheduleAutoNonstop(ctx, guildId, musicManager);

        log.info("[Voice] Bot joined {} in guild {}", channel.getName(), gid);
        event.replyEmbeds(new EmbedBuilder()
                .setDescription(Lang.t(gid, "joined", channel.getName()))
                .setColor(0x57F287).build()).queue();
    }

    public void handleLeave(SlashCommandInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();
        GuildMusicManager manager = ctx.getGuildMusic(event.getGuild());

        ctx.nonstopGuilds.remove(guildId);
        NonstopHandler.cancelAutoNonstop(ctx, guildId);
        manager.scheduler.stop();
        ctx.lastChannelIds.remove(guildId);
        ctx.musicManagers.remove(guildId);
        event.getGuild().getAudioManager().closeAudioConnection();

        log.info("[Voice] Bot left guild {}", guildId);
        event.replyEmbeds(new EmbedBuilder()
                .setDescription(Lang.t(guildId, "bye"))
                .setColor(0xED4245).build()).queue();
    }

    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        Guild guild = event.getGuild();
        long guildId = guild.getIdLong();

        if (!event.getMember().getUser().equals(event.getJDA().getSelfUser())) {
            return;
        }

        if (event.getChannelJoined() != null) {
            ctx.lastChannelIds.put(guildId, event.getChannelJoined().getIdLong());
            log.info("[Voice] Bot joined: {}", event.getChannelJoined().getName());
        }

        if (event.getChannelLeft() != null && event.getChannelJoined() == null) {
            log.info("[Voice] Bot disconnected von: {}", event.getChannelLeft().getName());
            Long channelId = ctx.lastChannelIds.get(guildId);
            if (channelId != null && ctx.musicManagers.containsKey(guildId)) {
                AudioChannel channel = guild.getVoiceChannelById(channelId);
                if (channel == null) channel = guild.getStageChannelById(channelId);
                if (channel != null) {
                    log.info("[Voice] Auto-Reconnect zu: {}", channel.getName());
                    GuildMusicManager manager = ctx.musicManagers.get(guildId);
                    AudioManager audioManager = guild.getAudioManager();
                    audioManager.setSendingHandler(manager.sendHandler);
                    audioManager.setSelfDeafened(true);
                    audioManager.openAudioConnection(channel);
                    NonstopHandler.installIdleHandler(ctx, guildId, manager);
                    NonstopHandler.scheduleAutoNonstop(ctx, guildId, manager);
                } else {
                    log.warn("[Voice] Channel {} nicht mehr vorhanden, Cleanup fuer guild {}", channelId, guildId);
                    ctx.cleanupGuild(guildId);
                }
            } else {
                ctx.cleanupGuild(guildId);
            }
        }
    }
}
