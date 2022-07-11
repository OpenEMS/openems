package io.openems.backend.metadata.dummy;

import io.openems.backend.common.metadata.Edge;
import io.openems.common.channel.Level;
import io.openems.common.types.EdgeConfig;

public class MyEdge extends Edge {

	private final String apikey;
	private final String setupPassword;

	public MyEdge(DummyMetadata parent, String id, String apikey, String setupPassword, String comment, String version,
			String producttype, Level sumState, EdgeConfig config) {
		super(parent, id, comment, version, producttype, sumState, config, null, null);
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
