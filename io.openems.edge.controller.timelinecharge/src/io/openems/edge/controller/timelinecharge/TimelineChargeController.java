package io.openems.edge.controller.timelinecharge;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.TimelineCharge", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class TimelineChargeController extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(TimelineChargeController.class);

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public TimelineChargeController() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Reference
	protected ComponentManager componentManager;

	private Config config;

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private AvgFiFoQueue floatingChargerPower = new AvgFiFoQueue(10, 1);
	private State currentState = State.NORMAL;

	public enum State {
		NORMAL, MINSOC, CHARGESOC
	}

	/*
	 * Methods
	 */
	@Override
	public void run() throws OpenemsNamedException {
		// Check if all parameters are available
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());
		int essCapacity = 40000;
		int essSoc = ess.getSoc().value().getOrError();
		GridMode gridMode = ess.getGridMode().value().asEnum();

		// start controller logic
		if (gridMode.isUndefined()) {
			this.logWarn(this.log, "Grid-Mode is [UNDEFINED]");
		}
		switch (gridMode) {
		case ON_GRID:
		case UNDEFINED:
			break;
		case OFF_GRID:
			this.logInfo(this.log, "Doing nothing... Off-Grid");
			return;
		}

		// Apply allowed apparent
		ess.addPowerConstraint("TimelineChargeController maxApparentPower", Phase.ALL, Pwr.ACTIVE,
				Relationship.GREATER_OR_EQUALS, this.config.allowedApparent() * -1);
		ess.addPowerConstraint("TimelineChargeController maxApparentPower", Phase.ALL, Pwr.REACTIVE,
				Relationship.GREATER_OR_EQUALS, this.config.allowedApparent() * -1);
		ess.addPowerConstraint("TimelineChargeController maxApparentPower", Phase.ALL, Pwr.ACTIVE,
				Relationship.LESS_OR_EQUALS, this.config.allowedApparent());
		ess.addPowerConstraint("TimelineChargeController maxApparentPower", Phase.ALL, Pwr.REACTIVE,
				Relationship.LESS_OR_EQUALS, this.config.allowedApparent());

		SocPoint socPoint = getSoc();
		double requiredEnergy = (essCapacity / 100.0 * socPoint.getSoc()) - (essCapacity / 100.0 * essSoc);
		long requiredTimeCharger = (long) (requiredEnergy / (floatingChargerPower.avg() * 3600.0));

		// limit time to one day
		if (requiredTimeCharger > 60 * 60 * 24) {
			requiredTimeCharger = 60 * 60 * 24;
		}
		long requiredTimeGrid = (long) (requiredEnergy / ((floatingChargerPower.avg()
				+ (ess.getPower().getMinPower(ess, Phase.ALL, Pwr.ACTIVE) + ess.getActivePower().value().orElse(0))
						* -1)
				* 3600.0));
		// limit time to one day
		if (requiredTimeGrid > 60 * 60 * 24) {
			requiredTimeGrid = 60 * 60 * 24;
		}

		if (floatingChargerPower.avg() >= 1000
				&& !LocalDateTime.now().plusSeconds(requiredTimeCharger).isBefore(socPoint.getTime())
				&& LocalDateTime.now().plusSeconds(requiredTimeGrid).isBefore(socPoint.getTime())) {
			// Prevent discharge -> load with Pv
			this.logInfo(this.log, "TimelineChargeController Prevent Discharge");
			int calculatedPower = ess.getPower().fitValueIntoMinMaxPower(config.id(), ess, Phase.ALL, Pwr.ACTIVE, 0);
			ess.getSetActivePowerEquals().setNextWriteValue(calculatedPower);

		} else if (requiredTimeGrid > 0
				&& !LocalDateTime.now().plusSeconds(requiredTimeGrid).isBefore(socPoint.getTime())
				&& socPoint.getTime().isAfter(LocalDateTime.now())) {
			// Charge with grid + pv
			this.logInfo(this.log, "TimelineChargeController Charge with grid + pv");
			int calculatedPower = ess.getPower().fitValueIntoMinMaxPower(config.id(), ess, Phase.ALL, Pwr.ACTIVE,
					ess.getPower().getMinPower(ess, Phase.ALL, Pwr.ACTIVE));
			ess.getSetActivePowerEquals().setNextWriteValue(calculatedPower);

		} else {
			// soc point in the past -> Hold load
			int minSoc = getCurrentSoc().getSoc();
			int chargeSoc = minSoc - 5;
			if (chargeSoc <= 1) {
				chargeSoc = 1;
			}
			switch (currentState) {
			case CHARGESOC:
				if (essSoc > minSoc) {
					currentState = State.MINSOC;
				} else {
					this.logInfo(this.log, "TimelineChargeController CHARGESOC");
					int calculatedPower = ess.getPower().fitValueIntoMinMaxPower(config.id(), ess, Phase.ALL,
							Pwr.ACTIVE, -10000);
					ess.getSetActivePowerEquals().setNextWriteValue(calculatedPower);
				}
				break;
			case MINSOC:
				if (essSoc < chargeSoc) {
					currentState = State.CHARGESOC;
				} else if (essSoc >= minSoc + 5) {
					currentState = State.NORMAL;
				} else {
					this.logInfo(this.log, "TimelineChargeController MINSOC");
					int calculatedPower = ess.getPower().fitValueIntoMinMaxPower(config.id(), ess, Phase.ALL,
							Pwr.ACTIVE, 0);
					ess.getSetActivePowerEquals().setNextWriteValue(calculatedPower);
				}
				break;
			case NORMAL:
				if (essSoc <= minSoc) {
					currentState = State.MINSOC;
				}
				break;
			}
		}
	}

	private JsonArray getJsonOfDay(DayOfWeek day) {
		try {
			switch (day) {
			case FRIDAY:
				return JsonUtils.getAsJsonArray(JsonUtils.parse(this.config.friday()));
			case SATURDAY:
				return JsonUtils.getAsJsonArray(JsonUtils.parse(this.config.saturday()));
			case SUNDAY:
				return JsonUtils.getAsJsonArray(JsonUtils.parse(this.config.sunday()));
			case THURSDAY:
				return JsonUtils.getAsJsonArray(JsonUtils.parse(this.config.thursday()));
			case TUESDAY:
				return JsonUtils.getAsJsonArray(JsonUtils.parse(this.config.tuesday()));
			case WEDNESDAY:
				return JsonUtils.getAsJsonArray(JsonUtils.parse(this.config.wednesday()));
			default:
			case MONDAY:
				return JsonUtils.getAsJsonArray(JsonUtils.parse(this.config.monday()));
			}
		} catch (OpenemsNamedException e) {
			this.logError(this.log, e.getMessage());
			return new JsonArray();
		}
	}

	private SocPoint getCurrentSoc() {
		SocPoint soc = null;
		JsonArray jHours = this.getJsonOfDay(LocalDate.now().getDayOfWeek());
		LocalTime time = LocalTime.now();
		int count = 1;
		while (soc == null && count < 8) {
			try {
				Map.Entry<LocalTime, Integer> entry = this.floorSoc(jHours, time);
				soc = new SocPoint(LocalDateTime.of(LocalDate.now().minusDays(count), entry.getKey()),
						entry.getValue());
			} catch (IndexOutOfBoundsException | OpenemsNamedException e) {
				time = LocalTime.MIN;
				jHours = this.getJsonOfDay(LocalDate.now().getDayOfWeek().minus(count));
			}
			count++;
		}
		if (soc == null) {
			soc = new SocPoint(LocalDateTime.MIN, 10);
		}
		return soc;
	}

	private SocPoint getSoc() {
		SocPoint soc = null;
		JsonArray jHours;
		jHours = getJsonOfDay(LocalDate.now().getDayOfWeek());
		LocalTime time = LocalTime.now();
		int count = 1;
		while (soc == null && count < 8) {
			try {
				Map.Entry<LocalTime, Integer> entry = this.higherSoc(jHours, time);
				soc = new SocPoint(LocalDateTime.of(LocalDate.now().plusDays(count - 1), entry.getKey()),
						entry.getValue());
			} catch (IndexOutOfBoundsException | OpenemsNamedException e) {
				time = LocalTime.MIN;
				jHours = getJsonOfDay(LocalDate.now().getDayOfWeek().plus(count));
			}
			count++;
		}
		if (soc == null) {
			soc = new SocPoint(LocalDateTime.MIN, 10);
		}
		return soc;
	}

	private Map.Entry<LocalTime, Integer> floorSoc(JsonArray jHours, LocalTime time) throws OpenemsNamedException {
		// fill times map; sorted by hour
		TreeMap<LocalTime, Integer> times = new TreeMap<>();
		for (JsonElement jHourElement : jHours) {
			JsonObject jHour = JsonUtils.getAsJsonObject(jHourElement);
			String hourTime = JsonUtils.getAsString(jHour, "time");
			int jsoc = JsonUtils.getAsInt(jHourElement, "soc");
			times.put(LocalTime.parse(hourTime), jsoc);
		}
		// return matching controllers
		if (times.floorEntry(time) != null) {
			return times.floorEntry(time);
		} else {
			throw new IndexOutOfBoundsException("No smaller time found");
		}
	}

	private Map.Entry<LocalTime, Integer> higherSoc(JsonArray jHours, LocalTime time) throws OpenemsNamedException {
		// fill times map; sorted by hour
		TreeMap<LocalTime, Integer> times = new TreeMap<>();
		for (JsonElement jHourElement : jHours) {
			JsonObject jHour = JsonUtils.getAsJsonObject(jHourElement);
			String hourTime = JsonUtils.getAsString(jHour, "time");
			int jsoc = JsonUtils.getAsInt(jHourElement, "soc");
			times.put(LocalTime.parse(hourTime), jsoc);
		}
		// return matching controllers
		if (times.higherEntry(time) != null) {
			return times.higherEntry(time);
		} else {
			throw new IndexOutOfBoundsException("No smaller time found");
		}
	}

	private class SocPoint {
		private final LocalDateTime time;
		private final int soc;

		public SocPoint(LocalDateTime time, int soc) {
			super();
			this.time = time;
			this.soc = soc;
		}

		public LocalDateTime getTime() {
			return time;
		}

		public int getSoc() {
			return soc;
		}

	}

}
