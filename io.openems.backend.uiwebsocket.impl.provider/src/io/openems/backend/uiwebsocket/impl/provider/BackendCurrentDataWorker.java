package io.openems.backend.uiwebsocket.impl.provider;

import java.util.Optional;

import org.java_websocket.WebSocket;

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.CurrentDataWorker;

public class BackendCurrentDataWorker extends CurrentDataWorker {

	private final UiWebsocketServer parent;
	private final int edgeId;

	public BackendCurrentDataWorker(UiWebsocketServer parent, WebSocket websocket, String messageId, int edgeId,
			HashMultimap<String, String> channels) {
		super(websocket, messageId, channels);
		this.parent = parent;
		this.edgeId = edgeId;
	}

	@Override
	protected Optional<JsonElement> getChannelValue(ChannelAddress channelAddress) {
		Optional<Object> channelCacheOpt = this.parent.parent.timeDataService.getChannelValue(this.edgeId,
				channelAddress);
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
}
