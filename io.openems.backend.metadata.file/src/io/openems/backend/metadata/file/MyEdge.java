package io.openems.backend.metadata.file;

import io.openems.backend.metadata.api.Edge;
import io.openems.common.channel.Level;
import io.openems.common.types.EdgeConfig;

public class MyEdge extends Edge {

	private final String apikey;

	public MyEdge(String id, String apikey, String comment, State state, String version, String producttype,
			EdgeConfig config, Integer soc, String ipv4, Level sumState) {
		super(id, comment, state, version, producttype, config, soc, ipv4, sumState);
		this.apikey = apikey;
	}

	public String getApikey() {
		return apikey;
	}

}
