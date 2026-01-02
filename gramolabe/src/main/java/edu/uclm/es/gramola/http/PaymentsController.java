package edu.uclm.es.gramola.http;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping; // <--- ESTO ES CRUCIAL
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.es.gramola.model.StripeTransaction;
import edu.uclm.es.gramola.services.PaymentService;

@RestController
@RequestMapping("payments")
@CrossOrigin("*") // <--- Ponemos "*" para curarnos en salud con los permisos
public class PaymentsController {

    @Autowired
    private PaymentService service;

    // 👇 CAMBIO CLAVE: @PostMapping en vez de @GetMapping
    @PostMapping("/prepay")
    public StripeTransaction prepay(@RequestBody Map<String, String> info) {
        try {
            // Angular nos envía el token en 'info', así que lo aceptamos
            return this.service.prepay();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}