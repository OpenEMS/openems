package io.openems.backend.common.metadata;

public class EdgeUser {

	private final int id;
	private final String userId;
	private final String edgeId;

	public EdgeUser(int id, String edgeId, String userId) {
		this.id = id;
		this.edgeId = edgeId;
		this.userId = userId;
	}

	public int getId() {
		return this.id;
	}

	public String getEdgeId() {
		return this.edgeId;
	}

	public String getUserId() {
		return this.userId;
	}
}
