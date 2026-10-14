package cl.retail.checkout.infrastructure.gcp;

import com.google.cloud.bigquery.BigQuery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@Profile("gcp & !test")
@EnableScheduling
@ConditionalOnProperty(prefix = "checkout.analytics", name = "enabled", havingValue = "true")
public class AnalyticsConfiguration {
    @Bean BigQueryBatchWriter bigQueryBatchWriter(BigQuery bigQuery, GcpProperties properties) { return new BigQueryBatchWriter(bigQuery, properties); }
    @Bean PubSubAnalyticsConsumer pubSubAnalyticsConsumer(BigQueryBatchWriter writer, GcpProperties properties) { return new PubSubAnalyticsConsumer(writer, properties); }
}
