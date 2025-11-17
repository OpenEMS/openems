package io.openems.edge.evse.chargepoint.abl;

import static io.openems.common.channel.ChannelUtils.setValue;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;

import java.time.Duration;
import java.time.Instant;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.Tuple;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.chargepoint.abl.enums.ChargingState;
import io.openems.edge.evse.chargepoint.abl.enums.Status;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.ABL", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class EvseChargePointAblImpl extends AbstractOpenemsModbusComponent
		implements EvseChargePointAbl, ModbusComponent, OpenemsComponent, TimedataProvider, EvseChargePoint,
		EventHandler, ElectricityMeter {

	private final Logger log = LoggerFactory.getLogger(EvseChargePointAblImpl.class);

	private final CalculateEnergyFromPower calculateEnergyL1 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1);
	private final CalculateEnergyFromPower calculateEnergyL2 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2);
	private final CalculateEnergyFromPower calculateEnergyL3 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3);

	private Config config;
	private Tuple<Instant, Integer> previousCurrent = null;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	protected ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public EvseChargePointAblImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				EvseChargePointAbl.ChannelId.values() //
		);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
		ElectricityMeter.calculatePhasesFromActivePower(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id());
		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.applyConfig(config);
	}

	private void applyConfig(Config config) {
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		/*
		 * ABL EVCC2/3 Modbus Protocol Definition based on specification document
		 * "Schnittstellenbeschreibung_Modbus-ASCII.pdf"
		 *
		 * Note: The specification uses Modbus ASCII over RS485, but for Modbus TCP we
		 * use the same register addresses.
		 */

		var modbusProtocol = new ModbusProtocol(this, //

				// Register 0x0001-0x0002: Read device-ID and firmware-revision
				new FC3ReadRegistersTask(0x0001, Priority.LOW, //
						m(EvseChargePointAbl.ChannelId.DEVICE_ID, new UnsignedWordElement(0x0001),
								new ElementToChannelConverter(value -> {
									// Bits 21..16 contain the device-ID (0x01...0x10)
									Integer val = TypeUtils.getAsType(INTEGER, value);
									if (val != null) {
										return (val >> 16) & 0x1F; // Extract device ID from bits 21..16
									}
									return null;
								})),
						m(EvseChargePointAbl.ChannelId.FIRMWARE_VERSION, new UnsignedWordElement(0x0002),
								new ElementToChannelConverter(value -> {
									// Bits 15..12: Major revision, Bits 11..8: Minor revision
									Integer val = TypeUtils.getAsType(INTEGER, value);
									if (val != null) {
										int major = (val >> 12) & 0x0F;
										int minor = (val >> 8) & 0x0F;
										return major + "." + minor;
									}
									return "Unknown";
								}))),

				// Register 0x0033-0x0035: Read current state and phase currents (short format)
				// This is the primary register for reading state and currents (1A resolution)
				new FC3ReadRegistersTask(0x0033, Priority.HIGH, //
						m(EvseChargePointAbl.ChannelId.EV_CONNECTED, new UnsignedWordElement(0x0033),
								new ElementToChannelConverter(value -> {
									// Bit 39 (MSB of first register): 0 = UCP > 10V (no EV), 1 = UCP <= 10V (EV
									// connected)
									Integer val = TypeUtils.getAsType(INTEGER, value);
									if (val != null) {
										return ((val >> 8) & 0x01) == 1; // Bit 39 is bit 8 of high byte
									}
									return false;
								})),
						m(EvseChargePointAbl.ChannelId.CHARGING_STATE, new UnsignedWordElement(0x0034),
								new ElementToChannelConverter(value -> {
									// Bits 31..24 contain the state value (0xA1, 0xB1, 0xC2, etc.)
									Integer val = TypeUtils.getAsType(INTEGER, value);
									if (val != null) {
										int stateValue = (val >> 8) & 0xFF; // High byte contains state
										return ChargingState.fromValue(stateValue);
									}
									return ChargingState.UNDEFINED;
								})),
						m(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L1, new UnsignedWordElement(0x0034),
								new ElementToChannelConverter(value -> {
									// Bits 23..16: ICT1 in Ampere (0x64 = not available)
									Integer val = TypeUtils.getAsType(INTEGER, value);
									if (val != null) {
										int current = val & 0xFF; // Low byte contains L1 current
										return current == 0x64 ? null : current;
									}
									return null;
								})),
						m(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L2, new UnsignedWordElement(0x0035),
								new ElementToChannelConverter(value -> {
									// Bits 15..8: ICT2 in Ampere (0x64 = not available)
									Integer val = TypeUtils.getAsType(INTEGER, value);
									if (val != null) {
										int current = (val >> 8) & 0xFF; // High byte contains L2 current
										return current == 0x64 ? null : current;
									}
									return null;
								})),
						m(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L3, new UnsignedWordElement(0x0035),
								new ElementToChannelConverter(value -> {
									// Bits 7..0: ICT3 in Ampere (0x64 = not available)
									Integer val = TypeUtils.getAsType(INTEGER, value);
									if (val != null) {
										int current = val & 0xFF; // Low byte contains L3 current
										return current == 0x64 ? null : current;
									}
									return null;
								}))),

				// Register 0x0014: Set Icmax (write only)
				// Value: Duty cycle Icmax [%]*10 (0x0050...0x03E8 = 8%...100%)
				// According to spec: 6A corresponds to ~10% duty cycle (0x0064)
				// 32A corresponds to ~53% duty cycle (0x0214)
				new FC16WriteRegistersTask(0x0014, //
						m(EvseChargePointAbl.ChannelId.SET_CHARGING_CURRENT, new UnsignedWordElement(0x0014),
								new ElementToChannelConverter(value -> {
									// Convert milliampere to duty cycle percentage * 10
									// Formula: duty_cycle = (current_mA / 600) [for 6A = 10%, 60A = 100%]
									// Simplified: duty_cycle_x10 = (current_mA * 10) / 600 = current_mA / 60
									Integer currentMa = TypeUtils.getAsType(INTEGER, value);
									if (currentMa == null || currentMa == 0) {
										return 0; // 0 = stop charging
									}

									// Convert mA to duty cycle % * 10
									// 6000 mA = 6A should map to ~10% duty cycle (100 in register, since
									// %*10)
									// 32000 mA = 32A should map to ~53% duty cycle (530 in register)
									// Linear mapping: duty_cycle_pct_x10 = (currentMa - 6000) * 900 / 54000 +
									// 100
									// Simplified approximation for 6-32A range
									int dutyCycleX10 = Math.min(1000, Math.max(80, (currentMa * 167) / 10000));

									return dutyCycleX10;
								})))

		);

		return modbusProtocol;
	}

	@Override
	public String debugLog() {
		var b = new StringBuilder() //
				.append("State:").append(this.getChargingState()).append("|") //
				.append("L:").append(this.getActivePower().asString());

		if (!this.config.readOnly()) {
			b //
					.append("|SetCurrent:") //
					.append(this.channel(EvseChargePointAbl.ChannelId.DEBUG_SET_CHARGING_CURRENT).value().asString());
		}
		return b.toString();
	}

	private void logIfDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		var config = this.config;
		if (config == null || config.readOnly()) {
			return null;
		}

		final var phases = this.getWiring();
		final var maxCurrentMa = config.maxCurrent() * 1000; // Convert A to mA

		return ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(phases, 6000, maxCurrentMa)) //
				.setIsEvConnected(this.getEvConnectedChannel().value().orElse(false)) //
				.setIsReadyForCharging(this.getIsReadyForCharging()) //
				.build();
	}

	private SingleOrThreePhase getWiring() {
		return this.config.wiring();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			// Calculate energy from power
			this.calculateEnergyL1.update(this.getActivePowerL1Channel().getNextValue().get());
			this.calculateEnergyL2.update(this.getActivePowerL2Channel().getNextValue().get());
			this.calculateEnergyL3.update(this.getActivePowerL3Channel().getNextValue().get());

			// Update IS_READY_FOR_CHARGING based on charging state
			var chargingState = this.getChargingState();
			var isReady = evaluateIsReadyForCharging(chargingState);
			setValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, isReady);

			// Map phase currents from ABL channels to ElectricityMeter channels
			// Convert A to mA (*1000) for meter channels
			var currentL1 = this.channel(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L1).value().asOptional();
			var currentL2 = this.channel(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L2).value().asOptional();
			var currentL3 = this.channel(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L3).value().asOptional();

			setValue(this, ElectricityMeter.ChannelId.CURRENT_L1, currentL1.map(i -> i * 1000).orElse(null));
			setValue(this, ElectricityMeter.ChannelId.CURRENT_L2, currentL2.map(i -> i * 1000).orElse(null));
			setValue(this, ElectricityMeter.ChannelId.CURRENT_L3, currentL3.map(i -> i * 1000).orElse(null));

			// Set voltage to nominal 230V per phase (ABL doesn't provide voltage
			// measurement)
			setValue(this, ElectricityMeter.ChannelId.VOLTAGE_L1, 230000); // 230V in mV
			setValue(this, ElectricityMeter.ChannelId.VOLTAGE_L2, 230000);
			setValue(this, ElectricityMeter.ChannelId.VOLTAGE_L3, 230000);
		}
		}
	}

	/**
	 * Evaluates if the charging station is ready for charging based on state.
	 *
	 * @param chargingState the current charging state
	 * @return true if ready for charging
	 */
	protected static boolean evaluateIsReadyForCharging(ChargingState chargingState) {
		if (chargingState == null) {
			return false;
		}

		return switch (chargingState.status) {
		case READY_FOR_CHARGING, CHARGING -> true;
		default -> false;
		};
	}

	@Override
	public void apply(ChargePointActions actions) {
		if (this.config.readOnly()) {
			return;
		}

		final var now = Instant.now();
		final var current = actions.getApplySetPointInMilliAmpere().value();

		this.handleApplyCharge(now, current);
	}

	/**
	 * Applies the charging current setpoint with rate limiting.
	 *
	 * @param now     current timestamp
	 * @param current desired current in milliampere
	 */
	private void handleApplyCharge(Instant now, int current) {
		// Rate limit: minimum 5 seconds between changes (as per best practice from
		// other implementations)
		if (this.previousCurrent != null && Duration.between(this.previousCurrent.a(), now).getSeconds() < 5) {
			this.logIfDebug("Rate limit active, skipping current update");
			return;
		}

		this.previousCurrent = Tuple.of(now, current);

		try {
			this.logIfDebug("Setting charging current to " + current + " mA");
			this.setChargingCurrent(current);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Failed to set charging current: " + e.getMessage());
		}
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public boolean isReadOnly() {
		return this.config.readOnly();
	}

	@Override
	public MeterType getMeterType() {
		if (this.config.readOnly()) {
			return MeterType.CONSUMPTION_METERED;
		}
		return MeterType.MANAGED_CONSUMPTION_METERED;
	}
}
