package io.openems.backend.metadata.odoo.odoo.http;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

public record OdooGetUserInfoResponse(//
		int odooUserId, //
		String login, //
		String name, //
		Language language, //
		Role globalRole, //
		boolean hasMultipleEdges, //
		JsonObject settings //
) {

	/**
	 * Returns a {@link JsonSerializer} for a {@link OdooGetUserInfoResponse}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<OdooGetUserInfoResponse> serializer() {
		return JsonSerializerUtil.jsonObjectSerializer(OdooGetUserInfoResponse.class, json -> {
			final var user = json.getJsonObjectPath("user");
			return new OdooGetUserInfoResponse(//
					user.getInt("id"), //
					user.getString("login"), //
					user.getString("name"), //
					user.getEnum("language", Language.class), //
					user.getEnum("global_role", Role.class), //
					user.getBoolean("has_multiple_edges"), //
					user.getOptionalString("settings") //
							.flatMap(JsonUtils::parseOptional) //
							.flatMap(JsonUtils::getAsOptionalJsonObject) //
							.orElse(new JsonObject()) //
			);
		}, obj -> JsonUtils.buildJsonObject() //
				.addProperty("id", obj.odooUserId()) //
				.addProperty("login", obj.login()) //
				.addProperty("name", obj.name()) //
				.addProperty("language", obj.language()) //
				.addProperty("global_role", obj.globalRole()) //
				.addProperty("has_multiple_edges", obj.hasMultipleEdges()) //
				.addProperty("settings", obj.settings().toString()) //
				.build());
	}

}