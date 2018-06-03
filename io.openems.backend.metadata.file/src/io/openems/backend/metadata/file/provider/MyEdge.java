package io.openems.backend.metadata.file.provider;

import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.Edge;
import io.openems.common.session.Role;

public class MyEdge extends Edge {

	private final Role role;

	public MyEdge(int id, String apikey, String name, String comment, State state, String version, String producttype,
			JsonObject jConfig, Role role) {
		super(id, apikey, name, comment, state, version, producttype, jConfig, null, null);
		this.role = role;
	}

	public Role getRole() {
		return role;
	}
}
