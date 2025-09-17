package io.openems.edge.scheduler.fromcalendar;

import static io.openems.edge.scheduler.fromcalendar.Utils.getNextPeriod;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jscalendar.JSCalendar.Task;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.scheduler.api.Scheduler;
import io.openems.edge.scheduler.fromcalendar.Utils.HighPeriod;
import io.openems.edge.scheduler.fromcalendar.Utils.Payload;

/**
 * This Scheduler returns all active Controllers from the calendar setting
 * including the before/after controllers.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Scheduler.FromCalendar", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SchedulerFromCalendarImpl extends AbstractOpenemsComponent
		implements SchedulerFromCalendar, Scheduler, OpenemsComponent {

	private Config config = null;
	private ImmutableList<Task<Payload>> schedule = ImmutableList.of();
	private HighPeriod nextCtrlPeriod;
	private Clock clock;

	@Reference
	private ComponentManager componentManager;

	public SchedulerFromCalendarImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Scheduler.ChannelId.values(), //
				SchedulerFromCalendar.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		this.applyConfig(config);
		super.modified(context, config.id(), config.alias(), config.enabled());
	}

	private void applyConfig(Config config) {
		this.config = config;
		this.clock = this.componentManager.getClock();
		this.schedule = Utils.parseConfig(this.config.controllerSchedule());
		this.updatePeriod();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public LinkedHashSet<String> getControllers() {
		var result = new LinkedHashSet<String>();

		// add "Always Run Before" Controllers
		for (String controllerId : this.config.alwaysRunBeforeController_ids()) {
			this.addControllerById(result, controllerId);
		}

		// add active controllers from calendar
		if (this.nextCtrlPeriod != null //
				&& this.nextCtrlPeriod.from().isBefore(this.clock.instant()) //
				&& this.nextCtrlPeriod.to().isAfter(this.clock.instant())) {

			for (String controllerId : this.nextCtrlPeriod.controllerIds()) {
				this.addControllerById(result, controllerId);
			}
		}

		// add "Always Run After" Controllers
		for (

		String controllerId : this.config.alwaysRunAfterController_ids()) {
			this.addControllerById(result, controllerId);
		}
		this.updatePeriod();
		return result;

	}

	private void addControllerById(LinkedHashSet<String> result, String controllerId) {
		if (controllerId.isEmpty()) {
			return;
		}
		result.add(controllerId);
	}

	private synchronized void updatePeriod() {
		var now = ZonedDateTime.now(this.clock);
		this.nextCtrlPeriod = getNextPeriod(now, this.schedule);
	}
}
