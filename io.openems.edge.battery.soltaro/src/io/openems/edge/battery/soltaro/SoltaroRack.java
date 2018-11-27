package io.openems.edge.battery.soltaro;

import java.time.LocalDateTime;
import java.util.Optional;

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
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.doc.AccessMode;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.OptionsEnum;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Bms.Fenecon.Soltaro", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
)
public class SoltaroRack extends AbstractOpenemsModbusComponent implements Battery, OpenemsComponent, EventHandler {

	// Default values for the battery ranges
	public static final int DISCHARGE_MIN_V = 696;
	public static final int CHARGE_MAX_V = 854;
	public static final int DISCHARGE_MAX_A = 20;
	public static final int CHARGE_MAX_A = 20;

	protected final static int SYSTEM_ON = 1;
	protected final static int SYSTEM_OFF = 0;

	private static final int SECURITY_INTERVAL_FOR_COMMANDS_IN_SECONDS = 3;
	private static final int MAX_TIME_FOR_INITIALIZATION_IN_SECONDS = 30;
	public static final Integer CAPACITY_KWH = 50;
	public static final int MAX_POWER_WATT = 50000;

	private final Logger log = LoggerFactory.getLogger(SoltaroRack.class);

	private String modbusBridgeId;
	private BatteryState batteryState;

	@Reference
	protected ConfigurationAdmin cm;

	private LocalDateTime lastCommandSent = LocalDateTime.now(); // timer variable to avoid that commands are sent to
																	// fast
	private LocalDateTime timeForSystemInitialization = null;
	private boolean isStopping = false; // indicates that system is stopping; during that time no commands should be
										// sent

