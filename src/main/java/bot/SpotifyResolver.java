package bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpotifyResolver {

    private static final Pattern SPOTIFY_TRACK = Pattern.compile("open\\.spotify\\.com/(?:intl-[a-z]{2}/)?track/([a-zA-Z0-9]+)");
    private static final Pattern SPOTIFY_PLAYLIST = Pattern.compile("open\\.spotify\\.com/(?:intl-[a-z]{2}/)?playlist/([a-zA-Z0-9]+)");
    private static final Pattern SPOTIFY_ALBUM = Pattern.compile("open\\.spotify\\.com/(?:intl-[a-z]{2}/)?album/([a-zA-Z0-9]+)");

    private final ExecutorService httpExecutor = Executors.newFixedThreadPool(8);
    private final HttpClient http = HttpClient.newBuilder().executor(httpExecutor).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String clientId;
    private final String clientSecret;
    private String accessToken;
    private long tokenExpiry;

    public SpotifyResolver(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public boolean isSpotifyUrl(String url) {
        return url.contains("open.spotify.com/") || url.contains("spotify.link/");
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }

    public CompletableFuture<List<String>> resolveAsync(String inputUrl) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> queries = new ArrayList<>();
            String url = inputUrl;
            try {
                if (url.contains("spotify.link/")) {
                    url = resolveRedirect(url);
                    System.out.println("[Spotify] Redirect aufgeloest: " + url);
                }

                Matcher trackMatcher = SPOTIFY_TRACK.matcher(url);
                Matcher playlistMatcher = SPOTIFY_PLAYLIST.matcher(url);
                Matcher albumMatcher = SPOTIFY_ALBUM.matcher(url);

                if (trackMatcher.find()) {
                    System.out.println("[Spotify] Track ID: " + trackMatcher.group(1));
                    String search = resolveTrack(trackMatcher.group(1));
                    if (search != null) queries.add(search);
                } else if (playlistMatcher.find()) {
                    System.out.println("[Spotify] Playlist ID: " + playlistMatcher.group(1));
                    queries.addAll(resolvePlaylist(playlistMatcher.group(1)));
                } else if (albumMatcher.find()) {
                    System.out.println("[Spotify] Album ID: " + albumMatcher.group(1));
                    queries.addAll(resolveAlbum(albumMatcher.group(1)));
                } else {
                    System.err.println("[Spotify] URL nicht erkannt: " + url);
                }
            } catch (Exception e) {
                System.err.println("[Spotify] Fehler: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("[Spotify] " + queries.size() + " Songs aufgeloest");
            return queries;
        }, httpExecutor);
    }

    private String resolveRedirect(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<Void> resp = http.send(req, HttpResponse.BodyHandlers.discarding());
        return resp.uri().toString();
    }

    private String resolveTrack(String trackId) throws Exception {
        ensureToken();
        JsonNode track = apiGet("https://api.spotify.com/v1/tracks/" + trackId);
        if (track.has("error")) {
            System.err.println("[Spotify] API Fehler: " + track.get("error").get("message").asText());
            return null;
        }
        return trackToSearch(track);
    }

    private List<String> resolvePlaylist(String playlistId) throws Exception {
        System.out.println("[Spotify] Lade Playlist via Embed-Seite...");
        return resolvePlaylistViaEmbed(playlistId);
    }

    private List<String> resolvePlaylistViaEmbed(String playlistId) throws Exception {
        List<String> results = new ArrayList<>();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://open.spotify.com/embed/playlist/" + playlistId))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        String body = resp.body();

        // JSON aus dem <script> Tag extrahieren
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("<script[^>]*>(\\{\"props\".*?)</script>", java.util.regex.Pattern.DOTALL).matcher(body);
        if (!m.find()) {
            System.err.println("[Spotify] Embed-Seite konnte nicht geparst werden");
            throw new RuntimeException("Playlist konnte nicht geladen werden. Moeglicherweise ist sie privat.");
        }

        JsonNode root = mapper.readTree(m.group(1));
        JsonNode trackList = root.at("/props/pageProps/state/data/entity/trackList");
        if (trackList == null || trackList.isMissingNode() || !trackList.isArray()) {
            System.err.println("[Spotify] Keine trackList in Embed-Response");
            throw new RuntimeException("Playlist konnte nicht geladen werden. Moeglicherweise ist sie privat.");
        }

        for (JsonNode track : trackList) {
            if (results.size() >= 200) break;
            String title = track.has("title") ? track.get("title").asText() : null;
            String artist = track.has("subtitle") ? track.get("subtitle").asText() : null;
            if (title != null && artist != null) {
                System.out.println("[Spotify] -> ytsearch: " + title + " " + artist);
                results.add("ytsearch:" + title + " " + artist);
            }
        }
        return results;
    }

    private List<String> resolveAlbum(String albumId) throws Exception {
        System.out.println("[Spotify] Lade Album via Embed-Seite...");
        List<String> results = new ArrayList<>();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://open.spotify.com/embed/album/" + albumId))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        String body = resp.body();

        java.util.regex.Matcher m = java.util.regex.Pattern.compile("<script[^>]*>(\\{\"props\".*?)</script>", java.util.regex.Pattern.DOTALL).matcher(body);
        if (!m.find()) {
            throw new RuntimeException("Album konnte nicht geladen werden.");
        }

        JsonNode root = mapper.readTree(m.group(1));
        JsonNode trackList = root.at("/props/pageProps/state/data/entity/trackList");
        if (trackList != null && trackList.isArray()) {
            for (JsonNode track : trackList) {
                String title = track.has("title") ? track.get("title").asText() : null;
                String artist = track.has("subtitle") ? track.get("subtitle").asText() : null;
                if (title != null && artist != null) {
                    results.add("ytsearch:" + title + " " + artist);
                }
            }
        }
        return results;
    }

    private String trackToSearch(JsonNode track) {
        String name = track.get("name").asText();
        String artist = track.get("artists").get(0).get("name").asText();
        System.out.println("[Spotify] -> ytsearch: " + name + " " + artist);
        return "ytsearch:" + name + " " + artist;
    }

    private void ensureToken() throws Exception {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiry) return;
        String auth = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://accounts.spotify.com/api/token"))
                .header("Authorization", "Basic " + auth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode json = mapper.readTree(resp.body());
        if (json.has("error")) {
            throw new RuntimeException("Spotify Auth fehlgeschlagen: " + json.get("error").asText());
        }
        accessToken = json.get("access_token").asText();
        tokenExpiry = System.currentTimeMillis() + json.get("expires_in").asLong() * 1000 - 60000;
        System.out.println("[Spotify] Token erhalten, gueltig fuer " + json.get("expires_in").asLong() + "s");
    }

    private JsonNode apiGet(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return mapper.readTree(resp.body());
    }
}
