package io.openems.backend.common.edge;

import io.openems.common.websocket.WsConnection;

public interface EdgeClient extends WsConnection {

	/**
	 * Gets the Edge-ID.
	 *
	 * @return the Edge-ID; possibly null
	 */
	String getEdgeId();

	/**
	 * Gets the Edge-ID or "UNKNOWN".
	 *
	 * @return never null
	 */
	default String getEdgeIdString() {
		return this.getEdgeId() != null //
				? this.getEdgeId() //
				: "UNKNOWN";
	}

}
