package edu.uclm.es.gramola.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uclm.es.gramola.model.SelectedTrack;

@Repository
public interface SelectedTrackDao extends JpaRepository<SelectedTrack, Long> {
    // Buscar todas las canciones de un usuario
    List<SelectedTrack> findByUserId(String userId);
    
    // Borrar una canción específica de un usuario usando el ID de Spotify
    void deleteByUserIdAndSpotifyId(String userId, String spotifyId);
}