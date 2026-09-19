package io.openems.backend.metadata.odoo.odoo.http;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

public record OdooSetEdgeSettingsRequest(//
		String edgeId, //
		JsonObject settings //
) {

	/**
	 * Returns a {@link JsonSerializer} for a {@link OdooSetEdgeSettingsRequest}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<OdooSetEdgeSettingsRequest> serializer() {
		return JsonSerializerUtil.jsonObjectSerializer(OdooSetEdgeSettingsRequest.class,
				json -> new OdooSetEdgeSettingsRequest(//
						json.getString("edgeId"), //
						json.getJsonObject("settings")), //
				obj -> JsonUtils.buildJsonObject() //
						.addProperty("edgeId", obj.edgeId()) //
						.add("settings", obj.settings())//
						.build());
	}

}
