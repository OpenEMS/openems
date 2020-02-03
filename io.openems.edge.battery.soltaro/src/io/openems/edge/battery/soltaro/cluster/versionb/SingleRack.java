package io.openems.edge.battery.soltaro.cluster.versionb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.soltaro.ChannelIdImpl;
import io.openems.edge.battery.soltaro.ChargeIndication;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.taskmanager.Priority;

/**
 * Helper class that provides channels and channel ids for a multi rack channels
 * and ids are created dynamically depending on system configuration
 *
 */
public class SingleRack {

	private static final String KEY_VOLTAGE = "VOLTAGE";
	private static final String KEY_CURRENT = "CURRENT";
	private static final String KEY_CHARGE_INDICATION = "CHARGE_INDICATION";
	private static final String KEY_SOC = "SOC";
	private static final String KEY_SOH = "SOH";
	public static final String KEY_MAX_CELL_VOLTAGE_ID = "MAX_CELL_VOLTAGE_ID";
	public static final String KEY_MAX_CELL_VOLTAGE = "MAX_CELL_VOLTAGE";
	public static final String KEY_MIN_CELL_VOLTAGE_ID = "MIN_CELL_VOLTAGE_ID";
	public static final String KEY_MIN_CELL_VOLTAGE = "MIN_CELL_VOLTAGE";
	public static final String KEY_ALARM_LEVEL_1_CELL_VOLTAGE_LOW = "ALARM_LEVEL_1_CELL_VOLTAGE_LOW";
	public static final String KEY_ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH = "ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH";

	private static final String KEY_MAX_CELL_TEMPERATURE_ID = "MAX_CELL_TEMPERATURE_ID";
	private static final String KEY_MAX_CELL_TEMPERATURE = "MAX_CELL_TEMPERATURE";
	private static final String KEY_MIN_CELL_TEMPERATURE_ID = "MIN_CELL_TEMPERATURE_ID";
	private static final String KEY_MIN_CELL_TEMPERATURE = "MIN_CELL_TEMPERATURE";
	private static final String KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW = "ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW";
	private static final String KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH = "ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH";
	private static final String KEY_ALARM_LEVEL_2_GR_TEMPERATURE_HIGH = "ALARM_LEVEL_2_GR_TEMPERATURE_HIGH";
	private static final String KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW = "ALARM_LEVEL_2_CELL_CHA_TEMP_LOW";
	private static final String KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH = "ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH";
	private static final String KEY_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH = "ALARM_LEVEL_2_DISCHA_CURRENT_HIGH";
	private static final String KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW = "ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW";
	private static final String KEY_ALARM_LEVEL_2_CELL_VOLTAGE_LOW = "ALARM_LEVEL_2_CELL_VOLTAGE_LOW";
	private static final String KEY_ALARM_LEVEL_2_CHA_CURRENT_HIGH = "ALARM_LEVEL_2_CHA_CURRENT_HIGH";
	private static final String KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH = "ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH";
	private static final String KEY_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH = "ALARM_LEVEL_2_CELL_VOLTAGE_HIGH";
	private static final String KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW = "ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW";
	private static final String KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH = "ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH";
	private static final String KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH = "ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH";
	private static final String KEY_ALARM_LEVEL_1_GR_TEMPERATURE_HIGH = "ALARM_LEVEL_1_GR_TEMPERATURE_HIGH";
	private static final String KEY_ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH = "ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH";
	private static final String KEY_ALARM_LEVEL_1_SOC_LOW = "ALARM_LEVEL_1_SOC_LOW";
	private static final String KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_LOW = "ALARM_LEVEL_1_CELL_CHA_TEMP_LOW";
	private static final String KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH = "ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH";
	private static final String KEY_ALARM_LEVEL_1_DISCHA_CURRENT_HIGH = "ALARM_LEVEL_1_DISCHA_CURRENT_HIGH";
	private static final String KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW = "ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW";

