package io.openems.edge.evcs.alpitronic;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;
import static io.openems.edge.common.channel.ChannelUtils.setValue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntFunction;

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
import io.openems.common.types.OpenemsType;
import io.openems.common.types.SemanticVersion;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.ModbusUtils.FunctionCode;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcs.api.ChargeStateHandler;
import io.openems.edge.evcs.api.DeprecatedEvcs;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.api.WriteHandler;
import io.openems.edge.evse.chargepoint.alpitronic.common.Alpitronic;
import io.openems.edge.evse.chargepoint.alpitronic.enums.AvailableState;
import io.openems.edge.evse.chargepoint.alpitronic.enums.SelectedConnector;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.AlpitronicHypercharger", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class EvcsAlpitronicImpl extends AbstractOpenemsModbusComponent
		implements Evcs, ManagedEvcs, DeprecatedEvcs, ElectricityMeter, OpenemsComponent, ModbusComponent, EventHandler,
		Alpitronic, EvcsAlpitronic, TimedataProvider {

	private static final int MAX_FIRMWARE_VERSION_READ_ERRORS = 5;
	private static final SemanticVersion FALLBACK_FIRMWARE_VERSION = new SemanticVersion(2, 5, 0);

	private final Logger log = LoggerFactory.getLogger(EvcsAlpitronicImpl.class);
	/** Modbus offset for multiple connectors. */
	private final IntFunction<Integer> offset = addr -> addr + this.config.connector().modbusOffset;

	@Reference
	private EvcsPower evcsPower;

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	protected Config config;

	final ModbusProtocol modbusProtocol;

	private SemanticVersion detectedFirmwareVersion = null;
	private boolean protocolConfigured = false;

	/**
	 * Calculates the value for total energy in [Wh_Σ].
	 * 
	 * <p>
	 * Accumulates the energy by calling this.calculateTotalEnergy.update(power);
	 */
	private final CalculateEnergyFromPower calculateTotalEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

	/** Handles charge states. */
	private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);

	/** Processes the controller's writes to this evcs component. */
	private final WriteHandler writeHandler = new WriteHandler(this);

	public EvcsAlpitronicImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				DeprecatedEvcs.ChannelId.values(), //
				EvcsAlpitronic.ChannelId.values(), //
				Alpitronic.ChannelId.values());

		DeprecatedEvcs.copyToDeprecatedEvcsChannels(this);

		// Automatically calculate L1/l2/L3 values from sum
		ElectricityMeter.calculatePhasesFromActivePower(this);
		this.addCalculateVoltageAndCurrentListeners();

		// Initialize empty ModbusProtocol; tasks added dynamically
		// post-firmware-detection
		this.modbusProtocol = new ModbusProtocol(this);
	}

	private void addCalculateVoltageAndCurrentListeners() {
		this.getChargingVoltageChannel().onSetNextValue(value -> {
			var milliVoltage = toMilliValue(value.get());
			setValue(this, ElectricityMeter.ChannelId.VOLTAGE, milliVoltage);
			setValue(this, ElectricityMeter.ChannelId.VOLTAGE_L1, milliVoltage);
			setValue(this, ElectricityMeter.ChannelId.VOLTAGE_L2, milliVoltage);
			setValue(this, ElectricityMeter.ChannelId.VOLTAGE_L3, milliVoltage);
		});

		this.getChargingCurrentChannel().onSetNextValue(value -> {
			var milliCurrent = toMilliValue(value.get());
			var phaseCurrent = TypeUtils.divide(milliCurrent, 3);
			setValue(this, ElectricityMeter.ChannelId.CURRENT, milliCurrent);
			setValue(this, ElectricityMeter.ChannelId.CURRENT_L1, phaseCurrent);
			setValue(this, ElectricityMeter.ChannelId.CURRENT_L2, phaseCurrent);
			setValue(this, ElectricityMeter.ChannelId.CURRENT_L3, phaseCurrent);
		});
	}

	private static Integer toMilliValue(Double value) {
		return value == null ? null : (int) Math.round(value * 1_000D);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		/*
		 * Calculates the maximum and minimum hardware power dynamically by listening on
		 * the fixed hardware limit and the phases used for charging
		 */
		Evcs.addCalculatePowerLimitListeners(this);

		this.applyConfig(context, config);

		// Detect firmware version asynchronously
		this.detectFirmwareVersion();
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.applyConfig(context, config);
	}

	private void applyConfig(ComponentContext context, Config config) {
		this.config = config;
		this._setFixedMinimumHardwarePower(config.minHwPower());
		this._setFixedMaximumHardwarePower(config.maxHwPower());
		this._setPowerPrecision(1);
		this._setPhases(3);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		// TODO implement handling for rotated Phases
		return PhaseRotation.L1_L2_L3;
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
			-> this.calculateTotalEnergy.update(this.getActivePower().get());
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
			-> this.writeHandler.run();
		}
	}

	private void detectFirmwareVersion() {
		final var consecutiveReadErrors = new AtomicInteger(0);
		final var lastReadErrorState = new AtomicReference<Task.ExecuteState>(null);

		ModbusUtils.readElementsOnce(FunctionCode.FC4, this.modbusProtocol, (executeState, value) -> {
			if (value != null) {
				consecutiveReadErrors.set(0);
				lastReadErrorState.set(null);
				return false;
			}

			if (executeState instanceof Task.ExecuteState.Error) {
				if (lastReadErrorState.getAndSet(executeState) != executeState) {
					consecutiveReadErrors.incrementAndGet();
				}
				return consecutiveReadErrors.get() < MAX_FIRMWARE_VERSION_READ_ERRORS;
			}

			consecutiveReadErrors.set(0);
			lastReadErrorState.set(null);
			return true;
		}, new UnsignedWordElement(46), new UnsignedWordElement(47), new UnsignedWordElement(48)).thenAccept(result -> {
			var values = result.values();
			var major = values.get(0);
			var minor = values.get(1);
			var patch = values.get(2);

			this.channel(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_MAJOR).setNextValue(major);
			this.channel(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_MINOR).setNextValue(minor);
			this.channel(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_PATCH).setNextValue(patch);

			SemanticVersion.fromNullable(major, minor, patch).ifPresentOrElse(this::configureProtocolForVersion,
					() -> this.configureFallbackFirmwareVersion("Firmware version detection failed after "
							+ MAX_FIRMWARE_VERSION_READ_ERRORS + " attempts; fallback to " + FALLBACK_FIRMWARE_VERSION
							+ ". Last state: " + result.executeState()));
		}).exceptionally(e -> {
			this.configureFallbackFirmwareVersion("Firmware version detection failed; fallback to "
					+ FALLBACK_FIRMWARE_VERSION + ". Reason: " + e.getMessage());
			return null;
		});
	}

	private void configureFallbackFirmwareVersion(String logMessage) {
		this.logInfo(this.log, logMessage);
		this.configureProtocolForVersion(FALLBACK_FIRMWARE_VERSION);
	}

	private void configureProtocolForVersion(SemanticVersion version) {
		if (this.protocolConfigured) {
			this.detectedFirmwareVersion = version;
			return;
		}
		this.detectedFirmwareVersion = version;

		this.addCommonStationTasks();

		if (AlpitronicVersionUtils.isVersion18(version)) {
			this.addProtocolTasksV18();
		} else if (AlpitronicVersionUtils.isVersion23(version)) {
			this.addProtocolTasksV23();
		} else if (AlpitronicVersionUtils.isVersion24(version)) {
			this.addProtocolTasksV24();
		} else {
			// Default to v2.5+
			this.addProtocolTasksV25();
		}

		// Calculates charge power by existing Channels
		this.addCalculatePowerListeners();

		// Map raw status to evcs status
		this.addStatusListener();

		// Log detected version
		this.logInfo(this.log, "Detected Alpitronic firmware version: " + version);
		this.protocolConfigured = true;
	}

	/**
	 * Adds common station-level Modbus read tasks.
	 */
	private void addCommonStationTasks() {
		// Common tasks for station-level registers
		this.modbusProtocol.addTask(new FC3ReadRegistersTask(this.offset.apply(0), Priority.LOW,
				m(Alpitronic.ChannelId.RAW_CHARGE_POWER_SET, new UnsignedDoublewordElement(this.offset.apply(0)))));

		this.modbusProtocol.addTask(new FC16WriteRegistersTask(this.offset.apply(0),
				m(Alpitronic.ChannelId.APPLY_CHARGE_POWER_LIMIT, new UnsignedDoublewordElement(this.offset.apply(0)))));
		final var reactivePowerWriteAddress = this.getSetpointReactivePowerWriteAddress();
		this.modbusProtocol.addTask(
				new FC16WriteRegistersTask(reactivePowerWriteAddress, m(Alpitronic.ChannelId.SETPOINT_REACTIVE_POWER,
						new UnsignedDoublewordElement(reactivePowerWriteAddress))));
	}

	private int getSetpointReactivePowerWriteAddress() {
		if (this.detectedFirmwareVersion != null && AlpitronicVersionUtils.isVersion18(this.detectedFirmwareVersion)) {
			return this.offset.apply(2);
		}
		return 2;
	}

	private ElementToChannelConverter connectorTypeConverter(boolean isVersion25OrLater) {
		return new ElementToChannelConverter(value -> SelectedConnector
				.fromRawValue(TypeUtils.getAsType(OpenemsType.INTEGER, value), isVersion25OrLater));
	}

	private void addProtocolTasksV23OrLater(boolean isVersion25OrLater) {
		this.modbusProtocol.addTask(new FC4ReadInputRegistersTask(this.offset.apply(0), Priority.LOW,
				m(EvcsAlpitronic.ChannelId.RAW_STATUS, new UnsignedWordElement(this.offset.apply(0))),
				m(EvcsAlpitronic.ChannelId.CHARGING_VOLTAGE, new UnsignedDoublewordElement(this.offset.apply(1)),
						SCALE_FACTOR_MINUS_2),
				m(EvcsAlpitronic.ChannelId.CHARGING_CURRENT, new UnsignedWordElement(this.offset.apply(3)),
						SCALE_FACTOR_MINUS_2),
				m(Alpitronic.ChannelId.RAW_CHARGE_POWER, new UnsignedDoublewordElement(this.offset.apply(4))),
				m(Alpitronic.ChannelId.CHARGED_TIME, new UnsignedWordElement(this.offset.apply(6))),
				m(Alpitronic.ChannelId.CHARGED_ENERGY, new UnsignedWordElement(this.offset.apply(7)),
						SCALE_FACTOR_MINUS_2).onUpdateCallback(this.createChargedEnergyCallback()),
				m(Alpitronic.ChannelId.EV_SOC, new UnsignedWordElement(this.offset.apply(8)), SCALE_FACTOR_MINUS_2),
				m(Alpitronic.ChannelId.CONNECTOR_TYPE, new UnsignedWordElement(this.offset.apply(9)),
						this.connectorTypeConverter(isVersion25OrLater)),
				m(Alpitronic.ChannelId.EV_MAX_CHARGING_POWER, new UnsignedDoublewordElement(this.offset.apply(10))),
				m(Alpitronic.ChannelId.EV_MIN_CHARGING_POWER, new UnsignedDoublewordElement(this.offset.apply(12)))));

		this.modbusProtocol.addTask(new FC4ReadInputRegistersTask(this.offset.apply(32), Priority.LOW,
				m(EvcsAlpitronic.ChannelId.TOTAL_CHARGED_ENERGY,
						new UnsignedQuadruplewordElement(this.offset.apply(32)))));

		// Station-level VAR registers for v2.3+
		this.modbusProtocol.addTask(new FC4ReadInputRegistersTask(49, Priority.LOW,
				m(Alpitronic.ChannelId.VAR_REACTIVE_MAX, new UnsignedDoublewordElement(49)),
				m(Alpitronic.ChannelId.VAR_REACTIVE_MIN, new UnsignedDoublewordElement(51), INVERT)));
	}

	/**
	 * Adds version-specific Modbus tasks for firmware v1.8.x.
	 */
	private void addProtocolTasksV18() {
		// v1.8: connector-level VAR registers, reactive power at offset+2
		this.modbusProtocol.addTask(new FC4ReadInputRegistersTask(this.offset.apply(0), Priority.LOW,
				m(EvcsAlpitronic.ChannelId.RAW_STATUS, new UnsignedWordElement(this.offset.apply(0))),
				m(EvcsAlpitronic.ChannelId.CHARGING_VOLTAGE, new UnsignedDoublewordElement(this.offset.apply(1)),
						SCALE_FACTOR_MINUS_2),
				m(EvcsAlpitronic.ChannelId.CHARGING_CURRENT, new UnsignedWordElement(this.offset.apply(3)),
						SCALE_FACTOR_MINUS_2),
				m(Alpitronic.ChannelId.RAW_CHARGE_POWER, new UnsignedDoublewordElement(this.offset.apply(4))),
				m(Alpitronic.ChannelId.CHARGED_TIME, new UnsignedWordElement(this.offset.apply(6))),
				m(Alpitronic.ChannelId.CHARGED_ENERGY, new UnsignedWordElement(this.offset.apply(7)),
						SCALE_FACTOR_MINUS_2).onUpdateCallback(this.createChargedEnergyCallback()),
				m(Alpitronic.ChannelId.EV_SOC, new UnsignedWordElement(this.offset.apply(8)), SCALE_FACTOR_MINUS_2),
				m(Alpitronic.ChannelId.CONNECTOR_TYPE, new UnsignedWordElement(this.offset.apply(9)),
						this.connectorTypeConverter(false)),
				m(Alpitronic.ChannelId.EV_MAX_CHARGING_POWER, new UnsignedDoublewordElement(this.offset.apply(10))),
				m(Alpitronic.ChannelId.EV_MIN_CHARGING_POWER, new UnsignedDoublewordElement(this.offset.apply(12))),
				m(Alpitronic.ChannelId.VAR_REACTIVE_MAX, new UnsignedDoublewordElement(this.offset.apply(14))),
				m(Alpitronic.ChannelId.VAR_REACTIVE_MIN, new UnsignedDoublewordElement(this.offset.apply(16)),
						INVERT)));
	}

	/**
	 * Adds version-specific Modbus tasks for firmware v2.3.x.
	 */
	private void addProtocolTasksV23() {
		// v2.3: station-level VAR registers, reactive power at station-level (register
		// 2),
		// added TOTAL_CHARGED_ENERGY
		this.addProtocolTasksV23OrLater(false);
	}

	/**
	 * Adds version-specific Modbus tasks for firmware v2.4.x.
	 */
	private void addProtocolTasksV24() {
		// v2.4: all v2.3 features + MAX_CHARGING_POWER_AC
		this.addProtocolTasksV23();

		this.modbusProtocol.addTask(new FC4ReadInputRegistersTask(this.offset.apply(36), Priority.LOW, m(
				EvcsAlpitronic.ChannelId.MAX_CHARGING_POWER_AC, new UnsignedDoublewordElement(this.offset.apply(36)))));
	}

	/**
	 * Adds version-specific Modbus tasks for firmware v2.5.x and later.
	 */
	private void addProtocolTasksV25() {
		this.addProtocolTasksV23OrLater(true);

		// v2.5+: all v2.4 features + MAX_CHARGING_POWER_AC
		this.modbusProtocol.addTask(new FC4ReadInputRegistersTask(this.offset.apply(36), Priority.LOW, m(
				EvcsAlpitronic.ChannelId.MAX_CHARGING_POWER_AC, new UnsignedDoublewordElement(this.offset.apply(36)))));

		// v2.5+ may have additional features like apparent power handling
		// (currently implemented via existing RAW_CHARGE_POWER)
	}

	/**
	 * Creates a callback for handling charged energy register updates.
	 *
	 * <p>
	 * The hardware resets CHARGED_ENERGY to 0 when a charging session ends. This
	 * callback preserves the channel value unless the status indicates not-ready.
	 *
	 * @return a Consumer for handling energy updates
	 */
	private Consumer<Integer> createChargedEnergyCallback() {
		return e -> {
			if (e == null) {
				return;
			}

			/**
			 * The internal session energy is set to 0 when the charging process has
			 * finished. The SessionEnergy Channel should still contain the current value
			 * for visualization.
			 */
			if (e == 0) {
				switch (this.getStatus()) {
				case UNDEFINED:
				case NOT_READY_FOR_CHARGING:
				case STARTING:
					this._setEnergySession(0);
					return;
				case CHARGING:
				case CHARGING_REJECTED:
				case ENERGY_LIMIT_REACHED:
				case ERROR:
				case READY_FOR_CHARGING:
					// Ignore 0 value
					return;
				}
			}
			this._setEnergySession(e * 10);
		};
	}

	/*
	 * TODO: Remove if the charge power register returns valid values with newer
	 * firmware versions.
	 */
	private void addCalculatePowerListeners() {
		// Only calculate if version < 2.5 (older versions return 0 on power register)
		if (this.detectedFirmwareVersion != null
				&& !AlpitronicVersionUtils.isVersion25OrLater(this.detectedFirmwareVersion)) {
			// Calculate power from voltage and current
			final Consumer<Value<Double>> calculatePower = ignore -> {
				var voltage = this.getChargingVoltageChannel().getNextValue().get();
				var current = this.getChargingCurrentChannel().getNextValue().get();
				this._setActivePower(voltage != null && current != null ? TypeUtils.getAsType(OpenemsType.INTEGER,
						TypeUtils.multiply(//
								voltage, //
								current //
				)) : null);
			};
			this.getChargingVoltageChannel().onSetNextValue(calculatePower);
			this.getChargingCurrentChannel().onSetNextValue(calculatePower);
		} else {
			// v2.5+: Use RAW_CHARGE_POWER directly
			this.channel(Alpitronic.ChannelId.RAW_CHARGE_POWER).onSetNextValue(power -> {
				var value = power.get();
				this._setActivePower(value != null ? (Integer) value : null);
			});
		}
	}

	private void addStatusListener() {
		this.channel(EvcsAlpitronic.ChannelId.RAW_STATUS).onSetNextValue(s -> {
			AvailableState rawState = s.asEnum();
			/**
			 * Maps the raw state into a {@link Status}.
			 */
			this._setStatus(switch (rawState) {
			case AVAILABLE //
				-> Status.NOT_READY_FOR_CHARGING;
			case PREPARING_TAG_ID_READY //
				-> Status.READY_FOR_CHARGING;
			case CHARGING, PREPARING_EV_READY //
				-> Status.CHARGING;
			case RESERVED, SUSPENDED_EV, SUSPENDED_EV_SE, FINISHING //
				-> Status.CHARGING_REJECTED;
			case FAULTED, UNAVAILABLE, UNAVAILABLE_CONNECTION_OBJECT //
				-> Status.ERROR;
			case UNAVAILABLE_FW_UPDATE, UNDEFINED //
				-> Status.UNDEFINED;
			});
		});
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return this.modbusProtocol;
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return this.config.minHwPower();
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return this.config.maxHwPower();
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws Exception {
		this.setApplyChargePowerLimit(power);
		return true;
	}

	@Override
	public boolean pauseChargeProcess() throws Exception {
		// Alpitronic is running into a fault state if the applied power is 0
		return this.applyChargePowerLimit(this.config.minHwPower());
	}

	@Override
	public boolean applyDisplayText(String text) throws OpenemsException {
		return false;
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		return 10;
	}

	@Override
	public ChargeStateHandler getChargeStateHandler() {
		return this.chargeStateHandler;
	}

	@Override
	public void logDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public String debugLog() {
		var versionPrefix = this.detectedFirmwareVersion != null ? this.detectedFirmwareVersion.toString() + "|" : "";
		return versionPrefix + "Limit:" + this.getSetChargePowerLimit().orElse(null) + "|" + this.getStatus().getName();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
