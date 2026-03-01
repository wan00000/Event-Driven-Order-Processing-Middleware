// api-service/src/main/java/com/acme/api/JaxrsApp.java
package com.acme.api;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/api")
public class JaxrsApp extends Application {}