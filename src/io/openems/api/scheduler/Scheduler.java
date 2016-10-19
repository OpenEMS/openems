package io.openems.api.scheduler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.openems.api.controller.Controller;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InjectionException;
import io.openems.api.thing.Thing;
import io.openems.core.databus.Databus;
import io.openems.core.utilities.AbstractWorker;
import io.openems.core.utilities.ControllerFactory;

public abstract class Scheduler extends AbstractWorker implements Thing {
	public final static String THINGID_PREFIX = "_scheduler";
	private static int instanceCounter = 0;
	protected final List<Controller> controllers = new CopyOnWriteArrayList<>();
	protected final Databus databus;

	public Scheduler(Databus databus) {
		super(THINGID_PREFIX + instanceCounter++);
		this.databus = databus;
	}

	public void addController(Controller controller) throws InjectionException, ConfigException {
		ControllerFactory.generateMappings(controller, databus);
		controllers.add(controller);
	}
}
