package io.openems.edge.core.host.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemRestart.Request;

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
public class ExecuteSystemRestart implements EndpointRequestType<Request, ExecuteSystemCommand.Response> {

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
		 * Returns a {@link JsonSerializer} for a {@link ExecuteSystemRestart.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<ExecuteSystemRestart.Request> serializer() {
			return jsonObjectSerializer(ExecuteSystemRestart.Request.class, //
					json -> new ExecuteSystemRestart.Request(//
							json.getEnum("type", Type.class)), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("type", obj.type()) //
							.build());
		}

	}

}
