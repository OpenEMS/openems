package io.openems.backend.metadata.file.provider;

import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.Edge;
import io.openems.common.session.Role;

public class MyEdge extends Edge {

	private final String apikey;
	private final Role role;

	public MyEdge(int id, String name, String comment, State state, String producttype, String version, Role role,
			String apikey, JsonObject jConfig) {
		super(id, name, comment, state, version, producttype, jConfig, null, null);
		this.role = role;
		this.apikey = apikey;
	}

	public String getApikey() {
		return apikey;
	}

	public Role getRole() {
		return role;
	}
}
