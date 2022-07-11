package io.openems.edge.core.componentmanager;

import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

import io.openems.common.worker.AbstractWorker;

public abstract class ComponentManagerWorker extends AbstractWorker implements ConfigurationListener {

	protected final ComponentManagerImpl parent;

	public ComponentManagerWorker(ComponentManagerImpl parent) {
		this.parent = parent;
	}

	/**
	 * Gets some output that is suitable for a continuous Debug log. Returns 'null'
	 * by default which causes no output.
	 *
	 * @return the debug log output
	 */
	public String debugLog() {
		return null;
	}

	@Override
	public void configurationEvent(ConfigurationEvent event) {
	}

}
