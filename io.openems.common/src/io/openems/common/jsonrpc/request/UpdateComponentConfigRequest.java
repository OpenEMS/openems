package io.openems.common.jsonrpc.request;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
 *     "update": [{
 *       "property": string,
 *       "value": any 
 *     }]
 *   }
 * }
 * </pre>
 */
public class UpdateComponentConfigRequest extends JsonrpcRequest {

	public static UpdateComponentConfigRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		String componentId = JsonUtils.getAsString(p, "componentId");
		List<Update> update = Update.from(JsonUtils.getAsJsonArray(p, "update"));
		return new UpdateComponentConfigRequest(r.getId(), componentId, update);
	}

	public final static String METHOD = "updateComponentConfig";

	private final String componentId;
	private final List<Update> update;

	public UpdateComponentConfigRequest(String componentId, List<Update> update) {
		this(UUID.randomUUID(), componentId, update);
	}

	public UpdateComponentConfigRequest(UUID id, String componentId, List<Update> update) {
		super(id, METHOD);
		this.componentId = componentId;
		this.update = update;
	}

	@Override
	public JsonObject getParams() {
		JsonArray update = new JsonArray();
		for (Update ue : this.update) {
			update.add(ue.toJson());
		}
		return JsonUtils.buildJsonObject() //
				.addProperty("componentId", this.componentId) //
				.add("update", update) //
				.build();
	}

	public String getComponentId() {
		return componentId;
	}

	public List<Update> getUpdate() {
		return update;
	}

	public static class Update {

		public static List<Update> from(JsonArray j) throws OpenemsNamedException {
			List<Update> update = new ArrayList<>();
			for (JsonElement ue : j) {
				String property = JsonUtils.getAsString(ue, "property");
				JsonElement value = JsonUtils.getSubElement(ue, "value");
				update.add(new Update(property, value));
			}
			return update;
		}

		private final String property;
		private final JsonElement value;

		/**
		 * @param property the Property-ID
		 * @param value    the new value
		 */
		public Update(String property, JsonElement value) {
			this.property = property;
			this.value = value;
		}

		public String getProperty() {
			return property;
		}

		public JsonElement getValue() {
			return value;
		}

		public JsonObject toJson() {
			return JsonUtils.buildJsonObject() //
					.addProperty("property", this.getProperty()) //
					.add("value", this.getValue()) //
					.build();
		}
	}
}
