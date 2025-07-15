package io.openems.edge.core.host.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.net.Inet4Address;
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

	public record NetworkInfoAddress(Inet4AddressWithSubnetmask ip, boolean dynamic) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link NetworkInfoAddress}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<NetworkInfoAddress> serializer() {
			return jsonObjectSerializer(NetworkInfoAddress.class, json -> {
				return new NetworkInfoAddress(new Inet4AddressWithSubnetmask(//
						json.getString("label"), //
						json.getStringParsed("address", new Inet4AddressWithSubnetmask.StringParserInet4Address()), //
						json.getInt("subnetmask")), //
						json.getOptionalBoolean("dynamic").orElse(false));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("label", obj.ip().getLabel())//
						.addProperty("address", obj.ip().getInet4Address().getHostAddress())//
						.addProperty("subnetmask", obj.ip().getSubnetmaskAsCidr())//
						.onlyIf(obj.dynamic(), b -> b//
								.addProperty("dynamic", obj.dynamic()))//
						.build();
			});
		}
	}

	public record NetworkInfoWrapper(String hardwareInterface, List<NetworkInfoAddress> ips) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetNetworkInfo.Route}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetNetworkInfo.NetworkInfoWrapper> serializer() {
			return jsonObjectSerializer(GetNetworkInfo.NetworkInfoWrapper.class, json -> {
				return new GetNetworkInfo.NetworkInfoWrapper(//
						json.getString("hardwareInterface"), //
						json.getList("ips", NetworkInfoAddress.serializer()) //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("hardwareInterface", obj.hardwareInterface())//
						.add("ips", NetworkInfoAddress.serializer().toListSerializer().serialize(obj.ips()))//
						.build();
			});
		}

	}

	public record Route(String dst, //
			String dev, //
			String protocol, //
			String scope, //
			Inet4Address prefsrc, //
			Integer metric //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetNetworkInfo.Route}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetNetworkInfo.Route> serializer() {
			return jsonObjectSerializer(GetNetworkInfo.Route.class, json -> {
				return new GetNetworkInfo.Route(//
						json.getString("dst"), //
						json.getString("dev"), //
						json.getString("protocol"), //
						json.getOptionalString("scope").orElse("link"), //
						json.getStringParsedOrNull("prefsrc",
								new Inet4AddressWithSubnetmask.StringParserInet4Address()), //
						json.getOptionalInt("metric").orElse(null));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("dst", obj.dst())//
						.addProperty("dev", obj.dev())//
						.addProperty("protocol", obj.protocol())//
						.addProperty("scope", obj.scope())//
						.onlyIf(obj.prefsrc() != null, //
								b -> b.addProperty("prefsrc", obj.prefsrc().getHostAddress()))
						.addPropertyIfNotNull("metric", obj.metric()) //
						.build();
			});
		}

	}

	public record Response(List<NetworkInfoWrapper> networkInterfaces, List<Route> route) {

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
								NetworkInfoWrapper.serializer().toListSerializer().serialize(obj.networkInterfaces()))//
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
