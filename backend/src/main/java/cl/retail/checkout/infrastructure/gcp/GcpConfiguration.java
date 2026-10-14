package cl.retail.checkout.infrastructure.gcp;

import cl.retail.checkout.application.CheckoutEventPublisher;
import cl.retail.checkout.application.CheckoutRepository;
import cl.retail.checkout.application.CheckoutService;
import cl.retail.checkout.domain.*;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.TopicName;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;

@Configuration
@Profile("!test")
@EnableConfigurationProperties(GcpProperties.class)
public class GcpConfiguration {
    @Bean
    Firestore firestore() {
        // Aquí uso ADC del CLI; así no dejo secretos ni llaves de servicio dentro del repositorio.
        return FirestoreOptions.getDefaultInstance().getService();
    }

    @Bean
    BigQuery bigQuery() {
        return BigQueryOptions.getDefaultInstance().getService();
    }

    @Bean(destroyMethod = "shutdown")
    Publisher checkoutPublisher(GcpProperties properties) throws Exception {
        return Publisher.newBuilder(TopicName.of(properties.projectId(), properties.topicId()))
                .setCredentialsProvider(FixedCredentialsProvider.create(GoogleCredentials.getApplicationDefault())).build();
    }

    @Bean
    CheckoutRepository checkoutRepository(Firestore firestore) {
        return new FirestoreCheckoutRepository(firestore);
    }

    @Bean
    CheckoutEventPublisher eventPublisher(Publisher publisher, GcpProperties properties) {
        return new PubSubCheckoutEventPublisher(publisher, properties.topicId());
    }

    @Bean
    CheckoutService checkoutService(CheckoutRepository repository, CheckoutEventPublisher publisher) {
        return new CheckoutService(repository, publisher);
    }

    @Bean
    ApplicationRunner seedCatalog(Firestore firestore) {
        return args -> {
            // Aquí garantizo datos de demo idempotentes en colecciones separadas de catálogo y promociones.
            var batch = firestore.batch();
            batch.set(firestore.collection("products").document("tv-001"), java.util.Map.of("name", "Smart TV 55 pulgadas", "category", "Tecnología", "unitPrice", 299990, "productDiscountPercent", 15));
            batch.set(firestore.collection("products").document("food-001"), java.util.Map.of("name", "Pack café premium", "category", "Despensa", "unitPrice", 24990, "productDiscountPercent", 0));
            batch.set(firestore.collection("products").document("home-001"), java.util.Map.of("name", "Set de sábanas king", "category", "Hogar", "unitPrice", 89990, "productDiscountPercent", 10));
            batch.set(firestore.collection("products").document("laptop-001"), java.util.Map.of("name", "Notebook 14 pulgadas", "category", "Tecnología", "unitPrice", 649990, "productDiscountPercent", 8));
            batch.set(firestore.collection("products").document("phone-001"), java.util.Map.of("name", "Smartphone 5G 256 GB", "category", "Tecnología", "unitPrice", 399990, "productDiscountPercent", 12));
            batch.set(firestore.collection("products").document("audio-001"), java.util.Map.of("name", "Audífonos inalámbricos", "category", "Tecnología", "unitPrice", 59990, "productDiscountPercent", 20));
            batch.set(firestore.collection("products").document("home-002"), java.util.Map.of("name", "Juego de toallas premium", "category", "Hogar", "unitPrice", 34990, "productDiscountPercent", 0));
            batch.set(firestore.collection("products").document("food-002"), java.util.Map.of("name", "Canasta desayuno familiar", "category", "Despensa", "unitPrice", 45990, "productDiscountPercent", 5));
            batch.set(firestore.collection("products").document("appliance-001"), java.util.Map.of("name", "Freidora de aire 5 litros", "category", "Electrodomésticos", "unitPrice", 79990, "productDiscountPercent", 18));
            batch.set(firestore.collection("products").document("appliance-002"), java.util.Map.of("name", "Cafetera programable", "category", "Electrodomésticos", "unitPrice", 49990, "productDiscountPercent", 10));
            batch.set(firestore.collection("products").document("sport-001"), java.util.Map.of("name", "Bicicleta urbana aro 29", "category", "Deportes", "unitPrice", 229990, "productDiscountPercent", 15));
            batch.set(firestore.collection("products").document("sport-002"), java.util.Map.of("name", "Set de mancuernas 20 kg", "category", "Deportes", "unitPrice", 69990, "productDiscountPercent", 0));
            batch.set(firestore.collection("promotions").document("CART_5_OVER_200K"), java.util.Map.of("name", "5% por compras sobre $200.000", "type", "CART_THRESHOLD", "minimumAmount", 200000, "percent", 5, "maxDiscount", 0, "priority", 200, "active", true));
            batch.set(firestore.collection("promotions").document("DEBIT_10"), java.util.Map.of("name", "10% de descuento pagando con débito", "type", "PAYMENT_METHOD", "paymentMethod", "DEBIT", "minimumAmount", 0, "percent", 10, "maxDiscount", 50000, "priority", 300, "active", true));
            batch.commit().get();
        };
    }
}
