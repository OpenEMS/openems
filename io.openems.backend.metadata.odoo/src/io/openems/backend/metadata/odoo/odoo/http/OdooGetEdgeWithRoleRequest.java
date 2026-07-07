package io.openems.backend.metadata.odoo.odoo.http;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

public record OdooGetEdgeWithRoleRequest(//
		String externalUserId, //
		String edgeId //
) {

	/**
	 * Returns a {@link JsonSerializer} for a {@link OdooGetEdgeWithRoleRequest}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<OdooGetEdgeWithRoleRequest> serializer() {
		return JsonSerializerUtil.jsonObjectSerializer(OdooGetEdgeWithRoleRequest.class,
				json -> new OdooGetEdgeWithRoleRequest(//
						json.getString("external_uid"), //
						json.getString("edge_id") //
				), obj -> JsonUtils.buildJsonObject() //
						.addProperty("external_uid", obj.externalUserId()) //
						.addProperty("edge_id", obj.edgeId()) //
						.build());
	}

}
