package edu.uclm.es.gramola.services;

import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import edu.uclm.es.gramola.model.StripeTransaction;

@Service
public class PaymentService {

    public PaymentService() {
        // 👇 TU CLAVE PRIVADA (sk_test_...)
        Stripe.apiKey = "sk_test_51SixjzR3ux91c7im0WOIr1M0L5MrJMSSHXCN1dgck9h5revqtadhqIs8hV54S3OJqnyt8hzAE6UEKYfz42uDuGvC008kB8FW5T";
    }

    public StripeTransaction prepay(double amount) throws Exception {
        // Convertimos a céntimos (Stripe usa enteros)
        long amountInCents = (long) (amount * 100);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setCurrency("eur")
                .setAmount(amountInCents)
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        StripeTransaction transaction = new StripeTransaction();
        // Usamos los setters de la clase POJO actualizada
        transaction.setClientSecret(intent.getClientSecret());
        transaction.setAmount(amount);
        
        return transaction;
    }
}