package io.openems.backend.metadata.file.provider;

import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.Edge;
import io.openems.common.session.Role;

public class MyEdge extends Edge {

	private final String apikey;
	private final Role role;

	public MyEdge(int id, String name, String comment, String producttype, Role role, String apikey,
			JsonObject jConfig) {
		super(id, name, comment, producttype, jConfig);
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
