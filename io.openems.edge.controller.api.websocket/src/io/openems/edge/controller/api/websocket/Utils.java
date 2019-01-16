package io.openems.edge.controller.api.websocket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import io.openems.common.OpenemsConstants;
import io.openems.common.jsonrpc.shared.EdgeMetadata;
import io.openems.common.session.Role;
import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(WebsocketApi c) {
		return Stream.of(//
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}

	/**
	 * Gets the EdgeMetadata for one Edge.
	 * 
	 * @param role the Role for this Edge
	 * @return a list of EdgeMetadatas
	 */
	public static List<EdgeMetadata> getEdgeMetadata(Role role) {
		List<EdgeMetadata> metadatas = new ArrayList<>();
		metadatas.add(new EdgeMetadata(//
				WebsocketApi.EDGE_ID, // Edge-ID
				WebsocketApi.EDGE_COMMENT, // Comment
				WebsocketApi.EDGE_PRODUCT_TYPE, // Product-Type
				OpenemsConstants.VERSION, // Version
				role, // Role
				true // Is Online
		));
		return metadatas;
	}
}
