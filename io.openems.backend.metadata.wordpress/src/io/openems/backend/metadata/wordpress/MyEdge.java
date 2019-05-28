package io.openems.backend.metadata.wordpress;

import io.openems.backend.metadata.api.Edge;
import io.openems.common.channel.Level;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.SemanticVersion;

public class MyEdge extends Edge {

	private final Role role;

	public MyEdge(String id, String apikey, String name, String comment, State state, String version, String producttype,
			EdgeConfig config, Role role, Integer soc, String ipv4, Level sumState) {
		super(id, apikey, comment, state, version, producttype, config, soc, ipv4, sumState);
		this.role = role;
	}

	public Role getRole() {
		return role;
	}
}
