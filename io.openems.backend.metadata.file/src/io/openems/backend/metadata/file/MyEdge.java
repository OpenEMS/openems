//package io.openems.backend.metadata.file;
//
//import io.openems.backend.common.metadata.Edge;
//import io.openems.common.channel.Level;
//import io.openems.common.types.EdgeConfig;
//
//public class MyEdge extends Edge {
//
//	private final String apikey;
//	private final String setupPassword;
//
//	public MyEdge(FileMetadata parent, String id, String apikey, String setupPassword, String comment, State state,
//			String version, String producttype, Level sumState, EdgeConfig config) {
//		super(parent, id, comment, state, version, producttype, sumState, config, null, null);
//		this.apikey = apikey;
//		this.setupPassword = setupPassword;
//	}
//
//	public String getApikey() {
//		return this.apikey;
//	}
//
//	public String getSetupPassword() {
//		return this.setupPassword;
//	}
//
//}
