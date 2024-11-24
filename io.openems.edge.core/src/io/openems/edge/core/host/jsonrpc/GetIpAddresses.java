package io.openems.edge.core.host.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.emptyObjectSerializer;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.net.Inet4Address;
import java.util.List;

import com.google.gson.JsonArray;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.EndpointRequestType;
import io.openems.edge.core.host.jsonrpc.GetIpAddresses.Request;
import io.openems.edge.core.host.jsonrpc.GetIpAddresses.Response;

public class GetIpAddresses implements EndpointRequestType<Request, Response> {
	public record Request() {
		
		/**
		 * Returns a {@link JsonSerializer} for a {@link GetIpAddresses.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return emptyObjectSerializer(Request::new);
		}
	}

	public record Response(List<Inet4Address> ips) {
		
		/**
		 * Returns a {@link JsonSerializer} for a {@link GetIpAddresses.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetIpAddresses.Response> seriliazer() {
			return jsonObjectSerializer(GetIpAddresses.Response.class, json -> {
				return new Response(List.of());
			}, obj -> {
				return JsonUtils.buildJsonObject()//
						.add("ips", buildInterfaceArray(obj.ips()))//
						.build();
			});
		}

		private static JsonArray buildInterfaceArray(List<Inet4Address> ips) {
			var builder = JsonUtils.buildJsonArray();
			ips.stream().map((ip) -> ip.getHostAddress()).forEach(builder::add);
			return builder.build();
		}
	}

	@Override
	public String getMethod() {
		return "getIpAddresses";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.seriliazer();
	}
}
