package bot;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PlaylistManager {

    private static final Path BASE_DIR = Paths.get("playlists");
    public static final int MAX_PLAYLISTS = 10;

    public static List<Playlist> load(long guildId, long userId) {
        Path file = getFile(guildId, userId);
        if (!Files.exists(file)) return new ArrayList<>();
        try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
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
            return playlists;
        } catch (IOException e) {
            System.err.println("[Playlist] Load error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static void save(long guildId, long userId, List<Playlist> playlists) {
        try {
            Files.createDirectories(getDir(guildId, userId));
            try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(getFile(guildId, userId)))) {
                out.writeInt(playlists.size());
                for (Playlist pl : playlists) {
                    out.writeUTF(pl.getName());
                    out.writeInt(pl.getSongs().size());
                    for (Playlist.Song song : pl.getSongs()) {
                        out.writeUTF(song.getTitle() != null ? song.getTitle() : "");
                        out.writeUTF(song.getUrl() != null ? song.getUrl() : "");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[Playlist] Save error: " + e.getMessage());
        }
    }

    public static void deleteAll(long guildId, long userId) {
        try {
            Path file = getFile(guildId, userId);
            Files.deleteIfExists(file);
            Path dir = getDir(guildId, userId);
            if (Files.exists(dir)) {
                try (var list = Files.list(dir)) {
                    if (list.findAny().isEmpty()) {
                        Files.deleteIfExists(dir);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[Playlist] Delete error: " + e.getMessage());
        }
    }

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
            System.err.println("[Playlist] Delete guild error: " + e.getMessage());
        }
    }

    public static boolean canCreate(long guildId, long userId) {
        return load(guildId, userId).size() < MAX_PLAYLISTS;
    }

    public static Playlist find(List<Playlist> playlists, String name) {
        for (Playlist pl : playlists) {
            if (pl.getName().equalsIgnoreCase(name)) return pl;
        }
        return null;
    }

    private static Path getDir(long guildId, long userId) {
        return BASE_DIR.resolve(String.valueOf(guildId)).resolve(String.valueOf(userId));
    }

    private static Path getGuildDir(long guildId) {
        return BASE_DIR.resolve(String.valueOf(guildId));
    }

    private static Path getFile(long guildId, long userId) {
        return getDir(guildId, userId).resolve("playlists.dat");
    }
}
