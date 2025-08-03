package io.openems.edge.common.update.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.update.jsonrpc.ExecuteUpdate.Request;

public class ExecuteUpdate implements EndpointRequestType<Request, EmptyObject> {

	@Override
	public String getMethod() {
		return "executeUpdate";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<EmptyObject> getResponseSerializer() {
		return EmptyObject.serializer();
	}

	public record Request(String id) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link ExecuteUpdate.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<ExecuteUpdate.Request> serializer() {
			return jsonObjectSerializer(ExecuteUpdate.Request.class, json -> {
				return new ExecuteUpdate.Request(json.getString("id"));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("id", obj.id()) //
						.build();
			});
		}

	}

}
