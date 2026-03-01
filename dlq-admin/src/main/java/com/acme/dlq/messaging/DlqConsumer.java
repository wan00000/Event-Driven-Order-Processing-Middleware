package com.acme.dlq.messaging;

import com.acme.dlq.store.DlqStore;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class DlqConsumer {

    @Inject
    DlqStore store;

    @Incoming("ordersDlqIn")
    public CompletionStage<Void> onDlq(Message<String> msg) {
        String payload = msg.getPayload();

        String key = "";
        String reason = "";

        try {
            ConsumerRecord<?, ?> record = msg.unwrap(ConsumerRecord.class);

            Object k = record.key();
            if (k != null) {
                key = String.valueOf(k);
            }

            Header h = record.headers().lastHeader("dead-letter-reason");
            if (h != null) {
                reason = new String(h.value(), StandardCharsets.UTF_8);
            }
        } catch (IllegalArgumentException ignored) {
            // Connector didn't support unwrap to ConsumerRecord; keep key/reason empty
        }

        store.add(key, payload, reason);
        return msg.ack();
    }
}