	private static final String KEY_ALARM_LEVEL_1_CHA_CURRENT_HIGH = "ALARM_LEVEL_1_CHA_CURRENT_HIGH";
	private static final String KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH = "ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH";
	private static final String KEY_ALARM_LEVEL_1_CELL_VOLTAGE_HIGH = "ALARM_LEVEL_1_CELL_VOLTAGE_HIGH";
	private static final String KEY_RUN_STATE = "RUN_STATE";
	private static final String KEY_FAILURE_INITIALIZATION = "FAILURE_INITIALIZATION";
	private static final String KEY_FAILURE_EEPROM = "FAILURE_EEPROM";
	private static final String KEY_FAILURE_INTRANET_COMMUNICATION = "FAILURE_INTRANET_COMMUNICATION";
	private static final String KEY_FAILURE_TEMPERATURE_SENSOR_CABLE = "FAILURE_TEMPERATURE_SENSOR_CABLE";
	private static final String KEY_FAILURE_BALANCING_MODULE = "FAILURE_BALANCING_MODULE";
	private static final String KEY_FAILURE_TEMPERATURE_PCB = "FAILURE_TEMPERATURE_PCB";
	private static final String KEY_FAILURE_GR_TEMPERATURE = "FAILURE_GR_TEMPERATURE";
	private static final String KEY_FAILURE_TEMP_SENSOR = "FAILURE_TEMP_SENSOR";
	private static final String KEY_FAILURE_TEMP_SAMPLING = "FAILURE_TEMP_SAMPLING";
	private static final String KEY_FAILURE_VOLTAGE_SAMPLING = "FAILURE_VOLTAGE_SAMPLING";
	private static final String KEY_FAILURE_LTC6803 = "FAILURE_LTC6803";
	private static final String KEY_FAILURE_CONNECTOR_WIRE = "FAILURE_CONNECTOR_WIRE";
	private static final String KEY_FAILURE_SAMPLING_WIRE = "FAILURE_SAMPLING_WIRE";
	public static final String KEY_RESET = "RESET";
	public static final String KEY_SLEEP = "SLEEP";

	private static final String VOLTAGE = "VOLTAGE";
	private static final String BATTERY = "BATTERY";
	private static final String RACK = "RACK";
	private static final String TEMPERATURE = "TEMPERATURE";

	public static final int VOLTAGE_SENSORS_PER_MODULE = 12;
	public static final int TEMPERATURE_SENSORS_PER_MODULE = 12;

	private static final String NUMBER_FORMAT = "%03d"; // creates string number with leading zeros
	private static final int VOLTAGE_ADDRESS_OFFSET = 0x800;
	private static final int TEMPERATURE_ADDRESS_OFFSET = 0xC00;

	private int rackNumber;
	private int numberOfSlaves;
	private int addressOffset;
	private Cluster parent;
	private final Map<String, ChannelId> channelIds;
	private final Map<String, Channel<?>> channelMap;

	protected SingleRack(int racknumber, int numberOfSlaves, int addressOffset, Cluster parent) {
		this.rackNumber = racknumber;
		this.numberOfSlaves = numberOfSlaves;
		this.addressOffset = addressOffset;
		this.parent = parent;
		channelIds = createChannelIdMap();
		channelMap = createChannelMap();
	}

	public Collection<Channel<?>> getChannels() {
		return channelMap.values();
	}

	public Channel<?> getChannel(String key) {
		return channelMap.get(key);
	}

	public int getSoC() {
		return getIntFromChannel(KEY_SOC, 0);
	}

	public int getMinimalCellVoltage() {
		return getIntFromChannel(KEY_MIN_CELL_VOLTAGE, -1);
	}
	
	public int getMaximalCellVoltage() {
		return getIntFromChannel(KEY_MAX_CELL_VOLTAGE, -1);		
	}

	public int getMinimalCellTemperature() {
		return getIntFromChannel(KEY_MIN_CELL_TEMPERATURE, -1);
	}
	
	public int getMaximalCellTemperature() {
		return getIntFromChannel(KEY_MAX_CELL_TEMPERATURE, -1);		
	}
	
	private int getIntFromChannel(String key, int defaultValue) {
		@SuppressWarnings("unchecked")
		Optional<Integer> opt = (Optional<Integer>) this.channelMap.get(key).value()
				.asOptional();
		int value = defaultValue;
		if (opt.isPresent()) {
			value = opt.get();
		}
		return value;
	}

