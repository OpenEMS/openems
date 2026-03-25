package io.openems.backend.metadata.odoo.odoo.http;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

public record OdooGetUserInfoRequest(//
		String externalUserId //
) {

	/**
	 * Returns a {@link JsonSerializer} for a {@link OdooGetUserInfoRequest}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<OdooGetUserInfoRequest> serializer() {
		return JsonSerializerUtil.jsonObjectSerializer(OdooGetUserInfoRequest.class,
				json -> new OdooGetUserInfoRequest(json.getString("external_uid")), //
				obj -> JsonUtils.buildJsonObject() //
						.addProperty("external_uid", obj.externalUserId()) //
						.build());
	}

}
