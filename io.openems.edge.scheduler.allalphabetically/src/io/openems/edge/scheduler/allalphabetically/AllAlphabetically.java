package io.openems.edge.scheduler.allalphabetically;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.controller.api.Controller;
import io.openems.edge.scheduler.api.AbstractScheduler;
import io.openems.edge.scheduler.api.Scheduler;

/**
 * This Scheduler returns all existing Controllers ordered by their ID.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Scheduler.AllAlphabetically", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class AllAlphabetically extends AbstractScheduler implements Scheduler {

	// private final Logger log = LoggerFactory.getLogger(AllAlphabetically.class);

	private List<Controller> _controllers = new CopyOnWriteArrayList<Controller>();

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	void addController(Controller controller) {
		this._controllers.add(controller);
	}

	void removeController(Controller controller) {
		this._controllers.remove(controller);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled(), config.cycleTime());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public List<Controller> getControllers() {
		return this._controllers.stream().sorted((c1, c2) -> c1.id().compareTo(c2.id())).collect(Collectors.toList());
	}
}
