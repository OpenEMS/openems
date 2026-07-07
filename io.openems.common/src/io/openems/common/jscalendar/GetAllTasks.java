package io.openems.common.jscalendar;

import java.util.UUID;

import com.google.common.collect.ImmutableList;

import io.openems.common.jscalendar.GetAllTasks.Response;
import io.openems.common.jscalendar.JSCalendar.Task;
import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

/**
 * Gets all {@link Task}s.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": {@link UUID},
 *   "method": "getAllTasks",
 *   "params": {}
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
 *     "tasks": [{@link Task}]
 *   }
 * }
 * </pre>
 */
public class GetAllTasks<PAYLOAD> implements EndpointRequestType<EmptyObject, Response<PAYLOAD>> {

	private final JsonSerializer<PAYLOAD> payloadSerializer;

	/**
	 * Create a {@link GetAllTasks} with Payload serializer.
	 * 
	 * @param payloadSerializer a {@link JsonSerializer} for the Payload
	 */
	public GetAllTasks(JsonSerializer<PAYLOAD> payloadSerializer) {
		this.payloadSerializer = payloadSerializer;
	}

	/**
	 * Create a {@link GetAllTasks} with no (i.e. {@link Void}) Payload.
	 * 
	 * @return {@link GetAllTasks}
	 */
	public static GetAllTasks<Void> withoutPayload() {
		return new GetAllTasks<>(JSCalendar.VOID_SERIALIZER);
	}

	@Override
	public String getMethod() {
		return "getAllTasks";
	}

	@Override
	public JsonSerializer<EmptyObject> getRequestSerializer() {
		return EmptyObject.serializer();
	}

	@Override
	public JsonSerializer<Response<PAYLOAD>> getResponseSerializer() {
		return Response.<PAYLOAD>serializer(this.payloadSerializer);
	}

	public record Response<PAYLOAD>(//
			ImmutableList<Task<PAYLOAD>> tasks) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetAllTasks.Response}.
		 * 
		 * @param <PAYLOAD>         the type of the Payload
		 * @param payloadSerializer a {@link JsonSerializer} for the Payload
		 * @return the created {@link JsonSerializer}
		 */
		public static <PAYLOAD> JsonSerializer<Response<PAYLOAD>> serializer(
				JsonSerializer<PAYLOAD> payloadSerializer) {
			return JsonSerializerUtil.<Response<PAYLOAD>>jsonObjectSerializer(//
					json -> new Response<PAYLOAD>(//
							json.getImmutableList("tasks", Task.serializer(payloadSerializer))),
					obj -> JsonUtils.buildJsonObject() //
							.add("tasks", Task.serializer(payloadSerializer).toListSerializer().serialize(obj.tasks())) //
							.build());
		}
	}
}
