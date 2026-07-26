package bot;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.junit.jupiter.api.*;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NonstopHandlerTest {

    @Test
    @DisplayName("DEFAULT_GENRES is not empty and has at least 20 entries")
    void defaultGenresNotEmpty() {
        assertNotNull(NonstopHandler.DEFAULT_GENRES);
        assertTrue(NonstopHandler.DEFAULT_GENRES.length >= 20,
                "Expected at least 20 genres but got " + NonstopHandler.DEFAULT_GENRES.length);
    }

    @Test
    @DisplayName("DEFAULT_MODIFIERS is not empty and has at least 10 entries")
    void defaultModifiersNotEmpty() {
        assertNotNull(NonstopHandler.DEFAULT_MODIFIERS);
        assertTrue(NonstopHandler.DEFAULT_MODIFIERS.length >= 10,
                "Expected at least 10 modifiers but got " + NonstopHandler.DEFAULT_MODIFIERS.length);
    }

    @Test
    @DisplayName("DEFAULT_GENRES contains no empty strings")
    void defaultGenresNoEmptyStrings() {
        for (String genre : NonstopHandler.DEFAULT_GENRES) {
            assertNotNull(genre);
            assertFalse(genre.isBlank(), "Genre must not be blank");
        }
    }

    @Test
    @DisplayName("DEFAULT_MODIFIERS contains no empty strings")
    void defaultModifiersNoEmptyStrings() {
        for (String mod : NonstopHandler.DEFAULT_MODIFIERS) {
            assertNotNull(mod);
            assertFalse(mod.isBlank(), "Modifier must not be blank");
        }
    }

    @Test
    @DisplayName("isLiveOrRecording returns true for streams")
    void isLiveOrRecordingReturnsTrueForStreams() {
        AudioTrackInfo info = new AudioTrackInfo("Normal Title", "url", 0, "id", false, "uri");
        AudioTrack track = mock(AudioTrack.class);
        when(track.getInfo()).thenReturn(info);
        // Override isStream by creating info with isStream=true
        AudioTrackInfo streamInfo = new AudioTrackInfo("Normal Title", "url", 0, "id", true, "uri");
        when(track.getInfo()).thenReturn(streamInfo);

        assertTrue(NonstopHandler.isLiveOrRecording(track));
    }

    @Test
    @DisplayName("isLiveOrRecording returns true for track with 'live' in title")
    void isLiveOrRecordingReturnsTrueForLiveTitle() {
        AudioTrackInfo info = new AudioTrackInfo("Live at Wacken 2024", "url", 0, "id", false, "uri");
        AudioTrack track = mock(AudioTrack.class);
        when(track.getInfo()).thenReturn(info);

        assertTrue(NonstopHandler.isLiveOrRecording(track));
    }

    @Test
    @DisplayName("isLiveOrRecording returns true for track with 'aufnahme' in title")
    void isLiveOrRecordingReturnsTrueForAufnahme() {
        AudioTrackInfo info = new AudioTrackInfo("Konzert Aufnahme", "url", 0, "id", false, "uri");
        AudioTrack track = mock(AudioTrack.class);
        when(track.getInfo()).thenReturn(info);

        assertTrue(NonstopHandler.isLiveOrRecording(track));
    }

    @Test
    @DisplayName("isLiveOrRecording returns true for track with 'concert' in title")
    void isLiveOrRecordingReturnsTrueForConcert() {
        AudioTrackInfo info = new AudioTrackInfo("Live Concert Recording", "url", 0, "id", false, "uri");
        AudioTrack track = mock(AudioTrack.class);
        when(track.getInfo()).thenReturn(info);

        assertTrue(NonstopHandler.isLiveOrRecording(track));
    }

    @Test
    @DisplayName("isLiveOrRecording returns true for track with 'bootleg' in title")
    void isLiveOrRecordingReturnsTrueForBootleg() {
        AudioTrackInfo info = new AudioTrackInfo("Rare Bootleg Edition", "url", 0, "id", false, "uri");
        AudioTrack track = mock(AudioTrack.class);
        when(track.getInfo()).thenReturn(info);

        assertTrue(NonstopHandler.isLiveOrRecording(track));
    }

    @Test
    @DisplayName("isLiveOrRecording returns false for normal track")
    void isLiveOrRecordingReturnsFalseForNormalTrack() {
        AudioTrackInfo info = new AudioTrackInfo("Studio Album Version", "url", 0, "id", false, "uri");
        AudioTrack track = mock(AudioTrack.class);
        when(track.getInfo()).thenReturn(info);

        assertFalse(NonstopHandler.isLiveOrRecording(track));
    }

    @Test
    @DisplayName("isLiveOrRecording returns false for null title")
    void isLiveOrRecordingReturnsFalseForNullTitle() {
        AudioTrackInfo info = new AudioTrackInfo(null, "url", 0, "id", false, "uri");
        AudioTrack track = mock(AudioTrack.class);
        when(track.getInfo()).thenReturn(info);

        assertFalse(NonstopHandler.isLiveOrRecording(track));
    }

    @Test
    @DisplayName("isLiveOrRecording is case insensitive")
    void isLiveOrRecordingIsCaseInsensitive() {
        AudioTrackInfo info = new AudioTrackInfo("LIVE PERFORMANCE", "url", 0, "id", false, "uri");
        AudioTrack track = mock(AudioTrack.class);
        when(track.getInfo()).thenReturn(info);

        assertTrue(NonstopHandler.isLiveOrRecording(track));
    }
}
