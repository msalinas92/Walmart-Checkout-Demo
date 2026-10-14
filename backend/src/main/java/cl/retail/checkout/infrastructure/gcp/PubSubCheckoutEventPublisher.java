package cl.retail.checkout.infrastructure.gcp;

import cl.retail.checkout.application.CheckoutEventPublisher;
import cl.retail.checkout.domain.CheckoutEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

public final class PubSubCheckoutEventPublisher implements CheckoutEventPublisher {
    private final Publisher publisher;
    private final ObjectMapper objectMapper = eventObjectMapper();

    public PubSubCheckoutEventPublisher(Publisher publisher, String ignoredTopicId) { this.publisher = publisher; }

    static ObjectMapper eventObjectMapper() {
        // Aquí fuerzo RFC 3339 para que Pub/Sub pueda escribir el campo TIMESTAMP directamente en BigQuery.
        return new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void publish(CheckoutEvent event) {
        try {
            // Aquí publico un contrato versionado, para que los consumidores evolucionen sin acoplarse al checkout.
            String payload = objectMapper.writeValueAsString(event);
            PubsubMessage message = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(payload))
                    .putAttributes("eventType", event.eventType()).putAttributes("schemaVersion", String.valueOf(event.schemaVersion())).build();
            publisher.publish(message).get();
        } catch (Exception exception) { throw new IllegalStateException("No pude publicar el evento de checkout", exception); }
    }
}
