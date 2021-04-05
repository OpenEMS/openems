package io.openems.backend.b2bwebsocket.jsonrpc.request;

import java.util.TreeSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request to subscribe to Channels of multiple Edges.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "subscribeEdgesChannels",
 *   "params": {
 *     "count": number // Request-Counter: the higher count wins
 *     "edgeIds": string[] // Edge-IDs
 *     "channels": string[] // Channel-IDs
 *   }
 * }
 * </pre>
 */
public class SubscribeEdgesChannelsRequest extends JsonrpcRequest {

	public static final String METHOD = "subscribeEdgesChannels";

	public static SubscribeEdgesChannelsRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		int count = JsonUtils.getAsInt(p, "count");
		SubscribeEdgesChannelsRequest result = new SubscribeEdgesChannelsRequest(r, count);
		JsonArray edgeIds = JsonUtils.getAsJsonArray(p, "ids");
		for (JsonElement edgeId : edgeIds) {
			result.addEdgeId(JsonUtils.getAsString(edgeId));
		}
		JsonArray channels = JsonUtils.getAsJsonArray(p, "channels");
		for (JsonElement channel : channels) {
			ChannelAddress address = ChannelAddress.fromString(JsonUtils.getAsString(channel));
			result.addChannel(address);
		}
		return result;
	}

	public static SubscribeEdgesChannelsRequest from(JsonObject j) throws OpenemsNamedException {
		return from(GenericJsonrpcRequest.from(j));
	}

	private final int count;
	private final TreeSet<String> edgeIds = new TreeSet<>();
	private final TreeSet<ChannelAddress> channels = new TreeSet<>();

	private SubscribeEdgesChannelsRequest(JsonrpcRequest request, int count) {
		super(request, METHOD);
		this.count = count;
	}

	public SubscribeEdgesChannelsRequest(int count) {
		super(METHOD);
		this.count = count;
	}

	public void addEdgeId(String edgeId) {
		this.edgeIds.add(edgeId);
	}

	public void removeEdgeId(String edgeId) {
		this.edgeIds.remove(edgeId);
	}

	public TreeSet<String> getEdgeIds() {
		return edgeIds;
	}

	public void addChannel(ChannelAddress address) {
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
		JsonArray edgeIds = new JsonArray();
		for (String edgeId : this.edgeIds) {
			edgeIds.add(edgeId);
		}
		JsonArray channels = new JsonArray();
		for (ChannelAddress address : this.channels) {
			channels.add(address.toString());
		}
		return JsonUtils.buildJsonObject() //
				.addProperty("count", this.count) //
				.add("ids", edgeIds) //
				.add("channels", channels) //
				.build();
	}
}
