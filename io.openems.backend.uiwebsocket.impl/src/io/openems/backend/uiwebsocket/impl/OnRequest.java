package io.openems.backend.uiwebsocket.impl;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;

import io.openems.common.access_control.*;
import io.openems.common.channel.AccessMode;
import io.openems.common.websocket.SubscribedChannelsWorker;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.backend.metadata.api.BackendUser;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.DeleteComponentConfigRequest;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesDataRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesEnergyRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesExportXlxsRequest;
import io.openems.common.jsonrpc.request.SetChannelValueRequest;
import io.openems.common.jsonrpc.request.SubscribeChannelsRequest;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.response.EdgeRpcResponse;
import io.openems.common.jsonrpc.response.GetEdgeConfigResponse;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesDataResponse;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesEnergyResponse;
import io.openems.common.session.Role;
import io.openems.common.session.User;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig;

public class OnRequest implements io.openems.common.websocket.OnRequest {

    private final Logger log = LoggerFactory.getLogger(OnRequest.class);
    private final UiWebsocketImpl parent;

    public OnRequest(UiWebsocketImpl parent) {
        this.parent = parent;
    }

    @Override
    public CompletableFuture<? extends JsonrpcResponseSuccess> run(WebSocket ws, JsonrpcRequest request)
            throws OpenemsNamedException {
        WsData wsData = ws.getAttachment();
        BackendUser user = this.assertUser(wsData, request);

        switch (request.getMethod()) {
            case EdgeRpcRequest.METHOD:
                return this.handleEdgeRpcRequest(wsData, user, EdgeRpcRequest.from(request));

            default:
                this.parent.logWarn(this.log, "Unhandled Request: " + request);
                throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
        }
    }

    /**
     * Gets the authenticated User or throws an Exception if User is not
     * authenticated.
     *
     * @param wsData  the WebSocket attachment
     * @param request the JsonrpcRequest
     * @return the User
     * @throws OpenemsNamedException if User is not authenticated
     */
    private BackendUser assertUser(WsData wsData, JsonrpcRequest request) throws OpenemsNamedException {
        Optional<String> userIdOpt = wsData.getUserId();
        if (!userIdOpt.isPresent()) {
            throw OpenemsError.COMMON_USER_NOT_AUTHENTICATED
                    .exception("User-ID is empty. Ignoring request [" + request.getMethod() + "]");
        }
        Optional<BackendUser> userOpt = this.parent.metadata.getUser(userIdOpt.get());
        if (!userOpt.isPresent()) {
            throw OpenemsError.COMMON_USER_NOT_AUTHENTICATED.exception("User with ID [" + userIdOpt.get()
                    + "] is unknown. Ignoring request [" + request.getMethod() + "]");
        }
        return userOpt.get();
    }

