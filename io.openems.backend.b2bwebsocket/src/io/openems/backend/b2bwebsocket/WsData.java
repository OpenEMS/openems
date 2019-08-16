package io.openems.backend.b2bwebsocket;

import io.openems.common.accesscontrol.RoleId;
import io.openems.common.websocket.SubscribedChannelsWorker;

public class WsData extends io.openems.common.websocket.WsData {

	private final SubscribedChannelsWorker worker;

	private RoleId roleId;

	public WsData(B2bWebsocket parent) {
		this.worker = new SubscribedChannelsWorkerMultipleEdges(parent, this);
	}

	public RoleId getRoleId() {
		return roleId;
	}

	public void setRoleId(RoleId roleId) {
		this.roleId = roleId;
	}

	@Override
	public void dispose() {
		this.worker.dispose();
	}

	/**
	 * Gets the SubscribedChannelsWorker to take care of subscribe to CurrentData.
	 *
	 * @return the SubscribedChannelsWorker
	 */
	SubscribedChannelsWorker getSubscribedChannelsWorker() {
		return this.worker;
	}

	@Override
	public String toString() {
		return "WsData{roleId=" + roleId + '}';
	}
}
