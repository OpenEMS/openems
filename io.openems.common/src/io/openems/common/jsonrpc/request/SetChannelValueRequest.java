package io.openems.common.jsonrpc.request;

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

	public static final String METHOD = "setChannelValue";

	/**
	 * Create {@link SetChannelValueRequest} from a template {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link SetChannelValueRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static SetChannelValueRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var componentId = JsonUtils.getAsString(p, "componentId");
		var channelId = JsonUtils.getAsString(p, "channelId");
		var value = JsonUtils.getSubElement(p, "value");
		return new SetChannelValueRequest(r, componentId, channelId, value);
	}

	private final String componentId;
	private final String channelId;
	private final JsonElement value;

	public SetChannelValueRequest(String componentId, String channelId, JsonElement value) {
		super(SetChannelValueRequest.METHOD);
		this.componentId = componentId;
		this.channelId = channelId;
		this.value = value;
	}

	private SetChannelValueRequest(JsonrpcRequest request, String componentId, String channelId, JsonElement value) {
		super(request, SetChannelValueRequest.METHOD);
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

	/**
	 * Gets the Component-ID.
	 *
	 * @return Component-ID
	 */
	public String getComponentId() {
		return this.componentId;
	}

	/**
	 * Gets the Channel-ID.
	 *
	 * @return Channel-ID
	 */
	public String getChannelId() {
		return this.channelId;
	}

	/**
	 * Gets the {@link ChannelAddress}.
	 *
	 * @return ChannelAddress
	 */
	public ChannelAddress getChannelAddress() {
		return new ChannelAddress(this.componentId, this.channelId);
	}

	/**
	 * Gets the Value.
	 *
	 * @return Value
	 */
	public JsonElement getValue() {
		return this.value;
	}
}
