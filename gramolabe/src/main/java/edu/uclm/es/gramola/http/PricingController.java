package edu.uclm.es.gramola.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uclm.es.gramola.dao.PricingDao;
import edu.uclm.es.gramola.model.Pricing;

@RestController
@RequestMapping("pricing")
@CrossOrigin("*")
public class PricingController {

    @Autowired
    private PricingDao pricingDao;

    @SuppressWarnings("unchecked")
    @GetMapping("/all")
    public Iterable<Pricing> getAllPrices() {
        // El doble cast evita el error de "Iterable<Object>"
        return (Iterable<Pricing>) (Iterable) this.pricingDao.findAll();
    }
}