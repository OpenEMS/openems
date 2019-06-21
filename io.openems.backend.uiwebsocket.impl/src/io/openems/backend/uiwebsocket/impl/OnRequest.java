package io.openems.backend.uiwebsocket.impl;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;

import io.openems.backend.metadata.api.Edge;
import io.openems.common.access_control.*;
import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.*;
import io.openems.common.jsonrpc.response.*;
import io.openems.common.jsonrpc.shared.EdgeMetadata;
import io.openems.common.websocket.SubscribedChannelsWorker;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
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

        // special handling for 'authenticate' request
        if (AuthenticateWithUsernameAndPasswordRequest.METHOD.equals(request.getMethod())) {
            return this.handleAuthenticateWithUsernameAndPasswordRequest(wsData, AuthenticateWithUsernameAndPasswordRequest.from(request));
        }

        // BackendUser user = this.assertUser(wsData, request);

        switch (request.getMethod()) {
            case EdgeRpcRequest.METHOD:
                return this.handleEdgeRpcRequest(wsData, EdgeRpcRequest.from(request));

            default:
                this.parent.logWarn(this.log, "Unhandled Request: " + request);
                throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
        }
    }

    private CompletableFuture<? extends JsonrpcResponseSuccess> handleAuthenticateWithUsernameAndPasswordRequest(WsData wsData, AuthenticateWithUsernameAndPasswordRequest request) throws OpenemsException {
        Pair<UUID, RoleId> tokenRolePair;
        try {
            tokenRolePair = this.parent.accessControl.login(request.getUsername(), request.getPassword());
        } catch (AuthenticationException e) {
            wsData.setRoleId(null);
            throw (e);
        }

        wsData.setRoleId(tokenRolePair.getValue());
        // generate token
        wsData.setToken(tokenRolePair.getKey());

        Set<String> edgeIds = this.parent.accessControl.getEdgeIds(tokenRolePair.getValue());
        List<EdgeMetadata> metadatas = new ArrayList<>();
        for (String edgeId : edgeIds) {
            Optional<Edge> edgeOpt = this.parent.metadata.getEdge(edgeId);
            if (edgeOpt.isPresent()) {
                Edge e = edgeOpt.get();
                metadatas.add(new EdgeMetadata(//
                        e.getId(), // Edge-ID
                        e.getComment(), // Comment
                        e.getProducttype(), // Product-Type
                        e.getVersion(), // Version
                        tokenRolePair.getValue(), // Role
                        e.isOnline() // Online-State
                ));
            }
        }

        // TODO unset on logout!
        return CompletableFuture.completedFuture(new AuthenticateWithUsernameAndPasswordResponse(request.getId(),
                tokenRolePair.getKey(), metadatas));
    }


    /**
     * Handles an EdgeRpcRequest.
     *
     * @param wsData         the WebSocket attachment
     * @param edgeRpcRequest the EdgeRpcRequest
     * @return the JSON-RPC Success Response Future
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<EdgeRpcResponse> handleEdgeRpcRequest(WsData wsData,
                                                                    EdgeRpcRequest edgeRpcRequest) throws OpenemsNamedException {
        String edgeId = edgeRpcRequest.getEdgeId();
        JsonrpcRequest request = edgeRpcRequest.getPayload();

        CompletableFuture<JsonrpcResponseSuccess> resultFuture;
        switch (request.getMethod()) {

            case SubscribeChannelsRequest.METHOD:
                resultFuture = this.handleSubscribeChannelsRequest(wsData, edgeId, null,
                        SubscribeChannelsRequest.from(request));
                break;

            case SubscribeSystemLogRequest.METHOD:
                resultFuture = this.handleSubscribeSystemLogRequest(wsData, edgeId, null,
                        SubscribeSystemLogRequest.from(request));
                break;

            case QueryHistoricTimeseriesDataRequest.METHOD:
                resultFuture = this.handleQueryHistoricDataRequest(wsData, edgeId, null,
                        QueryHistoricTimeseriesDataRequest.from(request));
                break;

            case QueryHistoricTimeseriesEnergyRequest.METHOD:
                resultFuture = this.handleQueryHistoricEnergyRequest(wsData, edgeId, null,
                        QueryHistoricTimeseriesEnergyRequest.from(request));
                break;

            case QueryHistoricTimeseriesExportXlxsRequest.METHOD:
                resultFuture = this.handleQueryHistoricTimeseriesExportXlxsRequest(wsData, edgeId, null,
                        QueryHistoricTimeseriesExportXlxsRequest.from(request));
                break;

            case GetEdgeConfigRequest.METHOD:
                resultFuture = this.handleGetEdgeConfigRequest(wsData, edgeId, null, GetEdgeConfigRequest.from(request));
                break;

            case CreateComponentConfigRequest.METHOD:
                resultFuture = this.handleCreateComponentConfigRequest(wsData, edgeId, null,
                        CreateComponentConfigRequest.from(request));
                break;

            case UpdateComponentConfigRequest.METHOD:
                resultFuture = this.handleUpdateComponentConfigRequest(wsData, edgeId, null,
                        UpdateComponentConfigRequest.from(request));
                break;

            case DeleteComponentConfigRequest.METHOD:
                resultFuture = this.handleDeleteComponentConfigRequest(wsData, edgeId, null,
                        DeleteComponentConfigRequest.from(request));
                break;

            case SetChannelValueRequest.METHOD:
                resultFuture = this.handleSetChannelValueRequest(wsData, edgeId, null, SetChannelValueRequest.from(request));
                break;

            case ComponentJsonApiRequest.METHOD:
                resultFuture = this.handleComponentJsonApiRequest(wsData, edgeId, null, ComponentJsonApiRequest.from(request));
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
        worker.handleSubscribeChannelsRequest(request.getCount(), permittedChannels, edgeId);

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
     * @param wsData  the WsData of the Socket
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
     * @param wsData  the WsData of the Socket
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
     * @param wsData  the WsData of the Socket
     * @param user    the User
     * @param request the QueryHistoricTimeseriesExportXlxsRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricTimeseriesExportXlxsRequest(WsData wsData, String edgeId,
                                                                                                     User user, QueryHistoricTimeseriesExportXlxsRequest request) throws OpenemsNamedException {
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), edgeId, QueryHistoricTimeseriesExportXlxsRequest.METHOD);
        return CompletableFuture
                .completedFuture(this.parent.timeData.handleQueryHistoricTimeseriesExportXlxsRequest(edgeId, request));
    }

    /**
     * Handles a GetEdgeConfigRequest.
     *
     * @param wsData  the WsData of the Socket
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
     * @param wsData                       the WsData of the Socket
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
     * @param wsData                       the WsData of the Socket
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
     * @param wsData                       the WsData of the Socket
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
     * @param wsData  the WsData of the Socket
     * @param user    the User
     * @param request the SetChannelValueRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleSetChannelValueRequest(WsData wsData, String edgeId, User user,
                                                                                   SetChannelValueRequest request) throws OpenemsNamedException {
        //user.assertRoleIsAtLeast(SetChannelValueRequest.METHOD, Role.ADMIN);

        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), edgeId, SetChannelValueRequest.METHOD);
        return this.parent.edgeWebsocket.send(edgeId, user, request);
    }

    /**
     * Handles a UpdateComponentConfigRequest.
     *
     * @param wsData                  the WsData of the Socket
     * @param edgeId                  the Edge-ID
     * @param user                    the User - Installer-level required
     * @param componentJsonApiRequest the ComponentJsonApiRequest
     * @return the Future JSON-RPC Response
     * @throws OpenemsNamedException on error
     */
    private CompletableFuture<JsonrpcResponseSuccess> handleComponentJsonApiRequest(WsData wsData, String edgeId, User user,
                                                                                    ComponentJsonApiRequest componentJsonApiRequest) throws OpenemsNamedException {
        // user.assertRoleIsAtLeast(ComponentJsonApiRequest.METHOD, Role.GUEST);
        this.parent.accessControl.assertExecutePermission(wsData.getRoleId(), edgeId, ComponentJsonApiRequest.METHOD);
        return this.parent.edgeWebsocket.send(edgeId, user, componentJsonApiRequest);
    }

}
