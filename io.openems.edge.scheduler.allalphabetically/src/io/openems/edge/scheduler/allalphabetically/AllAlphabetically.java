package io.openems.edge.scheduler.allalphabetically;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.scheduler.api.AbstractScheduler;
import io.openems.edge.scheduler.api.Scheduler;

/**
 * This Scheduler returns all existing Controllers ordered by their ID.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Scheduler.AllAlphabetically", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class AllAlphabetically extends AbstractScheduler implements Scheduler, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(AllAlphabetically.class);

	private Map<String, Controller> _controllers = new ConcurrentHashMap<>();

	private final List<Controller> sortedControllers = new ArrayList<>();

	private String[] controllersIds = new String[0];

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	protected synchronized void addController(Controller controller) {
		if (controller != null && controller.id() != null) {
			this._controllers.put(controller.id(), controller);
		}
		this.updateSortedControllers();
	}

	protected synchronized void removeController(Controller controller) {
		if (controller != null && controller.id() != null) {
			this._controllers.remove(controller.id(), controller);
		}
		this.updateSortedControllers();
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled(), config.cycleTime());

		this.controllersIds = config.controllers_ids();
		this.updateSortedControllers();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public AllAlphabetically() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Override
	public List<Controller> getControllers() {
		return this.sortedControllers;
	}

	/**
	 * Fills sortedControllers using the order of controller_ids config property
	 */
	private synchronized void updateSortedControllers() {
		TreeMap<String, Controller> allControllers = new TreeMap<>(this._controllers);
		this.sortedControllers.clear();
		// add sorted controllers
		for (String id : this.controllersIds) {
			if (id.equals("")) {
				continue;
			}
			Controller controller = allControllers.remove(id);
			if (controller == null) {
				this.logWarn(this.log, "Required Controller [" + id + "] is not available.");
			} else {
				this.sortedControllers.add(controller);
			}
		}

		// add remaining controllers; TreeMap is sorted alphabetically by key
		Collection<Controller> remainingControllers = allControllers.values();
		for (Controller controller : remainingControllers) {
			this.sortedControllers.add(controller);
		}
	}
}
