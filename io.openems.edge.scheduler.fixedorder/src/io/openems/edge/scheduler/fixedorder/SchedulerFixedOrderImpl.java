package io.openems.edge.scheduler.fixedorder;

import java.util.LinkedHashSet;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.scheduler.api.Scheduler;

/**
 * This Scheduler takes a list of Component IDs and returns the Controllers
 * statically sorted by this order.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Scheduler.FixedOrder", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SchedulerFixedOrderImpl extends AbstractOpenemsComponent implements SchedulerFixedOrder, Scheduler {

	private final LinkedHashSet<String> controllerIds = new LinkedHashSet<>();

	public SchedulerFixedOrderImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Scheduler.ChannelId.values(), //
				SchedulerFixedOrder.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private synchronized void applyConfig(Config config) {
		this.controllerIds.clear();
		for (String id : config.controllers_ids()) {
			if (id.equals("")) {
				continue;
			}
			this.controllerIds.add(id);
		}
	}

	@Override
	public LinkedHashSet<String> getControllers() {
		return this.controllerIds;
	}

}
