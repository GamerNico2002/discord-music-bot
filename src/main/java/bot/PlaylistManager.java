package bot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** JSON-based persistence layer for per-user, per-guild playlists stored under the {@code playlists/} directory. */
public class PlaylistManager {

    private static final Logger log = LoggerFactory.getLogger(PlaylistManager.class);

    private static final Path BASE_DIR = Paths.get("playlists");
    public static final int MAX_PLAYLISTS = 10;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type PLAYLIST_LIST_TYPE = new TypeToken<List<Playlist>>(){}.getType();

    /**
     * Loads all playlists for the given guild and user, migrating from the legacy {@code .dat} format if necessary.
     */
    public static List<Playlist> load(long guildId, long userId) {
        Path file = getFile(guildId, userId, ".json");
        if (!Files.exists(file)) {
            Path oldFile = getFile(guildId, userId, ".dat");
            if (Files.exists(oldFile)) {
                return migrate(guildId, userId, oldFile);
            }
            return new ArrayList<>();
        }
        try (var reader = Files.newBufferedReader(file)) {
            List<Playlist> playlists = GSON.fromJson(reader, PLAYLIST_LIST_TYPE);
            return playlists != null ? playlists : new ArrayList<>();
        } catch (IOException e) {
            log.error("[Playlist] Load error: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /** Saves the given list of playlists to disk for the specified guild and user. */
    public static void save(long guildId, long userId, List<Playlist> playlists) {
        try {
            Files.createDirectories(getDir(guildId, userId));
            Path file = getFile(guildId, userId, ".json");
            try (var writer = Files.newBufferedWriter(file)) {
                GSON.toJson(playlists, writer);
            }
        } catch (IOException e) {
            log.error("[Playlist] Save error: {}", e.getMessage());
        }
    }

    /** Deletes both the JSON and legacy {@code .dat} playlist files for the given guild and user. */
    public static void deleteAll(long guildId, long userId) {
        try {
            Path jsonFile = getFile(guildId, userId, ".json");
            Files.deleteIfExists(jsonFile);
            Path datFile = getFile(guildId, userId, ".dat");
            Files.deleteIfExists(datFile);
            Path dir = getDir(guildId, userId);
            if (Files.exists(dir)) {
                try (var list = Files.list(dir)) {
                    if (list.findAny().isEmpty()) {
                        Files.deleteIfExists(dir);
                    }
                }
            }
        } catch (IOException e) {
            log.error("[Playlist] Delete error: {}", e.getMessage());
        }
    }

    /** Recursively deletes all playlist data for an entire guild. */
    public static void deleteGuild(long guildId) {
        Path dir = getGuildDir(guildId);
        try {
            if (Files.exists(dir)) {
                try (var files = Files.walk(dir)) {
                    files.sorted(Comparator.reverseOrder())
                         .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
                }
            }
        } catch (IOException e) {
            log.error("[Playlist] Delete guild error: {}", e.getMessage());
        }
    }

    /**
     * Returns whether the user can create another playlist (has not reached {@value #MAX_PLAYLISTS}).
     */
    public static boolean canCreate(long guildId, long userId) {
        return load(guildId, userId).size() < MAX_PLAYLISTS;
    }

    /**
     * Finds a playlist by name (case-insensitive) within the given list.
     *
     * @return the matching playlist, or {@code null} if not found
     */
    public static Playlist find(List<Playlist> playlists, String name) {
        for (Playlist pl : playlists) {
            if (pl.getName().equalsIgnoreCase(name)) return pl;
        }
        return null;
    }

    private static List<Playlist> migrate(long guildId, long userId, Path oldFile) {
        log.info("[Playlist] Migrating old .dat format to .json for guild={} user={}", guildId, userId);
        try (DataInputStream in = new DataInputStream(Files.newInputStream(oldFile))) {
            int count = in.readInt();
            List<Playlist> playlists = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String name = in.readUTF();
                Playlist pl = new Playlist(name);
                int songCount = in.readInt();
                for (int j = 0; j < songCount; j++) {
                    String title = in.readUTF();
                    String url = in.readUTF();
                    pl.getSongs().add(new Playlist.Song(title, url));
                }
                playlists.add(pl);
            }
            save(guildId, userId, playlists);
            Files.deleteIfExists(oldFile);
            log.info("[Playlist] Migration complete for guild={} user={}", guildId, userId);
            return playlists;
        } catch (IOException e) {
            log.error("[Playlist] Migration error: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private static Path getDir(long guildId, long userId) {
        return BASE_DIR.resolve(String.valueOf(guildId)).resolve(String.valueOf(userId));
    }

    private static Path getGuildDir(long guildId) {
        return BASE_DIR.resolve(String.valueOf(guildId));
    }

    private static Path getFile(long guildId, long userId, String extension) {
        return getDir(guildId, userId).resolve("playlists" + extension);
    }
}
