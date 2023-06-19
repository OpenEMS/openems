package io.openems.edge.scheduler.allalphabetically;

import java.util.Comparator;
import java.util.LinkedHashSet;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.scheduler.api.Scheduler;

/**
 * This Scheduler returns all existing Controllers ordered by their ID.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Scheduler.AllAlphabetically", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SchedulerAllAlphabeticallyImpl extends AbstractOpenemsComponent
		implements SchedulerAllAlphabetically, Scheduler, OpenemsComponent {

	@Reference
	private ComponentManager componentManager;

	private Config config;

	public SchedulerAllAlphabeticallyImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Scheduler.ChannelId.values(), //
				SchedulerAllAlphabetically.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public LinkedHashSet<String> getControllers() {
		var result = new LinkedHashSet<String>();

		// add sorted controllers
		for (String id : this.config.controllers_ids()) {
			if (id.equals("")) {
				continue;
			}
			result.add(id);
		}

		// add remaining controllers
		this.componentManager.getEnabledComponents().stream() //
				.filter(c -> c instanceof Controller) //
				.sorted(Comparator.comparing(OpenemsComponent::id)) //
				.forEach(c -> result.add(c.id()));

		return result;
	}
}
