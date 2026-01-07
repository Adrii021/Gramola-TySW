package edu.uclm.es.gramola.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import edu.uclm.es.gramola.model.SelectedTrack;

@Repository
// 👇 CAMBIO CLAVE: <SelectedTrack, String> (antes era Long)
public interface SelectedTrackDao extends CrudRepository<SelectedTrack, String> {
    List<SelectedTrack> findByUserId(String userId);
}