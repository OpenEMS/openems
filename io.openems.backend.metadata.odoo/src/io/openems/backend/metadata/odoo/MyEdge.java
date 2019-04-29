package io.openems.backend.metadata.odoo;

import io.openems.backend.metadata.api.Edge;
import io.openems.common.channel.Level;
import io.openems.common.types.EdgeConfig;

public class MyEdge extends Edge {

	private final int odooId;

	public MyEdge(int odooId, String edgeId, String apikey, String comment, State state, String version,
			String producttype, EdgeConfig config, Integer soc, String ipv4, Level sumState) {
		super(edgeId, apikey, comment, state, version, producttype, config, soc, ipv4, sumState);
		this.odooId = odooId;
	}

	public int getOdooId() {
		return this.odooId;
	}

}
