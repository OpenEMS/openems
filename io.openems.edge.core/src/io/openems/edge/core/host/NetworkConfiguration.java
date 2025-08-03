package io.openems.edge.core.host;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.toJsonObject;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;

public class NetworkConfiguration {

	public static final String PATTERN_INET4ADDRESS = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

	/**
	 * Returns a {@link JsonSerializer} for a {@link NetworkConfiguration}.
	 * 
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<NetworkConfiguration> serializer() {
		return jsonObjectSerializer(NetworkConfiguration.class, json -> {
			return new NetworkConfiguration(json.getJsonObjectPath("interfaces").collectStringKeys(
					mapping(t -> NetworkInterface.serializer(t.getKey()).deserializePath(t.getValue()),
							toMap(NetworkInterface::getName, Function.identity()))));
		}, obj -> {
			return JsonUtils.buildJsonObject() //
					.add("interfaces", obj.getInterfaces().entrySet().stream() //
							.collect(toJsonObject(Entry::getKey,
									e -> NetworkInterface.serializer(e.getKey()).serialize(e.getValue())))) //
					.build();
		});
	}

	private final Map<String, NetworkInterface<?>> interfaces;

	public NetworkConfiguration(Map<String, NetworkInterface<?>> interfaces) {
		this.interfaces = interfaces;
	}

	/**
	 * Return this NetworkConfiguration as a JSON object.
	 *
	 * <pre>
	 * {
	 *   "interfaces": {
	 *     [name: string]: {
	 *       "dhcp"?: boolean,
	 *       "linkLocalAddressing"?: boolean,
	 *       "gateway"?: string,
	 *       "dns"?: string,
	 *       "addresses": [{ 
	 *         "label": string, 
	 *         "address": string, 
	 *         "subnetmask": string 
	 *       }]
	 *     }
	 *   }
	 * }
	 * </pre>
	 *
	 * @return this configuration as JSON
	 */
	public JsonObject toJson() {
		var interfaces = new JsonObject();
		for (Entry<String, NetworkInterface<?>> entry : this.interfaces.entrySet()) {
			interfaces.add(entry.getKey(), entry.getValue().toJson());
		}
		return JsonUtils.buildJsonObject() //
				.add("interfaces", interfaces) //
				.build();
	}

	/**
	 * Gets the network interfaces configuration.
	 *
	 * @return a map of network interfaces per name
	 */
	public Map<String, NetworkInterface<?>> getInterfaces() {
		return this.interfaces;
	}

}
