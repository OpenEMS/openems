package io.openems.backend.b2bwebsocket;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.openems.common.jsonrpc.request.*;
import io.openems.common.jsonrpc.response.EdgeRpcResponse;
import io.openems.common.websocket.SubscribedChannelsWorker;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.backend.b2bwebsocket.jsonrpc.request.SubscribeEdgesChannelsRequest;
import io.openems.backend.common.jsonrpc.request.GetEdgesChannelsValuesRequest;
import io.openems.backend.common.jsonrpc.request.GetEdgesStatusRequest;
import io.openems.backend.common.jsonrpc.response.GetEdgesChannelsValuesResponse;
import io.openems.backend.common.jsonrpc.response.GetEdgesStatusResponse;
import io.openems.backend.common.jsonrpc.response.GetEdgesStatusResponse.EdgeInfo;
import io.openems.backend.metadata.api.BackendUser;
import io.openems.backend.metadata.api.Edge;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.session.Role;
import io.openems.common.session.User;
import io.openems.common.types.ChannelAddress;

public class OnRequest implements io.openems.common.websocket.OnRequest {

    private final Logger log = LoggerFactory.getLogger(OnRequest.class);
    private final B2bWebsocket parent;

    public OnRequest(B2bWebsocket parent) {
        this.parent = parent;
    }

    @Override
    public CompletableFuture<? extends JsonrpcResponseSuccess> run(WebSocket ws, JsonrpcRequest request)
            throws OpenemsException, OpenemsNamedException {
        WsData wsData = ws.getAttachment();
		BackendUser user = wsData.getUserWithTimeout(5, TimeUnit.SECONDS);

        switch (request.getMethod()) {

            case GetEdgesStatusRequest.METHOD:
                return this.handleGetStatusOfEdgesRequest(user, request.getId(), GetEdgesStatusRequest.from(request));

            case GetEdgesChannelsValuesRequest.METHOD:
                return this.handleGetChannelsValuesRequest(user, request.getId(),
                        GetEdgesChannelsValuesRequest.from(request));

            case SubscribeEdgesChannelsRequest.METHOD:
                return this.handleSubscribeEdgesChannelsRequest(wsData, user, request.getId(),
                        SubscribeEdgesChannelsRequest.from(request));

            case EdgeRpcRequest.METHOD:
                return this.handleEdgeRpcRequest(wsData, user, EdgeRpcRequest.from(request));

            case SetGridConnScheduleRequest.METHOD:
                return this.handleSetGridConnScheduleRequest(user, request.getId(),
                        SetGridConnScheduleRequest.from(request));

            default:
                this.parent.logWarn(this.log, "Unhandled Request: " + request);
                throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
        }
    }

    private CompletableFuture<? extends JsonrpcResponseSuccess> handleEdgeRpcRequest(WsData wsData, BackendUser backendUser, EdgeRpcRequest edgeRpcRequest) throws OpenemsNamedException {
        String edgeId = edgeRpcRequest.getEdgeId();
        JsonrpcRequest request = edgeRpcRequest.getPayload();
        User user = backendUser.getAsCommonUser(edgeId);
        user.assertRoleIsAtLeast(EdgeRpcRequest.METHOD, Role.GUEST);

        CompletableFuture<JsonrpcResponseSuccess> resultFuture;
        switch (request.getMethod()) {

            case SubscribeChannelsRequest.METHOD:
                resultFuture = this.handleSubscribeChannelsRequest(wsData, edgeId, user,
                        SubscribeChannelsRequest.from(request));
                break;

            default:
                this.parent.logWarn(this.log, "Unhandled EdgeRpcRequest: " + request);
                throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
        }

        // Wrap reply in EdgeRpcResponse
        CompletableFuture<EdgeRpcResponse> result = new CompletableFuture<EdgeRpcResponse>();
        resultFuture.thenAccept(r -> {
            result.complete(new EdgeRpcResponse(edgeRpcRequest.getId(), r));
        });
        return result;
    }

    private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeChannelsRequest(WsData wsData, String edgeId, User user, SubscribeChannelsRequest request) {
        TreeSet<ChannelAddress> permittedChannels = new TreeSet<>();
        permittedChannels.addAll(request.getChannels());

        // activate SubscribedChannelsWorker
        SubscribedChannelsWorker worker = wsData.getSubscribedChannelsWorker();
        worker.clearAll();
        worker.handleSubscribeChannelsRequest(request.getCount(), permittedChannels, edgeId);

        // JSON-RPC response
        return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
    }

