package io.openems.backend.common.jsonrpc.request;

import java.util.TreeSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
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
 *   "method": "getEdgesChannelsValues",
 *   "params": {
 *     "ids": string[] // Edge-IDs
 *     "channels": string[] // Channel-IDs
 *   }
 * }
 * </pre>
 */
public class GetEdgesChannelsValuesRequest extends JsonrpcRequest {

	public static final String METHOD = "getEdgesChannelsValues";

	/**
	 * Create {@link GetEdgesChannelsValuesRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link GetEdgesChannelsValuesRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static GetEdgesChannelsValuesRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var result = new GetEdgesChannelsValuesRequest(r);
		var edgeIds = JsonUtils.getAsJsonArray(p, "ids");
		for (JsonElement edgeId : edgeIds) {
			result.addEdgeId(JsonUtils.getAsString(edgeId));
		}
		var channels = JsonUtils.getAsJsonArray(p, "channels");
		for (JsonElement channel : channels) {
			var address = ChannelAddress.fromString(JsonUtils.getAsString(channel));
			result.addChannel(address);
		}
		return result;
	}

	private final TreeSet<String> edgeIds = new TreeSet<>();
	private final TreeSet<ChannelAddress> channels = new TreeSet<>();

	public GetEdgesChannelsValuesRequest() {
		super(GetEdgesChannelsValuesRequest.METHOD);
	}

	private GetEdgesChannelsValuesRequest(JsonrpcRequest request) {
		super(request, GetEdgesChannelsValuesRequest.METHOD);
	}

	/**
	 * Adds a Edge-ID.
	 *
	 * @param edgeId the Edge-ID
	 */
	public void addEdgeId(String edgeId) {
		this.edgeIds.add(edgeId);
	}

	/**
	 * Gets the Edge-IDs.
	 *
	 * @return set of Edge-IDs.
	 */
	public TreeSet<String> getEdgeIds() {
		return this.edgeIds;
	}

	/**
	 * Adds a {@link ChannelAddress}.
	 *
	 * @param address the {@link ChannelAddress}
	 */
	public void addChannel(ChannelAddress address) {
		this.channels.add(address);
	}

	/**
	 * Gets the {@link ChannelAddress}es.
	 *
	 * @return the {@link ChannelAddress}es
	 */
	public TreeSet<ChannelAddress> getChannels() {
		return this.channels;
	}

	@Override
	public JsonObject getParams() {
		var edgeIds = new JsonArray();
		for (String edgeId : this.edgeIds) {
			edgeIds.add(edgeId);
		}
		var channels = new JsonArray();
		for (ChannelAddress address : this.channels) {
			channels.add(address.toString());
		}
		return JsonUtils.buildJsonObject() //
				.add("ids", edgeIds) //
				.add("channels", channels) //
				.build();
	}

}
