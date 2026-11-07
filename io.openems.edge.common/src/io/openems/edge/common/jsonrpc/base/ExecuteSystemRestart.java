package io.openems.edge.common.jsonrpc.base;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request to execute a system restart.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "executeSystemRestart",
 *   "params": {
 *   	"type": "SOFT" | "HARD"
 *   }
 * }
 * </pre>
 */
public class ExecuteSystemRestart
		implements EndpointRequestType<ExecuteSystemRestart.Request, ExecuteSystemCommand.Response> {

	@Override
	public String getMethod() {
		return "executeSystemRestart";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<ExecuteSystemCommand.Response> getResponseSerializer() {
		return ExecuteSystemCommand.Response.serializer();
	}

	public record Request(//
			Type type //
	) {

		public enum Type {
			/**
			 * SOFT: restart only the Java OpenEMS Edge process.
			 */
			SOFT,
			/**
			 * HARD: reboot the device.
			 */
			HARD;
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, //
					json -> new Request(//
							json.getEnum("type", Type.class)), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("type", obj.type()) //
							.build());
		}

	}

}
