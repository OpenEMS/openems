package io.openems.common.jsonrpc.request;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest.Property;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request for 'createComponentConfig'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "createComponentConfig",
 *   "params": {
 *     "factoryPid": string,
 *     "properties": [{
 *       "name": string,
 *       "value": any 
 *     }]
 *   }
 * }
 * </pre>
 */
public class CreateComponentConfigRequest extends JsonrpcRequest {

	public static CreateComponentConfigRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		String factoryPid = JsonUtils.getAsString(p, "factoryPid");
		List<Property> properties = Property.from(JsonUtils.getAsJsonArray(p, "properties"));
		return new CreateComponentConfigRequest(r.getId(), factoryPid, properties);
	}

	public final static String METHOD = "createComponentConfig";

	private final String factoryPid;
	private final List<Property> properties;

	public CreateComponentConfigRequest(String factoryPid, List<Property> properties) {
		this(UUID.randomUUID(), factoryPid, properties);
	}

	public CreateComponentConfigRequest(UUID id, String factoryPid, List<Property> properties) {
		super(id, METHOD);
		this.factoryPid = factoryPid;
		this.properties = properties;
	}

	@Override
	public JsonObject getParams() {
		JsonArray properties = new JsonArray();
		for (Property property : this.properties) {
			properties.add(property.toJson());
		}
		return JsonUtils.buildJsonObject() //
				.addProperty("factoryPid", this.factoryPid) //
				.add("properties", properties) //
				.build();
	}

	public String getFactoryPid() {
		return factoryPid;
	}

	public List<UpdateComponentConfigRequest.Property> getProperties() {
		return properties;
	}
}
