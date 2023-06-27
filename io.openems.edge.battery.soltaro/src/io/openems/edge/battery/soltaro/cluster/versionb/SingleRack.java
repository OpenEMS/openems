package io.openems.edge.battery.soltaro.cluster.versionb;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;

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
import io.openems.edge.battery.soltaro.common.enums.ChargeIndication;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.ChannelId.ChannelIdImpl;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.taskmanager.Priority;

/**
 * Helper class that provides channels and channel ids for a multi rack channels
 * and ids are created dynamically depending on system configuration.
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

	private final int rackNumber;
	private final int numberOfSlaves;
	private final int addressOffset;
	private final BatterySoltaroClusterVersionBImpl parent;
	private final Map<String, ChannelId> channelIds;
	private final Map<String, Channel<?>> channelMap;

	protected SingleRack(int racknumber, int numberOfSlaves, int addressOffset,
			BatterySoltaroClusterVersionBImpl parent) {
		this.rackNumber = racknumber;
		this.numberOfSlaves = numberOfSlaves;
		this.addressOffset = addressOffset;
		this.parent = parent;
		this.channelIds = this.createChannelIdMap();
		this.channelMap = this.createChannelMap();
	}

	protected Collection<Channel<?>> getChannels() {
		return this.channelMap.values();
	}

	protected Channel<?> getChannel(String key) {
		return this.channelMap.get(key);
	}

	protected int getSoC() {
		return this.getIntFromChannel(KEY_SOC, 0);
	}

	protected int getMinimalCellVoltage() {
		return this.getIntFromChannel(KEY_MIN_CELL_VOLTAGE, -1);
	}

	protected int getMaximalCellVoltage() {
		return this.getIntFromChannel(KEY_MAX_CELL_VOLTAGE, -1);
	}

	protected int getMinimalCellTemperature() {
		return this.getIntFromChannel(KEY_MIN_CELL_TEMPERATURE, -1);
	}

	protected int getMaximalCellTemperature() {
		return this.getIntFromChannel(KEY_MAX_CELL_TEMPERATURE, -1);
	}

	protected Collection<Task> getTasks() {
		Collection<Task> tasks = new ArrayList<>();

		// State values
		tasks.add(new FC3ReadRegistersTask(this.addressOffset + 0x100, Priority.HIGH, //
				this.parent.map(this.channelIds.get(KEY_VOLTAGE), this.getUnsignedWordElement(0x100), SCALE_FACTOR_2), //
				this.parent.map(this.channelIds.get(KEY_CURRENT), this.getSignedWordElement(0x101), SCALE_FACTOR_2), //
				this.parent.map(this.channelIds.get(KEY_CHARGE_INDICATION), this.getUnsignedWordElement(0x102)), //
				this.parent.map(this.channelIds.get(KEY_SOC), this.getUnsignedWordElement(0x103)). //
						onUpdateCallback(val -> {
							this.parent.recalculateSoc();
						}), //
				this.parent.map(this.channelIds.get(KEY_SOH), this.getUnsignedWordElement(0x104)), //
				this.parent.map(this.channelIds.get(KEY_MAX_CELL_VOLTAGE_ID), this.getUnsignedWordElement(0x105)), //
				this.parent.map(this.channelIds.get(KEY_MAX_CELL_VOLTAGE), this.getUnsignedWordElement(0x106)). //
						onUpdateCallback(val -> {
							this.parent.recalculateMaxCellVoltage();
						}), //
				this.parent.map(this.channelIds.get(KEY_MIN_CELL_VOLTAGE_ID), this.getUnsignedWordElement(0x107)), //
				this.parent.map(this.channelIds.get(KEY_MIN_CELL_VOLTAGE), this.getUnsignedWordElement(0x108)). //
						onUpdateCallback(val -> {
							this.parent.recalculateMinCellVoltage();
						}), //
				this.parent.map(this.channelIds.get(KEY_MAX_CELL_TEMPERATURE_ID), this.getUnsignedWordElement(0x109)), //
				this.parent.map(this.channelIds.get(KEY_MAX_CELL_TEMPERATURE), this.getSignedWordElement(0x10A),
						SCALE_FACTOR_MINUS_1). //
						onUpdateCallback(val -> {
							this.parent.recalculateMaxCellTemperature();
						}), //
				this.parent.map(this.channelIds.get(KEY_MIN_CELL_TEMPERATURE_ID), this.getUnsignedWordElement(0x10B)), //
				this.parent.map(this.channelIds.get(KEY_MIN_CELL_TEMPERATURE), this.getSignedWordElement(0x10C),
						SCALE_FACTOR_MINUS_1). //
						onUpdateCallback(val -> {
							this.parent.recalculateMinCellTemperature();
						}) //
		));

		// Alarm levels
		tasks.add(new FC3ReadRegistersTask(this.addressOffset + 0x140, Priority.LOW, //
				this.parent.map(this.getBitsWordElement(0x140, this.parent) //
						.bit(0, this.channelIds.get(KEY_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH)) //
						.bit(1, this.channelIds.get(KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH)) //
						.bit(2, this.channelIds.get(KEY_ALARM_LEVEL_2_CHA_CURRENT_HIGH)) //
						.bit(3, this.channelIds.get(KEY_ALARM_LEVEL_2_CELL_VOLTAGE_LOW)) //
						.bit(4, this.channelIds.get(KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW)) //
						.bit(5, this.channelIds.get(KEY_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH)) //
						.bit(6, this.channelIds.get(KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH)) //
						.bit(7, this.channelIds.get(KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW)) //
						.bit(10, this.channelIds.get(KEY_ALARM_LEVEL_2_GR_TEMPERATURE_HIGH)) //
						.bit(14, this.channelIds.get(KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH)) //
						.bit(15, this.channelIds.get(KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW)) //
				), //
				this.parent.map(this.getBitsWordElement(0x141, this.parent) //
						.bit(0, this.channelIds.get(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_HIGH)) //
						.bit(1, this.channelIds.get(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH)) //
						.bit(2, this.channelIds.get(KEY_ALARM_LEVEL_1_CHA_CURRENT_HIGH)) //
						.bit(3, this.channelIds.get(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_LOW)) //
						.bit(4, this.channelIds.get(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW)) //
						.bit(5, this.channelIds.get(KEY_ALARM_LEVEL_1_DISCHA_CURRENT_HIGH)) //
						.bit(6, this.channelIds.get(KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH)) //
						.bit(7, this.channelIds.get(KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_LOW)) //
						.bit(8, this.channelIds.get(KEY_ALARM_LEVEL_1_SOC_LOW)) //
						.bit(9, this.channelIds.get(KEY_ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH)) //
						.bit(10, this.channelIds.get(KEY_ALARM_LEVEL_1_GR_TEMPERATURE_HIGH)) //
						.bit(11, this.channelIds.get(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH)) //
						.bit(13, this.channelIds.get(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH)) //
						.bit(14, this.channelIds.get(KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH)) //
						.bit(15, this.channelIds.get(KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW)) //
				), //
				this.parent.map(this.channelIds.get(KEY_RUN_STATE), this.getUnsignedWordElement(0x142)) //
		) //
		);

		// Error Codes
		tasks.add(new FC3ReadRegistersTask(this.addressOffset + 0x185, Priority.LOW, //
				this.parent.map(this.getBitsWordElement(0x185, this.parent) //
						.bit(0, this.channelIds.get(KEY_FAILURE_SAMPLING_WIRE))//
						.bit(1, this.channelIds.get(KEY_FAILURE_CONNECTOR_WIRE))//
						.bit(2, this.channelIds.get(KEY_FAILURE_LTC6803))//
						.bit(3, this.channelIds.get(KEY_FAILURE_VOLTAGE_SAMPLING))//
						.bit(4, this.channelIds.get(KEY_FAILURE_TEMP_SAMPLING))//
						.bit(5, this.channelIds.get(KEY_FAILURE_TEMP_SENSOR))//
						.bit(6, this.channelIds.get(KEY_FAILURE_GR_TEMPERATURE))//
						.bit(7, this.channelIds.get(KEY_FAILURE_TEMPERATURE_PCB))//
						.bit(8, this.channelIds.get(KEY_FAILURE_BALANCING_MODULE))//
						.bit(9, this.channelIds.get(KEY_FAILURE_TEMPERATURE_SENSOR_CABLE))//
						.bit(10, this.channelIds.get(KEY_FAILURE_INTRANET_COMMUNICATION))//
						.bit(11, this.channelIds.get(KEY_FAILURE_EEPROM))//
						.bit(12, this.channelIds.get(KEY_FAILURE_INITIALIZATION))//
				) //
		));

		// Reset and sleep
		tasks.add(new FC6WriteRegisterTask(this.addressOffset + 0x0004, //
				this.parent.map(this.channelIds.get(KEY_RESET), this.getUnsignedWordElement(0x0004))));
		tasks.add(new FC6WriteRegisterTask(this.addressOffset + 0x001D, //
				this.parent.map(this.channelIds.get(KEY_SLEEP), this.getUnsignedWordElement(0x001D))));

		var maxElementsPerTask = 100;

		// Cell voltages
		for (var i = 0; i < this.numberOfSlaves; i++) {
			List<AbstractModbusElement<?, ?>> elements = new ArrayList<>();
			for (var j = i * VOLTAGE_SENSORS_PER_MODULE; j < (i + 1) * VOLTAGE_SENSORS_PER_MODULE; j++) {
				var key = this.getSingleCellPrefix(j) + "_" + VOLTAGE;
				var uwe = this.getUnsignedWordElement(VOLTAGE_ADDRESS_OFFSET + j);
				AbstractModbusElement<?, ?> ame = this.parent.map(this.channelIds.get(key), uwe);
				elements.add(ame);
			}

			// not more than 100 elements per task, because it can cause problems..
			var taskCount = elements.size() / maxElementsPerTask + 1;

			for (var x = 0; x < taskCount; x++) {
				var subElements = elements.subList(x * maxElementsPerTask,
						Math.min((x + 1) * maxElementsPerTask, elements.size()));
				var taskElements = subElements.toArray(new AbstractModbusElement<?, ?>[0]);
				tasks.add(new FC3ReadRegistersTask(taskElements[0].getStartAddress(), Priority.LOW, taskElements));
			}

		}

		// Cell temperatures
		for (var i = 0; i < this.numberOfSlaves; i++) {
			List<AbstractModbusElement<?, ?>> elements = new ArrayList<>();
			for (var j = i * TEMPERATURE_SENSORS_PER_MODULE; j < (i + 1) * TEMPERATURE_SENSORS_PER_MODULE; j++) {
				var key = this.getSingleCellPrefix(j) + "_" + TEMPERATURE;

				var swe = this.getSignedWordElement(TEMPERATURE_ADDRESS_OFFSET + j);
				AbstractModbusElement<?, ?> ame = this.parent.map(this.channelIds.get(key), swe);
				elements.add(ame);
			}

			// not more than 100 elements per task, because it can cause problems..
			var taskCount = elements.size() / maxElementsPerTask + 1;

			for (var x = 0; x < taskCount; x++) {
				var subElements = elements.subList(x * maxElementsPerTask,
						Math.min((x + 1) * maxElementsPerTask, elements.size()));
				var taskElements = subElements.toArray(new AbstractModbusElement<?, ?>[0]);
				tasks.add(new FC3ReadRegistersTask(taskElements[0].getStartAddress(), Priority.LOW, taskElements));
			}
		}

		return tasks;
	}

	private int getIntFromChannel(String key, int defaultValue) {
		@SuppressWarnings("unchecked")
		var opt = (Optional<Integer>) this.channelMap.get(key).value().asOptional();
		var value = defaultValue;
		if (opt.isPresent()) {
			value = opt.get();
		}
		return value;
	}

	private Map<String, Channel<?>> createChannelMap() {
		Map<String, Channel<?>> channels = new HashMap<>();

		channels.put(KEY_VOLTAGE, this.parent.addChannel(this.channelIds.get(KEY_VOLTAGE)));
		channels.put(KEY_CURRENT, this.parent.addChannel(this.channelIds.get(KEY_CURRENT)));
		channels.put(KEY_CHARGE_INDICATION, this.parent.addChannel(this.channelIds.get(KEY_CHARGE_INDICATION)));
		channels.put(KEY_SOC, this.parent.addChannel(this.channelIds.get(KEY_SOC)));
		channels.put(KEY_SOH, this.parent.addChannel(this.channelIds.get(KEY_SOH)));
		channels.put(KEY_MAX_CELL_VOLTAGE_ID, this.parent.addChannel(this.channelIds.get(KEY_MAX_CELL_VOLTAGE_ID)));
		channels.put(KEY_MAX_CELL_VOLTAGE, this.parent.addChannel(this.channelIds.get(KEY_MAX_CELL_VOLTAGE)));
		channels.put(KEY_MIN_CELL_VOLTAGE_ID, this.parent.addChannel(this.channelIds.get(KEY_MIN_CELL_VOLTAGE_ID)));
		channels.put(KEY_MIN_CELL_VOLTAGE, this.parent.addChannel(this.channelIds.get(KEY_MIN_CELL_VOLTAGE)));
		channels.put(KEY_MAX_CELL_TEMPERATURE_ID,
				this.parent.addChannel(this.channelIds.get(KEY_MAX_CELL_TEMPERATURE_ID)));
		channels.put(KEY_MAX_CELL_TEMPERATURE, this.parent.addChannel(this.channelIds.get(KEY_MAX_CELL_TEMPERATURE)));
		channels.put(KEY_MIN_CELL_TEMPERATURE_ID,
				this.parent.addChannel(this.channelIds.get(KEY_MIN_CELL_TEMPERATURE_ID)));
		channels.put(KEY_MIN_CELL_TEMPERATURE, this.parent.addChannel(this.channelIds.get(KEY_MIN_CELL_TEMPERATURE)));

		channels.put(KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW)));
		channels.put(KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH)));
		channels.put(KEY_ALARM_LEVEL_2_GR_TEMPERATURE_HIGH,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_2_GR_TEMPERATURE_HIGH)));
		channels.put(KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW)));
		channels.put(KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH)));
		channels.put(KEY_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH)));
		channels.put(KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW)));
		channels.put(KEY_ALARM_LEVEL_2_CELL_VOLTAGE_LOW,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_2_CELL_VOLTAGE_LOW)));
		channels.put(KEY_ALARM_LEVEL_2_CHA_CURRENT_HIGH,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_2_CHA_CURRENT_HIGH)));
		channels.put(KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH)));
		channels.put(KEY_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH)));
		channels.put(KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW)));
		channels.put(KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH)));
		channels.put(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH)));
		channels.put(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH)));
		channels.put(KEY_ALARM_LEVEL_1_GR_TEMPERATURE_HIGH,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_1_GR_TEMPERATURE_HIGH)));
		channels.put(KEY_ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH)));
		channels.put(KEY_ALARM_LEVEL_1_SOC_LOW, this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_1_SOC_LOW)));
		channels.put(KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_LOW,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_LOW)));
		channels.put(KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH)));
		channels.put(KEY_ALARM_LEVEL_1_DISCHA_CURRENT_HIGH,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_1_DISCHA_CURRENT_HIGH)));
		channels.put(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW)));
		channels.put(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_LOW,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_LOW)));
		channels.put(KEY_ALARM_LEVEL_1_CHA_CURRENT_HIGH,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_1_CHA_CURRENT_HIGH)));
		channels.put(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH)));
		channels.put(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_HIGH,
				this.parent.addChannel(this.channelIds.get(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_HIGH)));
		channels.put(KEY_RUN_STATE, this.parent.addChannel(this.channelIds.get(KEY_RUN_STATE)));
		channels.put(KEY_FAILURE_INITIALIZATION,
				this.parent.addChannel(this.channelIds.get(KEY_FAILURE_INITIALIZATION)));
		channels.put(KEY_FAILURE_EEPROM, this.parent.addChannel(this.channelIds.get(KEY_FAILURE_EEPROM)));
		channels.put(KEY_FAILURE_INTRANET_COMMUNICATION,
				this.parent.addChannel(this.channelIds.get(KEY_FAILURE_INTRANET_COMMUNICATION)));
		channels.put(KEY_FAILURE_TEMPERATURE_SENSOR_CABLE,
				this.parent.addChannel(this.channelIds.get(KEY_FAILURE_TEMPERATURE_SENSOR_CABLE)));
		channels.put(KEY_FAILURE_BALANCING_MODULE,
				this.parent.addChannel(this.channelIds.get(KEY_FAILURE_BALANCING_MODULE)));
		channels.put(KEY_FAILURE_TEMPERATURE_PCB,
				this.parent.addChannel(this.channelIds.get(KEY_FAILURE_TEMPERATURE_PCB)));
		channels.put(KEY_FAILURE_GR_TEMPERATURE,
				this.parent.addChannel(this.channelIds.get(KEY_FAILURE_GR_TEMPERATURE)));
		channels.put(KEY_FAILURE_TEMP_SENSOR, this.parent.addChannel(this.channelIds.get(KEY_FAILURE_TEMP_SENSOR)));
		channels.put(KEY_FAILURE_TEMP_SAMPLING, this.parent.addChannel(this.channelIds.get(KEY_FAILURE_TEMP_SAMPLING)));
		channels.put(KEY_FAILURE_VOLTAGE_SAMPLING,
				this.parent.addChannel(this.channelIds.get(KEY_FAILURE_VOLTAGE_SAMPLING)));
		channels.put(KEY_FAILURE_LTC6803, this.parent.addChannel(this.channelIds.get(KEY_FAILURE_LTC6803)));
		channels.put(KEY_FAILURE_CONNECTOR_WIRE,
				this.parent.addChannel(this.channelIds.get(KEY_FAILURE_CONNECTOR_WIRE)));
		channels.put(KEY_FAILURE_SAMPLING_WIRE, this.parent.addChannel(this.channelIds.get(KEY_FAILURE_SAMPLING_WIRE)));
		channels.put(KEY_RESET, this.parent.addChannel(this.channelIds.get(KEY_RESET)));
		channels.put(KEY_SLEEP, this.parent.addChannel(this.channelIds.get(KEY_SLEEP)));

		// Cell voltages
		for (var i = 0; i < this.numberOfSlaves; i++) {
			for (var j = i * VOLTAGE_SENSORS_PER_MODULE; j < (i + 1) * VOLTAGE_SENSORS_PER_MODULE; j++) {
				var key = this.getSingleCellPrefix(j) + "_" + VOLTAGE;
				channels.put(key, this.parent.addChannel(this.channelIds.get(key)));
			}
		}

		// Cell temperatures
		for (var i = 0; i < this.numberOfSlaves; i++) {
			for (var j = i * TEMPERATURE_SENSORS_PER_MODULE; j < (i + 1) * TEMPERATURE_SENSORS_PER_MODULE; j++) {
				var key = this.getSingleCellPrefix(j) + "_" + TEMPERATURE;
				channels.put(key, this.parent.addChannel(this.channelIds.get(key)));
			}
		}

		return channels;
	}

	private Map<String, ChannelId> createChannelIdMap() {
		Map<String, ChannelId> map = new HashMap<>();

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
		this.addEntry(map, KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW, Doc.of(Level.FAULT)
				.text("Rack" + this.rackNumber + " Cell Discharge Temperature Low Alarm Level 2")); /* Bit 15 */
		this.addEntry(map, KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH, Doc.of(Level.FAULT)
				.text("Rack" + this.rackNumber + " Cell Discharge Temperature High Alarm Level 2")); /* Bit 14 */
		this.addEntry(map, KEY_ALARM_LEVEL_2_GR_TEMPERATURE_HIGH,
				Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " GR Temperature High Alarm Level 2")); /* Bit 10 */
		this.addEntry(map, KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW, Doc.of(Level.FAULT)
				.text("Rack" + this.rackNumber + " Cell Charge Temperature Low Alarm Level 2")); /* Bit 7 */
		this.addEntry(map, KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH, Doc.of(Level.FAULT)
				.text("Rack" + this.rackNumber + " Cell Charge Temperature High Alarm Level 2")); /* Bit 6 */
		this.addEntry(map, KEY_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH, Doc.of(Level.FAULT)
				.text("Rack" + this.rackNumber + " Discharge Current High Alarm Level 2")); /* Bit 5 */
		this.addEntry(map, KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW,
				Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Total Voltage Low Alarm Level 2")); /* Bit 4 */
		this.addEntry(map, KEY_ALARM_LEVEL_2_CELL_VOLTAGE_LOW,
				Doc.of(Level.FAULT).text("Cluster 1 Cell Voltage Low Alarm Level 2")); /* Bit 3 */
		this.addEntry(map, KEY_ALARM_LEVEL_2_CHA_CURRENT_HIGH,
				Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Charge Current High Alarm Level 2")); /* Bit 2 */
		this.addEntry(map, KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH,
				Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Total Voltage High Alarm Level 2")); /* Bit 1 */
		this.addEntry(map, KEY_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH,
				Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Cell Voltage High Alarm Level 2")); /* Bit 0 */
		this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW, Doc.of(Level.WARNING)
				.text("Rack" + this.rackNumber + " Cell Discharge Temperature Low Alarm Level 1")); /* Bit 15 */
		this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH, Doc.of(Level.WARNING)
				.text("Rack" + this.rackNumber + " Cell Discharge Temperature High Alarm Level 1")); /* Bit 14 */
		this.addEntry(map, KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH, Doc.of(Level.WARNING)
				.text("Rack" + this.rackNumber + " Total Voltage Diff High Alarm Level 1")); /* Bit 13 */
		this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH, Doc.of(Level.WARNING)
				.text("Rack" + this.rackNumber + " Cell Voltage Diff High Alarm Level 1")); /* Bit 11 */
		this.addEntry(map, KEY_ALARM_LEVEL_1_GR_TEMPERATURE_HIGH, Doc.of(Level.WARNING)
				.text("Rack" + this.rackNumber + " GR Temperature High Alarm Level 1")); /* Bit 10 */
		this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH, Doc.of(Level.WARNING)
				.text("Rack" + this.rackNumber + " Cell temperature Diff High Alarm Level 1")); /* Bit 9 */
		this.addEntry(map, KEY_ALARM_LEVEL_1_SOC_LOW,
				Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " SOC Low Alarm Level 1")); /* Bit 8 */
		this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_LOW, Doc.of(Level.WARNING)
				.text("Rack" + this.rackNumber + " Cell Charge Temperature Low Alarm Level 1")); /* Bit 7 */
		this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH, Doc.of(Level.WARNING)
				.text("Rack" + this.rackNumber + " Cell Charge Temperature High Alarm Level 1")); /* Bit 6 */
		this.addEntry(map, KEY_ALARM_LEVEL_1_DISCHA_CURRENT_HIGH, Doc.of(Level.WARNING)
				.text("Rack" + this.rackNumber + " Discharge Current High Alarm Level 1")); /* Bit 5 */
		this.addEntry(map, KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW,
				Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Total Voltage Low Alarm Level 1")); /* Bit 4 */
		this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_VOLTAGE_LOW,
				Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Cell Voltage Low Alarm Level 1")); /* Bit 3 */
		this.addEntry(map, KEY_ALARM_LEVEL_1_CHA_CURRENT_HIGH, Doc.of(Level.WARNING)
				.text("Rack" + this.rackNumber + " Charge Current High Alarm Level 1")); /* Bit 2 */
		this.addEntry(map, KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH,
				Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Total Voltage High Alarm Level 1")); /* Bit 1 */
		this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_VOLTAGE_HIGH,
				Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Cell Voltage High Alarm Level 1")); /* Bit 0 */
		this.addEntry(map, KEY_RUN_STATE, Doc.of(Enums.ClusterRunState.values())); //
		this.addEntry(map, KEY_FAILURE_INITIALIZATION, Doc.of(Level.FAULT).text("Initialization failure")); /* Bit */
		this.addEntry(map, KEY_FAILURE_EEPROM, Doc.of(Level.FAULT).text("EEPROM fault")); /* Bit 11 */
		this.addEntry(map, KEY_FAILURE_INTRANET_COMMUNICATION,
				Doc.of(Level.FAULT).text("Internal communication fault")); /* Bit 10 */
		this.addEntry(map, KEY_FAILURE_TEMPERATURE_SENSOR_CABLE,
				Doc.of(Level.FAULT).text("Temperature sensor cable fault")); /* Bit 9 */
		this.addEntry(map, KEY_FAILURE_BALANCING_MODULE, Doc.of(Level.OK).text("Balancing module fault")); /* Bit 8 */
		this.addEntry(map, KEY_FAILURE_TEMPERATURE_PCB, Doc.of(Level.FAULT).text("Temperature PCB error")); /* Bit 7 */
		this.addEntry(map, KEY_FAILURE_GR_TEMPERATURE, Doc.of(Level.FAULT).text("GR Temperature error")); /* Bit 6 */
		this.addEntry(map, KEY_FAILURE_TEMP_SENSOR, Doc.of(Level.FAULT).text("Temperature sensor fault")); /* Bit 5 */
		this.addEntry(map, KEY_FAILURE_TEMP_SAMPLING,
				Doc.of(Level.FAULT).text("Temperature sampling fault")); /* Bit 4 */
		this.addEntry(map, KEY_FAILURE_VOLTAGE_SAMPLING,
				Doc.of(Level.FAULT).text("Voltage sampling fault")); /* Bit 3 */
		this.addEntry(map, KEY_FAILURE_LTC6803, Doc.of(Level.FAULT).text("LTC6803 fault")); /* Bit 2 */
		this.addEntry(map, KEY_FAILURE_CONNECTOR_WIRE, Doc.of(Level.FAULT).text("connector wire fault")); /* Bit 1 */
		this.addEntry(map, KEY_FAILURE_SAMPLING_WIRE, Doc.of(Level.FAULT).text("sampling wire fault")); /* Bit 0 */
		this.addEntry(map, KEY_SLEEP, Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE));
		this.addEntry(map, KEY_RESET, Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE));

		// Cell voltages formatted like: "RACK_1_BATTERY_000_VOLTAGE"
		for (var i = 0; i < this.numberOfSlaves; i++) {
			for (var j = i * VOLTAGE_SENSORS_PER_MODULE; j < (i + 1) * VOLTAGE_SENSORS_PER_MODULE; j++) {
				var key = this.getSingleCellPrefix(j) + "_" + VOLTAGE;
				this.addEntry(map, key, new IntegerDoc().unit(Unit.MILLIVOLT));
			}
		}
		// Cell temperatures formatted like : "RACK_1_BATTERY_000_TEMPERATURE"
		for (var i = 0; i < this.numberOfSlaves; i++) {
			for (var j = i * TEMPERATURE_SENSORS_PER_MODULE; j < (i + 1) * TEMPERATURE_SENSORS_PER_MODULE; j++) {
				var key = this.getSingleCellPrefix(j) + "_" + TEMPERATURE;
				this.addEntry(map, key, new IntegerDoc().unit(Unit.DEZIDEGREE_CELSIUS));
			}
		}

		return map;
	}

	protected int getRackNumber() {
		return this.rackNumber;
	}

	protected int getAddressOffset() {
		return this.addressOffset;
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

	private BitsWordElement getBitsWordElement(int addressWithoutOffset, AbstractOpenemsModbusComponent component) {
		return new BitsWordElement(this.addressOffset + addressWithoutOffset, component);
	}

	private UnsignedWordElement getUnsignedWordElement(int addressWithoutOffset) {
		return new UnsignedWordElement(this.addressOffset + addressWithoutOffset);
	}

	private SignedWordElement getSignedWordElement(int addressWithoutOffset) {
		return new SignedWordElement(this.addressOffset + addressWithoutOffset);
	}
}
