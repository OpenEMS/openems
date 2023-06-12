package io.openems.backend.metadata.odoo;

import java.time.ZonedDateTime;

import io.openems.backend.common.metadata.Edge;

public class MyEdge extends Edge {

	private final int odooId;
	private final String apikey;

	public MyEdge(MetadataOdoo parent, int odooId, String edgeId, String apikey, String comment, String version,
			String producttype, ZonedDateTime lastMessage) {
		super(parent, edgeId, comment, version, producttype, lastMessage);
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
