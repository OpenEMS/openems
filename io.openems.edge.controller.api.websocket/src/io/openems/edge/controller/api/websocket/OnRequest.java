package io.openems.edge.controller.api.websocket;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import io.openems.common.access_control.RoleId;
import io.openems.common.channel.AccessMode;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.AuthenticateWithPasswordRequest;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.DeleteComponentConfigRequest;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesDataRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesEnergyRequest;
import io.openems.common.jsonrpc.request.SetChannelValueRequest;
import io.openems.common.jsonrpc.request.SubscribeChannelsRequest;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.response.AuthenticateWithPasswordResponse;
import io.openems.common.jsonrpc.response.EdgeRpcResponse;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesDataResponse;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesEnergyResponse;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;
import io.openems.common.websocket.SubscribedChannelsWorker;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.user.EdgeUser;

public class OnRequest implements io.openems.common.websocket.OnRequest {

    private final Logger log = LoggerFactory.getLogger(OnRequest.class);
    private final WebsocketApi parent;

    public OnRequest(WebsocketApi parent) {
        this.parent = parent;
    }

    @Override
    public CompletableFuture<? extends JsonrpcResponseSuccess> run(WebSocket ws, JsonrpcRequest request)
            throws OpenemsException, OpenemsNamedException {
        // get websocket attachment
        WsData wsData = ws.getAttachment();

        // special handling for 'authenticate' request
        if (request.getMethod().equals(AuthenticateWithPasswordRequest.METHOD)) {
            return this.handleAuthenticateWithPasswordRequest(wsData, AuthenticateWithPasswordRequest.from(request));
        }

        // is user authenticated?
        EdgeUser user = wsData.assertUserIsAuthenticated(request.getMethod());
        user.assertRoleIsAtLeast(request.getMethod(), Role.GUEST);

        switch (request.getMethod()) {
            case EdgeRpcRequest.METHOD:
                return this.handleEdgeRpcRequest(wsData, user, EdgeRpcRequest.from(request));
            default:
                this.parent.logWarn(this.log, "Unhandled Request: " + request);
                throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
        }
    }

