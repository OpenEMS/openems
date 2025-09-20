package io.openems.common.jsonrpc.request;

import java.util.List;

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

	public static final String METHOD = "createComponentConfig";

	/**
	 * Create {@link CreateComponentConfigRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link CreateComponentConfigRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static CreateComponentConfigRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var factoryPid = JsonUtils.getAsString(p, "factoryPid");
		var properties = Property.from(JsonUtils.getAsJsonArray(p, "properties"));
		return new CreateComponentConfigRequest(r, factoryPid, properties);
	}

	/**
	 * Create {@link CreateComponentConfigRequest} from a {@link JsonObject}.
	 *
	 * @param params the {@link JsonObject}
	 * @return the {@link CreateComponentConfigRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static CreateComponentConfigRequest from(JsonObject params) throws OpenemsNamedException {
		var factoryPid = JsonUtils.getAsString(params, "factoryPid");
		var properties = Property.from(JsonUtils.getAsJsonArray(params, "properties"));
		return new CreateComponentConfigRequest(factoryPid, properties);
	}

	private final String factoryPid;
	private final List<Property> properties;

	public CreateComponentConfigRequest(String factoryPid, List<Property> properties) {
		super(CreateComponentConfigRequest.METHOD);
		this.factoryPid = factoryPid;
		this.properties = properties;
	}

	private CreateComponentConfigRequest(JsonrpcRequest request, String factoryPid, List<Property> properties) {
		super(request, CreateComponentConfigRequest.METHOD);
		this.factoryPid = factoryPid;
		this.properties = properties;
	}

	@Override
	public JsonObject getParams() {
		var properties = new JsonArray();
		for (Property property : this.properties) {
			properties.add(property.toJson());
		}
		return JsonUtils.buildJsonObject() //
				.addProperty("factoryPid", this.factoryPid) //
				.add("properties", properties) //
				.build();
	}

	/**
	 * Gets the Factory-PID.
	 *
	 * @return Factory-PID
	 */
	public String getFactoryPid() {
		return this.factoryPid;
	}

	/**
	 * Gets the Component-ID, or empty String if none is given.
	 *
	 * @return Component-ID
	 */
	public String getComponentId() {
		for (UpdateComponentConfigRequest.Property property : this.properties) {
			if (property.getName().equals("id")) {
				return property.getValue().getAsString();
			}
		}
		return "";
	}

	/**
	 * Gets the List of Properties.
	 *
	 * @return Properties
	 */
	public List<UpdateComponentConfigRequest.Property> getProperties() {
		return this.properties;
	}
}
