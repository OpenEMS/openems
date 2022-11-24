package io.openems.edge.controller.api.websocket;

import java.time.ZonedDateTime;
import java.util.List;

import com.google.common.collect.Lists;

import io.openems.common.OpenemsConstants;
import io.openems.common.jsonrpc.response.AuthenticateResponse.EdgeMetadata;
import io.openems.common.session.Role;

public class Utils {
	/**
	 * Gets the EdgeMetadata for one Edge.
	 *
	 * @param role the {@link Role} for this Edge
	 * @return a list of {@link EdgeMetadata}s
	 */
	public static List<EdgeMetadata> getEdgeMetadata(Role role) {
		return Lists.newArrayList(new EdgeMetadata(//
				WebsocketApi.EDGE_ID, // Edge-ID
				WebsocketApi.EDGE_COMMENT, // Comment
				WebsocketApi.EDGE_PRODUCT_TYPE, // Product-Type
				OpenemsConstants.VERSION, // Version
				role, // Role
				true, // Is Online
				ZonedDateTime.now() // now
		));
	}
}
