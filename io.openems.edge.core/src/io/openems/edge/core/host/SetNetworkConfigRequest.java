package io.openems.edge.core.host;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

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
 *       "addresses"?: string[]
 *     }
 *   }
 * }
 * </pre>
 */
public class SetNetworkConfigRequest extends JsonrpcRequest {

	public static final String METHOD = "setNetworkConfig";

	public static SetNetworkConfigRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		JsonObject jInterfaces = JsonUtils.getAsJsonObject(p, "interfaces");
		List<NetworkInterface<?>> interfaces = new ArrayList<>();
		for (Entry<String, JsonElement> entry : jInterfaces.entrySet()) {
			interfaces.add(NetworkInterface.from(entry.getKey(), JsonUtils.getAsJsonObject(entry.getValue())));
		}
		return new SetNetworkConfigRequest(r.getId(), interfaces);
	}

	private final List<NetworkInterface<?>> networkInterfaces;

	public SetNetworkConfigRequest(List<NetworkInterface<?>> interfaces) {
		this(UUID.randomUUID(), interfaces);
	}

	public SetNetworkConfigRequest(UUID id, List<NetworkInterface<?>> networkInterfaces) {
		super(id, METHOD);
		this.networkInterfaces = networkInterfaces;
	}

	@Override
	public JsonObject getParams() {
		JsonObject interfaces = new JsonObject();
		for (NetworkInterface<?> iface : this.networkInterfaces) {
			interfaces.add(iface.getName(), iface.toJson());
		}
		return JsonUtils.buildJsonObject() //
				.add("interfaces", interfaces) //
				.build();
	}

	public List<NetworkInterface<?>> getNetworkInterface() {
		return this.networkInterfaces;
	}

}
