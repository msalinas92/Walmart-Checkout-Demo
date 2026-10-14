package cl.retail.checkout.application;

import cl.retail.checkout.TestCheckoutConfiguration;
import cl.retail.checkout.domain.CheckoutOrder;
import cl.retail.checkout.domain.PaymentMethod;
import cl.retail.checkout.domain.Quote;
import cl.retail.checkout.infrastructure.testsupport.CollectingEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestCheckoutConfiguration.class)
class CheckoutServiceIntegrationTest {
    @Autowired CheckoutService service;
    @Autowired CollectingEventPublisher events;

    @Test
    void confirmsOnlyOnceForTheSameIdempotencyKeyAndPublishesOneEvent() {
        Quote quote = service.quote(new QuoteCommand(List.of(new QuoteCommand.Item("tv-001", 1)), PaymentMethod.DEBIT));
        CheckoutOrder first = service.confirm(new ConfirmCommand(quote.id()), "test-key-1");
        CheckoutOrder retry = service.confirm(new ConfirmCommand(quote.id()), "test-key-1");
        assertThat(retry.id()).isEqualTo(first.id());
        assertThat(events.events()).hasSize(1);
        assertThat(events.events().get(0).orderId()).isEqualTo(first.id());
    }
}
