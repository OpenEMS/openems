package io.openems.common.jscalendar;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

import io.openems.common.jscalendar.AddTask.Request;
import io.openems.common.jscalendar.AddTask.Response;
import io.openems.common.jscalendar.JSCalendar.RecurrenceRule;
import io.openems.common.jscalendar.JSCalendar.Task;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

/**
 * Adds a new {@link Task}.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": {@link UUID},
 *   "method": "addTask",
 *   "params": {
 *   	"task": {
 *   	   "@type": "Task",
 *   	   "start": {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME},
 *   	   "duration"?: {@link java.time.Duration#toString},
 *   	   "recurrenceRules"?: [{@link RecurrenceRule}],
 *   	   "payload"?: {@link PAYLOAD}	
 *   	}
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
 *   "id": {@link UUID},
 *   "result": {
 *     "uid": {@link UUID}
 *   }
 * }
 * </pre>
 */
public class AddTask<PAYLOAD> implements EndpointRequestType<Request<PAYLOAD>, Response> {

	private final JsonSerializer<PAYLOAD> payloadSerializer;

	/**
	 * Create a {@link AddTask} with Payload serializer.
	 * 
	 * @param payloadSerializer a {@link JsonSerializer} for the Payload
	 */
	public AddTask(JsonSerializer<PAYLOAD> payloadSerializer) {
		this.payloadSerializer = payloadSerializer;
	}

	/**
	 * Create a {@link AddTask} with no (i.e. {@link Void}) Payload.
	 * 
	 * @return {@link AddTask}
	 */
	public static AddTask<Void> withoutPayload() {
		return new AddTask<>(JSCalendar.VOID_SERIALIZER);
	}

	@Override
	public String getMethod() {
		return "addTask";
	}

	@Override
	public JsonSerializer<Request<PAYLOAD>> getRequestSerializer() {
		return Request.<PAYLOAD>serializer(this.payloadSerializer);
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Request<PAYLOAD>(//
			Task<PAYLOAD> task) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link AddTask.Request}.
		 * 
		 * @param <PAYLOAD>         the type of the Payload
		 * @param payloadSerializer a {@link JsonSerializer} for the Payload
		 * @return the created {@link JsonSerializer}
		 */
		public static <PAYLOAD> JsonSerializer<Request<PAYLOAD>> serializer(//
				JsonSerializer<PAYLOAD> payloadSerializer) {
			return JsonSerializerUtil.<Request<PAYLOAD>>jsonObjectSerializer(//
					json -> new Request<PAYLOAD>(//
							json.getObject("task", Task.serializer(payloadSerializer))), //
					obj -> JsonUtils.buildJsonObject() //
							.add("task", Task.serializer(payloadSerializer).serialize(obj.task())) //
							.build());
		}
	}

	public record Response(//
			UUID uid) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link AddTask.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Response> serializer() {
			return JsonSerializerUtil.<Response>jsonObjectSerializer(//
					json -> new Response(//
							json.getUuid("uid")), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("uid", obj.uid()) //
							.build());
		}
	}
}
