package edu.uclm.es.gramola.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import edu.uclm.es.gramola.model.User;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.data.search.simplified.SearchTracksRequest;

@Service
public class MusicService {

    // 🧠 MEMORIA: Guardamos aquí las listas de cada usuario
    private ConcurrentHashMap<String, List<Object>> playlists = new ConcurrentHashMap<>();

    public List<Track> searchTracks(String query, User user) {
        try {
            SpotifyApi spotifyApi = new SpotifyApi.Builder()
                    .setClientId(user.getClientId())
                    .setClientSecret(user.getClientSecret())
                    .build();
            String accessToken = spotifyApi.clientCredentials().build().execute().getAccessToken();
            spotifyApi.setAccessToken(accessToken);

            SearchTracksRequest searchRequest = spotifyApi.searchTracks(query).limit(10).build();
            Paging<Track> trackPaging = searchRequest.execute();
            return List.of(trackPaging.getItems());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // 👇 Guardar canción
    public void addTrack(String userId, Object track) {
        // Si no existe lista para este usuario, la crea
        this.playlists.putIfAbsent(userId, new ArrayList<>());
        // Añade la canción a su lista
        this.playlists.get(userId).add(track);
    }

    // 👇 Recuperar lista
    public List<Object> getPlaylist(String userId) {
        return this.playlists.getOrDefault(userId, new ArrayList<>());
    }
    public void removeTrack(String userId, String trackId) {
        List<Object> userPlaylist = this.playlists.get(userId);
        if (userPlaylist != null) {
            // Recorremos la lista y quitamos la canción que tenga ese ID
            userPlaylist.removeIf(track -> {
                if (track instanceof java.util.Map) {
                    java.util.Map<String, Object> trackMap = (java.util.Map<String, Object>) track;
                    return trackId.equals(trackMap.get("id"));
                }
                return false;
            });
        }
    }
}