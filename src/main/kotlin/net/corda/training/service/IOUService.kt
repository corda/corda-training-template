package net.corda.training.service

import net.corda.core.node.PluginServiceHub
import net.corda.training.flow.SignTransactionFlow
import net.corda.training.flow.SignTransactionFlowResponder

object IOUService {
    class Service(services: PluginServiceHub) {
        init {
            services.registerFlowInitiator(SignTransactionFlow.Initiator::class.java) {
                SignTransactionFlowResponder(it)
            }
        }
    }
}