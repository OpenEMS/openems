package io.openems.edge.weather.openmeteo;

import java.util.List;

import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.weather.api.HourlyWeatherSnapshot;
import io.openems.edge.weather.openmeteo.HourlyWeatherForecastEndpoint.Request;
import io.openems.edge.weather.openmeteo.HourlyWeatherForecastEndpoint.Response;

/**
 * JSON-RPC Request for the "hourlyWeatherForecast" method.
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
 *       "method": "hourlyWeatherForecast",
 *       "params": { 
 *       	"forecastHours": 7
 *       }
 *     }
 *   }
 * }
 * </pre>
 */
public class HourlyWeatherForecastEndpoint implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "hourlyWeatherForecast";
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
			int forecastHours//
	) {

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link HourlyWeatherForecastEndpoint.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<HourlyWeatherForecastEndpoint.Request> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(HourlyWeatherForecastEndpoint.Request.class, //
					json -> new HourlyWeatherForecastEndpoint.Request(//
							json.getInt("forecastHours")), //
					obj -> JsonUtils.buildJsonObject()//
							.addProperty("forecastHours", obj.forecastHours())//
							.build());
		}
	}

	public record Response(//
			List<HourlyWeatherSnapshot> hourlyWeatherForecast//
	) {
		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link HourlyWeatherForecastEndpoint.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<HourlyWeatherForecastEndpoint.Response> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(HourlyWeatherForecastEndpoint.Response.class, //
					json -> {
						var hourlyWeatherForecast = json.getList("hourlyWeatherForecast",
								HourlyWeatherSnapshot.serializer());
						return new Response(hourlyWeatherForecast);
					}, obj -> JsonUtils.buildJsonObject()//
							.add("hourlyWeatherForecast", HourlyWeatherSnapshot.serializer().toListSerializer()//
									.serialize(obj.hourlyWeatherForecast()))//
							.build());
		}
	}
}
