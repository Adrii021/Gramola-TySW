package edu.uclm.es.gramola.services;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;

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
    // Para detectar cambios/skip en la reproducción
    private volatile String lastPlayingId = null;
    // SSE emitters
    private CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

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
            e.printStackTrace();
        }
    }

    // SSE: suscripción desde frontend
    public SseEmitter subscribeToEvents() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        this.emitters.add(emitter);

        emitter.onCompletion(() -> this.emitters.remove(emitter));
        emitter.onTimeout(() -> this.emitters.remove(emitter));
        emitter.onError((ex) -> this.emitters.remove(emitter));

        // Enviar estado inicial
        try {
            Map<String, Object> state = this.buildStateSnapshot();
            emitter.send(SseEmitter.event().name("state").data(state));
        } catch (Exception e) {
            // ignore send failures at subscribe
        }

        return emitter;
    }

    private Map<String, Object> buildStateSnapshot() {
        Map<String, Object> state = new HashMap<>();
        try {
            Object current = this.spotifyApi.getInformationAboutUsersCurrentPlayback().build().execute();
            Object devices = this.spotifyApi.getUsersAvailableDevices().build().execute();
            List<SelectedTrack> queue = getSortedTracks();
            state.put("current", current);
            state.put("devices", devices);
            state.put("queue", queue);
        } catch (Exception e) {
            // ignore, return partial state
            state.put("current", null);
            state.put("devices", null);
            state.put("queue", getSortedTracks());
        }
        return state;
    }

    private void broadcastState() {
        Map<String, Object> state = buildStateSnapshot();
        for (SseEmitter emitter : this.emitters) {
            try {
                emitter.send(SseEmitter.event().name("state").data(state));
            } catch (Exception e) {
                this.emitters.remove(emitter);
            }
        }
    }

    // 👇👇👇 EL DJ INTELIGENTE (Cada 5 segundos) 👇👇👇
    @Scheduled(fixedDelay = 5000) 
    public void manageQueue() {
        try {
            System.out.println("⏱️ manageQueue tick");
            // 1. Ver qué suena
            var currentPlayback = this.spotifyApi.getInformationAboutUsersCurrentPlayback().build().execute();
            
            // Si no hay nada sonando, intentamos enviar la primera canción para arrancar
            if (currentPlayback == null || currentPlayback.getItem() == null) {
                System.out.println("ℹ️ No hay reproducción activa. Intentando enviar la primera canción de la cola...");
                feedNextSongIfReady(); 
                return;
            }
            
            // 2. Limpieza: Si la canción que suena estaba en la lista, borramos las anteriores
            String playingId = currentPlayback.getItem().getId();
            List<SelectedTrack> allTracks = getSortedTracks();
            
            System.out.println("🎶 Actualmente suena ID: " + playingId);

            // Detectar si ha habido un cambio brusco en la reproducción (skip/manual)
            boolean playbackChanged = (this.lastPlayingId != null && !this.lastPlayingId.equals(playingId));
            if (playbackChanged) {
                System.out.println("⚡ Cambio de reproducción detectado (skip?): " + this.lastPlayingId + " -> " + playingId);
                boolean hasPending = allTracks.stream().anyMatch(t -> !t.isSentToSpotify());
                if (hasPending) {
                    System.out.println("ℹ️ Tras el cambio hay pistas pendientes — enviando siguiente inmediatamente.");
                    feedNextSongIfReady();
                    // Actualizamos lastPlayingId y salimos para evitar doble envío en este tick
                    this.lastPlayingId = playingId;
                    return;
                }
            }

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

            // Si la canción actual NO pertenece a nuestra cola pero hay pistas pendientes,
            // enviamos inmediatamente la siguiente para que la cola empiece a sonar.
            if (matchIndex < 0) {
                boolean hasPending = allTracks.stream().anyMatch(t -> !t.isSentToSpotify());
                if (hasPending) {
                    System.out.println("ℹ️ La pista que suena no pertenece a la cola. Enviando siguiente pista pendiente.");
                    feedNextSongIfReady();
                    return;
                }
            }

            // 3. Lógica "Just-in-Time": 
            // Si queda poco para que acabe la canción (< 20 segundos) O si no hemos enviado la siguiente aún...
            long progress = currentPlayback.getProgress_ms();
            long duration = currentPlayback.getItem().getDurationMs();
            long timeLeft = duration - progress;

            System.out.println("⏱️ progress=" + progress + "ms duration=" + duration + "ms timeLeft=" + timeLeft + "ms matchIndex=" + matchIndex);

            // Enviamos la siguiente SI quedan menos de 20s para el final
            if (timeLeft < 20000) {
                System.out.println("ℹ️ Quedan <20s en la pista actual — intentando enviar siguiente.");
                feedNextSongIfReady();
            } else {
                System.out.println("ℹ️ Aún quedan >20s en la pista actual — no envío. (timeLeft=" + timeLeft + ")");
            }

            // Difundimos estado tras cada tick para que front reciba cambios en tiempo real
            try { this.broadcastState(); } catch (Exception ex) { /* ignore */ }

        } catch (Exception e) {
            // Reintentar conexión silenciosamente
            System.err.println("⚠️ Excepción en manageQueue: " + e.getMessage());
            e.printStackTrace();
            try { this.refreshAccessToken(); } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    // Método auxiliar para enviar la SIGUIENTE canción de la lista (la más prioritaria)
    private void feedNextSongIfReady() {
        List<SelectedTrack> allTracks = getSortedTracks();
        System.out.println("📋 Cola actual (total): " + allTracks.size());
        
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

        System.out.println("ℹ️ No hay canciones pendientes de enviar o todas ya fueron enviadas.");
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

    // Devuelve información de reproducción actual (objeto tal como lo devuelve la biblioteca)
    public Object getCurrentPlaybackInfo() {
        try {
            var currentPlayback = this.spotifyApi.getInformationAboutUsersCurrentPlayback().build().execute();
            return currentPlayback;
        } catch (Exception e) {
            this.refreshAccessToken();
            try {
                return this.spotifyApi.getInformationAboutUsersCurrentPlayback().build().execute();
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    // Devuelve lista de dispositivos disponibles para el usuario
    public Object getAvailableDevices() {
        try {
            return this.spotifyApi.getUsersAvailableDevices().build().execute();
        } catch (Exception e) {
            this.refreshAccessToken();
            try {
                return this.spotifyApi.getUsersAvailableDevices().build().execute();
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
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

        // Si no hay reproducción activa, intentamos arrancar la canción añadida
        try {
            var current = this.spotifyApi.getInformationAboutUsersCurrentPlayback().build().execute();
            if (current == null || current.getItem() == null) {
                System.out.println("ℹ️ Cola vacía y sin reproducción: intentamos iniciar reproducción de la pista añadida...");
                tryStartPlaybackOfTrack(spotifyTrack.getUri());
            }
        } catch (Exception e) {
            // intentar refrescar token y no bloquear
            System.err.println("⚠️ Error comprobando reproducción tras añadir: " + e.getMessage());
            try { this.refreshAccessToken(); } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    // Intentar iniciar/reanudar reproducción con una pista concreta
    public void tryStartPlaybackOfTrack(String trackUri) {
        if (trackUri == null) return;
        try {
            System.out.println("▶️ Intentando start/resume con URI: " + trackUri);
            // Primero añadimos la pista a la cola
            this.spotifyApi.addItemToUsersPlaybackQueue(trackUri).build().execute();
            // Luego iniciamos la reproducción sin argumentos
            this.spotifyApi.startResumeUsersPlayback().build().execute();
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo iniciar reproducción directamente: " + e.getMessage());
            e.printStackTrace();
            try { this.refreshAccessToken(); } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    public void play() {
        try {
            this.spotifyApi.startResumeUsersPlayback().build().execute();
        } catch (Exception e) {
            System.err.println("⚠️ Error en play(): " + e.getMessage());
            e.printStackTrace();
            try { this.refreshAccessToken(); } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    public void pause() {
        try {
            this.spotifyApi.pauseUsersPlayback().build().execute();
        } catch (Exception e) {
            System.err.println("⚠️ Error en pause(): " + e.getMessage());
            e.printStackTrace();
            try { this.refreshAccessToken(); } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    public void skipToNext() {
        // Implementación alternativa: arrancamos la siguiente pista pendiente directamente
        try {
            List<SelectedTrack> all = getSortedTracks();
            for (SelectedTrack t : all) {
                if (!t.isSentToSpotify()) {
                    System.out.println("⏭️ Skip: iniciando siguiente pista " + t.getName());
                    tryStartPlaybackOfTrack("spotify:track:" + t.getSpotifyId());
                    t.setSentToSpotify(true);
                    this.trackDao.save(t);
                    return;
                }
            }
            System.out.println("⏭️ Skip: no hay pista pendiente para iniciar.");
        } catch (Exception e) {
            System.err.println("⚠️ Error en skipToNext(): " + e.getMessage());
            e.printStackTrace();
            try { this.refreshAccessToken(); } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    public void transferPlayback(String deviceId, boolean play) {
        try {
            if (deviceId == null) return;
            System.out.println("🔁 Transfer playback to device: " + deviceId + " play=" + play);
            JsonArray array = new JsonArray();
            array.add(deviceId);
            this.spotifyApi.transferUsersPlayback(array).build().execute();
        } catch (Exception e) {
            System.err.println("⚠️ Error en transferPlayback(): " + e.getMessage());
            e.printStackTrace();
            try { this.refreshAccessToken(); } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    private void sendToSpotifyQueue(String trackId) {
        try {
            String uri = "spotify:track:" + trackId;
            this.spotifyApi.addItemToUsersPlaybackQueue(uri).build().execute();
        } catch (Exception e) {
            System.err.println("⚠️ Fallo al enviar a cola: " + e.getMessage());
            e.printStackTrace();
            System.err.println("   -> Comprueba que Spotify esté abierto y que exista un dispositivo activo (app de escritorio o móvil).\n" +
                               "   -> Si el mensaje indica token inválido, revisa las credenciales/refresh token.");
        }
    }

    public List<SelectedTrack> getPlaylist(String userId) {
        List<SelectedTrack> allTracks = this.trackDao.findByUserId(userId);
        return allTracks.stream()
                .sorted(Comparator.comparingLong(SelectedTrack::getCreatedAt))
                .collect(Collectors.toList());
    }

    // Devuelve la cola (lista ordenada) para un usuario (misma estructura que getPlaylist)
    public List<SelectedTrack> getQueueForUser(String userId) {
        return getSortedTracks().stream()
                .filter(t -> t.getUserId() != null && t.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public void removeTrack(String userId, String trackId) {
        SelectedTrack track = this.trackDao.findById(trackId).orElse(null);
        if (track != null && track.getUserId().equals(userId)) {
            this.trackDao.delete(track);
        }
    }
}