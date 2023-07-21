package io.openems.edge.battery.soltaro.cluster.versionc;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
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
import io.openems.edge.battery.soltaro.cluster.SoltaroCluster;
import io.openems.edge.battery.soltaro.cluster.enums.Rack;
import io.openems.edge.battery.soltaro.cluster.versionc.statemachine.Context;
import io.openems.edge.battery.soltaro.cluster.versionc.statemachine.StateMachine;
import io.openems.edge.battery.soltaro.cluster.versionc.statemachine.StateMachine.State;
import io.openems.edge.battery.soltaro.common.batteryprotection.BatteryProtectionDefinitionSoltaro3500Wh;
import io.openems.edge.battery.soltaro.common.enums.ModuleType;
import io.openems.edge.battery.soltaro.single.versionc.enums.PreChargeControl;
import io.openems.edge.battery.soltaro.versionc.utils.CellChannelFactory;
import io.openems.edge.battery.soltaro.versionc.utils.Constants;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
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
		name = "Battery.Soltaro.Cluster.VersionC", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class BatterySoltaroClusterVersionCImpl extends AbstractOpenemsModbusComponent implements //
		BatterySoltaroClusterVersionC, SoltaroCluster, //
		Battery, ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave {

	private static final int WATCHDOG = 90;

	private final Logger log = LoggerFactory.getLogger(BatterySoltaroClusterVersionCImpl.class);
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);
	private final TreeSet<Rack> racks = new TreeSet<>();

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

	public BatterySoltaroClusterVersionCImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				SoltaroCluster.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				BatterySoltaroClusterVersionC.ChannelId.values(), //
				BatteryProtection.ChannelId.values() //
		);

	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		// Initialize active racks

		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		// Initialize Battery-Protection
		this.batteryProtection = BatteryProtection.create(this) //
				.applyBatteryProtectionDefinition(new BatteryProtectionDefinitionSoltaro3500Wh(), this.componentManager) //
				.build();

		// Read Number of Towers and Modules
		this.getNumberOfTowers().thenAccept(numberOfTower -> {
			this.getNumberOfModules().thenAccept(numberOfModules -> {
				this.calculateCapacity(numberOfTower, numberOfModules);
				this.initializeBatteryLimits(numberOfModules);
				this.channel(BatterySoltaroClusterVersionC.ChannelId.NUMBER_OF_TOWERS).setNextValue(numberOfTower);
				this.channel(BatterySoltaroClusterVersionC.ChannelId.NUMBER_OF_MODULES_PER_TOWER)
						.setNextValue(numberOfModules);

				// Avoid race-condition: fill local 'racks', then update Channels and only
				// finally update global 'racks'
				var racks = new TreeSet<Rack>();
				if (numberOfTower > 0) {
					racks.add(Rack.RACK_1);
				}
				if (numberOfTower > 1) {
					racks.add(Rack.RACK_2);
				}
				if (numberOfTower > 2) {
					racks.add(Rack.RACK_3);
				}
				if (numberOfTower > 3) {
					racks.add(Rack.RACK_4);
				}
				if (numberOfTower > 4) {
					racks.add(Rack.RACK_5);
				}
				try {
					this.updateRackChannels(numberOfTower, racks);
				} catch (OpenemsException e) {
					this.logError(this.log,
							"Error while updatingRackChannels(" + numberOfTower + "): " + e.getMessage());
					e.printStackTrace();
				}
				this.racks.addAll(racks);
			});
		});

		// Set Watchdog Timeout
		IntegerWriteChannel c = this.channel(BatterySoltaroClusterVersionC.ChannelId.EMS_COMMUNICATION_TIMEOUT);
		c.setNextWriteValue(WATCHDOG);

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void updateRackChannels(Integer numberOfModules, TreeSet<Rack> racks) throws OpenemsException {
		for (Rack r : racks) {
			try {
				this.getModbusProtocol().addTasks(//

						new FC3ReadRegistersTask(r.offset + 0x000B, Priority.LOW, //
								m(this.createChannelId(r, RackChannel.EMS_ADDRESS),
										new UnsignedWordElement(r.offset + 0x000B)), //
								m(this.createChannelId(r, RackChannel.EMS_BAUDRATE),
										new UnsignedWordElement(r.offset + 0x000C)), //
								new DummyRegisterElement(r.offset + 0x000D, r.offset + 0x000F),
								m(this.createChannelId(r, RackChannel.PRE_CHARGE_CONTROL),
										new UnsignedWordElement(r.offset + 0x0010)), //
								new DummyRegisterElement(r.offset + 0x0011, r.offset + 0x0014),
								m(this.createChannelId(r, RackChannel.SET_SUB_MASTER_ADDRESS),
										new UnsignedWordElement(r.offset + 0x0015)) //
						), //
						new FC3ReadRegistersTask(r.offset + 0x00F4, Priority.LOW, //
								m(this.createChannelId(r, RackChannel.EMS_COMMUNICATION_TIMEOUT),
										new UnsignedWordElement(r.offset + 0x00F4)) //
						),

						// Single Cluster Control Registers (running without Master BMS)
						new FC6WriteRegisterTask(r.offset + 0x0010, //
								m(this.createChannelId(r, RackChannel.PRE_CHARGE_CONTROL),
										new UnsignedWordElement(r.offset + 0x0010)) //
						), //
						new FC6WriteRegisterTask(r.offset + 0x00F4, //
								m(this.createChannelId(r, RackChannel.EMS_COMMUNICATION_TIMEOUT),
										new UnsignedWordElement(r.offset + 0x00F4)) //
						), //
						new FC16WriteRegistersTask(r.offset + 0x000B, //
								m(this.createChannelId(r, RackChannel.EMS_ADDRESS),
										new UnsignedWordElement(r.offset + 0x000B)), //
								m(this.createChannelId(r, RackChannel.EMS_BAUDRATE),
										new UnsignedWordElement(r.offset + 0x000C)) //
						), //

						// Single Cluster Control Registers (General)
						new FC6WriteRegisterTask(r.offset + 0x00CC, //
								m(this.createChannelId(r, RackChannel.SYSTEM_TOTAL_CAPACITY),
										new UnsignedWordElement(r.offset + 0x00CC)) //
						), //
						new FC6WriteRegisterTask(r.offset + 0x0015, //
								m(this.createChannelId(r, RackChannel.SET_SUB_MASTER_ADDRESS),
										new UnsignedWordElement(r.offset + 0x0015)) //
						), //
						new FC6WriteRegisterTask(r.offset + 0x00F3, //
								m(this.createChannelId(r, RackChannel.VOLTAGE_LOW_PROTECTION),
										new UnsignedWordElement(r.offset + 0x00F3)) //
						), //
						new FC3ReadRegistersTask(r.offset + 0x00CC, Priority.LOW, //
								m(this.createChannelId(r, RackChannel.SYSTEM_TOTAL_CAPACITY),
										new UnsignedWordElement(r.offset + 0x00CC)) //
						),

						// Single Cluster Status Registers
						new FC3ReadRegistersTask(r.offset + 0x100, Priority.HIGH, //
								m(this.createChannelId(r, RackChannel.VOLTAGE),
										new UnsignedWordElement(r.offset + 0x100), SCALE_FACTOR_2),
								m(this.createChannelId(r, RackChannel.CURRENT), new SignedWordElement(r.offset + 0x101),
										SCALE_FACTOR_2),
								m(this.createChannelId(r, RackChannel.CHARGE_INDICATION),
										new UnsignedWordElement(r.offset + 0x102)),
								m(this.createChannelId(r, RackChannel.SOC), new UnsignedWordElement(r.offset + 0x103)),
								m(this.createChannelId(r, RackChannel.SOH), new UnsignedWordElement(r.offset + 0x104)),
								m(this.createChannelId(r, RackChannel.MAX_CELL_VOLTAGE_ID),
										new UnsignedWordElement(r.offset + 0x105)),
								m(this.createChannelId(r, RackChannel.MAX_CELL_VOLTAGE),
										new UnsignedWordElement(r.offset + 0x106)),
								m(this.createChannelId(r, RackChannel.MIN_CELL_VOLTAGE_ID),
										new UnsignedWordElement(r.offset + 0x107)),
								m(this.createChannelId(r, RackChannel.MIN_CELL_VOLTAGE),
										new UnsignedWordElement(r.offset + 0x108)),
								m(this.createChannelId(r, RackChannel.MAX_CELL_TEMPERATURE_ID),
										new UnsignedWordElement(r.offset + 0x109)),
								m(this.createChannelId(r, RackChannel.MAX_CELL_TEMPERATURE),
										new SignedWordElement(r.offset + 0x10A), SCALE_FACTOR_MINUS_1),
								m(this.createChannelId(r, RackChannel.MIN_CELL_TEMPERATURE_ID),
										new UnsignedWordElement(r.offset + 0x10B)),
								m(this.createChannelId(r, RackChannel.MIN_CELL_TEMPERATURE),
										new SignedWordElement(r.offset + 0x10C), SCALE_FACTOR_MINUS_1),
								m(this.createChannelId(r, RackChannel.AVERAGE_VOLTAGE),
										new UnsignedWordElement(r.offset + 0x10D)),
								m(this.createChannelId(r, RackChannel.SYSTEM_INSULATION),
										new UnsignedWordElement(r.offset + 0x10E)),
								m(this.createChannelId(r, RackChannel.SYSTEM_MAX_CHARGE_CURRENT),
										new UnsignedWordElement(r.offset + 0x10F), SCALE_FACTOR_2),
								m(this.createChannelId(r, RackChannel.SYSTEM_MAX_DISCHARGE_CURRENT),
										new UnsignedWordElement(r.offset + 0x110), SCALE_FACTOR_2),
								m(this.createChannelId(r, RackChannel.POSITIVE_INSULATION),
										new UnsignedWordElement(r.offset + 0x111)),
								m(this.createChannelId(r, RackChannel.NEGATIVE_INSULATION),
										new UnsignedWordElement(r.offset + 0x112)),
								m(this.createChannelId(r, RackChannel.CLUSTER_RUN_STATE),
										new UnsignedWordElement(r.offset + 0x113)),
								m(this.createChannelId(r, RackChannel.AVG_TEMPERATURE),
										new SignedWordElement(r.offset + 0x114))),
						new FC3ReadRegistersTask(r.offset + 0x18b, Priority.LOW,
								m(this.createChannelId(r, RackChannel.PROJECT_ID),
										new UnsignedWordElement(r.offset + 0x18b)),
								m(this.createChannelId(r, RackChannel.VERSION_MAJOR),
										new UnsignedWordElement(r.offset + 0x18c)),
								m(this.createChannelId(r, RackChannel.VERSION_SUB),
										new UnsignedWordElement(r.offset + 0x18d)),
								m(this.createChannelId(r, RackChannel.VERSION_MODIFY),
										new UnsignedWordElement(r.offset + 0x18e))),

						// System Warning/Shut Down Status Registers
						new FC3ReadRegistersTask(r.offset + 0x140, Priority.LOW,
								// Level 2 Alarm: BMS Self-protect, main contactor shut down
								m(new BitsWordElement(r.offset + 0x140, this) //
										.bit(0, this.createChannelId(r, RackChannel.LEVEL2_CELL_VOLTAGE_HIGH)) //
										.bit(1, this.createChannelId(r, RackChannel.LEVEL2_TOTAL_VOLTAGE_HIGH)) //
										.bit(2, this.createChannelId(r, RackChannel.LEVEL2_CHARGE_CURRENT_HIGH)) //
										.bit(3, this.createChannelId(r, RackChannel.LEVEL2_CELL_VOLTAGE_LOW)) //
										.bit(4, this.createChannelId(r, RackChannel.LEVEL2_TOTAL_VOLTAGE_LOW)) //
										.bit(5, this.createChannelId(r, RackChannel.LEVEL2_DISCHARGE_CURRENT_HIGH)) //
										.bit(6, this.createChannelId(r, RackChannel.LEVEL2_CHARGE_TEMP_HIGH)) //
										.bit(7, this.createChannelId(r, RackChannel.LEVEL2_CHARGE_TEMP_LOW)) //
										// 8 -> Reserved
										// 9 -> Reserved
										.bit(10, this.createChannelId(r, RackChannel.LEVEL2_POWER_POLE_TEMP_HIGH)) //
										// 11 -> Reserved
										.bit(12, this.createChannelId(r, RackChannel.LEVEL2_INSULATION_VALUE)) //
										// 13 -> Reserved
										.bit(14, this.createChannelId(r, RackChannel.LEVEL2_DISCHARGE_TEMP_HIGH)) //
										.bit(15, this.createChannelId(r, RackChannel.LEVEL2_DISCHARGE_TEMP_LOW)) //
								),
								// Level 1 Alarm: EMS Control to stop charge, discharge, charge&discharge
								m(new BitsWordElement(r.offset + 0x141, this) //
										.bit(0, this.createChannelId(r, RackChannel.LEVEL1_CELL_VOLTAGE_HIGH)) //
										.bit(1, this.createChannelId(r, RackChannel.LEVEL1_TOTAL_VOLTAGE_HIGH)) //
										.bit(2, this.createChannelId(r, RackChannel.LEVEL1_CHARGE_CURRENT_HIGH)) //
										.bit(3, this.createChannelId(r, RackChannel.LEVEL1_CELL_VOLTAGE_LOW)) //
										.bit(4, this.createChannelId(r, RackChannel.LEVEL1_TOTAL_VOLTAGE_LOW)) //
										.bit(5, this.createChannelId(r, RackChannel.LEVEL1_DISCHARGE_CURRENT_HIGH)) //
										.bit(6, this.createChannelId(r, RackChannel.LEVEL1_CHARGE_TEMP_HIGH)) //
										.bit(7, this.createChannelId(r, RackChannel.LEVEL1_CHARGE_TEMP_LOW)) //
										.bit(8, this.createChannelId(r, RackChannel.LEVEL1_SOC_LOW)) //
										.bit(9, this.createChannelId(r, RackChannel.LEVEL1_TEMP_DIFF_TOO_BIG)) //
										.bit(10, this.createChannelId(r, RackChannel.LEVEL1_POWER_POLE_TEMP_HIGH)) //
										.bit(11, this.createChannelId(r, RackChannel.LEVEL1_CELL_VOLTAGE_DIFF_TOO_BIG)) //
										.bit(12, this.createChannelId(r, RackChannel.LEVEL1_INSULATION_VALUE)) //
										.bit(13, this.createChannelId(r, RackChannel.LEVEL1_TOTAL_VOLTAGE_DIFF_TOO_BIG)) //
										.bit(14, this.createChannelId(r, RackChannel.LEVEL1_DISCHARGE_TEMP_HIGH)) //
										.bit(15, this.createChannelId(r, RackChannel.LEVEL1_DISCHARGE_TEMP_LOW)) //
								),
								// Pre-Alarm: Temperature Alarm will active current limication
								m(new BitsWordElement(r.offset + 0x142, this) //
										.bit(0, this.createChannelId(r, RackChannel.PRE_ALARM_CELL_VOLTAGE_HIGH)) //
										.bit(1, this.createChannelId(r, RackChannel.PRE_ALARM_TOTAL_VOLTAGE_HIGH)) //
										.bit(2, this.createChannelId(r, RackChannel.PRE_ALARM_CHARGE_CURRENT_HIGH)) //
										.bit(3, this.createChannelId(r, RackChannel.PRE_ALARM_CELL_VOLTAGE_LOW)) //
										.bit(4, this.createChannelId(r, RackChannel.PRE_ALARM_TOTAL_VOLTAGE_LOW)) //
										.bit(5, this.createChannelId(r, RackChannel.PRE_ALARM_DISCHARGE_CURRENT_HIGH)) //
										.bit(6, this.createChannelId(r, RackChannel.PRE_ALARM_CHARGE_TEMP_HIGH)) //
										.bit(7, this.createChannelId(r, RackChannel.PRE_ALARM_CHARGE_TEMP_LOW)) //
										.bit(8, this.createChannelId(r, RackChannel.PRE_ALARM_SOC_LOW)) //
										.bit(9, this.createChannelId(r, RackChannel.PRE_ALARM_TEMP_DIFF_TOO_BIG)) //
										.bit(10, this.createChannelId(r, RackChannel.PRE_ALARM_POWER_POLE_HIGH))//
										.bit(11, this.createChannelId(r,
												RackChannel.PRE_ALARM_CELL_VOLTAGE_DIFF_TOO_BIG)) //
										.bit(12, this.createChannelId(r, RackChannel.PRE_ALARM_INSULATION_FAIL)) //
										.bit(13, this.createChannelId(r,
												RackChannel.PRE_ALARM_TOTAL_VOLTAGE_DIFF_TOO_BIG)) //
										.bit(14, this.createChannelId(r, RackChannel.PRE_ALARM_DISCHARGE_TEMP_HIGH)) //
										.bit(15, this.createChannelId(r, RackChannel.PRE_ALARM_DISCHARGE_TEMP_LOW)) //
								) //
						),
						// Other Alarm Info
						new FC3ReadRegistersTask(r.offset + 0x1A5, Priority.LOW, //
								m(new BitsWordElement(r.offset + 0x1A5, this) //
										.bit(0, this.createChannelId(r, RackChannel.ALARM_COMMUNICATION_TO_MASTER_BMS)) //
										.bit(1, this.createChannelId(r, RackChannel.ALARM_COMMUNICATION_TO_SLAVE_BMS)) //
										.bit(2, this.createChannelId(r,
												RackChannel.ALARM_COMMUNICATION_SLAVE_BMS_TO_TEMP_SENSORS)) //
										.bit(3, this.createChannelId(r, RackChannel.ALARM_SLAVE_BMS_HARDWARE)) //
								)),
						// Slave BMS Fault Message Registers
						new FC3ReadRegistersTask(r.offset + 0x185, Priority.LOW, //
								m(new BitsWordElement(r.offset + 0x185, this) //
										.bit(0, this.createChannelId(r, RackChannel.SLAVE_BMS_VOLTAGE_SENSOR_CABLES)) //
										.bit(1, this.createChannelId(r, RackChannel.SLAVE_BMS_POWER_CABLE)) //
										.bit(2, this.createChannelId(r, RackChannel.SLAVE_BMS_LTC6803)) //
										.bit(3, this.createChannelId(r, RackChannel.SLAVE_BMS_VOLTAGE_SENSORS)) //
										.bit(4, this.createChannelId(r, RackChannel.SLAVE_BMS_TEMP_SENSOR_CABLES)) //
										.bit(5, this.createChannelId(r, RackChannel.SLAVE_BMS_TEMP_SENSORS)) //
										.bit(6, this.createChannelId(r, RackChannel.SLAVE_BMS_POWER_POLE_TEMP_SENSOR)) //
										.bit(7, this.createChannelId(r, RackChannel.SLAVE_BMS_TEMP_BOARD_COM)) //
										.bit(8, this.createChannelId(r, RackChannel.SLAVE_BMS_BALANCE_MODULE)) //
										.bit(9, this.createChannelId(r, RackChannel.SLAVE_BMS_TEMP_SENSORS2)) //
										.bit(10, this.createChannelId(r, RackChannel.SLAVE_BMS_INTERNAL_COM)) //
										.bit(11, this.createChannelId(r, RackChannel.SLAVE_BMS_EEPROM)) //
										.bit(12, this.createChannelId(r, RackChannel.SLAVE_BMS_INIT)) //
								)) //
				);
			} catch (OpenemsException e) {
				this.logError(this.log, "Error while creating modbus tasks: " + e.getMessage());
				e.printStackTrace();
			} //
			Consumer<CellChannelFactory.Type> addCellChannels = type -> {
				for (var i = 0; i < numberOfModules; i++) {
					var elements = new AbstractModbusElement<?>[type.getSensorsPerModule()];
					for (var j = 0; j < type.getSensorsPerModule(); j++) {
						var sensorIndex = i * type.getSensorsPerModule() + j;
						var channelId = CellChannelFactory.create(r, type, sensorIndex);
						// Register the Channel at this Component
						this.addChannel(channelId);
						// Add the Modbus Element and map it to the Channel
						elements[j] = m(channelId, new UnsignedWordElement(r.offset + type.getOffset() + sensorIndex));
					}
					// Add a Modbus read task for this module
					try {
						this.getModbusProtocol().addTasks(//
								new FC3ReadRegistersTask(r.offset + type.getOffset() + i * type.getSensorsPerModule(),
										Priority.LOW, elements));
					} catch (OpenemsException e) {
						this.logError(this.log, "Error while creating modbus tasks: " + e.getMessage());
						e.printStackTrace();
					}
				}
			};
			addCellChannels.accept(CellChannelFactory.Type.VOLTAGE_CLUSTER);
			addCellChannels.accept(CellChannelFactory.Type.TEMPERATURE_CLUSTER);

			// WARN_LEVEL_Pre Alarm (Pre Alarm configuration registers RW)
			{
				AbstractModbusElement<?>[] elements = {
						m(this.createChannelId(r, RackChannel.PRE_ALARM_CELL_OVER_VOLTAGE_ALARM),
								new UnsignedWordElement(r.offset + 0x080)), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_CELL_OVER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x081)), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_SYSTEM_OVER_VOLTAGE_ALARM),
								new UnsignedWordElement(r.offset + 0x082), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_SYSTEM_OVER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x083), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_SYSTEM_CHARGE_OVER_CURRENT_ALARM),
								new UnsignedWordElement(r.offset + 0x084), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_SYSTEM_CHARGE_OVER_CURRENT_RECOVER),
								new UnsignedWordElement(r.offset + 0x085), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_CELL_UNDER_VOLTAGE_ALARM),
								new UnsignedWordElement(r.offset + 0x086)), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_CELL_UNDER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x087)), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_SYSTEM_UNDER_VOLTAGE_ALARM),
								new UnsignedWordElement(r.offset + 0x088), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_SYSTEM_UNDER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x089), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_SYSTEM_DISCHARGE_OVER_CURRENT_ALARM),
								new UnsignedWordElement(r.offset + 0x08A), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER),
								new UnsignedWordElement(r.offset + 0x08B), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_CELL_OVER_TEMPERATURE_ALARM),
								new SignedWordElement(r.offset + 0x08C)), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_CELL_OVER_TEMPERATURE_RECOVER),
								new SignedWordElement(r.offset + 0x08D)), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_CELL_UNDER_TEMPERATURE_ALARM),
								new SignedWordElement(r.offset + 0x08E)), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_CELL_UNDER_TEMPERATURE_RECOVER),
								new SignedWordElement(r.offset + 0x08F)), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_SOC_LOW_ALARM),
								new UnsignedWordElement(r.offset + 0x090)), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_SOC_LOW_ALARM_RECOVER),
								new UnsignedWordElement(r.offset + 0x091)), //
						new DummyRegisterElement(r.offset + 0x092, r.offset + 0x093),
						m(this.createChannelId(r, RackChannel.PRE_ALARM_CONNECTOR_TEMPERATURE_HIGH_ALARM),
								new SignedWordElement(r.offset + 0x094)), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_CONNECTOR_TEMPERATURE_HIGH_ALARM_RECOVER),
								new SignedWordElement(r.offset + 0x095)), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_INSULATION_ALARM),
								new UnsignedWordElement(r.offset + 0x096)), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_INSULATION_ALARM_RECOVER),
								new UnsignedWordElement(r.offset + 0x097)), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_CELL_VOLTAGE_DIFFERENCE_ALARM),
								new UnsignedWordElement(r.offset + 0x098)), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_CELL_VOLTAGE_DIFFERENCE_ALARM_RECOVER),
								new UnsignedWordElement(r.offset + 0x099)), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_TOTAL_VOLTAGE_DIFFERENCE_ALARM),
								new UnsignedWordElement(r.offset + 0x09A), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_TOTAL_VOLTAGE_DIFFERENCE_ALARM_RECOVER),
								new UnsignedWordElement(r.offset + 0x09B), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_DISCHARGE_TEMPERATURE_HIGH_ALARM),
								new SignedWordElement(r.offset + 0x09C)), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_DISCHARGE_TEMPERATURE_HIGH_ALARM_RECOVER),
								new SignedWordElement(r.offset + 0x09D)), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_DISCHARGE_TEMPERATURE_LOW_ALARM),
								new SignedWordElement(r.offset + 0x09E)), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_DISCHARGE_TEMPERATURE_LOW_ALARM_RECOVER),
								new SignedWordElement(r.offset + 0x09F)), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_TEMPERATURE_DIFFERENCE_ALARM),
								new SignedWordElement(r.offset + 0x0A0)), //
						m(this.createChannelId(r, RackChannel.PRE_ALARM_TEMPERATURE_DIFFERENCE_ALARM_RECOVER),
								new SignedWordElement(r.offset + 0x0A1)) //
				};
				this.getModbusProtocol().addTasks(//
						new FC16WriteRegistersTask(r.offset + 0x080, elements));
				this.getModbusProtocol().addTasks(//
						new FC3ReadRegistersTask(r.offset + 0x080, Priority.LOW, elements));
			}

			// WARN_LEVEL1 (Level1 warning registers RW)
			{
				AbstractModbusElement<?>[] elements = {
						m(this.createChannelId(r, RackChannel.LEVEL1_CELL_OVER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x040)), //
						m(this.createChannelId(r, RackChannel.LEVEL1_CELL_OVER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x041)), //
						m(this.createChannelId(r, RackChannel.LEVEL1_SYSTEM_OVER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x042), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.LEVEL1_SYSTEM_OVER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x043), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.LEVEL1_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION),
								new UnsignedWordElement(r.offset + 0x044), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.LEVEL1_SYSTEM_CHARGE_OVER_CURRENT_RECOVER),
								new UnsignedWordElement(r.offset + 0x045), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.LEVEL1_CELL_UNDER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x046)), //
						m(this.createChannelId(r, RackChannel.LEVEL1_CELL_UNDER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x047)), //
						m(this.createChannelId(r, RackChannel.LEVEL1_SYSTEM_UNDER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x048), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.LEVEL1_SYSTEM_UNDER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x049), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.LEVEL1_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION),
								new UnsignedWordElement(r.offset + 0x04A), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.LEVEL1_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER),
								new UnsignedWordElement(r.offset + 0x04B), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.LEVEL1_CELL_OVER_TEMPERATURE_PROTECTION),
								new SignedWordElement(r.offset + 0x04C)), //
						m(this.createChannelId(r, RackChannel.LEVEL1_CELL_OVER_TEMPERATURE_RECOVER),
								new SignedWordElement(r.offset + 0x04D)), //
						m(this.createChannelId(r, RackChannel.LEVEL1_CELL_UNDER_TEMPERATURE_PROTECTION),
								new SignedWordElement(r.offset + 0x04E)), //
						m(this.createChannelId(r, RackChannel.LEVEL1_CELL_UNDER_TEMPERATURE_RECOVER),
								new SignedWordElement(r.offset + 0x04F)), //
						m(this.createChannelId(r, RackChannel.LEVEL1_SOC_LOW_PROTECTION),
								new UnsignedWordElement(r.offset + 0x050)), //
						m(this.createChannelId(r, RackChannel.LEVEL1_SOC_LOW_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x051)), //
						new DummyRegisterElement(r.offset + 0x052, r.offset + 0x053), //
						m(this.createChannelId(r, RackChannel.LEVEL1_CONNECTOR_TEMPERATURE_HIGH_PROTECTION),
								new SignedWordElement(r.offset + 0x054)), //
						m(this.createChannelId(r, RackChannel.LEVEL1_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER),
								new SignedWordElement(r.offset + 0x055)), //
						m(this.createChannelId(r, RackChannel.LEVEL1_INSULATION_PROTECTION),
								new UnsignedWordElement(r.offset + 0x056)), //
						m(this.createChannelId(r, RackChannel.LEVEL1_INSULATION_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x057)), //
						m(this.createChannelId(r, RackChannel.LEVEL1_CELL_VOLTAGE_DIFFERENCE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x058)), //
						m(this.createChannelId(r, RackChannel.LEVEL1_CELL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x059)), //
						m(this.createChannelId(r, RackChannel.LEVEL1_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x05A), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.LEVEL1_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x05B), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.LEVEL1_DISCHARGE_TEMPERATURE_HIGH_PROTECTION),
								new SignedWordElement(r.offset + 0x05C)), //
						m(this.createChannelId(r, RackChannel.LEVEL1_DISCHARGE_TEMPERATURE_HIGH_PROTECTION_RECOVER),
								new SignedWordElement(r.offset + 0x05D)), //
						m(this.createChannelId(r, RackChannel.LEVEL1_DISCHARGE_TEMPERATURE_LOW_PROTECTION),
								new SignedWordElement(r.offset + 0x05E)), //
						m(this.createChannelId(r, RackChannel.LEVEL1_DISCHARGE_TEMPERATURE_LOW_PROTECTION_RECOVER),
								new SignedWordElement(r.offset + 0x05F)), //
						m(this.createChannelId(r, RackChannel.LEVEL1_TEMPERATURE_DIFFERENCE_PROTECTION),
								new SignedWordElement(r.offset + 0x060)), //
						m(this.createChannelId(r, RackChannel.LEVEL1_TEMPERATURE_DIFFERENCE_PROTECTION_RECOVER),
								new SignedWordElement(r.offset + 0x061)) //
				};
				this.getModbusProtocol().addTasks(//
						new FC16WriteRegistersTask(r.offset + 0x040, elements));
				this.getModbusProtocol().addTasks(//
						new FC3ReadRegistersTask(r.offset + 0x040, Priority.LOW, elements));
			}

			// WARN_LEVEL2 (Level2 Protection registers RW)
			{
				AbstractModbusElement<?>[] elements = {
						m(this.createChannelId(r, RackChannel.LEVEL2_CELL_OVER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x400)), //
						m(this.createChannelId(r, RackChannel.LEVEL2_CELL_OVER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x401)), //
						m(this.createChannelId(r, RackChannel.LEVEL2_SYSTEM_OVER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x402)), //
						m(this.createChannelId(r, RackChannel.LEVEL2_SYSTEM_OVER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x403), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.LEVEL2_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION),
								new UnsignedWordElement(r.offset + 0x404), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.LEVEL2_SYSTEM_CHARGE_OVER_CURRENT_RECOVER),
								new UnsignedWordElement(r.offset + 0x405), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.LEVEL2_CELL_UNDER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x406)), //
						m(this.createChannelId(r, RackChannel.LEVEL2_CELL_UNDER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x407)), //
						m(this.createChannelId(r, RackChannel.LEVEL2_SYSTEM_UNDER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x408), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.LEVEL2_SYSTEM_UNDER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x409), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.LEVEL2_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION),
								new UnsignedWordElement(r.offset + 0x40A), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.LEVEL2_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER),
								new UnsignedWordElement(r.offset + 0x40B), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.LEVEL2_CELL_OVER_TEMPERATURE_PROTECTION),
								new SignedWordElement(r.offset + 0x40C)), //
						m(this.createChannelId(r, RackChannel.LEVEL2_CELL_OVER_TEMPERATURE_RECOVER),
								new SignedWordElement(r.offset + 0x40D)), //
						m(this.createChannelId(r, RackChannel.LEVEL2_CELL_UNDER_TEMPERATURE_PROTECTION),
								new SignedWordElement(r.offset + 0x40E)), //
						m(this.createChannelId(r, RackChannel.LEVEL2_CELL_UNDER_TEMPERATURE_RECOVER),
								new SignedWordElement(r.offset + 0x40F)), //
						m(this.createChannelId(r, RackChannel.LEVEL2_SOC_LOW_PROTECTION),
								new UnsignedWordElement(r.offset + 0x410)), //
						m(this.createChannelId(r, RackChannel.LEVEL2_SOC_LOW_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x411)), //
						new DummyRegisterElement(r.offset + 0x412, r.offset + 0x413), //
						m(this.createChannelId(r, RackChannel.LEVEL2_CONNECTOR_TEMPERATURE_HIGH_PROTECTION),
								new SignedWordElement(r.offset + 0x414)), //
						m(this.createChannelId(r, RackChannel.LEVEL2_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER),
								new SignedWordElement(r.offset + 0x415)), //
						m(this.createChannelId(r, RackChannel.LEVEL2_INSULATION_PROTECTION),
								new UnsignedWordElement(r.offset + 0x416)), //
						m(this.createChannelId(r, RackChannel.LEVEL2_INSULATION_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x417)), //
						m(this.createChannelId(r, RackChannel.LEVEL2_CELL_VOLTAGE_DIFFERENCE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x418)), //
						m(this.createChannelId(r, RackChannel.LEVEL2_CELL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x419)), //
						m(this.createChannelId(r, RackChannel.LEVEL2_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x41A), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.LEVEL2_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x41B), SCALE_FACTOR_2), //
						m(this.createChannelId(r, RackChannel.LEVEL2_DISCHARGE_TEMPERATURE_HIGH_PROTECTION),
								new SignedWordElement(r.offset + 0x41C)), //
						m(this.createChannelId(r, RackChannel.LEVEL2_DISCHARGE_TEMPERATURE_HIGH_PROTECTION_RECOVER),
								new SignedWordElement(r.offset + 0x41D)), //
						m(this.createChannelId(r, RackChannel.LEVEL2_DISCHARGE_TEMPERATURE_LOW_PROTECTION),
								new SignedWordElement(r.offset + 0x41E)), //
						m(this.createChannelId(r, RackChannel.LEVEL2_DISCHARGE_TEMPERATURE_LOW_PROTECTION_RECOVER),
								new SignedWordElement(r.offset + 0x41F)), //
						m(this.createChannelId(r, RackChannel.LEVEL2_TEMPERATURE_DIFFERENCE_PROTECTION),
								new SignedWordElement(r.offset + 0x420)), //
						m(this.createChannelId(r, RackChannel.LEVEL2_TEMPERATURE_DIFFERENCE_PROTECTION_RECOVER),
								new SignedWordElement(r.offset + 0x421)) //
				};
				this.getModbusProtocol().addTasks(//
						new FC16WriteRegistersTask(r.offset + 0x400, elements));
				this.getModbusProtocol().addTasks(//
						new FC3ReadRegistersTask(r.offset + 0x400, Priority.LOW, elements));
			}

		}
	}

	private void initializeBatteryLimits(int numberOfModules) {
		// Initialize Battery Limits
		this._setChargeMaxCurrent(0 /* default value 0 to avoid damages */);
		this._setDischargeMaxCurrent(0 /* default value 0 to avoid damages */);
		this._setChargeMaxVoltage(numberOfModules * Constants.MAX_VOLTAGE_MILLIVOLT_PER_MODULE / 1000);
		this._setDischargeMinVoltage(numberOfModules * Constants.MIN_VOLTAGE_MILLIVOLT_PER_MODULE / 1000);
	}

	/**
	 * Calculates the Capacity as Capacity per module multiplied with number of
	 * modules and sets the CAPACITY channel.
	 *
	 * @param numberOfTowers  the number of battery towers
	 * @param numberOfModules the number of battery modules
	 */
	private void calculateCapacity(int numberOfTowers, int numberOfModules) {
		var capacity = numberOfTowers * numberOfModules * ModuleType.MODULE_3_5_KWH.getCapacity_Wh();
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
			ModbusUtils
					.readELementOnce(this.getModbusProtocol(),
							new UnsignedWordElement(0x20C1 /* No of modules for 1st tower */), true)
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
	 * Recursively reads the 'No of modules' register of each tower. Eventually
	 * completes the {@link CompletableFuture}.
	 *
	 * @param result              the {@link CompletableFuture}
	 * @param totalNumberOfTowers the recursively incremented total number of towers
	 * @param addresses           Queue with the remaining 'No of modules' registers
	 * @param tryAgainOnError     if true, tries to read till it receives a value;
	 *                            if false, stops after first try and possibly
	 *                            return null
	 */
	private void checkNumberOfTowers(CompletableFuture<Integer> result, int totalNumberOfTowers,
			final Queue<Integer> addresses, boolean tryAgainOnError) {
		final var address = addresses.poll();

		if (address == null) {
			// Finished Queue
			result.complete(totalNumberOfTowers);
			return;
		}

		try {
			// Read next address in Queue
			ModbusUtils.readELementOnce(this.getModbusProtocol(), new UnsignedWordElement(address), tryAgainOnError)
					.thenAccept(numberOfModules -> {
						if (numberOfModules == null) {
							if (tryAgainOnError) {
								// Try again
								return;
							}
							// Read error -> this tower does not exist. Stop here.
							result.complete(totalNumberOfTowers);
							return;
						}

						// Read successful -> try to read next tower
						this.checkNumberOfTowers(result, totalNumberOfTowers + 1, addresses, false);
					});
		} catch (OpenemsException e) {
			e.printStackTrace();
			result.completeExceptionally(e);
			return;
		}
	}

	private CompletableFuture<Integer> getNumberOfTowers() throws OpenemsException {
		final var result = new CompletableFuture<Integer>();

		Queue<Integer> addresses = new LinkedList<>();
		addresses.add(0x20C1 /* No of modules for 1st tower */);
		addresses.add(0x30C1 /* No of modules for 2nd tower */);
		addresses.add(0x40C1 /* No of modules for 3rd tower */);
		addresses.add(0x50C1 /* No of modules for 4th tower */);
		addresses.add(0x60C1 /* No of modules for 5th tower */);

		this.checkNumberOfTowers(result, 0, addresses, true);

		return result;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {

		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			this.updateChannels();
			this.batteryProtection.apply();
		}

		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE ->
			this.handleStateMachine();
		
		}
	}

	/**
	 * Handles the State-Machine.
	 */
	private void handleStateMachine() {
		// Store the current State
		this.channel(BatterySoltaroClusterVersionC.ChannelId.STATE_MACHINE)
				.setNextValue(this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		// Prepare Context
		var context = new Context(this, this.config);

		// Call the StateMachine
		try {
			this.stateMachine.run(context);

			this.channel(BatterySoltaroClusterVersionC.ChannelId.RUN_FAILED).setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(BatterySoltaroClusterVersionC.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	@Override
	public String debugLog() {
		return new StringBuilder() //
				.append(this.stateMachine.debugLog()) //
				.append("|SoC:").append(this.getSoc()) //
				.append("|Actual:").append(this.getVoltage()) //
				.append(";").append(this.getCurrent()) //
				.append("|Charge:").append(this.getChargeMaxVoltage()) //
				.append(";").append(this.getChargeMaxCurrent()) //
				.append("|Discharge:").append(this.getDischargeMinVoltage()) //
				.append(";").append(this.getDischargeMaxCurrent()) //
				.toString();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this,
				/*
				 * BMS Control Registers
				 */
				new FC16WriteRegistersTask(0x1024,
						m(BatterySoltaroClusterVersionC.ChannelId.EMS_COMMUNICATION_TIMEOUT,
								new UnsignedWordElement(0x1024)), //
						m(BatterySoltaroClusterVersionC.ChannelId.EMS_ADDRESS, new UnsignedWordElement(0x1025)), //
						m(BatterySoltaroClusterVersionC.ChannelId.EMS_BAUDRATE, new UnsignedWordElement(0x1026))), //
				new FC3ReadRegistersTask(0x1024, Priority.LOW,
						m(BatterySoltaroClusterVersionC.ChannelId.EMS_COMMUNICATION_TIMEOUT,
								new UnsignedWordElement(0x1024)), //
						m(BatterySoltaroClusterVersionC.ChannelId.EMS_ADDRESS, new UnsignedWordElement(0x1025)), //
						m(BatterySoltaroClusterVersionC.ChannelId.EMS_BAUDRATE, new UnsignedWordElement(0x1026))), //
				new FC16WriteRegistersTask(0x10C3, //
						m(SoltaroCluster.ChannelId.CLUSTER_START_STOP, new UnsignedWordElement(0x10C3)), //
						m(SoltaroCluster.ChannelId.RACK_1_USAGE, new UnsignedWordElement(0x10C4)), //
						m(SoltaroCluster.ChannelId.RACK_2_USAGE, new UnsignedWordElement(0x10C5)), //
						m(SoltaroCluster.ChannelId.RACK_3_USAGE, new UnsignedWordElement(0x10C6)), //
						m(SoltaroCluster.ChannelId.RACK_4_USAGE, new UnsignedWordElement(0x10C7)), //
						m(SoltaroCluster.ChannelId.RACK_5_USAGE, new UnsignedWordElement(0x10C8))), //
				new FC3ReadRegistersTask(0x10C3, Priority.LOW,
						m(SoltaroCluster.ChannelId.CLUSTER_START_STOP, new UnsignedWordElement(0x10C3)), //
						m(SoltaroCluster.ChannelId.RACK_1_USAGE, new UnsignedWordElement(0x10C4)), //
						m(SoltaroCluster.ChannelId.RACK_2_USAGE, new UnsignedWordElement(0x10C5)), //
						m(SoltaroCluster.ChannelId.RACK_3_USAGE, new UnsignedWordElement(0x10C6)), //
						m(SoltaroCluster.ChannelId.RACK_4_USAGE, new UnsignedWordElement(0x10C7)), //
						m(SoltaroCluster.ChannelId.RACK_5_USAGE, new UnsignedWordElement(0x10C8))), //
				/*
				 * BMS System Running Status Registers
				 */
				new FC3ReadRegistersTask(0x1044, Priority.HIGH, //
						m(SoltaroCluster.ChannelId.CHARGE_INDICATION, new UnsignedWordElement(0x1044)), //
						m(Battery.ChannelId.CURRENT, new UnsignedWordElement(0x1045), //
								SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(0x1046), //
						m(BatterySoltaroClusterVersionC.ChannelId.ORIGINAL_SOC, new UnsignedWordElement(0x1047)), //
						m(SoltaroCluster.ChannelId.SYSTEM_RUNNING_STATE, new UnsignedWordElement(0x1048)), //
						m(Battery.ChannelId.VOLTAGE, new UnsignedWordElement(0x1049), //
								SCALE_FACTOR_MINUS_1), //
						m(SoltaroCluster.ChannelId.SYSTEM_INSULATION, new UnsignedWordElement(0x104A)), //
						new DummyRegisterElement(0x104B, 0x104D), //
						m(BatteryProtection.ChannelId.BP_CHARGE_BMS, new UnsignedWordElement(0x104E),
								SCALE_FACTOR_MINUS_1), //
						m(BatteryProtection.ChannelId.BP_DISCHARGE_BMS, new UnsignedWordElement(0x104F),
								SCALE_FACTOR_MINUS_1)), //
				new FC3ReadRegistersTask(0x1081, Priority.LOW, //
						m(new BitsWordElement(0x1081, this) //
								.bit(0, BatterySoltaroClusterVersionC.ChannelId.MASTER_PCS_COMMUNICATION_FAILURE) //
								.bit(1, BatterySoltaroClusterVersionC.ChannelId.MASTER_PCS_CONTROL_FAILURE) //
								.bit(2, BatterySoltaroClusterVersionC.ChannelId.MASTER_EMS_COMMUNICATION_FAILURE) //
						), //
						m(new BitsWordElement(0x1082, this) //
								.bit(0, SoltaroCluster.ChannelId.SUB_MASTER_1_COMMUNICATION_FAILURE) //
								.bit(1, SoltaroCluster.ChannelId.SUB_MASTER_2_COMMUNICATION_FAILURE) //
								.bit(2, SoltaroCluster.ChannelId.SUB_MASTER_3_COMMUNICATION_FAILURE) //
								.bit(3, SoltaroCluster.ChannelId.SUB_MASTER_4_COMMUNICATION_FAILURE) //
								.bit(4, SoltaroCluster.ChannelId.SUB_MASTER_5_COMMUNICATION_FAILURE) //
						), //
						m(new BitsWordElement(0x1083, this) //
								.bit(0, BatterySoltaroClusterVersionC.ChannelId.RACK_1_VOLTAGE_DIFFERENCE) //
								.bit(1, BatterySoltaroClusterVersionC.ChannelId.RACK_1_OVER_CURRENT) //
								.bit(2, BatterySoltaroClusterVersionC.ChannelId.RACK_1_HARDWARE_FAILURE) //
								.bit(3, BatterySoltaroClusterVersionC.ChannelId.RACK_1_COMMUNICATION_TO_MASTER_FAILURE) //
								.bit(4, BatterySoltaroClusterVersionC.ChannelId.RACK_1_PCS_CONTROL_FAILURE) //
								.bit(5, BatterySoltaroClusterVersionC.ChannelId.RACK_1_LEVEL_2_ALARM) //
						), //
						m(new BitsWordElement(0x1084, this) //
								.bit(0, BatterySoltaroClusterVersionC.ChannelId.RACK_2_VOLTAGE_DIFFERENCE) //
								.bit(1, BatterySoltaroClusterVersionC.ChannelId.RACK_2_OVER_CURRENT) //
								.bit(2, BatterySoltaroClusterVersionC.ChannelId.RACK_2_HARDWARE_FAILURE) //
								.bit(3, BatterySoltaroClusterVersionC.ChannelId.RACK_2_COMMUNICATION_TO_MASTER_FAILURE) //
								.bit(4, BatterySoltaroClusterVersionC.ChannelId.RACK_2_PCS_CONTROL_FAILURE) //
								.bit(5, BatterySoltaroClusterVersionC.ChannelId.RACK_2_LEVEL_2_ALARM) //
						), //
						m(new BitsWordElement(0x1085, this) //
								.bit(0, BatterySoltaroClusterVersionC.ChannelId.RACK_3_VOLTAGE_DIFFERENCE) //
								.bit(1, BatterySoltaroClusterVersionC.ChannelId.RACK_3_OVER_CURRENT) //
								.bit(2, BatterySoltaroClusterVersionC.ChannelId.RACK_3_HARDWARE_FAILURE) //
								.bit(3, BatterySoltaroClusterVersionC.ChannelId.RACK_3_COMMUNICATION_TO_MASTER_FAILURE) //
								.bit(4, BatterySoltaroClusterVersionC.ChannelId.RACK_3_PCS_CONTROL_FAILURE) //
								.bit(5, BatterySoltaroClusterVersionC.ChannelId.RACK_3_LEVEL_2_ALARM) //
						), //
						m(new BitsWordElement(0x1086, this) //
								.bit(0, BatterySoltaroClusterVersionC.ChannelId.RACK_4_VOLTAGE_DIFFERENCE) //
								.bit(1, BatterySoltaroClusterVersionC.ChannelId.RACK_4_OVER_CURRENT) //
								.bit(2, BatterySoltaroClusterVersionC.ChannelId.RACK_4_HARDWARE_FAILURE) //
								.bit(3, BatterySoltaroClusterVersionC.ChannelId.RACK_4_COMMUNICATION_TO_MASTER_FAILURE) //
								.bit(4, BatterySoltaroClusterVersionC.ChannelId.RACK_4_PCS_CONTROL_FAILURE) //
								.bit(5, BatterySoltaroClusterVersionC.ChannelId.RACK_4_LEVEL_2_ALARM) //
						), //
						m(new BitsWordElement(0x1087, this) //
								.bit(0, BatterySoltaroClusterVersionC.ChannelId.RACK_5_VOLTAGE_DIFFERENCE) //
								.bit(1, BatterySoltaroClusterVersionC.ChannelId.RACK_5_OVER_CURRENT) //
								.bit(2, BatterySoltaroClusterVersionC.ChannelId.RACK_5_HARDWARE_FAILURE) //
								.bit(3, BatterySoltaroClusterVersionC.ChannelId.RACK_5_COMMUNICATION_TO_MASTER_FAILURE) //
								.bit(4, BatterySoltaroClusterVersionC.ChannelId.RACK_5_PCS_CONTROL_FAILURE) //
								.bit(5, BatterySoltaroClusterVersionC.ChannelId.RACK_5_LEVEL_2_ALARM) //
						), //
						new DummyRegisterElement(0x1088, 0x1092), //
						// Pre-Alarm Summary: Temperature Alarm can be used for current limitation,
						// while all other alarms are just for alarm. Note: Alarm for all clusters
						m(new BitsWordElement(0x1093, this) //
								.bit(0, BatterySoltaroClusterVersionC.ChannelId.PRE_ALARM_CELL_VOLTAGE_HIGH) //
								.bit(1, BatterySoltaroClusterVersionC.ChannelId.PRE_ALARM_TOTAL_VOLTAGE_HIGH) //
								.bit(2, BatterySoltaroClusterVersionC.ChannelId.PRE_ALARM_CHARGE_CURRENT_HIGH) //
								.bit(3, BatterySoltaroClusterVersionC.ChannelId.PRE_ALARM_CELL_VOLTAGE_LOW) //
								.bit(4, BatterySoltaroClusterVersionC.ChannelId.PRE_ALARM_TOTAL_VOLTAGE_LOW) //
								.bit(5, BatterySoltaroClusterVersionC.ChannelId.PRE_ALARM_DISCHARGE_CURRENT_HIGH) //
								.bit(6, BatterySoltaroClusterVersionC.ChannelId.PRE_ALARM_CHARGE_TEMP_HIGH) //
								.bit(7, BatterySoltaroClusterVersionC.ChannelId.PRE_ALARM_CHARGE_TEMP_LOW) //
								.bit(8, BatterySoltaroClusterVersionC.ChannelId.PRE_ALARM_SOC_LOW) //
								.bit(9, BatterySoltaroClusterVersionC.ChannelId.PRE_ALARM_TEMP_DIFF_TOO_BIG) //
								.bit(10, BatterySoltaroClusterVersionC.ChannelId.PRE_ALARM_POWER_POLE_HIGH) //
								.bit(11, BatterySoltaroClusterVersionC.ChannelId.PRE_ALARM_CELL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(12, BatterySoltaroClusterVersionC.ChannelId.PRE_ALARM_INSULATION_FAIL) //
								.bit(13, BatterySoltaroClusterVersionC.ChannelId.PRE_ALARM_TOTAL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(14, BatterySoltaroClusterVersionC.ChannelId.PRE_ALARM_DISCHARGE_TEMP_HIGH) //
								.bit(15, BatterySoltaroClusterVersionC.ChannelId.PRE_ALARM_DISCHARGE_TEMP_LOW)), //
						// Level 1 Alarm Summary
						m(new BitsWordElement(0x1094, this) //
								.bit(0, BatterySoltaroClusterVersionC.ChannelId.LEVEL1_CELL_VOLTAGE_HIGH) //
								.bit(1, BatterySoltaroClusterVersionC.ChannelId.LEVEL1_TOTAL_VOLTAGE_HIGH) //
								.bit(2, BatterySoltaroClusterVersionC.ChannelId.LEVEL1_CHARGE_CURRENT_HIGH) //
								.bit(3, BatterySoltaroClusterVersionC.ChannelId.LEVEL1_CELL_VOLTAGE_LOW) //
								.bit(4, BatterySoltaroClusterVersionC.ChannelId.LEVEL1_TOTAL_VOLTAGE_LOW) //
								.bit(5, BatterySoltaroClusterVersionC.ChannelId.LEVEL1_DISCHARGE_CURRENT_HIGH) //
								.bit(6, BatterySoltaroClusterVersionC.ChannelId.LEVEL1_CHARGE_TEMP_HIGH) //
								.bit(7, BatterySoltaroClusterVersionC.ChannelId.LEVEL1_CHARGE_TEMP_LOW) //
								.bit(8, BatterySoltaroClusterVersionC.ChannelId.LEVEL1_SOC_LOW) //
								.bit(9, BatterySoltaroClusterVersionC.ChannelId.LEVEL1_TEMP_DIFF_TOO_BIG) //
								.bit(10, BatterySoltaroClusterVersionC.ChannelId.LEVEL1_POWER_POLE_TEMP_HIGH) //
								.bit(11, BatterySoltaroClusterVersionC.ChannelId.LEVEL1_CELL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(12, BatterySoltaroClusterVersionC.ChannelId.LEVEL1_INSULATION_VALUE) //
								.bit(13, BatterySoltaroClusterVersionC.ChannelId.LEVEL1_TOTAL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(14, BatterySoltaroClusterVersionC.ChannelId.LEVEL1_DISCHARGE_TEMP_HIGH) //
								.bit(15, BatterySoltaroClusterVersionC.ChannelId.LEVEL1_DISCHARGE_TEMP_LOW)), //
						// Level 2 Alarm Summary
						m(new BitsWordElement(0x1095, this) //
								.bit(0, BatterySoltaroClusterVersionC.ChannelId.LEVEL2_CELL_VOLTAGE_HIGH) //
								.bit(1, BatterySoltaroClusterVersionC.ChannelId.LEVEL2_TOTAL_VOLTAGE_HIGH) //
								.bit(2, BatterySoltaroClusterVersionC.ChannelId.LEVEL2_CHARGE_CURRENT_HIGH) //
								.bit(3, BatterySoltaroClusterVersionC.ChannelId.LEVEL2_CELL_VOLTAGE_LOW) //
								.bit(4, BatterySoltaroClusterVersionC.ChannelId.LEVEL2_TOTAL_VOLTAGE_LOW) //
								.bit(5, BatterySoltaroClusterVersionC.ChannelId.LEVEL2_DISCHARGE_CURRENT_HIGH) //
								.bit(6, BatterySoltaroClusterVersionC.ChannelId.LEVEL2_CHARGE_TEMP_HIGH) //
								.bit(7, BatterySoltaroClusterVersionC.ChannelId.LEVEL2_CHARGE_TEMP_LOW) //
								.bit(8, BatterySoltaroClusterVersionC.ChannelId.LEVEL2_SOC_LOW) //
								.bit(9, BatterySoltaroClusterVersionC.ChannelId.LEVEL2_TEMP_DIFF_TOO_BIG) //
								.bit(10, BatterySoltaroClusterVersionC.ChannelId.LEVEL2_POWER_POLE_TEMP_HIGH) //
								.bit(11, BatterySoltaroClusterVersionC.ChannelId.LEVEL2_CELL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(12, BatterySoltaroClusterVersionC.ChannelId.LEVEL2_INSULATION_VALUE) //
								.bit(13, BatterySoltaroClusterVersionC.ChannelId.LEVEL2_TOTAL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(14, BatterySoltaroClusterVersionC.ChannelId.LEVEL2_DISCHARGE_TEMP_HIGH) //
								.bit(15, BatterySoltaroClusterVersionC.ChannelId.LEVEL2_DISCHARGE_TEMP_LOW)))); //
	}

	/**
	 * Factory-Function for SingleRack-ChannelIds. Creates a ChannelId, registers
	 * the Channel and returns the ChannelId.
	 *
	 * @param rack        the {@link Rack}
	 * @param rackChannel the {@link RackChannel}
	 * @return the {@link io.openems.edge.common.channel.ChannelId}
	 */
	private final io.openems.edge.common.channel.ChannelId createChannelId(Rack rack, RackChannel rackChannel) {
		@SuppressWarnings("deprecation")
		Channel<?> existingChannel = this._channel(rackChannel.toChannelIdString(rack));
		if (existingChannel != null) {
			return existingChannel.channelId();
		}
		var channelId = rackChannel.toChannelId(rack);
		this.addChannel(channelId);
		return channelId;
	}

	/**
	 * Updates Channels on BEFORE_PROCESS_IMAGE event.
	 */
	private void updateChannels() {
		this._setSoc(this.calculateRackAverage(RackChannel.SOC));
		this._setSoh(this.calculateRackAverage(RackChannel.SOH));
		this._setMaxCellVoltage(this.calculateRackMax(RackChannel.MAX_CELL_VOLTAGE));
		this._setMinCellVoltage(this.calculateRackMin(RackChannel.MIN_CELL_VOLTAGE));
		this._setMaxCellTemperature(this.calculateRackMax(RackChannel.MAX_CELL_TEMPERATURE));
		this._setMinCellTemperature(this.calculateRackMin(RackChannel.MIN_CELL_TEMPERATURE));
	}

	/**
	 * Calculates the average of RackChannel over all active Racks.
	 *
	 * @param rackChannel the {@link RackChannel}
	 * @return the average value or null
	 */
	private Integer calculateRackAverage(RackChannel rackChannel) {
		Integer cumulated = null;
		for (Rack rack : this.racks) {
			IntegerReadChannel channel = this.channel(rack, rackChannel);
			var value = channel.getNextValue().get();
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
		}
		return null;
	}

	/**
	 * Finds the maximum of a RackChannel over all active Racks.
	 *
	 * @param rackChannel the {@link RackChannel}
	 * @return the maximum value or null
	 */
	private Integer calculateRackMax(RackChannel rackChannel) {
		Integer result = null;
		for (Rack rack : this.racks) {
			IntegerReadChannel channel = this.channel(rack, rackChannel);
			var value = channel.getNextValue().get();
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
	 * @param rackChannel the {@link RackChannel}
	 * @return the minimum value or null
	 */
	private Integer calculateRackMin(RackChannel rackChannel) {
		Integer result = null;
		for (Rack rack : this.racks) {
			IntegerReadChannel channel = this.channel(rack, rackChannel);
			var value = channel.getNextValue().get();
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
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode) //
		);
	}

	@Override
	public <T extends Channel<?>> T channel(Rack rack, RackChannel rackChannel) {
		return this.channel(rackChannel.toChannelIdString(rack));
	}

	@Override
	public Optional<PreChargeControl> getCommonPreChargeControl() {
		PreChargeControl result = null;
		for (Rack rack : this.racks) {
			EnumReadChannel channel = this.channel(rack, RackChannel.PRE_CHARGE_CONTROL);
			PreChargeControl value = channel.value().asEnum();

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
		return Optional.ofNullable(result);
	}

	@Override
	public Set<Rack> getRacks() {
		return this.racks;
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
		return switch (this.config.startStop()) {
		case AUTO ->
			// read StartStop-Channel
			 this.startStopTarget.get();
		case START ->
			// force START
			 StartStop.START;
		case STOP ->
			// force STOP
			 StartStop.STOP;
		default -> {
			assert false;
			yield StartStop.UNDEFINED; // can never happen
		}
		};	
	}
	
}
