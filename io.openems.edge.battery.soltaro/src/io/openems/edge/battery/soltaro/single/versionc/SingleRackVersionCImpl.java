package io.openems.edge.battery.soltaro.single.versionc;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIRECT_1_TO_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.battery.soltaro.common.batteryprotection.BatteryProtectionDefinitionSoltaro3500Wh;
import io.openems.edge.battery.soltaro.common.enums.ModuleType;
import io.openems.edge.battery.soltaro.single.versionc.statemachine.Context;
import io.openems.edge.battery.soltaro.single.versionc.statemachine.StateMachine;
import io.openems.edge.battery.soltaro.single.versionc.statemachine.StateMachine.State;
import io.openems.edge.battery.soltaro.versionc.utils.CellChannelFactory;
import io.openems.edge.battery.soltaro.versionc.utils.CellChannelFactory.Type;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.IntegerWriteChannel;
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
		name = "Bms.Soltaro.SingleRack.VersionC", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class SingleRackVersionCImpl extends AbstractOpenemsModbusComponent implements SingleRackVersionC, Battery,
		ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave, StartStoppable {

	private final Logger log = LoggerFactory.getLogger(SingleRackVersionCImpl.class);

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager componentManager;

	/**
	 * Manages the {@link State}s of the StateMachine.
	 */
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);
	private static final int WATCHDOG = 60;

	private Config config;
	private BatteryProtection batteryProtection = null;

	public SingleRackVersionCImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				SingleRackVersionC.ChannelId.values(), //
				BatteryProtection.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		// Initialize Battery-Protection
		// Special settings for 3.5 kWh module
		this.batteryProtection = BatteryProtection.create(this) //
				.applyBatteryProtectionDefinition(new BatteryProtectionDefinitionSoltaro3500Wh(), this.componentManager) //
				.build();

		this.getNumberOfModules().thenAccept(numberOfModules -> {
			this.calculateCapacity(numberOfModules);
			this.createCellVoltageAndTemperatureChannels(numberOfModules);
		});

		// Set Watchdog Timeout
		IntegerWriteChannel c = this.channel(SingleRackVersionC.ChannelId.EMS_COMMUNICATION_TIMEOUT);
		c.setNextWriteValue(WATCHDOG);

	}

	/**
	 * Calculates the Capacity as Capacity per module multiplied with number of
	 * modules and sets the CAPACITY channel.
	 *
	 * @param numberOfModules the number of battery modules
	 */
	private void calculateCapacity(Integer numberOfModules) {
		var capacity = numberOfModules * ModuleType.MODULE_3_5_KWH.getCapacity_Wh();
		this._setCapacity(capacity);
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

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {

		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.batteryProtection.apply();
			break;

		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.handleStateMachine();
			break;
		}
	}

	/**
	 * Handles the State-Machine.
	 */
	private void handleStateMachine() {
		// Store the current State
		this.channel(SingleRackVersionC.ChannelId.STATE_MACHINE).setNextValue(this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		// Prepare Context
		var context = new Context(this, this.config);

		// Call the StateMachine
		try {
			this.stateMachine.run(context);

			this.channel(SingleRackVersionC.ChannelId.RUN_FAILED).setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(SingleRackVersionC.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc() //
				+ "|Discharge:" + this.getDischargeMinVoltage() + ";" + this.getDischargeMaxCurrent() //
				+ "|Charge:" + this.getChargeMaxVoltage() + ";" + this.getChargeMaxCurrent() //
				+ "|State:" + this.stateMachine.getCurrentState();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		var protocol = new ModbusProtocol(this, //
				new FC6WriteRegisterTask(0x2004, //
						m(SingleRackVersionC.ChannelId.SYSTEM_RESET, new UnsignedWordElement(0x2004)) //
				), //
				new FC6WriteRegisterTask(0x2010, //
						m(SingleRackVersionC.ChannelId.PRE_CHARGE_CONTROL, new UnsignedWordElement(0x2010)) //
				), //
				new FC6WriteRegisterTask(0x2014, //
						m(SingleRackVersionC.ChannelId.AUTO_SET_SLAVES_ID, new UnsignedWordElement(0x2014)) //
				), //
				new FC6WriteRegisterTask(0x2019, //
						m(SingleRackVersionC.ChannelId.AUTO_SET_SLAVES_TEMPERATURE_ID, new UnsignedWordElement(0x2019)) //
				), //
				new FC6WriteRegisterTask(0x201D, //
						m(SingleRackVersionC.ChannelId.SLEEP, new UnsignedWordElement(0x201D)) //
				), //
				new FC16WriteRegistersTask(0x200B, //
						m(SingleRackVersionC.ChannelId.EMS_ADDRESS, new UnsignedWordElement(0x200B)), //
						m(SingleRackVersionC.ChannelId.EMS_BAUDRATE, new UnsignedWordElement(0x200C)) //
				), //
				new FC6WriteRegisterTask(0x20C1, //
						m(SingleRackVersionC.ChannelId.NUMBER_OF_MODULES_PER_TOWER, new UnsignedWordElement(0x20C1)) //
				), //
				new FC6WriteRegisterTask(0x20F4,
						m(SingleRackVersionC.ChannelId.EMS_COMMUNICATION_TIMEOUT, new UnsignedWordElement(0x20F4)) //
				), //
				new FC6WriteRegisterTask(0x20CC, //
						m(SingleRackVersionC.ChannelId.SYSTEM_TOTAL_CAPACITY, new UnsignedWordElement(0x20CC)) //
				), //
				new FC6WriteRegisterTask(0x2015, //
						m(SingleRackVersionC.ChannelId.SET_SUB_MASTER_ADDRESS, new UnsignedWordElement(0x2015)) //
				), //
				new FC6WriteRegisterTask(0x20F3, //
						m(SingleRackVersionC.ChannelId.VOLTAGE_LOW_PROTECTION, new UnsignedWordElement(0x20F3)) //
				), //

				new FC3ReadRegistersTask(0x200B, Priority.LOW, //
						m(SingleRackVersionC.ChannelId.EMS_ADDRESS, new UnsignedWordElement(0x200B)), //
						m(SingleRackVersionC.ChannelId.EMS_BAUDRATE, new UnsignedWordElement(0x200C)), //
						new DummyRegisterElement(0x200D, 0x200F),
						m(SingleRackVersionC.ChannelId.PRE_CHARGE_CONTROL, new UnsignedWordElement(0x2010)), //
						new DummyRegisterElement(0x2011, 0x2013),
						m(SingleRackVersionC.ChannelId.AUTO_SET_SLAVES_ID, new UnsignedWordElement(0x2014)), //
						m(SingleRackVersionC.ChannelId.SET_SUB_MASTER_ADDRESS, new UnsignedWordElement(0x2015)), //
						new DummyRegisterElement(0x2016, 0x2018),
						m(SingleRackVersionC.ChannelId.AUTO_SET_SLAVES_TEMPERATURE_ID, new UnsignedWordElement(0x2019)), //
						new DummyRegisterElement(0x201A, 0x201C),
						m(SingleRackVersionC.ChannelId.SLEEP, new UnsignedWordElement(0x201D)) //
				), //
				new FC3ReadRegistersTask(0x20C1, Priority.LOW, //
						m(SingleRackVersionC.ChannelId.NUMBER_OF_MODULES_PER_TOWER, new UnsignedWordElement(0x20C1)), //
						new DummyRegisterElement(0x20C2, 0x20CB),
						m(SingleRackVersionC.ChannelId.SYSTEM_TOTAL_CAPACITY, new UnsignedWordElement(0x20CC)) //
				), //
				new FC3ReadRegistersTask(0x20F3, Priority.LOW, //
						m(SingleRackVersionC.ChannelId.VOLTAGE_LOW_PROTECTION, new UnsignedWordElement(0x20F3)), //
						m(SingleRackVersionC.ChannelId.EMS_COMMUNICATION_TIMEOUT, new UnsignedWordElement(0x20F4)) //
				), //

				// Single Cluster Running Status Registers
				new FC3ReadRegistersTask(0x2100, Priority.HIGH, //
						m(new UnsignedWordElement(0x2100)) //
								.m(SingleRackVersionC.ChannelId.CLUSTER_1_VOLTAGE, SCALE_FACTOR_2) // [mV]
								.m(Battery.ChannelId.VOLTAGE, SCALE_FACTOR_MINUS_1) // [V]
								.build(), //
						m(new UnsignedWordElement(0x2101)) //
								.m(SingleRackVersionC.ChannelId.CLUSTER_1_CURRENT, SCALE_FACTOR_2) // [mA]
								.m(Battery.ChannelId.CURRENT, SCALE_FACTOR_MINUS_1) // [A]
								.build(), //
						m(SingleRackVersionC.ChannelId.CHARGE_INDICATION, new UnsignedWordElement(0x2102)),
						m(Battery.ChannelId.SOC, new UnsignedWordElement(0x2103)), m(new UnsignedWordElement(0x2104)) //
								.m(SingleRackVersionC.ChannelId.CLUSTER_1_SOH, DIRECT_1_TO_1) // [%]
								.m(Battery.ChannelId.SOH, DIRECT_1_TO_1) // [%]
								.build(), //
						m(SingleRackVersionC.ChannelId.CLUSTER_1_MAX_CELL_VOLTAGE_ID, new UnsignedWordElement(0x2105)), //
						m(new UnsignedWordElement(0x2106)) //
								.m(SingleRackVersionC.ChannelId.CLUSTER_1_MAX_CELL_VOLTAGE, DIRECT_1_TO_1) //
								.m(Battery.ChannelId.MAX_CELL_VOLTAGE, DIRECT_1_TO_1) //
								.build(), //
						m(SingleRackVersionC.ChannelId.CLUSTER_1_MIN_CELL_VOLTAGE_ID, new UnsignedWordElement(0x2107)), //
						m(new UnsignedWordElement(0x2108)) //
								.m(SingleRackVersionC.ChannelId.CLUSTER_1_MIN_CELL_VOLTAGE, DIRECT_1_TO_1) //
								.m(Battery.ChannelId.MIN_CELL_VOLTAGE, DIRECT_1_TO_1) //
								.build(), //
						m(SingleRackVersionC.ChannelId.CLUSTER_1_MAX_CELL_TEMPERATURE_ID,
								new UnsignedWordElement(0x2109)), //
						m(new SignedWordElement(0x210A)) //
								.m(SingleRackVersionC.ChannelId.CLUSTER_1_MAX_CELL_TEMPERATURE, DIRECT_1_TO_1) //
								.m(Battery.ChannelId.MAX_CELL_TEMPERATURE, SCALE_FACTOR_MINUS_1) //
								.build(), //
						m(SingleRackVersionC.ChannelId.CLUSTER_1_MIN_CELL_TEMPERATURE_ID,
								new UnsignedWordElement(0x210B)), //
						m(new SignedWordElement(0x210C)) //
								.m(SingleRackVersionC.ChannelId.CLUSTER_1_MIN_CELL_TEMPERATURE, DIRECT_1_TO_1) //
								.m(Battery.ChannelId.MIN_CELL_TEMPERATURE, SCALE_FACTOR_MINUS_1) //
								.build(), //
						m(SingleRackVersionC.ChannelId.CLUSTER_1_AVERAGE_VOLTAGE, new UnsignedWordElement(0x210D)), //
						m(SingleRackVersionC.ChannelId.CLUSTER_1_SYSTEM_INSULATION, new UnsignedWordElement(0x210E)), //
						m(BatteryProtection.ChannelId.BP_CHARGE_BMS, new UnsignedWordElement(0x210F),
								SCALE_FACTOR_MINUS_1), //
						m(BatteryProtection.ChannelId.BP_DISCHARGE_BMS, new UnsignedWordElement(0x2110),
								SCALE_FACTOR_MINUS_1), //
						m(SingleRackVersionC.ChannelId.POSITIVE_INSULATION, new UnsignedWordElement(0x2111)), //
						m(SingleRackVersionC.ChannelId.NEGATIVE_INSULATION, new UnsignedWordElement(0x2112)), //
						m(SingleRackVersionC.ChannelId.CLUSTER_RUN_STATE, new UnsignedWordElement(0x2113)), //
						m(SingleRackVersionC.ChannelId.CLUSTER_1_AVG_TEMPERATURE, new UnsignedWordElement(0x2114)) //
				), //
				new FC3ReadRegistersTask(0x218b, Priority.LOW,
						m(SingleRackVersionC.ChannelId.CLUSTER_1_PROJECT_ID, new UnsignedWordElement(0x218b)), //
						m(SingleRackVersionC.ChannelId.CLUSTER_1_VERSION_MAJOR, new UnsignedWordElement(0x218c)), //
						m(SingleRackVersionC.ChannelId.CLUSTER_1_VERSION_SUB, new UnsignedWordElement(0x218d)), //
						m(SingleRackVersionC.ChannelId.CLUSTER_1_VERSION_MODIFY, new UnsignedWordElement(0x218e)) //
				), //

				// System Warning/Shut Down Status Registers
				new FC3ReadRegistersTask(0x2140, Priority.LOW, //
						// Level 2 Alarm: BMS Self-protect, main contactor shut down
						m(new BitsWordElement(0x2140, this) //
								.bit(0, SingleRackVersionC.ChannelId.LEVEL2_CELL_VOLTAGE_HIGH) //
								.bit(1, SingleRackVersionC.ChannelId.LEVEL2_TOTAL_VOLTAGE_HIGH) //
								.bit(2, SingleRackVersionC.ChannelId.LEVEL2_CHARGE_CURRENT_HIGH) //
								.bit(3, SingleRackVersionC.ChannelId.LEVEL2_CELL_VOLTAGE_LOW) //
								.bit(4, SingleRackVersionC.ChannelId.LEVEL2_TOTAL_VOLTAGE_LOW) //
								.bit(5, SingleRackVersionC.ChannelId.LEVEL2_DISCHARGE_CURRENT_HIGH) //
								.bit(6, SingleRackVersionC.ChannelId.LEVEL2_CHARGE_TEMP_HIGH) //
								.bit(7, SingleRackVersionC.ChannelId.LEVEL2_CHARGE_TEMP_LOW) //
								// 8 -> Reserved
								// 9 -> Reserved
								.bit(10, SingleRackVersionC.ChannelId.LEVEL2_POWER_POLE_TEMP_HIGH) //
								// 11 -> Reserved
								.bit(12, SingleRackVersionC.ChannelId.LEVEL2_INSULATION_VALUE) //
								// 13 -> Reserved
								.bit(14, SingleRackVersionC.ChannelId.LEVEL2_DISCHARGE_TEMP_HIGH) //
								.bit(15, SingleRackVersionC.ChannelId.LEVEL2_DISCHARGE_TEMP_LOW) //
						), //

						// Level 1 Alarm: EMS Control to stop charge, discharge, charge&discharge
						m(new BitsWordElement(0x2141, this) //
								.bit(0, SingleRackVersionC.ChannelId.LEVEL1_CELL_VOLTAGE_HIGH) //
								.bit(1, SingleRackVersionC.ChannelId.LEVEL1_TOTAL_VOLTAGE_HIGH) //
								.bit(2, SingleRackVersionC.ChannelId.LEVEL1_CHARGE_CURRENT_HIGH) //
								.bit(3, SingleRackVersionC.ChannelId.LEVEL1_CELL_VOLTAGE_LOW) //
								.bit(4, SingleRackVersionC.ChannelId.LEVEL1_TOTAL_VOLTAGE_LOW) //
								.bit(5, SingleRackVersionC.ChannelId.LEVEL1_DISCHARGE_CURRENT_HIGH) //
								.bit(6, SingleRackVersionC.ChannelId.LEVEL1_CHARGE_TEMP_HIGH) //
								.bit(7, SingleRackVersionC.ChannelId.LEVEL1_CHARGE_TEMP_LOW) //
								// 8 -> Reserved
								// 9 -> Reserved
								.bit(10, SingleRackVersionC.ChannelId.LEVEL1_POWER_POLE_TEMP_HIGH) //
								// 11 -> Reserved
								.bit(12, SingleRackVersionC.ChannelId.LEVEL1_INSULATION_VALUE) //
								// 13 -> Reserved
								.bit(14, SingleRackVersionC.ChannelId.LEVEL1_DISCHARGE_TEMP_HIGH) //
								.bit(15, SingleRackVersionC.ChannelId.LEVEL1_DISCHARGE_TEMP_LOW) //
						), //

						// Pre-Alarm: Temperature Alarm will active current limitation
						m(new BitsWordElement(0x2142, this) //
								// Removed Warning/Info as this is properly covered by Battery Protection
								// .bit(0, SingleRackVersionC.ChannelId.PRE_ALARM_CELL_VOLTAGE_HIGH) //
								// .bit(1, SingleRackVersionC.ChannelId.PRE_ALARM_TOTAL_VOLTAGE_HIGH) //
								.bit(2, SingleRackVersionC.ChannelId.PRE_ALARM_CHARGE_CURRENT_HIGH) //
								// .bit(3, SingleRackVersionC.ChannelId.PRE_ALARM_CELL_VOLTAGE_LOW) //
								.bit(4, SingleRackVersionC.ChannelId.PRE_ALARM_TOTAL_VOLTAGE_LOW) //
								.bit(5, SingleRackVersionC.ChannelId.PRE_ALARM_DISCHARGE_CURRENT_HIGH) //
								.bit(6, SingleRackVersionC.ChannelId.PRE_ALARM_CHARGE_TEMP_HIGH) //
								.bit(7, SingleRackVersionC.ChannelId.PRE_ALARM_CHARGE_TEMP_LOW) //
								.bit(8, SingleRackVersionC.ChannelId.PRE_ALARM_SOC_LOW) //
								.bit(10, SingleRackVersionC.ChannelId.PRE_ALARM_POWER_POLE_HIGH) //
								.bit(11, SingleRackVersionC.ChannelId.PRE_ALARM_CELL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(12, SingleRackVersionC.ChannelId.PRE_ALARM_INSULATION_FAIL) //
								.bit(13, SingleRackVersionC.ChannelId.PRE_ALARM_TOTAL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(14, SingleRackVersionC.ChannelId.PRE_ALARM_DISCHARGE_TEMP_HIGH) //
								.bit(15, SingleRackVersionC.ChannelId.PRE_ALARM_DISCHARGE_TEMP_LOW) //
						) //
				), //

				// Other Alarm Info
				new FC3ReadRegistersTask(0x21A5, Priority.LOW, //
						m(new BitsWordElement(0x21A5, this) //
								.bit(0, SingleRackVersionC.ChannelId.ALARM_COMMUNICATION_TO_MASTER_BMS) //
								.bit(1, SingleRackVersionC.ChannelId.ALARM_COMMUNICATION_TO_SLAVE_BMS) //
								.bit(2, SingleRackVersionC.ChannelId.ALARM_COMMUNICATION_SLAVE_BMS_TO_TEMP_SENSORS) //
								.bit(3, SingleRackVersionC.ChannelId.ALARM_SLAVE_BMS_HARDWARE) //
						)), //

				// Slave BMS Fault Message Registers
				new FC3ReadRegistersTask(0x2180, Priority.LOW, //
						m(SingleRackVersionC.ChannelId.CYCLE_TIME, new UnsignedWordElement(0x2180)), //
						// TODO to be checked, was high + low bits
						m(SingleRackVersionC.ChannelId.TOTAL_CAPACITY, new UnsignedDoublewordElement(0x2181)),
						m(new BitsWordElement(0x2183, this) //
								.bit(3, SingleRackVersionC.ChannelId.SLAVE_20_COMMUNICATION_ERROR)//
								.bit(2, SingleRackVersionC.ChannelId.SLAVE_19_COMMUNICATION_ERROR)//
								.bit(1, SingleRackVersionC.ChannelId.SLAVE_18_COMMUNICATION_ERROR)//
								.bit(0, SingleRackVersionC.ChannelId.SLAVE_17_COMMUNICATION_ERROR)//
						), //
						m(new BitsWordElement(0x2184, this) //
								.bit(15, SingleRackVersionC.ChannelId.SLAVE_16_COMMUNICATION_ERROR)//
								.bit(14, SingleRackVersionC.ChannelId.SLAVE_15_COMMUNICATION_ERROR)//
								.bit(13, SingleRackVersionC.ChannelId.SLAVE_14_COMMUNICATION_ERROR)//
								.bit(12, SingleRackVersionC.ChannelId.SLAVE_13_COMMUNICATION_ERROR)//
								.bit(11, SingleRackVersionC.ChannelId.SLAVE_12_COMMUNICATION_ERROR)//
								.bit(10, SingleRackVersionC.ChannelId.SLAVE_11_COMMUNICATION_ERROR)//
								.bit(9, SingleRackVersionC.ChannelId.SLAVE_10_COMMUNICATION_ERROR)//
								.bit(8, SingleRackVersionC.ChannelId.SLAVE_9_COMMUNICATION_ERROR)//
								.bit(7, SingleRackVersionC.ChannelId.SLAVE_8_COMMUNICATION_ERROR)//
								.bit(6, SingleRackVersionC.ChannelId.SLAVE_7_COMMUNICATION_ERROR)//
								.bit(5, SingleRackVersionC.ChannelId.SLAVE_6_COMMUNICATION_ERROR)//
								.bit(4, SingleRackVersionC.ChannelId.SLAVE_5_COMMUNICATION_ERROR)//
								.bit(3, SingleRackVersionC.ChannelId.SLAVE_4_COMMUNICATION_ERROR)//
								.bit(2, SingleRackVersionC.ChannelId.SLAVE_3_COMMUNICATION_ERROR)//
								.bit(1, SingleRackVersionC.ChannelId.SLAVE_2_COMMUNICATION_ERROR)//
								.bit(0, SingleRackVersionC.ChannelId.SLAVE_1_COMMUNICATION_ERROR)//
						), //
						m(new BitsWordElement(0x2185, this) //
								.bit(0, SingleRackVersionC.ChannelId.SLAVE_BMS_VOLTAGE_SENSOR_CABLES)//
								.bit(1, SingleRackVersionC.ChannelId.SLAVE_BMS_POWER_CABLE)//
								.bit(2, SingleRackVersionC.ChannelId.SLAVE_BMS_LTC6803)//
								.bit(3, SingleRackVersionC.ChannelId.SLAVE_BMS_VOLTAGE_SENSORS)//
								.bit(4, SingleRackVersionC.ChannelId.SLAVE_BMS_TEMP_SENSOR_CABLES)//
								.bit(5, SingleRackVersionC.ChannelId.SLAVE_BMS_TEMP_SENSORS)//
								.bit(6, SingleRackVersionC.ChannelId.SLAVE_BMS_POWER_POLE_TEMP_SENSOR)//
								.bit(7, SingleRackVersionC.ChannelId.SLAVE_BMS_TEMP_BOARD_COM)//
								.bit(8, SingleRackVersionC.ChannelId.SLAVE_BMS_BALANCE_MODULE)//
								.bit(9, SingleRackVersionC.ChannelId.SLAVE_BMS_TEMP_SENSORS2)//
								.bit(10, SingleRackVersionC.ChannelId.SLAVE_BMS_INTERNAL_COM)//
								.bit(11, SingleRackVersionC.ChannelId.SLAVE_BMS_EEPROM)//
								.bit(12, SingleRackVersionC.ChannelId.SLAVE_BMS_INIT)//
						))); //
		{
			AbstractModbusElement<?>[] elements = {
					m(SingleRackVersionC.ChannelId.PRE_ALARM_CELL_OVER_VOLTAGE_ALARM, new UnsignedWordElement(0x2080)), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_CELL_OVER_VOLTAGE_RECOVER,
							new UnsignedWordElement(0x2081)), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_SYSTEM_OVER_VOLTAGE_ALARM, new UnsignedWordElement(0x2082),
							SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_SYSTEM_OVER_VOLTAGE_RECOVER,
							new UnsignedWordElement(0x2083), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_SYSTEM_CHARGE_OVER_CURRENT_ALARM,
							new UnsignedWordElement(0x2084), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_SYSTEM_CHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x2085), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_CELL_UNDER_VOLTAGE_ALARM, new UnsignedWordElement(0x2086)), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_CELL_UNDER_VOLTAGE_RECOVER,
							new UnsignedWordElement(0x2087)), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_SYSTEM_UNDER_VOLTAGE_ALARM,
							new UnsignedWordElement(0x2088), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_SYSTEM_UNDER_VOLTAGE_RECOVER,
							new UnsignedWordElement(0x2089), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_SYSTEM_DISCHARGE_OVER_CURRENT_ALARM,
							new UnsignedWordElement(0x208A), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x208B), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_CELL_OVER_TEMPERATURE_ALARM,
							new SignedWordElement(0x208C)), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_CELL_OVER_TEMPERATURE_RECOVER,
							new SignedWordElement(0x208D)), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_CELL_UNDER_TEMPERATURE_ALARM,
							new SignedWordElement(0x208E)), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_CELL_UNDER_TEMPERATURE_RECOVER,
							new SignedWordElement(0x208F)), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_SOC_LOW_ALARM, new UnsignedWordElement(0x2090)), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_SOC_LOW_ALARM_RECOVER, new UnsignedWordElement(0x2091)), //
					new DummyRegisterElement(0x2092, 0x2093),
					m(SingleRackVersionC.ChannelId.PRE_ALARM_CONNECTOR_TEMPERATURE_HIGH_ALARM,
							new SignedWordElement(0x2094)), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_CONNECTOR_TEMPERATURE_HIGH_ALARM_RECOVER,
							new SignedWordElement(0x2095)), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_INSULATION_ALARM, new UnsignedWordElement(0x2096)), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_INSULATION_ALARM_RECOVER, new UnsignedWordElement(0x2097)), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_CELL_VOLTAGE_DIFFERENCE_ALARM,
							new UnsignedWordElement(0x2098)), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_CELL_VOLTAGE_DIFFERENCE_ALARM_RECOVER,
							new UnsignedWordElement(0x2099)), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_TOTAL_VOLTAGE_DIFFERENCE_ALARM,
							new UnsignedWordElement(0x209A), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_TOTAL_VOLTAGE_DIFFERENCE_ALARM_RECOVER,
							new UnsignedWordElement(0x209B), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_DISCHARGE_TEMPERATURE_HIGH_ALARM,
							new SignedWordElement(0x209C)), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_DISCHARGE_TEMPERATURE_HIGH_ALARM_RECOVER,
							new SignedWordElement(0x209D)), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_DISCHARGE_TEMPERATURE_LOW_ALARM,
							new SignedWordElement(0x209E)), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_DISCHARGE_TEMPERATURE_LOW_ALARM_RECOVER,
							new SignedWordElement(0x209F)), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_TEMPERATURE_DIFFERENCE_ALARM,
							new SignedWordElement(0x20A0)), //
					m(SingleRackVersionC.ChannelId.PRE_ALARM_TEMPERATURE_DIFFERENCE_ALARM_RECOVER,
							new SignedWordElement(0x20A1)) //
			};
			protocol.addTask(new FC16WriteRegistersTask(0x2080, elements));
			protocol.addTask(new FC3ReadRegistersTask(0x2080, Priority.LOW, elements));
		}

		// WARN_LEVEL1 (Level1 warning registers RW)
		{
			AbstractModbusElement<?>[] elements = {
					m(SingleRackVersionC.ChannelId.LEVEL1_CELL_OVER_VOLTAGE_PROTECTION,
							new UnsignedWordElement(0x2040)), //
					m(SingleRackVersionC.ChannelId.LEVEL1_CELL_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2041)), //
					m(SingleRackVersionC.ChannelId.LEVEL1_SYSTEM_OVER_VOLTAGE_PROTECTION,
							new UnsignedWordElement(0x2042), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.LEVEL1_SYSTEM_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2043),
							SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.LEVEL1_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION,
							new UnsignedWordElement(0x2044), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.LEVEL1_SYSTEM_CHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x2045), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.LEVEL1_CELL_UNDER_VOLTAGE_PROTECTION,
							new UnsignedWordElement(0x2046)), //
					m(SingleRackVersionC.ChannelId.LEVEL1_CELL_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2047)), //
					m(SingleRackVersionC.ChannelId.LEVEL1_SYSTEM_UNDER_VOLTAGE_PROTECTION,
							new UnsignedWordElement(0x2048), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.LEVEL1_SYSTEM_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2049),
							SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.LEVEL1_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION,
							new UnsignedWordElement(0x204A), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.LEVEL1_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x204B), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.LEVEL1_CELL_OVER_TEMPERATURE_PROTECTION,
							new SignedWordElement(0x204C)), //
					m(SingleRackVersionC.ChannelId.LEVEL1_CELL_OVER_TEMPERATURE_RECOVER, new SignedWordElement(0x204D)), //
					m(SingleRackVersionC.ChannelId.LEVEL1_CELL_UNDER_TEMPERATURE_PROTECTION,
							new SignedWordElement(0x204E)), //
					m(SingleRackVersionC.ChannelId.LEVEL1_CELL_UNDER_TEMPERATURE_RECOVER,
							new SignedWordElement(0x204F)), //
					m(SingleRackVersionC.ChannelId.LEVEL1_SOC_LOW_PROTECTION, new UnsignedWordElement(0x2050)), //
					m(SingleRackVersionC.ChannelId.LEVEL1_SOC_LOW_PROTECTION_RECOVER, new UnsignedWordElement(0x2051)), //
					new DummyRegisterElement(0x2052, 0x2053), //
					m(SingleRackVersionC.ChannelId.LEVEL1_CONNECTOR_TEMPERATURE_HIGH_PROTECTION,
							new SignedWordElement(0x2054)), //
					m(SingleRackVersionC.ChannelId.LEVEL1_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER,
							new SignedWordElement(0x2055)), //
					m(SingleRackVersionC.ChannelId.LEVEL1_INSULATION_PROTECTION, new UnsignedWordElement(0x2056)), //
					m(SingleRackVersionC.ChannelId.LEVEL1_INSULATION_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2057)), //
					m(SingleRackVersionC.ChannelId.LEVEL1_CELL_VOLTAGE_DIFFERENCE_PROTECTION,
							new UnsignedWordElement(0x2058)), //
					m(SingleRackVersionC.ChannelId.LEVEL1_CELL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2059)), //
					m(SingleRackVersionC.ChannelId.LEVEL1_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION,
							new UnsignedWordElement(0x205A), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.LEVEL1_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x205B), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.LEVEL1_DISCHARGE_TEMPERATURE_HIGH_PROTECTION,
							new SignedWordElement(0x205C)), //
					m(SingleRackVersionC.ChannelId.LEVEL1_DISCHARGE_TEMPERATURE_HIGH_PROTECTION_RECOVER,
							new SignedWordElement(0x205D)), //
					m(SingleRackVersionC.ChannelId.LEVEL1_DISCHARGE_TEMPERATURE_LOW_PROTECTION,
							new SignedWordElement(0x205E)), //
					m(SingleRackVersionC.ChannelId.LEVEL1_DISCHARGE_TEMPERATURE_LOW_PROTECTION_RECOVER,
							new SignedWordElement(0x205F)), //
					m(SingleRackVersionC.ChannelId.LEVEL1_TEMPERATURE_DIFFERENCE_PROTECTION,
							new SignedWordElement(0x2060)), //
					m(SingleRackVersionC.ChannelId.LEVEL1_TEMPERATURE_DIFFERENCE_PROTECTION_RECOVER,
							new SignedWordElement(0x2061)) //
			};
			protocol.addTask(new FC16WriteRegistersTask(0x2040, elements));
			protocol.addTask(new FC3ReadRegistersTask(0x2040, Priority.LOW, elements));
		}

		// WARN_LEVEL2 (Level2 Protection registers RW)
		{
			AbstractModbusElement<?>[] elements = {
					m(SingleRackVersionC.ChannelId.LEVEL2_CELL_OVER_VOLTAGE_PROTECTION,
							new UnsignedWordElement(0x2400)), //
					m(SingleRackVersionC.ChannelId.LEVEL2_CELL_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2401)), //
					m(new UnsignedWordElement(0x2402)) //
							.m(SingleRackVersionC.ChannelId.LEVEL2_SYSTEM_OVER_VOLTAGE_PROTECTION, SCALE_FACTOR_2) // [mV]
							.m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, SCALE_FACTOR_MINUS_1) // [V]
							.build(), //
					m(SingleRackVersionC.ChannelId.LEVEL2_SYSTEM_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2403),
							SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.LEVEL2_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION,
							new UnsignedWordElement(0x2404), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.LEVEL2_SYSTEM_CHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x2405), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.LEVEL2_CELL_UNDER_VOLTAGE_PROTECTION,
							new UnsignedWordElement(0x2406)), //
					m(SingleRackVersionC.ChannelId.LEVEL2_CELL_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2407)), //
					m(new UnsignedWordElement(0x2408)) //
							.m(SingleRackVersionC.ChannelId.LEVEL2_SYSTEM_UNDER_VOLTAGE_PROTECTION, SCALE_FACTOR_2) // [mV]
							.m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, SCALE_FACTOR_MINUS_1) // [V]
							.build(), //
					m(SingleRackVersionC.ChannelId.LEVEL2_SYSTEM_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2409),
							SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.LEVEL2_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION,
							new UnsignedWordElement(0x240A), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.LEVEL2_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x240B), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.LEVEL2_CELL_OVER_TEMPERATURE_PROTECTION,
							new SignedWordElement(0x240C)), //
					m(SingleRackVersionC.ChannelId.LEVEL2_CELL_OVER_TEMPERATURE_RECOVER, new SignedWordElement(0x240D)), //
					m(SingleRackVersionC.ChannelId.LEVEL2_CELL_UNDER_TEMPERATURE_PROTECTION,
							new SignedWordElement(0x240E)), //
					m(SingleRackVersionC.ChannelId.LEVEL2_CELL_UNDER_TEMPERATURE_RECOVER,
							new SignedWordElement(0x240F)), //
					m(SingleRackVersionC.ChannelId.LEVEL2_SOC_LOW_PROTECTION, new UnsignedWordElement(0x2410)), //
					m(SingleRackVersionC.ChannelId.LEVEL2_SOC_LOW_PROTECTION_RECOVER, new UnsignedWordElement(0x2411)), //
					new DummyRegisterElement(0x2412, 0x2413), //
					m(SingleRackVersionC.ChannelId.LEVEL2_CONNECTOR_TEMPERATURE_HIGH_PROTECTION,
							new SignedWordElement(0x2414)), //
					m(SingleRackVersionC.ChannelId.LEVEL2_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER,
							new SignedWordElement(0x2415)), //
					m(SingleRackVersionC.ChannelId.LEVEL2_INSULATION_PROTECTION, new UnsignedWordElement(0x2416)), //
					m(SingleRackVersionC.ChannelId.LEVEL2_INSULATION_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2417)), //
					m(SingleRackVersionC.ChannelId.LEVEL2_CELL_VOLTAGE_DIFFERENCE_PROTECTION,
							new UnsignedWordElement(0x2418)), //
					m(SingleRackVersionC.ChannelId.LEVEL2_CELL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2419)), //
					m(SingleRackVersionC.ChannelId.LEVEL2_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION,
							new UnsignedWordElement(0x241A), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.LEVEL2_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x241B), SCALE_FACTOR_2), //
					m(SingleRackVersionC.ChannelId.LEVEL2_DISCHARGE_TEMPERATURE_HIGH_PROTECTION,
							new SignedWordElement(0x241C)), //
					m(SingleRackVersionC.ChannelId.LEVEL2_DISCHARGE_TEMPERATURE_HIGH_PROTECTION_RECOVER,
							new SignedWordElement(0x241D)), //
					m(SingleRackVersionC.ChannelId.LEVEL2_DISCHARGE_TEMPERATURE_LOW_PROTECTION,
							new SignedWordElement(0x241E)), //
					m(SingleRackVersionC.ChannelId.LEVEL2_DISCHARGE_TEMPERATURE_LOW_PROTECTION_RECOVER,
							new SignedWordElement(0x241F)), //
					m(SingleRackVersionC.ChannelId.LEVEL2_TEMPERATURE_DIFFERENCE_PROTECTION,
							new SignedWordElement(0x2420)), //
					m(SingleRackVersionC.ChannelId.LEVEL2_TEMPERATURE_DIFFERENCE_PROTECTION_RECOVER,
							new SignedWordElement(0x2421)) //
			};
			protocol.addTask(new FC16WriteRegistersTask(0x2400, elements));
			protocol.addTask(new FC3ReadRegistersTask(0x2400, Priority.LOW, elements));
		}

		return protocol;
	}

	void createCellVoltageAndTemperatureChannels(int numberOfModules) {
		/*
		 * Add tasks for cell voltages and temperatures according to the number of
		 * slaves, one task per module is created Cell voltages
		 */
		Consumer<CellChannelFactory.Type> addCellChannels = type -> {
			for (var i = 0; i < numberOfModules; i++) {
				var elements = new AbstractModbusElement<?>[type.getSensorsPerModule()];
				for (var j = 0; j < type.getSensorsPerModule(); j++) {
					var sensorIndex = i * type.getSensorsPerModule() + j;
					var channelId = CellChannelFactory.create(type, sensorIndex);
					// Register the Channel at this Component
					this.addChannel(channelId);
					// Add the Modbus Element and map it to the Channel
					if (type == Type.VOLTAGE_SINGLE) {
						elements[j] = m(channelId, new UnsignedWordElement(type.getOffset() + sensorIndex));
					} else {
						elements[j] = m(channelId, new SignedWordElement(type.getOffset() + sensorIndex));
					}

				}
				// Add a Modbus read task for this module
				var startAddress = type.getOffset() + i * type.getSensorsPerModule();
				try {
					this.getModbusProtocol().addTask(//
							new FC3ReadRegistersTask(startAddress, Priority.LOW, elements));
				} catch (OpenemsException e) {
					this.logWarn(this.log, "Error while adding Modbus task for slave [" + i + "] starting at ["
							+ startAddress + "]: " + e.getMessage());
					e.printStackTrace();
				}
			}
		};
		addCellChannels.accept(CellChannelFactory.Type.VOLTAGE_SINGLE);
		addCellChannels.accept(CellChannelFactory.Type.TEMPERATURE_SINGLE);

	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode) //
		);
	}

	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(StartStop.UNDEFINED);

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

}
