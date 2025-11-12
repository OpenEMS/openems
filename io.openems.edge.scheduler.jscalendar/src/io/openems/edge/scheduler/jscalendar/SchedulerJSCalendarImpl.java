package io.openems.edge.scheduler.jscalendar;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.util.LinkedHashSet;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.scheduler.api.Scheduler;
import io.openems.edge.scheduler.jscalendar.Utils.Payload;

/**
 * This Scheduler returns all active Controllers from the JSCalendar including
 * the before/after controllers.
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
	private JSCalendar.Tasks<Payload> tasks = JSCalendar.Tasks.empty();

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
		this.applyConfig(config);
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		this.applyConfig(config);
		super.modified(context, config.id(), config.alias(), config.enabled());
	}

	private void applyConfig(Config config) {
		this.config = config;
		this.tasks = config.enabled() //
				? JSCalendar.Tasks.fromStringOrEmpty(this.componentManager.getClock(), //
						config.jsCalendar(), Payload.serializer()) //
				: JSCalendar.Tasks.empty();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public synchronized LinkedHashSet<String> getControllers() {
		var result = new LinkedHashSet<String>();

		// Add "Always Run Before" Controllers
		this.addControllersById(result, this.config.alwaysRunBeforeController_ids());

		// Get and update Active-Task
		var activeTask = this.tasks.getActiveOneTask();

		// Add active controllers from JSCalendar
		if (activeTask != null) {
			this.addControllersById(result, activeTask.payload().controllerIds());
		}

		// Add "Always Run After" Controllers
		this.addControllersById(result, this.config.alwaysRunAfterController_ids());

		return result;
	}

	private void addControllersById(LinkedHashSet<String> result, String[] controllerIds) {
		for (var controllerId : controllerIds) {
			if (controllerId.isEmpty()) {
				continue;
			}
			result.add(controllerId);
		}
	}
}
