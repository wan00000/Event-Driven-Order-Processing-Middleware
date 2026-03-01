// dlq-admin/src/main/java/com/acme/dlq/store/DlqStore.java
package com.acme.dlq.store;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DlqStore {
    public record DlqItem(String id, String key, String payload, String reason) {}

    private final Deque<DlqItem> items = new ConcurrentLinkedDeque<>();
    private final int max = 500;

    public void add(String key, String payload, String reason) {
        items.addFirst(new DlqItem(UUID.randomUUID().toString(), key, payload, reason));
        while (items.size() > max) items.removeLast();
    }

    public List<DlqItem> list() {
        return new ArrayList<>(items);
    }

    public DlqItem get(String id) {
        return items.stream().filter(x -> x.id().equals(id)).findFirst().orElse(null);
    }
}