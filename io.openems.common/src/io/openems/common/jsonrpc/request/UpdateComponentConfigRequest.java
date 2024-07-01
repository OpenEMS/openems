package io.openems.common.jsonrpc.request;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request for 'updateComponentConfig'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "updateComponentConfig",
 *   "params": {
 *     "componentId": string,
 *     "properties": [{
 *       "name": string,
 *       "value": any
 *     }]
 *   }
 * }
 * </pre>
 */
public class UpdateComponentConfigRequest extends JsonrpcRequest {

	public static final String METHOD = "updateComponentConfig";

	/**
	 * Create {@link UpdateComponentConfigRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link UpdateComponentConfigRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static UpdateComponentConfigRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var componentId = JsonUtils.getAsString(p, "componentId");
		var properties = Property.from(JsonUtils.getAsJsonArray(p, "properties"));
		return new UpdateComponentConfigRequest(r, componentId, properties);
	}

	private final String componentId;
	private final List<Property> properties;

	public UpdateComponentConfigRequest(String componentId, List<Property> properties) {
		super(UpdateComponentConfigRequest.METHOD);
		this.componentId = componentId;
		this.properties = properties;
	}

	private UpdateComponentConfigRequest(JsonrpcRequest request, String componentId, List<Property> properties) {
		super(request, UpdateComponentConfigRequest.METHOD);
		this.componentId = componentId;
		this.properties = properties;
	}

	@Override
	public JsonObject getParams() {
		var properties = new JsonArray();
		for (Property property : this.properties) {
			properties.add(property.toJson());
		}
		return JsonUtils.buildJsonObject() //
				.addProperty("componentId", this.componentId) //
				.add("properties", properties) //
				.build();
	}

	/**
	 * Gets the Component-ID.
	 *
	 * @return Component-ID
	 */
	public String getComponentId() {
		return this.componentId;
	}

	/**
	 * Gets a list of Properties.
	 *
	 * @return the Properties
	 */
	public List<Property> getProperties() {
		return this.properties;
	}

	public static class Property {

		protected static List<Property> from(JsonArray j) throws OpenemsNamedException {
			List<Property> properties = new ArrayList<>();
			for (JsonElement property : j) {
				var name = JsonUtils.getAsString(property, "name");
				var value = JsonUtils.getSubElement(property, "value");
				properties.add(new Property(name, value));
			}
			return properties;
		}

		private final String name;
		private final JsonElement value;

		/**
		 * Initializes a Property.
		 *
		 * @param name  the Property name
		 * @param value the new value
		 */
		public Property(String name, JsonElement value) {
			// convert underscore ('_') to point ('.') in property name
			this.name = name.replace("_", ".");
			this.value = value;
		}

		public Property(String name, String value) {
			this(name, new JsonPrimitive(value));
		}

		public Property(String name, boolean value) {
			this(name, new JsonPrimitive(value));
		}

		public Property(String name, Number value) {
			this(name, new JsonPrimitive(value));
		}

		/**
		 * Gets the Name.
		 *
		 * @return Name
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Gets the Value.
		 *
		 * @return Value
		 */
		public JsonElement getValue() {
			return this.value;
		}

		protected JsonObject toJson() {
			return JsonUtils.buildJsonObject() //
					.addProperty("name", this.getName()) //
					.add("value", this.getValue()) //
					.build();
		}
	}
}
