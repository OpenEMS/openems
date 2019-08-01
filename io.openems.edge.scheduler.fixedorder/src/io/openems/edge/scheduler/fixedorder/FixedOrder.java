package io.openems.edge.scheduler.fixedorder;

import java.util.LinkedHashSet;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.ComponentManager;
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

	@Reference
	protected ComponentManager componentManager;

	private Config config;

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
		super.activate(context, config.id(), config.alias(), config.enabled(), config.cycleTime());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public LinkedHashSet<Controller> getControllers() throws OpenemsNamedException {
		LinkedHashSet<Controller> result = new LinkedHashSet<>();

		// add sorted controllers
		for (String id : this.config.controllers_ids()) {
			if (id.equals("")) {
				continue;
			}
			Controller controller = this.componentManager.getComponent(id);
			result.add(controller);
		}

		return result;
	}

}
