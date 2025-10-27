package io.openems.edge.weather.openmeteo;

import java.util.List;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.weather.api.DailyWeatherSnapshot;
import io.openems.edge.weather.openmeteo.DailyWeatherForecastEndpoint.Response;

/**
 * JSON-RPC Request for the "dailyWeatherForecast" method.
 *
 * <p>
 * Example request:
 * 
 * <pre>
 * {
 *   "method": "componentJsonApi",
 *   "params": {
 *     "componentId": "weather0",
 *     "payload": {
 *       "method": "dailyWeatherForecast",
 *       "params": { }
 *     }
 *   }
 * }
 * </pre>
 */
public class DailyWeatherForecastEndpoint implements EndpointRequestType<EmptyObject, Response> {

	@Override
	public String getMethod() {
		return "dailyWeatherForecast";
	}

	@Override
	public JsonSerializer<EmptyObject> getRequestSerializer() {
		return EmptyObject.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Response(//
			List<DailyWeatherSnapshot> dailyWeatherForecast//
	) {
		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link DailyWeatherForecastEndpoint.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<DailyWeatherForecastEndpoint.Response> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(DailyWeatherForecastEndpoint.Response.class, //
					json -> {
						var dailyWeatherForecast = json.getList("dailyWeatherForecast",
								DailyWeatherSnapshot.serializer());
						return new Response(dailyWeatherForecast);
					}, obj -> JsonUtils.buildJsonObject()//
							.add("dailyWeatherForecast", DailyWeatherSnapshot.serializer().toListSerializer()//
									.serialize(obj.dailyWeatherForecast()))//
							.build());
		}
	}
}
