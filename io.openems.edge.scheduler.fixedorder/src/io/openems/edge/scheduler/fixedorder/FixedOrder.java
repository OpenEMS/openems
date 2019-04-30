package io.openems.edge.scheduler.fixedorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.cm.ConfigurationAdmin;
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

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.scheduler.api.AbstractScheduler;
import io.openems.edge.scheduler.api.Scheduler;

/**
 * This Scheduler takes a list of Component IDs and returns the Controllers
 * statically sorted by this order.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Scheduler.FixedOrder", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class FixedOrder extends AbstractScheduler implements Scheduler {

	private final Logger log = LoggerFactory.getLogger(FixedOrder.class);

	private final List<Controller> sortedControllers = new ArrayList<>();

	private String[] controllersIds = new String[0];

	@Reference
	protected ConfigurationAdmin cm;

	private Map<String, Controller> _controllers = new ConcurrentHashMap<>();

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	void addController(Controller controller) {
		this._controllers.put(controller.id(), controller);
		this.updateSortedControllers();
	}

	void removeController(Controller controller) {
		this._controllers.remove(controller.id(), controller);
		this.updateSortedControllers();
	}

	public enum ThisChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;

		private ThisChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	protected FixedOrder() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Scheduler.ChannelId.values(), //
				ThisChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		this.controllersIds = config.controllers_ids();
		this.updateSortedControllers();

		super.activate(context, config.id(), config.alias(), config.enabled(), config.cycleTime());

		// update filter for 'Controller'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Controller",
				config.controllers_ids())) {
			return;
		}
	}

	/**
	 * Fills sortedControllers using the order of controller_ids config property
	 */
	private synchronized void updateSortedControllers() {
		this.sortedControllers.clear();
		for (String id : this.controllersIds) {
			Controller controller = this._controllers.get(id);
			if (controller == null) {
				log.warn("Required Controller [" + id + "] is not available.");
			} else {
				this.sortedControllers.add(controller);
			}
		}
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
