package io.openems.edge.controller.api.websocket.handler;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.AuthenticateWithPasswordRequest;
import io.openems.common.jsonrpc.request.AuthenticateWithTokenRequest;
import io.openems.common.jsonrpc.request.LogoutRequest;
import io.openems.common.jsonrpc.response.AuthenticateResponse;
import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.edge.common.jsonapi.CreateAccountFromSetupKey;
import io.openems.edge.common.jsonapi.EdgeGuards;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.user.User;
import io.openems.edge.common.user.UserService;
import io.openems.edge.controller.api.websocket.OnRequest;
import io.openems.edge.controller.api.websocket.WsData;

@Component(property = "entry=" + RootRequestHandler.ENTRY_POINT)
public class AuthenticationRequestHandler implements JsonApi {

	private final Logger log = LoggerFactory.getLogger(AuthenticationRequestHandler.class);

	private final Map<String, String /* userid */> sessionTokens = new ConcurrentHashMap<>();

	@Reference
	private UserService userService;

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(AuthenticateWithTokenRequest.METHOD, call -> {
			final var request = AuthenticateWithTokenRequest.from(call.getRequest());
			var token = request.getToken();

			final var user = Optional.ofNullable(this.sessionTokens.get(token)) //
					.flatMap(this.userService::getUserById) //
					.orElse(null);

			return this.handleAuthentication(call.get(OnRequest.WS_DATA_KEY), request.getId(), user, token);
		});

		builder.handleRequest(AuthenticateWithPasswordRequest.METHOD, call -> {
			final var request = AuthenticateWithPasswordRequest.from(call.getRequest());

			return this.handleAuthentication(call.get(OnRequest.WS_DATA_KEY), request.getId(),
					this.userService.authenticate(request.password).orElse(null), UUID.randomUUID().toString());
		});

		builder.handleRequest(LogoutRequest.METHOD, endpoint -> {
			endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.GUEST));
		}, call -> {
			final var wsData = call.get(OnRequest.WS_DATA_KEY);
			this.sessionTokens.remove(wsData.getSessionToken(), call.get(EdgeKeys.USER_KEY));
			wsData.logout();
			return new GenericJsonrpcResponseSuccess(call.getRequest().getId());
		});

		builder.handleRequest(new CreateAccountFromSetupKey(), endpoint -> {
			endpoint.setDescription("""
					Creates a new account if the provided setup key is the setup key of this edge.
					""");
		}, call -> {
			final var request = call.getRequest();
			this.userService.registerAdminUser(request.setupKey(), request.username(), request.password(),
					Language.DEFAULT);
			return EmptyObject.INSTANCE;
		});

	}

	/**
	 * Common handler for {@link AuthenticateWithTokenRequest} and
	 * {@link AuthenticateWithPasswordRequest}.
	 *
	 * @param wsData    the WebSocket attachment
	 * @param requestId the ID of the original {@link JsonrpcRequest}
	 * @param user      the {@link User}; nullable
	 * @param token     the existing or new token
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private JsonrpcResponseSuccess handleAuthentication(//
			WsData wsData, //
			UUID requestId, //
			User user, //
			String token //
	) throws OpenemsNamedException {
		if (user == null) {
			wsData.unsetUser();
			throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
		}
		wsData.setSessionToken(token);
		wsData.setUser(user);
		this.sessionTokens.put(token, user.getId());
		this.log.info("User [{}:{}] connected.", user.getId(), user.getName());

		return new AuthenticateResponse(requestId, token, user, user.getLanguage());
	}

}
