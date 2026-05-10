package io.openems.backend.metadata.odoo.odoo.http;

import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.util.List;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;

public record OdooGetEdgesResponse(//
		List<OdooDeviceData> devices //
) {

	/**
	 * Returns a {@link JsonSerializer} for a {@link OdooGetEdgesResponse}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<OdooGetEdgesResponse> serializer() {
		return JsonSerializerUtil.jsonObjectSerializer(OdooGetEdgesResponse.class,
				json -> new OdooGetEdgesResponse(json.getList("devices", OdooDeviceData.serializer())),
				obj -> buildJsonObject() //
						.add("devices", OdooDeviceData.serializer().toListSerializer() //
								.serialize(obj.devices())) //
						.build());
	}

}