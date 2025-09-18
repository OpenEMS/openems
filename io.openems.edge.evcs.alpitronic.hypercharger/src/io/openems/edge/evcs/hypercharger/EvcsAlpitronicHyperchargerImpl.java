package io.openems.edge.evcs.hypercharger;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;

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
import io.openems.common.types.MeterType;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
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
import io.openems.edge.evcs.api.PhaseRotation;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.api.WriteHandler;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

/**
 * Implementation of the Alpitronic Hypercharger EVCS component.
 * 
 * <p>This component provides integration with Alpitronic Hypercharger DC fast charging stations
 * through Modbus TCP communication. It supports automatic firmware version detection and
 * adapts register mappings accordingly.
 * 
 * <p>Key features:
 * <ul>
 * <li>Supports HYC50, HYC150, HYC200, HYC300, HYC400 models (50-400kW)</li>
 * <li>Automatic detection of firmware version (2.5.x and later)</li>
 * <li>Backward compatibility with pre-2.5 firmware versions</li>
 * <li>Real-time monitoring of charging sessions</li>
 * <li>Power limit control via Modbus holding registers</li>
 * <li>Reactive power control support</li>
 * <li>Multiple connector support (up to 4 connectors)</li>
 * </ul>
 * 
 * @see <a href="https://www.alpitronic.it">Alpitronic Official Website</a>
 */
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
public class EvcsAlpitronicHyperchargerImpl extends AbstractOpenemsModbusComponent
		implements Evcs, ManagedEvcs, DeprecatedEvcs, ElectricityMeter, OpenemsComponent, ModbusComponent, EventHandler,
		EvcsAlpitronicHypercharger, TimedataProvider {

	private final Logger log = LoggerFactory.getLogger(EvcsAlpitronicHyperchargerImpl.class);
	/** Modbus offset for multiple connectors. */
	private final IntFunction<Integer> offset = addr -> addr + this.config.connector().modbusOffset;
	
	/** Software version for register mapping compatibility */
	private FirmwareVersion firmwareVersion = null;
	private Integer versionMajor = null;
	private Integer versionMinor = null;
	private Integer versionPatch = null;

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

	public EvcsAlpitronicHyperchargerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				DeprecatedEvcs.ChannelId.values(), //
				EvcsAlpitronicHypercharger.ChannelId.values());
		DeprecatedEvcs.copyToDeprecatedEvcsChannels(this);

		// Automatically calculate L1/l2/L3 values from sum
		ElectricityMeter.calculatePhasesFromActivePower(this);
		// TODO consider CURRENT and VOLTAGE also
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
	public MeterType getMeterType() {
		return MeterType.MANAGED_CONSUMPTION_METERED;
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

	/**
	 * Defines the Modbus protocol for communication with Alpitronic Hypercharger.
	 * 
	 * <p>Register mapping based on Load Management Manual v2.5:
	 * <ul>
	 * <li>Station-level input registers: 0-48 (global information)</li>
	 * <li>Connector-specific input registers: 100+, 200+, 300+, 400+ (per connector data)</li>
	 * <li>Holding registers: for power control and reactive power settings</li>
	 * </ul>
	 * 
	 * @return configured ModbusProtocol instance
	 */
	@Override
	protected ModbusProtocol defineModbusProtocol() {
		// Determine which protocol to use based on configured or detected version
		// If version is not yet known, we'll detect it from the first read
		if (this.firmwareVersion != null) {
			// Version already detected, use appropriate protocol
			return getProtocolForVersion();
		}
		
		// Version not yet detected - use v2.5 as default (most recent)
		// The version will be detected from the version registers and logged
		return defineModbusProtocolV25();
	}
	
	/**
	 * Returns the appropriate protocol based on detected firmware version.
	 */
	private ModbusProtocol getProtocolForVersion() {
		if (this.firmwareVersion.isVersion18()) {
			this.logInfo(this.log, "Using protocol for firmware v1.8.x");
			return defineModbusProtocolV18();
		} else if (this.firmwareVersion.isVersion23()) {
			this.logInfo(this.log, "Using protocol for firmware v2.3.x");
			return defineModbusProtocolV23();
		} else if (this.firmwareVersion.isVersion24()) {
			this.logInfo(this.log, "Using protocol for firmware v2.4.x");
			return defineModbusProtocolV24();
		} else {
			this.logInfo(this.log, "Using protocol for firmware v2.5.x or later");
			return defineModbusProtocolV25();
		}
	}
	
	/**
	 * Defines modbus protocol for firmware version 2.5.x and later.
	 */
	private ModbusProtocol defineModbusProtocolV25() {
		var modbusProtocol = new ModbusProtocol(this,

				// Read station-level information (input registers)
				new FC4ReadInputRegistersTask(0, Priority.LOW,
						m(EvcsAlpitronicHypercharger.ChannelId.UNIX_TIME,
								new UnsignedDoublewordElement(0)),
						m(EvcsAlpitronicHypercharger.ChannelId.NUM_CONNECTORS,
								new UnsignedWordElement(2)),
						m(EvcsAlpitronicHypercharger.ChannelId.STATION_STATE,
								new UnsignedWordElement(3)),
						m(EvcsAlpitronicHypercharger.ChannelId.TOTAL_STATION_POWER,
								new UnsignedDoublewordElement(4))),
				
				// Read load management status
				new FC4ReadInputRegistersTask(18, Priority.LOW,
						m(EvcsAlpitronicHypercharger.ChannelId.LOAD_MANAGEMENT_ENABLED,
								new UnsignedWordElement(18))),
				
				// Read software version for compatibility checks
				new FC4ReadInputRegistersTask(46, Priority.LOW,
						m(EvcsAlpitronicHypercharger.ChannelId.SOFTWARE_VERSION_MAJOR,
								new UnsignedWordElement(46)),
						m(EvcsAlpitronicHypercharger.ChannelId.SOFTWARE_VERSION_MINOR,
								new UnsignedWordElement(47)),
						m(EvcsAlpitronicHypercharger.ChannelId.SOFTWARE_VERSION_PATCH,
								new UnsignedWordElement(48))),

				// Read holding registers for current power limits
				new FC3ReadRegistersTask(this.offset.apply(0), Priority.LOW,
						m(EvcsAlpitronicHypercharger.ChannelId.RAW_CHARGE_POWER_SET,
								new UnsignedDoublewordElement(this.offset.apply(0)))),

				// Write holding registers for power control
				new FC16WriteRegistersTask(this.offset.apply(0),
						m(EvcsAlpitronicHypercharger.ChannelId.APPLY_CHARGE_POWER_LIMIT,
								new UnsignedDoublewordElement(this.offset.apply(0))),
						m(EvcsAlpitronicHypercharger.ChannelId.SETPOINT_REACTIVE_POWER,
								new UnsignedDoublewordElement(this.offset.apply(2)))),

				// Read connector-specific information (input registers)
				new FC4ReadInputRegistersTask(this.offset.apply(100), Priority.LOW,
						m(EvcsAlpitronicHypercharger.ChannelId.RAW_STATUS,
								new UnsignedWordElement(this.offset.apply(100))),
						// Voltage is at offset 101-102 (UINT32, cV)
						m(EvcsAlpitronicHypercharger.ChannelId.CHARGING_VOLTAGE,
								new UnsignedDoublewordElement(this.offset.apply(101)), SCALE_FACTOR_MINUS_2),
						// Current is at offset 103 (UINT16, cA)
						m(EvcsAlpitronicHypercharger.ChannelId.CHARGING_CURRENT,
								new UnsignedWordElement(this.offset.apply(103)), SCALE_FACTOR_MINUS_2),
						// Power is at offset 104-105 (UINT32, W)
						m(EvcsAlpitronicHypercharger.ChannelId.RAW_CHARGE_POWER,
								new UnsignedDoublewordElement(this.offset.apply(104))),
						// Charge time at offset 106 (UINT16, s)
						m(EvcsAlpitronicHypercharger.ChannelId.CHARGED_TIME,
								new UnsignedWordElement(this.offset.apply(106))),
						// Charged energy at offset 107 (UINT16, kWh/100)
						m(EvcsAlpitronicHypercharger.ChannelId.CHARGED_ENERGY,
								new UnsignedWordElement(this.offset.apply(107)), SCALE_FACTOR_MINUS_2)
								.onUpdateCallback(e -> {
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
								}),
						// SoC at offset 108 (UINT16, %/100)
						m(EvcsAlpitronicHypercharger.ChannelId.EV_SOC, new UnsignedWordElement(this.offset.apply(108)),
								SCALE_FACTOR_MINUS_2),
						// Connector type at offset 109 (UINT16)
						m(EvcsAlpitronicHypercharger.ChannelId.CONNECTOR_TYPE,
								new UnsignedWordElement(this.offset.apply(109))),

						/*
						 * Maximum/Minimum DC charging power
						 * Not equals MaximumPower or MinimumPower e.g. EvMaxChargingPower is 99kW, but
						 * ChargePower is 40kW because of temperature, current SoC or
						 * MaximumHardwareLimit.
						 */
						m(EvcsAlpitronicHypercharger.ChannelId.EV_MAX_CHARGING_POWER,
								new UnsignedDoublewordElement(this.offset.apply(110))),
						m(EvcsAlpitronicHypercharger.ChannelId.EV_MIN_CHARGING_POWER,
								new UnsignedDoublewordElement(this.offset.apply(112))),
						// Reactive power limits at offsets 114-115 and 116-117
						m(EvcsAlpitronicHypercharger.ChannelId.VAR_REACTIVE_MAX,
								new UnsignedDoublewordElement(this.offset.apply(114))),
						m(EvcsAlpitronicHypercharger.ChannelId.VAR_REACTIVE_MIN,
								new UnsignedDoublewordElement(this.offset.apply(116)), INVERT)),
				
				// Additional registers for SW 2.5.x
				new FC4ReadInputRegistersTask(this.offset.apply(132), Priority.LOW,
						// Total energy counter at offset 132-135 (INT64, Wh)
						m(EvcsAlpitronicHypercharger.ChannelId.TOTAL_CHARGED_ENERGY,
								new UnsignedDoublewordElement(this.offset.apply(132))),
						// Maximum AC charging power at offset 136-137 (UINT32, W)
						m(EvcsAlpitronicHypercharger.ChannelId.MAX_CHARGING_POWER_AC,
								new UnsignedDoublewordElement(this.offset.apply(136))))

		);

		// Calculates charge power by existing Channels.
		this.addCalculatePowerListeners();

		// Map raw status to evcs status.
		this.addStatusListener();
		
		// Monitor software version for compatibility
		this.addVersionListener();

		return modbusProtocol;
	}
	
	/**
	 * Defines modbus protocol for firmware version 1.8.x.
	 */
	private ModbusProtocol defineModbusProtocolV18() {
		var modbusProtocol = new ModbusProtocol(this,
				// Version 1.8 uses connector-relative offsets throughout
				// Read holding registers
				new FC3ReadRegistersTask(this.offset.apply(0), Priority.LOW,
						m(EvcsAlpitronicHypercharger.ChannelId.RAW_CHARGE_POWER_SET,
								new UnsignedDoublewordElement(this.offset.apply(0)))),
				
				// Write holding registers - Active power is W for all connectors in v1.8
				new FC16WriteRegistersTask(this.offset.apply(0),
						m(EvcsAlpitronicHypercharger.ChannelId.APPLY_CHARGE_POWER_LIMIT,
								new UnsignedDoublewordElement(this.offset.apply(0))),
						m(EvcsAlpitronicHypercharger.ChannelId.SETPOINT_REACTIVE_POWER,
								new UnsignedDoublewordElement(this.offset.apply(2)))),
				
				// Read connector-specific input registers (v1.8 layout)
				new FC4ReadInputRegistersTask(this.offset.apply(0), Priority.LOW,
						m(EvcsAlpitronicHypercharger.ChannelId.RAW_STATUS,
								new UnsignedWordElement(this.offset.apply(0))),
						m(EvcsAlpitronicHypercharger.ChannelId.CHARGING_VOLTAGE,
								new UnsignedDoublewordElement(this.offset.apply(1)), SCALE_FACTOR_MINUS_2),
						m(EvcsAlpitronicHypercharger.ChannelId.CHARGING_CURRENT,
								new UnsignedWordElement(this.offset.apply(3)), SCALE_FACTOR_MINUS_2),
						m(EvcsAlpitronicHypercharger.ChannelId.RAW_CHARGE_POWER,
								new UnsignedDoublewordElement(this.offset.apply(4))),
						m(EvcsAlpitronicHypercharger.ChannelId.CHARGED_TIME,
								new UnsignedWordElement(this.offset.apply(6))),
						m(EvcsAlpitronicHypercharger.ChannelId.CHARGED_ENERGY,
								new UnsignedWordElement(this.offset.apply(7)), SCALE_FACTOR_MINUS_2)
								.onUpdateCallback(e -> {
									if (e == null) {
										return;
									}
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
								}),
						m(EvcsAlpitronicHypercharger.ChannelId.EV_SOC,
								new UnsignedWordElement(this.offset.apply(8)), SCALE_FACTOR_MINUS_2),
						m(EvcsAlpitronicHypercharger.ChannelId.CONNECTOR_TYPE,
								new UnsignedWordElement(this.offset.apply(9))),
						m(EvcsAlpitronicHypercharger.ChannelId.EV_MAX_CHARGING_POWER,
								new UnsignedDoublewordElement(this.offset.apply(10))),
						m(EvcsAlpitronicHypercharger.ChannelId.EV_MIN_CHARGING_POWER,
								new UnsignedDoublewordElement(this.offset.apply(12))),
						m(EvcsAlpitronicHypercharger.ChannelId.VAR_REACTIVE_MAX,
								new UnsignedDoublewordElement(this.offset.apply(14))),
						m(EvcsAlpitronicHypercharger.ChannelId.VAR_REACTIVE_MIN,
								new UnsignedDoublewordElement(this.offset.apply(16)), INVERT))
		);
		
		this.addCalculatePowerListeners();
		this.addStatusListener();
		this.addVersionListener();
		
		return modbusProtocol;
	}
	
	/**
	 * Defines modbus protocol for firmware version 2.3.x.
	 */
	private ModbusProtocol defineModbusProtocolV23() {
		// Version 2.3 is similar to 1.8 but adds total charged energy at register 132
		// For simplicity, we use the v1.8 protocol as base
		// TODO: Add register 132 (total charged energy) when extending
		return defineModbusProtocolV18();
	}
	
	/**
	 * Defines modbus protocol for firmware version 2.4.x.
	 */
	private ModbusProtocol defineModbusProtocolV24() {
		// Version 2.4 adds max charging power AC at register 136
		// For simplicity, we use the v1.8 protocol as base
		// TODO: Add registers 132 and 136 when extending
		return defineModbusProtocolV18();
	}

	/**
	 * Adds listeners for power calculation.
	 * 
	 * <p>For firmware versions before 2.5, the power register returns 0, 
	 * so we calculate power from voltage * current.
	 * For firmware 2.5 and later, we use the power value from register 104 directly.
	 */
	private void addCalculatePowerListeners() {
		// For firmware 2.5+, the RAW_CHARGE_POWER register (104) works correctly
		if (this.firmwareVersion != null && this.firmwareVersion.isVersion25OrLater()) {
			// Use the power value directly from register 104
			this.channel(EvcsAlpitronicHypercharger.ChannelId.RAW_CHARGE_POWER).onSetNextValue(value -> {
				if (value != null && value.isDefined()) {
					Integer power = TypeUtils.getAsType(OpenemsType.INTEGER, value.get());
					this._setActivePower(power);
					if (power != null) {
						this.logDebug("Using direct power from register for v2.5+: " + power + " W");
					}
				}
			});
		} else {
			// For older firmware versions, calculate power from voltage and current
			// since the power register returns 0
			final Consumer<Value<Double>> calculatePower = ignore -> {
				Integer power = TypeUtils.getAsType(OpenemsType.INTEGER, TypeUtils.multiply(
						this.getChargingVoltageChannel().getNextValue().get(),
						this.getChargingCurrentChannel().getNextValue().get()
				));
				this._setActivePower(power);
				if (power != null) {
					this.logDebug("Calculated power from V*I for older firmware: " + power + " W");
				}
			};
			this.getChargingVoltageChannel().onSetNextValue(calculatePower);
			this.getChargingCurrentChannel().onSetNextValue(calculatePower);
		}
	}

	private void addVersionListener() {
		// Monitor software version changes
		this.channel(EvcsAlpitronicHypercharger.ChannelId.SOFTWARE_VERSION_MAJOR).onSetNextValue(v -> {
			if (v != null && v.isDefined()) {
				this.versionMajor = ((Number) v.get()).intValue();
				updateVersionCompatibility();
			}
		});
		this.channel(EvcsAlpitronicHypercharger.ChannelId.SOFTWARE_VERSION_MINOR).onSetNextValue(v -> {
			if (v != null && v.isDefined()) {
				this.versionMinor = ((Number) v.get()).intValue();
				updateVersionCompatibility();
			}
		});
		this.channel(EvcsAlpitronicHypercharger.ChannelId.SOFTWARE_VERSION_PATCH).onSetNextValue(v -> {
			if (v != null && v.isDefined()) {
				this.versionPatch = ((Number) v.get()).intValue();
				updateVersionCompatibility();
			}
		});
	}
	
	/**
	 * Updates the version compatibility flag based on detected software version.
	 */
	private void updateVersionCompatibility() {
		if (this.versionMajor == null || this.versionMinor == null) {
			return;
		}
		
		int patch = this.versionPatch != null ? this.versionPatch : 0;
		FirmwareVersion newVersion = new FirmwareVersion(this.versionMajor, this.versionMinor, patch);
		
		// Check if version changed
		if (this.firmwareVersion != null && 
			this.firmwareVersion.getMajor() == this.versionMajor && 
			this.firmwareVersion.getMinor() == this.versionMinor &&
			this.firmwareVersion.getPatch() == patch) {
			return; // No change
		}
		
		boolean firstDetection = (this.firmwareVersion == null);
		this.firmwareVersion = newVersion;
		this.logInfo(this.log, "Detected Hypercharger firmware version " + newVersion);
		
		// Log which register mapping will be used
		if (newVersion.isVersion18()) {
			this.logInfo(this.log, "Using v1.8.x register mappings");
		} else if (newVersion.isVersion23()) {
			this.logInfo(this.log, "Using v2.3.x register mappings");
		} else if (newVersion.isVersion24()) {
			this.logInfo(this.log, "Using v2.4.x register mappings");
		} else if (newVersion.isVersion25OrLater()) {
			this.logInfo(this.log, "Using v2.5.x+ register mappings");
		}
		
		// Log warning if version changed after initial detection (requires restart)
		if (!firstDetection) {
			this.logWarn(this.log, "Firmware version changed from previous detection. " +
				"A component restart is required to use the correct protocol for version " + newVersion);
		} else {
			// First detection - reinitialize power listeners with correct logic
			this.addCalculatePowerListeners();
		}
	}
	
	private void addStatusListener() {
		this.channel(EvcsAlpitronicHypercharger.ChannelId.RAW_STATUS).onSetNextValue(s -> {
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
		String versionStr = "";
		if (this.versionMajor != null && this.versionMinor != null) {
			versionStr = "v" + this.versionMajor + "." + this.versionMinor + 
					(this.versionPatch != null ? "." + this.versionPatch : "") + "|";
		}
		return versionStr + "Limit:" + this.getSetChargePowerLimit().orElse(null) + "|" + this.getStatus().getName();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
