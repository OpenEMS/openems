
package io.openems.edge.core.host.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemUpdate.Request;

/**
 * Executes a System Update.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "executeSystemUpdate",
 *   "params": {
 *     "isDebug": boolean
 *   }
 * }
 * </pre>
 */
public class ExecuteSystemUpdate implements EndpointRequestType<Request, EmptyObject> {

	@Override
	public String getMethod() {
		return "executeSystemUpdate";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<EmptyObject> getResponseSerializer() {
		return EmptyObject.serializer();
	}

	public record Request(boolean isDebug) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link ExecuteSystemUpdate}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, //
					json -> new Request(json.getBoolean("isDebug")), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("isDebug", obj.isDebug()) //
							.build());
		}

	}

}
