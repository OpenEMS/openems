package io.openems.edge.controller.api.websocket;

import java.time.ZonedDateTime;

import io.openems.common.OpenemsConstants;
import io.openems.common.jsonrpc.response.GetEdgesResponse.EdgeMetadata;
import io.openems.common.session.Role;

public class Utils {

	/**
	 * Gets the EdgeMetadata for one Edge.
	 *
	 * @param role the {@link Role} for this Edge
	 * @return the {@link EdgeMetadata}
	 */
	public static EdgeMetadata getEdgeMetadata(Role role) {
		return new EdgeMetadata(//
				ControllerApiWebsocket.EDGE_ID, // Edge-ID
				ControllerApiWebsocket.EDGE_COMMENT, // Comment
				ControllerApiWebsocket.EDGE_PRODUCT_TYPE, // Product-Type
				OpenemsConstants.VERSION, // Version
				role, // Role
				true, // Is Online
				ZonedDateTime.now(), // now
				null, //
				ControllerApiWebsocket.SUM_STATE //
		);
	}

}
