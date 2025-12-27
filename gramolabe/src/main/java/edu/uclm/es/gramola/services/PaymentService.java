package edu.uclm.es.gramola.services;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import edu.uclm.es.gramola.dao.StripeTransactionDao;
import edu.uclm.es.gramola.model.StripeTransaction;

@Service
public class PaymentService {

    static {
        // Clave de prueba del documento
        Stripe.apiKey = "sk_test_51SixjzR3ux91c7im0WOIr1M0L5MrJMSSHXCN1dgck9h5revqtadhqIs8hV54S3OJqnyt8hzAE6UEKYfz42uDuGvC008kB8FW5T";
    }

    @Autowired
    private StripeTransactionDao dao;

    public StripeTransaction prepay() throws StripeException {
        PaymentIntentCreateParams createParams = new PaymentIntentCreateParams.Builder()
                .setCurrency("eur")
                .setAmount(1000L) // 10.00 euros
                .build();

        PaymentIntent intent = PaymentIntent.create(createParams);
        JSONObject transactionDetails = new JSONObject(intent.toJson());

        StripeTransaction st = new StripeTransaction();
        st.setData(transactionDetails);
        this.dao.save(st);

        return st;
    }
}