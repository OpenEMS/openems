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

public class SubscribeChannelsRequest extends JsonrpcRequest {

	public final static String METHOD = "subscribeChannels";

	public static SubscribeChannelsRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		SubscribeChannelsRequest result = new SubscribeChannelsRequest(r.getId());
		JsonObject p = r.getParams();
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

	private final TreeSet<ChannelAddress> channels = new TreeSet<>();

	public SubscribeChannelsRequest(UUID id) {
		super(id, METHOD);
	}

	public SubscribeChannelsRequest() {
		this(UUID.randomUUID());
	}

	private void addChannel(ChannelAddress address) {
		this.channels.add(address);
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
				.add("channels", channels) //
				.build();
	}
}
