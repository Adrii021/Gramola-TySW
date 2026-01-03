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
            
            System.out.println("----------------------------------------------------------------");
            System.out.println("CORREO SIMULADO PARA: " + email);
            System.out.println("Para confirmar tu cuenta haz clic aquí:");
            System.out.println("http://localhost:8080/users/confirm/Token/" + email + "?token=" + token.getId());
            System.out.println("----------------------------------------------------------------");
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
        if(userToken.getCreationTime() < System.currentTimeMillis() - (60*1000*30)) {
            throw new ResponseStatusException(HttpStatus.GONE, "Token caducado");
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
        
        if (!user.getCreationToken().isUsed()) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Debes completar el pago antes de entrar");
        }
        
        return user;
    }

    // 👇 AQUÍ ESTABA EL ERROR: Usamos setBar en vez de setName
    public User updateUser(String email, String newName, String newPassword) {
        User user = this.userDao.findById(email).orElse(null);
        if (user == null) return null;

        // Actualizamos el nombre del bar
        if (newName != null && !newName.isEmpty()) {
            user.setBar(newName); // <--- CAMBIO: setBar
        }

        // Actualizamos la contraseña
        if (newPassword != null && !newPassword.isEmpty()) {
            user.setPwd(newPassword);
        }

        return this.userDao.save(user);
    }
}