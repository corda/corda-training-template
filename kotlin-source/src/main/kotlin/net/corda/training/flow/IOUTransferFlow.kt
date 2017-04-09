package net.corda.training.flow

import net.corda.core.contracts.StateRef
import net.corda.core.crypto.Party
import net.corda.core.flows.FlowLogic

/**
 *
 */
class IOUTransferFlow(iouToTransfer: StateRef, otherParty: Party): FlowLogic<Unit>() {
    override fun call(): Unit {

    }
}
