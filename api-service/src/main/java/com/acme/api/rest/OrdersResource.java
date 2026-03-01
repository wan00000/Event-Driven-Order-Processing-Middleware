// api-service/src/main/java/com/acme/api/rest/OrdersResource.java
package com.acme.api.rest;

import com.acme.common.model.OrderCreated;
import com.acme.common.model.OrderRequest;
import com.acme.common.util.JsonUtil;
import io.smallrye.reactive.messaging.kafka.api.KafkaMetadataUtil;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Path("/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrdersResource {

    @Inject
    @Channel("ordersCreated")
    Emitter<String> emitter;

    @POST
    public Response create(OrderRequest req,
                           @HeaderParam("X-Correlation-Id") String correlationIdHeader) {

        if (req == null || req.orderId() == null || req.orderId().isBlank()) {
            return Response.status(400).entity("{\"error\":\"orderId is required\"}").build();
        }

        String correlationId = (correlationIdHeader == null || correlationIdHeader.isBlank())
                ? UUID.randomUUID().toString()
                : correlationIdHeader;

        OrderCreated event = new OrderCreated(
                req.orderId(),
                req.customer(),
                req.amount(),
                req.forceFail(),
                correlationId,
                Instant.now().toEpochMilli(),
                0
        );

        String payload = JsonUtil.toJson(event);

        // Add Kafka key + headers (correlation id + event type)
        Headers headers = new RecordHeaders();
        headers.add("x-correlation-id", correlationId.getBytes(StandardCharsets.UTF_8));
        headers.add("x-event-type", "OrderCreated".getBytes(StandardCharsets.UTF_8));

        OutgoingKafkaRecordMetadata<String> meta = OutgoingKafkaRecordMetadata.<String>builder()
                .withKey(req.orderId())
                .withHeaders(headers)
                .build();

        Message<String> outMsg =
                KafkaMetadataUtil.writeOutgoingKafkaMetadata(Message.of(payload), meta);

        emitter.send(outMsg);

        return Response.accepted()
                .entity("{\"ok\":true,\"orderId\":\"" + req.orderId() + "\",\"correlationId\":\"" + correlationId + "\"}")
                .build();
    }
}