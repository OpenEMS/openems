package io.openems.edge.controller.api.websocket;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;

import io.openems.common.accesscontrol.AuthenticationException;
import io.openems.common.accesscontrol.Pair;
import io.openems.common.accesscontrol.RoleId;
import io.openems.common.channel.AccessMode;
import io.openems.common.jsonrpc.request.*;
import io.openems.common.jsonrpc.response.*;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.types.ChannelAddress;
import io.openems.common.websocket.SubscribedChannelsWorker;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.user.EdgeUser;

public class OnRequest implements io.openems.common.websocket.OnRequest {

    private final Logger log = LoggerFactory.getLogger(OnRequest.class);
    private final WebsocketApi parent;

    OnRequest(WebsocketApi parent) {
        this.parent = parent;
    }

    @Override
    public CompletableFuture<? extends JsonrpcResponseSuccess> run(WebSocket ws, JsonrpcRequest request)
            throws OpenemsException, OpenemsNamedException {
        // get websocket attachment
        WsData wsData = ws.getAttachment();

        // special handling for 'authenticate' request
        switch (request.getMethod()) {
            case AuthenticateWithPasswordRequest.METHOD:
                return this.handleAuthenticateWithPasswordRequest(wsData, AuthenticateWithPasswordRequest.from(request));
            case AuthenticateWithUsernameAndPasswordRequest.METHOD:
                return this.handleAuthenticateWithUsernameAndPasswordRequest(wsData, AuthenticateWithUsernameAndPasswordRequest.from(request));
        }

        if (EdgeRpcRequest.METHOD.equals(request.getMethod())) {
            return this.handleEdgeRpcRequest(wsData, EdgeRpcRequest.from(request));
        }

        this.parent.logWarn(this.log, "Unhandled Request: " + request);
        throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
    }

