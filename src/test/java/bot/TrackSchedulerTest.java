package bot;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TrackSchedulerTest {

    private AudioPlayer player;
    private TrackScheduler scheduler;

    private AudioTrack createMockTrack(String uri) {
        AudioTrack track = mock(AudioTrack.class);
        AudioTrackInfo info = new AudioTrackInfo("Title", "url", 300000, uri, false, uri);
        when(track.getInfo()).thenReturn(info);
        when(track.makeClone()).thenReturn(track);
        return track;
    }

    @BeforeEach
    void setUp() {
        player = mock(AudioPlayer.class);
        when(player.startTrack(any(AudioTrack.class), eq(true))).thenReturn(true);
        when(player.startTrack(any(AudioTrack.class), eq(false))).thenReturn(true);
        when(player.getPlayingTrack()).thenReturn(null);
        scheduler = new TrackScheduler(player);
    }

    @Test
    @DisplayName("queue adds track to queue when player starts it")
    void queueAddsTrackWhenStarted() {
        AudioTrack track = createMockTrack("uri1");
        scheduler.queue(track);
        verify(player).startTrack(track, true);
    }

    @Test
    @DisplayName("queue enqueues track when player can't start it")
    void queueEnqueuesWhenPlayerBusy() {
        when(player.startTrack(any(AudioTrack.class), eq(true))).thenReturn(false);

        AudioTrack track = createMockTrack("uri1");
        scheduler.queue(track);
        assertEquals(1, scheduler.getQueue().size());
    }

    @Test
    @DisplayName("skip removes first track and starts next")
    void skipRemovesFirstTrack() {
        when(player.startTrack(any(AudioTrack.class), eq(true))).thenReturn(false);

        AudioTrack first = createMockTrack("uri1");
        AudioTrack second = createMockTrack("uri2");
        scheduler.queue(first);
        scheduler.queue(second);
        assertEquals(2, scheduler.getQueue().size());

        scheduler.skip();
        assertEquals(1, scheduler.getQueue().size());
        verify(player).startTrack(first, false);
        assertEquals("uri2", scheduler.getQueue().get(0).getInfo().uri);
    }

    @Test
    @DisplayName("skip stops playback when queue is empty")
    void skipStopsWhenEmpty() {
        AtomicBoolean idleCalled = new AtomicBoolean(false);
        scheduler.setOnIdle(() -> idleCalled.set(true));

        scheduler.skip();
        verify(player).stopTrack();
        assertTrue(idleCalled.get());
    }

    @Test
    @DisplayName("clearQueue empties the queue")
    void clearQueueEmptiesQueue() {
        when(player.startTrack(any(AudioTrack.class), eq(true))).thenReturn(false);

        AudioTrack first = createMockTrack("uri1");
        AudioTrack second = createMockTrack("uri2");
        AudioTrack third = createMockTrack("uri3");
        scheduler.queue(first);
        scheduler.queue(second);
        scheduler.queue(third);
        assertEquals(3, scheduler.getQueue().size());

        scheduler.clearQueue();
        assertTrue(scheduler.getQueue().isEmpty());
    }

    @Test
    @DisplayName("getRepeatMode default is OFF")
    void getRepeatModeDefaultIsOff() {
        assertEquals(TrackScheduler.RepeatMode.OFF, scheduler.getRepeatMode());
    }

    @Test
    @DisplayName("setRepeatMode changes mode")
    void setRepeatModeChangesMode() {
        scheduler.setRepeatMode(TrackScheduler.RepeatMode.TRACK);
        assertEquals(TrackScheduler.RepeatMode.TRACK, scheduler.getRepeatMode());

        scheduler.setRepeatMode(TrackScheduler.RepeatMode.QUEUE);
        assertEquals(TrackScheduler.RepeatMode.QUEUE, scheduler.getRepeatMode());

        scheduler.setRepeatMode(TrackScheduler.RepeatMode.OFF);
        assertEquals(TrackScheduler.RepeatMode.OFF, scheduler.getRepeatMode());
    }

    @Test
    @DisplayName("shuffle changes queue order")
    void shuffleChangesOrder() {
        when(player.startTrack(any(AudioTrack.class), eq(true))).thenReturn(false);

        for (int i = 0; i < 20; i++) {
            scheduler.queue(createMockTrack("uri" + i));
        }
        assertEquals(20, scheduler.getQueue().size());

        boolean orderChanged = false;
        for (int attempt = 0; attempt < 10; attempt++) {
            List<AudioTrack> before = new ArrayList<>(scheduler.getQueue());
            scheduler.shuffle();
            List<AudioTrack> after = scheduler.getQueue();
            if (!before.equals(after)) {
                orderChanged = true;
                break;
            }
        }
        assertTrue(orderChanged, "Shuffle should change order at least once in 10 attempts");
    }

    @Test
    @DisplayName("isDuplicate detects duplicates in queue")
    void isDuplicateDetectsDuplicates() {
        when(player.startTrack(any(AudioTrack.class), eq(true))).thenReturn(false);

        AudioTrack track = createMockTrack("duplicate_uri");
        scheduler.queue(track);

        assertTrue(scheduler.isDuplicate("duplicate_uri"));
        assertFalse(scheduler.isDuplicate("other_uri"));
    }

    @Test
    @DisplayName("isDuplicate returns false for empty queue")
    void isDuplicateReturnsFalseForEmptyQueue() {
        assertFalse(scheduler.isDuplicate("any_uri"));
    }

    @Test
    @DisplayName("isDuplicate detects duplicate on currently playing track")
    void isDuplicateDetectsPlayingTrack() {
        AudioTrack track = createMockTrack("playing_uri");
        when(player.getPlayingTrack()).thenReturn(track);
        when(player.startTrack(any(AudioTrack.class), eq(true))).thenReturn(true);
        scheduler.queue(track);

        assertTrue(scheduler.isDuplicate("playing_uri"));
    }

    @Test
    @DisplayName("peek returns first track without removing")
    void peekReturnsFirstTrack() {
        when(player.startTrack(any(AudioTrack.class), eq(true))).thenReturn(false);

        AudioTrack track = createMockTrack("uri1");
        scheduler.queue(track);

        AudioTrack peeked = scheduler.peek();
        assertNotNull(peeked);
        assertEquals("uri1", peeked.getInfo().uri);
        assertEquals(1, scheduler.getQueue().size());
    }

    @Test
    @DisplayName("peek returns null for empty queue")
    void peekReturnsNullForEmptyQueue() {
        assertNull(scheduler.peek());
    }

    @Test
    @DisplayName("removeFromQueue removes track at index")
    void removeFromQueueRemovesCorrectTrack() {
        when(player.startTrack(any(AudioTrack.class), eq(true))).thenReturn(false);

        AudioTrack first = createMockTrack("uri0");
        AudioTrack second = createMockTrack("uri1");
        AudioTrack third = createMockTrack("uri2");
        scheduler.queue(first);
        scheduler.queue(second);
        scheduler.queue(third);

        AudioTrack removed = scheduler.removeFromQueue(1);
        assertNotNull(removed);
        assertEquals("uri1", removed.getInfo().uri);
        assertEquals(2, scheduler.getQueue().size());
    }

    @Test
    @DisplayName("removeFromQueue returns null for invalid index")
    void removeFromQueueReturnsNullForInvalid() {
        assertNull(scheduler.removeFromQueue(-1));
        assertNull(scheduler.removeFromQueue(999));
    }

    @Test
    @DisplayName("moveInQueue reorders tracks")
    void moveInQueueReorders() {
        when(player.startTrack(any(AudioTrack.class), eq(true))).thenReturn(false);

        AudioTrack first = createMockTrack("uri0");
        AudioTrack second = createMockTrack("uri1");
        AudioTrack third = createMockTrack("uri2");
        scheduler.queue(first);
        scheduler.queue(second);
        scheduler.queue(third);

        assertTrue(scheduler.moveInQueue(0, 2));
        assertEquals("uri1", scheduler.getQueue().get(0).getInfo().uri);
        assertEquals("uri2", scheduler.getQueue().get(1).getInfo().uri);
        assertEquals("uri0", scheduler.getQueue().get(2).getInfo().uri);
    }

    @Test
    @DisplayName("moveInQueue returns false for invalid indices")
    void moveInQueueReturnsFalseForInvalid() {
        assertFalse(scheduler.moveInQueue(-1, 0));
        assertFalse(scheduler.moveInQueue(0, 999));
    }

    @Test
    @DisplayName("stop clears queue and resets repeat mode")
    void stopClearsAndResets() {
        when(player.startTrack(any(AudioTrack.class), eq(true))).thenReturn(false);

        AudioTrack track = createMockTrack("uri1");
        scheduler.queue(track);
        scheduler.setRepeatMode(TrackScheduler.RepeatMode.QUEUE);

        scheduler.stop();
        assertTrue(scheduler.getQueue().isEmpty());
        assertEquals(TrackScheduler.RepeatMode.OFF, scheduler.getRepeatMode());
        verify(player).stopTrack();
    }
}
