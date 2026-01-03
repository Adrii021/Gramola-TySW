package edu.uclm.es.gramola.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map; // Importante para que funcione el borrado

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.uclm.es.gramola.dao.SelectedTrackDao;
import edu.uclm.es.gramola.model.SelectedTrack;
import edu.uclm.es.gramola.model.User;
import jakarta.transaction.Transactional;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.data.search.simplified.SearchTracksRequest;

@Service
public class MusicService {

    @Autowired
    private SelectedTrackDao trackDao; // <--- Inyectamos el nuevo DAO

    // Búsqueda en Spotify (esto no cambia)
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

    // 👇 CAMBIO: Guardar en Base de Datos (Convertimos el JSON de Spotify a nuestra Entidad)
    public void addTrack(String userId, Object trackObj) {
        if (trackObj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) trackObj;
            
            SelectedTrack track = new SelectedTrack();
            track.setUserId(userId);
            track.setSpotifyId((String) map.get("id"));
            track.setName((String) map.get("name"));
            
            // Extraer artista (viene en una lista)
            List<Map<String, Object>> artists = (List<Map<String, Object>>) map.get("artists");
            if (artists != null && !artists.isEmpty()) {
                track.setArtist((String) artists.get(0).get("name"));
            }
            
            // Extraer imagen (viene en una lista)
            Map<String, Object> album = (Map<String, Object>) map.get("album");
            if (album != null) {
                List<Map<String, Object>> images = (List<Map<String, Object>>) album.get("images");
                if (images != null && !images.isEmpty()) {
                    track.setImageUrl((String) images.get(0).get("url"));
                }
            }
            
            this.trackDao.save(track);
        }
    }

    // 👇 CAMBIO: Recuperar de Base de Datos
    public List<SelectedTrack> getPlaylist(String userId) {
        return this.trackDao.findByUserId(userId);
    }

    // 👇 CAMBIO: Borrar de Base de Datos
    @Transactional
    public void removeTrack(String userId, String spotifyId) {
        this.trackDao.deleteByUserIdAndSpotifyId(userId, spotifyId);
    }
}