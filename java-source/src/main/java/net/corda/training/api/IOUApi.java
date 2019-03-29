package net.corda.training.api;

import net.corda.core.contracts.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.internal.InternalUtils;
import net.corda.core.internal.FetchDataFlow.Result;
import net.corda.finance.contracts.asset.Cash;
import net.corda.finance.flows.CashIssueFlow;

import net.corda.finance.workflows.GetBalances;
import net.corda.training.flow.IOUIssueFlow;
import net.corda.training.flow.IOUSettleFlow;
import net.corda.training.flow.IOUTransferFlow;
import net.corda.training.flow.SelfIssueCashFlow;
import net.corda.training.state.IOUState;

import java.util.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This API is accessible from /api/iou. The endpoint paths specified below are relative to it.
 * We've defined a bunch of endpoints to deal with IOUs, cash and the various operations you can perform with them.
 */
@Path("iou")
public class IOUApi {
    private final CordaRPCOps rpcOps;
    private final CordaX500Name me;

    private static final Logger logger = LoggerFactory.getLogger(IOUApi.class);

    public IOUApi(CordaRPCOps rpcOps) {
        this.rpcOps = rpcOps;
        this.me = rpcOps.nodeInfo().getLegalIdentities().get(0).getName();
    }

    /** Helpers for filtering the network map cache. */
    public String toDisplayString(X500Name name){
        return BCStyle.INSTANCE.toString(name);
    }

    private boolean isNotary(NodeInfo nodeInfo) {
        return !rpcOps.notaryIdentities()
                .stream().filter(el -> nodeInfo.isLegalIdentity(el))
                .collect(Collectors.toList()).isEmpty();
    }

    private boolean isMe(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().equals(me);
    }

    private boolean isNetworkMap(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().getOrganisation().equals("Network Map Service");
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public HashMap<String, String> whoami() {
        HashMap<String, String> myMap = new HashMap<>();
        myMap.put("me", me.toString());
        return myMap;
    }

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public HashMap<String, List<String>> getPeers() {
        HashMap<String, List<String>> myMap = new HashMap<>();

        // Find all nodes that are not notaries, ourself, or the network map.
        Stream<NodeInfo> filteredNodes = rpcOps.networkMapSnapshot().stream()
                .filter(el -> !isNotary(el) && !isMe(el) && !isNetworkMap(el));
        // Get their names as strings
        List<String> nodeNames = filteredNodes.map(el -> el.getLegalIdentities().get(0).getName().toString())
                .collect(Collectors.toList());

        myMap.put("peers", nodeNames);
        return myMap;
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
    public List<StateAndRef<IOUState>> getIOUs() {
        // Filter by state type: IOU.
        return rpcOps.vaultQuery(IOUState.class).getStates();
    }

    /**
     * Displays all cash states that exist in the node's vault.
     */
    @GET
    @Path("cash")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<Cash.State>> getCash() {
        // Filter by state type: Cash.
        return rpcOps.vaultQuery(Cash.State.class).getStates();
    }

    /**
     * Displays all cash states that exist in the node's vault.
     */
    @GET
    @Path("cash-balances")
    @Produces(MediaType.APPLICATION_JSON)
    // Display cash balances.
    public Map<Currency,Amount<Currency>> getCashBalances(){
        return GetBalances.getCashBalances(rpcOps);
    }

    /**
     * Initiates a flow to agree an IOU between two parties.
     * Example request:
     * curl -X PUT 'http://localhost:10007/api/iou/issue-iou?amount=99&currency=GBP&party=O=ParticipantC,L=New%20York,C=US
     */
//    @PUT
//    @Path("issue-iou")
//    public Response issueIOU(@QueryParam(value = "amount") int amount,
//                             @QueryParam(value = "currency") String currency,
//                             @QueryParam(value = "party") String party) throws IllegalArgumentException {
//        // Get party objects for myself and the counterparty.
//        Party me = rpcOps.nodeInfo().getLegalIdentities().get(0);
//        Party lender = Optional.ofNullable(rpcOps.wellKnownPartyFromX500Name(CordaX500Name.parse(party))).orElseThrow(() -> new IllegalArgumentException("Unknown party name."));
//        // Create a new IOU state using the parameters given.
//        try {
//            IOUState state = new IOUState(new Amount<>((long)amount * 100, Currency.getInstance(currency)), lender, me);
//            // Start the IOUIssueFlow. We block and waits for the flow to return.
//            SignedTransaction result = rpcOps.startTrackedFlowDynamic(IOUIssueFlow.InitiatorFlow.class, state).getReturnValue().get();
//            // Return the response.
//            return Response
//                    .status(Response.Status.CREATED)
//                    .entity(String.format("Transaction id %h committed to ledger.\n%h", result.getId(), result.getTx().getOutputs().get(0)))
//                    .build();
//            // For the purposes of this demo app, we do not differentiate by exception type.
//        } catch (Exception e) {
//            return Response
//                    .status(Response.Status.BAD_REQUEST)
//                    .entity(e.getMessage())
//                    .build();
//        }
//    }

    /**
     * Transfers an IOU specified by [linearId] to a new party.
     */
    @GET
    @Path("transfer-iou")
    public Response transferIOU(@QueryParam(value = "id") String id,
                                @QueryParam(value = "party") String party) {
        return Response
                .status(Response.Status.NOT_IMPLEMENTED)
                .entity("Not implemented")
                .build();
    }

    /**
     * Settles an IOU. Requires cash in the right currency to be able to settle.
     */
    @GET
    @Path("settle-iou")
    public Response settleIOU(@QueryParam(value = "id") String id,
                              @QueryParam(value = "amount") int amount,
                              @QueryParam(value = "currency") String currency) {
        return Response
                .status(Response.Status.NOT_IMPLEMENTED)
                .entity("Not implemented")
                .build();
    }

    /**
     * Helper end-point to issue some cash to ourselves.
     */
    @GET
    @Path("self-issue-cash")
    public Response selfIssueCash(@QueryParam(value = "amount") int amount,
                                  @QueryParam(value = "currency") String currency) {
        return Response
                .status(Response.Status.NOT_IMPLEMENTED)
                .entity("Not implemented")
                .build();
    }
}