package io.openems.edge.timedata.rrd4j;

import io.openems.edge.timedata.rrd4j.version.Version;

public final class Rrd4jConstants {

	public static final String RRD4J_PATH = "rrd4j";
	public static final String DEFAULT_DATASOURCE_NAME = "value";
	public static final int DEFAULT_STEP_SECONDS = 300;
	public static final int DEFAULT_HEARTBEAT_SECONDS = DEFAULT_STEP_SECONDS;

	/**
	 * Creates a string of the default datasource name with the version included.
	 * 
	 * @param version the version to include in the name
	 * @return the datasource name
	 */
	public static final String createDefaultDatasourceNameOf(Version version) {
		return DEFAULT_DATASOURCE_NAME + "_v_" + version.getVersion();
	}

	private Rrd4jConstants() {
	}

}
