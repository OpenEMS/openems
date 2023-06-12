package io.openems.edge.scheduler.daily;

import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.TreeMap;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.DateUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.scheduler.api.Scheduler;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Scheduler.Daily", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SchedulerDailyImpl extends AbstractOpenemsComponent
		implements SchedulerDaily, Scheduler, OpenemsComponent {

	private final TreeMap<LocalTime, LinkedHashSet<String>> controllerSchedule = new TreeMap<>();

	@Reference
	private ComponentManager componentManager;

	private Config config = null;

	public SchedulerDailyImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Scheduler.ChannelId.values(), //
				SchedulerDaily.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		if (config.controllerScheduleJson() != null && !config.controllerScheduleJson().trim().isEmpty()) {
			this.updateControllerSchedule(JsonUtils.getAsJsonArray(JsonUtils.parse(config.controllerScheduleJson())));
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Updates the internal "controllerSchedule" map.
	 *
	 * @param schedule the configured schedule as {@link JsonArray}
	 * @throws OpenemsNamedException on error
	 */
	private void updateControllerSchedule(JsonArray schedule) throws OpenemsNamedException {
		this.controllerSchedule.clear();
		for (JsonElement period : schedule) {
			var time = DateUtils.parseLocalTimeOrError(JsonUtils.getAsString(period, "time"));
			var jControllerIds = JsonUtils.getAsJsonArray(period, "controllers");
			var controllerIds = new LinkedHashSet<String>();
			for (JsonElement controllerId : jControllerIds) {
				controllerIds.add(JsonUtils.getAsString(controllerId));
			}
			this.controllerSchedule.put(time, controllerIds);
		}
	}

	@Override
	public LinkedHashSet<String> getControllers() {
		var result = new LinkedHashSet<String>();

		// add "Always Run Before" Controllers
		for (String controllerId : this.config.alwaysRunBeforeController_ids()) {
			this.addControllerById(result, controllerId);
		}

		// add "Daily Schedule" Controllers
		var scheduledIds = this.controllerSchedule.lowerEntry(LocalTime.now(this.componentManager.getClock()));
		if (scheduledIds == null) {
			// No entry found -> take the one with highest time, i.e. the one before
			// midnight.
			scheduledIds = this.controllerSchedule.lastEntry();
		}
		if (scheduledIds != null) {
			// Do we have Controller-IDs?
			for (String controllerId : scheduledIds.getValue()) {
				this.addControllerById(result, controllerId);
			}
		}

		// add "Always Run After" Controllers
		for (String controllerId : this.config.alwaysRunAfterController_ids()) {
			this.addControllerById(result, controllerId);
		}

		return result;
	}

	private void addControllerById(LinkedHashSet<String> result, String controllerId) {
		if (controllerId.equals("")) {
			return;
		}
		result.add(controllerId);
	}

}