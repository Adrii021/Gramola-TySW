package edu.uclm.es.gramola.http;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uclm.es.gramola.dao.UserDao;
import edu.uclm.es.gramola.model.SelectedTrack;
import edu.uclm.es.gramola.model.User;
import edu.uclm.es.gramola.services.MusicService;
import se.michaelthelin.spotify.model_objects.specification.Track;

@RestController
@RequestMapping("music")
@CrossOrigin("*")
public class MusicController {

    @Autowired
    private MusicService service;
    
    @Autowired
    private UserDao userDao;

    @PostMapping("/search")
    public List<Track> search(@RequestBody Map<String, String> info) {
        String userId = info.get("userId");
        String query = info.get("query");
        
        User user = this.userDao.findById(userId).orElse(null);
        if (user == null) return null;
        
        return this.service.searchTracks(query, user);
    }

    // 👇 MÉTODO CORREGIDO
    @PostMapping("/add")
    public void add(@RequestBody Map<String, Object> info) {
        String userId = (String) info.get("userId");
        Object track = info.get("track");
        
        // Leemos si viene el campo "priority" (true/false). Si no viene, es false.
        boolean isPriority = false;
        if (info.containsKey("priority")) {
            isPriority = (boolean) info.get("priority");
        }

        // Llamamos al servicio con los 3 argumentos
        this.service.addTrack(userId, track, isPriority);
    }

    @PostMapping("/playlist")
    public List<SelectedTrack> getPlaylist(@RequestBody Map<String, String> info) {
        String userId = info.get("userId");
        return this.service.getPlaylist(userId);
    }

    @PostMapping("/remove")
    public void remove(@RequestBody Map<String, String> info) {
        String userId = info.get("userId");
        String trackId = info.get("trackId");
        this.service.removeTrack(userId, trackId);
    }
}