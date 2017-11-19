package io.openems.backend.browserwebsocket.session;

import java.util.Optional;

import org.java_websocket.WebSocket;

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.backend.timedata.Timedata;
import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.CurrentDataWorker;

public class BackendCurrentDataWorker extends CurrentDataWorker {

	private final WebSocket websocket;
	private final int deviceId;

	public BackendCurrentDataWorker(int deviceId, String deviceName, WebSocket websocket, JsonArray jId,
			HashMultimap<String, String> channels) {
		super(jId, Optional.of(deviceName), channels);
		this.deviceId = deviceId;
		this.websocket = websocket;
	}

	@Override
	protected Optional<JsonElement> getChannelValue(ChannelAddress channelAddress) {
		Optional<Object> channelCacheOpt = Timedata.instance().getChannelValue(this.deviceId, channelAddress);
		if (channelCacheOpt.isPresent()) {
			try {
				return Optional.ofNullable(JsonUtils.getAsJsonElement(channelCacheOpt.get()));
			} catch (NotImplementedException e) {
				return Optional.empty();
			}
		} else {
			return Optional.empty();
		}
	}

	@Override
	protected Optional<WebSocket> getWebsocket() {
		return Optional.of(this.websocket);
	}

}
