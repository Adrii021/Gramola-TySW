package edu.uclm.es.gramola.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class User {
    @Id
    private String email;
    private String pwd;
    
    private String bar;
    private String clientId;
    private String clientSecret;

    // 👇 CAMBIO: FetchType.EAGER para cargar el token siempre
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, optional = false)
    @JoinColumn(name = "creation_token_id", referencedColumnName = "id")
    // @JsonIgnore  <--- ⚠️ COMENTADO: Así el frontend recibe el token y sabe si has pagado
    private Token creationToken;

    // Token para recuperar contraseña
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "reset_token_id", referencedColumnName = "id")
    @JsonIgnore
    private Token resetToken;

    public void setPwd(String pwd) {
        this.pwd = this.encryptPassword(pwd);
    }

    public String getPwd() {
        return pwd;  
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public String getBar() {
        return bar;
    }

    public void setBar(String bar) {
        this.bar = bar;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setCreationToken(Token token) {
        this.creationToken = token;
    }

    public Token getCreationToken() {
        return creationToken;
    }

    public void setResetToken(Token resetToken) {
        this.resetToken = resetToken;
    }

    public Token getResetToken() {
        return resetToken;
    }

    public void setPassword(String password) {
        this.pwd = encryptPassword(password);
    }

    public String encryptPassword(String password) {
        try {
            MessageDigest digest;
            digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al encriptar la contraseña", e);
        }
    }
}