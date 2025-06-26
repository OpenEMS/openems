package io.openems.edge.core.componentmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.type.Base64RequestType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.componentmanager.jsonrpc.ChannelExportXlsx.Request;

/**
 * Exports Channels with current value and metadata to an Excel (xlsx) file.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "channelExportXlsx",
 *   "params": {
 *   	"componentId": string
 *   }
 * }
 * </pre>
 */
public class ChannelExportXlsx implements EndpointRequestType<Request, Base64RequestType> {

	@Override
	public String getMethod() {
		return "channelExportXlsx";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Base64RequestType> getResponseSerializer() {
		return Base64RequestType.serializer();
	}

	public static record Request(//
			String componentId //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link AddAppInstance.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, //
					json -> new Request(//
							json.getString("componentId")),
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("componentId", obj.componentId()) //
							.build());
		}

	}

}
