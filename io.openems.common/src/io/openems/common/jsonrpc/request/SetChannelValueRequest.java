package io.openems.common.jsonrpc.request;

import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;

/**
 * Sets the write value of a Channel.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "setChannelValue",
 *   "params": {
 *     "componentId": string,
 *     "channelId": string,
 *     "value": any
 *   }
 * }
 * </pre>
 */
public class SetChannelValueRequest extends JsonrpcRequest {

	public static SetChannelValueRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		String componentId = JsonUtils.getAsString(p, "componentId");
		String channelId = JsonUtils.getAsString(p, "channelId");
		JsonElement value = JsonUtils.getSubElement(p, "value");
		return new SetChannelValueRequest(r.getId(), componentId, channelId, value);
	}

	public final static String METHOD = "setChannelValue";

	private final String componentId;
	private final String channelId;
	private final JsonElement value;

	public SetChannelValueRequest(String componentId, String channelId, JsonElement value) {
		this(UUID.randomUUID(), componentId, channelId, value);
	}

	public SetChannelValueRequest(UUID id, String componentId, String channelId, JsonElement value) {
		super(id, METHOD);
		this.componentId = componentId;
		this.channelId = channelId;
		this.value = value;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("componentId", this.componentId) //
				.addProperty("channelId", this.channelId) //
				.add("value", this.value) //
				.build();
	}

	public String getComponentId() {
		return componentId;
	}

	public String getChannelId() {
		return channelId;
	}

	public ChannelAddress getChannelAddress() {
		return new ChannelAddress(this.componentId, this.channelId);
	}

	public JsonElement getValue() {
		return value;
	}
}
