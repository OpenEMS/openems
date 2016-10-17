package io.openems.core.scheduler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import io.openems.api.controller.Controller;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InjectionException;
import io.openems.api.thing.Thing;
import io.openems.core.controller.ControllerFactory;
import io.openems.core.databus.Databus;
import io.openems.core.utilities.AbstractWorker;

public abstract class Scheduler extends AbstractWorker implements Thing {
	public final static String THINGID_PREFIX = "_scheduler";
	private static int instanceCounter = 0;
	protected final List<Controller> controllers = new CopyOnWriteArrayList<>();
	protected final Databus databus;

	public Scheduler(Databus databus) {
		super(THINGID_PREFIX + instanceCounter++);
		this.databus = databus;
	}

	/**
	 * While initialization in ThingFactory databus is not populated. This method is used to supply the Things anyway.
	 *
	 * @param controller
	 * @param things
	 * @throws InjectionException
	 * @throws ConfigException
	 */
	public void addController(Controller controller, Map<String, Thing> things)
			throws InjectionException, ConfigException {
		ControllerFactory.generateMappings(controller, things);
		controllers.add(controller);
	}
}
