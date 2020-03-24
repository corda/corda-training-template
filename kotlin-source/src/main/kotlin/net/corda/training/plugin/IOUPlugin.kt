package net.corda.training.plugin

import net.corda.core.messaging.CordaRPCOps
import net.corda.training.api.IOUApi
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function

class IOUPlugin : WebServerPluginRegistry {
    /**
     * A list of classes that expose web APIs.
     */
    override val webApis: List<Function<CordaRPCOps, out Any>> = listOf(Function(::IOUApi))

    /**
     * A list of directories in the resources directory that will be served by Jetty under /web.
     * The template's web frontend is accessible at /web/template.
     */
    override val staticServeDirs: Map<String, String> = mapOf(
            // This will serve the iouWeb directory in resources to /web/template
            "iou" to javaClass.classLoader.getResource("iouWeb").toExternalForm()
    )
}