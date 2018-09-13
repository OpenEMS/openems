package io.openems.edge.scheduler.allalphabetically;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import io.openems.edge.controller.api.Controller;
import io.openems.edge.scheduler.api.AbstractScheduler;
import io.openems.edge.scheduler.api.Scheduler;

/**
 * This Scheduler returns all existing Controllers ordered by their ID.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Scheduler.AllAlphabetically", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class AllAlphabetically extends AbstractScheduler implements Scheduler {

	private final Logger log = LoggerFactory.getLogger(AllAlphabetically.class);

	private Map<String, Controller> _controllers = new ConcurrentHashMap<>();

	private final List<Controller> sortedControllers = new ArrayList<>();

	private String[] controllersIds = new String[0];

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	void addController(Controller controller) {
		if (controller != null && controller.id() != null) {
			this._controllers.put(controller.id(), controller);
		}
		this.updateSortedControllers();
	}

	void removeController(Controller controller) {
		if (controller != null && controller.id() != null) {
			this._controllers.remove(controller.id(), controller);
		}
		this.updateSortedControllers();
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled(), config.cycleTime());

		this.controllersIds = config.controllers_ids();
		this.updateSortedControllers();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public List<Controller> getControllers() {
		return this.sortedControllers;
	}

	/**
	 * Fills sortedControllers using the order of controller_ids config property
	 */
	private synchronized void updateSortedControllers() {
		HashMap<String, Controller> allControllers = new HashMap<>(this._controllers);
		this.sortedControllers.clear();
		// add sorted controllers
		for (String id : this.controllersIds) {
			if (id.equals("")) {
				continue;
			}
			Controller controller = allControllers.remove(id);
			if (controller == null) {
				log.warn("Required Controller [" + id + "] is not available.");
			} else {
				this.sortedControllers.add(controller);
			}
		}
		// add remaining controllers
		allControllers.entrySet().stream() //
				.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey())) //
				.map(e -> e.getValue()) //
				.forEach(c -> {
					this.sortedControllers.add(c);
				});
	}
}
