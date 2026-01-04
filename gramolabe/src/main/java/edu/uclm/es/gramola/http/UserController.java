package edu.uclm.es.gramola.http;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.es.gramola.model.User;
import edu.uclm.es.gramola.services.UserService;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("users")
@CrossOrigin("*")
public class UserController {

    @Autowired
    private UserService service;

    @PostMapping("/register")
    public void register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String pwd1 = body.get("pwd1");
        String pwd2 = body.get("pwd2");
        String bar = body.get("bar");
        String clientId = body.get("clientId");
        String clientSecret = body.get("clientSecret");

        if(!pwd1.equals(pwd2)) throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Passwords do not match");
        if(pwd1.length() < 4) throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Password too short");
        
        this.service.register(email, pwd1, bar, clientId, clientSecret);
    }

    @DeleteMapping("/delete")
    public void delete(@RequestParam String email) {
        this.service.delete(email);
    }

    @GetMapping("/confirm/Token/{email}")
    public void confirmToken(@PathVariable String email, @RequestParam String token, HttpServletResponse response) throws IOException {
        this.service.confirmToken(email, token);
        response.sendRedirect("http://localhost:4200/payment?token=" + token);
    }

    @PostMapping("/login")
    public User login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String pwd = body.get("pwd");
        return this.service.login(email, pwd);
    }

    @PostMapping("/update")
    public User update(@RequestBody Map<String, String> info) {
        String email = info.get("userId");
        String name = info.get("name");
        String pwd = info.get("password");
        
        User updatedUser = this.service.updateUser(email, name, pwd);
        
        if (updatedUser == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        return updatedUser;
    }

    // 👇 NUEVO: Pedir recuperación
    @PostMapping("/request-reset")
    public void requestReset(@RequestBody Map<String, String> info) {
        String email = info.get("email");
        this.service.requestPasswordReset(email);
    }

    // 👇 NUEVO: Ejecutar cambio de contraseña
    @PostMapping("/reset-pwd")
    public void resetPwd(@RequestBody Map<String, String> info) {
        String email = info.get("email");
        String token = info.get("token");
        String pwd1 = info.get("pwd1");
        String pwd2 = info.get("pwd2");

        if (!pwd1.equals(pwd2)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las contraseñas no coinciden");
        
        this.service.resetPassword(email, token, pwd1);
    }
}