package io.openems.edge.energy;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.toJsonArray;

import java.time.ZonedDateTime;
import java.util.Objects;

import com.google.common.collect.ImmutableList;

import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.type.AuthenticateWithToken;
import io.openems.edge.energy.GetSchedule.Request;
import io.openems.edge.energy.GetSchedule.Response;
import io.openems.common.jsonrpc.type.ScheduleUpdate.Period;

/**
 * Gets a Schedule.
 *
 * <ul>
 * <li>If 'from' is '00:00': gets one full day
 * <li>Otherwise: gets two full days
 * </ul>
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getSchedule",
 *   "params": {
 *     "from": ZonedDateTime
 *   }
 * }
 * </pre>
 *
 * <p>
 * Response:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "data": [{
 *       ...
 *     }]
 *   }
 * }
 * </pre>
 */
public class GetSchedule implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "getSchedule";
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
			ZonedDateTime from //
	) {

		public Request {
			Objects.requireNonNull(from);
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link AuthenticateWithToken}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, //
					json -> new Request(//
							json.getZonedDateTime("from")), //
					obj -> buildJsonObject() //
							.addProperty("from", obj.from()) //
							.build());
		}
	}

	public record Response(//
			ImmutableList<Period> periods //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetSchedule.Response}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Response> serializer() {
			return jsonObjectSerializer(Response.class, //
					json -> new Response(//
							json.getImmutableList("data", Period.serializer())), //
					obj -> buildJsonObject() //
							.add("data", obj.periods().stream() //
									.map(Period.serializer()::serialize) //
									.collect(toJsonArray())) //
							.build());
		}
	}
}
