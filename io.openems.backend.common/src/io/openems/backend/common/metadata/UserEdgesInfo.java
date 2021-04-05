package io.openems.backend.common.metadata;

import java.util.HashMap;
import java.util.Map;

public class UserEdgesInfo {

	private final BackendUser user;
	private final Map<String, Edge> edges = new HashMap<>();

	public UserEdgesInfo(BackendUser user) {
		super();
		this.user = user;
	}

	public void addDevice(Edge device) {
		this.edges.put(device.getId(), device);
	}

	public BackendUser getUser() {
		return user;
	}

	public Map<String, Edge> getEdges() {
		return this.edges;
	}

	@Override
	public String toString() {
		return "UserEdgesInfo [user=" + user + ", edges=" + edges + "]";
	}
}
