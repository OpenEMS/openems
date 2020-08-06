package io.openems.backend.metadata.odoo;

import io.openems.backend.metadata.api.Edge;
import io.openems.common.types.EdgeConfig;

public class MyEdge extends Edge {

	private final int odooId;
	private final String apikey;

	public MyEdge(int odooId, String edgeId, String apikey, String comment, State state, String version,
			String producttype, EdgeConfig config) {
		super(edgeId, comment, state, version, producttype, config);
		this.apikey = apikey;
		this.odooId = odooId;
	}

	public int getOdooId() {
		return this.odooId;
	}

	public String getApikey() {
		return apikey;
	}

}
