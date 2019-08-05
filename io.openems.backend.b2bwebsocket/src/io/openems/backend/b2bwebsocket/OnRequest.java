package io.openems.backend.b2bwebsocket;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import io.openems.backend.metadata.api.Edge;
import io.openems.common.jsonrpc.request.*;
import io.openems.common.jsonrpc.response.EdgeRpcResponse;
import io.openems.common.websocket.SubscribedChannelsWorker;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.b2bwebsocket.jsonrpc.request.SubscribeEdgesChannelsRequest;
import io.openems.backend.common.jsonrpc.request.GetEdgesChannelsValuesRequest;
import io.openems.backend.common.jsonrpc.request.GetEdgesStatusRequest;
import io.openems.backend.common.jsonrpc.response.GetEdgesChannelsValuesResponse;
import io.openems.backend.common.jsonrpc.response.GetEdgesStatusResponse;
import io.openems.backend.common.jsonrpc.response.GetEdgesStatusResponse.EdgeInfo;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
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

        switch (request.getMethod()) {

            case GetEdgesStatusRequest.METHOD:
                return this.handleGetStatusOfEdgesRequest(request.getId(), GetEdgesStatusRequest.from(request), wsData);

            case GetEdgesChannelsValuesRequest.METHOD:
                return this.handleGetChannelsValuesRequest(request.getId(),
                        GetEdgesChannelsValuesRequest.from(request), wsData);

            case SubscribeEdgesChannelsRequest.METHOD:
                return this.handleSubscribeEdgesChannelsRequest(wsData, request.getId(),
                        SubscribeEdgesChannelsRequest.from(request));

            case EdgeRpcRequest.METHOD:
                return this.handleEdgeRpcRequest(wsData, EdgeRpcRequest.from(request));

            case SetGridConnScheduleRequest.METHOD:
                return this.handleSetGridConnScheduleRequest(request.getId(),
                        SetGridConnScheduleRequest.from(request), wsData);

            default:
                this.parent.logWarn(this.log, "Unhandled Request: " + request);
                throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
        }
    }

    private CompletableFuture<? extends JsonrpcResponseSuccess> handleEdgeRpcRequest(WsData wsData, EdgeRpcRequest edgeRpcRequest) throws OpenemsNamedException {
        String edgeId = edgeRpcRequest.getEdgeId();
        JsonrpcRequest request = edgeRpcRequest.getPayload();
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), edgeId, EdgeRpcRequest.METHOD);

        CompletableFuture<JsonrpcResponseSuccess> resultFuture;
		if (SubscribeChannelsRequest.METHOD.equals(request.getMethod())) {
			resultFuture = this.handleSubscribeChannelsRequest(wsData, edgeId,
				SubscribeChannelsRequest.from(request));
		} else {
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

    private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeChannelsRequest(WsData wsData, String edgeId, SubscribeChannelsRequest request) {
		TreeSet<ChannelAddress> permittedChannels = new TreeSet<>(request.getChannels());

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
     * @param messageId the JSON-RPC Message-ID
     * @param request   the GetStatusOfEdgesRequest
     * @param wsData
     * @return the JSON-RPC Success Response Future
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<GetEdgesStatusResponse> handleGetStatusOfEdgesRequest(UUID messageId,
                                                                                    GetEdgesStatusRequest request, WsData wsData) throws OpenemsNamedException {
        Map<String, EdgeInfo> result = new HashMap<>();

        for (String edgeId : this.parent.accessControl.getEdgeIds(wsData.getRoleId())) {
            Optional<Edge> edgeOpt = this.parent.metadata.getEdge(edgeId);
            edgeOpt.ifPresent(edge -> result.put(edge.getId(), new EdgeInfo(edge.isOnline())));
        }

        return CompletableFuture.completedFuture(new GetEdgesStatusResponse(messageId, result));
    }

    /**
     * Handles a GetChannelsValuesRequest.
     *
     * @param messageId the JSON-RPC Message-ID
     * @param request   the GetChannelsValuesRequest
     * @param wsData
     * @return the JSON-RPC Success Response Future
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<GetEdgesChannelsValuesResponse> handleGetChannelsValuesRequest(UUID messageId, GetEdgesChannelsValuesRequest request, WsData wsData) throws OpenemsNamedException {
        GetEdgesChannelsValuesResponse response = new GetEdgesChannelsValuesResponse(messageId);
        for (String edgeId : request.getEdgeIds()) {
            Set<ChannelAddress> permittedChannels = this.parent.accessControl.intersectAccessPermission(wsData.getRoleId(), edgeId, request.getChannels());
            for (ChannelAddress channel : permittedChannels) {
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
     * @param messageId the JSON-RPC Message-ID
     * @param request   the SubscribeEdgesChannelsRequest
     * @return the JSON-RPC Success Response Future
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<GenericJsonrpcResponseSuccess> handleSubscribeEdgesChannelsRequest(WsData wsData,
                                                                                                 UUID messageId, SubscribeEdgesChannelsRequest request) throws OpenemsNamedException {
        for (String edgeId : request.getEdgeIds()) {
            this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), edgeId, SubscribeEdgesChannelsRequest.METHOD);
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
     * @param messageId                  the JSON-RPC Message-ID
     * @param setGridConnScheduleRequest the SetGridConnScheduleRequest
     * @param wsData
     * @return the JSON-RPC Success Response Future
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<GenericJsonrpcResponseSuccess> handleSetGridConnScheduleRequest(UUID messageId, SetGridConnScheduleRequest setGridConnScheduleRequest, WsData wsData) throws OpenemsNamedException {
        String edgeId = setGridConnScheduleRequest.getEdgeId();
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), edgeId, SetGridConnScheduleRequest.METHOD);

        // wrap original request inside ComponentJsonApiRequest
        String componentId = "ctrlBalancingSchedule0"; // TODO find dynamic Component-ID of BalancingScheduleController
        ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, setGridConnScheduleRequest);

        CompletableFuture<JsonrpcResponseSuccess> resultFuture = this.parent.edgeWebsocket.send(edgeId, request);

        // Wrap reply in GenericJsonrpcResponseSuccess
        CompletableFuture<GenericJsonrpcResponseSuccess> result = new CompletableFuture<GenericJsonrpcResponseSuccess>();
        resultFuture.thenAccept(r -> {
            result.complete(new GenericJsonrpcResponseSuccess(messageId, r.toJsonObject()));
        });
        return result;
    }

}
