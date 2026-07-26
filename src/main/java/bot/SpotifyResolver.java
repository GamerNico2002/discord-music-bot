package bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpotifyResolver {

    private static final Logger log = LoggerFactory.getLogger(SpotifyResolver.class);

    private static final Pattern SPOTIFY_TRACK = Pattern.compile("open\\.spotify\\.com/(?:intl-[a-z]{2}/)?track/([a-zA-Z0-9]+)");
    private static final Pattern SPOTIFY_PLAYLIST = Pattern.compile("open\\.spotify\\.com/(?:intl-[a-z]{2}/)?playlist/([a-zA-Z0-9]+)");
    private static final Pattern SPOTIFY_ALBUM = Pattern.compile("open\\.spotify\\.com/(?:intl-[a-z]{2}/)?album/([a-zA-Z0-9]+)");

    private final ExecutorService httpExecutor = Executors.newFixedThreadPool(8);
    private final HttpClient http = HttpClient.newBuilder()
            .executor(httpExecutor)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String clientId;
    private final String clientSecret;
    private volatile String accessToken;
    private volatile long tokenExpiry;

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
                    log.info("[Spotify] Redirect aufgeloest: {}", url);
                }

                Matcher trackMatcher = SPOTIFY_TRACK.matcher(url);
                Matcher playlistMatcher = SPOTIFY_PLAYLIST.matcher(url);
                Matcher albumMatcher = SPOTIFY_ALBUM.matcher(url);

                if (trackMatcher.find()) {
                    log.info("[Spotify] Track ID: {}", trackMatcher.group(1));
                    String search = resolveTrack(trackMatcher.group(1));
                    if (search != null) queries.add(search);
                } else if (playlistMatcher.find()) {
                    log.info("[Spotify] Playlist ID: {}", playlistMatcher.group(1));
                    queries.addAll(resolvePlaylist(playlistMatcher.group(1)));
                } else if (albumMatcher.find()) {
                    log.info("[Spotify] Album ID: {}", albumMatcher.group(1));
                    queries.addAll(resolveAlbum(albumMatcher.group(1)));
                } else {
                    log.warn("[Spotify] URL nicht erkannt: {}", url);
                }
            } catch (Exception e) {
                log.error("[Spotify] Fehler: {}", e.getMessage(), e);
            }
            log.info("[Spotify] {} Songs aufgeloest", queries.size());
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
            log.error("[Spotify] API Fehler: {}", track.get("error").get("message").asText());
            return null;
        }
        return trackToSearch(track);
    }

    private List<String> resolvePlaylist(String playlistId) throws Exception {
        log.info("[Spotify] Lade Playlist via API mit Pagination...");
        ensureToken();
        List<String> results = new ArrayList<>();
        int offset = 0;
        int limit = 50;
        while (true) {
            JsonNode page = apiGet("https://api.spotify.com/v1/playlists/" + playlistId
                    + "/tracks?limit=" + limit + "&offset=" + offset + "&fields=items(track(name,artists(name))),total,next");
            JsonNode items = page.get("items");
            if (items == null || !items.isArray()) break;
            for (JsonNode item : items) {
                JsonNode track = item.get("track");
                if (track == null || track.isNull()) continue;
                String search = trackToSearch(track);
                if (search != null) results.add(search);
            }
            if (results.size() >= 500) break;
            if (page.get("next") == null || page.get("next").isNull()) break;
            offset += limit;
        }
        log.info("[Spotify] Playlist: {} Songs via API geladen", results.size());
        return results;
    }

    private List<String> resolveAlbum(String albumId) throws Exception {
        log.info("[Spotify] Lade Album via API mit Pagination...");
        ensureToken();
        List<String> results = new ArrayList<>();
        int offset = 0;
        int limit = 50;
        while (true) {
            JsonNode page = apiGet("https://api.spotify.com/v1/albums/" + albumId
                    + "/tracks?limit=" + limit + "&offset=" + offset + "&fields=items(name,artists(name)),total,next");
            JsonNode items = page.get("items");
            if (items == null || !items.isArray()) break;
            for (JsonNode track : items) {
                String search = trackToSearch(track);
                if (search != null) results.add(search);
            }
            if (results.size() >= 500) break;
            if (page.get("next") == null || page.get("next").isNull()) break;
            offset += limit;
        }
        log.info("[Spotify] Album: {} Songs via API geladen", results.size());
        return results;
    }

    private String trackToSearch(JsonNode track) {
        String name = track.get("name").asText();
        String artist = track.get("artists").get(0).get("name").asText();
        log.debug("[Spotify] -> ytsearch: {} {}", name, artist);
        return "ytsearch:" + name + " " + artist;
    }

    private synchronized void ensureToken() throws Exception {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiry) return;
        accessToken = null;
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
        log.info("[Spotify] Token erhalten, gueltig fuer {}s", json.get("expires_in").asLong());
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
