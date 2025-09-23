package io.openems.edge.scheduler.jscalendar;

import static io.openems.edge.scheduler.jscalendar.Utils.getNextPeriod;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
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
import io.openems.edge.scheduler.jscalendar.Utils.HighPeriod;
import io.openems.edge.scheduler.jscalendar.Utils.Payload;

/**
 * This Scheduler returns all active Controllers from the calendar setting
 * including the before/after controllers.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Scheduler.JSCalendar", //
		immediate = true, //
		configurationPolicy = REQUIRE)
//CHECKSTYLE:OFF
public class SchedulerJSCalendarImpl extends AbstractOpenemsComponent
		implements SchedulerJSCalendar, Scheduler, OpenemsComponent {
	// CHECKSTYLE:ON

	private Config config = null;
	private ImmutableList<Task<Payload>> schedule = ImmutableList.of();
	private HighPeriod nextCtrlPeriod;

	@Reference
	private ComponentManager componentManager;

	public SchedulerJSCalendarImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Scheduler.ChannelId.values(), //
				SchedulerJSCalendar.ChannelId.values());
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
		this.schedule = Utils.parseConfig(this.config.jsCalendar());
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

		final var now = Instant.now(this.componentManager.getClock());
		// add active controllers from calendar
		if (this.nextCtrlPeriod != null //
				&& this.nextCtrlPeriod.from().isBefore(now) //
				&& this.nextCtrlPeriod.to().isAfter(now)) {

			for (var controllerId : this.nextCtrlPeriod.controllerIds()) {
				this.addControllerById(result, controllerId);
			}
		}

		// add "Always Run After" Controllers
		for (var controllerId : this.config.alwaysRunAfterController_ids()) {
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
		var now = ZonedDateTime.now(this.componentManager.getClock());
		this.nextCtrlPeriod = getNextPeriod(now, this.schedule);
	}
}
