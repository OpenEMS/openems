package io.openems.backend.uiwebsocket.impl;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.alerting.UserAlertingSettings;
import io.openems.backend.common.jsonrpc.request.AddEdgeToUserRequest;
import io.openems.backend.common.jsonrpc.request.GetEmsTypeRequest;
import io.openems.backend.common.jsonrpc.request.GetSetupProtocolDataRequest;
import io.openems.backend.common.jsonrpc.request.GetSetupProtocolRequest;
import io.openems.backend.common.jsonrpc.request.GetUserAlertingConfigsRequest;
import io.openems.backend.common.jsonrpc.request.GetUserInformationRequest;
import io.openems.backend.common.jsonrpc.request.RegisterUserRequest;
import io.openems.backend.common.jsonrpc.request.SetUserAlertingConfigsRequest;
import io.openems.backend.common.jsonrpc.request.SetUserInformationRequest;
import io.openems.backend.common.jsonrpc.request.SimulationRequest;
import io.openems.backend.common.jsonrpc.request.SubmitSetupProtocolRequest;
import io.openems.backend.common.jsonrpc.request.SubscribeEdgesRequest;
import io.openems.backend.common.jsonrpc.response.AddEdgeToUserResponse;
import io.openems.backend.common.jsonrpc.response.GetEmsTypeResponse;
import io.openems.backend.common.jsonrpc.response.GetUserAlertingConfigsResponse;
import io.openems.backend.common.jsonrpc.response.GetUserInformationResponse;
import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.AuthenticateWithPasswordRequest;
import io.openems.common.jsonrpc.request.AuthenticateWithTokenRequest;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.request.GetEdgeRequest;
import io.openems.common.jsonrpc.request.GetEdgesRequest;
import io.openems.common.jsonrpc.request.LogoutRequest;
import io.openems.common.jsonrpc.request.SubscribeChannelsRequest;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.jsonrpc.request.UpdateUserLanguageRequest;
import io.openems.common.jsonrpc.request.UpdateUserSettingsRequest;
import io.openems.common.jsonrpc.response.AuthenticateResponse;
import io.openems.common.jsonrpc.response.Base64PayloadResponse;
import io.openems.common.jsonrpc.response.EdgeRpcResponse;
import io.openems.common.jsonrpc.response.GetEdgeResponse;
import io.openems.common.jsonrpc.response.GetEdgesResponse;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final Logger log = LoggerFactory.getLogger(OnRequest.class);

	private final UiWebsocketImpl parent;

	public OnRequest(UiWebsocketImpl parent) {
		this.parent = parent;
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> apply(WebSocket ws, JsonrpcRequest request)
			throws OpenemsNamedException {
		WsData wsData = ws.getAttachment();

		// Start with authentication requests
		CompletableFuture<? extends JsonrpcResponseSuccess> result = null;
		switch (request.getMethod()) {
		case AuthenticateWithTokenRequest.METHOD:
			return this.handleAuthenticateWithTokenRequest(wsData, AuthenticateWithTokenRequest.from(request));

		case AuthenticateWithPasswordRequest.METHOD:
			return this.handleAuthenticateWithPasswordRequest(wsData, AuthenticateWithPasswordRequest.from(request));

		case RegisterUserRequest.METHOD:
			return this.handleRegisterUserReuqest(wsData, RegisterUserRequest.from(request));
		}

		// should be authenticated
		var user = this.parent.assertUser(wsData, request);

		result = switch (request.getMethod()) {
		case LogoutRequest.METHOD -> //
			this.handleLogoutRequest(wsData, user, LogoutRequest.from(request));
		case EdgeRpcRequest.METHOD -> //
			this.handleEdgeRpcRequest(wsData, user, EdgeRpcRequest.from(request));
		case AddEdgeToUserRequest.METHOD -> //
			this.handleAddEdgeToUserRequest(user, AddEdgeToUserRequest.from(request));
		case GetEmsTypeRequest.METHOD -> //
			this.handleGetEmsTypeRequest(user, GetEmsTypeRequest.from(request));
		case GetUserInformationRequest.METHOD -> //
			this.handleGetUserInformationRequest(user, GetUserInformationRequest.from(request));
		case SetUserInformationRequest.METHOD -> //
			this.handleSetUserInformationRequest(user, SetUserInformationRequest.from(request));
		case GetSetupProtocolRequest.METHOD -> //
			this.handleGetSetupProtocolRequest(user, GetSetupProtocolRequest.from(request));
		case SubmitSetupProtocolRequest.METHOD -> //
			this.handleSubmitSetupProtocolRequest(user, SubmitSetupProtocolRequest.from(request));
		case UpdateUserLanguageRequest.METHOD -> //
			this.handleUpdateUserLanguageRequest(user, UpdateUserLanguageRequest.from(request));
		case GetUserAlertingConfigsRequest.METHOD -> //
			this.handleGetUserAlertingConfigsRequest(user, GetUserAlertingConfigsRequest.from(request));
		case SetUserAlertingConfigsRequest.METHOD -> //
			this.handleSetUserAlertingConfigsRequest(user, SetUserAlertingConfigsRequest.from(request));
		case GetSetupProtocolDataRequest.METHOD -> //
			this.handleGetSetupProtocolDataRequest(user, GetSetupProtocolDataRequest.from(request));
		case SubscribeEdgesRequest.METHOD -> //
			this.handleSubscribeEdgesRequest(wsData, SubscribeEdgesRequest.from(request));
		case GetEdgesRequest.METHOD -> //
			this.handleGetEdgesRequest(user, GetEdgesRequest.from(request));
		case GetEdgeRequest.METHOD -> //
			this.handleGetEdgeRequest(user, GetEdgeRequest.from(request));
		case UpdateUserSettingsRequest.METHOD -> //
			this.handleUpdateUserSettingsRequest(user, UpdateUserSettingsRequest.from(request));
		default -> null;
		};

		if (result != null) {
			// was able to handle request directly
			return result;
		}

		// forward to generic request handler
		return this.parent.jsonRpcRequestHandler.handleRequest(this.parent.getName(), user, request);
	}

	/**
	 * Handles a {@link AuthenticateWithTokenRequest}.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param request the {@link AuthenticateWithTokenRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleAuthenticateWithTokenRequest(WsData wsData,
			AuthenticateWithTokenRequest request) throws OpenemsNamedException {
		return this.handleAuthentication(wsData, request.getId(),
				this.parent.metadata.authenticate(request.getToken()));
	}

	/**
	 * Handles a {@link AuthenticateWithPasswordRequest}.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param request the {@link AuthenticateWithPasswordRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleAuthenticateWithPasswordRequest(WsData wsData,
			AuthenticateWithPasswordRequest request) throws OpenemsNamedException {
		if (request.usernameOpt.isPresent()) {
			return this.handleAuthentication(wsData, request.getId(),
					this.parent.metadata.authenticate(request.usernameOpt.get(), request.password));
		}
		return this.handleAuthentication(wsData, request.getId(), this.parent.metadata.authenticate(request.password));
	}

	/**
	 * Common handler for {@link AuthenticateWithTokenRequest} and
	 * {@link AuthenticateWithPasswordRequest}.
	 *
	 * @param wsData    the WebSocket attachment
	 * @param requestId the ID of the original {@link JsonrpcRequest}
	 * @param user      the authenticated {@link User}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleAuthentication(WsData wsData, UUID requestId, User user)
			throws OpenemsNamedException {
		wsData.setUserId(user.getId());
		wsData.setToken(user.getToken());
		return CompletableFuture
				.completedFuture(new AuthenticateResponse(requestId, user.getToken(), user, user.getLanguage()));
	}

	/**
	 * Handles a {@link RegisterUserRequest}.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param request the {@link RegisterUserRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleRegisterUserReuqest(WsData wsData,
			RegisterUserRequest request) throws OpenemsNamedException {
		this.parent.metadata.registerUser(request.getUser(), request.getOem());

		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a {@link LogoutRequest}.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param user    the authenticated {@link User}
	 * @param request the {@link LogoutRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleLogoutRequest(WsData wsData, User user,
			LogoutRequest request) throws OpenemsNamedException {
		wsData.logout();
		this.parent.metadata.logout(user);
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles an {@link EdgeRpcRequest}.
	 *
	 * @param wsData         the WebSocket attachment
	 * @param user           the authenticated {@link User}
	 * @param edgeRpcRequest the {@link EdgeRpcRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<EdgeRpcResponse> handleEdgeRpcRequest(WsData wsData, User user,
			EdgeRpcRequest edgeRpcRequest) throws OpenemsNamedException {
		var edgeId = edgeRpcRequest.getEdgeId();
		var request = edgeRpcRequest.getPayload();

		// TODO still not the best to check the access => should be separate service
		if (user.getRole(edgeId).isEmpty()) {
			this.parent.metadata.getEdgeMetadataForUser(user, edgeId);
			this.parent.logInfo(this.log, "Role was not defined for user=" + user.getId() + ", edge=" + edgeId);
		}
		user.assertEdgeRoleIsAtLeast(EdgeRpcRequest.METHOD, edgeId, Role.GUEST);

		CompletableFuture<JsonrpcResponseSuccess> resultFuture = switch (request.getMethod()) {
		case SubscribeChannelsRequest.METHOD ->
			this.handleSubscribeChannelsRequest(wsData, edgeId, user, SubscribeChannelsRequest.from(request));
		case SubscribeSystemLogRequest.METHOD ->
			this.handleSubscribeSystemLogRequest(wsData, edgeId, user, SubscribeSystemLogRequest.from(request));
		case SimulationRequest.METHOD -> this.handleSimulationRequest(edgeId, user, SimulationRequest.from(request));

		default -> {
			// unable to handle; try generic handler
			yield null;
		}
		};

		if (resultFuture == null) {
			return null;
		}

		// Wrap reply in EdgeRpcResponse
		var result = new CompletableFuture<EdgeRpcResponse>();
		resultFuture.whenComplete((r, ex) -> {
			if (ex != null) {
				result.completeExceptionally(ex);
			} else if (r != null) {
				result.complete(new EdgeRpcResponse(edgeRpcRequest.getId(), r));
			} else {
				result.completeExceptionally(
						new OpenemsNamedException(OpenemsError.JSONRPC_UNHANDLED_METHOD, request.getMethod()));
			}
		});
		return result;
	}

	/**
	 * Handles a {@link GetSimulationRequest}.
	 * 
	 * @param edgeId  the Edge-ID
	 * @param user    the {@link User} - no specific level required
	 * @param request the {@link GetSimulationRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSimulationRequest(String edgeId, User user,
			SimulationRequest request) throws OpenemsNamedException {

		final var simulation = this.parent.simulation;
		if (simulation == null) {
			throw new OpenemsException("simulation unavailable");
		}

		return simulation.handleRequest(edgeId, user, request);
	}

	/**
	 * Handles a {@link SubscribeChannelsRequest}.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param edgeId  the Edge-ID
	 * @param user    the {@link User} - no specific level required
	 * @param request the SubscribeChannelsRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeChannelsRequest(WsData wsData, String edgeId,
			User user, SubscribeChannelsRequest request) throws OpenemsNamedException {
		// Register subscription in WsData
		wsData.handleSubscribeChannelsRequest(edgeId, request);

		// JSON-RPC response
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a {@link SubscribeEdgesRequest}.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param request the SubscribeChannelsRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeEdgesRequest(WsData wsData,
			SubscribeEdgesRequest request) throws OpenemsNamedException {
		// Register subscription in WsData
		wsData.handleSubscribeEdgesRequest(request.getEdges());

		// JSON-RPC response
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a {@link SubscribeSystemLogRequest}.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param edgeId  the Edge-ID
	 * @param user    the {@link User}
	 * @param request the {@link SubscribeSystemLogRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(WsData wsData, String edgeId,
			User user, SubscribeSystemLogRequest request) throws OpenemsNamedException {
		user.assertEdgeRoleIsAtLeast(SubscribeSystemLogRequest.METHOD, edgeId, Role.OWNER);

		// Forward to Edge
		return this.parent.edgeWebsocket.handleSubscribeSystemLogRequest(edgeId, user, wsData.getId(), request);
	}

	/**
	 * Handles an {@link AddEdgeToUserRequest}.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link AddEdgeToUserRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<AddEdgeToUserResponse> handleAddEdgeToUserRequest(User user, AddEdgeToUserRequest request)
			throws OpenemsNamedException {
		var edge = this.parent.metadata.addEdgeToUser(user, request.getSetupPassword());
		var serialNumber = this.parent.metadata.getSerialNumberForEdge(edge);

		return CompletableFuture
				.completedFuture(new AddEdgeToUserResponse(request.getId(), edge, serialNumber.orElse(null)));
	}

	/**
	 * Handles an {@link GetEmsTypeRequest}.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link GetEmsTypeRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GetEmsTypeResponse> handleGetEmsTypeRequest(User user, GetEmsTypeRequest request)
			throws OpenemsNamedException {
		if (user.getRole(request.getEdgeId()).isEmpty()) {
			this.parent.metadata.getEdgeMetadataForUser(user, request.getEdgeId());
		}
		user.assertEdgeRoleIsAtLeast(GetEmsTypeRequest.METHOD, request.getEdgeId(), Role.GUEST);
		final var emsType = this.parent.metadata.getEmsTypeForEdge(request.getEdgeId());

		return CompletableFuture.completedFuture(new GetEmsTypeResponse(request.getId(), emsType.orElse(null)));
	}

	/**
	 * Handles a {@link GetUserInformationRequest}.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link GetUserInformationRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GetUserInformationResponse> handleGetUserInformationRequest(User user,
			GetUserInformationRequest request) throws OpenemsNamedException {
		var userInformation = this.parent.metadata.getUserInformation(user);

		return CompletableFuture.completedFuture(new GetUserInformationResponse(request.getId(), userInformation));
	}

	/**
	 * Handles a {@link SetUserInformationRequest}.
	 *
	 * @param user    the {@link User}r
	 * @param request the {@link SetUserInformationRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleSetUserInformationRequest(User user,
			SetUserInformationRequest request) throws OpenemsNamedException {
		this.parent.metadata.setUserInformation(user, request.getJsonObject());

		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a {@link SubmitSetupProtocolRequest}.
	 *
	 * @param user    the {@link User}r
	 * @param request the {@link SubmitSetupProtocolRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleSubmitSetupProtocolRequest(User user,
			SubmitSetupProtocolRequest request) throws OpenemsNamedException {
		var protocolId = this.parent.metadata.submitSetupProtocol(user, request.getJsonObject());

		var response = JsonUtils.buildJsonObject() //
				.addProperty("setupProtocolId", protocolId) //
				.build();

		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId(), response));
	}

	/**
	 * Handles a {@link GetSetupProtocolRequest}.
	 *
	 * @param user    the {@link User}r
	 * @param request the {@link GetSetupProtocolRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<Base64PayloadResponse> handleGetSetupProtocolRequest(User user,
			GetSetupProtocolRequest request) throws OpenemsNamedException {
		var protocol = this.parent.metadata.getSetupProtocol(user, request.getSetupProtocolId());

		return CompletableFuture.completedFuture(new Base64PayloadResponse(request.getId(), protocol));
	}

	/**
	 * Handles a {@link UpdateUserLanguageRequest}.
	 *
	 * @param user    the {@link User}r
	 * @param request the {@link UpdateUserLanguageRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleUpdateUserLanguageRequest(User user,
			UpdateUserLanguageRequest request) throws OpenemsNamedException {
		this.parent.metadata.updateUserLanguage(user, request.getLanguage());

		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a {@link GetSetupProtocolDataRequest}.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link GetSetupProtocolDataRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleGetSetupProtocolDataRequest(User user,
			GetSetupProtocolDataRequest request) throws OpenemsNamedException {
		var latestProtocolJson = this.parent.metadata.getSetupProtocolData(user, request.getEdgeId());

		return CompletableFuture
				.completedFuture(new GenericJsonrpcResponseSuccess(request.getId(), latestProtocolJson));
	}

	/**
	 * Handles a {@link GetUserAlertingConfigsRequest}.
	 *
	 * @param user    {@link User} who called the request
	 * @param request the {@link SetUserAlertingConfigsRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsException on error
	 */
	private CompletableFuture<? extends JsonrpcResponseSuccess> handleGetUserAlertingConfigsRequest(User user,
			GetUserAlertingConfigsRequest request) throws OpenemsException {
		var edgeId = request.getEdgeId();

		UserAlertingSettings currentUser = null;
		List<UserAlertingSettings> otherUser = List.of();

		if (userIsAdmin(user, edgeId)) {
			var allSettings = this.parent.metadata.getUserAlertingSettings(edgeId);

			var userOpt = allSettings.stream() //
					.filter(s -> s.userLogin().equals(user.getId())) //
					.findAny();
			if (userOpt.isPresent()) {
				allSettings.remove(userOpt.get());
				currentUser = userOpt.get();
			}
			otherUser = allSettings;
		} else {
			currentUser = this.parent.metadata.getUserAlertingSettings(edgeId, user.getId());
		}

		if (currentUser == null) {
			currentUser = new UserAlertingSettings(edgeId, user.getId());
		}

		return CompletableFuture.completedFuture(//
				new GetUserAlertingConfigsResponse(request.getId(), currentUser, otherUser));
	}

	/**
	 * Handles a {@link SetUserAlertingConfigsRequest}.
	 *
	 * @param user    {@link User} who called the request
	 * @param request the {@link SetUserAlertingConfigsRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsException      on error
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<? extends JsonrpcResponseSuccess> handleSetUserAlertingConfigsRequest(User user,
			SetUserAlertingConfigsRequest request) throws OpenemsException {
		var edgeId = request.getEdgeId();
		var userId = user.getId();
		var userSettings = request.getUserSettings();

		var containsOtherUsersSettings = userSettings.stream() //
				.anyMatch(u -> !Objects.equals(u.userLogin(), userId));

		if (containsOtherUsersSettings && !userIsAdmin(user, edgeId)) {
			throw new OpenemsException(
					"Not allowed to update/set alerting information for other users as user [" + userId + "]");
		}

		this.parent.metadata.setUserAlertingSettings(user, edgeId, request.getUserSettings());

		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	private static boolean userIsAdmin(User user, String edgeId) {
		return user.getRole(edgeId).map(role -> role.isAtLeast(Role.ADMIN)).orElse(false);
	}

	/**
	 * Handles a {@link GetEdgesRequest}.
	 *
	 * @param user    {@User} who called the request
	 * @param request the {@link GetEdgesRequest}
	 * @return the {@link GetEdgesResponse} Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<? extends JsonrpcResponseSuccess> handleGetEdgesRequest(//
			final User user, //
			final GetEdgesRequest request //
	) throws OpenemsNamedException {
		final var edgeMetadata = this.parent.metadata.getPageDevice(user, request.getPaginationOptions());
		return CompletableFuture.completedFuture(new GetEdgesResponse(request.getId(), edgeMetadata));
	}

	/**
	 * Handles a {@link GetEdgeRequest}.
	 *
	 * @param user    {@User} who called the request
	 * @param request the {@link GetEdgeRequest}
	 * @return the {@link GetEdgeResponse} Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<? extends JsonrpcResponseSuccess> handleGetEdgeRequest(//
			final User user, //
			final GetEdgeRequest request //
	) throws OpenemsNamedException {
		final var edgeMetadata = this.parent.metadata.getEdgeMetadataForUser(user, request.edgeId);
		if (edgeMetadata == null) {
			throw new OpenemsException("Unable to find edge with id [" + request.edgeId + "]");
		}

		return CompletableFuture //
				.completedFuture(new GetEdgeResponse(request.getId(), edgeMetadata));
	}

	/**
	 * Handles a {@link UpdateUserSettingsRequest}.
	 *
	 * @param user    the authenticated {@link User}
	 * @param request the {@link UpdateUserSettingsRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<? extends JsonrpcResponseSuccess> handleUpdateUserSettingsRequest(//
			final User user, //
			final UpdateUserSettingsRequest request //
	) throws OpenemsNamedException {
		this.parent.metadata.updateUserSettings(user, request.getSettings());
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

}
