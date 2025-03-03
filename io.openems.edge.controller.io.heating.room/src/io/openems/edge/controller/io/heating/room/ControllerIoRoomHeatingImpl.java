package io.openems.edge.controller.io.heating.room;

import static io.openems.edge.controller.io.heating.room.Utils.getNextHighPeriod;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jscalendar.JSCalendar.Task;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.MeterType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.io.heating.room.Utils.HighPeriod;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.IO.Heating.Room", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerIoRoomHeatingImpl extends AbstractOpenemsComponent
		implements ControllerIoRoomHeating, Controller, ElectricityMeter, OpenemsComponent, TimedataProvider {

	private static final int MINIMUM_SWITCHING_TIME = 180; // [s]

	private final Logger log = LoggerFactory.getLogger(ControllerIoRoomHeatingImpl.class);
	private final Clock clock;
	private final CalculateEnergyFromPower calculateEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private Timedata timedata;

	@Reference(policyOption = ReferencePolicyOption.GREEDY)
	private Thermometer floorThermometer;

	@Reference(policyOption = ReferencePolicyOption.GREEDY)
	private Thermometer ambientThermometer;

	@Reference(policyOption = ReferencePolicyOption.GREEDY)
	private List<DigitalOutput> floorRelayComponents;

	@Reference(policyOption = ReferencePolicyOption.GREEDY)
	private List<DigitalOutput> infraredRelayComponents;

	private Config config = null;
	private ImmutableList<Task<JsonObject>> schedule = ImmutableList.of();
	private final List<ChannelAddress> floorRelays = new ArrayList<>();
	private final List<ChannelAddress> infraredRelays = new ArrayList<>();

	private HighPeriod highPeriod;
	private RelayState lastFloorRelayState = null;
	private RelayState lastInfraredRelayState = null;

	public ControllerIoRoomHeatingImpl(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerIoRoomHeating.ChannelId.values() //
		);
		this.clock = clock;

		// Set static Meter channels
		this._setActiveConsumptionEnergy(0);
		this._setReactivePower(0);
	}

	public ControllerIoRoomHeatingImpl() {
		this(Clock.systemDefaultZone());
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		this.applyConfig(config);
		super.modified(context, config.id(), config.alias(), config.enabled());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public synchronized void run() throws OpenemsNamedException {
		this.applyHeatingLogic();
		this.updateMeterValues();
	}

	/**
	 * Applies the actual heating logic.
	 */
	private void applyHeatingLogic() {
		/*
		 * Evaluate actual temperatures
		 */
		var floorActual = this.floorThermometer.getTemperature();
		var ambientActual = this.ambientThermometer.getTemperature();
		this._setFloorActual(floorActual.get());
		this._setAmbientActual(ambientActual.get());

		/*
		 * Evaluate target temperatures
		 */
		final var actualMode = switch (this.config.mode()) {
		case AUTOMATIC -> this.getActualModeFromSchedule();
		case MANUAL_HIGH -> ActualMode.HIGH;
		case MANUAL_LOW -> ActualMode.LOW;
		case OFF -> ActualMode.OFF;
		};
		var floorTarget = switch (actualMode) {
		case HIGH -> this.config.highFloorTemperature();
		case LOW -> this.config.lowFloorTemperature();
		case OFF -> null;
		};
		var ambientTarget = switch (actualMode) {
		case HIGH -> this.config.highAmbientTemperature();
		case LOW -> this.config.lowAmbientTemperature();
		case OFF -> null;
		};
		this._setFloorTarget(floorTarget);
		this._setAmbientTarget(ambientTarget);

		/*
		 * Control floor heating
		 */
		if (floorActual.isDefined() && floorTarget != null && floorActual.get() < floorTarget) {
			if (this.config.hasExternalAmbientHeating() //
					&& ambientActual.isDefined() && ambientTarget != null && ambientActual.get() > ambientTarget) {
				// Switch Floor heating OFF if there is external heating in the room and ambient
				// target temperature is already met
				this.switchFloorRelays(Switch.OFF);
			} else {
				this.switchFloorRelays(Switch.ON);
			}

		} else {
			this.switchFloorRelays(Switch.OFF);
		}

		/*
		 * Control infrared heating
		 */
		if (ambientActual.isDefined() && ambientTarget != null && ambientActual.get() < ambientTarget) {
			this.switchInfraredRelays(Switch.ON);
		} else {
			this.switchInfraredRelays(Switch.OFF);
		}
	}

	public static enum ActualMode {
		OFF, LOW, HIGH;
	}

	/**
	 * Gets the {@link ActualMode} from the Schedule.
	 * 
	 * @return the {@link ActualMode}
	 */
	protected synchronized ActualMode getActualModeFromSchedule() {
		var hp = this.highPeriod;
		if (hp == null) {
			return ActualMode.LOW;
		}
		var now = Instant.now(this.clock);
		if (now.isBefore(hp.from())) {
			return ActualMode.LOW;
		}
		if (now.isAfter(hp.to())) {
			this.updateHighPeriod();
			return this.getActualModeFromSchedule();
		}
		return ActualMode.HIGH;
	}

	public static enum Switch {
		ON, OFF;
	}

	/**
	 * Switch the Floor Heating Relays.
	 * 
	 * @param target the {@link Switch} target
	 */
	private void switchFloorRelays(Switch target) {
		this.lastFloorRelayState = this.switchRelays(//
				this.getFloorRelayChannels(), //
				this.lastFloorRelayState, //
				target);
	}

	/**
	 * Switch the Infrared Heating Relays.
	 * 
	 * @param target the {@link Switch} target
	 */
	private void switchInfraredRelays(Switch target) {
		this.lastInfraredRelayState = this.switchRelays(//
				this.getInfraredRelayChannels(), //
				this.lastInfraredRelayState, //
				target);
	}

	private static record RelayState(Instant lastChange, Switch target) {
	}

	/**
	 * Switches Relays ON or OFF. Does not switch faster than MINIMUM_SWITCHING_TIME
	 * and does not set the command if the Relay is already in correct state.
	 * 
	 * @param channels       the Relay channels
	 * @param lastRelayState the matching {@link RelayState} information object
	 * @param target         the {@link Switch} target
	 * @return a new {@link RelayState} information object
	 */
	private RelayState switchRelays(List<WriteChannel<Boolean>> channels, RelayState lastRelayState, Switch target) {
		var now = Instant.now(this.clock);
		if (lastRelayState != null
				&& Duration.between(lastRelayState.lastChange, now).getSeconds() < MINIMUM_SWITCHING_TIME) {
			// Hysteresis is active
			return lastRelayState;
		}

		boolean value = target == Switch.ON;
		for (var channel : channels) {
			try {
				if (channel.value().asOptional().equals(Optional.of(value))) {
					// it is already in the desired state
				} else {
					channel.setNextWriteValue(value);
				}
			} catch (OpenemsNamedException e) {
				this.logError(this.log,
						"Unable to switch Relay [" + channel.address() + "] " + target.name() + ": " + e.getMessage());
			}
		}
		return new RelayState(now, target);
	}

	/**
	 * Gets the Floor Heating Relay Channels.
	 * 
	 * @return a list of {@link BooleanWriteChannel}s
	 */
	private List<WriteChannel<Boolean>> getFloorRelayChannels() {
		return getChannels(this.floorRelayComponents, this.floorRelays); //
	}

	/**
	 * Gets the Infrared Heating Relay Channels.
	 * 
	 * @return a list of {@link BooleanWriteChannel}s
	 */
	private List<WriteChannel<Boolean>> getInfraredRelayChannels() {
		return getChannels(this.infraredRelayComponents, this.infraredRelays); //
	}

	/**
	 * Gets the Relay Channels from addresses.
	 * 
	 * @param components the {@link DigitalOutput} components
	 * @param addresses  the {@link ChannelAddress}es of the Relays
	 * @return a list of relay channels
	 */
	private static List<WriteChannel<Boolean>> getChannels(List<DigitalOutput> components,
			List<ChannelAddress> addresses) {
		final List<WriteChannel<Boolean>> result = new ArrayList<>();
		for (var address : addresses) {
			for (var component : components) {
				if (address.getComponentId().equals(component.id())) {
					WriteChannel<Boolean> channel = component.channel(address.getChannelId());
					result.add(channel);
				}
			}
		}
		return result;
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.CONSUMPTION_METERED;
	}

	/**
	 * Update the Meter values.
	 */
	private void updateMeterValues() {
		/*
		 * Floor Power
		 */
		var floorIsOn = this.getFloorRelayChannels().stream() //
				.map(c -> c.value()) //
				.filter(v -> v.isDefined() && v.get()) // is any channel value true?
				.findAny() //
				.isPresent();
		final int floorPower;
		if (floorIsOn) {
			floorPower = this.config.floorPower();
		} else {
			floorPower = 0;
		}

		/*
		 * Infrared Power
		 */
		var infraredIsOn = this.getInfraredRelayChannels().stream() //
				.map(c -> c.value()) //
				.filter(v -> v.isDefined() && v.get()) // is any channel value true?
				.findAny() //
				.isPresent();
		final int infraredPower;
		if (infraredIsOn) {
			infraredPower = this.config.infraredPower();
		} else {
			infraredPower = 0;
		}

		/*
		 * Sum
		 */
		var power = floorPower + infraredPower;
		this._setActivePower(power);
		this.calculateEnergy.update(power);
	}

	/**
	 * Applies the Configuration and updates reference target filters.
	 * 
	 * @param config the {@link Config}
	 * @throws OpenemsNamedException on error
	 */
	private synchronized void applyConfig(Config config) throws OpenemsNamedException {
		this.config = config;

		// Reset RelayStates
		this.lastFloorRelayState = null;
		this.lastInfraredRelayState = null;

		OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "floorThermometer",
				this.config.floorThermometer_id());
		OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ambientThermometer",
				this.config.ambientThermometer_id());

		// Parse Channel-Addresses
		{
			this.floorRelays.clear();
			for (String channel : config.floorRelays()) {
				if (channel.isEmpty()) {
					continue;
				}
				var address = ChannelAddress.fromString(channel);
				this.floorRelays.add(address);
			}
			OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "floorRelayComponents",
					this.floorRelays.stream().map(c -> c.getComponentId()).distinct().toArray(String[]::new));
		}
		{
			this.infraredRelays.clear();
			for (String channel : config.infraredRelays()) {
				if (channel.isEmpty()) {
					continue;
				}
				var address = ChannelAddress.fromString(channel);
				this.infraredRelays.add(address);
			}
			OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "infraredRelayComponents",
					this.infraredRelays.stream().map(c -> c.getComponentId()).distinct().toArray(String[]::new));
		}

		this.schedule = JSCalendar.Task.fromStringOrEmpty(config.schedule(), j -> j);
		this.updateHighPeriod();
	}

	private synchronized void updateHighPeriod() {
		var now = ZonedDateTime.now(this.clock);
		this.highPeriod = getNextHighPeriod(now, this.schedule);
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public String debugLog() {
		final var mode = this.config.mode();
		final var b = new StringBuilder(); //
		switch (mode) {
		case OFF -> b.append("Off");
		case MANUAL_HIGH -> b.append("Manual HIGH");
		case MANUAL_LOW -> b.append("Manual LOW");
		case AUTOMATIC -> {
			var hp = this.highPeriod;
			b.append("Auto|");
			if (hp == null) {
				b.append("LOW|NoSchedule");
			} else {
				var now = Instant.now(this.clock);
				if (now.isBefore(hp.from())) {
					b //
							.append("LOW|NextHigh:") //
							.append(LocalDateTime.ofInstant(hp.from(), this.clock.getZone())
									.format(ISO_LOCAL_DATE_TIME));
				} else if (now.isAfter(hp.to())) {
					b.append("LOW|NoSchedule"); //
				} else {
					b //
							.append("HIGH|Till:") //
							.append(LocalDateTime.ofInstant(hp.to(), this.clock.getZone()).format(ISO_LOCAL_DATE_TIME));
				}
			}
		}
		}
		if (mode != Mode.OFF) {
			b //
					.append("|Floor:") //
					.append(this.getFloorActual().asStringWithoutUnit()) //
					.append("->") //
					.append(this.getFloorTarget().asString()) //
					.append("|Ambient:") //
					.append(this.getAmbientActual().asStringWithoutUnit()) //
					.append("->") //
					.append(this.getAmbientTarget().asString()) //
					.append("|") //
					.append(this.getActivePower().asString());
		}
		return b.toString();
	}
}
