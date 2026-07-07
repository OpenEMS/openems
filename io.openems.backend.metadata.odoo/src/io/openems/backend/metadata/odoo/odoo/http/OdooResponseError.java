package io.openems.backend.metadata.odoo.odoo.http;

import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.util.List;

import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

public record OdooResponseError(//
		int code, //
		String message, //
		String dataName, //
		String dataDebug, //
		String dataMessage, //
		List<String> dataArguments, //
		String dataExceptionType //
) {

	/**
	 * Returns a {@link JsonSerializer} for a {@link OdooResponseError}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<OdooResponseError> serializer() {
		return JsonSerializerUtil.jsonObjectSerializer(OdooResponseError.class, json -> {
			final var data = json.getJsonObjectPath("data");
			return new OdooResponseError(//
					json.getInt("code"), //
					json.getString("message"), //
					data.getString("name"), //
					data.getString("debug"), //
					data.getString("message"), //
					data.getList("arguments", JsonElementPath::getAsString), //
					data.getStringOrNull("exception_type") //
			);
		}, obj -> buildJsonObject() //
				.addProperty("code", obj.code()) //
				.addProperty("message", obj.message()) //
				.add("data", buildJsonObject() //
						.addProperty("name", obj.dataName()) //
						.addProperty("debug", obj.dataDebug()) //
						.addProperty("message", obj.dataMessage()) //
						.add("arguments", obj.dataArguments().stream() //
								.map(JsonPrimitive::new) //
								.collect(JsonUtils.toJsonArray())) //
						.addPropertyIfNotNull("exception_type", obj.dataExceptionType()) //
						.build()) //
				.build());
	}

}