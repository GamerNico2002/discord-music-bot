package bot;

import java.util.ArrayList;
import java.util.List;

/** Data model for a user's playlist, holding a name and a list of {@link Song} entries. */
public class Playlist {
    private String name;
    private List<Song> songs;

    public Playlist(String name) {
        this.name = name;
        this.songs = new ArrayList<>();
    }

    public String getName() { return name; }
    public List<Song> getSongs() { return songs; }

    /** A single song entry within a playlist, storing its display title and playback URL. */
    public static class Song {
        private String title;
        private String url;

        public Song(String title, String url) {
            this.title = title;
            this.url = url;
        }

        public String getTitle() { return title; }
        public String getUrl() { return url; }
    }
}
