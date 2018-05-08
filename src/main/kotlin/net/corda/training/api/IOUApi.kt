package net.corda.training.api

import net.corda.core.contracts.Amount
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.x500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.NodeInfo
import net.corda.core.utilities.loggerFor
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.contracts.getCashBalances
import net.corda.training.flow.IOUIssueFlow
import net.corda.training.flow.IOUSettleFlow
import net.corda.training.flow.IOUTransferFlow
import net.corda.training.flow.SelfIssueCashFlow
import net.corda.training.state.IOUState
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import org.slf4j.Logger
import java.util.Currency
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * This API is accessible from /api/iou. The endpoint paths specified below are relative to it.
 * We've defined a bunch of endpoints to deal with IOUs, cash and the various operations you can perform with them.
 */
@Path("iou")
class IOUApi(val rpcOps: CordaRPCOps) {
    private val me = rpcOps.nodeInfo().legalIdentities.first().name
    private val myLegalName = me.x500Name

    companion object {
        private val logger: Logger = loggerFor<IOUApi>()
    }

    fun X500Name.toDisplayString() : String  = BCStyle.INSTANCE.toString(this)

    /** Helpers for filtering the network map cache. */
    private fun isNotary(nodeInfo: NodeInfo) = rpcOps.notaryIdentities().any { nodeInfo.isLegalIdentity(it) }
    private fun isMe(nodeInfo: NodeInfo) = nodeInfo.legalIdentities.first().name == me
    private fun isNetworkMap(nodeInfo : NodeInfo) = nodeInfo.legalIdentities.single().name.organisation == "Network Map Service"

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName.toDisplayString())

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<String>> {
        return mapOf("peers" to rpcOps.networkMapSnapshot()
                .filter { isNotary(it).not() && isMe(it).not() && isNetworkMap(it).not() }
                .map { it.legalIdentities.first().name.x500Name.toDisplayString() })
    }

    /**
     * Task 1
     * Displays all IOU states that exist in the node's vault.
     * TODO: Return a list of IOUStates on ledger
     * Hint - Use [rpcOps] to query the vault all unconsumed [IOUState]s
     */
    @GET
    @Path("ious")
    @Produces(MediaType.APPLICATION_JSON)
    fun getIOUs(): List<StateAndRef<ContractState>> {
        // Filter by state type: IOU.
        return listOf()
    }

    /**
     * Displays all cash states that exist in the node's vault.
     */
    @GET
    @Path("cash")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCash(): List<StateAndRef<ContractState>> {
        // Filter by state type: Cash.
        return rpcOps.vaultQueryBy<Cash.State>().states
    }

    /**
     * Displays all cash states that exist in the node's vault.
     */
    @GET
    @Path("cash-balances")
    @Produces(MediaType.APPLICATION_JSON)
            // Display cash balances.
    fun getCashBalances() = rpcOps.getCashBalances()

    /**
     * Initiates a flow to agree an IOU between two parties.
     */
//    @PUT
//    @Path("issue-iou")
//    fun issueIOU(@QueryParam(value = "amount") amount: Int,
//                 @QueryParam(value = "currency") currency: String,
//                 @QueryParam(value = "party") party: String): Response {
//        // Get party objects for myself and the counterparty.
//        val me = rpcOps.nodeInfo().legalIdentities.first()
//        val lender = rpcOps.wellKnownPartyFromX500Name(CordaX500Name.parse(party)) ?: throw IllegalArgumentException("Unknown party name.")
//        // Create a new IOU state using the parameters given.
//        try {
//            val state = IOUState(Amount(amount.toLong() * 100, Currency.getInstance(currency)), lender, me)
//            // Start the IOUIssueFlow. We block and waits for the flow to return.
//            val result = rpcOps.startTrackedFlow(::IOUIssueFlow, state).returnValue.get()
//            // Return the response.
//            return Response
//                    .status(Response.Status.CREATED)
//                    .entity("Transaction id ${result.id} committed to ledger.\n${result.tx.outputs.single()}")
//                    .build()
//            // For the purposes of this demo app, we do not differentiate by exception type.
//        } catch (e: Exception) {
//            return Response
//                    .status(Response.Status.BAD_REQUEST)
//                    .entity(e.message)
//                    .build()
//        }
//    }

    /**
     * Transfers an IOU specified by [linearId] to a new party.
     */
    @PUT
    @Path("transfer-iou")
    fun transferIOU(@QueryParam(value = "id") id: String,
                    @QueryParam(value = "party") party: String): Response {
        val linearId = UniqueIdentifier.fromString(id)
        val newLender = rpcOps.wellKnownPartyFromX500Name(CordaX500Name.parse(party)) ?: throw IllegalArgumentException("Unknown party name.")
        try {
            rpcOps.startFlow(::IOUTransferFlow, linearId, newLender).returnValue.get()
            return Response.status(Response.Status.CREATED).entity("IOU $id transferred to $party.").build()

        } catch (e: Exception) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(e.message)
                    .build()
        }
    }

    /**
     * Settles an IOU. Requires cash in the right currency to be able to settle.
     */
    @PUT
    @Path("settle-iou")
    fun settleIOU(@QueryParam(value = "id") id: String,
                  @QueryParam(value = "amount") amount: Int,
                  @QueryParam(value = "currency") currency: String): Response {
        val linearId = UniqueIdentifier.fromString(id)
        val settleAmount = Amount(amount.toLong() * 100, Currency.getInstance(currency))

        try {
            rpcOps.startFlow(::IOUSettleFlow, linearId, settleAmount).returnValue.get()
            return Response.status(Response.Status.CREATED).entity("$amount $currency paid off on IOU id $id.").build()

        } catch (e: Exception) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(e.message)
                    .build()
        }
    }

    /**
     * Helper end-point to issue some cash to ourselves.
     */
    @PUT
    @Path("self-issue-cash")
    fun selfIssueCash(@QueryParam(value = "amount") amount: Int,
                      @QueryParam(value = "currency") currency: String): Response {
        val issueAmount = Amount(amount.toLong() * 100, Currency.getInstance(currency))

        try {
            val cashState = rpcOps.startFlow(::SelfIssueCashFlow, issueAmount).returnValue.get()
            return Response.status(Response.Status.CREATED).entity(cashState.toString()).build()

        } catch (e: Exception) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(e.message)
                    .build()
        }
    }
}