package edu.uclm.es.gramola.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.util.UUID;
import org.json.JSONObject;

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
}