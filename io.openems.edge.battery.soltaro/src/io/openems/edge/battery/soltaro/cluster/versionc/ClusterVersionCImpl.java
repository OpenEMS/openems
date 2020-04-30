package io.openems.edge.battery.soltaro.cluster.versionc;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.soltaro.SoltaroBattery;
import io.openems.edge.battery.soltaro.State;
import io.openems.edge.battery.soltaro.cluster.SoltaroCluster;
import io.openems.edge.battery.soltaro.cluster.enums.Rack;
import io.openems.edge.battery.soltaro.cluster.versionc.statemachine.StateMachine;
import io.openems.edge.battery.soltaro.single.versionc.enums.PreChargeControl;
import io.openems.edge.battery.soltaro.versionc.SoltaroBatteryVersionC;
import io.openems.edge.battery.soltaro.versionc.utils.Constants;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Bms.Soltaro.Cluster.VersionC", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
		})
public class ClusterVersionCImpl extends AbstractOpenemsModbusComponent implements //
		ClusterVersionC, SoltaroBattery, SoltaroBatteryVersionC, SoltaroCluster, //
		Battery, OpenemsComponent, EventHandler, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(ClusterVersionCImpl.class);

	@Reference
	protected ConfigurationAdmin cm;

	/**
	 * Manages the {@link State}s of the StateMachine.
	 */
	private final StateMachine stateMachine = new StateMachine();

	private Config config;
	private Set<Rack> racks = new HashSet<>();

	public ClusterVersionCImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				SoltaroBattery.ChannelId.values(), //
				SoltaroBatteryVersionC.ChannelId.values(), //
				SoltaroCluster.ChannelId.values(), //
				ClusterVersionC.ChannelId.values() //
		);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		// Initialize active racks
		if (config.isRack1Used()) {
			this.racks.add(Rack.RACK_1);
		}
		if (config.isRack2Used()) {
			this.racks.add(Rack.RACK_2);
		}
		if (config.isRack3Used()) {
			this.racks.add(Rack.RACK_3);
		}
		if (config.isRack4Used()) {
			this.racks.add(Rack.RACK_4);
		}
		if (config.isRack5Used()) {
			this.racks.add(Rack.RACK_5);
		}

		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());

		// Calculate Capacity
		int capacity = this.config.numberOfSlaves() * this.config.moduleType().getCapacity_Wh();
		this.channel(Battery.ChannelId.CAPACITY).setNextValue(capacity);

		// Set Watchdog Timeout
		IntegerWriteChannel c = this.channel(SoltaroBatteryVersionC.ChannelId.EMS_COMMUNICATION_TIMEOUT);
		c.setNextWriteValue(config.watchdog());

		// Initialize Battery Limits
		this.channel(Battery.ChannelId.CHARGE_MAX_CURRENT).setNextValue(0 /* default value 0 to avoid damages */ );
		this.channel(Battery.ChannelId.DISCHARGE_MAX_CURRENT).setNextValue(0 /* default value 0 to avoid damages */ );
		this.channel(Battery.ChannelId.CHARGE_MAX_VOLTAGE)
				.setNextValue(this.config.numberOfSlaves() * Constants.MAX_VOLTAGE_MILLIVOLT / 1000);
		this.channel(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE)
				.setNextValue(this.config.numberOfSlaves() * Constants.MIN_VOLTAGE_MILLIVOLT / 1000);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {

		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.updateChannels();
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
		this.channel(ClusterVersionC.ChannelId.STATE_MACHINE).setNextValue(this.stateMachine.getCurrentState());

		// Initialize 'Ready-For-Working' Channel
		this.setReadyForWorking(false);

		// Prepare Context
		StateMachine.Context context = new StateMachine.Context(this, this.config);

		// Call the StateMachine
		try {
			this.stateMachine.run(context);

			this.channel(SoltaroBatteryVersionC.ChannelId.RUN_FAILED).setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(SoltaroBatteryVersionC.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value() //
				+ "|Discharge:" + this.getDischargeMinVoltage().value() + ";" + this.getDischargeMaxCurrent().value() //
				+ "|Charge:" + this.getChargeMaxVoltage().value() + ";" + this.getChargeMaxCurrent().value() //
				+ "|State:" + this.stateMachine.getCurrentState();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		ModbusProtocol protocol = new ModbusProtocol(this,
				/*
				 * BMS Control Registers
				 */
				new FC16WriteRegistersTask(0x1024,
						m(SoltaroBatteryVersionC.ChannelId.EMS_COMMUNICATION_TIMEOUT, new UnsignedWordElement(0x1024)), //
						m(SoltaroBatteryVersionC.ChannelId.EMS_ADDRESS, new UnsignedWordElement(0x1025)), //
						m(SoltaroBatteryVersionC.ChannelId.EMS_BAUDRATE, new UnsignedWordElement(0x1026)) //
				), //
				new FC3ReadRegistersTask(0x1024, Priority.LOW,
						m(SoltaroBatteryVersionC.ChannelId.EMS_COMMUNICATION_TIMEOUT, new UnsignedWordElement(0x1024)), //
						m(SoltaroBatteryVersionC.ChannelId.EMS_ADDRESS, new UnsignedWordElement(0x1025)), //
						m(SoltaroBatteryVersionC.ChannelId.EMS_BAUDRATE, new UnsignedWordElement(0x1026)) //
				), //
				new FC16WriteRegistersTask(0x10C3, //
						m(SoltaroCluster.ChannelId.START_STOP, new UnsignedWordElement(0x10C3)), //
						m(SoltaroCluster.ChannelId.RACK_1_USAGE, new UnsignedWordElement(0x10C4)), //
						m(SoltaroCluster.ChannelId.RACK_2_USAGE, new UnsignedWordElement(0x10C5)), //
						m(SoltaroCluster.ChannelId.RACK_3_USAGE, new UnsignedWordElement(0x10C6)), //
						m(SoltaroCluster.ChannelId.RACK_4_USAGE, new UnsignedWordElement(0x10C7)), //
						m(SoltaroCluster.ChannelId.RACK_5_USAGE, new UnsignedWordElement(0x10C8)) //
				), //
				new FC3ReadRegistersTask(0x10C3, Priority.LOW,
						m(SoltaroCluster.ChannelId.START_STOP, new UnsignedWordElement(0x10C3)), //
						m(SoltaroCluster.ChannelId.RACK_1_USAGE, new UnsignedWordElement(0x10C4)), //
						m(SoltaroCluster.ChannelId.RACK_2_USAGE, new UnsignedWordElement(0x10C5)), //
						m(SoltaroCluster.ChannelId.RACK_3_USAGE, new UnsignedWordElement(0x10C6)), //
						m(SoltaroCluster.ChannelId.RACK_4_USAGE, new UnsignedWordElement(0x10C7)), //
						m(SoltaroCluster.ChannelId.RACK_5_USAGE, new UnsignedWordElement(0x10C8))), //
				/*
				 * BMS System Running Status Registers
				 */
				new FC3ReadRegistersTask(0x1044, Priority.LOW, //
						m(SoltaroBattery.ChannelId.CHARGE_INDICATION, new UnsignedWordElement(0x1044)), //
						m(SoltaroCluster.ChannelId.SYSTEM_CURRENT, new UnsignedWordElement(0x1045), //
								ElementToChannelConverter.SCALE_FACTOR_2),
						new DummyRegisterElement(0x1046), //
						m(ClusterVersionC.ChannelId.ORIGINAL_SOC, new UnsignedWordElement(0x1047)), //
						m(SoltaroCluster.ChannelId.SYSTEM_RUNNING_STATE, new UnsignedWordElement(0x1048)), //
						m(Battery.ChannelId.VOLTAGE, new UnsignedWordElement(0x1049), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(SoltaroCluster.ChannelId.SYSTEM_INSULATION, new UnsignedWordElement(0x104A)), //
						new DummyRegisterElement(0x104B, 0x104D), //
						m(Battery.ChannelId.CHARGE_MAX_CURRENT, new UnsignedWordElement(0x104E),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(Battery.ChannelId.DISCHARGE_MAX_CURRENT, new UnsignedWordElement(0x104F),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //
				new FC3ReadRegistersTask(0x1081, Priority.LOW, //
						m(new BitsWordElement(0x1081, this) //
								.bit(0, ClusterVersionC.ChannelId.MASTER_PCS_COMMUNICATION_FAILURE) //
								.bit(1, ClusterVersionC.ChannelId.MASTER_PCS_CONTROL_FAILURE) //
								.bit(2, ClusterVersionC.ChannelId.MASTER_EMS_COMMUNICATION_FAILURE) //
						), //
						m(new BitsWordElement(0x1082, this) //
								.bit(0, SoltaroCluster.ChannelId.SUB_MASTER_1_COMMUNICATION_FAILURE) //
								.bit(1, SoltaroCluster.ChannelId.SUB_MASTER_2_COMMUNICATION_FAILURE) //
								.bit(2, SoltaroCluster.ChannelId.SUB_MASTER_3_COMMUNICATION_FAILURE) //
								.bit(3, SoltaroCluster.ChannelId.SUB_MASTER_4_COMMUNICATION_FAILURE) //
								.bit(4, SoltaroCluster.ChannelId.SUB_MASTER_5_COMMUNICATION_FAILURE) //
						), //
						m(new BitsWordElement(0x1083, this) //
								.bit(0, ClusterVersionC.ChannelId.RACK_1_VOLTAGE_DIFFERENCE) //
								.bit(1, ClusterVersionC.ChannelId.RACK_1_OVER_CURRENT) //
								.bit(2, ClusterVersionC.ChannelId.RACK_1_HARDWARE_FAILURE) //
								.bit(3, ClusterVersionC.ChannelId.RACK_1_COMMUNICATION_TO_MASTER_FAILURE) //
								.bit(4, ClusterVersionC.ChannelId.RACK_1_PCS_CONTROL_FAILURE) //
								.bit(5, ClusterVersionC.ChannelId.RACK_1_LEVEL_2_ALARM) //
						), //
						m(new BitsWordElement(0x1084, this) //
								.bit(0, ClusterVersionC.ChannelId.RACK_2_VOLTAGE_DIFFERENCE) //
								.bit(1, ClusterVersionC.ChannelId.RACK_2_OVER_CURRENT) //
								.bit(2, ClusterVersionC.ChannelId.RACK_2_HARDWARE_FAILURE) //
								.bit(3, ClusterVersionC.ChannelId.RACK_2_COMMUNICATION_TO_MASTER_FAILURE) //
								.bit(4, ClusterVersionC.ChannelId.RACK_2_PCS_CONTROL_FAILURE) //
								.bit(5, ClusterVersionC.ChannelId.RACK_2_LEVEL_2_ALARM) //
						), //
						m(new BitsWordElement(0x1085, this) //
								.bit(0, ClusterVersionC.ChannelId.RACK_3_VOLTAGE_DIFFERENCE) //
								.bit(1, ClusterVersionC.ChannelId.RACK_3_OVER_CURRENT) //
								.bit(2, ClusterVersionC.ChannelId.RACK_3_HARDWARE_FAILURE) //
								.bit(3, ClusterVersionC.ChannelId.RACK_3_COMMUNICATION_TO_MASTER_FAILURE) //
								.bit(4, ClusterVersionC.ChannelId.RACK_3_PCS_CONTROL_FAILURE) //
								.bit(5, ClusterVersionC.ChannelId.RACK_3_LEVEL_2_ALARM) //
						), //
						m(new BitsWordElement(0x1086, this) //
								.bit(0, ClusterVersionC.ChannelId.RACK_4_VOLTAGE_DIFFERENCE) //
								.bit(1, ClusterVersionC.ChannelId.RACK_4_OVER_CURRENT) //
								.bit(2, ClusterVersionC.ChannelId.RACK_4_HARDWARE_FAILURE) //
								.bit(3, ClusterVersionC.ChannelId.RACK_4_COMMUNICATION_TO_MASTER_FAILURE) //
								.bit(4, ClusterVersionC.ChannelId.RACK_4_PCS_CONTROL_FAILURE) //
								.bit(5, ClusterVersionC.ChannelId.RACK_4_LEVEL_2_ALARM) //
						), //
						m(new BitsWordElement(0x1087, this) //
								.bit(0, ClusterVersionC.ChannelId.RACK_5_VOLTAGE_DIFFERENCE) //
								.bit(1, ClusterVersionC.ChannelId.RACK_5_OVER_CURRENT) //
								.bit(2, ClusterVersionC.ChannelId.RACK_5_HARDWARE_FAILURE) //
								.bit(3, ClusterVersionC.ChannelId.RACK_5_COMMUNICATION_TO_MASTER_FAILURE) //
								.bit(4, ClusterVersionC.ChannelId.RACK_5_PCS_CONTROL_FAILURE) //
								.bit(5, ClusterVersionC.ChannelId.RACK_5_LEVEL_2_ALARM) //
						), //
						new DummyRegisterElement(0x1088, 0x1092), //
						// Pre-Alarm Summary: Temperature Alarm can be used for current limitation,
						// while all other alarms are just for alarm. Note: Alarm for all clusters
						m(new BitsWordElement(0x1093, this) //
								.bit(0, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_CELL_VOLTAGE_HIGH) //
								.bit(1, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_TOTAL_VOLTAGE_HIGH) //
								.bit(2, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_CHARGE_CURRENT_HIGH) //
								.bit(3, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_CELL_VOLTAGE_LOW) //
								.bit(4, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_TOTAL_VOLTAGE_LOW) //
								.bit(5, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_DISCHARGE_CURRENT_HIGH) //
								.bit(6, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_CHARGE_TEMP_HIGH) //
								.bit(7, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_CHARGE_TEMP_LOW) //
								.bit(8, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_SOC_LOW) //
								.bit(9, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_TEMP_DIFF_TOO_BIG) //
								.bit(10, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_POWER_POLE_HIGH) //
								.bit(11, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_CELL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(12, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_INSULATION_FAIL) //
								.bit(13, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_TOTAL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(14, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_DISCHARGE_TEMP_HIGH) //
								.bit(15, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_DISCHARGE_TEMP_LOW)), //
						// Level 1 Alarm Summary
						m(new BitsWordElement(0x1094, this) //
								.bit(0, SoltaroBatteryVersionC.ChannelId.LEVEL1_CELL_VOLTAGE_HIGH) //
								.bit(1, SoltaroBatteryVersionC.ChannelId.LEVEL1_TOTAL_VOLTAGE_HIGH) //
								.bit(2, SoltaroBatteryVersionC.ChannelId.LEVEL1_CHARGE_CURRENT_HIGH) //
								.bit(3, SoltaroBatteryVersionC.ChannelId.LEVEL1_CELL_VOLTAGE_LOW) //
								.bit(4, SoltaroBatteryVersionC.ChannelId.LEVEL1_TOTAL_VOLTAGE_LOW) //
								.bit(5, SoltaroBatteryVersionC.ChannelId.LEVEL1_DISCHARGE_CURRENT_HIGH) //
								.bit(6, SoltaroBatteryVersionC.ChannelId.LEVEL1_CHARGE_TEMP_HIGH) //
								.bit(7, SoltaroBatteryVersionC.ChannelId.LEVEL1_CHARGE_TEMP_LOW) //
								.bit(8, SoltaroBatteryVersionC.ChannelId.LEVEL1_SOC_LOW) //
								.bit(9, SoltaroBatteryVersionC.ChannelId.LEVEL1_TEMP_DIFF_TOO_BIG) //
								.bit(10, SoltaroBatteryVersionC.ChannelId.LEVEL1_POWER_POLE_TEMP_HIGH) //
								.bit(11, SoltaroBatteryVersionC.ChannelId.LEVEL1_CELL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(12, SoltaroBatteryVersionC.ChannelId.LEVEL1_INSULATION_VALUE) //
								.bit(13, SoltaroBatteryVersionC.ChannelId.LEVEL1_TOTAL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(14, SoltaroBatteryVersionC.ChannelId.LEVEL1_DISCHARGE_TEMP_HIGH) //
								.bit(15, SoltaroBatteryVersionC.ChannelId.LEVEL1_DISCHARGE_TEMP_LOW)), //
						// Level 2 Alarm Summary
						m(new BitsWordElement(0x1095, this) //
								.bit(0, SoltaroBatteryVersionC.ChannelId.LEVEL2_CELL_VOLTAGE_HIGH) //
								.bit(1, SoltaroBatteryVersionC.ChannelId.LEVEL2_TOTAL_VOLTAGE_HIGH) //
								.bit(2, SoltaroBatteryVersionC.ChannelId.LEVEL2_CHARGE_CURRENT_HIGH) //
								.bit(3, SoltaroBatteryVersionC.ChannelId.LEVEL2_CELL_VOLTAGE_LOW) //
								.bit(4, SoltaroBatteryVersionC.ChannelId.LEVEL2_TOTAL_VOLTAGE_LOW) //
								.bit(5, SoltaroBatteryVersionC.ChannelId.LEVEL2_DISCHARGE_CURRENT_HIGH) //
								.bit(6, SoltaroBatteryVersionC.ChannelId.LEVEL2_CHARGE_TEMP_HIGH) //
								.bit(7, SoltaroBatteryVersionC.ChannelId.LEVEL2_CHARGE_TEMP_LOW) //
								.bit(8, SoltaroBatteryVersionC.ChannelId.LEVEL2_SOC_LOW) //
								.bit(9, SoltaroBatteryVersionC.ChannelId.LEVEL2_TEMP_DIFF_TOO_BIG) //
								.bit(10, SoltaroBatteryVersionC.ChannelId.LEVEL2_POWER_POLE_TEMP_HIGH) //
								.bit(11, SoltaroBatteryVersionC.ChannelId.LEVEL2_CELL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(12, SoltaroBatteryVersionC.ChannelId.LEVEL2_INSULATION_VALUE) //
								.bit(13, SoltaroBatteryVersionC.ChannelId.LEVEL2_TOTAL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(14, SoltaroBatteryVersionC.ChannelId.LEVEL2_DISCHARGE_TEMP_HIGH) //
								.bit(15, SoltaroBatteryVersionC.ChannelId.LEVEL2_DISCHARGE_TEMP_LOW) //
						) //
				)); //

		// Create racks dynamically, do this before super() call because super() uses
		// getModbusProtocol, and it is using racks...
		for (Rack r : this.racks) {
			protocol.addTasks(//
					// Single Cluster Control Registers (running without Master BMS)
					new FC6WriteRegisterTask(r.offset + 0x0010, //
							m(rack(r, RackChannel.PRE_CHARGE_CONTROL), new UnsignedWordElement(r.offset + 0x0010)) //
					), //
					new FC16WriteRegistersTask(r.offset + 0x000B, //
							m(rack(r, RackChannel.EMS_ADDRESS), new UnsignedWordElement(r.offset + 0x000B)), //
							m(rack(r, RackChannel.EMS_BAUDRATE), new UnsignedWordElement(r.offset + 0x000C)) //
					), //
					new FC6WriteRegisterTask(r.offset + 0x00F4, m(rack(r, RackChannel.EMS_COMMUNICATION_TIMEOUT),
							new UnsignedWordElement(r.offset + 0x00F4)) //
					), //
					new FC3ReadRegistersTask(r.offset + 0x000B, Priority.LOW, //
							m(rack(r, RackChannel.EMS_ADDRESS), new UnsignedWordElement(r.offset + 0x000B)), //
							m(rack(r, RackChannel.EMS_BAUDRATE), new UnsignedWordElement(r.offset + 0x000C)), //
							new DummyRegisterElement(r.offset + 0x000D, r.offset + 0x000F),
							m(rack(r, RackChannel.PRE_CHARGE_CONTROL), new UnsignedWordElement(r.offset + 0x0010)), //
							new DummyRegisterElement(r.offset + 0x0011, r.offset + 0x0014),
							m(rack(r, RackChannel.SET_SUB_MASTER_ADDRESS), new UnsignedWordElement(r.offset + 0x0015)) //
					), //
					new FC3ReadRegistersTask(r.offset + 0x00F4, Priority.LOW, //
							m(rack(r, RackChannel.EMS_COMMUNICATION_TIMEOUT),
									new UnsignedWordElement(r.offset + 0x00F4)) //
					),

					// Single Cluster Control Registers (General)
					new FC6WriteRegisterTask(r.offset + 0x00CC, //
							m(rack(r, RackChannel.SYSTEM_TOTAL_CAPACITY), new UnsignedWordElement(r.offset + 0x00CC)) //
					), //
					new FC6WriteRegisterTask(r.offset + 0x0015, //
							m(rack(r, RackChannel.SET_SUB_MASTER_ADDRESS), new UnsignedWordElement(r.offset + 0x0015)) //
					), //
					new FC6WriteRegisterTask(r.offset + 0x00F3, //
							m(rack(r, RackChannel.VOLTAGE_LOW_PROTECTION), new UnsignedWordElement(r.offset + 0x00F3)) //
					), //
					new FC3ReadRegistersTask(r.offset + 0x00CC, Priority.LOW, //
							m(this.rack(r, RackChannel.SYSTEM_TOTAL_CAPACITY),
									new UnsignedWordElement(r.offset + 0x00CC)) //
					),

					// Single Cluster Status Registers
					new FC3ReadRegistersTask(r.offset + 0x100, Priority.HIGH, //
							m(rack(r, RackChannel.VOLTAGE), new UnsignedWordElement(r.offset + 0x100),
									ElementToChannelConverter.SCALE_FACTOR_2),
							m(rack(r, RackChannel.CURRENT), new UnsignedWordElement(r.offset + 0x101),
									ElementToChannelConverter.SCALE_FACTOR_2),
							m(rack(r, RackChannel.CHARGE_INDICATION), new UnsignedWordElement(r.offset + 0x102)),
							m(rack(r, RackChannel.SOC), new UnsignedWordElement(r.offset + 0x103)),
							m(rack(r, RackChannel.SOH), new UnsignedWordElement(r.offset + 0x104)),
							m(rack(r, RackChannel.MAX_CELL_VOLTAGE_ID), new UnsignedWordElement(r.offset + 0x105)),
							m(rack(r, RackChannel.MAX_CELL_VOLTAGE), new UnsignedWordElement(r.offset + 0x106)),
							m(rack(r, RackChannel.MIN_CELL_VOLTAGE_ID), new UnsignedWordElement(r.offset + 0x107)),
							m(rack(r, RackChannel.MIN_CELL_VOLTAGE), new UnsignedWordElement(r.offset + 0x108)),
							m(rack(r, RackChannel.MAX_CELL_TEMPERATURE_ID), new UnsignedWordElement(r.offset + 0x109)),
							m(rack(r, RackChannel.MAX_CELL_TEMPERATURE), new UnsignedWordElement(r.offset + 0x10A)),
							m(rack(r, RackChannel.MIN_CELL_TEMPERATURE_ID), new UnsignedWordElement(r.offset + 0x10B)),
							m(rack(r, RackChannel.MIN_CELL_TEMPERATURE), new UnsignedWordElement(r.offset + 0x10C)),
							m(rack(r, RackChannel.AVERAGE_VOLTAGE), new UnsignedWordElement(r.offset + 0x10D)),
							m(rack(r, RackChannel.SYSTEM_INSULATION), new UnsignedWordElement(r.offset + 0x10E)),
							m(rack(r, RackChannel.SYSTEM_MAX_CHARGE_CURRENT), new UnsignedWordElement(r.offset + 0x10F),
									ElementToChannelConverter.SCALE_FACTOR_2),
							m(rack(r, RackChannel.SYSTEM_MAX_DISCHARGE_CURRENT),
									new UnsignedWordElement(r.offset + 0x110),
									ElementToChannelConverter.SCALE_FACTOR_2),
							m(rack(r, RackChannel.POSITIVE_INSULATION), new UnsignedWordElement(r.offset + 0x111)),
							m(rack(r, RackChannel.NEGATIVE_INSULATION), new UnsignedWordElement(r.offset + 0x112)),
							m(rack(r, RackChannel.CLUSTER_RUN_STATE), new UnsignedWordElement(r.offset + 0x113)),
							m(rack(r, RackChannel.AVG_TEMPERATURE), new UnsignedWordElement(r.offset + 0x114))),
					new FC3ReadRegistersTask(r.offset + 0x18b, Priority.LOW,
							m(rack(r, RackChannel.PROJECT_ID), new UnsignedWordElement(r.offset + 0x18b)),
							m(rack(r, RackChannel.VERSION_MAJOR), new UnsignedWordElement(r.offset + 0x18c)),
							m(rack(r, RackChannel.VERSION_SUB), new UnsignedWordElement(r.offset + 0x18d)),
							m(rack(r, RackChannel.VERSION_MODIFY), new UnsignedWordElement(r.offset + 0x18e))),

					// System Warning/Shut Down Status Registers
					new FC3ReadRegistersTask(r.offset + 0x140, Priority.LOW,
							// Level 2 Alarm: BMS Self-protect, main contactor shut down
							m(new BitsWordElement(r.offset + 0x140, this) //
									.bit(0, rack(r, RackChannel.LEVEL2_CELL_VOLTAGE_HIGH)) //
									.bit(1, rack(r, RackChannel.LEVEL2_TOTAL_VOLTAGE_HIGH)) //
									.bit(2, rack(r, RackChannel.LEVEL2_CHARGE_CURRENT_HIGH)) //
									.bit(3, rack(r, RackChannel.LEVEL2_CELL_VOLTAGE_LOW)) //
									.bit(4, rack(r, RackChannel.LEVEL2_TOTAL_VOLTAGE_LOW)) //
									.bit(5, rack(r, RackChannel.LEVEL2_DISCHARGE_CURRENT_HIGH)) //
									.bit(6, rack(r, RackChannel.LEVEL2_CHARGE_TEMP_HIGH)) //
									.bit(7, rack(r, RackChannel.LEVEL2_CHARGE_TEMP_LOW)) //
									// 8 -> Reserved
									// 9 -> Reserved
									.bit(10, rack(r, RackChannel.LEVEL2_POWER_POLE_TEMP_HIGH)) //
									// 11 -> Reserved
									.bit(12, rack(r, RackChannel.LEVEL2_INSULATION_VALUE)) //
									// 13 -> Reserved
									.bit(14, rack(r, RackChannel.LEVEL2_DISCHARGE_TEMP_HIGH)) //
									.bit(15, rack(r, RackChannel.LEVEL2_DISCHARGE_TEMP_LOW)) //
							),
							// Level 1 Alarm: EMS Control to stop charge, discharge, charge&discharge
							m(new BitsWordElement(r.offset + 0x141, this) //
									.bit(0, rack(r, RackChannel.LEVEL1_CELL_VOLTAGE_HIGH)) //
									.bit(1, rack(r, RackChannel.LEVEL1_TOTAL_VOLTAGE_HIGH)) //
									.bit(2, rack(r, RackChannel.LEVEL1_CHARGE_CURRENT_HIGH)) //
									.bit(3, rack(r, RackChannel.LEVEL1_CELL_VOLTAGE_LOW)) //
									.bit(4, rack(r, RackChannel.LEVEL1_TOTAL_VOLTAGE_LOW)) //
									.bit(5, rack(r, RackChannel.LEVEL1_DISCHARGE_CURRENT_HIGH)) //
									.bit(6, rack(r, RackChannel.LEVEL1_CHARGE_TEMP_HIGH)) //
									.bit(7, rack(r, RackChannel.LEVEL1_CHARGE_TEMP_LOW)) //
									.bit(8, rack(r, RackChannel.LEVEL1_SOC_LOW)) //
									.bit(9, rack(r, RackChannel.LEVEL1_TEMP_DIFF_TOO_BIG)) //
									.bit(10, rack(r, RackChannel.LEVEL1_POWER_POLE_TEMP_HIGH)) //
									.bit(11, rack(r, RackChannel.LEVEL1_CELL_VOLTAGE_DIFF_TOO_BIG)) //
									.bit(12, rack(r, RackChannel.LEVEL1_INSULATION_VALUE)) //
									.bit(13, rack(r, RackChannel.LEVEL1_TOTAL_VOLTAGE_DIFF_TOO_BIG)) //
									.bit(14, rack(r, RackChannel.LEVEL1_DISCHARGE_TEMP_HIGH)) //
									.bit(15, rack(r, RackChannel.LEVEL1_DISCHARGE_TEMP_LOW)) //
							),
							// Pre-Alarm: Temperature Alarm will active current limication
							m(new BitsWordElement(r.offset + 0x142, this) //
									.bit(0, rack(r, RackChannel.PRE_ALARM_CELL_VOLTAGE_HIGH)) //
									.bit(1, rack(r, RackChannel.PRE_ALARM_TOTAL_VOLTAGE_HIGH)) //
									.bit(2, rack(r, RackChannel.PRE_ALARM_CHARGE_CURRENT_HIGH)) //
									.bit(3, rack(r, RackChannel.PRE_ALARM_CELL_VOLTAGE_LOW)) //
									.bit(4, rack(r, RackChannel.PRE_ALARM_TOTAL_VOLTAGE_LOW)) //
									.bit(5, rack(r, RackChannel.PRE_ALARM_DISCHARGE_CURRENT_HIGH)) //
									.bit(6, rack(r, RackChannel.PRE_ALARM_CHARGE_TEMP_HIGH)) //
									.bit(7, rack(r, RackChannel.PRE_ALARM_CHARGE_TEMP_LOW)) //
									.bit(8, rack(r, RackChannel.PRE_ALARM_SOC_LOW)) //
									.bit(9, rack(r, RackChannel.PRE_ALARM_TEMP_DIFF_TOO_BIG)) //
									.bit(10, rack(r, RackChannel.PRE_ALARM_POWER_POLE_HIGH))//
									.bit(11, rack(r, RackChannel.PRE_ALARM_CELL_VOLTAGE_DIFF_TOO_BIG)) //
									.bit(12, rack(r, RackChannel.PRE_ALARM_INSULATION_FAIL)) //
									.bit(13, rack(r, RackChannel.PRE_ALARM_TOTAL_VOLTAGE_DIFF_TOO_BIG)) //
									.bit(14, rack(r, RackChannel.PRE_ALARM_DISCHARGE_TEMP_HIGH)) //
									.bit(15, rack(r, RackChannel.PRE_ALARM_DISCHARGE_TEMP_LOW)) //
							) //
					),
					// Other Alarm Info
					new FC3ReadRegistersTask(r.offset + 0x1A5, Priority.LOW, //
							m(new BitsWordElement(r.offset + 0x1A5, this) //
									.bit(0, rack(r, RackChannel.ALARM_COMMUNICATION_TO_MASTER_BMS)) //
									.bit(1, rack(r, RackChannel.ALARM_COMMUNICATION_TO_SLAVE_BMS)) //
									.bit(2, rack(r, RackChannel.ALARM_COMMUNICATION_SLAVE_BMS_TO_TEMP_SENSORS)) //
									.bit(3, rack(r, RackChannel.ALARM_SLAVE_BMS_HARDWARE)) //
							)),
					// Slave BMS Fault Message Registers
					new FC3ReadRegistersTask(r.offset + 0x185, Priority.LOW, //
							m(new BitsWordElement(r.offset + 0x185, this) //
									.bit(0, rack(r, RackChannel.SLAVE_BMS_VOLTAGE_SENSOR_CABLES)) //
									.bit(1, rack(r, RackChannel.SLAVE_BMS_POWER_CABLE)) //
									.bit(2, rack(r, RackChannel.SLAVE_BMS_LTC6803)) //
									.bit(3, rack(r, RackChannel.SLAVE_BMS_VOLTAGE_SENSORS)) //
									.bit(4, rack(r, RackChannel.SLAVE_BMS_TEMP_SENSOR_CABLES)) //
									.bit(5, rack(r, RackChannel.SLAVE_BMS_TEMP_SENSORS)) //
									.bit(6, rack(r, RackChannel.SLAVE_BMS_POWER_POLE_TEMP_SENSOR)) //
									.bit(7, rack(r, RackChannel.SLAVE_BMS_TEMP_BOARD_COM)) //
									.bit(8, rack(r, RackChannel.SLAVE_BMS_BALANCE_MODULE)) //
									.bit(9, rack(r, RackChannel.SLAVE_BMS_TEMP_SENSORS2)) //
									.bit(10, rack(r, RackChannel.SLAVE_BMS_INTERNAL_COM)) //
									.bit(11, rack(r, RackChannel.SLAVE_BMS_EEPROM)) //
									.bit(12, rack(r, RackChannel.SLAVE_BMS_INIT)) //
							)) //
			); //

// TODO			/*
//			 * Add tasks for cell voltages and temperatures according to the number of
//			 * slaves, one task per module is created Cell voltages
//			 */
//			Consumer<CellChannelFactory.Type> addCellChannels = (type) -> {
//				for (int i = 0; i < this.config.numberOfSlaves(); i++) {
//					AbstractModbusElement<?>[] elements = new AbstractModbusElement<?>[type.getSensorsPerModule()];
//					for (int j = 0; j < type.getSensorsPerModule(); j++) {
//						int sensorIndex = i * type.getSensorsPerModule() + j;
//						io.openems.edge.common.channel.ChannelId channelId = CellChannelFactory.create(r, type,
//								sensorIndex);
//						// Register the Channel at this Component
//						this.addChannel(channelId);
//						// Add the Modbus Element and map it to the Channel
//						elements[j] = m(channelId, new UnsignedWordElement(r.offset + type.getOffset() + sensorIndex));
//					}
//					// Add a Modbus read task for this module
//					protocol.addTask(//
//							new FC3ReadRegistersTask(r.offset + type.getOffset() + i * type.getSensorsPerModule(),
//									Priority.LOW, elements));
//				}
//			};
//			addCellChannels.accept(CellChannelFactory.Type.VOLTAGE);
//			addCellChannels.accept(CellChannelFactory.Type.TEMPERATURE);

			// WARN_LEVEL_Pre Alarm (Pre Alarm configuration registers RW)
			{
				AbstractModbusElement<?>[] elements = new AbstractModbusElement<?>[] {
						m(rack(r, RackChannel.PRE_ALARM_CELL_OVER_VOLTAGE_ALARM),
								new UnsignedWordElement(r.offset + 0x080)), //
						m(rack(r, RackChannel.PRE_ALARM_CELL_OVER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x081)), //
						m(rack(r, RackChannel.PRE_ALARM_SYSTEM_OVER_VOLTAGE_ALARM),
								new UnsignedWordElement(r.offset + 0x082), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.PRE_ALARM_SYSTEM_OVER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x083), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.PRE_ALARM_SYSTEM_CHARGE_OVER_CURRENT_ALARM),
								new UnsignedWordElement(r.offset + 0x084), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.PRE_ALARM_SYSTEM_CHARGE_OVER_CURRENT_RECOVER),
								new UnsignedWordElement(r.offset + 0x085), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.PRE_ALARM_CELL_UNDER_VOLTAGE_ALARM),
								new UnsignedWordElement(r.offset + 0x086)), //
						m(rack(r, RackChannel.PRE_ALARM_CELL_UNDER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x087)), //
						m(rack(r, RackChannel.PRE_ALARM_SYSTEM_UNDER_VOLTAGE_ALARM),
								new UnsignedWordElement(r.offset + 0x088), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.PRE_ALARM_SYSTEM_UNDER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x089), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.PRE_ALARM_SYSTEM_DISCHARGE_OVER_CURRENT_ALARM),
								new UnsignedWordElement(r.offset + 0x08A), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.PRE_ALARM_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER),
								new UnsignedWordElement(r.offset + 0x08B), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.PRE_ALARM_CELL_OVER_TEMPERATURE_ALARM),
								new UnsignedWordElement(r.offset + 0x08C)), //
						m(rack(r, RackChannel.PRE_ALARM_CELL_OVER_TEMPERATURE_RECOVER),
								new UnsignedWordElement(r.offset + 0x08D)), //
						m(rack(r, RackChannel.PRE_ALARM_CELL_UNDER_TEMPERATURE_ALARM),
								new UnsignedWordElement(r.offset + 0x08E)), //
						m(rack(r, RackChannel.PRE_ALARM_CELL_UNDER_TEMPERATURE_RECOVER),
								new UnsignedWordElement(r.offset + 0x08F)), //
						m(rack(r, RackChannel.PRE_ALARM_SOC_LOW_ALARM), new UnsignedWordElement(r.offset + 0x090)), //
						m(rack(r, RackChannel.PRE_ALARM_SOC_LOW_ALARM_RECOVER),
								new UnsignedWordElement(r.offset + 0x091)), //
						new DummyRegisterElement(r.offset + 0x092, r.offset + 0x093),
						m(rack(r, RackChannel.PRE_ALARM_CONNECTOR_TEMPERATURE_HIGH_ALARM),
								new UnsignedWordElement(r.offset + 0x094)), //
						m(rack(r, RackChannel.PRE_ALARM_CONNECTOR_TEMPERATURE_HIGH_ALARM_RECOVER),
								new UnsignedWordElement(r.offset + 0x095)), //
						m(rack(r, RackChannel.PRE_ALARM_INSULATION_ALARM), new UnsignedWordElement(r.offset + 0x096)), //
						m(rack(r, RackChannel.PRE_ALARM_INSULATION_ALARM_RECOVER),
								new UnsignedWordElement(r.offset + 0x097)), //
						m(rack(r, RackChannel.PRE_ALARM_CELL_VOLTAGE_DIFFERENCE_ALARM),
								new UnsignedWordElement(r.offset + 0x098)), //
						m(rack(r, RackChannel.PRE_ALARM_CELL_VOLTAGE_DIFFERENCE_ALARM_RECOVER),
								new UnsignedWordElement(r.offset + 0x099)), //
						m(rack(r, RackChannel.PRE_ALARM_TOTAL_VOLTAGE_DIFFERENCE_ALARM),
								new UnsignedWordElement(r.offset + 0x09A), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.PRE_ALARM_TOTAL_VOLTAGE_DIFFERENCE_ALARM_RECOVER),
								new UnsignedWordElement(r.offset + 0x09B), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.PRE_ALARM_DISCHARGE_TEMPERATURE_HIGH_ALARM),
								new UnsignedWordElement(r.offset + 0x09C)), //
						m(rack(r, RackChannel.PRE_ALARM_DISCHARGE_TEMPERATURE_HIGH_ALARM_RECOVER),
								new UnsignedWordElement(r.offset + 0x09D)), //
						m(rack(r, RackChannel.PRE_ALARM_DISCHARGE_TEMPERATURE_LOW_ALARM),
								new UnsignedWordElement(r.offset + 0x09E)), //
						m(rack(r, RackChannel.PRE_ALARM_DISCHARGE_TEMPERATURE_LOW_ALARM_RECOVER),
								new UnsignedWordElement(r.offset + 0x09F)), //
						m(rack(r, RackChannel.PRE_ALARM_TEMPERATURE_DIFFERENCE_ALARM),
								new UnsignedWordElement(r.offset + 0x0A0)), //
						m(rack(r, RackChannel.PRE_ALARM_TEMPERATURE_DIFFERENCE_ALARM_RECOVER),
								new UnsignedWordElement(r.offset + 0x0A1)) //
				};
				protocol.addTask(new FC16WriteRegistersTask(r.offset + 0x080, elements));
				protocol.addTask(new FC3ReadRegistersTask(r.offset + 0x080, Priority.LOW, elements));
			}

			// WARN_LEVEL1 (Level1 warning registers RW)
			{
				AbstractModbusElement<?>[] elements = new AbstractModbusElement<?>[] {
						m(rack(r, RackChannel.LEVEL1_CELL_OVER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x040)), //
						m(rack(r, RackChannel.LEVEL1_CELL_OVER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x041)), //
						m(rack(r, RackChannel.LEVEL1_SYSTEM_OVER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x042), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.LEVEL1_SYSTEM_OVER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x043), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.LEVEL1_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION),
								new UnsignedWordElement(r.offset + 0x044), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.LEVEL1_SYSTEM_CHARGE_OVER_CURRENT_RECOVER),
								new UnsignedWordElement(r.offset + 0x045), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.LEVEL1_CELL_UNDER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x046)), //
						m(rack(r, RackChannel.LEVEL1_CELL_UNDER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x047)), //
						m(rack(r, RackChannel.LEVEL1_SYSTEM_UNDER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x048), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.LEVEL1_SYSTEM_UNDER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x049), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.LEVEL1_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION),
								new UnsignedWordElement(r.offset + 0x04A), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.LEVEL1_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER),
								new UnsignedWordElement(r.offset + 0x04B), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.LEVEL1_CELL_OVER_TEMPERATURE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x04C)), //
						m(rack(r, RackChannel.LEVEL1_CELL_OVER_TEMPERATURE_RECOVER),
								new UnsignedWordElement(r.offset + 0x04D)), //
						m(rack(r, RackChannel.LEVEL1_CELL_UNDER_TEMPERATURE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x04E)), //
						m(rack(r, RackChannel.LEVEL1_CELL_UNDER_TEMPERATURE_RECOVER),
								new UnsignedWordElement(r.offset + 0x04F)), //
						m(rack(r, RackChannel.LEVEL1_SOC_LOW_PROTECTION), new UnsignedWordElement(r.offset + 0x050)), //
						m(rack(r, RackChannel.LEVEL1_SOC_LOW_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x051)), //
						new DummyRegisterElement(r.offset + 0x052, r.offset + 0x053), //
						m(rack(r, RackChannel.LEVEL1_CONNECTOR_TEMPERATURE_HIGH_PROTECTION),
								new UnsignedWordElement(r.offset + 0x054)), //
						m(rack(r, RackChannel.LEVEL1_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x055)), //
						m(rack(r, RackChannel.LEVEL1_INSULATION_PROTECTION), new UnsignedWordElement(r.offset + 0x056)), //
						m(rack(r, RackChannel.LEVEL1_INSULATION_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x057)), //
						m(rack(r, RackChannel.LEVEL1_CELL_VOLTAGE_DIFFERENCE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x058)), //
						m(rack(r, RackChannel.LEVEL1_CELL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x059)), //
						m(rack(r, RackChannel.LEVEL1_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x05A), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.LEVEL1_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x05B), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.LEVEL1_DISCHARGE_TEMPERATURE_HIGH_PROTECTION),
								new UnsignedWordElement(r.offset + 0x05C)), //
						m(rack(r, RackChannel.LEVEL1_DISCHARGE_TEMPERATURE_HIGH_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x05D)), //
						m(rack(r, RackChannel.LEVEL1_DISCHARGE_TEMPERATURE_LOW_PROTECTION),
								new UnsignedWordElement(r.offset + 0x05E)), //
						m(rack(r, RackChannel.LEVEL1_DISCHARGE_TEMPERATURE_LOW_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x05F)), //
						m(rack(r, RackChannel.LEVEL1_TEMPERATURE_DIFFERENCE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x060)), //
						m(rack(r, RackChannel.LEVEL1_TEMPERATURE_DIFFERENCE_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x061)) //
				};
				protocol.addTask(new FC16WriteRegistersTask(r.offset + 0x040, elements));
				protocol.addTask(new FC3ReadRegistersTask(r.offset + 0x040, Priority.LOW, elements));
			}

			// WARN_LEVEL2 (Level2 Protection registers RW)
			{
				AbstractModbusElement<?>[] elements = new AbstractModbusElement<?>[] {
						m(rack(r, RackChannel.LEVEL2_CELL_OVER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x400)), //
						m(rack(r, RackChannel.LEVEL2_CELL_OVER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x401)), //
						m(new UnsignedWordElement(r.offset + 0x402)) //
								.m(rack(r, RackChannel.LEVEL2_SYSTEM_OVER_VOLTAGE_PROTECTION),
										ElementToChannelConverter.SCALE_FACTOR_2) // [mV]
								.m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [V]
								.build(), //
						m(rack(r, RackChannel.LEVEL2_SYSTEM_OVER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x403), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.LEVEL2_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION),
								new UnsignedWordElement(r.offset + 0x404), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.LEVEL2_SYSTEM_CHARGE_OVER_CURRENT_RECOVER),
								new UnsignedWordElement(r.offset + 0x405), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.LEVEL2_CELL_UNDER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x406)), //
						m(rack(r, RackChannel.LEVEL2_CELL_UNDER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x407)), //
						m(rack(r, RackChannel.LEVEL2_SYSTEM_UNDER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x408), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.LEVEL2_SYSTEM_UNDER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x409), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.LEVEL2_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION),
								new UnsignedWordElement(r.offset + 0x40A), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.LEVEL2_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER),
								new UnsignedWordElement(r.offset + 0x40B), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.LEVEL2_CELL_OVER_TEMPERATURE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x40C)), //
						m(rack(r, RackChannel.LEVEL2_CELL_OVER_TEMPERATURE_RECOVER),
								new UnsignedWordElement(r.offset + 0x40D)), //
						m(rack(r, RackChannel.LEVEL2_CELL_UNDER_TEMPERATURE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x40E)), //
						m(rack(r, RackChannel.LEVEL2_CELL_UNDER_TEMPERATURE_RECOVER),
								new UnsignedWordElement(r.offset + 0x40F)), //
						m(rack(r, RackChannel.LEVEL2_SOC_LOW_PROTECTION), new UnsignedWordElement(r.offset + 0x410)), //
						m(rack(r, RackChannel.LEVEL2_SOC_LOW_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x411)), //
						new DummyRegisterElement(r.offset + 0x412, r.offset + 0x413), //
						m(rack(r, RackChannel.LEVEL2_CONNECTOR_TEMPERATURE_HIGH_PROTECTION),
								new UnsignedWordElement(r.offset + 0x414)), //
						m(rack(r, RackChannel.LEVEL2_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x415)), //
						m(rack(r, RackChannel.LEVEL2_INSULATION_PROTECTION), new UnsignedWordElement(r.offset + 0x416)), //
						m(rack(r, RackChannel.LEVEL2_INSULATION_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x417)), //
						m(rack(r, RackChannel.LEVEL2_CELL_VOLTAGE_DIFFERENCE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x418)), //
						m(rack(r, RackChannel.LEVEL2_CELL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x419)), //
						m(rack(r, RackChannel.LEVEL2_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x41A), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.LEVEL2_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x41B), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, RackChannel.LEVEL2_DISCHARGE_TEMPERATURE_HIGH_PROTECTION),
								new UnsignedWordElement(r.offset + 0x41C)), //
						m(rack(r, RackChannel.LEVEL2_DISCHARGE_TEMPERATURE_HIGH_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x41D)), //
						m(rack(r, RackChannel.LEVEL2_DISCHARGE_TEMPERATURE_LOW_PROTECTION),
								new UnsignedWordElement(r.offset + 0x41E)), //
						m(rack(r, RackChannel.LEVEL2_DISCHARGE_TEMPERATURE_LOW_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x41F)), //
						m(rack(r, RackChannel.LEVEL2_TEMPERATURE_DIFFERENCE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x420)), //
						m(rack(r, RackChannel.LEVEL2_TEMPERATURE_DIFFERENCE_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x421)) //
				};
				protocol.addTask(new FC16WriteRegistersTask(r.offset + 0x400, elements));
				protocol.addTask(new FC3ReadRegistersTask(r.offset + 0x400, Priority.LOW, elements));
			}

		}
		return protocol;
	}

	/**
	 * Factory-Function for SingleRack-ChannelIds. Creates a ChannelId, registers
	 * the Channel and returns the ChannelId.
	 * 
	 * @param rack        the {@link Rack}
	 * @param rackChannel the {@link RackChannel}
	 * @return the {@link io.openems.edge.common.channel.ChannelId}
	 */
	private final io.openems.edge.common.channel.ChannelId rack(Rack rack, RackChannel rackChannel) {
		Channel<?> existingChannel = this._channel(rackChannel.toChannelIdString(rack));
		if (existingChannel != null) {
			return existingChannel.channelId();
		} else {
			io.openems.edge.common.channel.ChannelId channelId = rackChannel.toChannelId(rack);
			this.addChannel(channelId);
			return channelId;
		}
	}

	/**
	 * Updates Channels on BEFORE_PROCESS_IMAGE event.
	 */
	private void updateChannels() {
		this.channel(Battery.ChannelId.SOC).setNextValue(this.calculateRackAverage(RackChannel.SOC));
		this.channel(Battery.ChannelId.SOH).setNextValue(this.calculateRackAverage(RackChannel.SOH));
		this.channel(Battery.ChannelId.MAX_CELL_VOLTAGE)
				.setNextValue(this.calculateRackMax(RackChannel.MAX_CELL_VOLTAGE));
		this.channel(Battery.ChannelId.MIN_CELL_VOLTAGE)
				.setNextValue(this.calculateRackMin(RackChannel.MIN_CELL_VOLTAGE));
		this.channel(Battery.ChannelId.MAX_CELL_TEMPERATURE)
				.setNextValue(this.calculateRackMax(RackChannel.MAX_CELL_TEMPERATURE));
		this.channel(Battery.ChannelId.MIN_CELL_TEMPERATURE)
				.setNextValue(this.calculateRackMin(RackChannel.MIN_CELL_TEMPERATURE));
	}

	/**
	 * Calculates the average of RackChannel over all active Racks.
	 * 
	 * @return the average value or null
	 */
	private Integer calculateRackAverage(RackChannel rackChannel) {
		Integer cumulated = null;
		for (Rack rack : this.racks) {
			IntegerReadChannel channel = this.channel(rack, rackChannel);
			Integer value = channel.getNextValue().get();
			if (value == null) {
				continue;
			} else if (cumulated == null) {
				cumulated = value;
			} else {
				cumulated += value;
			}
		}
		if (cumulated != null) {
			return cumulated / this.racks.size();
		} else {
			return null;
		}
	}

	/**
	 * Finds the maximum of a RackChannel over all active Racks.
	 * 
	 * @return the maximum value or null
	 */
	private Integer calculateRackMax(RackChannel rackChannel) {
		Integer result = null;
		for (Rack rack : this.racks) {
			IntegerReadChannel channel = this.channel(rack, rackChannel);
			Integer value = channel.getNextValue().get();
			if (value == null) {
				continue;
			} else if (result == null) {
				result = value;
			} else {
				result = Math.max(result, value);
			}
		}
		return result;
	}

	/**
	 * Finds the minimum of a RackChannel over all active Racks.
	 * 
	 * @return the minimum value or null
	 */
	private Integer calculateRackMin(RackChannel rackChannel) {
		Integer result = null;
		for (Rack rack : this.racks) {
			IntegerReadChannel channel = this.channel(rack, rackChannel);
			Integer value = channel.getNextValue().get();
			if (value == null) {
				continue;
			} else if (result == null) {
				result = value;
			} else {
				result = Math.min(result, value);
			}
		}
		return result;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode) //
		);
	}

	@Override
	public <T extends Channel<?>> T channel(Rack rack, RackChannel rackChannel) {
		return this.channel(rackChannel.toChannelIdString(rack));
	}

	@Override
	public boolean isSystemStopped() {
		return this.getCommonPreChargeControl().orElse(PreChargeControl.UNDEFINED) == PreChargeControl.SWITCH_OFF;
	}

	@Override
	public boolean isSystemRunning() {
		return this.getCommonPreChargeControl().orElse(PreChargeControl.UNDEFINED) == PreChargeControl.SWITCH_ON;
	}

	@Override
	public Optional<PreChargeControl> getCommonPreChargeControl() {
		StringBuilder b = new StringBuilder();

		PreChargeControl result = null;
		for (Rack rack : this.racks) {
			EnumReadChannel channel = this.channel(rack, RackChannel.PRE_CHARGE_CONTROL);
			PreChargeControl value = channel.value().asEnum();

			b.append(rack.name() + ":" + value.toString() + " ");

			if (result != value) {
				if (result == null) {
					// first match
					result = value;
				} else {
					// no common PreChargeControl
					return Optional.empty();
				}
			}
		}

		b.append("   Common:" + result);

		this.logInfo(this.log, b.toString());
		return Optional.ofNullable(result);
	}

	@Override
	public Set<Rack> getRacks() {
		return racks;
	}

}
