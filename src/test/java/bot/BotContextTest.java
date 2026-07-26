package bot;

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class BotContextTest {

    private static DefaultAudioPlayerManager sharedManager;
    private BotContext ctx;

    @BeforeAll
    static void initManager() {
        sharedManager = new DefaultAudioPlayerManager();
    }

    @BeforeEach
    void setUp() {
        ctx = new BotContext();
    }

    @Test
    @DisplayName("formatTime formats 0ms correctly")
    void formatTimeZero() {
        assertEquals("0:00", ctx.formatTime(0));
    }

    @Test
    @DisplayName("formatTime formats 65000ms as 1:05")
    void formatTimeOneMinuteFiveSeconds() {
        assertEquals("1:05", ctx.formatTime(65000));
    }

    @Test
    @DisplayName("formatTime formats 3661000ms as 61:01")
    void formatTimeOneHourOneMinute() {
        assertEquals("61:01", ctx.formatTime(3661000));
    }

    @Test
    @DisplayName("formatTime formats 300000ms as 5:00")
    void formatTimeFiveMinutes() {
        assertEquals("5:00", ctx.formatTime(300000));
    }

    @Test
    @DisplayName("formatTime formats 1000ms as 0:01")
    void formatTimeOneSecond() {
        assertEquals("0:01", ctx.formatTime(1000));
    }

    @Test
    @DisplayName("formatTimeLong formats under an hour as minutes and seconds")
    void formatTimeLongUnderHour() {
        assertEquals("5m 0s", ctx.formatTimeLong(300000));
        assertEquals("1m 5s", ctx.formatTimeLong(65000));
    }

    @Test
    @DisplayName("formatTimeLong formats over an hour as hours and minutes")
    void formatTimeLongOverHour() {
        assertEquals("1h 1m", ctx.formatTimeLong(3661000));
        assertEquals("2h 30m", ctx.formatTimeLong(9000000));
    }

    @Test
    @DisplayName("formatTimeLong formats 0ms as 0m 0s")
    void formatTimeLongZero() {
        assertEquals("0m 0s", ctx.formatTimeLong(0));
    }

    @Test
    @DisplayName("extractVideoId extracts ID from YouTube v= URL")
    void extractVideoIdFromVEqualsUrl() {
        assertEquals("dQw4w9WgXcQ", ctx.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
    }

    @Test
    @DisplayName("extractVideoId extracts ID from youtu.be URL")
    void extractVideoIdFromShortUrl() {
        assertEquals("dQw4w9WgXcQ", ctx.extractVideoId("https://youtu.be/dQw4w9WgXcQ"));
    }

    @Test
    @DisplayName("extractVideoId handles URL with extra params")
    void extractVideoIdWithExtraParams() {
        assertEquals("dQw4w9WgXcQ", ctx.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PLrAXtmErZgOeiKm4sgNOknGvNjby9efdf"));
    }

    @Test
    @DisplayName("extractVideoId returns empty string for non-YouTube URL")
    void extractVideoIdReturnsEmptyForNonYoutube() {
        assertEquals("", ctx.extractVideoId("https://www.google.com"));
    }

    @Test
    @DisplayName("extractVideoId returns empty string for null")
    void extractVideoIdReturnsEmptyForNull() {
        assertEquals("", ctx.extractVideoId(null));
    }

    @Test
    @DisplayName("buildProgressBar returns correct format at start")
    void buildProgressBarAtStart() {
        String bar = ctx.buildProgressBar(0, 100);
        assertNotNull(bar);
        assertTrue(bar.length() > 0);
        assertTrue(bar.contains("\uD83D\uDD18"));
    }

    @Test
    @DisplayName("buildProgressBar returns correct format at middle")
    void buildProgressBarAtMiddle() {
        String bar = ctx.buildProgressBar(50, 100);
        assertNotNull(bar);
        assertTrue(bar.contains("\uD83D\uDD18"));
    }

    @Test
    @DisplayName("buildProgressBar returns correct format at end")
    void buildProgressBarAtEnd() {
        String bar = ctx.buildProgressBar(100, 100);
        assertNotNull(bar);
        assertTrue(bar.contains("\uD83D\uDD18"));
    }

    @Test
    @DisplayName("buildProgressBar handles zero duration")
    void buildProgressBarZeroDuration() {
        String bar = ctx.buildProgressBar(0, 0);
        assertNotNull(bar);
        assertTrue(bar.contains("\uD83D\uDD18"));
    }

    @Test
    @DisplayName("buildVolumeBar returns correct format")
    void buildVolumeBarReturnsCorrectFormat() {
        String bar = ctx.buildVolumeBar(50);
        assertNotNull(bar);
        assertTrue(bar.contains("\uD83D\uDD18"));
    }

    @Test
    @DisplayName("buildVolumeBar at zero volume")
    void buildVolumeBarAtZero() {
        String bar = ctx.buildVolumeBar(0);
        assertNotNull(bar);
        assertTrue(bar.startsWith("\uD83D\uDD18") || bar.charAt(0) == '\u25AC');
    }

    @Test
    @DisplayName("buildVolumeBar at max volume")
    void buildVolumeBarAtMax() {
        String bar = ctx.buildVolumeBar(100);
        assertNotNull(bar);
        assertTrue(bar.contains("\uD83D\uDD18"));
    }
}
