package bot;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrackScheduler extends AudioEventAdapter {

    public enum RepeatMode { OFF, TRACK, QUEUE }

    private final AudioPlayer player;
    private final List<AudioTrack> queue = Collections.synchronizedList(new ArrayList<>());
    private RepeatMode repeatMode = RepeatMode.OFF;
    private JDA jda;
    private long guildId;
    private Runnable onIdle;

    public void setOnIdle(Runnable onIdle) {
        this.onIdle = onIdle;
    }

    public TrackScheduler(AudioPlayer player) {
        this.player = player;
        player.addListener(this);
    }

    public void setJda(JDA jda) {
        this.jda = jda;
    }

    public void setGuildId(long guildId) {
        this.guildId = guildId;
    }

    public void queue(AudioTrack track) {
        if (!player.startTrack(track, true)) {
            queue.add(track);
        }
    }

    public void playNow(AudioTrack track) {
        player.startTrack(track, false);
    }

    public AudioTrack peek() {
        synchronized (queue) {
            return queue.isEmpty() ? null : queue.get(0);
        }
    }

    public void skip() {
        AudioTrack next;
        synchronized (queue) {
            next = queue.isEmpty() ? null : queue.remove(0);
        }
        if (next != null) {
            player.startTrack(next, false);
        } else {
            player.stopTrack();
            if (onIdle != null) {
                onIdle.run();
            }
        }
    }

    public void stop() {
        queue.clear();
        repeatMode = RepeatMode.OFF;
        player.stopTrack();
    }

    public List<AudioTrack> getQueue() {
        return queue;
    }

    public RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public void setRepeatMode(RepeatMode mode) {
        this.repeatMode = mode;
    }

    public AudioTrack removeFromQueue(int index) {
        synchronized (queue) {
            return index >= 0 && index < queue.size() ? queue.remove(index) : null;
        }
    }

    public void clearQueue() {
        queue.clear();
    }

    public boolean moveInQueue(int from, int to) {
        synchronized (queue) {
            if (from < 0 || from >= queue.size() || to < 0 || to >= queue.size()) return false;
            AudioTrack track = queue.remove(from);
            queue.add(to, track);
            return true;
        }
    }

    public AudioTrack skipTo(int index) {
        synchronized (queue) {
            if (index < 0 || index >= queue.size()) return null;
            AudioTrack target = queue.get(index);
            List<AudioTrack> remaining = new ArrayList<>(queue.subList(index + 1, queue.size()));
            queue.clear();
            queue.addAll(remaining);
            player.startTrack(target, false);
            return target;
        }
    }

    public boolean isDuplicate(String uri) {
        AudioTrack current = player.getPlayingTrack();
        if (current != null && current.getInfo().uri.equals(uri)) return true;
        synchronized (queue) {
            for (AudioTrack track : queue) {
                if (track.getInfo().uri.equals(uri)) return true;
            }
        }
        return false;
    }

    public void shuffle() {
        synchronized (queue) {
            Collections.shuffle(queue);
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        System.out.println("[Track] Ended: " + track.getInfo().title + " reason=" + endReason);
        if (endReason.mayStartNext) {
            if (repeatMode == RepeatMode.TRACK) {
                player.startTrack(track.makeClone(), false);
            } else if (repeatMode == RepeatMode.QUEUE) {
                queue.add(track.makeClone());
                skip();
            } else {
                AudioTrack next;
                synchronized (queue) {
                    next = queue.isEmpty() ? null : queue.remove(0);
                }
                if (next != null) {
                    player.startTrack(next, false);
                } else if (onIdle != null) {
                    onIdle.run();
                }
            }
        }
        if (player.getPlayingTrack() == null && jda != null) {
            updateVoiceStatus("");
        }
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        System.out.println("[Track] Playing: " + track.getInfo().title + " (" + track.getInfo().uri + ")");
        String title = track.getInfo().title;
        if (title != null && !title.isBlank() && !title.equalsIgnoreCase("Unknown title")) {
            updateVoiceStatus("\uD83C\uDFB5 " + title);
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        System.err.println("[Track] ERROR playing " + track.getInfo().title + ": " + exception.getMessage());
        exception.printStackTrace();
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        System.err.println("[Track] STUCK: " + track.getInfo().title + " (threshold=" + thresholdMs + "ms)");
        skip();
    }

    private void updateVoiceStatus(String status) {
        if (jda == null || guildId == 0) return;
        var guild = jda.getGuildById(guildId);
        if (guild == null) return;
        AudioChannel channel = guild.getAudioManager().getConnectedChannel();
        if (channel instanceof VoiceChannel vc) {
            String truncated = status.length() > 500 ? status.substring(0, 500) : status;
            System.out.println("[Voice] Status Update -> Guild: " + guild.getName() + " | Channel: " + vc.getName() + " | Status: " + truncated);
            vc.modifyStatus(truncated).queue(null, err ->
                    System.err.println("[Voice] Status update fehlgeschlagen: " + err.getMessage()));
        }
    }
}
