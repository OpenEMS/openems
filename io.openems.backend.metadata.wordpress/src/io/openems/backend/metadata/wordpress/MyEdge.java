package io.openems.backend.metadata.wordpress;

import io.openems.backend.metadata.api.Edge;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;

public class MyEdge extends Edge {

	private final Role role;
	private final String apikey;

	public MyEdge(String id, String apikey, String name, String comment, State state, String version,
			String producttype, EdgeConfig config, Role role) {
		super(id, comment, state, version, producttype, config);
		this.role = role;
		this.apikey = apikey;
	}

	public Role getRole() {
		return role;
	}

	public String getApikey() {
		return apikey;
	}
}
