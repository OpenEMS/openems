package io.openems.edge.scheduler.fixedorder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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

@Designate(ocd = Config.class, factory = true)
@Component(name = "Scheduler.FixedOrder", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class FixedOrder extends AbstractScheduler implements Scheduler {

	private final Logger log = LoggerFactory.getLogger(FixedOrder.class);

	private final List<Controller> sortedControllers = new ArrayList<>();

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private List<Controller> controllers = new CopyOnWriteArrayList<>();

	void activate(Config config) {
		for (String id : config.controllers_ids()) {
			for (Controller controller : this.controllers) {
				if (controller.id().equals(id)) {
					this.sortedControllers.add(controller);
				}
			}
		}
		super.activate(config.service_pid(), config.enabled(), config.cycleTime());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.sortedControllers.clear();
	}

	@Override
	public List<Controller> getControllers() {
		return this.sortedControllers;
	}

}
