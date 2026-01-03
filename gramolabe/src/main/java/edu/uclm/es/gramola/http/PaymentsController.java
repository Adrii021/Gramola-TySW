package edu.uclm.es.gramola.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/prepay")
    public StripeTransaction prepay() {
        try {
            return this.service.prepay();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}