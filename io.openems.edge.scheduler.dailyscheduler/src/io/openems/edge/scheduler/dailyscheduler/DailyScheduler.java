package io.openems.edge.scheduler.dailyscheduler;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.scheduler.api.AbstractScheduler;
import io.openems.edge.scheduler.api.Scheduler;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Scheduler.DailyScheduler", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)

public class DailyScheduler extends AbstractScheduler implements Scheduler, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(DailyScheduler.class);

	private final List<Controller> sortedControllers = new ArrayList<>();
	private final List<Controller> listOfControllers = new ArrayList<>();

	private final TreeMap<LocalTime, List<Controller>> contollersSchedule = new TreeMap<>();

	private Map<String, Controller> _controllers = new ConcurrentHashMap<>();

	private String controllersIdsJson = new String();

	private String[] controllersIds = new String[0];

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE, target = "(enabled=true)")
	protected synchronized void addController(Controller controller) throws OpenemsNamedException {
		if (controller != null && controller.id() != null) {
			this._controllers.put(controller.id(), controller);
		}
		this.updateControllers();
	}

	protected synchronized void removeController(Controller controller) throws OpenemsNamedException {
		if (controller != null && controller.id() != null) {
			this._controllers.remove(controller.id(), controller);
		}
		this.updateControllers();
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

	public DailyScheduler() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Scheduler.ChannelId.values(), //
				ThisChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.enabled(), config.cycleTime());
		this.controllersIdsJson = config.controllers_ids_json();
		this.controllersIds = config.controllers_ids();
		this.updateControllers();
	}

	/**
	 * Adds Controllers using the order of controller_ids and controllers_ids_json
	 * Config property
	 */

	private synchronized void updateControllers() throws OpenemsNamedException {
		this.listOfControllers.clear();

		// Controllers that are activated always.
		for (String id : this.controllersIds) {
			Controller alwaysRunningController = this._controllers.get(id);
			if (alwaysRunningController == null) {
				log.warn("Required Controller [" + id + "] is not available.");
			} else {
				this.listOfControllers.add(alwaysRunningController);
			}
		}

		// Controllers that are activated for a given time based on the Json input.
		if (this.controllersIdsJson != null && !(this.controllersIdsJson.isEmpty())) {
			try {
				JsonArray controllerTime = JsonUtils.getAsJsonArray(JsonUtils.parse(this.controllersIdsJson));
				if (controllerTime != null) {
					for (JsonElement element : controllerTime) {
						LocalTime Time = LocalTime.parse(JsonUtils.getAsString(element, "time"));
						JsonArray JsonControllers = JsonUtils.getAsJsonArray(element, "controller");
						List<Controller> listOfControllersJson = new ArrayList<>();

						for (JsonElement Jsonid : JsonControllers) {
							Controller controller = this._controllers.get(JsonUtils.getAsString(Jsonid));
							if (controller == null) {
								this.contollersSchedule.put(Time, null);
							} else {
								listOfControllersJson.add(controller);
								this.contollersSchedule.put(Time, listOfControllersJson);
							}
						}
					}
				}
			} catch (NullPointerException e) {
				throw new OpenemsException("Unable to set values [" + controllersIds + "] " + e.getMessage());
			}
		}
	}

	@Deactivate
	protected void deactivate() {
		this.sortedControllers.clear();
		super.deactivate();
	}

	/**
	 * returns the Controllers from both lists of controllers based on given time in
	 * a day.
	 */

	@Override
	public List<Controller> getControllers() {
		this.sortedControllers.clear();

		LocalTime currentTime = LocalTime.now();

		// returns all the controllers that are activated at given time.
		if (!(this.contollersSchedule.isEmpty())) {
			if (this.contollersSchedule.lowerEntry(currentTime) != null) {
				if (this.contollersSchedule.lowerEntry(currentTime).getValue() == null) {
					log.warn("No controllers specified in config Json field");
				} else {
					System.out.println(this.contollersSchedule.lowerEntry(currentTime).getValue());
					this.sortedControllers.addAll(this.contollersSchedule.lowerEntry(currentTime).getValue());
				}
			}
		}
		this.sortedControllers.addAll(listOfControllers);
		return this.sortedControllers;
	}
}
