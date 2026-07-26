package bot;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.api.audio.AudioSendHandler;

import java.nio.ByteBuffer;

/** Bridges a Lavaplayer {@link AudioPlayer} to JDA's {@link AudioSendHandler} for streaming audio to Discord. */
public class AudioPlayerSendHandler implements AudioSendHandler {

    private final AudioPlayer player;
    private AudioFrame lastFrame;

    public AudioPlayerSendHandler(AudioPlayer player) {
        this.player = player;
    }

    /** Requests the next audio frame from the player and caches it for transmission. */
    @Override
    public boolean canProvide() {
        lastFrame = player.provide();
        return lastFrame != null;
    }

    /** Returns the cached Opus audio frame as a {@link ByteBuffer}. */
    @Override
    public ByteBuffer provide20MsAudio() {
        if (lastFrame == null) {
            return ByteBuffer.allocate(0);
        }
        return ByteBuffer.wrap(lastFrame.getData());
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}
