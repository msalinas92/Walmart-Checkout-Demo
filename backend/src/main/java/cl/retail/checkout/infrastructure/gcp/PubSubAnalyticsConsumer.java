package cl.retail.checkout.infrastructure.gcp;

import cl.retail.checkout.domain.CheckoutEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.pubsub.v1.ProjectSubscriptionName;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class PubSubAnalyticsConsumer {
    private final Queue<CheckoutEvent> pending = new ConcurrentLinkedQueue<>();
    private final BigQueryBatchWriter writer;
    private final GcpProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private Subscriber subscriber;

    public PubSubAnalyticsConsumer(BigQueryBatchWriter writer, GcpProperties properties) {
        this.writer = writer; this.properties = properties;
    }

    @PostConstruct
    void start() throws Exception {
        ProjectSubscriptionName subscription = ProjectSubscriptionName.of(properties.projectId(), properties.subscriptionId());
        MessageReceiver receiver = (message, consumer) -> {
            try {
                pending.add(objectMapper.readValue(message.getData().toStringUtf8(), CheckoutEvent.class));
                consumer.ack();
            } catch (Exception exception) {
                consumer.nack();
            }
        };
        subscriber = Subscriber.newBuilder(subscription, receiver)
                .setCredentialsProvider(FixedCredentialsProvider.create(GoogleCredentials.getApplicationDefault())).build();
        subscriber.startAsync().awaitRunning();
    }

    // Aquí agrupo los mensajes pendientes para reducir operaciones y preservar la carga batch de BigQuery.
    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "${checkout.analytics.flush-ms:10000}")
    void flush() {
        List<CheckoutEvent> batch = new ArrayList<>();
        while (batch.size() < 100) {
            CheckoutEvent event = pending.poll();
            if (event == null) break;
            batch.add(event);
        }
        writer.append(batch);
    }

    @PreDestroy
    void stop() { if (subscriber != null) subscriber.stopAsync(); }
}
