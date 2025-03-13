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
import io.openems.common.session.Language;
import io.openems.common.session.Role;
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

	private final Map<String, User> sessionTokens = new ConcurrentHashMap<>();

	@Reference
	private UserService userService;

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(AuthenticateWithTokenRequest.METHOD, call -> {
			final var request = AuthenticateWithTokenRequest.from(call.getRequest());
			var token = request.getToken();

			return this.handleAuthentication(call.get(OnRequest.WS_DATA_KEY), request.getId(),
					Optional.ofNullable(this.sessionTokens.get(token)), token);
		});

		builder.handleRequest(AuthenticateWithPasswordRequest.METHOD, call -> {
			final var request = AuthenticateWithPasswordRequest.from(call.getRequest());
			
            Optional<User> user = request.usernameOpt.isPresent() ?
                this.userService.authenticate(request.usernameOpt.get(), request.password) :
                this.userService.authenticate(request.password);
			
            return this.handleAuthentication(
                call.get(OnRequest.WS_DATA_KEY),
                request.getId(),
                user,
                UUID.randomUUID().toString()
            );	
		});

		builder.handleRequest(LogoutRequest.METHOD, endpoint -> {
			endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.GUEST));
		}, call -> {
			final var wsData = call.get(OnRequest.WS_DATA_KEY);
			this.sessionTokens.remove(wsData.getSessionToken(), call.get(EdgeKeys.USER_KEY));
			wsData.logout();
			return new GenericJsonrpcResponseSuccess(call.getRequest().getId());
		});
	}

	/**
	 * Common handler for {@link AuthenticateWithTokenRequest} and
	 * {@link AuthenticateWithPasswordRequest}.
	 *
	 * @param wsData    the WebSocket attachment
	 * @param requestId the ID of the original {@link JsonrpcRequest}
	 * @param userOpt   the optional {@link User}
	 * @param token     the existing or new token
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private JsonrpcResponseSuccess handleAuthentication(//
			WsData wsData, //
			UUID requestId, //
			Optional<User> userOpt, //
			String token //
	) throws OpenemsNamedException {
		if (userOpt.isEmpty()) {
			wsData.unsetUser();
			throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
		}
		final var user = userOpt.get();
		wsData.setSessionToken(token);
		wsData.setUser(user);
		this.sessionTokens.put(token, user);
		this.log.info("User [" + user.getId() + ":" + user.getName() + "] connected.");

		return new AuthenticateResponse(requestId, token, user, Language.DEFAULT);
	}

}
