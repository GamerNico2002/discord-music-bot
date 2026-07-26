package bot;

import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LangTest {

    private static final long UNKNOWN_GUILD = 999999999L;

    @Test
    @DisplayName("getLang returns default 'de' for unknown guild")
    void getLangReturnsDefaultForUnknownGuild() {
        String lang = Lang.getLang(UNKNOWN_GUILD);
        assertEquals("de", lang);
    }

    @Test
    @DisplayName("setLang and getLang work correctly for supported language")
    void setAndGetLangWorkCorrectly() {
        long guildId = System.nanoTime();
        Lang.setLang(guildId, "en");
        assertEquals("en", Lang.getLang(guildId));
        Lang.setLang(guildId, "fr");
        assertEquals("fr", Lang.getLang(guildId));
    }

    @Test
    @DisplayName("setLang ignores unsupported language code")
    void setLangIgnoresUnsupportedCode() {
        long guildId = System.nanoTime();
        Lang.setLang(guildId, "xx");
        assertEquals("de", Lang.getLang(guildId));
    }

    @Test
    @DisplayName("t() returns translation for known key")
    void tReturnsTranslationForKnownKey() {
        String result = Lang.t("de", "voice.required");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertNotEquals("voice.required", result);
    }

    @Test
    @DisplayName("t() returns key itself for unknown key")
    void tReturnsKeyForUnknownKey() {
        String result = Lang.t("de", "totally.nonexistent.key.12345");
        assertEquals("totally.nonexistent.key.12345", result);
    }

    @Test
    @DisplayName("t() with args formats MessageFormat correctly")
    void tWithArgsFormatsCorrectly() {
        String result = Lang.t("de", "track.added", "My Song Title");
        assertNotNull(result);
        assertTrue(result.contains("My Song Title"));
    }

    @Test
    @DisplayName("t() with guild ID resolves language and translates")
    void tWithGuildIdResolvesLang() {
        long guildId = System.nanoTime();
        Lang.setLang(guildId, "en");
        String result = Lang.t(guildId, "queue.empty");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("SUPPORTED has all 5 languages")
    void supportedHasFiveLanguages() {
        Map<String, String> supported = Lang.SUPPORTED;
        assertEquals(5, supported.size());
        assertTrue(supported.containsKey("de"));
        assertTrue(supported.containsKey("en"));
        assertTrue(supported.containsKey("fr"));
        assertTrue(supported.containsKey("es"));
        assertTrue(supported.containsKey("it"));
    }

    @Test
    @DisplayName("DEFAULT constant is 'de'")
    void defaultIsDe() {
        assertEquals("de", Lang.DEFAULT);
    }
}
