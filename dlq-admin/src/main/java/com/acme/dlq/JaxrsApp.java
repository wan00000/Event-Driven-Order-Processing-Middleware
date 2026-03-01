// dlq-admin/src/main/java/com/acme/dlq/JaxrsApp.java
package com.acme.dlq;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/api")
public class JaxrsApp extends Application {}