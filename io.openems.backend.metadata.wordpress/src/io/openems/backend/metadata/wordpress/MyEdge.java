package io.openems.backend.metadata.wordpress;

import io.openems.backend.metadata.api.Edge;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.SemanticVersion;

public class MyEdge extends Edge {

	private final Role role;

	public MyEdge(String id, String apikey, String name, String comment, State state, SemanticVersion version, String producttype,
			EdgeConfig config, Role role) {
		super(id, apikey, comment, state, version.toString(), producttype, config, null, null);
		this.role = role;
	}

	public Role getRole() {
		return role;
	}
}
