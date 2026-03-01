// dlq-admin/src/main/java/com/acme/dlq/rest/DlqResource.java
package com.acme.dlq.rest;

import com.acme.dlq.store.DlqStore;
import com.acme.dlq.store.DlqStore.DlqItem;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/dlq")
@Produces(MediaType.APPLICATION_JSON)
public class DlqResource {

    @Inject
    DlqStore store;

    @GET
    public List<DlqItem> list() {
        return store.list();
    }

    @GET
    @Path("/{id}")
    public DlqItem get(@PathParam("id") String id) {
        DlqItem item = store.get(id);
        if (item == null) throw new NotFoundException();
        return item;
    }
}