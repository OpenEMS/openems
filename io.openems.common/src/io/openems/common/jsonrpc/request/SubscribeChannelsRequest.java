package io.openems.common.jsonrpc.request;

import java.util.TreeSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request to subscribe to Channels.
 *
 * <p>
 * This is used by UI to get regular updates on specific channels.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "subscribeChannels",
 *   "params": {
 *     "count": number // Request-Counter: the higher count wins
 *     "channels": string[]
 *   }
 * }
 * </pre>
 */
public class SubscribeChannelsRequest extends JsonrpcRequest {

	public static final String METHOD = "subscribeChannels";

	/**
	 * Create {@link SubscribeChannelsRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link SubscribeChannelsRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static SubscribeChannelsRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var count = JsonUtils.getAsInt(p, "count");
		var result = new SubscribeChannelsRequest(r, count);
		var channels = JsonUtils.getAsJsonArray(p, "channels");
		for (JsonElement channel : channels) {
			result.addChannel(JsonUtils.getAsString(channel));
		}
		return result;
	}

	private final int count;
	private final TreeSet<String> channels = new TreeSet<>();

	private SubscribeChannelsRequest(JsonrpcRequest request, int count) {
		super(request, SubscribeChannelsRequest.METHOD);
		this.count = count;
	}

	public SubscribeChannelsRequest(int count) {
		super(SubscribeChannelsRequest.METHOD);
		this.count = count;
	}

	private void addChannel(String address) {
		this.channels.add(address);
	}

	/**
	 * Gets the Count value.
	 *
	 * <p>
	 * This value is increased with every request to assure order.
	 *
	 * @return the count value
	 */
	public int getCount() {
		return this.count;
	}

	/**
	 * Gets the set of {@link ChannelAddress}es.
	 *
	 * @return the {@link ChannelAddress}es
	 */
	public TreeSet<String> getChannels() {
		return this.channels;
	}

	@Override
	public JsonObject getParams() {
		var channels = new JsonArray();
		for (var address : this.channels) {
			channels.add(address);
		}
		return JsonUtils.buildJsonObject() //
				.addProperty("count", this.count) //
				.add("channels", channels) //
				.build();
	}
}
