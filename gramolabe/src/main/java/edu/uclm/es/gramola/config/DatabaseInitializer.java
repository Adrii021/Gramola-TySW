package edu.uclm.es.gramola.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import edu.uclm.es.gramola.dao.PricingDao;
import edu.uclm.es.gramola.model.Pricing;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private PricingDao pricingDao;

    @Override
    public void run(String... args) throws Exception {
        // Creamos los precios por defecto si no existen
        if (!pricingDao.existsById("MONTHLY")) {
            pricingDao.save(new Pricing("MONTHLY", 9.99));
            System.out.println("💰 Precio MENSUAL inicializado: 9.99€");
        }
        if (!pricingDao.existsById("YEARLY")) {
            pricingDao.save(new Pricing("YEARLY", 99.00));
            System.out.println("💰 Precio ANUAL inicializado: 99.00€");
        }
        if (!pricingDao.existsById("SONG")) {
            pricingDao.save(new Pricing("SONG", 0.99)); // Precio por canción
            System.out.println("💰 Precio CANCIÓN inicializado: 0.99€");
        }
    }
}