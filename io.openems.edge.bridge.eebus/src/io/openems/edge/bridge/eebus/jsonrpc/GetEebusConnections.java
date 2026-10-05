package io.openems.edge.bridge.eebus.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.emptyObjectSerializer;

import java.util.List;

import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.eebus.api.EebusConnectionInfo;

public class GetEebusConnections
		implements EndpointRequestType<GetEebusConnections.Request, GetEebusConnections.Response> {

	@Override
	public String getMethod() {
		return "getEebusConnections";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Request() {

		public static JsonSerializer<Request> serializer() {
			return emptyObjectSerializer(Request::new);
		}
	}

	public record Response(List<EebusConnectionInfo> connections) {

		public static JsonSerializer<Response> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(//
					json -> new Response(//
							json.getList("connections", EebusConnectionInfo.serializer())),
					obj -> JsonUtils.buildJsonObject() //
							.add("connections", obj.connections, EebusConnectionInfo.serializer().toListSerializer()) //
							.build());
		}
	}

}
