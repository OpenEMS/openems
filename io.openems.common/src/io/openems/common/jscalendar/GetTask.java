package io.openems.common.jscalendar;

import java.util.UUID;

import io.openems.common.jscalendar.GetTask.Request;
import io.openems.common.jscalendar.GetTask.Response;
import io.openems.common.jscalendar.JSCalendar.Task;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

/**
 * Gets the {@link Task} with the specified {@link UUID}.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": {@link UUID},
 *   "method": "getTask",
 *   "params": {
 *   	"uid": {@link UUID}
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
 *     "task": {@link Task}
 *   }
 * }
 * </pre>
 */
public class GetTask<PAYLOAD> implements EndpointRequestType<Request, Response<PAYLOAD>> {

	private final JsonSerializer<PAYLOAD> payloadSerializer;

	/**
	 * Create a {@link GetTask} with Payload serializer.
	 * 
	 * @param payloadSerializer a {@link JsonSerializer} for the Payload
	 */
	public GetTask(JsonSerializer<PAYLOAD> payloadSerializer) {
		this.payloadSerializer = payloadSerializer;
	}

	/**
	 * Create a {@link GetTask} with no (i.e. {@link Void}) Payload.
	 * 
	 * @return {@link GetTask}
	 */
	public static GetTask<Void> withoutPayload() {
		return new GetTask<>(JSCalendar.VOID_SERIALIZER);
	}

	@Override
	public String getMethod() {
		return "getTask";
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
			UUID uid) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link DeleteTask.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return JsonSerializerUtil.<Request>jsonObjectSerializer(//
					json -> new Request(//
							json.getUuidOrNull("uid")), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("uid", obj.uid()) //
							.build());
		}
	}

	public record Response<PAYLOAD>(//
			Task<PAYLOAD> task) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetTask.Response}.
		 * 
		 * @param <PAYLOAD>         the type of the Payload
		 * @param payloadSerializer a {@link JsonSerializer} for the Payload
		 * @return the created {@link JsonSerializer}
		 */
		public static <PAYLOAD> JsonSerializer<Response<PAYLOAD>> serializer(
				JsonSerializer<PAYLOAD> payloadSerializer) {
			return JsonSerializerUtil.<Response<PAYLOAD>>jsonObjectSerializer(//
					json -> new Response<PAYLOAD>(//
							json.getObject("task", Task.serializer(payloadSerializer))),
					obj -> JsonUtils.buildJsonObject() //
							.add("task", Task.serializer(payloadSerializer).serialize(obj.task())) //
							.build());
		}
	}
}
