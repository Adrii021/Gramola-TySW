package edu.uclm.es.gramola.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class StripeTransaction {
    @Id
    private String id;
    
    // 👇 CAMPOS NUEVOS QUE FALTABAN
    private double amount;
    private String clientSecret;
    private long timestamp;

    public StripeTransaction() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    // 👇 AQUÍ ESTÁN LOS MÉTODOS QUE PEDÍA EL ERROR
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}