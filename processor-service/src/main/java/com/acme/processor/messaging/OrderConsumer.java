// processor-service/src/main/java/com/acme/processor/messaging/OrderConsumer.java
package com.acme.processor.messaging;

import com.acme.common.model.OrderCreated;
import com.acme.common.model.OrderProcessed;
import com.acme.common.util.JsonUtil;
import com.acme.processor.service.OrderProcessor;
import io.smallrye.reactive.messaging.kafka.api.KafkaMetadataUtil;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class OrderConsumer {

    @Inject
    OrderProcessor processor;

    @Inject
    @Channel("ordersProcessed")
    Emitter<String> processedEmitter;

    @Incoming("ordersCreatedIn")
    public CompletionStage<Void> onMessage(Message<String> msg) {
        try {
            OrderCreated order = JsonUtil.fromJson(msg.getPayload(), OrderCreated.class);

            // Stage 1 retry (via MP Fault Tolerance in OrderProcessor)
            processor.process(order);

            OrderProcessed out = new OrderProcessed(
                    order.orderId(),
                    "PROCESSED",
                    order.correlationId(),
                    Instant.now().toEpochMilli()
            );

            String outJson = JsonUtil.toJson(out);

            // Produce with Kafka key + headers (optional but useful)
            Headers headers = new RecordHeaders();
            headers.add("x-correlation-id", order.correlationId().getBytes(StandardCharsets.UTF_8));
            headers.add("x-event-type", "OrderProcessed".getBytes(StandardCharsets.UTF_8));

            OutgoingKafkaRecordMetadata<String> meta = OutgoingKafkaRecordMetadata.<String>builder()
                    .withKey(order.orderId())
                    .withHeaders(headers) // expects org.apache.kafka.common.header.Headers
                    .build();

            Message<String> outMsg =
                    KafkaMetadataUtil.writeOutgoingKafkaMetadata(Message.of(outJson), meta);

            processedEmitter.send(outMsg);

            return msg.ack();
        } catch (Exception e) {
            // This will trigger your configured failure-strategy (DLQ) for the incoming channel
            return msg.nack(e);
        }
    }
}