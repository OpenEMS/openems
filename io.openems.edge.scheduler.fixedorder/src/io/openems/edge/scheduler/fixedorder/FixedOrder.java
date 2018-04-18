package io.openems.edge.scheduler.fixedorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.cm.ConfigurationAdmin;
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
 * This Scheduler takes a list of Component IDs and returns the Controllers
 * statically sorted by this order.
 * 
 * @author stefan.feilmeier
 *
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Scheduler.FixedOrder", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class FixedOrder extends AbstractScheduler implements Scheduler {

	private final Logger log = LoggerFactory.getLogger(FixedOrder.class);

	@Reference
	protected ConfigurationAdmin cm;

	private final List<Controller> sortedControllers = new ArrayList<>();

	private Map<String, Controller> _controllers = new ConcurrentHashMap<>();

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private void addControllers(Controller controller) {
		this._controllers.put(controller.id(), controller);
	}

	@Activate
	void activate(Config config) {
		// update filter for 'Controllers'
		if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "Controllers",
				config.controllers_ids())) {
			return;
		}

		/*
		 * Fill sortedControllers using the order of controller_ids config property
		 */
		for (String id : config.controllers_ids()) {
			Controller controller = this._controllers.get(id);
			if (controller == null) {
				log.warn("Required Controller [" + id + "] is not available.");
			} else {
				this.sortedControllers.add(controller);
			}
		}

		super.activate(config.id(), config.enabled(), config.cycleTime());
	}

	@Deactivate
	protected void deactivate() {
		this.sortedControllers.clear();
		super.deactivate();
	}

	@Override
	public List<Controller> getControllers() {
		return this.sortedControllers;
	}

}
