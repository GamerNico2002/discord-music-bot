package bot;

import com.sedmelluq.discord.lavaplayer.filter.equalizer.EqualizerFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

/** Per-guild music manager that bundles the {@link AudioPlayer}, {@link TrackScheduler}, send handler, and equalizer. */
public class GuildMusicManager {

    public final AudioPlayer player;
    public final TrackScheduler scheduler;
    public final AudioPlayerSendHandler sendHandler;
    public final EqualizerFactory equalizer;

    private String activeFilter = "off";

    /**
     * Creates a new music manager for a guild, initializing the player, scheduler, send handler, and equalizer.
     */
    public GuildMusicManager(AudioPlayerManager manager) {
        this.player = manager.createPlayer();
        this.equalizer = new EqualizerFactory();
        this.scheduler = new TrackScheduler(player);
        this.sendHandler = new AudioPlayerSendHandler(player);
    }

    /** Returns the name of the currently active audio filter preset. */
    public String getActiveFilter() {
        return activeFilter;
    }

    /**
     * Applies an audio equalizer preset to the guild's player.
     *
     * @param filter one of {@code "bassboost"}, {@code "treble"}, {@code "pop"}, {@code "rock"}, or {@code "off"}
     */
    public void applyFilter(String filter) {
        this.activeFilter = filter;
        switch (filter) {
            case "bassboost" -> {
                player.setFilterFactory(equalizer);
                float[] gains = {0.25f, 0.20f, 0.15f, 0.10f, 0.05f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f};
                for (int i = 0; i < gains.length; i++) equalizer.setGain(i, gains[i]);
            }
            case "treble" -> {
                player.setFilterFactory(equalizer);
                float[] gains = {0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.10f, 0.15f, 0.20f, 0.20f, 0.25f};
                for (int i = 0; i < gains.length; i++) equalizer.setGain(i, gains[i]);
            }
            case "pop" -> {
                player.setFilterFactory(equalizer);
                float[] gains = {-0.02f, -0.01f, 0f, 0.02f, 0.05f, 0.07f, 0.05f, 0.02f, 0f, -0.01f, -0.02f, 0f, 0.02f, 0.03f, 0.05f};
                for (int i = 0; i < gains.length; i++) equalizer.setGain(i, gains[i]);
            }
            case "rock" -> {
                player.setFilterFactory(equalizer);
                float[] gains = {0.15f, 0.10f, 0.05f, 0f, -0.05f, -0.05f, 0f, 0.05f, 0.10f, 0.12f, 0.15f, 0.15f, 0.12f, 0.10f, 0.10f};
                for (int i = 0; i < gains.length; i++) equalizer.setGain(i, gains[i]);
            }
            default -> {
                this.activeFilter = "off";
                player.setFilterFactory(null);
                for (int i = 0; i < 15; i++) equalizer.setGain(i, 0f);
            }
        }
    }
}
