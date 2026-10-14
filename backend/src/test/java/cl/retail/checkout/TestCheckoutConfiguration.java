package cl.retail.checkout;

import cl.retail.checkout.application.*;
import cl.retail.checkout.domain.*;
import cl.retail.checkout.infrastructure.testsupport.CollectingEventPublisher;
import cl.retail.checkout.infrastructure.testsupport.InMemoryCheckoutRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import java.math.BigDecimal;
import java.util.List;

@TestConfiguration
public class TestCheckoutConfiguration {
    @Bean CheckoutRepository repository() {
        return new InMemoryCheckoutRepository(List.of(
                new Product("tv-001", "Smart TV 55 pulgadas", "Tecnología", BigDecimal.valueOf(299990), BigDecimal.valueOf(15)),
                new Product("food-001", "Pack café premium", "Despensa", BigDecimal.valueOf(24990), BigDecimal.ZERO),
                new Product("home-001", "Set de sábanas king", "Hogar", BigDecimal.valueOf(89990), BigDecimal.valueOf(10))));
    }
    @Bean @Primary CollectingEventPublisher collectingEventPublisher() { return new CollectingEventPublisher(); }
    @Bean CheckoutService checkoutService(CheckoutRepository repository, CheckoutEventPublisher publisher) { return new CheckoutService(repository, publisher); }
}