    /**
     * Handles an EdgeRpcRequest.
     *
     * @param wsData         the WebSocket attachment
     * @param backendUser    the authenticated User
     * @param edgeRpcRequest the EdgeRpcRequest
     * @return the JSON-RPC Success Response Future
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<EdgeRpcResponse> handleEdgeRpcRequest(WsData wsData, BackendUser backendUser,
                                                                    EdgeRpcRequest edgeRpcRequest) throws OpenemsNamedException {
        String edgeId = edgeRpcRequest.getEdgeId();
        JsonrpcRequest request = edgeRpcRequest.getPayload();
        User user = backendUser.getAsCommonUser(edgeId);

        CompletableFuture<JsonrpcResponseSuccess> resultFuture;
        switch (request.getMethod()) {

            case SubscribeChannelsRequest.METHOD:
                resultFuture = this.handleSubscribeChannelsRequest(wsData, edgeId, user,
                        SubscribeChannelsRequest.from(request));
                break;

            case SubscribeSystemLogRequest.METHOD:
                resultFuture = this.handleSubscribeSystemLogRequest(wsData, edgeId, user,
                        SubscribeSystemLogRequest.from(request));
                break;

            case QueryHistoricTimeseriesDataRequest.METHOD:
                resultFuture = this.handleQueryHistoricDataRequest(wsData, edgeId, user,
                        QueryHistoricTimeseriesDataRequest.from(request));
                break;

            case QueryHistoricTimeseriesEnergyRequest.METHOD:
                resultFuture = this.handleQueryHistoricEnergyRequest(wsData, edgeId, user,
                        QueryHistoricTimeseriesEnergyRequest.from(request));
                break;

		case QueryHistoricTimeseriesExportXlxsRequest.METHOD:
			resultFuture = this.handleQueryHistoricTimeseriesExportXlxsRequest(edgeId, user,
					QueryHistoricTimeseriesExportXlxsRequest.from(request));
			break;

            case GetEdgeConfigRequest.METHOD:
                resultFuture = this.handleGetEdgeConfigRequest(wsData, edgeId, user, GetEdgeConfigRequest.from(request));
                break;

            case CreateComponentConfigRequest.METHOD:
                resultFuture = this.handleCreateComponentConfigRequest(wsData, edgeId, user,
                        CreateComponentConfigRequest.from(request));
                break;

            case UpdateComponentConfigRequest.METHOD:
                resultFuture = this.handleUpdateComponentConfigRequest(wsData, edgeId, user,
                        UpdateComponentConfigRequest.from(request));
                break;

            case DeleteComponentConfigRequest.METHOD:
                resultFuture = this.handleDeleteComponentConfigRequest(wsData, edgeId, user,
                        DeleteComponentConfigRequest.from(request));
                break;

		case SetChannelValueRequest.METHOD:
			resultFuture = this.handleSetChannelValueRequest(edgeId, user, SetChannelValueRequest.from(request));
			break;

            case ComponentJsonApiRequest.METHOD:
                resultFuture = this.handleComponentJsonApiRequest(edgeId, user, ComponentJsonApiRequest.from(request));
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

    /**
     * Handles a SubscribeChannelsRequest.
     *
     * @param wsData  the WebSocket attachment
     * @param edgeId  the Edge-ID
     * @param user    the User - no specific level required
     * @param request the SubscribeChannelsRequest
     * @return the JSON-RPC Success Response Future
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeChannelsRequest(WsData wsData, String edgeId,
                                                                                     User user, SubscribeChannelsRequest request) throws AuthenticationException, ServiceNotAvailableException, AuthorizationException {

        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), edgeId, SubscribeChannelsRequest.METHOD);
        Set<ChannelAddress> permittedChannels = this.parent.accessControl.intersectAccessPermission(
                wsData.getRoleId(),
                edgeId,
                request.getChannels(),
                AccessMode.READ_WRITE, AccessMode.READ_ONLY);

        // activate SubscribedChannelsWorker
        SubscribedChannelsWorker worker = wsData.getSubscribedChannelsWorker();
        worker.clearAll();
        worker.handleSubscribeChannelsRequest(user.getRole(), request.getCount(), permittedChannels, edgeId);

        // JSON-RPC response
        return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
    }

    /**
     * Handles a SubscribeSystemLogRequest.
     *
     * @param wsData  the WebSocket attachment
     * @param edgeId  the Edge-ID
     * @param user    the User
     * @param request the SubscribeSystemLogRequest
     * @return the JSON-RPC Success Response Future
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(WsData wsData, String edgeId,
                                                                                      User user, SubscribeSystemLogRequest request) throws OpenemsNamedException {
        // user.assertRoleIsAtLeast(SubscribeSystemLogRequest.METHOD, Role.OWNER);
        // TODO test
        UUID token = wsData.assertToken();

        // TODO test
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), edgeId, SubscribeSystemLogRequest.METHOD);

        // Forward to Edge
        return this.parent.edgeWebsocket.handleSubscribeSystemLogRequest(edgeId, user, token, request);
    }

    /**
     * Handles a QueryHistoricTimeseriesDataRequest.
     *
     * @param wsData
     * @param edgeId  the Edge-ID
     * @param user    the User - no specific level required
     * @param request the QueryHistoricDataRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricDataRequest(WsData wsData, String edgeId, User user,
                                                                                     QueryHistoricTimeseriesDataRequest request) throws OpenemsNamedException {
        // TODO test
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), edgeId, QueryHistoricTimeseriesDataRequest.METHOD);
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> historicData = this.parent.timeData
				.queryHistoricData(//

						edgeId, //
						request.getFromDate(), //
						request.getToDate(), //
						request.getChannels());

        // JSON-RPC response
		return CompletableFuture
				.completedFuture(new QueryHistoricTimeseriesDataResponse(request.getId(), historicData));
    }

    /**
     * Handles a QueryHistoricTimeseriesEnergyRequest.
     *
     * @param wsData
     * @param edgeId  the Edge-ID
     * @param user    the User - no specific level required
     * @param request the QueryHistoricEnergyRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricEnergyRequest(WsData wsData, String edgeId, User user,
                                                                                       QueryHistoricTimeseriesEnergyRequest request) throws OpenemsNamedException {
        // TODO test
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), edgeId, QueryHistoricTimeseriesEnergyRequest.METHOD);
        Map<ChannelAddress, JsonElement> data;
        data = this.parent.timeData.queryHistoricEnergy(//
                edgeId, /* ignore Edge-ID */
                request.getFromDate(), request.getToDate(), request.getChannels());

        // JSON-RPC response
        return CompletableFuture.completedFuture(new QueryHistoricTimeseriesEnergyResponse(request.getId(), data));
	}

	/**
	 * Handles a QueryHistoricTimeseriesExportXlxsRequest.
	 * 
	 * @param user    the User
	 * @param request the QueryHistoricTimeseriesExportXlxsRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricTimeseriesExportXlxsRequest(String edgeId,
			User user, QueryHistoricTimeseriesExportXlxsRequest request) throws OpenemsNamedException {
		return CompletableFuture
				.completedFuture(this.parent.timeData.handleQueryHistoricTimeseriesExportXlxsRequest(edgeId, request));
    }

    /**
     * Handles a GetEdgeConfigRequest.
     *
     * @param wsData
     * @param edgeId  the Edge-ID
     * @param user    the User - no specific level required
     * @param request the GetEdgeConfigRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleGetEdgeConfigRequest(WsData wsData, String edgeId, User user,
                                                                                 GetEdgeConfigRequest request) throws OpenemsNamedException {
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), edgeId, GetEdgeConfigRequest.METHOD);
        EdgeConfig config = this.parent.metadata.getEdgeOrError(edgeId).getConfig();

        // JSON-RPC response
        return CompletableFuture.completedFuture(new GetEdgeConfigResponse(request.getId(), config));
    }

    /**
     * Handles a CreateComponentConfigRequest.
     *
     * @param wsData
     * @param edgeId                       the Edge-ID
     * @param user                         the User - Installer-level required
     * @param createComponentConfigRequest the CreateComponentConfigRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleCreateComponentConfigRequest(WsData wsData, String edgeId, User user,
                                                                                         CreateComponentConfigRequest createComponentConfigRequest) throws OpenemsNamedException {
        //user.assertRoleIsAtLeast(CreateComponentConfigRequest.METHOD, Role.INSTALLER);
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), edgeId, CreateComponentConfigRequest.METHOD);

        return this.parent.edgeWebsocket.send(edgeId, user, createComponentConfigRequest);
    }

    /**
     * Handles a UpdateComponentConfigRequest.
     *
     * @param wsData
     * @param edgeId                       the Edge-ID
     * @param user                         the User - Installer-level required
     * @param updateComponentConfigRequest the UpdateComponentConfigRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleUpdateComponentConfigRequest(WsData wsData, String edgeId, User user,
                                                                                         UpdateComponentConfigRequest updateComponentConfigRequest) throws OpenemsNamedException {
//		user.assertRoleIsAtLeast(UpdateComponentConfigRequest.METHOD, Role.INSTALLER);
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), edgeId, UpdateComponentConfigRequest.METHOD);

        return this.parent.edgeWebsocket.send(edgeId, user, updateComponentConfigRequest);
    }

    /**
     * Handles a DeleteComponentConfigRequest.
     *
     * @param wsData
     * @param edgeId                       the Edge-ID
     * @param user                         the User - Installer-level required
     * @param deleteComponentConfigRequest the DeleteComponentConfigRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleDeleteComponentConfigRequest(WsData wsData, String edgeId, User user,
                                                                                         DeleteComponentConfigRequest deleteComponentConfigRequest) throws OpenemsNamedException {
        // user.assertRoleIsAtLeast(DeleteComponentConfigRequest.METHOD, Role.INSTALLER);
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), edgeId, DeleteComponentConfigRequest.METHOD);

		return this.parent.edgeWebsocket.send(edgeId, user, deleteComponentConfigRequest);
	}

	/**
	 * Handles a SetChannelValueRequest.
	 * 
	 * @param user    the User
	 * @param request the SetChannelValueRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSetChannelValueRequest(String edgeId, User user,
			SetChannelValueRequest request) throws OpenemsNamedException {
		user.assertRoleIsAtLeast(SetChannelValueRequest.METHOD, Role.ADMIN);

        return this.parent.edgeWebsocket.send(edgeId, user, request);
    }

    /**
     * Handles a UpdateComponentConfigRequest.
     *
     * @param edgeId                  the Edge-ID
     * @param user                    the User - Installer-level required
     * @param componentJsonApiRequest the ComponentJsonApiRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleComponentJsonApiRequest(String edgeId, User user,
                                                                                    ComponentJsonApiRequest componentJsonApiRequest) throws OpenemsNamedException {
        user.assertRoleIsAtLeast(ComponentJsonApiRequest.METHOD, Role.GUEST);

        return this.parent.edgeWebsocket.send(edgeId, user, componentJsonApiRequest);
    }

}
