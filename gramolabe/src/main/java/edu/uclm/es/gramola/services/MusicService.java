package edu.uclm.es.gramola.services;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.uclm.es.gramola.dao.SelectedTrackDao;
import edu.uclm.es.gramola.dao.UserDao;
import edu.uclm.es.gramola.model.SelectedTrack;
import edu.uclm.es.gramola.model.User;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.data.search.simplified.SearchTracksRequest;

@Service
public class MusicService {

    @Autowired
    private SelectedTrackDao trackDao;
    
    @Autowired
    private UserDao userDao;

    public List<Track> searchTracks(String query, User user) {
        SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setClientId(user.getClientId())
                .setClientSecret(user.getClientSecret())
                .build();
        
        try {
            var clientCredentialsRequest = spotifyApi.clientCredentials().build();
            var clientCredentials = clientCredentialsRequest.execute();
            spotifyApi.setAccessToken(clientCredentials.getAccessToken());

            SearchTracksRequest searchTracksRequest = spotifyApi.searchTracks(query).build();
            Paging<Track> trackPaging = searchTracksRequest.execute();
            
            return List.of(trackPaging.getItems());
        } catch (Exception e) {
            System.err.println("Error buscando en Spotify: " + e.getMessage());
            return List.of();
        }
    }

    // 👇 MODIFICADO: Acepta booleano de prioridad
    public void addTrack(String userId, Object trackObj, boolean isPriority) {
        java.util.Map<String, Object> map = (java.util.Map<String, Object>) trackObj;
        
        SelectedTrack track = new SelectedTrack();
        track.setSpotifyId((String) map.get("id"));
        track.setName((String) map.get("name"));
        
        List<java.util.Map> artists = (List<java.util.Map>) map.get("artists");
        if (artists != null && !artists.isEmpty()) {
            track.setArtist((String) artists.get(0).get("name"));
        } else {
            track.setArtist("Desconocido");
        }
        
        track.setUserId(userId);

        if (isPriority) {
            // TRUCO: Si ha pagado, ponemos fecha 0 para que sea la "más vieja"
            // y salga al principio de la lista al ordenar.
            track.setCreatedAt(0); 
            System.out.println("🚀 PRIORITY: Canción insertada al principio de la cola.");
        } else {
            // Normal: se añade al final (fecha actual)
            track.setCreatedAt(System.currentTimeMillis());
        }

        this.trackDao.save(track);
    }

    // 👇 MODIFICADO: Devuelve la lista ordenada por fecha
    public List<SelectedTrack> getPlaylist(String userId) {
        List<SelectedTrack> allTracks = this.trackDao.findByUserId(userId);
        
        return allTracks.stream()
                // Orden ascendente: 0 (Priority) sale primero, luego las nuevas
                .sorted(Comparator.comparingLong(SelectedTrack::getCreatedAt))
                .collect(Collectors.toList());
    }

    public void removeTrack(String userId, String trackId) {
        SelectedTrack track = this.trackDao.findById(trackId).orElse(null);
        if (track != null && track.getUserId().equals(userId)) {
            this.trackDao.delete(track);
        }
    }
}