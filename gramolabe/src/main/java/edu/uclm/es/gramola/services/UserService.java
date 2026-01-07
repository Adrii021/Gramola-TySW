package edu.uclm.es.gramola.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.es.gramola.dao.TokenDao;
import edu.uclm.es.gramola.dao.UserDao;
import edu.uclm.es.gramola.model.Token;
import edu.uclm.es.gramola.model.User;

@Service
public class UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private TokenDao tokenDao;

    public void register(String email, String pwd, String bar, String clientId, String clientSecret) {
        Optional<User> optUser = this.userDao.findById(email);
        if (optUser.isEmpty()) {
            User user = new User();
            user.setEmail(email);
            user.setPwd(pwd);
            
            user.setBar(bar);
            user.setClientId(clientId);
            user.setClientSecret(clientSecret);
            
            Token token = new Token();
            user.setCreationToken(token);
            this.userDao.save(user);
            
            System.out.println("--- EMAIL REGISTRO ---");
            System.out.println("Confirmar: http://localhost:8080/users/confirm/Token/" + email + "?token=" + token.getId());
        }
        else {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya existe");
        }
    }

    public void delete(String email) {
        this.userDao.deleteById(email);
    }

    public void confirmToken(String email, String token) {
        Optional<User> optUser = this.userDao.findById(email);
        
        if (optUser.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el usuario");
        }
        
        User user = optUser.get();
        Token userToken = user.getCreationToken();
        
        if(!userToken.getId().equals(token)) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Token incorrecto");
        }
        if(userToken.isUsed()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Token ya usado");
        }
        
        userToken.use();
        this.userDao.save(user); 
    }

    public User login(String email, String pwd) {
        Optional<User> optUser = this.userDao.findById(email);
        
        if (optUser.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Credenciales incorrectas");
        }
        User user = optUser.get();
        
        if (!user.getPwd().equals(user.encryptPassword(pwd))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Credenciales incorrectas");
        }
        
        // 👇 CAMBIO: Comentamos esto para permitir login sin pago (Modelo Freemium)
        /*
        if (!user.getCreationToken().isUsed()) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Debes completar el pago antes de entrar");
        }
        */
        
        return user;
    }

    public User updateUser(String email, String newName, String newPassword) {
        User user = this.userDao.findById(email).orElse(null);
        if (user == null) return null;

        if (newName != null && !newName.isEmpty()) {
            user.setBar(newName);
        }

        if (newPassword != null && !newPassword.isEmpty()) {
            user.setPwd(newPassword);
        }

        return this.userDao.save(user);
    }

    public void requestPasswordReset(String email) {
        Optional<User> optUser = this.userDao.findById(email);
        if (optUser.isPresent()) {
            User user = optUser.get();
            Token token = new Token();
            user.setResetToken(token);
            this.userDao.save(user);

            System.out.println("----------------------------------------------------------------");
            System.out.println("📧 RECUPERACIÓN DE CONTRASEÑA PARA: " + email);
            System.out.println("Haz clic aquí para cambiarla:");
            System.out.println("http://localhost:4200/reset-password?email=" + email + "&token=" + token.getId());
            System.out.println("----------------------------------------------------------------");
        }
    }

    public void resetPassword(String email, String token, String newPassword) {
        User user = this.userDao.findById(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        
        Token resetToken = user.getResetToken();
        if (resetToken == null || !resetToken.getId().equals(token)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido");
        }
        
        user.setPwd(newPassword);
        user.setResetToken(null); 
        this.userDao.save(user);
    }
    public String getBarName(String email) {
        return this.userDao.findById(email)
            .map(User::getBar) // Obtiene el nombre si existe
            .orElse(null);     // Devuelve null si no existe
    }
}