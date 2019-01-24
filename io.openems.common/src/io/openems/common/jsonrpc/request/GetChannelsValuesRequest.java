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
 * Represents a JSON-RPC Request for 'getChannelsValues'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getChannelsValues",
 *   "params": {
 *     "ids": string[] // Edge-IDs
 *     "channels": string[] // Channel-IDs
 *   }
 * }
 * </pre>
 */
public class GetChannelsValuesRequest extends JsonrpcRequest {

	public static GetChannelsValuesRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		GetChannelsValuesRequest result = new GetChannelsValuesRequest(r.getId());
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

	public static GetChannelsValuesRequest from(JsonObject j) throws OpenemsNamedException {
		return from(GenericJsonrpcRequest.from(j));
	}

	public final static String METHOD = "getChannelsValues";

	private final TreeSet<String> edgeIds = new TreeSet<>();
	private final TreeSet<ChannelAddress> channels = new TreeSet<>();

	public GetChannelsValuesRequest() {
		this(UUID.randomUUID());
	}

	public GetChannelsValuesRequest(UUID id) {
		super(id, METHOD);
	}

	public void addEdgeId(String edgeId) {
		this.edgeIds.add(edgeId);
	}

	public TreeSet<String> getEdgeIds() {
		return edgeIds;
	}

	public void addChannel(ChannelAddress address) {
		this.channels.add(address);
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
				.add("ids", edgeIds) //
				.add("channels", channels) //
				.build();
	}

}
