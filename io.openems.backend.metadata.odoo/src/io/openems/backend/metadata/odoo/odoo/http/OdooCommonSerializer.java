package io.openems.backend.metadata.odoo.odoo.http;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import com.google.gson.JsonPrimitive;

import io.openems.backend.metadata.odoo.odoo.OdooUtils;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.jsonrpc.serialization.StringParser;

public final class OdooCommonSerializer {

	/**
	 * Returns a {@link JsonSerializer} for a {@link ZonedDateTime} from odoo api.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<ZonedDateTime> serializerOdooZonedDateTime() {
		return JsonSerializerUtil.jsonSerializer(ZonedDateTime.class, json -> {
			return json.getAsStringParsed(//
					OdooUtils.DateTime::stringToDateTime, //
					() -> new StringParser.ExampleValues<>("2020-01-01 01:00:00", ZonedDateTime
							.of(LocalDate.of(2020, 1, 1), LocalTime.of(1, 0), OdooUtils.DateTime.SERVER_TIMEZONE)) //
			);
		}, obj -> new JsonPrimitive(obj.format(OdooUtils.DateTime.DATETIME_FORMATTER)));
	}

	private OdooCommonSerializer() {
	}
}
