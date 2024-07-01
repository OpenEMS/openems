package io.openems.edge.core.host.jsonrpc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.host.NetworkInterface;

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
 *       "metric"?:integer,
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
public class SetNetworkConfigRequest extends JsonrpcRequest {

	public static final String METHOD = "setNetworkConfig";

	/**
	 * Parses a generic {@link JsonrpcRequest} to a {@link SetNetworkConfigRequest}.
	 *
	 * @param r the {@link JsonrpcRequest}
	 * @return the {@link SetNetworkConfigRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static SetNetworkConfigRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var jInterfaces = JsonUtils.getAsJsonObject(p, "interfaces");
		List<NetworkInterface<?>> interfaces = new ArrayList<>();
		for (Entry<String, JsonElement> entry : jInterfaces.entrySet()) {
			interfaces.add(NetworkInterface.from(entry.getKey(), JsonUtils.getAsJsonObject(entry.getValue())));
		}
		return new SetNetworkConfigRequest(r, interfaces);
	}

	private final List<NetworkInterface<?>> networkInterfaces;

	public SetNetworkConfigRequest(List<NetworkInterface<?>> interfaces) {
		super(METHOD);
		this.networkInterfaces = interfaces;
	}

	private SetNetworkConfigRequest(JsonrpcRequest request, List<NetworkInterface<?>> interfaces) {
		super(request, METHOD);
		this.networkInterfaces = interfaces;
	}

	@Override
	public JsonObject getParams() {
		var interfaces = new JsonObject();
		for (NetworkInterface<?> iface : this.networkInterfaces) {
			interfaces.add(iface.getName(), iface.toJson());
		}

		return JsonUtils.buildJsonObject() //
				.add("interfaces", interfaces) //
				.build();
	}

	/**
	 * Gets the request network interfaces.
	 *
	 * @return the network interfaces
	 */
	public List<NetworkInterface<?>> getNetworkInterface() {
		return this.networkInterfaces;
	}

}
