package bot;

import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlaylistManagerTest {

    @Test
    @DisplayName("canCreate returns true when under limit")
    void canCreateReturnsTrueWhenUnderLimit() {
        long guildId = 1001L;
        long userId = 2001L;
        PlaylistManager.deleteAll(guildId, userId);

        assertTrue(PlaylistManager.canCreate(guildId, userId));
    }

    @Test
    @DisplayName("canCreate returns false when at limit")
    void canCreateReturnsFalseWhenAtLimit() {
        long guildId = 1002L;
        long userId = 2002L;
        PlaylistManager.deleteAll(guildId, userId);

        List<Playlist> playlists = new ArrayList<>();
        for (int i = 0; i < PlaylistManager.MAX_PLAYLISTS; i++) {
            playlists.add(new Playlist("Playlist " + i));
        }
        PlaylistManager.save(guildId, userId, playlists);

        assertFalse(PlaylistManager.canCreate(guildId, userId));

        PlaylistManager.deleteAll(guildId, userId);
    }

    @Test
    @DisplayName("MAX_PLAYLISTS is 10")
    void maxPlaylistsIsTen() {
        assertEquals(10, PlaylistManager.MAX_PLAYLISTS);
    }

    @Test
    @DisplayName("find returns null for non-existent playlist")
    void findReturnsNullForNonExistent() {
        List<Playlist> playlists = new ArrayList<>();
        playlists.add(new Playlist("MyPlaylist"));
        playlists.add(new Playlist("AnotherPlaylist"));

        assertNull(PlaylistManager.find(playlists, "NoSuchPlaylist"));
    }

    @Test
    @DisplayName("find returns correct playlist")
    void findReturnsCorrectPlaylist() {
        List<Playlist> playlists = new ArrayList<>();
        Playlist target = new Playlist("TargetPlaylist");
        playlists.add(new Playlist("First"));
        playlists.add(target);
        playlists.add(new Playlist("Third"));

        Playlist found = PlaylistManager.find(playlists, "TargetPlaylist");
        assertNotNull(found);
        assertEquals("TargetPlaylist", found.getName());
    }

    @Test
    @DisplayName("find is case insensitive")
    void findIsCaseInsensitive() {
        List<Playlist> playlists = new ArrayList<>();
        playlists.add(new Playlist("MyFavorites"));

        Playlist found = PlaylistManager.find(playlists, "myfavorites");
        assertNotNull(found);
        assertEquals("MyFavorites", found.getName());
    }

    @Test
    @DisplayName("save and load roundtrip works")
    void saveAndLoadRoundtrip() {
        long guildId = 1003L;
        long userId = 2003L;
        PlaylistManager.deleteAll(guildId, userId);

        List<Playlist> playlists = new ArrayList<>();
        Playlist pl = new Playlist("TestRoundtrip");
        pl.getSongs().add(new Playlist.Song("Song1", "https://example.com/1"));
        pl.getSongs().add(new Playlist.Song("Song2", "https://example.com/2"));
        playlists.add(pl);
        PlaylistManager.save(guildId, userId, playlists);

        List<Playlist> loaded = PlaylistManager.load(guildId, userId);
        assertEquals(1, loaded.size());
        assertEquals("TestRoundtrip", loaded.get(0).getName());
        assertEquals(2, loaded.get(0).getSongs().size());
        assertEquals("Song1", loaded.get(0).getSongs().get(0).getTitle());

        PlaylistManager.deleteAll(guildId, userId);
    }

    @Test
    @DisplayName("load returns empty list for non-existent user")
    void loadReturnsEmptyForNonExistent() {
        List<Playlist> loaded = PlaylistManager.load(999999L, 888888L);
        assertNotNull(loaded);
        assertTrue(loaded.isEmpty());
    }

    @Test
    @DisplayName("deleteAll removes saved playlists")
    void deleteAllRemovesPlaylists() {
        long guildId = 1004L;
        long userId = 2004L;

        List<Playlist> playlists = new ArrayList<>();
        playlists.add(new Playlist("ToDelete"));
        PlaylistManager.save(guildId, userId, playlists);

        PlaylistManager.deleteAll(guildId, userId);

        List<Playlist> loaded = PlaylistManager.load(guildId, userId);
        assertTrue(loaded.isEmpty());
    }
}
