package edu.uclm.es.gramola.services;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.uclm.es.gramola.dao.SelectedTrackDao;
import edu.uclm.es.gramola.dao.UserDao;
import edu.uclm.es.gramola.model.SelectedTrack;
import edu.uclm.es.gramola.model.User;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;

@Service
public class MusicService {

    @Autowired
    private SelectedTrackDao trackDao;
    
    @Autowired
    private UserDao userDao;

    private SpotifyApi spotifyApi;

    // TUS CREDENCIALES
    private static final String REFRESH_TOKEN = "AQA39Cv3NgIAWKO2Zkh9DWawSdpXC87ZEDRKN-rsSMQ7HWNff3TYcYWPiEFOfXMa25BeWdxt2ghX4djMWxNwhO_uYVQxXqhE3V99iYwby9YVcQjm9KpK9CHAwoTmM25FqJk";

    public MusicService() {
        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId("35dce5653a984b54b813115ba82f578b")
                .setClientSecret("887354fe270744a3ba4a02eb77a24d42")
                .setRefreshToken(REFRESH_TOKEN)
                .build();
        
        this.refreshAccessToken();
    }

    private void refreshAccessToken() {
        try {
            AuthorizationCodeCredentials credentials = this.spotifyApi.authorizationCodeRefresh()
                .build()
                .execute();
            this.spotifyApi.setAccessToken(credentials.getAccessToken());
        } catch (Exception e) {
            System.err.println("❌ Error token: " + e.getMessage());
        }
    }

    // 👇👇👇 EL DJ INTELIGENTE (Cada 5 segundos) 👇👇👇
    @Scheduled(fixedDelay = 5000) 
    public void manageQueue() {
        try {
            // 1. Ver qué suena
            var currentPlayback = this.spotifyApi.getInformationAboutUsersCurrentPlayback().build().execute();
            
            // Si no hay nada sonando, intentamos enviar la primera canción para arrancar
            if (currentPlayback == null || currentPlayback.getItem() == null) {
                feedNextSongIfReady(); 
                return;
            }
            
            // 2. Limpieza: Si la canción que suena estaba en la lista, borramos las anteriores
            String playingId = currentPlayback.getItem().getId();
            List<SelectedTrack> allTracks = getSortedTracks();
            
            int matchIndex = -1;
            for (int i = 0; i < allTracks.size(); i++) {
                if (allTracks.get(i).getSpotifyId().equals(playingId)) {
                    matchIndex = i;
                    break;
                }
            }
            
            if (matchIndex >= 0) {
                // Borramos las que ya han pasado
                for (int i = 0; i < matchIndex; i++) {
                    this.trackDao.delete(allTracks.get(i));
                }
                // Refrescamos la lista tras borrar
                allTracks = getSortedTracks();
            }

            // 3. Lógica "Just-in-Time": 
            // Si queda poco para que acabe la canción (< 20 segundos) O si no hemos enviado la siguiente aún...
            long progress = currentPlayback.getProgress_ms();
            long duration = currentPlayback.getItem().getDurationMs();
            long timeLeft = duration - progress;

            // Enviamos la siguiente SI quedan menos de 20s para el final
            if (timeLeft < 20000) {
                feedNextSongIfReady();
            }

        } catch (Exception e) {
            // Reintentar conexión silenciosamente
            try { this.refreshAccessToken(); } catch (Exception ex) {}
        }
    }

    // Método auxiliar para enviar la SIGUIENTE canción de la lista (la más prioritaria)
    private void feedNextSongIfReady() {
        List<SelectedTrack> allTracks = getSortedTracks();
        
        // Buscamos la primera canción que NO haya sido enviada a Spotify aún
        // (Saltamos la 0 si es la que está sonando actualmente, buscamos la siguiente candidata)
        for (SelectedTrack track : allTracks) {
            if (!track.isSentToSpotify()) {
                System.out.println("🎧 DJ: Turno de '" + track.getName() + "'. Enviando a Spotify...");
                sendToSpotifyQueue(track.getSpotifyId());
                
                track.setSentToSpotify(true);
                this.trackDao.save(track);
                return; // Solo enviamos una cada vez para mantener el control
            }
        }
    }

    private List<SelectedTrack> getSortedTracks() {
        return StreamSupport.stream(this.trackDao.findAll().spliterator(), false)
            .sorted(Comparator.comparingLong(SelectedTrack::getCreatedAt))
            .collect(Collectors.toList());
    }

    public List<Track> searchTracks(String query, User user) {
        try {
            Paging<Track> result = this.spotifyApi.searchTracks(query).build().execute();
            return List.of(result.getItems());
        } catch (Exception e) {
            this.refreshAccessToken();
            try {
                return List.of(this.spotifyApi.searchTracks(query).build().execute().getItems());
            } catch (Exception ex) { throw new RuntimeException("Error búsqueda", ex); }
        }
    }

    public void addTrack(String userId, Object trackObj, boolean isPriority) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Track spotifyTrack = mapper.convertValue(trackObj, Track.class);
        
        SelectedTrack selectedTrack = new SelectedTrack();
        selectedTrack.setId(UUID.randomUUID().toString());
        selectedTrack.setSpotifyId(spotifyTrack.getId());
        selectedTrack.setName(spotifyTrack.getName());
        
        if (spotifyTrack.getArtists() != null && spotifyTrack.getArtists().length > 0) {
            selectedTrack.setArtist(spotifyTrack.getArtists()[0].getName());
        } else {
            selectedTrack.setArtist("Desconocido");
        }
        
        selectedTrack.setUserId(userId);

        if (isPriority) {
            selectedTrack.setCreatedAt(0); 
            System.out.println("🚀 PRIORITY: Añadida al principio de la BD.");
        } else {
            selectedTrack.setCreatedAt(System.currentTimeMillis());
            System.out.println("🎵 NORMAL: Añadida al final de la BD.");
        }

        // 🛑 IMPORTANTE: YA NO ENVIAMOS A SPOTIFY AQUÍ.
        // Lo dejamos marcado como "no enviado" (false por defecto) 
        // y el Vigilante (DJ) lo recogerá en el orden correcto.
        
        this.trackDao.save(selectedTrack);
    }

    private void sendToSpotifyQueue(String trackId) {
        try {
            String uri = "spotify:track:" + trackId;
            this.spotifyApi.addItemToUsersPlaybackQueue(uri).build().execute();
        } catch (Exception e) {
            System.err.println("⚠️ Fallo al enviar a cola: " + e.getMessage());
        }
    }

    public List<SelectedTrack> getPlaylist(String userId) {
        List<SelectedTrack> allTracks = this.trackDao.findByUserId(userId);
        return allTracks.stream()
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