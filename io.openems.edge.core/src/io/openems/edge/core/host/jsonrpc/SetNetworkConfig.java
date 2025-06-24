package io.openems.edge.core.host.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.toJsonObject;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.List;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.host.NetworkInterface;
import io.openems.edge.core.host.jsonrpc.SetNetworkConfig.Request;

/**
 * Updates the current network configuration.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "setNetworkConfig",
 *   "params": {
 *   "interfaces": {
 *     [name: string]: {
 *       "dhcp"?: boolean,
 *       "linkLocalAddressing"?: boolean,
 *       "gateway"?: string,
 *       "dns"?: string,
 *       "addresses"?: [{
 *         "label": string,
 *         "address": string,
 *         "subnetmask": string
 *       }]
 *     }
 *   }
 * }
 * </pre>
 */
public class SetNetworkConfig implements EndpointRequestType<Request, EmptyObject> {

	@Override
	public String getMethod() {
		return "setNetworkConfig";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<EmptyObject> getResponseSerializer() {
		return EmptyObject.serializer();
	}

	public record Request(List<NetworkInterface<?>> networkInterfaces) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link SetNetworkConfig.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<SetNetworkConfig.Request> serializer() {
			return jsonObjectSerializer(SetNetworkConfig.Request.class, json -> {
				return new SetNetworkConfig.Request(
						json.getJsonObjectPath("interfaces").collectStringKeys(mapping(t -> {
							return NetworkInterface.serializer(t.getKey()).deserializePath(t.getValue());
						}, toList())));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.add("interfaces", obj.networkInterfaces().stream() //
								.collect(toJsonObject(NetworkInterface::getName, NetworkInterface::toJson))) //
						.build();
			});
		}

	}

}
