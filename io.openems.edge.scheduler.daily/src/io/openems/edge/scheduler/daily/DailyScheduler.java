package io.openems.edge.scheduler.daily;

import java.time.Clock;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
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
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.scheduler.api.AbstractScheduler;
import io.openems.edge.scheduler.api.Scheduler;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Scheduler.Daily", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class DailyScheduler extends AbstractScheduler implements Scheduler, OpenemsComponent {

	private final Clock clock;
	private final TreeMap<LocalTime, List<String>> controllerSchedule = new TreeMap<>();

	@Reference
	protected ComponentManager componentManager;

	private Config config = null;

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

	public DailyScheduler() {
		this(Clock.systemDefaultZone());
	}

	protected DailyScheduler(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Scheduler.ChannelId.values(), //
				ThisChannelId.values() //
		);
		this.clock = clock;
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.cycleTime());
		this.config = config;
		this.updateControllerSchedule(JsonUtils.getAsJsonArray(JsonUtils.parse(config.controllerScheduleJson())));
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
			LocalTime time = LocalTime.parse(JsonUtils.getAsString(period, "time"));
			JsonArray jControllerIds = JsonUtils.getAsJsonArray(period, "controllers");
			List<String> controllerIds = new ArrayList<>();
			for (JsonElement controllerId : jControllerIds) {
				controllerIds.add(JsonUtils.getAsString(controllerId));
			}
			this.controllerSchedule.put(time, controllerIds);
		}
	}

	@Override
	public LinkedHashSet<Controller> getControllers() throws OpenemsNamedException {
		LinkedHashSet<Controller> result = new LinkedHashSet<>();

		// add "Always Run Before" Controllers
		for (String controllerId : this.config.alwaysRunBeforeController_ids()) {
			this.addControllerById(result, controllerId);
		}

		// add "Daily Schedule" Controllers
		Entry<LocalTime, List<String>> scheduledIds = this.controllerSchedule.lowerEntry(LocalTime.now(this.clock));
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

	private void addControllerById(LinkedHashSet<Controller> result, String controllerId) throws OpenemsNamedException {
		if (controllerId.equals("")) {
			return;
		}
		Controller controller = this.componentManager.getPossiblyDisabledComponent(controllerId);
		result.add(controller);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}
}