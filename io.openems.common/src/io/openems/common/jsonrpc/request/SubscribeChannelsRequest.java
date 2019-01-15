package io.openems.common.jsonrpc.request;

import java.util.TreeSet;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request to subscribe to Channels.
 * 
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

	public final static String METHOD = "subscribeChannels";

	public static SubscribeChannelsRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		int count = JsonUtils.getAsInt(p, "count");
		SubscribeChannelsRequest result = new SubscribeChannelsRequest(r.getId(), count);
		JsonArray channels = JsonUtils.getAsJsonArray(p, "channels");
		for (JsonElement channel : channels) {
			ChannelAddress address = ChannelAddress.fromString(JsonUtils.getAsString(channel));
			result.addChannel(address);
		}
		return result;
	}

	public static SubscribeChannelsRequest from(JsonObject j) throws OpenemsNamedException {
		return from(GenericJsonrpcRequest.from(j));
	}

	private final int count;
	private final TreeSet<ChannelAddress> channels = new TreeSet<>();

	public SubscribeChannelsRequest(UUID id, int count) {
		super(id, METHOD);
		this.count = count;
	}

	public SubscribeChannelsRequest(int count) {
		this(UUID.randomUUID(), count);
	}

	private void addChannel(ChannelAddress address) {
		this.channels.add(address);
	}

	public int getCount() {
		return this.count;
	}

	public TreeSet<ChannelAddress> getChannels() {
		return channels;
	}

	@Override
	public JsonObject getParams() {
		JsonArray channels = new JsonArray();
		for (ChannelAddress address : this.channels) {
			channels.add(address.toString());
		}
		return JsonUtils.buildJsonObject() //
				.addProperty("count", this.count) //
				.add("channels", channels) //
				.build();
	}
}
