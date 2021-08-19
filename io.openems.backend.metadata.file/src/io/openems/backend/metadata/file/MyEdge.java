package io.openems.backend.metadata.file;

import io.openems.backend.common.metadata.Edge;
import io.openems.common.channel.Level;
import io.openems.common.types.EdgeConfig;

public class MyEdge extends Edge {

	private final String apikey;
	private final String setupPassword;

	public MyEdge(String id, String apikey, String setupPassword, String comment, State state, String version,
			String producttype, Level sumState, EdgeConfig config) {
		super(id, comment, state, version, producttype, sumState, config);
		this.apikey = apikey;
		this.setupPassword = setupPassword;
	}

	public String getApikey() {
		return this.apikey;
	}

	public String getSetupPassword() {
		return this.setupPassword;
	}

}
