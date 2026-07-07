package io.openems.backend.common.test;

import java.time.ZonedDateTime;

import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Metadata;

public class DummyEdge extends Edge {

	public DummyEdge(Metadata parent, String id, String comment, String version, String producttype,
			ZonedDateTime lastmessage) {
		super(parent, id, comment, version, producttype, lastmessage);
	}

}
