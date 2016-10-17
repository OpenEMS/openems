package io.openems.core.bridge;

import io.openems.api.device.Device;
import io.openems.api.thing.Thing;
import io.openems.core.utilities.AbstractWorker;

public abstract class Bridge extends AbstractWorker implements Thing {
	public final static String THINGID_PREFIX = "_bridge";
	private static int instanceCounter = 0;
	protected Device[] devices = null;

	/**
	 * Initialize the Thread with a name
	 *
	 * @param name
	 */
	public Bridge() {
		super(THINGID_PREFIX + instanceCounter++);
	}

	@Override
	public String getThingId() {
		return getName();
	}

	public void setDevices(Device... devices) {
		this.devices = devices;
	}
}