    /**
     * Handles an EdgeRpcRequest.
     *
     * @param wsData         the WebSocket attachment
     * @param edgeRpcRequest the EdgeRpcRequest
     * @return the JSON-RPC Success Response Future
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<EdgeRpcResponse> handleEdgeRpcRequest(WsData wsData, EdgeRpcRequest edgeRpcRequest) throws OpenemsNamedException {
        JsonrpcRequest request = edgeRpcRequest.getPayload();

        CompletableFuture<JsonrpcResponseSuccess> resultFuture;
        switch (request.getMethod()) {

            case SubscribeChannelsRequest.METHOD:
                SubscribeChannelsRequest subscribeChannelsRequest = SubscribeChannelsRequest.from(request);
                resultFuture = this.handleSubscribeChannelsRequest(wsData, subscribeChannelsRequest);
                break;

            case SubscribeSystemLogRequest.METHOD:
                resultFuture = this.handleSubscribeSystemLogRequest(wsData, SubscribeSystemLogRequest.from(request));
                break;

            case QueryHistoricTimeseriesDataRequest.METHOD:
                resultFuture = this.handleQueryHistoricDataRequest(wsData, QueryHistoricTimeseriesDataRequest.from(request));
                break;

            case QueryHistoricTimeseriesEnergyRequest.METHOD:
                resultFuture = this.handleQueryHistoricEnergyRequest(wsData, QueryHistoricTimeseriesEnergyRequest.from(request));
                break;

            case QueryHistoricTimeseriesExportXlxsRequest.METHOD:
                resultFuture = this.handleQueryHistoricTimeseriesExportXlxsRequest(
                        QueryHistoricTimeseriesExportXlxsRequest.from(request));
                break;

            case CreateComponentConfigRequest.METHOD:
                resultFuture = this.handleCreateComponentConfigRequest(wsData, CreateComponentConfigRequest.from(request));
                break;

            case UpdateComponentConfigRequest.METHOD:
                resultFuture = this.handleUpdateComponentConfigRequest(wsData, UpdateComponentConfigRequest.from(request));
                break;

            case DeleteComponentConfigRequest.METHOD:
                resultFuture = this.handleDeleteComponentConfigRequest(DeleteComponentConfigRequest.from(request), wsData);
                break;

            case GetEdgeConfigRequest.METHOD:
                resultFuture = this.handleGetEdgeConfigRequest(wsData, GetEdgeConfigRequest.from(request));
                break;

            case SetChannelValueRequest.METHOD:
                resultFuture = this.handleSetChannelValueRequest(wsData, SetChannelValueRequest.from(request));
                break;

            case ComponentJsonApiRequest.METHOD:
                resultFuture = this.handleComponentJsonApiRequest(ComponentJsonApiRequest.from(request));
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
        if (!userOpt.isPresent()) {
            wsData.unsetUser();
            throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
        }

        // authentication successful
        EdgeUser user = userOpt.get();
        wsData.setUser(user);
        // TODO set when access control gets used
        wsData.setRoleId(null);
        // this.parent.sessionTokens.put(wsData.getSessionToken(), user);
        // TODO unset on logout!
        return CompletableFuture.completedFuture(new AuthenticateWithPasswordResponse(request.getId(),
                wsData.getSessionToken(), Utils.getEdgeMetadata(null)));
    }

    private CompletableFuture<? extends JsonrpcResponseSuccess> handleAuthenticateWithUsernameAndPasswordRequest(WsData wsData, AuthenticateWithUsernameAndPasswordRequest request) throws OpenemsException {
        Pair<UUID, RoleId> sessionRolePair;
        try {
            sessionRolePair = this.parent.accessControl.login(request.getUsername(), request.getPassword());
        } catch (AuthenticationException e) {
            wsData.setRoleId(null);
            throw(e);
        }

        wsData.setSessionToken(sessionRolePair.getKey());
        wsData.setRoleId(sessionRolePair.getValue());
        // TODO unset on logout!
        return CompletableFuture.completedFuture(new AuthenticateWithUsernameAndPasswordResponse(request.getId(),
                wsData.getSessionToken(), Utils.getEdgeMetadata(sessionRolePair.getValue())));
    }


    /**
     * Handles a SubscribeChannelsRequest.
     *
     * @param wsData  the WebSocket attachment
     * @param request the SubscribeChannelsRequest
     * @return the JSON-RPC Success Response Future
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeChannelsRequest(WsData wsData, SubscribeChannelsRequest request) throws OpenemsNamedException {
        // activate SubscribedChannelsWorker
        SubscribedChannelsWorker worker = wsData.getSubscribedChannelsWorker();
        worker.clearAll();

        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), this.parent.edgeIdentifier, SubscribeChannelsRequest.METHOD);
        Set<ChannelAddress> permittedChannels = this.parent.accessControl.intersectAccessPermission(
                wsData.getRoleId(),
                this.parent.edgeIdentifier, request.getChannels(),
                AccessMode.READ_ONLY,
                AccessMode.READ_WRITE);
        worker.handleSubscribeChannelsRequest(request.getCount(), permittedChannels, WebsocketApi.EDGE_ID);

        // JSON-RPC response
        return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
    }

    /**
     * Handles a QueryHistoricDataRequest.
     *
     * @param wsData
     * @param request the QueryHistoricDataRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricDataRequest(WsData wsData,
                                                                                     QueryHistoricTimeseriesDataRequest request) throws OpenemsNamedException {
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), this.parent.edgeIdentifier, QueryHistoricTimeseriesDataRequest.METHOD);
        SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> data = this.parent.getTimedata()
                .queryHistoricData(//
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
     * Handles a QueryHistoricTimeseriesExportXlxsRequest.
     *
     * @param request the QueryHistoricTimeseriesExportXlxsRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricTimeseriesExportXlxsRequest(QueryHistoricTimeseriesExportXlxsRequest request) throws OpenemsNamedException {
        return CompletableFuture.completedFuture(this.parent.getTimedata()
                .handleQueryHistoricTimeseriesExportXlxsRequest(null /* ignore Edge-ID */, request));
    }

    /**
     * Handles a CreateComponentConfigRequest.
     *
     * @param wsData
     * @param createComponentConfigRequest the CreateComponentConfigRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleCreateComponentConfigRequest(WsData wsData,
                                                                                         CreateComponentConfigRequest createComponentConfigRequest) throws OpenemsNamedException {
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), this.parent.edgeIdentifier, CreateComponentConfigRequest.METHOD);
        // wrap original request inside ComponentJsonApiRequest
        String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
        ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, createComponentConfigRequest);

        return this.handleComponentJsonApiRequest(request);
    }

    /**
     * Handles a UpdateComponentConfigRequest.
     *
     * @param wsData
     * @param updateComponentConfigRequest the UpdateComponentConfigRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleUpdateComponentConfigRequest(WsData wsData,
                                                                                         UpdateComponentConfigRequest updateComponentConfigRequest) throws OpenemsNamedException {
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), this.parent.edgeIdentifier, UpdateComponentConfigRequest.METHOD);
        // wrap original request inside ComponentJsonApiRequest
        String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
        ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, updateComponentConfigRequest);

        return this.handleComponentJsonApiRequest(request);
    }

    /**
     * Handles a DeleteComponentConfigRequest.
     *
     * @param deleteComponentConfigRequest the DeleteComponentConfigRequest
     * @param wsData
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleDeleteComponentConfigRequest(DeleteComponentConfigRequest deleteComponentConfigRequest, WsData wsData) throws OpenemsNamedException {
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), this.parent.edgeIdentifier, DeleteComponentConfigRequest.METHOD);
        // wrap original request inside ComponentJsonApiRequest
        String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
        ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, deleteComponentConfigRequest);

        return this.handleComponentJsonApiRequest(request);
    }

    /**
     * Handles a GetEdgeConfigRequest.
     *
     * @param wsData
     * @param getEdgeConfigRequest the GetEdgeConfigRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleGetEdgeConfigRequest(WsData wsData,
                                                                                 GetEdgeConfigRequest getEdgeConfigRequest) throws OpenemsNamedException {
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), this.parent.edgeIdentifier, GetEdgeConfigRequest.METHOD);
        // wrap original request inside ComponentJsonApiRequest
        ComponentJsonApiRequest request = new ComponentJsonApiRequest(OpenemsConstants.COMPONENT_MANAGER_ID,
                getEdgeConfigRequest);

        return this.handleComponentJsonApiRequest(request);
    }

    /**
     * Handles a SetChannelValueRequest.
     *
     * @param wsData
     * @param request the SetChannelValueRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleSetChannelValueRequest(WsData wsData, SetChannelValueRequest request) throws OpenemsNamedException {
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), this.parent.edgeIdentifier, SetChannelValueRequest.METHOD);
        return this.parent.apiWorker.handleSetChannelValueRequest(this.parent.componentManager, request);
    }

    /**
     * Handles a ComponentJsonApiRequest.
     *
     * @param request the ComponentJsonApiRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleComponentJsonApiRequest(ComponentJsonApiRequest request) throws OpenemsNamedException {
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
        CompletableFuture<JsonrpcResponseSuccess> responseFuture = jsonApi.handleJsonrpcRequest(
                /*TODO fix this!*/null, request.getPayload());

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
     * @param request the SubscribeSystemLogRequest
     * @return the JSON-RPC Success Response Future
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(WsData wsData,
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
