package io.openems.backend.metadata.odoo;

import java.time.ZonedDateTime;

import io.openems.backend.common.metadata.Edge;
import io.openems.common.channel.Level;
import io.openems.common.types.EdgeConfig;

public class MyEdge extends Edge {

	private final int odooId;
	private final String apikey;

	public MyEdge(OdooMetadata parent, int odooId, String edgeId, String apikey, String comment, State state,
			String version, String producttype, Level sumState, EdgeConfig config, ZonedDateTime lastMessage,
			ZonedDateTime lastUpdate) {
		super(parent, edgeId, comment, state, version, producttype, sumState, config, lastMessage, lastUpdate);
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
