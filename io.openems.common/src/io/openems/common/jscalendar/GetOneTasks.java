package io.openems.common.jscalendar;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.google.common.collect.ImmutableList;

import io.openems.common.jscalendar.GetOneTasks.Request;
import io.openems.common.jscalendar.GetOneTasks.Response;
import io.openems.common.jscalendar.JSCalendar.Tasks;
import io.openems.common.jscalendar.JSCalendar.Tasks.OneTask;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.jsonrpc.type.QueryHistoricTimeseriesData;
import io.openems.common.utils.JsonUtils;

/**
 * Gets the next {@link OneTask}s.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getOneTasks",
 *   "params": {
 *   	"from"?: {@link DateTimeFormatter#ISO_ZONED_DATE_TIME},
 *   	"to": {@link DateTimeFormatter#ISO_ZONED_DATE_TIME}
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
 *     "oneTasks": [{@link OneTask}]
 *   }
 * }
 * </pre>
 */
public class GetOneTasks<PAYLOAD> implements EndpointRequestType<Request, Response<PAYLOAD>> {

	private final JsonSerializer<PAYLOAD> payloadSerializer;

	/**
	 * Create a {@link GetOneTasks} with Payload serializer.
	 * 
	 * @param payloadSerializer a {@link JsonSerializer} for the Payload
	 */
	public GetOneTasks(JsonSerializer<PAYLOAD> payloadSerializer) {
		this.payloadSerializer = payloadSerializer;
	}

	/**
	 * Create a {@link GetOneTasks} with no (i.e. {@link Void}) Payload.
	 * 
	 * @return {@link GetOneTasks}
	 */
	public static GetOneTasks<Void> withoutPayload() {
		return new GetOneTasks<>(JSCalendar.VOID_SERIALIZER);
	}

	@Override
	public String getMethod() {
		return "getOneTasks";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Response<PAYLOAD>> getResponseSerializer() {
		return Response.<PAYLOAD>serializer(this.payloadSerializer);
	}

	public record Request(//
			ZonedDateTime from, //
			ZonedDateTime to //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link QueryHistoricTimeseriesData}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, json -> {
				return new Request(//
						json.getZonedDateTime("from"), //
						json.getZonedDateTime("to")); //
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("from", obj.from()) //
						.addProperty("to", obj.to()) //
						.build();
			});
		}
	}

	public record Response<PAYLOAD>(//
			ImmutableList<OneTask<PAYLOAD>> oneTasks //
	) {

		/**
		 * Creates a {@link Response}.
		 * 
		 * @param <PAYLOAD> the type of the Payload
		 * @param request   the {@link Request}
		 * @param tasks     the {@link Tasks}
		 * @return the created {@link Response}
		 */
		public static <PAYLOAD> Response<PAYLOAD> create(Request request, JSCalendar.Tasks<PAYLOAD> tasks) {
			var ots = tasks.getOneTasksBetween(request.from, request.to);
			return new Response<PAYLOAD>(ImmutableList.copyOf(ots));
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetApp.Response}.
		 * 
		 * @param <PAYLOAD>         the type of the Payload
		 * @param payloadSerializer a {@link JsonSerializer} for the Payload
		 * @return the created {@link JsonSerializer}
		 */
		public static <PAYLOAD> JsonSerializer<Response<PAYLOAD>> serializer(
				JsonSerializer<PAYLOAD> payloadSerializer) {
			return JsonSerializerUtil.<Response<PAYLOAD>>jsonObjectSerializer(//
					json -> new Response<PAYLOAD>(//
							json.getImmutableList("oneTasks", OneTask.serializer(payloadSerializer))),
					obj -> JsonUtils.buildJsonObject() //
							.add("oneTasks", OneTask.listSerializer(payloadSerializer).serialize(obj.oneTasks)) //
							.build());
		}
	}
}
