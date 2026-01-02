package edu.uclm.es.gramola.http;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin; // Importa todo para no fallar
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.es.gramola.dao.UserDao;
import edu.uclm.es.gramola.model.User;
import edu.uclm.es.gramola.services.MusicService;
import se.michaelthelin.spotify.model_objects.specification.Track;

@RestController
@RequestMapping("/music")
@CrossOrigin("*")
public class MusicController {

    @Autowired
    private MusicService musicService;

    @Autowired
    private UserDao userDao;

    @PostMapping("/search")
    public List<Track> search(@RequestBody Map<String, String> info) {
        String userId = info.get("userId");
        String query = info.get("query");
        Optional<User> optUser = this.userDao.findById(userId);
        if (optUser.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        return this.musicService.searchTracks(query, optUser.get());
    }

    // 👇 NUEVO: Añadir canción
    @PostMapping("/add")
    public void add(@RequestBody Map<String, Object> info) {
        String userId = (String) info.get("userId");
        // Convertimos el mapa del track a un objeto usable o lo guardamos tal cual
        // Para simplificar, pasamos el objeto Track directamente al servicio
        this.musicService.addTrack(userId, info.get("track"));
    }

    // 👇 NUEVO: Ver lista
    @PostMapping("/playlist")
    public List<Object> getPlaylist(@RequestBody Map<String, String> info) {
        String userId = info.get("userId");
        return this.musicService.getPlaylist(userId);
    }

    // 👇 NUEVO: Endpoint para borrar
    @PostMapping("/remove")
    public void remove(@RequestBody Map<String, String> info) {
        String userId = info.get("userId");
        String trackId = info.get("trackId");
        this.musicService.removeTrack(userId, trackId);
    }
}