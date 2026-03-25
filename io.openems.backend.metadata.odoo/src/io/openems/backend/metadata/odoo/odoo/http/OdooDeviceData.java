package io.openems.backend.metadata.odoo.odoo.http;

import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.time.ZonedDateTime;
import java.util.List;

import com.google.gson.JsonObject;

import io.openems.common.channel.Level;
import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.session.Role;

public record OdooDeviceData(//
		String name, //
		String comment, //
		String producttype, //
		Role role, //
		ZonedDateTime lastmessage, // nullable
		Level sumState, //
		ZonedDateTime firstSetupProtocol, // nullable
		JsonObject settings // nullable
) {

	/**
	 * Returns a {@link JsonSerializer} for a {@link OdooDeviceData}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<OdooDeviceData> serializer() {
		return JsonSerializerUtil.jsonObjectSerializer(OdooDeviceData.class, json -> {

			final var comment = json.getNullableJsonElementPath("comment")
					.mapIfPresentOptional(path -> path.multiple(List.of(//
							new JsonElementPath.Case<>(JsonElementPath::isBoolean, t -> null), //
							new JsonElementPath.Case<>(t -> true /* default */, JsonElementPath::getAsString) //
			))).orElse("");

			final var productType = json.getNullableJsonElementPath("producttype")
					.mapIfPresentOptional(path -> path.multiple(List.of(//
							new JsonElementPath.Case<>(JsonElementPath::isBoolean, t -> null), //
							new JsonElementPath.Case<>(t -> true /* default */, JsonElementPath::getAsString) //
			))).orElse("");

			final var lastMessage = json.getNullableJsonElementPath("lastmessage")
					.mapIfPresent(path -> path.multiple(List.of(//
							new JsonElementPath.Case<>(JsonElementPath::isBoolean, t -> null), //
							new JsonElementPath.Case<>(t -> true /* default */,
									t -> t.getAsObject(OdooCommonSerializer.serializerOdooZonedDateTime())) //
			)));

			final var level = json.getNullableJsonElementPath("openems_sum_state_level")
					.mapIfPresentOptional(path -> path.multiple(List.of(//
							new JsonElementPath.Case<>(JsonElementPath::isBoolean, t -> null), //
							new JsonElementPath.Case<>(t -> true /* default */, t -> t.getAsEnum(Level.class)) //
			))).orElse(Level.OK);

			return new OdooDeviceData(//
					json.getString("name"), //
					comment, //
					productType, //
					json.getEnum("role", Role.class), //
					lastMessage, //
					level, //
					json.getObjectOrNull("first_setup_protocol_date",
							OdooCommonSerializer.serializerOdooZonedDateTime()), //
					json.getNullableJsonObjectPath("settings").getOrNull() //
			);
		}, obj -> {
			return buildJsonObject() //
					.addProperty("name", obj.name()) //
					.addProperty("comment", obj.comment()) //
					.addProperty("producttype", obj.producttype()) //
					.addProperty("role", obj.role()) //
					.addPropertyIfNotNull("lastmessage", obj.lastmessage()) //
					.addPropertyIfNotNull("openems_sum_state_level", obj.sumState()) //
					.addPropertyIfNotNull("first_setup_protocol_date", obj.firstSetupProtocol()) //
					.addIfNotNull("settings", obj.settings()) //
					.build();
		});
	}

}