    /**
     * Handles an EdgeRpcRequest.
     *
     * @param wsData         the WebSocket attachment
     * @param user           the User
     * @param edgeRpcRequest the EdgeRpcRequest
     * @return the JSON-RPC Success Response Future
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<EdgeRpcResponse> handleEdgeRpcRequest(WsData wsData, EdgeUser user, EdgeRpcRequest edgeRpcRequest) throws OpenemsNamedException {
        JsonrpcRequest request = edgeRpcRequest.getPayload();

        CompletableFuture<JsonrpcResponseSuccess> resultFuture;
        switch (request.getMethod()) {

            case SubscribeChannelsRequest.METHOD:
                SubscribeChannelsRequest subscribeChannelsRequest = SubscribeChannelsRequest.from(request);
                resultFuture = this.handleSubscribeChannelsRequest(wsData, user, subscribeChannelsRequest);
                break;

            case SubscribeSystemLogRequest.METHOD:
                resultFuture = this.handleSubscribeSystemLogRequest(wsData, user, SubscribeSystemLogRequest.from(request));
                break;

            case QueryHistoricTimeseriesDataRequest.METHOD:
                resultFuture = this.handleQueryHistoricDataRequest(wsData, user, QueryHistoricTimeseriesDataRequest.from(request));
                break;

            case QueryHistoricTimeseriesEnergyRequest.METHOD:
                resultFuture = this.handleQueryHistoricEnergyRequest(wsData, QueryHistoricTimeseriesEnergyRequest.from(request));
                break;

            case CreateComponentConfigRequest.METHOD:
                resultFuture = this.handleCreateComponentConfigRequest(wsData, user, CreateComponentConfigRequest.from(request));
                break;

            case UpdateComponentConfigRequest.METHOD:
                resultFuture = this.handleUpdateComponentConfigRequest(wsData, user, UpdateComponentConfigRequest.from(request));
                break;

            case DeleteComponentConfigRequest.METHOD:
                resultFuture = this.handleDeleteComponentConfigRequest(user, DeleteComponentConfigRequest.from(request), wsData);
                break;

            case GetEdgeConfigRequest.METHOD:
                resultFuture = this.handleGetEdgeConfigRequest(wsData, user, GetEdgeConfigRequest.from(request));
                break;

		case SetChannelValueRequest.METHOD:
			resultFuture = this.handleSetChannelValueRequest(user, SetChannelValueRequest.from(request));
			break;

            case ComponentJsonApiRequest.METHOD:
                resultFuture = this.handleComponentJsonApiRequest(user, ComponentJsonApiRequest.from(request));
                break;

            // TODO: to be implemented: UI Logout

            default:
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
     * Handles a AuthenticateWithPasswordRequest.
     *
     * @param wsData  the WebSocket attachment
     * @param request the AuthenticateWithPasswordRequest
     * @return the JSON-RPC Success Response Future
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleAuthenticateWithPasswordRequest(WsData wsData,
                                                                                            AuthenticateWithPasswordRequest request) throws OpenemsNamedException {
        Optional<EdgeUser> userOpt = this.parent.userService.authenticate(request.getPassword());
        RoleId roleId = this.parent.userService.authenticate2(request.getPassword());
        if (!userOpt.isPresent()) {
            wsData.unsetUser();
            throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
        }

        // authentication successful
        EdgeUser user = userOpt.get();
        wsData.setUser(user);
        wsData.setRoleId(roleId);
        this.parent.sessionTokens.put(wsData.getSessionToken(), user);
        // TODO unset on logout!
        return CompletableFuture.completedFuture(new AuthenticateWithPasswordResponse(request.getId(),
                wsData.getSessionToken(), Utils.getEdgeMetadata(user.getRole())));
    }

    /**
     * Handles a SubscribeChannelsRequest.
     *
     * @param wsData  the WebSocket attachment
     * @param user    the User
     * @param request the SubscribeChannelsRequest
     * @return the JSON-RPC Success Response Future
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeChannelsRequest(WsData wsData, EdgeUser user, SubscribeChannelsRequest request) throws OpenemsNamedException {
        // activate SubscribedChannelsWorker
        SubscribedChannelsWorker worker = wsData.getSubscribedChannelsWorker();
        worker.clearAll();

        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), this.parent.edgeIdentifier, SubscribeChannelsRequest.METHOD);
        Set<ChannelAddress> permittedChannels = this.parent.accessControl.intersectAccessPermission(
                wsData.getRoleId(),
                this.parent.edgeIdentifier, request.getChannels(),
                AccessMode.READ_ONLY,
                AccessMode.READ_WRITE);
        worker.handleSubscribeChannelsRequest(user.getRole(), request.getCount(), permittedChannels, WebsocketApi.EDGE_ID);

        // JSON-RPC response
        return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
    }

    /**
     * Handles a QueryHistoricDataRequest.
     *
     * @param wsData
     * @param user    the User
     * @param request the QueryHistoricDataRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricDataRequest(WsData wsData, EdgeUser user,
                                                                                     QueryHistoricTimeseriesDataRequest request) throws OpenemsNamedException {
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), this.parent.edgeIdentifier, QueryHistoricTimeseriesDataRequest.METHOD);
        TreeBasedTable<ZonedDateTime, ChannelAddress, JsonElement> data = this.parent.getTimedata().queryHistoricData(//
                null, /* ignore Edge-ID */
                request.getFromDate(), //
                request.getToDate(), //
                request.getChannels());

        // JSON-RPC response
        return CompletableFuture.completedFuture(new QueryHistoricTimeseriesDataResponse(request.getId(), data));
    }

    /**
     * Handles a QueryHistoricEnergyRequest.
     *
     * @param wsData
     * @param request the QueryHistoricEnergyRequest
     * @return the Future JSPN-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricEnergyRequest(
            WsData wsData, QueryHistoricTimeseriesEnergyRequest request) throws OpenemsNamedException {
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), this.parent.edgeIdentifier, QueryHistoricTimeseriesEnergyRequest.METHOD);
        Map<ChannelAddress, JsonElement> data = this.parent.getTimedata().queryHistoricEnergy(//
                null, /* ignore Edge-ID */
                request.getFromDate(), request.getToDate(), request.getChannels());

        // JSON-RPC response
        return CompletableFuture.completedFuture(new QueryHistoricTimeseriesEnergyResponse(request.getId(), data));
    }

    /**
     * Handles a CreateComponentConfigRequest.
     *
     * @param wsData
     * @param user                         the User
     * @param createComponentConfigRequest the CreateComponentConfigRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleCreateComponentConfigRequest(WsData wsData, EdgeUser user,
                                                                                         CreateComponentConfigRequest createComponentConfigRequest) throws OpenemsNamedException {
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), this.parent.edgeIdentifier, CreateComponentConfigRequest.METHOD);
        // wrap original request inside ComponentJsonApiRequest
        String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
        ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, createComponentConfigRequest);

        return this.handleComponentJsonApiRequest(user, request);
    }

    /**
     * Handles a UpdateComponentConfigRequest.
     *
     * @param wsData
     * @param user                         the User
     * @param updateComponentConfigRequest the UpdateComponentConfigRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleUpdateComponentConfigRequest(WsData wsData, EdgeUser user,
                                                                                         UpdateComponentConfigRequest updateComponentConfigRequest) throws OpenemsNamedException {
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), this.parent.edgeIdentifier, UpdateComponentConfigRequest.METHOD);
        // wrap original request inside ComponentJsonApiRequest
        String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
        ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, updateComponentConfigRequest);

        return this.handleComponentJsonApiRequest(user, request);
    }

    /**
     * Handles a DeleteComponentConfigRequest.
     *
     * @param user                         the User
     * @param deleteComponentConfigRequest the DeleteComponentConfigRequest
     * @param wsData
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleDeleteComponentConfigRequest(EdgeUser user,
                                                                                         DeleteComponentConfigRequest deleteComponentConfigRequest, WsData wsData) throws OpenemsNamedException {
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), this.parent.edgeIdentifier, DeleteComponentConfigRequest.METHOD);
        // wrap original request inside ComponentJsonApiRequest
        String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
        ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, deleteComponentConfigRequest);

        return this.handleComponentJsonApiRequest(user, request);
    }

    /**
     * Handles a GetEdgeConfigRequest.
     *
     * @param wsData
     * @param user                 the User
     * @param getEdgeConfigRequest the GetEdgeConfigRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleGetEdgeConfigRequest(WsData wsData, EdgeUser user,
                                                                                 GetEdgeConfigRequest getEdgeConfigRequest) throws OpenemsNamedException {
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), this.parent.edgeIdentifier, GetEdgeConfigRequest.METHOD);
        // wrap original request inside ComponentJsonApiRequest
        ComponentJsonApiRequest request = new ComponentJsonApiRequest(OpenemsConstants.COMPONENT_MANAGER_ID,
                getEdgeConfigRequest);

        return this.handleComponentJsonApiRequest(user, request);
	}

	/**
	 * Handles a SetChannelValueRequest.
	 * 
	 * @param user    the User
	 * @param request the SetChannelValueRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSetChannelValueRequest(EdgeUser user,
			SetChannelValueRequest request) throws OpenemsNamedException {
		user.assertRoleIsAtLeast(SetChannelValueRequest.METHOD, Role.ADMIN);

		return this.parent.apiWorker.handleSetChannelValueRequest(this.parent.componentManager, user, request);
    }

    /**
     * Handles a ComponentJsonApiRequest.
     *
     * @param user    the User
     * @param request the ComponentJsonApiRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleComponentJsonApiRequest(EdgeUser user,
                                                                                    ComponentJsonApiRequest request) throws OpenemsNamedException {
        // get Component
        String componentId = request.getComponentId();
        OpenemsComponent component = this.parent.componentManager.getComponent(componentId);

        if (component == null) {
            throw new OpenemsException("Unable to find Component [" + componentId + "]");
        }

        if (!(component instanceof JsonApi)) {
            throw new OpenemsException("Component [" + componentId + "] is no JsonApi");
        }

        // call JsonApi
        JsonApi jsonApi = (JsonApi) component;
        CompletableFuture<JsonrpcResponseSuccess> responseFuture = jsonApi.handleJsonrpcRequest(user,
                request.getPayload());

        // handle null response
        if (responseFuture == null) {
            OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getPayload().getMethod());
        }

        // Wrap reply in new JsonrpcResponseSuccess
        CompletableFuture<JsonrpcResponseSuccess> jsonrpcResponse = new CompletableFuture<>();
        responseFuture.thenAccept(response -> {
            jsonrpcResponse.complete(new GenericJsonrpcResponseSuccess(request.getId(), response.getResult()));
        });

        return jsonrpcResponse;
    }

    /**
     * Handles a SubscribeSystemLogRequest.
     *
     * @param wsData  the WebSocket attachment
     * @param user    the User
     * @param request the SubscribeSystemLogRequest
     * @return the JSON-RPC Success Response Future
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(WsData wsData, EdgeUser user,
                                                                                      SubscribeSystemLogRequest request) throws OpenemsNamedException {
        UUID token = wsData.getSessionToken();
        if (token == null) {
            throw OpenemsError.BACKEND_UI_TOKEN_MISSING.exception();
        }

        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), this.parent.edgeIdentifier, SubscribeSystemLogRequest.METHOD);

        // TODO @Stefan: Muss hier noch eine weitere Prüfung auf Channels durchgeführt werden? Der RPC hört sich so nach
        // zyklischen Channel Informationen an

        this.parent.handleSubscribeSystemLogRequest(token, request);
        return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
    }

}
