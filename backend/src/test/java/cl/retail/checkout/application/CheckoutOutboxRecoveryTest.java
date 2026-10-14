package cl.retail.checkout.application;

import cl.retail.checkout.domain.CheckoutEvent;
import cl.retail.checkout.domain.CheckoutOrder;
import cl.retail.checkout.domain.PaymentMethod;
import cl.retail.checkout.domain.Product;
import cl.retail.checkout.domain.Quote;
import cl.retail.checkout.infrastructure.testsupport.InMemoryCheckoutRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutOutboxRecoveryTest {
    @Test
    void republishesAPendingEventWithTheSameIdentityAfterAFailure() {
        InMemoryCheckoutRepository repository = new InMemoryCheckoutRepository(List.of(
                new Product("tv-001", "Smart TV", "Tecnología", BigDecimal.valueOf(299990), BigDecimal.valueOf(15))));
        FailOncePublisher publisher = new FailOncePublisher();
        CheckoutService service = new CheckoutService(repository, publisher);
        Quote quote = service.quote(new QuoteCommand(List.of(new QuoteCommand.Item("tv-001", 1)), PaymentMethod.DEBIT));

        assertThatThrownBy(() -> service.confirm(new ConfirmCommand(quote.id()), "recoverable-key"))
                .isInstanceOf(IllegalStateException.class);

        CheckoutOrder recovered = service.confirm(new ConfirmCommand(quote.id()), "recoverable-key");
        CheckoutOrder retry = service.confirm(new ConfirmCommand(quote.id()), "recoverable-key");

        assertThat(retry.id()).isEqualTo(recovered.id());
        assertThat(publisher.attempts).isEqualTo(2);
        assertThat(publisher.published).singleElement()
                .extracting(CheckoutEvent::eventId).isEqualTo("evt_" + recovered.id().substring(4));
    }

    private static final class FailOncePublisher implements CheckoutEventPublisher {
        private int attempts;
        private final List<CheckoutEvent> published = new ArrayList<>();

        @Override
        public void publish(CheckoutEvent event) {
            attempts++;
            if (attempts == 1) throw new IllegalStateException("fallo transitorio simulado");
            published.add(event);
        }
    }
}
