package io.openems.common.jscalendar;

import java.util.UUID;

import io.openems.common.jscalendar.DeleteTask.Request;
import io.openems.common.jscalendar.JSCalendar.Task;
import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

/**
 * Deletes the {@link Task} with the specified {@link UUID}.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": {@link UUID},
 *   "method": "deleteTask",
 *   "params": {
 *   	"uid": {@link UUID}
 *   }
 * }
 * </pre>
 */
public class DeleteTask implements EndpointRequestType<Request, EmptyObject> {

	@Override
	public String getMethod() {
		return "deleteTask";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<EmptyObject> getResponseSerializer() {
		return EmptyObject.serializer();
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
}
