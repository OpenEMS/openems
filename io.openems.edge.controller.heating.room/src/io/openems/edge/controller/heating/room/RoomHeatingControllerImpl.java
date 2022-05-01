package io.openems.edge.controller.heating.room;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Dictionary;
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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Heating.Room", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class RoomHeatingControllerImpl extends AbstractOpenemsComponent
		implements RoomHeatingController, Controller, SymmetricMeter, OpenemsComponent, TimedataProvider {

	private static final int MINIMUM_SWITCHING_TIME = 180; // [s]

	private final Logger log = LoggerFactory.getLogger(RoomHeatingControllerImpl.class);
	private final Clock clock;
	private final CalculateEnergyFromPower calculateEnergy = new CalculateEnergyFromPower(this,
			SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

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
	private final List<ChannelAddress> floorRelays = new ArrayList<>();
	private final List<ChannelAddress> infraredRelays = new ArrayList<>();

	private RelayState lastFloorRelayState = null;
	private RelayState lastInfraredRelayState = null;

	public RoomHeatingControllerImpl(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				Controller.ChannelId.values(), //
				RoomHeatingController.ChannelId.values() //
		);
		this.clock = clock;

		// Set static Meter channels
		this._setActiveConsumptionEnergy(0);
		this._setReactivePower(0);
	}

	public RoomHeatingControllerImpl() {
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
		var floorActualOpt = this.floorThermometer.getTemperature();
		var ambientActualOpt = this.ambientThermometer.getTemperature();
		this._setFloorActual(floorActualOpt.get());
		this._setAmbientActual(ambientActualOpt.get());

		/*
		 * Evaluate target temperatures
		 */
		final Integer floorTarget;
		final Integer ambientTarget;
		switch (this.getActualMode()) {
		case HIGH:
			// HIGH mode
			floorTarget = this.config.highFloorTemperature();
			ambientTarget = this.config.highAmbientTemperature();
			break;

		case LOW:
			// LOW mode
			floorTarget = this.config.lowFloorTemperature();
			ambientTarget = this.config.lowAmbientTemperature();

			break;

		case OFF:
		default:
			// OFF; stop early
			floorTarget = null;
			ambientTarget = null;
		}
		this._setFloorTarget(floorTarget);
		this._setAmbientTarget(ambientTarget);

		/*
		 * Switch off if state is unknown
		 */
		if (!floorActualOpt.isDefined() || floorTarget == null) {
			this.switchFloorRelays(Switch.OFF);
		}
		if (!ambientActualOpt.isDefined() || ambientTarget == null) {
			this.switchInfraredRelays(Switch.OFF);
		}
		var ambientActual = ambientActualOpt.get();
		var floorActual = floorActualOpt.get();

		/*
		 * Control floor heating
		 */
		if (floorActual < floorTarget) {
			if (this.config.hasExternalAmbientHeating() && ambientActual > ambientTarget) {
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
		if (ambientActual < ambientTarget) {
			this.switchInfraredRelays(Switch.ON);
		} else {
			this.switchInfraredRelays(Switch.OFF);
		}
	}

	public static enum ActualMode {
		OFF, LOW, HIGH;
	}

	/**
	 * Gets the actual mode: HIGH, LOW or OFF.
	 * 
	 * @return the {@link ActualMode}
	 */
	private ActualMode getActualMode() {
		switch (this.config.mode()) {
		case AUTOMATIC:
			return this.getActualModeFromSchedule(this.config.schedule());
		case MANUAL_HIGH:
			return ActualMode.HIGH;
		case MANUAL_LOW:
			return ActualMode.LOW;
		case OFF:
			return ActualMode.OFF;
		}
		// should never happen
		return ActualMode.OFF;
	}

	/**
	 * Gets the {@link ActualMode} from a 96 character Schedule string. This method
	 * takes the character at the position of the current quarter of the day.
	 * 
	 * <ul>
	 * <li># is interpreted as HIGH
	 * <li>_ is interpreted as LOW
	 * </ul>
	 * 
	 * @param schedule the Schedule string
	 * @return the {@link ActualMode}
	 */
	protected ActualMode getActualModeFromSchedule(String schedule) {
		var now = LocalTime.now(this.clock);
		var quarter = now.toSecondOfDay() / (60 * 15);
		String mode;
		try {
			mode = schedule.substring(quarter, quarter + 1);
		} catch (IndexOutOfBoundsException e) {
			this.log.warn("Schedule is wrong. IndexOutOfBoundsException for index [" + quarter + " at " + now + "]: "
					+ e.getMessage());
			return ActualMode.LOW;
		}
		if (mode.equals("#")) {
			return ActualMode.HIGH;
		} else {
			return ActualMode.LOW;
		}
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
		return this.getChannels(this.floorRelayComponents, this.floorRelays); //
	}

	/**
	 * Gets the Infrared Heating Relay Channels.
	 * 
	 * @return a list of {@link BooleanWriteChannel}s
	 */
	private List<WriteChannel<Boolean>> getInfraredRelayChannels() {
		return this.getChannels(this.infraredRelayComponents, this.infraredRelays); //
	}

	/**
	 * Gets the Relay Channels from addresses.
	 * 
	 * @param components the {@link DigitalOutput} components
	 * @param addresses  the {@link ChannelAddress}es of the Relays
	 * @return
	 */
	private List<WriteChannel<Boolean>> getChannels(List<DigitalOutput> components, List<ChannelAddress> addresses) {
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

		// Set MeterType as property (TODO)
		{
			try {
				var c = this.cm.getConfiguration(this.servicePid(), "?");
				Dictionary<String, Object> properties = c.getProperties();
				if (!"CONSUMPTION_METERED".equals(properties.get("type"))) {
					properties.put("type", "CONSUMPTION_METERED");
					c.update(properties);
				}
			} catch (IOException | SecurityException e) {
				System.err
						.println("updateReferenceFilter ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage());
				e.printStackTrace();
			}
		}

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
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public String debugLog() {
		return this.getActualMode() //
				+ "|Floor " + this.getFloorActual().asStringWithoutUnit() + "->" + this.getFloorTarget().asString() //
				+ "|Ambient " + this.getAmbientActual().asStringWithoutUnit() + "->"
				+ this.getAmbientTarget().asString() //
				+ "|" + this.getActivePower().asString();
	}
}
