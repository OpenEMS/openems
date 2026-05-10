package io.openems.backend.b2bwebsocket;

import static io.openems.common.websocket.WebsocketUtils.getAsString;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.Handshakedata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.authentication.api.AuthUserPasswordAuthenticationService;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	private final Logger log = LoggerFactory.getLogger(OnOpen.class);
	private final Supplier<Metadata> metadata;
	private final Supplier<AuthUserPasswordAuthenticationService> userAuthService;
	private final BiConsumer<Logger, String> logInfo;

	public OnOpen(//
			Supplier<Metadata> metadata, //
			Supplier<AuthUserPasswordAuthenticationService> userAuthService, //
			BiConsumer<Logger, String> logInfo) {
		this.metadata = metadata;
		this.userAuthService = userAuthService;
		this.logInfo = logInfo;
	}

	@Override
	public OpenemsError apply(WebSocket ws, Handshakedata handshakedata) {
		var error = this.doApply(ws, handshakedata);
		if (error != null) {
			ws.close();
		}
		return error;
	}

	private OpenemsError doApply(WebSocket ws, Handshakedata handshakedata) {
		// Read "Authorization" header for Simple HTTP authentication. Source:
		// https://stackoverflow.com/questions/16000517/how-to-get-password-from-http-basic-authentication
		final var authorization = getAsString(handshakedata, "authorization");
		if (authorization == null || !authorization.toLowerCase().startsWith("basic")) {
			return OpenemsError.COMMON_AUTHENTICATION_FAILED;
		}

		var base64Credentials = authorization.substring("Basic".length()).trim();
		var credDecoded = Base64.getDecoder().decode(base64Credentials);
		var credentials = new String(credDecoded, StandardCharsets.UTF_8);
		// credentials = username:password
		final var values = credentials.split(":", 2);
		if (values.length != 2) {
			return OpenemsError.COMMON_AUTHENTICATION_FAILED;
		}
		var username = values[0];
		var password = values[1];
		final var metadata = this.metadata.get();
		if (metadata == null) {
			return OpenemsError.COMMON_SERVICE_NOT_AVAILABLE;
		}
		User user;
		try {
			final var authResult = this.userAuthService.get().authenticateWithPassword(username, password).get(30,
					TimeUnit.SECONDS);
			user = this.metadata.get().getUserByExternalId(authResult.userId()).get(30, TimeUnit.SECONDS);
		} catch (ExecutionException | TimeoutException e) {
			return OpenemsError.COMMON_AUTHENTICATION_FAILED;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return OpenemsError.COMMON_AUTHENTICATION_FAILED;
		}

		WsData wsData = ws.getAttachment();
		wsData.setUser(user);
		this.logInfo.accept(this.log, "User [" + user.getName() + "] logged in");

		return null; // No error
	}

}
