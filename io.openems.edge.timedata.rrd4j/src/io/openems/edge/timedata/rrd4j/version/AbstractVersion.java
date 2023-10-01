package io.openems.edge.timedata.rrd4j.version;

import org.osgi.service.component.ComponentContext;

public abstract class AbstractVersion implements Version {

	private final int version;

	protected AbstractVersion(ComponentContext context) {
		this.version = (int) context.getProperties().get("version");
	}

	@Override
	public int getVersion() {
		return this.version;
	}

}
