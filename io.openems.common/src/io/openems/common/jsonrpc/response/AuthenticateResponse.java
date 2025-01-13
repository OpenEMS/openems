package io.openems.common.jsonrpc.response;

import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.AuthenticateWithPasswordRequest;
import io.openems.common.jsonrpc.request.AuthenticateWithTokenRequest;
import io.openems.common.session.AbstractUser;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for {@link AuthenticateWithPasswordRequest} or
 * {@link AuthenticateWithTokenRequest}.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "token": String,
 *     "user": {@link AbstractUser#toJsonObject()}
 *   }
 * }
 * </pre>
 */
public class AuthenticateResponse extends JsonrpcResponseSuccess {

	private final String token;
	private final AbstractUser user;
	private final Language language;

	public AuthenticateResponse(UUID id, String token, AbstractUser user, Language language) {
		super(id);
		this.token = token;
		this.user = user;
		this.language = language;
	}

	/**
	 * This method formats the {@link AuthenticateResponse} so that it contains the
	 * required information for OpenEMS UI.
	 */
	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.addProperty("token", this.token) //
				.add("user", JsonUtils.buildJsonObject() //
						.addProperty("id", this.user.getId()) //
						.addProperty("name", this.user.getName()) //
						.addProperty("language", this.language.name())//
						.addProperty("hasMultipleEdges", this.user.hasMultipleEdges())//
						.add("settings", this.user.getSettings()) //
						.add("globalRole", this.user.getGlobalRole().asJson()) //
						.build()) //
				// still kept to avoid NPE in UI for older versions
				.add("edges", new JsonArray()) //
				.build();
	}

}
