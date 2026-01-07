package edu.uclm.es.gramola.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class SelectedTrack {
    @Id
    private String id;
    private String spotifyId;
    private String name;
    private String artist;
    private String userId;
    private long createdAt;
    
    // 👇 CAMPO NUEVO: Para controlar el "Just-in-Time"
    private boolean sentToSpotify = false;

    public SelectedTrack() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSpotifyId() { return spotifyId; }
    public void setSpotifyId(String spotifyId) { this.spotifyId = spotifyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    // 👇 Getters y Setters del nuevo campo
    public boolean isSentToSpotify() { return sentToSpotify; }
    public void setSentToSpotify(boolean sentToSpotify) { this.sentToSpotify = sentToSpotify; }
}