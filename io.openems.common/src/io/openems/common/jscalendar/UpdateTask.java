package io.openems.common.jscalendar;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

import io.openems.common.jscalendar.JSCalendar.RecurrenceRule;
import io.openems.common.jscalendar.JSCalendar.Task;
import io.openems.common.jscalendar.UpdateTask.Request;
import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

/**
 * Updates the {@link Task}.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": {@link UUID},
 *   "method": "updateTask",
 *   "params": {
 *   	"task": {
 *   	   "@type": "Task",
 *   	   "uid": {@link UUID},
 *   	   "start": {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME},
 *   	   "duration"?: {@link java.time.Duration#toString},
 *   	   "recurrenceRules"?: [{@link RecurrenceRule}],
 *   	   "payload"?: {@link PAYLOAD}	
 *   	}
 *   }
 * }
 * </pre>
 */
public class UpdateTask<PAYLOAD> implements EndpointRequestType<Request<PAYLOAD>, EmptyObject> {

	private final JsonSerializer<PAYLOAD> payloadSerializer;

	/**
	 * Create a {@link UpdateTask} with Payload serializer.
	 * 
	 * @param payloadSerializer a {@link JsonSerializer} for the Payload
	 */
	public UpdateTask(JsonSerializer<PAYLOAD> payloadSerializer) {
		this.payloadSerializer = payloadSerializer;
	}

	/**
	 * Create a {@link UpdateTask} with no (i.e. {@link Void}) Payload.
	 * 
	 * @return {@link UpdateTask}
	 */
	public static UpdateTask<Void> withoutPayload() {
		return new UpdateTask<>(JSCalendar.VOID_SERIALIZER);
	}

	@Override
	public String getMethod() {
		return "updateTask";
	}

	@Override
	public JsonSerializer<Request<PAYLOAD>> getRequestSerializer() {
		return Request.<PAYLOAD>serializer(this.payloadSerializer);
	}

	@Override
	public JsonSerializer<EmptyObject> getResponseSerializer() {
		return EmptyObject.serializer();
	}

	public record Request<PAYLOAD>(//
			Task<PAYLOAD> task) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link UpdateTask.Request}.
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
}
