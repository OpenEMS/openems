package io.openems.core.utilities.websocket;

import java.util.Optional;

import org.java_websocket.WebSocket;

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.api.channel.Channel;
import io.openems.common.exceptions.AccessDeniedException;
import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.CurrentDataWorker;
import io.openems.core.Databus;
import io.openems.core.ThingRepository;

public class EdgeCurrentDataWorker extends CurrentDataWorker {

	private final EdgeWebsocketHandler edgeWebsocketHandler;
	private final ThingRepository thingRepository;
	private final Databus databus;

	/**
	 * The access level Role of this worker
	 */
	private final Role role;

	public EdgeCurrentDataWorker(JsonArray jId, HashMultimap<String, String> channels, Role role,
			EdgeWebsocketHandler edgeWebsocketHandler) {
		super(jId, Optional.empty(), channels);
		this.role = role;
		this.edgeWebsocketHandler = edgeWebsocketHandler;
		this.thingRepository = ThingRepository.getInstance();
		this.databus = Databus.getInstance();
	}

	@Override
	protected Optional<JsonElement> getChannelValue(ChannelAddress channelAddress) {
		// TODO rename getChannel() to getChannelOpt
		// TODO create new getChannel() that throws an error if not existing
		Optional<Channel> channelOpt = thingRepository.getChannel(channelAddress);
		if (channelOpt.isPresent()) {
			Channel channel = channelOpt.get();
			try {
				channel.assertReadAllowed(this.role);
				return Optional.ofNullable(JsonUtils.getAsJsonElement(databus.getValue(channel).orElse(null)));
			} catch (AccessDeniedException | NotImplementedException e) {
				return Optional.empty();
				// TODO log error message: not allowed - or conversion not implemented
			}
		} else {
			return Optional.empty();
			// TODO log error message: channel not found
		}
	}

	@Override
	protected Optional<WebSocket> getWebsocket() {
		return this.edgeWebsocketHandler.getWebsocket();
	}
}
