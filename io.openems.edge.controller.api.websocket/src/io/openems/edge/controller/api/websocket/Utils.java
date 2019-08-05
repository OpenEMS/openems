package io.openems.edge.controller.api.websocket;

import java.util.ArrayList;
import java.util.List;

import io.openems.common.OpenemsConstants;
import io.openems.common.accesscontrol.RoleId;
import io.openems.common.jsonrpc.shared.EdgeMetadata;

public class Utils {
	/**
	 * Gets the EdgeMetadata for one Edge.
	 * 
	 * @param roleId the Role for this Edge
	 * @return a list of EdgeMetadatas
	 */
	public static List<EdgeMetadata> getEdgeMetadata(RoleId roleId) {
		List<EdgeMetadata> metadatas = new ArrayList<>();
		metadatas.add(new EdgeMetadata(//
				WebsocketApi.EDGE_ID, // Edge-ID
				WebsocketApi.EDGE_COMMENT, // Comment
				WebsocketApi.EDGE_PRODUCT_TYPE, // Product-Type
				OpenemsConstants.VERSION, // Version
				roleId, // Role
				true // Is Online
		));
		return metadatas;
	}
}
