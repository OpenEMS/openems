package io.openems.edge.core.host.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.List;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.host.Inet4AddressWithSubnetmask;
import io.openems.edge.core.host.jsonrpc.GetNetworkInfo.Response;

public class GetNetworkInfo implements EndpointRequestType<EmptyObject, Response> {

	@Override
	public String getMethod() {
		return "getNetworkInfo";
	}

	public record NetworkInfoWrapper(String hardwareInterface, List<Inet4AddressWithSubnetmask> ips) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetNetworkInfo.Route}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetNetworkInfo.NetworkInfoWrapper> serializer() {
			return jsonObjectSerializer(GetNetworkInfo.NetworkInfoWrapper.class, json -> {
				JsonUtils.stream(json.getJsonArray("ips")).map(entry -> {
					var ip = entry.getAsJsonObject();
					try {
						var inet = (Inet4Address) Inet4Address.getByName(ip.get("prefsrc").getAsString());
						return new Inet4AddressWithSubnetmask(//
								ip.get("family").getAsString(), //
								inet, //
								ip.get("subnetmask").getAsInt());
					} catch (UnknownHostException e) {
						// TODO: use Get Inet4Address Method once available
						return null;
					}
				});
				return new GetNetworkInfo.NetworkInfoWrapper(json.getString("hardwareInterface"), List.of());
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("hardwareInterface", obj.hardwareInterface())//
						.add("ips", //
								obj.ips().stream().map(ip -> {
									return JsonUtils.buildJsonObject()//
											.addProperty("family", ip.getLabel())//
											.addProperty("address", ip.getInet4Address().getHostAddress())//
											.addProperty("subnetmask", ip.getSubnetmaskAsCidr())//
											.build();
								}).collect(JsonUtils.toJsonArray()))//
						.build();
			});
		}

	}

	public record Route(String dst, //
			String dev, //
			String protocol, //
			String scope, //
			Inet4Address prefsrc, //
			int metric //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetNetworkInfo.Route}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetNetworkInfo.Route> serializer() {
			return jsonObjectSerializer(GetNetworkInfo.Route.class, json -> {
				Inet4Address prefsrc;
				try {
					// TODO: use inet4 method
					prefsrc = (Inet4Address) Inet4Address.getByName(json.getString("prefsrc"));
				} catch (UnknownHostException e) {
					prefsrc = null;
				}
				return new GetNetworkInfo.Route(//
						json.getString("dst"), //
						json.getString("dev"), //
						json.getString("protocol"), //
						json.getString("scope"), //
						prefsrc, //
						// TODO: use int method once implemented
						json.get().get("metric") == null ? 0 : json.get().get("metric").getAsInt());
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("dst", obj.dst())//
						.addProperty("dev", obj.dev())//
						.addProperty("protocol", obj.protocol())//
						.addProperty("scope", obj.scope())//
						.addProperty("prefsrc", obj.prefsrc().getHostAddress())//
						.addProperty("metric", obj.metric())//
						.build();
			});
		}

	}

	public record Response(List<NetworkInfoWrapper> ips, List<Route> route) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetNetworkInfo.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetNetworkInfo.Response> serializer() {
			return jsonObjectSerializer(GetNetworkInfo.Response.class, json -> {
				return new Response(//
						json.getList("networkInterfaces", NetworkInfoWrapper.serializer()), //
						json.getList("routes", Route.serializer()) //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject()//
						.add("networkInterfaces",
								NetworkInfoWrapper.serializer().toListSerializer().serialize(obj.ips()))//
						.add("routes", Route.serializer().toListSerializer().serialize(obj.route()))//
						.build();
			});
		}

	}

	@Override
	public JsonSerializer<EmptyObject> getRequestSerializer() {
		return EmptyObject.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}
}
