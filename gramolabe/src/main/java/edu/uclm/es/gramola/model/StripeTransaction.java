package edu.uclm.es.gramola.model;

import java.util.UUID;

import org.json.JSONObject;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity
public class StripeTransaction {
    @Id
    private String id;

    @Lob 
    private String data;

    private String email;

    public StripeTransaction() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getData() { return data; }
    public void setData(JSONObject json) { this.data = json.toString(); }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // 👇 ESTE ES EL MÉTODO QUE SOLUCIONA EL ERROR
    // Extrae el client_secret del JSON 'data' para enviarlo al Frontend
    public String getClientSecret() {
        if (this.data != null) {
            try {
                JSONObject json = new JSONObject(this.data);
                return json.optString("client_secret");
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}