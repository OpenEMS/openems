package io.openems.backend.b2bwebsocket;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.BackendUser;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	private final Logger log = LoggerFactory.getLogger(OnClose.class);
	private final B2bWebsocket parent;

	public OnOpen(B2bWebsocket parent) {
		this.parent = parent;
	}

	@Override
	public void run(WebSocket ws, JsonObject handshake) throws OpenemsNamedException {
		try {
			// Read "Authorization" header for Simple HTTP authentication. Source:
			// https://stackoverflow.com/questions/16000517/how-to-get-password-from-http-basic-authentication
			final String authorization = JsonUtils.getAsString(handshake, "Authorization");
			if (authorization == null || !authorization.toLowerCase().startsWith("basic")) {
				throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
			}

			String base64Credentials = authorization.substring("Basic".length()).trim();
			byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
			String credentials = new String(credDecoded, StandardCharsets.UTF_8);
			// credentials = username:password
			final String[] values = credentials.split(":", 2);
			if (values.length != 2) {
				throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
			}
			String username = values[0];
			String password = values[1];

			// TODO handle dat case
			// BackendUser user = this.parent.metadata.authenticate(username, password);
			// WsData wsData = ws.getAttachment();
			// wsData.setUser(user);
			//this.parent.logInfo(this.log, "User [" + user.getName() + "] logged in");

		} catch (OpenemsNamedException e) {
			ws.close();
			throw e;
		}
	}

}
