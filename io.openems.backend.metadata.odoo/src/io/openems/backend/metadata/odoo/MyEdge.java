package io.openems.backend.metadata.odoo;

import io.openems.backend.common.metadata.Edge;
import io.openems.common.channel.Level;
import io.openems.common.types.EdgeConfig;

public class MyEdge extends Edge {

	private final int odooId;
	private final String apikey;

	public MyEdge(int odooId, String edgeId, String apikey, String comment, State state, String version,
			String producttype, Level sumState, EdgeConfig config) {
		super(edgeId, comment, state, version, producttype, sumState, config);
		this.apikey = apikey;
		this.odooId = odooId;
	}

	public int getOdooId() {
		return this.odooId;
	}

	public String getApikey() {
		return this.apikey;
	}

}
