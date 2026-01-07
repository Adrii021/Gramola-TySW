package edu.uclm.es.gramola.http;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.es.gramola.model.StripeTransaction;
import edu.uclm.es.gramola.services.PaymentService;

@RestController
@RequestMapping("payments")
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentsController {

    @Autowired
    private PaymentService service;

    @PostMapping("/prepay")
    public StripeTransaction prepay(@RequestBody Map<String, Object> info) {
        try {
            // 👇 CLAVE: Si el frontend manda precio, lo usamos. Si no, default a 10.00
            // Esto evita el error 400 si el JSON viene incompleto.
            double amount = 10.00; 
            if (info.containsKey("amount")) {
                amount = Double.parseDouble(info.get("amount").toString());
            }
            return this.service.prepay(amount);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}