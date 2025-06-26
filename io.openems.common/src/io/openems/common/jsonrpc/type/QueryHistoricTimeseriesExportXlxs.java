package io.openems.common.jsonrpc.type;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.type.QueryHistoricTimeseriesExportXlxs.Request;
import io.openems.common.utils.JsonUtils;

public class QueryHistoricTimeseriesExportXlxs implements EndpointRequestType<Request, Base64RequestType> {

	@Override
	public String getMethod() {
		return "queryHistoricTimeseriesExportXlxs";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Base64RequestType> getResponseSerializer() {
		return Base64RequestType.serializer();
	}

	public record Request(//
			ZonedDateTime fromDate, //
			ZonedDateTime toDate //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link QueryHistoricTimeseriesExportXlxs}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, json -> {
				final var timezone = json.getObject("timezone", QueryHistoricTimeseriesData.zoneIdSerializer());

				return new Request(//
						json.getLocalDate("fromDate").atStartOfDay(timezone), //
						json.getLocalDate("toDate").atStartOfDay(timezone).plusDays(1) //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("fromDate", obj.fromDate().format(DateTimeFormatter.ISO_ZONED_DATE_TIME)) //
						.addProperty("toDate", obj.toDate().minusDays(1).format(DateTimeFormatter.ISO_ZONED_DATE_TIME)) //
						.add("timezone",
								QueryHistoricTimeseriesData.zoneIdSerializer().serialize(obj.fromDate().getZone())) //
						.build();
			});
		}

	}

}
