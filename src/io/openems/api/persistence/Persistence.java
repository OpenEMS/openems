package io.openems.api.persistence;

import io.openems.api.thing.Thing;
import io.openems.core.utilities.AbstractWorker;

public abstract class Persistence extends AbstractWorker implements Thing {

	public final static String THINGID_PREFIX = "_persistence";
	private static int instanceCounter = 0;

	public Persistence() {
		super(THINGID_PREFIX + instanceCounter++);
	}
}
