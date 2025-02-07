package io.openems.common.jsonrpc.type;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static io.openems.common.utils.JsonUtils.toJsonObject;
import static java.util.stream.Collectors.toMap;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.StringPathParser.StringParserChannelAddress;
import io.openems.common.jsonrpc.type.QueryHistoricTimeseriesEnergy.Request;
import io.openems.common.jsonrpc.type.QueryHistoricTimeseriesEnergy.Response;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;

public class QueryHistoricTimeseriesEnergy implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "queryHistoricTimeseriesEnergy";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Request(//
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link QueryHistoricTimeseriesEnergy}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, json -> {
				final var timezone = json.getObject("timezone", QueryHistoricTimeseriesData.zoneIdSerializer());

				return new Request(//
						json.getLocalDate("fromDate").atStartOfDay(timezone), //
						json.getLocalDate("toDate").atStartOfDay(timezone).plusDays(1), //
						json.getSet("channels", JsonElementPath::getAsChannelAddress) //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("fromDate", obj.fromDate().format(DateTimeFormatter.ISO_ZONED_DATE_TIME)) //
						.addProperty("toDate", obj.toDate().minusDays(1).format(DateTimeFormatter.ISO_ZONED_DATE_TIME)) //
						.add("channels", obj.channels().stream() //
								.map(Object::toString) //
								.map(JsonPrimitive::new) //
								.collect(toJsonArray())) //
						.add("timezone",
								QueryHistoricTimeseriesData.zoneIdSerializer().serialize(obj.fromDate().getZone())) //
						.build();
			});
		}

	}

	public record Response(Map<ChannelAddress, JsonElement> data) {

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link QueryHistoricTimeseriesEnergy.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<QueryHistoricTimeseriesEnergy.Response> serializer() {
			return jsonObjectSerializer(QueryHistoricTimeseriesEnergy.Response.class, json -> {
				return new Response(json.getJsonObjectPath("data").collect(new StringParserChannelAddress(),
						toMap(t -> t.getKey().get(), t -> t.getValue().get())));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.add("data", obj.data().entrySet().stream() //
								.collect(toJsonObject(t -> t.getKey().toString(), t -> t.getValue()))) //
						.build();
			});
		}

	}

}
