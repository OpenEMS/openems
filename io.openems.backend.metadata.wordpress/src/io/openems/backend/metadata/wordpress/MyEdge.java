package io.openems.backend.metadata.wordpress;

import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.Edge;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;

public class MyEdge extends Edge {

	private final Role role;

	public MyEdge(String id, String apikey, String name, String comment, State state, String version, String producttype,
			EdgeConfig config, Role role) {
		super(id, apikey, comment, state, version, producttype, config, null, null);
		this.role = role;
	}

	public Role getRole() {
		return role;
	}
}
