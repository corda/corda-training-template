package net.corda.training.plugin;

import net.corda.core.messaging.CordaRPCOps;
import net.corda.webserver.services.WebServerPluginRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.function.Function;
import java.util.*;

import net.corda.training.api.IOUApi;

public class IOUPlugin implements WebServerPluginRegistry {
    /**
     * A list of classes that expose web APIs.
     */
    private final List<Function<CordaRPCOps, ?>> webApis = ImmutableList.of(IOUApi::new);

    /**
     * A list of directories in the resources directory that will be served by Jetty under /web.
     */
    private final Map<String, String> staticServeDirs = ImmutableMap.of(
            // This will serve the exampleWeb directory in resources to /web/example
            "iou", getClass().getClassLoader().getResource("iouWeb").toExternalForm()
    );

    @Override
    public List<Function<CordaRPCOps, ?>> getWebApis() {
        return webApis;
    }

    @Override
    public Map<String, String> getStaticServeDirs() {
        return staticServeDirs;
    }

    @Override
    public void customizeJSONSerialization(ObjectMapper objectMapper) { 

     }
}