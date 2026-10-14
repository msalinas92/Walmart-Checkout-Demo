package cl.retail.checkout.infrastructure.gcp;

import cl.retail.checkout.domain.CheckoutEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PubSubCheckoutEventPublisherTest {
    @Test
    void serializesTheBigQueryPartitionTimestampAsRfc3339() throws Exception {
        CheckoutEvent event = new CheckoutEvent("evt-1", "checkout.transaction.v1",
                Instant.parse("2026-10-13T04:30:00Z"), "ord-1",
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.valueOf(7), "DEBIT", List.of("DEBIT_10"), 1);

        String payload = PubSubCheckoutEventPublisher.eventObjectMapper().writeValueAsString(event);

        assertThat(payload).contains("\"occurredAt\":\"2026-10-13T04:30:00Z\"");
    }
}