	private Map<String, Channel<?>> createChannelMap() {
		Map<String, Channel<?>> channels = new HashMap<>();

		channels.put(KEY_VOLTAGE, parent.addChannel(channelIds.get(KEY_VOLTAGE)));
		channels.put(KEY_CURRENT, parent.addChannel(channelIds.get(KEY_CURRENT)));
		channels.put(KEY_CHARGE_INDICATION, parent.addChannel(channelIds.get(KEY_CHARGE_INDICATION)));
		channels.put(KEY_SOC, parent.addChannel(channelIds.get(KEY_SOC)));
		channels.put(KEY_SOH, parent.addChannel(channelIds.get(KEY_SOH)));
		channels.put(KEY_MAX_CELL_VOLTAGE_ID, parent.addChannel(channelIds.get(KEY_MAX_CELL_VOLTAGE_ID)));
		channels.put(KEY_MAX_CELL_VOLTAGE, parent.addChannel(channelIds.get(KEY_MAX_CELL_VOLTAGE)));
		channels.put(KEY_MIN_CELL_VOLTAGE_ID, parent.addChannel(channelIds.get(KEY_MIN_CELL_VOLTAGE_ID)));
		channels.put(KEY_MIN_CELL_VOLTAGE, parent.addChannel(channelIds.get(KEY_MIN_CELL_VOLTAGE)));
		channels.put(KEY_MAX_CELL_TEMPERATURE_ID, parent.addChannel(channelIds.get(KEY_MAX_CELL_TEMPERATURE_ID)));
		channels.put(KEY_MAX_CELL_TEMPERATURE, parent.addChannel(channelIds.get(KEY_MAX_CELL_TEMPERATURE)));
		channels.put(KEY_MIN_CELL_TEMPERATURE_ID, parent.addChannel(channelIds.get(KEY_MIN_CELL_TEMPERATURE_ID)));
		channels.put(KEY_MIN_CELL_TEMPERATURE, parent.addChannel(channelIds.get(KEY_MIN_CELL_TEMPERATURE)));

		channels.put(KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW)));
		channels.put(KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH)));
		channels.put(KEY_ALARM_LEVEL_2_GR_TEMPERATURE_HIGH,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_GR_TEMPERATURE_HIGH)));
		channels.put(KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW)));
		channels.put(KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH)));
		channels.put(KEY_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH)));
		channels.put(KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW)));
		channels.put(KEY_ALARM_LEVEL_2_CELL_VOLTAGE_LOW,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_CELL_VOLTAGE_LOW)));
		channels.put(KEY_ALARM_LEVEL_2_CHA_CURRENT_HIGH,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_CHA_CURRENT_HIGH)));
		channels.put(KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH)));
		channels.put(KEY_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH)));

		channels.put(KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW)));
		channels.put(KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH)));
		channels.put(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH)));
		channels.put(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH)));
		channels.put(KEY_ALARM_LEVEL_1_GR_TEMPERATURE_HIGH,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_GR_TEMPERATURE_HIGH)));
		channels.put(KEY_ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH)));
		channels.put(KEY_ALARM_LEVEL_1_SOC_LOW, parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_SOC_LOW)));
		channels.put(KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_LOW,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_LOW)));
		channels.put(KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH)));
		channels.put(KEY_ALARM_LEVEL_1_DISCHA_CURRENT_HIGH,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_DISCHA_CURRENT_HIGH)));
		channels.put(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW)));
		channels.put(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_LOW,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_LOW)));
		channels.put(KEY_ALARM_LEVEL_1_CHA_CURRENT_HIGH,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_CHA_CURRENT_HIGH)));
		channels.put(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH)));
		channels.put(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_HIGH,
				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_HIGH)));

		channels.put(KEY_RUN_STATE, parent.addChannel(channelIds.get(KEY_RUN_STATE)));

		channels.put(KEY_FAILURE_INITIALIZATION, parent.addChannel(channelIds.get(KEY_FAILURE_INITIALIZATION)));
		channels.put(KEY_FAILURE_EEPROM, parent.addChannel(channelIds.get(KEY_FAILURE_EEPROM)));
		channels.put(KEY_FAILURE_INTRANET_COMMUNICATION,
				parent.addChannel(channelIds.get(KEY_FAILURE_INTRANET_COMMUNICATION)));
		channels.put(KEY_FAILURE_TEMPERATURE_SENSOR_CABLE,
				parent.addChannel(channelIds.get(KEY_FAILURE_TEMPERATURE_SENSOR_CABLE)));
		channels.put(KEY_FAILURE_BALANCING_MODULE, parent.addChannel(channelIds.get(KEY_FAILURE_BALANCING_MODULE)));
		channels.put(KEY_FAILURE_TEMPERATURE_PCB, parent.addChannel(channelIds.get(KEY_FAILURE_TEMPERATURE_PCB)));
		channels.put(KEY_FAILURE_GR_TEMPERATURE, parent.addChannel(channelIds.get(KEY_FAILURE_GR_TEMPERATURE)));
		channels.put(KEY_FAILURE_TEMP_SENSOR, parent.addChannel(channelIds.get(KEY_FAILURE_TEMP_SENSOR)));
		channels.put(KEY_FAILURE_TEMP_SAMPLING, parent.addChannel(channelIds.get(KEY_FAILURE_TEMP_SAMPLING)));
		channels.put(KEY_FAILURE_VOLTAGE_SAMPLING, parent.addChannel(channelIds.get(KEY_FAILURE_VOLTAGE_SAMPLING)));
		channels.put(KEY_FAILURE_LTC6803, parent.addChannel(channelIds.get(KEY_FAILURE_LTC6803)));
		channels.put(KEY_FAILURE_CONNECTOR_WIRE, parent.addChannel(channelIds.get(KEY_FAILURE_CONNECTOR_WIRE)));
		channels.put(KEY_FAILURE_SAMPLING_WIRE, parent.addChannel(channelIds.get(KEY_FAILURE_SAMPLING_WIRE)));

		channels.put(KEY_RESET, parent.addChannel(channelIds.get(KEY_RESET)));
		channels.put(KEY_SLEEP, parent.addChannel(channelIds.get(KEY_SLEEP)));

		// Cell voltages
		for (int i = 0; i < this.numberOfSlaves; i++) {
			for (int j = i * VOLTAGE_SENSORS_PER_MODULE; j < (i + 1) * VOLTAGE_SENSORS_PER_MODULE; j++) {
				String key = this.getSingleCellPrefix(j) + "_" + VOLTAGE;
				channels.put(key, parent.addChannel(channelIds.get(key)));
			}
		}

		// Cell temperatures
		for (int i = 0; i < this.numberOfSlaves; i++) {
			for (int j = i * TEMPERATURE_SENSORS_PER_MODULE; j < (i + 1) * TEMPERATURE_SENSORS_PER_MODULE; j++) {
				String key = this.getSingleCellPrefix(j) + "_" + TEMPERATURE;
				channels.put(key, parent.addChannel(channelIds.get(key)));
			}
		}

		return channels;
	}

	private Map<String, ChannelId> createChannelIdMap() {
		Map<String, ChannelId> map = new HashMap<String, ChannelId>();

		this.addEntry(map, KEY_VOLTAGE, new IntegerDoc().unit(Unit.MILLIVOLT));
		this.addEntry(map, KEY_CURRENT, new IntegerDoc().unit(Unit.MILLIAMPERE));

		this.addEntry(map, KEY_CHARGE_INDICATION, Doc.of(ChargeIndication.values()));
		this.addEntry(map, KEY_SOC, new IntegerDoc().unit(Unit.PERCENT));
		this.addEntry(map, KEY_SOH, new IntegerDoc().unit(Unit.PERCENT));
		this.addEntry(map, KEY_MAX_CELL_VOLTAGE_ID, new IntegerDoc().unit(Unit.NONE));
		this.addEntry(map, KEY_MAX_CELL_VOLTAGE, new IntegerDoc().unit(Unit.MILLIVOLT));
		this.addEntry(map, KEY_MIN_CELL_VOLTAGE_ID, new IntegerDoc().unit(Unit.NONE));
		this.addEntry(map, KEY_MIN_CELL_VOLTAGE, new IntegerDoc().unit(Unit.MILLIVOLT));
		this.addEntry(map, KEY_MAX_CELL_TEMPERATURE_ID, new IntegerDoc().unit(Unit.NONE));
		this.addEntry(map, KEY_MAX_CELL_TEMPERATURE, new IntegerDoc().unit(Unit.DEZIDEGREE_CELSIUS));
		this.addEntry(map, KEY_MIN_CELL_TEMPERATURE_ID, new IntegerDoc().unit(Unit.NONE));
		this.addEntry(map, KEY_MIN_CELL_TEMPERATURE, new IntegerDoc().unit(Unit.DEZIDEGREE_CELSIUS));

		this.addEntry(map, KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW,
				Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Cell Discharge Temperature Low Alarm Level 2")); // Bit
																														// 15
		this.addEntry(map, KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH,
				Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Cell Discharge Temperature High Alarm Level 2")); // Bit
																														// 14
		this.addEntry(map, KEY_ALARM_LEVEL_2_GR_TEMPERATURE_HIGH,
				Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " GR Temperature High Alarm Level 2")); // Bit
																											// 10
		this.addEntry(map, KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW,
				Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Cell Charge Temperature Low Alarm Level 2")); // Bit
																													// 7
		this.addEntry(map, KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH,
				Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Cell Charge Temperature High Alarm Level 2")); // Bit
																														// 6
		this.addEntry(map, KEY_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH,
				Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Discharge Current High Alarm Level 2")); // Bit
																												// 5
		this.addEntry(map, KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW,
				Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Total Voltage Low Alarm Level 2")); // Bit
																											// 4
		this.addEntry(map, KEY_ALARM_LEVEL_2_CELL_VOLTAGE_LOW,
				Doc.of(Level.FAULT).text("Cluster 1 Cell Voltage Low Alarm Level 2")); // Bit 3
		this.addEntry(map, KEY_ALARM_LEVEL_2_CHA_CURRENT_HIGH,
				Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Charge Current High Alarm Level 2")); // Bit
																											// 2
		this.addEntry(map, KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH,
				Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Total Voltage High Alarm Level 2")); // Bit
																											// 1
		this.addEntry(map, KEY_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH,
				Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Cell Voltage High Alarm Level 2")); // Bit
																											// 0

		this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW,
				Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Cell Discharge Temperature Low Alarm Level 1")); // Bit
																															// 15
		this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH, Doc.of(Level.WARNING)
				.text("Rack" + this.rackNumber + " Cell Discharge Temperature High Alarm Level 1")); // Bit 14
		this.addEntry(map, KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH,
				Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Total Voltage Diff High Alarm Level 1")); // Bit
																													// 13
		this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH,
				Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Cell Voltage Diff High Alarm Level 1")); // Bit
																													// 11
		this.addEntry(map, KEY_ALARM_LEVEL_1_GR_TEMPERATURE_HIGH,
				Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " GR Temperature High Alarm Level 1")); // Bit
																												// 10
		this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH,
				Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Cell temperature Diff High Alarm Level 1")); // Bit
																														// 9
		this.addEntry(map, KEY_ALARM_LEVEL_1_SOC_LOW,
				Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " SOC Low Alarm Level 1")); // Bit 8
		this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_LOW,
				Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Cell Charge Temperature Low Alarm Level 1")); // Bit
																														// 7
		this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH,
				Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Cell Charge Temperature High Alarm Level 1")); // Bit
																														// 6
		this.addEntry(map, KEY_ALARM_LEVEL_1_DISCHA_CURRENT_HIGH,
				Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Discharge Current High Alarm Level 1")); // Bit
																													// 5
		this.addEntry(map, KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW,
				Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Total Voltage Low Alarm Level 1")); // Bit
																											// 4
		this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_VOLTAGE_LOW,
				Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Cell Voltage Low Alarm Level 1")); // Bit
																											// 3
		this.addEntry(map, KEY_ALARM_LEVEL_1_CHA_CURRENT_HIGH,
				Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Charge Current High Alarm Level 1")); // Bit
																												// 2
		this.addEntry(map, KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH,
				Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Total Voltage High Alarm Level 1")); // Bit
																												// 1
		this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_VOLTAGE_HIGH,
				Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Cell Voltage High Alarm Level 1")); // Bit
																											// 0
		this.addEntry(map, KEY_RUN_STATE, Doc.of(Enums.ClusterRunState.values())); //

		this.addEntry(map, KEY_FAILURE_INITIALIZATION, Doc.of(Level.FAULT).text("Initialization failure")); // Bit
		// 12
		this.addEntry(map, KEY_FAILURE_EEPROM, Doc.of(Level.FAULT).text("EEPROM fault")); // Bit 11
		this.addEntry(map, KEY_FAILURE_INTRANET_COMMUNICATION,
				Doc.of(Level.FAULT).text("Internal communication fault")); // Bit
																			// 10
		this.addEntry(map, KEY_FAILURE_TEMPERATURE_SENSOR_CABLE,
				Doc.of(Level.FAULT).text("Temperature sensor cable fault")); // Bit
																				// 9
		this.addEntry(map, KEY_FAILURE_BALANCING_MODULE, Doc.of(Level.OK).text("Balancing module fault")); // Bit 8
		this.addEntry(map, KEY_FAILURE_TEMPERATURE_PCB, Doc.of(Level.FAULT).text("Temperature PCB error")); // Bit 7
		this.addEntry(map, KEY_FAILURE_GR_TEMPERATURE, Doc.of(Level.FAULT).text("GR Temperature error")); // Bit 6
		this.addEntry(map, KEY_FAILURE_TEMP_SENSOR, Doc.of(Level.FAULT).text("Temperature sensor fault")); // Bit 5
		this.addEntry(map, KEY_FAILURE_TEMP_SAMPLING, Doc.of(Level.FAULT).text("Temperature sampling fault")); // Bit
																												// 4
		this.addEntry(map, KEY_FAILURE_VOLTAGE_SAMPLING, Doc.of(Level.FAULT).text("Voltage sampling fault")); // Bit
																												// 3
		this.addEntry(map, KEY_FAILURE_LTC6803, Doc.of(Level.FAULT).text("LTC6803 fault")); // Bit 2
		this.addEntry(map, KEY_FAILURE_CONNECTOR_WIRE, Doc.of(Level.FAULT).text("connector wire fault")); // Bit 1
		this.addEntry(map, KEY_FAILURE_SAMPLING_WIRE, Doc.of(Level.FAULT).text("sampling wire fault")); // Bit 0

		this.addEntry(map, KEY_SLEEP, Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE));
		this.addEntry(map, KEY_RESET, Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE));

		// Cell voltages formatted like: "RACK_1_BATTERY_000_VOLTAGE"
		for (int i = 0; i < this.numberOfSlaves; i++) {
			for (int j = i * VOLTAGE_SENSORS_PER_MODULE; j < (i + 1) * VOLTAGE_SENSORS_PER_MODULE; j++) {
				String key = getSingleCellPrefix(j) + "_" + VOLTAGE;
				this.addEntry(map, key, new IntegerDoc().unit(Unit.MILLIVOLT));
			}
		}
		// Cell temperatures formatted like : "RACK_1_BATTERY_000_TEMPERATURE"
		for (int i = 0; i < numberOfSlaves; i++) {
			for (int j = i * TEMPERATURE_SENSORS_PER_MODULE; j < (i + 1) * TEMPERATURE_SENSORS_PER_MODULE; j++) {
				String key = getSingleCellPrefix(j) + "_" + TEMPERATURE;
				this.addEntry(map, key, new IntegerDoc().unit(Unit.DEZIDEGREE_CELSIUS));
			}
		}

		return map;
	}

	public Collection<Task> getTasks() {
		Collection<Task> tasks = new ArrayList<>();

		// State values
		tasks.add(new FC3ReadRegistersTask(this.addressOffset + 0x100, Priority.HIGH, //
				parent.map(channelIds.get(KEY_VOLTAGE), getUWE(0x100), ElementToChannelConverter.SCALE_FACTOR_2), //
				parent.map(channelIds.get(KEY_CURRENT), getSWE(0x101), ElementToChannelConverter.SCALE_FACTOR_2), //
				parent.map(channelIds.get(KEY_CHARGE_INDICATION), getUWE(0x102)), //
				parent.map(channelIds.get(KEY_SOC), getUWE(0x103)). //
					onUpdateCallback(val -> {
						parent.recalculateSoc();
					}), //
				parent.map(channelIds.get(KEY_SOH), getUWE(0x104)), //
				parent.map(channelIds.get(KEY_MAX_CELL_VOLTAGE_ID), getUWE(0x105)), //
				parent.map(channelIds.get(KEY_MAX_CELL_VOLTAGE), getUWE(0x106)). //
					onUpdateCallback(val -> {
						parent.recalculateMaxCellVoltage();
					}), //
				parent.map(channelIds.get(KEY_MIN_CELL_VOLTAGE_ID), getUWE(0x107)), //
				parent.map(channelIds.get(KEY_MIN_CELL_VOLTAGE), getUWE(0x108)). //
					onUpdateCallback(val -> {
						parent.recalculateMinCellVoltage();
					}), //
				parent.map(channelIds.get(KEY_MAX_CELL_TEMPERATURE_ID), getUWE(0x109)), //
				parent.map(channelIds.get(KEY_MAX_CELL_TEMPERATURE), getUWE(0x10A), ElementToChannelConverter.SCALE_FACTOR_MINUS_1). //
					onUpdateCallback(val -> {
						parent.recalculateMaxCellTemperature();
					}), //
				parent.map(channelIds.get(KEY_MIN_CELL_TEMPERATURE_ID), getUWE(0x10B)), //
				parent.map(channelIds.get(KEY_MIN_CELL_TEMPERATURE), getUWE(0x10C), ElementToChannelConverter.SCALE_FACTOR_MINUS_1). //
					onUpdateCallback(val -> {
						parent.recalculateMinCellTemperature();
					}) //
		));

		// Alarm levels
		tasks.add(new FC3ReadRegistersTask(this.addressOffset + 0x140, Priority.LOW, //
				parent.map(getBWE(0x140, parent) //
						.bit(0, channelIds.get(KEY_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH)) //
						.bit(1, channelIds.get(KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH)) //
						.bit(2, channelIds.get(KEY_ALARM_LEVEL_2_CHA_CURRENT_HIGH)) //
						.bit(3, channelIds.get(KEY_ALARM_LEVEL_2_CELL_VOLTAGE_LOW)) //
						.bit(4, channelIds.get(KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW)) //
						.bit(5, channelIds.get(KEY_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH)) //
						.bit(6, channelIds.get(KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH)) //
						.bit(7, channelIds.get(KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW)) //
						.bit(10, channelIds.get(KEY_ALARM_LEVEL_2_GR_TEMPERATURE_HIGH)) //
						.bit(14, channelIds.get(KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH)) //
						.bit(15, channelIds.get(KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW)) //
				), //
				parent.map(getBWE(0x141, parent) //
						.bit(0, channelIds.get(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_HIGH)) //
						.bit(1, channelIds.get(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH)) //
						.bit(2, channelIds.get(KEY_ALARM_LEVEL_1_CHA_CURRENT_HIGH)) //
						.bit(3, channelIds.get(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_LOW)) //
						.bit(4, channelIds.get(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW)) //
						.bit(5, channelIds.get(KEY_ALARM_LEVEL_1_DISCHA_CURRENT_HIGH)) //
						.bit(6, channelIds.get(KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH)) //
						.bit(7, channelIds.get(KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_LOW)) //
						.bit(8, channelIds.get(KEY_ALARM_LEVEL_1_SOC_LOW)) //
						.bit(9, channelIds.get(KEY_ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH)) //
						.bit(10, channelIds.get(KEY_ALARM_LEVEL_1_GR_TEMPERATURE_HIGH)) //
						.bit(11, channelIds.get(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH)) //
						.bit(13, channelIds.get(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH)) //
						.bit(14, channelIds.get(KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH)) //
						.bit(15, channelIds.get(KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW)) //
				), //
				parent.map(channelIds.get(KEY_RUN_STATE), getUWE(0x142)) //
		) //
		);

		// Error Codes
		tasks.add(new FC3ReadRegistersTask(this.addressOffset + 0x185, Priority.LOW, //
				parent.map(getBWE(0x185, parent) //
						.bit(0, channelIds.get(KEY_FAILURE_SAMPLING_WIRE))//
						.bit(1, channelIds.get(KEY_FAILURE_CONNECTOR_WIRE))//
						.bit(2, channelIds.get(KEY_FAILURE_LTC6803))//
						.bit(3, channelIds.get(KEY_FAILURE_VOLTAGE_SAMPLING))//
						.bit(4, channelIds.get(KEY_FAILURE_TEMP_SAMPLING))//
						.bit(5, channelIds.get(KEY_FAILURE_TEMP_SENSOR))//
						.bit(6, channelIds.get(KEY_FAILURE_GR_TEMPERATURE))//
						.bit(7, channelIds.get(KEY_FAILURE_TEMPERATURE_PCB))//
						.bit(8, channelIds.get(KEY_FAILURE_BALANCING_MODULE))//
						.bit(9, channelIds.get(KEY_FAILURE_TEMPERATURE_SENSOR_CABLE))//
						.bit(10, channelIds.get(KEY_FAILURE_INTRANET_COMMUNICATION))//
						.bit(11, channelIds.get(KEY_FAILURE_EEPROM))//
						.bit(12, channelIds.get(KEY_FAILURE_INITIALIZATION))//
				) //
		));

		// Reset and sleep
		tasks.add(new FC6WriteRegisterTask(this.addressOffset + 0x0004, //
				parent.map(channelIds.get(KEY_RESET), getUWE(0x0004))));
		tasks.add(new FC6WriteRegisterTask(this.addressOffset + 0x001D, //
				parent.map(channelIds.get(KEY_SLEEP), getUWE(0x001D))));

		int MAX_ELEMENTS_PER_TASK = 100;

		// Cell voltages
		for (int i = 0; i < this.numberOfSlaves; i++) {
			List<AbstractModbusElement<?>> elements = new ArrayList<>();
			for (int j = i * VOLTAGE_SENSORS_PER_MODULE; j < (i + 1) * VOLTAGE_SENSORS_PER_MODULE; j++) {
				String key = getSingleCellPrefix(j) + "_" + VOLTAGE;
				UnsignedWordElement uwe = getUWE(VOLTAGE_ADDRESS_OFFSET + j);
				AbstractModbusElement<?> ame = parent.map(channelIds.get(key), uwe);
				elements.add(ame);
			}

			// not more than 100 elements per task, because it can cause problems..
			int taskCount = (elements.size() / MAX_ELEMENTS_PER_TASK) + 1;

			for (int x = 0; x < taskCount; x++) {
				List<AbstractModbusElement<?>> subElements = elements.subList(x * MAX_ELEMENTS_PER_TASK,
						Math.min(((x + 1) * MAX_ELEMENTS_PER_TASK), elements.size()));
				AbstractModbusElement<?>[] taskElements = subElements.toArray(new AbstractModbusElement<?>[0]);
				tasks.add(new FC3ReadRegistersTask(taskElements[0].getStartAddress(), Priority.LOW, taskElements));
			}

		}

		// Cell temperatures
		for (int i = 0; i < this.numberOfSlaves; i++) {
			List<AbstractModbusElement<?>> elements = new ArrayList<>();
			for (int j = i * TEMPERATURE_SENSORS_PER_MODULE; j < (i + 1) * TEMPERATURE_SENSORS_PER_MODULE; j++) {
				String key = getSingleCellPrefix(j) + "_" + TEMPERATURE;

				SignedWordElement swe = getSWE(TEMPERATURE_ADDRESS_OFFSET + j);
				AbstractModbusElement<?> ame = parent.map(channelIds.get(key), swe);
				elements.add(ame);
			}

			// not more than 100 elements per task, because it can cause problems..
			int taskCount = (elements.size() / MAX_ELEMENTS_PER_TASK) + 1;

			for (int x = 0; x < taskCount; x++) {
				List<AbstractModbusElement<?>> subElements = elements.subList(x * MAX_ELEMENTS_PER_TASK,
						Math.min(((x + 1) * MAX_ELEMENTS_PER_TASK), elements.size()));
				AbstractModbusElement<?>[] taskElements = subElements.toArray(new AbstractModbusElement<?>[0]);
				tasks.add(new FC3ReadRegistersTask(taskElements[0].getStartAddress(), Priority.LOW, taskElements));
			}
		}

		return tasks;
	}

	public int getRackNumber() {
		return rackNumber;
	}

	public int getAddressOffset() {
		return addressOffset;
	}

	private ChannelId createChannelId(String key, Doc doc) {
		return new ChannelIdImpl(this.getRackPrefix() + key, doc);
	}

	private void addEntry(Map<String, ChannelId> map, String key, Doc doc) {
		map.put(key, this.createChannelId(key, doc));
	}

	private String getSingleCellPrefix(int num) {
		return BATTERY + "_" + String.format(NUMBER_FORMAT, num);
	}

	private String getRackPrefix() {
		return RACK + "_" + this.rackNumber + "_";
	}

	private BitsWordElement getBWE(int addressWithoutOffset, AbstractOpenemsModbusComponent component) {
		return new BitsWordElement(this.addressOffset + addressWithoutOffset, component);
	}

	private UnsignedWordElement getUWE(int addressWithoutOffset) {
		return new UnsignedWordElement(this.addressOffset + addressWithoutOffset);
	}

	private SignedWordElement getSWE(int addressWithoutOffset) {
		return new SignedWordElement(this.addressOffset + addressWithoutOffset);
	}
}