	public SoltaroRack() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id());
		this.modbusBridgeId = config.modbus_id();

		this.batteryState = config.batteryState();
		initializeCallbacks();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void initializeCallbacks() {
		this.channel(ChannelId.BMS_CONTACTOR_CONTROL).onChange(value -> {
			Optional<Enum<?>> ccOpt = value.asEnumOptional();
			if (!ccOpt.isPresent()) {
				return;
			}

			ContactorControl cc = (ContactorControl) ccOpt.get();

			switch (cc) {
			case CONNECTION_INITIATING:
				timeForSystemInitialization = LocalDateTime.now();
				this.channel(Battery.ChannelId.READY_FOR_WORKING).setNextValue(false);
				break;
			case CUT_OFF:
				timeForSystemInitialization = null;
				this.channel(Battery.ChannelId.READY_FOR_WORKING).setNextValue(false);
				isStopping = false;
				break;
			case ON_GRID:
				timeForSystemInitialization = null;
				this.channel(Battery.ChannelId.READY_FOR_WORKING).setNextValue(true);
				break;
			default:
				break;
			}
		});

		this.channel(ChannelId.CLUSTER_1_VOLTAGE).onChange(value -> {
			@SuppressWarnings("unchecked")
			Optional<Integer> vOpt = (Optional<Integer>) value.asOptional();
			if (!vOpt.isPresent()) {
				return;
			}
			int voltage_volt = vOpt.get();
			this.channel(Battery.ChannelId.VOLTAGE).setNextValue(voltage_volt);
		});

		this.channel(ChannelId.CLUSTER_1_MIN_CELL_VOLTAGE).onChange(value -> {
			@SuppressWarnings("unchecked")
			Optional<Integer> vOpt = (Optional<Integer>) value.asOptional();
			if (!vOpt.isPresent()) {
				return;
			}
			int voltage_millivolt = vOpt.get();
			this.channel(Battery.ChannelId.MINIMAL_CELL_VOLTAGE).setNextValue(voltage_millivolt);
		});
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {

		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			handleBatteryState();
			break;
		}
	}

	private void handleBatteryState() {
		// Avoid that commands are written to fast to the battery rack
		if (lastCommandSent.plusSeconds(SECURITY_INTERVAL_FOR_COMMANDS_IN_SECONDS).isAfter(LocalDateTime.now())) {
			return;
		} else {
			lastCommandSent = LocalDateTime.now();
		}

		switch (this.batteryState) {
		case DEFAULT:
			checkSystemState();
			break;
		case OFF:
			stopSystem();
			break;
		case ON:
			startSystem();
			break;
		}
	}

	private void checkSystemState() {

		IntegerReadChannel contactorControlChannel = this.channel(ChannelId.BMS_CONTACTOR_CONTROL);

		Optional<Enum<?>> ccOpt = contactorControlChannel.value().asEnumOptional();
		if (!ccOpt.isPresent()) {
			return;
		}
		ContactorControl cc = (ContactorControl) ccOpt.get();

		if (cc == ContactorControl.CONNECTION_INITIATING) {
			if (timeForSystemInitialization == null || timeForSystemInitialization
					.plusSeconds(MAX_TIME_FOR_INITIALIZATION_IN_SECONDS).isAfter(LocalDateTime.now())) {
				return;
			} else {
				// Maybe battery hung up in precharge mode...stop system, it will be restarted
				// automatically
				this.channel(ChannelId.PRECHARGE_TAKING_TOO_LONG).setNextValue(true);
				stopSystem();
				return;
			}
		}
		if (cc == ContactorControl.CUT_OFF) {
			startSystem();
			return;
		}

		if (cc == ContactorControl.ON_GRID) {
			// TODO: Implement error handling or prevention on system temperature errors/
			// low voltage/...
		}
	}

	public String getModbusBridgeId() {
		return modbusBridgeId;
	}
	

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value() //
				+ "|Discharge:" + this.getDischargeMinVoltage().value() + ";" + this.getDischargeMaxCurrent().value() //
				+ "|Charge:" + this.getChargeMaxVoltage().value() + ";" + this.getChargeMaxCurrent().value();
	}

	private void startSystem() {
		if (isStopping) {
			return;
		}
				
		IntegerWriteChannel contactorControlChannel = this.channel(ChannelId.BMS_CONTACTOR_CONTROL);
		
		Optional<Integer> contactorControlOpt = contactorControlChannel.value().asOptional();
		// To avoid hardware damages do not send start command if system has already started
		if (contactorControlOpt.isPresent() && contactorControlOpt.get() == ContactorControl.ON_GRID.getValue()) {
			return;
		}
		
		try {
			contactorControlChannel.setNextWriteValue(SYSTEM_ON);
		} catch (OpenemsException e) {
			log.error("Error while trying to start system\n" + e.getMessage());
		}
	}

	private void stopSystem() {
		IntegerWriteChannel contactorControlChannel = this.channel(ChannelId.BMS_CONTACTOR_CONTROL);
		
		Optional<Integer> contactorControlOpt = contactorControlChannel.value().asOptional();
		// To avoid hardware damages do not send stop command if system has already stopped
		if (contactorControlOpt.isPresent() && contactorControlOpt.get() == ContactorControl.CUT_OFF.getValue()) {
			return;
		}
		
		try {
			contactorControlChannel.setNextWriteValue(SYSTEM_OFF);
			isStopping = true;
		} catch (OpenemsException e) {
			log.error("Error while trying to stop system\n" + e.getMessage());
		}
	}

	public enum ChargeIndication implements OptionsEnum {

		STANDING(0, "Standing"), DISCHARGING(1, "Discharging"), CHARGING(2, "Charging");

		private int value;
		private String option;

		private ChargeIndication(int value, String option) {
			this.value = value;
			this.option = option;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getOption() {
			return option;
		}
	}

	public enum ContactorControl implements OptionsEnum {

		CUT_OFF(0, "Cut off"), CONNECTION_INITIATING(1, "Connection initiating"), ON_GRID(3, "On grid");

		int value;
		String option;

		private ContactorControl(int value, String option) {
			this.value = value;
			this.option = option;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getOption() {
			return option;
		}
	}

	public enum ClusterRunState implements OptionsEnum {

		NORMAL(0, "Normal"), STOP_CHARGING(1, "Stop charging"), STOP_DISCHARGE(2, "Stop discharging"),
		STANDBY(3, "Standby");

		private int value;
		private String option;

		private ClusterRunState(int value, String option) {
			this.value = value;
			this.option = option;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getOption() {
			return option;
		}
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		BMS_CONTACTOR_CONTROL(new Doc().options(ContactorControl.values())), //
		SYSTEM_OVER_VOLTAGE_PROTECTION(new Doc().unit(Unit.MILLIVOLT)), //
		SYSTEM_UNDER_VOLTAGE_PROTECTION(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_VOLTAGE(new Doc().unit(Unit.VOLT)), //
		CLUSTER_1_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
		CLUSTER_1_CHARGE_INDICATION(new Doc().options(ChargeIndication.values())), //
		CLUSTER_1_SOH(new Doc().unit(Unit.PERCENT)), //
		CLUSTER_1_MAX_CELL_VOLTAGE_ID(new Doc().unit(Unit.NONE)), //
		CLUSTER_1_MAX_CELL_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_MIN_CELL_VOLTAGE_ID(new Doc().unit(Unit.NONE)), //
		CLUSTER_1_MIN_CELL_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_MAX_CELL_TEMPERATURE_ID(new Doc().unit(Unit.NONE)), //
		CLUSTER_1_MAX_CELL_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_MIN_CELL_TEMPERATURE_ID(new Doc().unit(Unit.NONE)), //
		CLUSTER_1_MIN_CELL_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		SYSTEM_INSULATION(new Doc().unit(Unit.KILOOHM)), //
		SYSTEM_ACCEPT_MAX_CHARGE_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
		SYSTEM_ACCEPT_MAX_DISCHARGE_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
		CELL_VOLTAGE_PROTECT(new Doc().accessMode(AccessMode.READ_WRITE).unit(Unit.MILLIVOLT)), //
		CELL_VOLTAGE_RECOVER(new Doc().accessMode(AccessMode.READ_WRITE).unit(Unit.MILLIVOLT)), //

		ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW(
				new Doc().level(Level.WARNING).text("Cluster 1 Cell Discharge Temperature Low Alarm Level 2")), //
		ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH(
				new Doc().level(Level.WARNING).text("Cluster 1 Cell Discharge Temperature High Alarm Level 2")), //
		ALARM_LEVEL_2_INSULATION_LOW(new Doc().level(Level.WARNING).text("Cluster1Insulation Low Alarm Level 2")), //
		ALARM_LEVEL_2_CELL_CHA_TEMP_LOW(
				new Doc().level(Level.WARNING).text("Cluster1 Cell Charge Temperature Low Alarm Level 2")), //
		ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH(
				new Doc().level(Level.WARNING).text("Cluster1 Cell Charge Temperature High Alarm Level 2")), //
		ALARM_LEVEL_2_DISCHA_CURRENT_HIGH(
				new Doc().level(Level.WARNING).text("Cluster 1 Discharge Current High Alarm Level 2")), //
		ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW(
				new Doc().level(Level.WARNING).text("Cluster 1 Total Voltage Low Alarm Level 2")), //
		ALARM_LEVEL_2_CELL_VOLTAGE_LOW(new Doc().level(Level.WARNING).text("Cluster 1 Cell Voltage Low Alarm Level 2")), //
		ALARM_LEVEL_2_CHA_CURRENT_HIGH(
				new Doc().level(Level.WARNING).text("Cluster 1 Charge Current High Alarm Level 2")), //
		ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH(
				new Doc().level(Level.WARNING).text("Cluster 1 Total Voltage High Alarm Level 2")), //
		ALARM_LEVEL_2_CELL_VOLTAGE_HIGH(
				new Doc().level(Level.WARNING).text("Cluster 1 Cell Voltage High Alarm Level 2")), //
		ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW(
				new Doc().level(Level.WARNING).text("Cluster 1 Cell Discharge Temperature Low Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH(
				new Doc().level(Level.WARNING).text("Cluster 1 Cell Discharge Temperature High Alarm Level 1")), //
		ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH(
				new Doc().level(Level.WARNING).text("Cluster1 Total Voltage Diff High Alarm Level 1")), //
		ALARM_LEVEL_1_INSULATION_LOW(new Doc().level(Level.WARNING).text("Cluster1 Insulation Low Alarm Level1")), //
		ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH(
				new Doc().level(Level.WARNING).text("Cluster 1 Cell Voltage Diff High Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH(
				new Doc().level(Level.WARNING).text("Cluster X Cell temperature Diff High Alarm Level 1")), //
		ALARM_LEVEL_1_SOC_LOW(new Doc().level(Level.WARNING).text("Cluster 1 SOC Low Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_CHA_TEMP_LOW(
				new Doc().level(Level.WARNING).text("Cluster 1 Cell Charge Temperature Low Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH(
				new Doc().level(Level.WARNING).text("Cluster 1 Cell Charge Temperature High Alarm Level 1")), //
		ALARM_LEVEL_1_DISCHA_CURRENT_HIGH(
				new Doc().level(Level.WARNING).text("Cluster 1 Discharge Current High Alarm Level 1")), //
		ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW(
				new Doc().level(Level.WARNING).text("Cluster 1 Total Voltage Low Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_VOLTAGE_LOW(new Doc().level(Level.WARNING).text("Cluster 1 Cell Voltage Low Alarm Level 1")), //
		ALARM_LEVEL_1_CHA_CURRENT_HIGH(
				new Doc().level(Level.WARNING).text("Cluster 1 Charge Current High Alarm Level 1")), //
		ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH(
				new Doc().level(Level.WARNING).text("Cluster 1 Total Voltage High Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_VOLTAGE_HIGH(
				new Doc().level(Level.WARNING).text("Cluster 1 Cell Voltage High Alarm Level 1")), //
		CLUSTER_RUN_STATE(new Doc().options(ClusterRunState.values())), //
		FAILURE_INITIALIZATION(new Doc().level(Level.FAULT).text("Initialization failure")), //
		FAILURE_EEPROM(new Doc().level(Level.FAULT).text("EEPROM fault")), //
		FAILURE_INTRANET_COMMUNICATION(new Doc().level(Level.FAULT).text("Intranet communication fault")), //
		FAILURE_TEMP_SAMPLING_LINE(new Doc().level(Level.FAULT).text("Temperature sampling line fault")), //
		FAILURE_BALANCING_MODULE(new Doc().level(Level.FAULT).text("Balancing module fault")), //
		FAILURE_TEMP_SENSOR(new Doc().level(Level.FAULT).text("Temperature sensor fault")), //
		FAILURE_TEMP_SAMPLING(new Doc().level(Level.FAULT).text("Temperature sampling fault")), //
		FAILURE_VOLTAGE_SAMPLING(new Doc().level(Level.FAULT).text("Voltage sampling fault")), //
		FAILURE_LTC6803(new Doc().level(Level.FAULT).text("LTC6803 fault")), //
		FAILURE_CONNECTOR_WIRE(new Doc().level(Level.FAULT).text("connector wire fault")), //
		FAILURE_SAMPLING_WIRE(new Doc().level(Level.FAULT).text("sampling wire fault")), //
		PRECHARGE_TAKING_TOO_LONG(new Doc().level(Level.FAULT).text("precharge time was too long")), //

		CLUSTER_1_BATTERY_000_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_001_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_002_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_003_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_004_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_005_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_006_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_007_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_008_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_009_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_010_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_011_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_012_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_013_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_014_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_015_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_016_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_017_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_018_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_019_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_020_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_021_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_022_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_023_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_024_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_025_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_026_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_027_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_028_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_029_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_030_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_031_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_032_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_033_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_034_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_035_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_036_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_037_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_038_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_039_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_040_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_041_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_042_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_043_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_044_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_045_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_046_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_047_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_048_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_049_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_050_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_051_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_052_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_053_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_054_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_055_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_056_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_057_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_058_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_059_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_060_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_061_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_062_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_063_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_064_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_065_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_066_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_067_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_068_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_069_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_070_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_071_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_072_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_073_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_074_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_075_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_076_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_077_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_078_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_079_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_080_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_081_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_082_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_083_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_084_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_085_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_086_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_087_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_088_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_089_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_090_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_091_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_092_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_093_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_094_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_095_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_096_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_097_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_098_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_099_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_100_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_101_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_102_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_103_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_104_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_105_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_106_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_107_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_108_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_109_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_110_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_111_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_112_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_113_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_114_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_115_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_116_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_117_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_118_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_119_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_120_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_121_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_122_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_123_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_124_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_125_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_126_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_127_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_128_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_129_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_130_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_131_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_132_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_133_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_134_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_135_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_136_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_137_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_138_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_139_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_140_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_141_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_142_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_143_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_144_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_145_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_146_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_147_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_148_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_149_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_150_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_151_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_152_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_153_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_154_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_155_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_156_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_157_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_158_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_159_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_160_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_161_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_162_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_163_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_164_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_165_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_166_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_167_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_168_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_169_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_170_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_171_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_172_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_173_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_174_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_175_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_176_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_177_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_178_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_179_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_180_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_181_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_182_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_183_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_184_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_185_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_186_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_187_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_188_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_189_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_190_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_191_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_192_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_193_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_194_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_195_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_196_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_197_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_198_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_199_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_200_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_201_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_202_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_203_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_204_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_205_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_206_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_207_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_208_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_209_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_210_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_211_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_212_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_213_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_214_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_215_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_216_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_217_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_218_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_219_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_220_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_221_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_222_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_223_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_224_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_225_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_226_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_227_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_228_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_229_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_230_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_231_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_232_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_233_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_234_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_235_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_236_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_237_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_238_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_239_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //

		CLUSTER_1_BATTERY_00_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_01_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_02_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_03_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_04_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_05_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_06_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_07_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_08_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_09_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_10_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_11_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_12_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_13_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_14_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_15_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_16_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_17_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_18_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_19_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_20_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_21_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_22_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_23_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_24_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_25_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_26_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_27_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_28_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_29_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_30_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_31_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_32_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_33_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_34_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_35_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_36_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_37_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_38_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_39_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_40_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_41_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_42_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_43_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_44_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_45_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_46_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_47_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC6WriteRegisterTask(0x2010, //
						m(SoltaroRack.ChannelId.BMS_CONTACTOR_CONTROL, new UnsignedWordElement(0x2010)) //
				), //
				new FC3ReadRegistersTask(0x2010, Priority.HIGH, //
						m(SoltaroRack.ChannelId.BMS_CONTACTOR_CONTROL, new UnsignedWordElement(0x2010)) //
				), //
				new FC3ReadRegistersTask(0x2042, Priority.HIGH, //
						m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, new UnsignedWordElement(0x2042), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
				), //
				new FC3ReadRegistersTask(0x2046, Priority.HIGH, //
						m(SoltaroRack.ChannelId.CELL_VOLTAGE_PROTECT, new UnsignedWordElement(0x2046)), //
						m(SoltaroRack.ChannelId.CELL_VOLTAGE_RECOVER, new UnsignedWordElement(0x2047)), //
						m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(0x2048), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
				), //
				new FC6WriteRegisterTask(0x2046, //
						m(SoltaroRack.ChannelId.CELL_VOLTAGE_PROTECT, new UnsignedWordElement(0x2046)) //
				), //
				new FC6WriteRegisterTask(0x2047, //
						m(SoltaroRack.ChannelId.CELL_VOLTAGE_RECOVER, new UnsignedWordElement(0x2047)) //
				), //
				new FC3ReadRegistersTask(0x2100, Priority.LOW, //
						m(SoltaroRack.ChannelId.CLUSTER_1_VOLTAGE, new UnsignedWordElement(0x2100), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(SoltaroRack.ChannelId.CLUSTER_1_CURRENT, new UnsignedWordElement(0x2101), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(SoltaroRack.ChannelId.CLUSTER_1_CHARGE_INDICATION, new UnsignedWordElement(0x2102)), //
						m(Battery.ChannelId.SOC, new UnsignedWordElement(0x2103)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_SOH, new UnsignedWordElement(0x2104)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_MAX_CELL_VOLTAGE_ID, new UnsignedWordElement(0x2105)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_MAX_CELL_VOLTAGE, new UnsignedWordElement(0x2106)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_MIN_CELL_VOLTAGE_ID, new UnsignedWordElement(0x2107)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_MIN_CELL_VOLTAGE, new UnsignedWordElement(0x2108)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_MAX_CELL_TEMPERATURE_ID, new UnsignedWordElement(0x2109)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_MAX_CELL_TEMPERATURE, new UnsignedWordElement(0x210A)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_MIN_CELL_TEMPERATURE_ID, new UnsignedWordElement(0x210B)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_MIN_CELL_TEMPERATURE, new UnsignedWordElement(0x210C)), //
						new DummyRegisterElement(0x210D, 0x2115), //
						m(SoltaroRack.ChannelId.SYSTEM_INSULATION, new UnsignedWordElement(0x2116)) //
				), //
				new FC3ReadRegistersTask(0x2160, Priority.HIGH, //
						m(Battery.ChannelId.CHARGE_MAX_CURRENT, new UnsignedWordElement(0x2160), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(Battery.ChannelId.DISCHARGE_MAX_CURRENT, new UnsignedWordElement(0x2161), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
				), //
				new FC3ReadRegistersTask(0x2140, Priority.LOW, //
						bm(new UnsignedWordElement(0x2140)) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_HIGH, 0) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH, 1) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_2_CHA_CURRENT_HIGH, 2) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_LOW, 3) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW, 4) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_2_DISCHA_CURRENT_HIGH, 5) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH, 6) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_LOW, 7) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_2_INSULATION_LOW, 12) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH, 14) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW, 15) //
								.build(), //
						bm(new UnsignedWordElement(0x2141)) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_1_CELL_VOLTAGE_HIGH, 0) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH, 1) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_1_CHA_CURRENT_HIGH, 2) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_1_CELL_VOLTAGE_LOW, 3) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW, 4) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_1_DISCHA_CURRENT_HIGH, 5) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH, 6) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_1_CELL_CHA_TEMP_LOW, 7) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_1_SOC_LOW, 8) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH, 9) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH, 11) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_1_INSULATION_LOW, 12) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH, 13) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH, 14) //
								.m(SoltaroRack.ChannelId.ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW, 15) //
								.build(), //
						m(SoltaroRack.ChannelId.CLUSTER_RUN_STATE, new UnsignedWordElement(0x2142)) //
				), //
				new FC3ReadRegistersTask(0x2185, Priority.LOW, //
						bm(new UnsignedWordElement(0x2185)) //
								.m(SoltaroRack.ChannelId.FAILURE_SAMPLING_WIRE, 0)//
								.m(SoltaroRack.ChannelId.FAILURE_CONNECTOR_WIRE, 1)//
								.m(SoltaroRack.ChannelId.FAILURE_LTC6803, 2)//
								.m(SoltaroRack.ChannelId.FAILURE_VOLTAGE_SAMPLING, 3)//
								.m(SoltaroRack.ChannelId.FAILURE_TEMP_SAMPLING, 4)//
								.m(SoltaroRack.ChannelId.FAILURE_TEMP_SENSOR, 5)//
								.m(SoltaroRack.ChannelId.FAILURE_BALANCING_MODULE, 8)//
								.m(SoltaroRack.ChannelId.FAILURE_TEMP_SAMPLING_LINE, 9)//
								.m(SoltaroRack.ChannelId.FAILURE_INTRANET_COMMUNICATION, 10)//
								.m(SoltaroRack.ChannelId.FAILURE_EEPROM, 11)//
								.m(SoltaroRack.ChannelId.FAILURE_INITIALIZATION, 12)//
								.build() //
				), //
				new FC3ReadRegistersTask(0x2800, Priority.LOW, //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_000_VOLTAGE, new UnsignedWordElement(0x2800)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_001_VOLTAGE, new UnsignedWordElement(0x2801)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_002_VOLTAGE, new UnsignedWordElement(0x2802)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_003_VOLTAGE, new UnsignedWordElement(0x2803)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_004_VOLTAGE, new UnsignedWordElement(0x2804)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_005_VOLTAGE, new UnsignedWordElement(0x2805)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_006_VOLTAGE, new UnsignedWordElement(0x2806)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_007_VOLTAGE, new UnsignedWordElement(0x2807)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_008_VOLTAGE, new UnsignedWordElement(0x2808)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_009_VOLTAGE, new UnsignedWordElement(0x2809)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_010_VOLTAGE, new UnsignedWordElement(0x280A)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_011_VOLTAGE, new UnsignedWordElement(0x280B)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_012_VOLTAGE, new UnsignedWordElement(0x280C)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_013_VOLTAGE, new UnsignedWordElement(0x280D)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_014_VOLTAGE, new UnsignedWordElement(0x280E)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_015_VOLTAGE, new UnsignedWordElement(0x280F)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_016_VOLTAGE, new UnsignedWordElement(0x2810)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_017_VOLTAGE, new UnsignedWordElement(0x2811)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_018_VOLTAGE, new UnsignedWordElement(0x2812)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_019_VOLTAGE, new UnsignedWordElement(0x2813)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_020_VOLTAGE, new UnsignedWordElement(0x2814)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_021_VOLTAGE, new UnsignedWordElement(0x2815)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_022_VOLTAGE, new UnsignedWordElement(0x2816)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_023_VOLTAGE, new UnsignedWordElement(0x2817)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_024_VOLTAGE, new UnsignedWordElement(0x2818)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_025_VOLTAGE, new UnsignedWordElement(0x2819)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_026_VOLTAGE, new UnsignedWordElement(0x281A)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_027_VOLTAGE, new UnsignedWordElement(0x281B)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_028_VOLTAGE, new UnsignedWordElement(0x281C)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_029_VOLTAGE, new UnsignedWordElement(0x281D)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_030_VOLTAGE, new UnsignedWordElement(0x281E)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_031_VOLTAGE, new UnsignedWordElement(0x281F)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_032_VOLTAGE, new UnsignedWordElement(0x2820)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_033_VOLTAGE, new UnsignedWordElement(0x2821)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_034_VOLTAGE, new UnsignedWordElement(0x2822)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_035_VOLTAGE, new UnsignedWordElement(0x2823)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_036_VOLTAGE, new UnsignedWordElement(0x2824)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_037_VOLTAGE, new UnsignedWordElement(0x2825)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_038_VOLTAGE, new UnsignedWordElement(0x2826)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_039_VOLTAGE, new UnsignedWordElement(0x2827)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_040_VOLTAGE, new UnsignedWordElement(0x2828)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_041_VOLTAGE, new UnsignedWordElement(0x2829)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_042_VOLTAGE, new UnsignedWordElement(0x282A)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_043_VOLTAGE, new UnsignedWordElement(0x282B)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_044_VOLTAGE, new UnsignedWordElement(0x282C)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_045_VOLTAGE, new UnsignedWordElement(0x282D)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_046_VOLTAGE, new UnsignedWordElement(0x282E)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_047_VOLTAGE, new UnsignedWordElement(0x282F)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_048_VOLTAGE, new UnsignedWordElement(0x2830)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_049_VOLTAGE, new UnsignedWordElement(0x2831)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_050_VOLTAGE, new UnsignedWordElement(0x2832)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_051_VOLTAGE, new UnsignedWordElement(0x2833)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_052_VOLTAGE, new UnsignedWordElement(0x2834)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_053_VOLTAGE, new UnsignedWordElement(0x2835)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_054_VOLTAGE, new UnsignedWordElement(0x2836)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_055_VOLTAGE, new UnsignedWordElement(0x2837)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_056_VOLTAGE, new UnsignedWordElement(0x2838)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_057_VOLTAGE, new UnsignedWordElement(0x2839)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_058_VOLTAGE, new UnsignedWordElement(0x283A)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_059_VOLTAGE, new UnsignedWordElement(0x283B)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_060_VOLTAGE, new UnsignedWordElement(0x283C)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_061_VOLTAGE, new UnsignedWordElement(0x283D)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_062_VOLTAGE, new UnsignedWordElement(0x283E)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_063_VOLTAGE, new UnsignedWordElement(0x283F)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_064_VOLTAGE, new UnsignedWordElement(0x2840)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_065_VOLTAGE, new UnsignedWordElement(0x2841)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_066_VOLTAGE, new UnsignedWordElement(0x2842)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_067_VOLTAGE, new UnsignedWordElement(0x2843)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_068_VOLTAGE, new UnsignedWordElement(0x2844)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_069_VOLTAGE, new UnsignedWordElement(0x2845)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_070_VOLTAGE, new UnsignedWordElement(0x2846)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_071_VOLTAGE, new UnsignedWordElement(0x2847)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_072_VOLTAGE, new UnsignedWordElement(0x2848)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_073_VOLTAGE, new UnsignedWordElement(0x2849)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_074_VOLTAGE, new UnsignedWordElement(0x284A)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_075_VOLTAGE, new UnsignedWordElement(0x284B)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_076_VOLTAGE, new UnsignedWordElement(0x284C)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_077_VOLTAGE, new UnsignedWordElement(0x284D)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_078_VOLTAGE, new UnsignedWordElement(0x284E)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_079_VOLTAGE, new UnsignedWordElement(0x284F)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_080_VOLTAGE, new UnsignedWordElement(0x2850)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_081_VOLTAGE, new UnsignedWordElement(0x2851)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_082_VOLTAGE, new UnsignedWordElement(0x2852)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_083_VOLTAGE, new UnsignedWordElement(0x2853)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_084_VOLTAGE, new UnsignedWordElement(0x2854)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_085_VOLTAGE, new UnsignedWordElement(0x2855)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_086_VOLTAGE, new UnsignedWordElement(0x2856)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_087_VOLTAGE, new UnsignedWordElement(0x2857)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_088_VOLTAGE, new UnsignedWordElement(0x2858)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_089_VOLTAGE, new UnsignedWordElement(0x2859)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_090_VOLTAGE, new UnsignedWordElement(0x285A)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_091_VOLTAGE, new UnsignedWordElement(0x285B)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_092_VOLTAGE, new UnsignedWordElement(0x285C)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_093_VOLTAGE, new UnsignedWordElement(0x285D)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_094_VOLTAGE, new UnsignedWordElement(0x285E)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_095_VOLTAGE, new UnsignedWordElement(0x285F)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_096_VOLTAGE, new UnsignedWordElement(0x2860)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_097_VOLTAGE, new UnsignedWordElement(0x2861)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_098_VOLTAGE, new UnsignedWordElement(0x2862)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_099_VOLTAGE, new UnsignedWordElement(0x2863)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_100_VOLTAGE, new UnsignedWordElement(0x2864)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_101_VOLTAGE, new UnsignedWordElement(0x2865)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_102_VOLTAGE, new UnsignedWordElement(0x2866)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_103_VOLTAGE, new UnsignedWordElement(0x2867)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_104_VOLTAGE, new UnsignedWordElement(0x2868)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_105_VOLTAGE, new UnsignedWordElement(0x2869)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_106_VOLTAGE, new UnsignedWordElement(0x286A)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_107_VOLTAGE, new UnsignedWordElement(0x286B)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_108_VOLTAGE, new UnsignedWordElement(0x286C)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_109_VOLTAGE, new UnsignedWordElement(0x286D)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_110_VOLTAGE, new UnsignedWordElement(0x286E)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_111_VOLTAGE, new UnsignedWordElement(0x286F)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_112_VOLTAGE, new UnsignedWordElement(0x2870)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_113_VOLTAGE, new UnsignedWordElement(0x2871)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_114_VOLTAGE, new UnsignedWordElement(0x2872)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_115_VOLTAGE, new UnsignedWordElement(0x2873)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_116_VOLTAGE, new UnsignedWordElement(0x2874)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_117_VOLTAGE, new UnsignedWordElement(0x2875)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_118_VOLTAGE, new UnsignedWordElement(0x2876)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_119_VOLTAGE, new UnsignedWordElement(0x2877)) //

				), //
				new FC3ReadRegistersTask(0x2878, Priority.LOW, //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_120_VOLTAGE, new UnsignedWordElement(0x2878)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_121_VOLTAGE, new UnsignedWordElement(0x2879)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_122_VOLTAGE, new UnsignedWordElement(0x287A)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_123_VOLTAGE, new UnsignedWordElement(0x287B)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_124_VOLTAGE, new UnsignedWordElement(0x287C)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_125_VOLTAGE, new UnsignedWordElement(0x287D)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_126_VOLTAGE, new UnsignedWordElement(0x287E)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_127_VOLTAGE, new UnsignedWordElement(0x287F)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_128_VOLTAGE, new UnsignedWordElement(0x2880)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_129_VOLTAGE, new UnsignedWordElement(0x2881)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_130_VOLTAGE, new UnsignedWordElement(0x2882)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_131_VOLTAGE, new UnsignedWordElement(0x2883)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_132_VOLTAGE, new UnsignedWordElement(0x2884)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_133_VOLTAGE, new UnsignedWordElement(0x2885)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_134_VOLTAGE, new UnsignedWordElement(0x2886)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_135_VOLTAGE, new UnsignedWordElement(0x2887)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_136_VOLTAGE, new UnsignedWordElement(0x2888)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_137_VOLTAGE, new UnsignedWordElement(0x2889)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_138_VOLTAGE, new UnsignedWordElement(0x288A)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_139_VOLTAGE, new UnsignedWordElement(0x288B)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_140_VOLTAGE, new UnsignedWordElement(0x288C)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_141_VOLTAGE, new UnsignedWordElement(0x288D)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_142_VOLTAGE, new UnsignedWordElement(0x288E)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_143_VOLTAGE, new UnsignedWordElement(0x288F)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_144_VOLTAGE, new UnsignedWordElement(0x2890)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_145_VOLTAGE, new UnsignedWordElement(0x2891)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_146_VOLTAGE, new UnsignedWordElement(0x2892)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_147_VOLTAGE, new UnsignedWordElement(0x2893)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_148_VOLTAGE, new UnsignedWordElement(0x2894)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_149_VOLTAGE, new UnsignedWordElement(0x2895)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_150_VOLTAGE, new UnsignedWordElement(0x2896)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_151_VOLTAGE, new UnsignedWordElement(0x2897)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_152_VOLTAGE, new UnsignedWordElement(0x2898)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_153_VOLTAGE, new UnsignedWordElement(0x2899)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_154_VOLTAGE, new UnsignedWordElement(0x289A)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_155_VOLTAGE, new UnsignedWordElement(0x289B)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_156_VOLTAGE, new UnsignedWordElement(0x289C)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_157_VOLTAGE, new UnsignedWordElement(0x289D)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_158_VOLTAGE, new UnsignedWordElement(0x289E)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_159_VOLTAGE, new UnsignedWordElement(0x289F)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_160_VOLTAGE, new UnsignedWordElement(0x28A0)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_161_VOLTAGE, new UnsignedWordElement(0x28A1)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_162_VOLTAGE, new UnsignedWordElement(0x28A2)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_163_VOLTAGE, new UnsignedWordElement(0x28A3)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_164_VOLTAGE, new UnsignedWordElement(0x28A4)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_165_VOLTAGE, new UnsignedWordElement(0x28A5)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_166_VOLTAGE, new UnsignedWordElement(0x28A6)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_167_VOLTAGE, new UnsignedWordElement(0x28A7)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_168_VOLTAGE, new UnsignedWordElement(0x28A8)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_169_VOLTAGE, new UnsignedWordElement(0x28A9)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_170_VOLTAGE, new UnsignedWordElement(0x28AA)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_171_VOLTAGE, new UnsignedWordElement(0x28AB)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_172_VOLTAGE, new UnsignedWordElement(0x28AC)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_173_VOLTAGE, new UnsignedWordElement(0x28AD)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_174_VOLTAGE, new UnsignedWordElement(0x28AE)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_175_VOLTAGE, new UnsignedWordElement(0x28AF)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_176_VOLTAGE, new UnsignedWordElement(0x28B0)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_177_VOLTAGE, new UnsignedWordElement(0x28B1)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_178_VOLTAGE, new UnsignedWordElement(0x28B2)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_179_VOLTAGE, new UnsignedWordElement(0x28B3)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_180_VOLTAGE, new UnsignedWordElement(0x28B4)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_181_VOLTAGE, new UnsignedWordElement(0x28B5)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_182_VOLTAGE, new UnsignedWordElement(0x28B6)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_183_VOLTAGE, new UnsignedWordElement(0x28B7)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_184_VOLTAGE, new UnsignedWordElement(0x28B8)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_185_VOLTAGE, new UnsignedWordElement(0x28B9)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_186_VOLTAGE, new UnsignedWordElement(0x28BA)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_187_VOLTAGE, new UnsignedWordElement(0x28BB)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_188_VOLTAGE, new UnsignedWordElement(0x28BC)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_189_VOLTAGE, new UnsignedWordElement(0x28BD)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_190_VOLTAGE, new UnsignedWordElement(0x28BE)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_191_VOLTAGE, new UnsignedWordElement(0x28BF)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_192_VOLTAGE, new UnsignedWordElement(0x28C0)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_193_VOLTAGE, new UnsignedWordElement(0x28C1)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_194_VOLTAGE, new UnsignedWordElement(0x28C2)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_195_VOLTAGE, new UnsignedWordElement(0x28C3)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_196_VOLTAGE, new UnsignedWordElement(0x28C4)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_197_VOLTAGE, new UnsignedWordElement(0x28C5)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_198_VOLTAGE, new UnsignedWordElement(0x28C6)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_199_VOLTAGE, new UnsignedWordElement(0x28C7)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_200_VOLTAGE, new UnsignedWordElement(0x28C8)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_201_VOLTAGE, new UnsignedWordElement(0x28C9)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_202_VOLTAGE, new UnsignedWordElement(0x28CA)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_203_VOLTAGE, new UnsignedWordElement(0x28CB)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_204_VOLTAGE, new UnsignedWordElement(0x28CC)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_205_VOLTAGE, new UnsignedWordElement(0x28CD)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_206_VOLTAGE, new UnsignedWordElement(0x28CE)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_207_VOLTAGE, new UnsignedWordElement(0x28CF)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_208_VOLTAGE, new UnsignedWordElement(0x28D0)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_209_VOLTAGE, new UnsignedWordElement(0x28D1)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_210_VOLTAGE, new UnsignedWordElement(0x28D2)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_211_VOLTAGE, new UnsignedWordElement(0x28D3)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_212_VOLTAGE, new UnsignedWordElement(0x28D4)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_213_VOLTAGE, new UnsignedWordElement(0x28D5)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_214_VOLTAGE, new UnsignedWordElement(0x28D6)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_215_VOLTAGE, new UnsignedWordElement(0x28D7)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_216_VOLTAGE, new UnsignedWordElement(0x28D8)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_217_VOLTAGE, new UnsignedWordElement(0x28D9)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_218_VOLTAGE, new UnsignedWordElement(0x28DA)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_219_VOLTAGE, new UnsignedWordElement(0x28DB)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_220_VOLTAGE, new UnsignedWordElement(0x28DC)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_221_VOLTAGE, new UnsignedWordElement(0x28DD)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_222_VOLTAGE, new UnsignedWordElement(0x28DE)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_223_VOLTAGE, new UnsignedWordElement(0x28DF)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_224_VOLTAGE, new UnsignedWordElement(0x28E0)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_225_VOLTAGE, new UnsignedWordElement(0x28E1)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_226_VOLTAGE, new UnsignedWordElement(0x28E2)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_227_VOLTAGE, new UnsignedWordElement(0x28E3)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_228_VOLTAGE, new UnsignedWordElement(0x28E4)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_229_VOLTAGE, new UnsignedWordElement(0x28E5)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_230_VOLTAGE, new UnsignedWordElement(0x28E6)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_231_VOLTAGE, new UnsignedWordElement(0x28E7)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_232_VOLTAGE, new UnsignedWordElement(0x28E8)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_233_VOLTAGE, new UnsignedWordElement(0x28E9)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_234_VOLTAGE, new UnsignedWordElement(0x28EA)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_235_VOLTAGE, new UnsignedWordElement(0x28EB)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_236_VOLTAGE, new UnsignedWordElement(0x28EC)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_237_VOLTAGE, new UnsignedWordElement(0x28ED)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_238_VOLTAGE, new UnsignedWordElement(0x28EE)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_239_VOLTAGE, new UnsignedWordElement(0x28EF)) //

				), //
				new FC3ReadRegistersTask(0x2C00, Priority.LOW, //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_00_TEMPERATURE, new UnsignedWordElement(0x2C00)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_01_TEMPERATURE, new UnsignedWordElement(0x2C01)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_02_TEMPERATURE, new UnsignedWordElement(0x2C02)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_03_TEMPERATURE, new UnsignedWordElement(0x2C03)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_04_TEMPERATURE, new UnsignedWordElement(0x2C04)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_05_TEMPERATURE, new UnsignedWordElement(0x2C05)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_06_TEMPERATURE, new UnsignedWordElement(0x2C06)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_07_TEMPERATURE, new UnsignedWordElement(0x2C07)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_08_TEMPERATURE, new UnsignedWordElement(0x2C08)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_09_TEMPERATURE, new UnsignedWordElement(0x2C09)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_10_TEMPERATURE, new UnsignedWordElement(0x2C0A)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_11_TEMPERATURE, new UnsignedWordElement(0x2C0B)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_12_TEMPERATURE, new UnsignedWordElement(0x2C0C)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_13_TEMPERATURE, new UnsignedWordElement(0x2C0D)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_14_TEMPERATURE, new UnsignedWordElement(0x2C0E)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_15_TEMPERATURE, new UnsignedWordElement(0x2C0F)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_16_TEMPERATURE, new UnsignedWordElement(0x2C10)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_17_TEMPERATURE, new UnsignedWordElement(0x2C11)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_18_TEMPERATURE, new UnsignedWordElement(0x2C12)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_19_TEMPERATURE, new UnsignedWordElement(0x2C13)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_20_TEMPERATURE, new UnsignedWordElement(0x2C14)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_21_TEMPERATURE, new UnsignedWordElement(0x2C15)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_22_TEMPERATURE, new UnsignedWordElement(0x2C16)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_23_TEMPERATURE, new UnsignedWordElement(0x2C17)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_24_TEMPERATURE, new UnsignedWordElement(0x2C18)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_25_TEMPERATURE, new UnsignedWordElement(0x2C19)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_26_TEMPERATURE, new UnsignedWordElement(0x2C1A)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_27_TEMPERATURE, new UnsignedWordElement(0x2C1B)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_28_TEMPERATURE, new UnsignedWordElement(0x2C1C)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_29_TEMPERATURE, new UnsignedWordElement(0x2C1D)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_30_TEMPERATURE, new UnsignedWordElement(0x2C1E)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_31_TEMPERATURE, new UnsignedWordElement(0x2C1F)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_32_TEMPERATURE, new UnsignedWordElement(0x2C20)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_33_TEMPERATURE, new UnsignedWordElement(0x2C21)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_34_TEMPERATURE, new UnsignedWordElement(0x2C22)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_35_TEMPERATURE, new UnsignedWordElement(0x2C23)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_36_TEMPERATURE, new UnsignedWordElement(0x2C24)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_37_TEMPERATURE, new UnsignedWordElement(0x2C25)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_38_TEMPERATURE, new UnsignedWordElement(0x2C26)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_39_TEMPERATURE, new UnsignedWordElement(0x2C27)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_40_TEMPERATURE, new UnsignedWordElement(0x2C28)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_41_TEMPERATURE, new UnsignedWordElement(0x2C29)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_42_TEMPERATURE, new UnsignedWordElement(0x2C2A)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_43_TEMPERATURE, new UnsignedWordElement(0x2C2B)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_44_TEMPERATURE, new UnsignedWordElement(0x2C2C)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_45_TEMPERATURE, new UnsignedWordElement(0x2C2D)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_46_TEMPERATURE, new UnsignedWordElement(0x2C2E)), //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_47_TEMPERATURE, new UnsignedWordElement(0x2C2F)) //
				)//
		); //
	}
}
