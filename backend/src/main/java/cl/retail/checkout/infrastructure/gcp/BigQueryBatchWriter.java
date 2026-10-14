package cl.retail.checkout.infrastructure.gcp;

import cl.retail.checkout.domain.CheckoutEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.bigquery.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class BigQueryBatchWriter {
    private final BigQuery bigQuery;
    private final TableId tableId;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public BigQueryBatchWriter(BigQuery bigQuery, GcpProperties properties) {
        this.bigQuery = bigQuery;
        this.tableId = TableId.of(properties.projectId(), properties.bigQueryDataset(), properties.bigQueryTable());
    }

    public void append(List<CheckoutEvent> events) {
        if (events.isEmpty()) return;
        try {
            StringBuilder jsonLines = new StringBuilder();
            for (CheckoutEvent event : events) jsonLines.append(objectMapper.writeValueAsString(event)).append('\n');
            WriteChannelConfiguration configuration = WriteChannelConfiguration.newBuilder(tableId)
                    .setFormatOptions(FormatOptions.json()).setWriteDisposition(JobInfo.WriteDisposition.WRITE_APPEND).build();
            // Aquí uso un load job batch, compatible con BigQuery Sandbox y sin inserciones de streaming.
            try (TableDataWriteChannel writer = bigQuery.writer(configuration)) {
                writer.write(ByteBuffer.wrap(jsonLines.toString().getBytes(StandardCharsets.UTF_8)));
            }
        } catch (Exception exception) { throw new IllegalStateException("No pude cargar el lote analítico en BigQuery", exception); }
    }
}
