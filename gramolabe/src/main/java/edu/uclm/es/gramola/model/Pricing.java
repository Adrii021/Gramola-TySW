package edu.uclm.es.gramola.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Pricing {
    @Id
    private String type; // Ej: "MONTHLY", "YEARLY", "SONG"
    private double price; // Ej: 9.99

    public Pricing() {}

    public Pricing(String type, double price) {
        this.type = type;
        this.price = price;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}