    /**
     * Handles a GetStatusOfEdgesRequest.
     *
     * @param user      the User
     * @param messageId the JSON-RPC Message-ID
     * @param request   the GetStatusOfEdgesRequest
     * @return the JSON-RPC Success Response Future
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<GetEdgesStatusResponse> handleGetStatusOfEdgesRequest(BackendUser user, UUID messageId,
                                                                                    GetEdgesStatusRequest request) throws OpenemsNamedException {
        Map<String, EdgeInfo> result = new HashMap<>();
        for (Entry<String, Role> entry : user.getEdgeRoles().entrySet()) {
            String edgeId = entry.getKey();

            // assure read permissions of this User for this Edge.
            if (!user.edgeRoleIsAtLeast(edgeId, Role.GUEST)) {
                continue;
            }

            Optional<Edge> edgeOpt = this.parent.metadata.getEdge(edgeId);
            if (edgeOpt.isPresent()) {
                Edge edge = edgeOpt.get();
                EdgeInfo info = new EdgeInfo(edge.isOnline());
                result.put(edge.getId(), info);
            }
        }
        return CompletableFuture.completedFuture(new GetEdgesStatusResponse(messageId, result));
    }

    /**
     * Handles a GetChannelsValuesRequest.
     *
     * @param user      the User
     * @param messageId the JSON-RPC Message-ID
     * @param request   the GetChannelsValuesRequest
     * @return the JSON-RPC Success Response Future
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<GetEdgesChannelsValuesResponse> handleGetChannelsValuesRequest(BackendUser user,
                                                                                             UUID messageId, GetEdgesChannelsValuesRequest request) throws OpenemsNamedException {
        GetEdgesChannelsValuesResponse response = new GetEdgesChannelsValuesResponse(messageId);
        for (String edgeId : request.getEdgeIds()) {
            // assure read permissions of this User for this Edge.
            if (!user.edgeRoleIsAtLeast(edgeId, Role.GUEST)) {
                continue;
            }

            for (ChannelAddress channel : request.getChannels()) {
                Optional<JsonElement> value = this.parent.timeData.getChannelValue(edgeId, channel);
                response.addValue(edgeId, channel, value.orElse(JsonNull.INSTANCE));
            }
        }
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Handles a SubscribeEdgesChannelsRequest.
     *
     * @param wsData    the WebSocket attachment
     * @param user      the User
     * @param messageId the JSON-RPC Message-ID
     * @param request   the SubscribeEdgesChannelsRequest
     * @return the JSON-RPC Success Response Future
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<GenericJsonrpcResponseSuccess> handleSubscribeEdgesChannelsRequest(WsData wsData,
                                                                                                 BackendUser user, UUID messageId, SubscribeEdgesChannelsRequest request) throws OpenemsNamedException {
        for (String edgeId : request.getEdgeIds()) {
            // assure read permissions of this User for this Edge.
            user.assertEdgeRoleIsAtLeast(SubscribeEdgesChannelsRequest.METHOD, edgeId, Role.GUEST);
        }

        SubscribedChannelsWorker worker = wsData.getSubscribedChannelsWorker();
        worker.clearAll();
        request.getEdgeIds().forEach(edgeId -> {
            worker.handleSubscribeChannelsRequest(request.getCount(), request.getChannels(), edgeId);
        });

        // JSON-RPC response
        return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
    }

    /**
     * Handles a SetGridConnScheduleRequest.
     *
     * @param backendUser                the User
     * @param messageId                  the JSON-RPC Message-ID
     * @param setGridConnScheduleRequest the SetGridConnScheduleRequest
     * @return the JSON-RPC Success Response Future
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<GenericJsonrpcResponseSuccess> handleSetGridConnScheduleRequest(BackendUser backendUser,
                                                                                              UUID messageId, SetGridConnScheduleRequest setGridConnScheduleRequest) throws OpenemsNamedException {
        String edgeId = setGridConnScheduleRequest.getEdgeId();
        User user = backendUser.getAsCommonUser(edgeId);
        user.assertRoleIsAtLeast(SetGridConnScheduleRequest.METHOD, Role.ADMIN);

        // wrap original request inside ComponentJsonApiRequest
        String componentId = "ctrlBalancingSchedule0"; // TODO find dynamic Component-ID of BalancingScheduleController
        ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, setGridConnScheduleRequest);

        CompletableFuture<JsonrpcResponseSuccess> resultFuture = this.parent.edgeWebsocket.send(edgeId, user, request);

        // Wrap reply in GenericJsonrpcResponseSuccess
        CompletableFuture<GenericJsonrpcResponseSuccess> result = new CompletableFuture<GenericJsonrpcResponseSuccess>();
        resultFuture.thenAccept(r -> {
            result.complete(new GenericJsonrpcResponseSuccess(messageId, r.toJsonObject()));
        });
        return result;
    }

}
