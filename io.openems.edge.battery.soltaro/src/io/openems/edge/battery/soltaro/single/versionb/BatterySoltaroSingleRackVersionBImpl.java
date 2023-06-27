package io.openems.edge.battery.soltaro.single.versionb;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIRECT_1_TO_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
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

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.battery.soltaro.common.batteryprotection.BatteryProtectionDefinitionSoltaro3000Wh;
import io.openems.edge.battery.soltaro.common.batteryprotection.BatteryProtectionDefinitionSoltaro3500Wh;
import io.openems.edge.battery.soltaro.common.enums.ModuleType;
import io.openems.edge.battery.soltaro.single.versionb.statemachine.Context;
import io.openems.edge.battery.soltaro.single.versionb.statemachine.ControlAndLogic;
import io.openems.edge.battery.soltaro.single.versionb.statemachine.StateMachine;
import io.openems.edge.battery.soltaro.single.versionb.statemachine.StateMachine.State;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.ChannelId.ChannelIdImpl;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Bms.Soltaro.SingleRack.VersionB", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class BatterySoltaroSingleRackVersionBImpl extends AbstractOpenemsModbusComponent implements Battery,
		ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave, StartStoppable, BatterySoltaroSingleRackVersionB {

	private final Logger log = LoggerFactory.getLogger(BatterySoltaroSingleRackVersionBImpl.class);
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);
	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(StartStop.UNDEFINED);

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;
	private BatteryProtection batteryProtection = null;
	private Optional<Integer> numberOfModules = Optional.empty();

	public BatterySoltaroSingleRackVersionBImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				BatterySoltaroSingleRackVersionB.ChannelId.values(), //
				BatteryProtection.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		// Initialize Battery-Protection
		if (config.moduleType() == ModuleType.MODULE_3_5_KWH) {
			// Special settings for 3.5 kWh module
			this.batteryProtection = BatteryProtection.create(this) //
					.applyBatteryProtectionDefinition(new BatteryProtectionDefinitionSoltaro3500Wh(),
							this.componentManager) //
					.build();
		} else {
			// Default
			this.batteryProtection = BatteryProtection.create(this) //
					.applyBatteryProtectionDefinition(new BatteryProtectionDefinitionSoltaro3000Wh(),
							this.componentManager) //
					.build();
		}

		ControlAndLogic.setWatchdog(this, config.watchdog());
		ControlAndLogic.setSoCLowAlarm(this, config.SoCLowAlarm());

		this.getNumberOfModules().thenAccept(numberOfModules -> {
			this.numberOfModules = Optional.of(numberOfModules);
			this.calculateCapacity(numberOfModules);
			this.createDynamicChannels(numberOfModules);
		});
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void handleStateMachine() {
		// Store the current State
		this.channel(BatterySoltaroSingleRackVersionB.ChannelId.STATE_MACHINE)
				.setNextValue(this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		// Prepare Context
		var context = new Context(this, this.config, this.numberOfModules);

		// Call the StateMachine
		try {
			this.stateMachine.run(context);

			this.channel(BatterySoltaroSingleRackVersionB.ChannelId.RUN_FAILED).setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(BatterySoltaroSingleRackVersionB.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {

		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			// TODO set soltaro protect/recover registers
			this.batteryProtection.apply();
			break;

		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.handleStateMachine();
			break;
		}
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode) //
		);
	}

	@Override
	public void setStartStop(StartStop value) {
		if (this.startStopTarget.getAndSet(value) != value) {
			// Set only if value changed
			this.stateMachine.forceNextState(State.UNDEFINED);
		}
	}

	@Override
	public StartStop getStartStopTarget() {
		switch (this.config.startStop()) {
		case AUTO:
			// read StartStop-Channel
			return this.startStopTarget.get();

		case START:
			// force START
			return StartStop.START;

		case STOP:
			// force STOP
			return StartStop.STOP;
		}

		assert false;
		return StartStop.UNDEFINED; // can never happen
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc() //
				+ "|Discharge:" + this.getDischargeMinVoltage() + ";" + this.getDischargeMaxCurrent() //
				+ "|Charge:" + this.getChargeMaxVoltage() + ";" + this.getChargeMaxCurrent() //
				+ "|Cell Voltages: Min:" + this.getMinCellVoltage() + "; Max:" + this.getMaxCellVoltage() //
				+ "|State:" + this.stateMachine.getCurrentState().asCamelCase();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

		var protocol = new ModbusProtocol(this, //
				// Main switch
				new FC6WriteRegisterTask(0x2010,
						m(BatterySoltaroSingleRackVersionB.ChannelId.BMS_CONTACTOR_CONTROL,
								new UnsignedWordElement(0x2010))), //

				// System reset
				new FC6WriteRegisterTask(0x2004, //
						m(BatterySoltaroSingleRackVersionB.ChannelId.SYSTEM_RESET, new UnsignedWordElement(0x2004))), //

				// EMS timeout --> Watchdog
				new FC6WriteRegisterTask(0x201C, //
						m(BatterySoltaroSingleRackVersionB.ChannelId.EMS_COMMUNICATION_TIMEOUT,
								new UnsignedWordElement(0x201C))), //

				// Sleep
				new FC6WriteRegisterTask(0x201D, //
						m(BatterySoltaroSingleRackVersionB.ChannelId.SLEEP, new UnsignedWordElement(0x201D))), //

				// Work parameter
				new FC6WriteRegisterTask(0x20C1, //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WORK_PARAMETER_NUMBER_OF_MODULES,
								new UnsignedWordElement(0x20C1))), //

				// Parameters for configuring
				new FC6WriteRegisterTask(0x2014,
						m(BatterySoltaroSingleRackVersionB.ChannelId.AUTO_SET_SLAVES_ID,
								new UnsignedWordElement(0x2014))),
				new FC6WriteRegisterTask(0x2019,
						m(BatterySoltaroSingleRackVersionB.ChannelId.AUTO_SET_SLAVES_TEMPERATURE_ID,
								new UnsignedWordElement(0x2019))),

				// Control registers
				new FC3ReadRegistersTask(0x2000, Priority.HIGH, //
						m(BatterySoltaroSingleRackVersionB.ChannelId.FAN_STATUS, new UnsignedWordElement(0x2000)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.MAIN_CONTACTOR_STATE,
								new UnsignedWordElement(0x2001)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.DRY_CONTACT_1_EXPORT,
								new UnsignedWordElement(0x2002)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.DRY_CONTACT_2_EXPORT,
								new UnsignedWordElement(0x2003)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.SYSTEM_RESET, new UnsignedWordElement(0x2004)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.SYSTEM_RUN_MODE, new UnsignedWordElement(0x2005)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.PRE_CONTACTOR_STATUS,
								new UnsignedWordElement(0x2006)), //
						m(new BitsWordElement(0x2007, this) //
								.bit(15, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_FLAG_STATUS_DISCHARGE_TEMPERATURE_LOW) //
								.bit(14, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_FLAG_STATUS_DISCHARGE_TEMPERATURE_HIGH) //
								.bit(13, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_FLAG_STATUS_VOLTAGE_DIFFERENCE) //
								.bit(12, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_FLAG_STATUS_INSULATION_LOW) //
								.bit(11, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_FLAG_STATUS_CELL_VOLTAGE_DIFFERENCE) //
								.bit(10, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_FLAG_STATUS_ELECTRODE_TEMPERATURE_HIGH) //
								.bit(9, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_FLAG_STATUS_TEMPERATURE_DIFFERENCE) //
								.bit(8, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_FLAG_STATUS_SOC_LOW) //
								.bit(7, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_FLAG_STATUS_CELL_OVER_TEMPERATURE) //
								.bit(6, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_FLAG_STATUS_CELL_LOW_TEMPERATURE) //
								.bit(5, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_FLAG_STATUS_DISCHARGE_OVER_CURRENT) //
								.bit(4, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_FLAG_STATUS_SYSTEM_LOW_VOLTAGE) //
								.bit(3, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_FLAG_STATUS_CELL_LOW_VOLTAGE) //
								.bit(2, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_FLAG_STATUS_CHARGE_OVER_CURRENT) //
								.bit(1, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_FLAG_STATUS_SYSTEM_OVER_VOLTAGE) //
								.bit(0, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_FLAG_STATUS_CELL_OVER_VOLTAGE)), //
						m(new BitsWordElement(0x2008, this) //
								.bit(15, BatterySoltaroSingleRackVersionB.ChannelId.PROTECT_FLAG_STATUS_DISCHARGE_TEMPERATURE_LOW) //
								.bit(14, BatterySoltaroSingleRackVersionB.ChannelId.PROTECT_FLAG_STATUS_DISCHARGE_TEMPERATURE_HIGH) //
								.bit(13, BatterySoltaroSingleRackVersionB.ChannelId.PROTECT_FLAG_STATUS_VOLTAGE_DIFFERENCE) //
								.bit(12, BatterySoltaroSingleRackVersionB.ChannelId.PROTECT_FLAG_STATUS_INSULATION_LOW) //
								.bit(11, BatterySoltaroSingleRackVersionB.ChannelId.PROTECT_FLAG_STATUS_CELL_VOLTAGE_DIFFERENCE) //
								.bit(10, BatterySoltaroSingleRackVersionB.ChannelId.PROTECT_FLAG_STATUS_ELECTRODE_TEMPERATURE_HIGH) //
								.bit(9, BatterySoltaroSingleRackVersionB.ChannelId.PROTECT_FLAG_STATUS_TEMPERATURE_DIFFERENCE) //
								.bit(8, BatterySoltaroSingleRackVersionB.ChannelId.PROTECT_FLAG_STATUS_SOC_LOW) //
								.bit(7, BatterySoltaroSingleRackVersionB.ChannelId.PROTECT_FLAG_STATUS_CELL_OVER_TEMPERATURE) //
								.bit(6, BatterySoltaroSingleRackVersionB.ChannelId.PROTECT_FLAG_STATUS_CELL_LOW_TEMPERATURE) //
								.bit(5, BatterySoltaroSingleRackVersionB.ChannelId.PROTECT_FLAG_STATUS_DISCHARGE_OVER_CURRENT) //
								.bit(4, BatterySoltaroSingleRackVersionB.ChannelId.PROTECT_FLAG_STATUS_SYSTEM_LOW_VOLTAGE) //
								.bit(3, BatterySoltaroSingleRackVersionB.ChannelId.PROTECT_FLAG_STATUS_CELL_LOW_VOLTAGE) //
								.bit(2, BatterySoltaroSingleRackVersionB.ChannelId.PROTECT_FLAG_STATUS_CHARGE_OVER_CURRENT) //
								.bit(1, BatterySoltaroSingleRackVersionB.ChannelId.PROTECT_FLAG_STATUS_SYSTEM_OVER_VOLTAGE) //
								.bit(0, BatterySoltaroSingleRackVersionB.ChannelId.PROTECT_FLAG_STATUS_CELL_OVER_VOLTAGE)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.ALARM_FLAG_REGISTER_1,
								new UnsignedWordElement(0x2009)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.ALARM_FLAG_REGISTER_2,
								new UnsignedWordElement(0x200A)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.PROTECT_FLAG_REGISTER_1,
								new UnsignedWordElement(0x200B)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.PROTECT_FLAG_REGISTER_2,
								new UnsignedWordElement(0x200C)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.SHORT_CIRCUIT_FUNCTION,
								new UnsignedWordElement(0x200D)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.TESTING_IO, new UnsignedWordElement(0x200E)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.SOFT_SHUTDOWN, new UnsignedWordElement(0x200F)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.BMS_CONTACTOR_CONTROL,
								new UnsignedWordElement(0x2010)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.CURRENT_BOX_SELF_CALIBRATION,
								new UnsignedWordElement(0x2011)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.PCS_ALARM_RESET, new UnsignedWordElement(0x2012)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.INSULATION_SENSOR_FUNCTION,
								new UnsignedWordElement(0x2013)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.AUTO_SET_SLAVES_ID,
								new UnsignedWordElement(0x2014)), //
						new DummyRegisterElement(0x2015, 0x2018), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.AUTO_SET_SLAVES_TEMPERATURE_ID,
								new UnsignedWordElement(0x2019)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.TRANSPARENT_MASTER,
								new UnsignedWordElement(0x201A)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.SET_EMS_ADDRESS, new UnsignedWordElement(0x201B)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.EMS_COMMUNICATION_TIMEOUT,
								new UnsignedWordElement(0x201C)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.SLEEP, new UnsignedWordElement(0x201D)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.VOLTAGE_LOW_PROTECTION,
								new UnsignedWordElement(0x201E))), //

				// Voltage ranges
				new FC3ReadRegistersTask(0x2082, Priority.LOW, //
						m(new UnsignedWordElement(0x2082)) //
								.m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_ALARM,
										SCALE_FACTOR_2) //
								.m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, SCALE_FACTOR_MINUS_1) //
								.build(), //
						new DummyRegisterElement(0x2083, 0x2087), //
						m(new UnsignedWordElement(0x2088)) //
								.m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_ALARM,
										SCALE_FACTOR_2) //
								.m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, SCALE_FACTOR_MINUS_1) //
								.build()), //

				// Summary state
				new FC3ReadRegistersTask(0x2100, Priority.HIGH, //
						m(new UnsignedWordElement(0x2100)) //
								.m(BatterySoltaroSingleRackVersionB.ChannelId.CLUSTER_1_VOLTAGE, SCALE_FACTOR_2) //
								.m(Battery.ChannelId.VOLTAGE, SCALE_FACTOR_MINUS_1) //
								.build(), //
						m(new SignedWordElement(0x2101)) //
								.m(BatterySoltaroSingleRackVersionB.ChannelId.CLUSTER_1_CURRENT, SCALE_FACTOR_2) //
								.m(Battery.ChannelId.CURRENT, SCALE_FACTOR_MINUS_1) //
								.build(), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.CHARGE_INDICATION,
								new UnsignedWordElement(0x2102)),
						m(Battery.ChannelId.SOC, new UnsignedWordElement(0x2103)),
						m(BatterySoltaroSingleRackVersionB.ChannelId.CLUSTER_1_SOH, new UnsignedWordElement(0x2104)),
						m(BatterySoltaroSingleRackVersionB.ChannelId.CLUSTER_1_MAX_CELL_VOLTAGE_ID,
								new UnsignedWordElement(0x2105)), //
						m(new SignedWordElement(0x2106)) //
								.m(BatterySoltaroSingleRackVersionB.ChannelId.CLUSTER_1_MAX_CELL_VOLTAGE, DIRECT_1_TO_1) //
								.m(Battery.ChannelId.MAX_CELL_VOLTAGE, DIRECT_1_TO_1) //
								.build(), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.CLUSTER_1_MIN_CELL_VOLTAGE_ID,
								new UnsignedWordElement(0x2107)), //
						m(new UnsignedWordElement(0x2108)) //
								.m(BatterySoltaroSingleRackVersionB.ChannelId.CLUSTER_1_MIN_CELL_VOLTAGE, DIRECT_1_TO_1) //
								.m(Battery.ChannelId.MIN_CELL_VOLTAGE, DIRECT_1_TO_1) //
								.build(), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.CLUSTER_1_MAX_CELL_TEMPERATURE_ID,
								new UnsignedWordElement(0x2109)), //
						m(new SignedWordElement(0x210A)) //
								.m(BatterySoltaroSingleRackVersionB.ChannelId.CLUSTER_1_MAX_CELL_TEMPERATURE,
										DIRECT_1_TO_1) //
								.m(Battery.ChannelId.MAX_CELL_TEMPERATURE, SCALE_FACTOR_MINUS_1) //
								.build(), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.CLUSTER_1_MIN_CELL_TEMPERATURE_ID,
								new UnsignedWordElement(0x210B)), //
						m(new SignedWordElement(0x210C)) //
								.m(BatterySoltaroSingleRackVersionB.ChannelId.CLUSTER_1_MIN_CELL_TEMPERATURE,
										DIRECT_1_TO_1) //
								.m(Battery.ChannelId.MIN_CELL_TEMPERATURE, SCALE_FACTOR_MINUS_1) //
								.build(), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.MAX_CELL_RESISTANCE_ID,
								new UnsignedWordElement(0x210D)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.MAX_CELL_RESISTANCE,
								new UnsignedWordElement(0x210E), SCALE_FACTOR_1), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.MIN_CELL_RESISTANCE_ID,
								new UnsignedWordElement(0x210F)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.MIN_CELL_RESISTANCE,
								new UnsignedWordElement(0x2110), SCALE_FACTOR_1), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.POSITIVE_INSULATION,
								new UnsignedWordElement(0x2111)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.NEGATIVE_INSULATION,
								new UnsignedWordElement(0x2112)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.MAIN_CONTACTOR_FLAG,
								new UnsignedWordElement(0x2113)), //
						new DummyRegisterElement(0x2114),
						m(BatterySoltaroSingleRackVersionB.ChannelId.ENVIRONMENT_TEMPERATURE,
								new UnsignedWordElement(0x2115)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.SYSTEM_INSULATION,
								new UnsignedWordElement(0x2116)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.CELL_VOLTAGE_DIFFERENCE,
								new UnsignedWordElement(0x2117)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.TOTAL_VOLTAGE_DIFFERENCE,
								new UnsignedWordElement(0x2118), SCALE_FACTOR_2), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.POWER_TEMPERATURE,
								new UnsignedWordElement(0x2119)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.POWER_SUPPLY_VOLTAGE,
								new UnsignedWordElement(0x211A))), //

				// Critical state
				new FC3ReadRegistersTask(0x2140, Priority.HIGH, //
						m(new BitsWordElement(0x2140, this) //
								.bit(0, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_HIGH) //
								.bit(1, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH) //
								.bit(2, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_CHA_CURRENT_HIGH) //
								.bit(3, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_LOW) //
								.bit(4, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW) //
								.bit(5, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_DISCHA_CURRENT_HIGH) //
								.bit(6, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH) //
								.bit(7, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_LOW) //
								.bit(8, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_SOC_LOW) //
								.bit(9, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_TEMPERATURE_DIFFERENCE_HIGH) //
								.bit(10, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_POLES_TEMPERATURE_DIFFERENCE_HIGH) //
								.bit(11, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_DIFFERENCE_HIGH) //
								.bit(12, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_INSULATION_LOW) //
								.bit(13, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_DIFFERENCE_HIGH) //
								.bit(14, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH) //
								.bit(15, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW)), //
						m(new BitsWordElement(0x2141, this) //
								.bit(0, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_1_CELL_VOLTAGE_HIGH) //
								.bit(1, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH) //
								.bit(2, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_1_CHA_CURRENT_HIGH) //
								.bit(3, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_1_CELL_VOLTAGE_LOW) //
								.bit(4, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW) //
								.bit(5, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_1_DISCHA_CURRENT_HIGH) //
								.bit(6, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH) //
								.bit(7, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_1_CELL_CHA_TEMP_LOW) //
								.bit(8, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_1_SOC_LOW) //
								.bit(9, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH) //
								.bit(10, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_1_POLE_TEMPERATURE_TOO_HIGH) //
								.bit(11, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH) //
								.bit(12, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_1_INSULATION_LOW) //
								.bit(13, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH) //
								.bit(14, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH) //
								.bit(15, BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.CLUSTER_RUN_STATE,
								new UnsignedWordElement(0x2142)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.MAXIMUM_CELL_VOLTAGE_NUMBER_WHEN_ALARM,
								new UnsignedWordElement(0x2143)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.MAXIMUM_CELL_VOLTAGE_WHEN_ALARM,
								new UnsignedWordElement(0x2144)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.MAXIMUM_CELL_VOLTAGE_NUMBER_WHEN_STOPPED,
								new UnsignedWordElement(0x2145)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.MAXIMUM_CELL_VOLTAGE_WHEN_STOPPED,
								new UnsignedWordElement(0x2146)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.MINIMUM_CELL_VOLTAGE_NUMBER_WHEN_ALARM,
								new UnsignedWordElement(0x2147)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.MINIMUM_CELL_VOLTAGE_WHEN_ALARM,
								new UnsignedWordElement(0x2148)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.MINIMUM_CELL_VOLTAGE_NUMBER_WHEN_STOPPED,
								new UnsignedWordElement(0x2149)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.MINIMUM_CELL_VOLTAGE_WHEN_STOPPED,
								new UnsignedWordElement(0x214A)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.OVER_VOLTAGE_VALUE_WHEN_ALARM,
								new UnsignedWordElement(0x214B)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.OVER_VOLTAGE_VALUE_WHEN_STOPPED,
								new UnsignedWordElement(0x214C)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.UNDER_VOLTAGE_VALUE_WHEN_ALARM,
								new UnsignedWordElement(0x214D)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.UNDER_VOLTAGE_VALUE_WHEN_STOPPED,
								new UnsignedWordElement(0x214E)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.OVER_CHARGE_CURRENT_WHEN_ALARM,
								new UnsignedWordElement(0x214F)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.OVER_CHARGE_CURRENT_WHEN_STOPPED,
								new UnsignedWordElement(0x2150)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.OVER_DISCHARGE_CURRENT_WHEN_ALARM,
								new UnsignedWordElement(0x2151)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.OVER_DISCHARGE_CURRENT_WHEN_STOPPED,
								new UnsignedWordElement(0x2152)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.NUMBER_OF_TEMPERATURE_WHEN_ALARM,
								new UnsignedWordElement(0x2153)), //
						new DummyRegisterElement(0x2154, 0x215A), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.OTHER_ALARM_EQUIPMENT_FAILURE,
								new UnsignedWordElement(0x215B)), //
						new DummyRegisterElement(0x215C, 0x215F), //
						m(BatteryProtection.ChannelId.BP_CHARGE_BMS, new UnsignedWordElement(0x2160),
								SCALE_FACTOR_MINUS_1), //
						m(BatteryProtection.ChannelId.BP_DISCHARGE_BMS, new UnsignedWordElement(0x2161),
								SCALE_FACTOR_MINUS_1)), //

				// Cluster info
				new FC3ReadRegistersTask(0x2180, Priority.LOW, //
						m(BatterySoltaroSingleRackVersionB.ChannelId.CYCLE_TIME, new UnsignedWordElement(0x2180)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.TOTAL_CAPACITY_HIGH_BITS,
								new UnsignedWordElement(0x2181)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.TOTAL_CAPACITY_LOW_BITS,
								new UnsignedWordElement(0x2182)), //
						m(new BitsWordElement(0x2183, this) //
								.bit(3, BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_20_COMMUNICATION_ERROR) //
								.bit(2, BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_19_COMMUNICATION_ERROR) //
								.bit(1, BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_18_COMMUNICATION_ERROR) //
								.bit(0, BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_17_COMMUNICATION_ERROR)), //
						m(new BitsWordElement(0x2184, this) //
								.bit(15, BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_16_COMMUNICATION_ERROR) //
								.bit(14, BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_15_COMMUNICATION_ERROR) //
								.bit(13, BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_14_COMMUNICATION_ERROR) //
								.bit(12, BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_13_COMMUNICATION_ERROR) //
								.bit(11, BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_12_COMMUNICATION_ERROR) //
								.bit(10, BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_11_COMMUNICATION_ERROR) //
								.bit(9, BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_10_COMMUNICATION_ERROR) //
								.bit(8, BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_9_COMMUNICATION_ERROR) //
								.bit(7, BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_8_COMMUNICATION_ERROR) //
								.bit(6, BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_7_COMMUNICATION_ERROR) //
								.bit(5, BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_6_COMMUNICATION_ERROR) //
								.bit(4, BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_5_COMMUNICATION_ERROR) //
								.bit(3, BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_4_COMMUNICATION_ERROR) //
								.bit(2, BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_3_COMMUNICATION_ERROR) //
								.bit(1, BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_2_COMMUNICATION_ERROR) //
								.bit(0, BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_1_COMMUNICATION_ERROR)), //
						m(new BitsWordElement(0x2185, this) //
								.bit(0, BatterySoltaroSingleRackVersionB.ChannelId.FAILURE_SAMPLING_WIRE) //
								.bit(1, BatterySoltaroSingleRackVersionB.ChannelId.FAILURE_CONNECTOR_WIRE) //
								.bit(2, BatterySoltaroSingleRackVersionB.ChannelId.FAILURE_LTC6803) //
								.bit(3, BatterySoltaroSingleRackVersionB.ChannelId.FAILURE_VOLTAGE_SAMPLING) //
								.bit(4, BatterySoltaroSingleRackVersionB.ChannelId.FAILURE_TEMP_SAMPLING) //
								.bit(5, BatterySoltaroSingleRackVersionB.ChannelId.FAILURE_TEMP_SENSOR) //
								.bit(6, BatterySoltaroSingleRackVersionB.ChannelId.FAILURE_GR_T) //
								.bit(7, BatterySoltaroSingleRackVersionB.ChannelId.FAILURE_PCB) //
								.bit(8, BatterySoltaroSingleRackVersionB.ChannelId.FAILURE_BALANCING_MODULE) //
								.bit(9, BatterySoltaroSingleRackVersionB.ChannelId.FAILURE_TEMP_SAMPLING_LINE) //
								.bit(10, BatterySoltaroSingleRackVersionB.ChannelId.FAILURE_INTRANET_COMMUNICATION) //
								.bit(11, BatterySoltaroSingleRackVersionB.ChannelId.FAILURE_EEPROM) //
								.bit(12, BatterySoltaroSingleRackVersionB.ChannelId.FAILURE_INITIALIZATION)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.SYSTEM_TIME_HIGH, new UnsignedWordElement(0x2186)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.SYSTEM_TIME_LOW, new UnsignedWordElement(0x2187)), //
						new DummyRegisterElement(0x2188, 0x218E), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.LAST_TIME_CHARGE_CAPACITY_LOW_BITS,
								new UnsignedWordElement(0x218F), SCALE_FACTOR_1), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.LAST_TIME_CHARGE_END_TIME_HIGH_BITS,
								new UnsignedWordElement(0x2190)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.LAST_TIME_CHARGE_END_TIME_LOW_BITS,
								new UnsignedWordElement(0x2191)), //
						new DummyRegisterElement(0x2192), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.LAST_TIME_DISCHARGE_CAPACITY_LOW_BITS,
								new UnsignedWordElement(0x2193), SCALE_FACTOR_1), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.LAST_TIME_DISCHARGE_END_TIME_HIGH_BITS,
								new UnsignedWordElement(0x2194)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.LAST_TIME_DISCHARGE_END_TIME_LOW_BITS,
								new UnsignedWordElement(0x2195)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.CELL_OVER_VOLTAGE_STOP_TIMES,
								new UnsignedWordElement(0x2196)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.BATTERY_OVER_VOLTAGE_STOP_TIMES,
								new UnsignedWordElement(0x2197)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.BATTERY_CHARGE_OVER_CURRENT_STOP_TIMES,
								new UnsignedWordElement(0x2198)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.CELL_VOLTAGE_LOW_STOP_TIMES,
								new UnsignedWordElement(0x2199)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.BATTERY_VOLTAGE_LOW_STOP_TIMES,
								new UnsignedWordElement(0x219A)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.BATTERY_DISCHARGE_OVER_CURRENT_STOP_TIMES,
								new UnsignedWordElement(0x219B)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.BATTERY_OVER_TEMPERATURE_STOP_TIMES,
								new UnsignedWordElement(0x219C)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.BATTERY_TEMPERATURE_LOW_STOP_TIMES,
								new UnsignedWordElement(0x219D)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.CELL_OVER_VOLTAGE_ALARM_TIMES,
								new UnsignedWordElement(0x219E)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.BATTERY_OVER_VOLTAGE_ALARM_TIMES,
								new UnsignedWordElement(0x219F)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.BATTERY_CHARGE_OVER_CURRENT_ALARM_TIMES,
								new UnsignedWordElement(0x21A0)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.CELL_VOLTAGE_LOW_ALARM_TIMES,
								new UnsignedWordElement(0x21A1)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.BATTERY_VOLTAGE_LOW_ALARM_TIMES,
								new UnsignedWordElement(0x21A2)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.BATTERY_DISCHARGE_OVER_CURRENT_ALARM_TIMES,
								new UnsignedWordElement(0x21A3)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.BATTERY_OVER_TEMPERATURE_ALARM_TIMES,
								new UnsignedWordElement(0x21A4)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.BATTERY_TEMPERATURE_LOW_ALARM_TIMES,
								new UnsignedWordElement(0x21A5)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.SYSTEM_SHORT_CIRCUIT_PROTECTION_TIMES,
								new UnsignedWordElement(0x21A6)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.SYSTEM_GR_OVER_TEMPERATURE_STOP_TIMES,
								new UnsignedWordElement(0x21A7)), //
						new DummyRegisterElement(0x21A8), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.SYSTEM_GR_OVER_TEMPERATURE_ALARM_TIMES,
								new UnsignedWordElement(0x21A9)), //
						new DummyRegisterElement(0x21AA), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.BATTERY_VOLTAGE_DIFFERENCE_ALARM_TIMES,
								new UnsignedWordElement(0x21AB)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.BATTERY_VOLTAGE_DIFFERENCE_STOP_TIMES,
								new UnsignedWordElement(0x21AC)), //
						new DummyRegisterElement(0x21AD, 0x21B3), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_TEMPERATURE_COMMUNICATION_ERROR_HIGH,
								new UnsignedWordElement(0x21B4)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_TEMPERATURE_COMMUNICATION_ERROR_LOW,
								new UnsignedWordElement(0x21B5))), //

				// Add tasks to read/write work and warn parameters

				// Stop parameter
				new FC16WriteRegistersTask(0x2040, //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_OVER_VOLTAGE_PROTECTION,
								new UnsignedWordElement(0x2040)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_OVER_VOLTAGE_RECOVER,
								new UnsignedWordElement(0x2041)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_PROTECTION,
								new UnsignedWordElement(0x2042), SCALE_FACTOR_2),
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER,
								new UnsignedWordElement(0x2043), SCALE_FACTOR_2), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION,
								new UnsignedWordElement(0x2044), SCALE_FACTOR_2), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_RECOVER,
								new UnsignedWordElement(0x2045), SCALE_FACTOR_2), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_UNDER_VOLTAGE_PROTECTION,
								new UnsignedWordElement(0x2046)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER,
								new UnsignedWordElement(0x2047)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_PROTECTION,
								new UnsignedWordElement(0x2048), SCALE_FACTOR_2), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER,
								new UnsignedWordElement(0x2049), SCALE_FACTOR_2), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION,
								new UnsignedWordElement(0x204A), SCALE_FACTOR_2), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER,
								new UnsignedWordElement(0x204B), SCALE_FACTOR_2), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_OVER_TEMPERATURE_PROTECTION,
								new SignedWordElement(0x204C)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_OVER_TEMPERATURE_RECOVER,
								new SignedWordElement(0x204D)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_UNDER_TEMPERATURE_PROTECTION,
								new SignedWordElement(0x204E)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_UNDER_TEMPERATURE_RECOVER,
								new SignedWordElement(0x204F)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION,
								new UnsignedWordElement(0x2050)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION_RECOVER,
								new UnsignedWordElement(0x2051)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SOC_HIGH_PROTECTION,
								new UnsignedWordElement(0x2052)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SOC_HIGH_PROTECTION_RECOVER,
								new UnsignedWordElement(0x2053)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_PROTECTION,
								new SignedWordElement(0x2054)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER,
								new SignedWordElement(0x2055)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_INSULATION_PROTECTION,
								new UnsignedWordElement(0x2056)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_INSULATION_PROTECTION_RECOVER,
								new UnsignedWordElement(0x2057)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_VOLTAGE_DIFFERENCE_PROTECTION,
								new UnsignedWordElement(0x2058)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER,
								new UnsignedWordElement(0x2059)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION,
								new UnsignedWordElement(0x205A), SCALE_FACTOR_2), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER,
								new UnsignedWordElement(0x205B), SCALE_FACTOR_2), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_PROTECTION,
								new SignedWordElement(0x205C)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_PROTECTION_RECOVER,
								new SignedWordElement(0x205D)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_LOW_PROTECTION,
								new SignedWordElement(0x205E)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_LOW_PROTECTION_RECOVER,
								new SignedWordElement(0x205F)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_TEMPERATURE_DIFFERENCE_PROTECTION,
								new SignedWordElement(0x2060)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_TEMPERATURE_DIFFERENCE_PROTECTION_RECOVER,
								new SignedWordElement(0x2061))), //

				// Warn parameter
				new FC16WriteRegistersTask(0x2080, //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_OVER_VOLTAGE_ALARM,
								new UnsignedWordElement(0x2080)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_OVER_VOLTAGE_RECOVER,
								new UnsignedWordElement(0x2081)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_ALARM,
								new UnsignedWordElement(0x2082), SCALE_FACTOR_2), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER,
								new UnsignedWordElement(0x2083), SCALE_FACTOR_2), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_ALARM,
								new UnsignedWordElement(0x2084), SCALE_FACTOR_2), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_RECOVER,
								new UnsignedWordElement(0x2085), SCALE_FACTOR_2), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_UNDER_VOLTAGE_ALARM,
								new UnsignedWordElement(0x2086)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER,
								new UnsignedWordElement(0x2087)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_ALARM,
								new UnsignedWordElement(0x2088), SCALE_FACTOR_2), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER,
								new UnsignedWordElement(0x2089), SCALE_FACTOR_2), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_ALARM,
								new UnsignedWordElement(0x208A), SCALE_FACTOR_2), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER,
								new UnsignedWordElement(0x208B), SCALE_FACTOR_2), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_OVER_TEMPERATURE_ALARM,
								new SignedWordElement(0x208C)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_OVER_TEMPERATURE_RECOVER,
								new SignedWordElement(0x208D)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_UNDER_TEMPERATURE_ALARM,
								new SignedWordElement(0x208E)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_UNDER_TEMPERATURE_RECOVER,
								new SignedWordElement(0x208F)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SOC_LOW_ALARM,
								new UnsignedWordElement(0x2090)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SOC_LOW_ALARM_RECOVER,
								new UnsignedWordElement(0x2091)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SOC_HIGH_ALARM,
								new UnsignedWordElement(0x2092)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SOC_HIGH_ALARM_RECOVER,
								new UnsignedWordElement(0x2093)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_ALARM,
								new SignedWordElement(0x2094)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_ALARM_RECOVER,
								new SignedWordElement(0x2095)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_INSULATION_ALARM,
								new UnsignedWordElement(0x2096)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_INSULATION_ALARM_RECOVER,
								new UnsignedWordElement(0x2097)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_VOLTAGE_DIFFERENCE_ALARM,
								new UnsignedWordElement(0x2098)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_VOLTAGE_DIFFERENCE_ALARM_RECOVER,
								new UnsignedWordElement(0x2099)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_ALARM,
								new UnsignedWordElement(0x209A), SCALE_FACTOR_2), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_ALARM_RECOVER,
								new UnsignedWordElement(0x209B), SCALE_FACTOR_2), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_ALARM,
								new SignedWordElement(0x209C)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_ALARM_RECOVER,
								new SignedWordElement(0x209D)), //
						new DummyRegisterElement(0x209E),
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_LOW_ALARM,
								new SignedWordElement(0x209F)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_LOW_ALARM_RECOVER,
								new SignedWordElement(0x20A0)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_TEMPERATURE_DIFFERENCE_ALARM,
								new SignedWordElement(0x20A1)), //
						m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_TEMPERATURE_DIFFERENCE_ALARM_RECOVER,
								new SignedWordElement(0x20A2))), //

				new FC6WriteRegisterTask(0x20DF,
						m(BatterySoltaroSingleRackVersionB.ChannelId.SET_SOC, new UnsignedWordElement(0x20DF))));

		if (!this.config.ReduceTasks()) {
			// Stop parameter
			protocol.addTask(new FC3ReadRegistersTask(0x2040, Priority.LOW, //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_OVER_VOLTAGE_PROTECTION,
							new UnsignedWordElement(0x2040)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_OVER_VOLTAGE_RECOVER,
							new UnsignedWordElement(0x2041)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_PROTECTION,
							new UnsignedWordElement(0x2042), SCALE_FACTOR_2),
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER,
							new UnsignedWordElement(0x2043), SCALE_FACTOR_2), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION,
							new UnsignedWordElement(0x2044), SCALE_FACTOR_2), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x2045), SCALE_FACTOR_2), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_UNDER_VOLTAGE_PROTECTION,
							new UnsignedWordElement(0x2046)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER,
							new UnsignedWordElement(0x2047)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_PROTECTION,
							new UnsignedWordElement(0x2048), SCALE_FACTOR_2), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER,
							new UnsignedWordElement(0x2049), SCALE_FACTOR_2), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION,
							new UnsignedWordElement(0x204A), SCALE_FACTOR_2), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x204B), SCALE_FACTOR_2), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_OVER_TEMPERATURE_PROTECTION,
							new SignedWordElement(0x204C)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_OVER_TEMPERATURE_RECOVER,
							new SignedWordElement(0x204D)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_UNDER_TEMPERATURE_PROTECTION,
							new SignedWordElement(0x204E)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_UNDER_TEMPERATURE_RECOVER,
							new SignedWordElement(0x204F)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION,
							new UnsignedWordElement(0x2050)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2051)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SOC_HIGH_PROTECTION,
							new UnsignedWordElement(0x2052)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_SOC_HIGH_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2053)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_PROTECTION,
							new SignedWordElement(0x2054)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER,
							new SignedWordElement(0x2055)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_INSULATION_PROTECTION,
							new UnsignedWordElement(0x2056)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_INSULATION_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2057)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_VOLTAGE_DIFFERENCE_PROTECTION,
							new UnsignedWordElement(0x2058)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2059)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION,
							new UnsignedWordElement(0x205A), SCALE_FACTOR_2), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x205B), SCALE_FACTOR_2), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_PROTECTION,
							new SignedWordElement(0x205C)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_PROTECTION_RECOVER,
							new SignedWordElement(0x205D)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_LOW_PROTECTION,
							new SignedWordElement(0x205E)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_LOW_PROTECTION_RECOVER,
							new SignedWordElement(0x205F)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_TEMPERATURE_DIFFERENCE_PROTECTION,
							new SignedWordElement(0x2060)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.STOP_PARAMETER_TEMPERATURE_DIFFERENCE_PROTECTION_RECOVER,
							new SignedWordElement(0x2061)))); //

			// Warn parameter
			protocol.addTask(new FC3ReadRegistersTask(0x2080, Priority.LOW, //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_OVER_VOLTAGE_ALARM,
							new UnsignedWordElement(0x2080)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_OVER_VOLTAGE_RECOVER,
							new UnsignedWordElement(0x2081)), //
					new DummyRegisterElement(0x2082),
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER,
							new UnsignedWordElement(0x2083), SCALE_FACTOR_2), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_ALARM,
							new UnsignedWordElement(0x2084), SCALE_FACTOR_2), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x2085), SCALE_FACTOR_2), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_UNDER_VOLTAGE_ALARM,
							new UnsignedWordElement(0x2086)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER,
							new UnsignedWordElement(0x2087)), //
					new DummyRegisterElement(0x2088),
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER,
							new UnsignedWordElement(0x2089), SCALE_FACTOR_2), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_ALARM,
							new UnsignedWordElement(0x208A), SCALE_FACTOR_2), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x208B), SCALE_FACTOR_2), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_OVER_TEMPERATURE_ALARM,
							new SignedWordElement(0x208C)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_OVER_TEMPERATURE_RECOVER,
							new SignedWordElement(0x208D)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_UNDER_TEMPERATURE_ALARM,
							new SignedWordElement(0x208E)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_UNDER_TEMPERATURE_RECOVER,
							new SignedWordElement(0x208F)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SOC_LOW_ALARM,
							new UnsignedWordElement(0x2090)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SOC_LOW_ALARM_RECOVER,
							new UnsignedWordElement(0x2091)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SOC_HIGH_ALARM,
							new UnsignedWordElement(0x2092)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_SOC_HIGH_ALARM_RECOVER,
							new UnsignedWordElement(0x2093)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_ALARM,
							new SignedWordElement(0x2094)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_ALARM_RECOVER,
							new SignedWordElement(0x2095)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_INSULATION_ALARM,
							new UnsignedWordElement(0x2096)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_INSULATION_ALARM_RECOVER,
							new UnsignedWordElement(0x2097)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_VOLTAGE_DIFFERENCE_ALARM,
							new UnsignedWordElement(0x2098)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_VOLTAGE_DIFFERENCE_ALARM_RECOVER,
							new UnsignedWordElement(0x2099)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_ALARM,
							new UnsignedWordElement(0x209A), SCALE_FACTOR_2), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_ALARM_RECOVER,
							new UnsignedWordElement(0x209B), SCALE_FACTOR_2), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_ALARM,
							new SignedWordElement(0x209C)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_ALARM_RECOVER,
							new SignedWordElement(0x209D)), //
					new DummyRegisterElement(0x209E),
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_LOW_ALARM,
							new SignedWordElement(0x209F)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_LOW_ALARM_RECOVER,
							new SignedWordElement(0x20A0)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_TEMPERATURE_DIFFERENCE_ALARM,
							new SignedWordElement(0x20A1)), //
					m(BatterySoltaroSingleRackVersionB.ChannelId.WARN_PARAMETER_TEMPERATURE_DIFFERENCE_ALARM_RECOVER,
							new SignedWordElement(0x20A2)))); //
		}

		return protocol;
	}

	/**
	 * Gets the Number of Modules.
	 *
	 * @return the Number of Modules as a {@link CompletableFuture}.
	 * @throws OpenemsException on error
	 */
	private CompletableFuture<Integer> getNumberOfModules() {
		final var result = new CompletableFuture<Integer>();
		try {
			ModbusUtils.readELementOnce(this.getModbusProtocol(), new UnsignedWordElement(0x20C1), true)
					.thenAccept(numberOfModules -> {
						if (numberOfModules == null) {
							return;
						}
						result.complete(numberOfModules);
					});
		} catch (OpenemsException e) {
			result.completeExceptionally(e);
		}
		return result;
	}

	/**
	 * Calculates the Capacity as Capacity per module multiplied with number of
	 * modules and sets the CAPACITY channel.
	 *
	 * @param numberOfModules the number of battery modules
	 */
	private void calculateCapacity(Integer numberOfModules) {
		var capacity = numberOfModules * this.config.moduleType().getCapacity_Wh();
		this._setCapacity(capacity);
	}

	private static final int ADDRESS_OFFSET = 0x2000;
	public static final int VOLTAGE_ADDRESS_OFFSET = ADDRESS_OFFSET + 0x800;
	public static final int TEMPERATURE_ADDRESS_OFFSET = ADDRESS_OFFSET + 0xC00;
	public static final int SENSORS_PER_MODULE = 12;

	/*
	 * Dynamically generate Channels and Modbus mappings for Cell-Temperatures and
	 * for Cell-Voltages. Channel-IDs are like "CLUSTER_1_BATTERY_001_VOLTAGE".
	 *
	 * @param numberOfModules the number of battery modules
	 */
	private void createDynamicChannels(int numberOfModules) {
		try {
			for (var i = 0; i < numberOfModules; i++) {
				var ameVolt = new ModbusElement<?>[SENSORS_PER_MODULE];
				var ameTemp = new ModbusElement<?>[SENSORS_PER_MODULE];
				for (var j = 0; j < SENSORS_PER_MODULE; j++) {
					var sensor = i * SENSORS_PER_MODULE + j;
					{
						// Create Voltage Channel
						var channelId = new ChannelIdImpl(
								"CLUSTER_1_BATTERY_" + String.format("%03d", sensor) + "_VOLTAGE",
								Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT));
						this.addChannel(channelId);
						// Create Modbus-Mapping for Voltages
						var uwe = new UnsignedWordElement(VOLTAGE_ADDRESS_OFFSET + sensor);
						ameVolt[j] = m(channelId, uwe);
					}
					{
						// Create Temperature Channel
						var channelId = new ChannelIdImpl(
								"CLUSTER_1_BATTERY_" + String.format("%03d", sensor) + "_TEMPERATURE",
								Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS));
						this.addChannel(channelId);
						// Create Modbus-Mapping for Temperatures
						var uwe = new UnsignedWordElement(TEMPERATURE_ADDRESS_OFFSET + sensor);
						ameTemp[j] = m(channelId, uwe);
					}
				}
				this.getModbusProtocol().addTasks(//
						new FC3ReadRegistersTask(VOLTAGE_ADDRESS_OFFSET + i * SENSORS_PER_MODULE, Priority.LOW,
								ameVolt), //
						new FC3ReadRegistersTask(TEMPERATURE_ADDRESS_OFFSET + i * SENSORS_PER_MODULE, Priority.LOW,
								ameTemp));
			}
		} catch (OpenemsException e) {
			e.printStackTrace();
		}
	}
}
