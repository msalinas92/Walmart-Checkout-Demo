package cl.retail.checkout.infrastructure.gcp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "checkout.gcp")
public record GcpProperties(String projectId, String topicId, String subscriptionId, String bigQueryDataset, String bigQueryTable) { }
