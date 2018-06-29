package io.openems.edge.battery.soltaro;

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

import io.openems.common.exceptions.InvalidValueException;
import io.openems.edge.battery.api.Bms;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.bridge.modbus.api.task.Priority;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;


/**
 * Implements the FENECON Soltaro battery system.
 */
@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Bms.Fenecon.Soltaro", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
)
public class SoltaroRack extends AbstractOpenemsModbusComponent
		implements Bms, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(AbstractOpenemsModbusComponent.class);

	protected final static int UNIT_ID = 0x1;
	
	
	protected static final int CONTACTOR_CONTROL_ADRESS = 0x2010;
	protected static final int REGISTER_BASE_ADRESS = 0x2000;

	protected static final int STATUS_OFFSET = 0x0100;
	protected static final int ALARM_OFFSET = 0x0140;
	protected static final int FAILURE_OFFSET = 0x0185;
	protected static final int VOLTAGE_OFFSET = 0x0800;
	protected static final int TEMPERATURE_OFFSET = 0x0C00;

	private String modbusBridgeId;
	
	@Reference
	protected ConfigurationAdmin cm;

	public SoltaroRack() {
		log.info("initializing channels");
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
		
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled(), UNIT_ID, this.cm, "Modbus",
				config.modbus_id());
		this.modbusBridgeId = config.modbus_id();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public String getModbusBridgeId() {
		return modbusBridgeId;
	}

	public enum ChargeIndication {
		
		STANDING(0),
		DISCHARGING(1),
		CHARGING(2);
		
		int value;

		private ChargeIndication(int value) {
			this.value = value;
		}
	}
	
public enum ContactorControl {
		
	CUT_OFF(0),
	CONNECTION_INITIATING(1),
	ON_GRID(3);
		
		int value;

		private ContactorControl(int value) {
			this.value = value;
		}
	}

public enum ClusterRunState {
	
	NORMAL(0),
	STOP_CHARGING(1),
	STOP_DISCHARGE(2),
	STANDBY(3);
		
		int value;

		private ClusterRunState(int value) {
			this.value = value;
		}
	}
	

// TODO Temperature units are in 0.1°C, this is not done correctly yet!
	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		BMS_CONTACTOR_CONTROL(new Doc() //
				.option(ContactorControl.CUT_OFF.value, ContactorControl.CUT_OFF) //
				.option(ContactorControl.CONNECTION_INITIATING.value, ContactorControl.CONNECTION_INITIATING) //
				.option(ContactorControl.ON_GRID.value, ContactorControl.ON_GRID) //
		),
		CLUSTER_1_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
		CLUSTER_1_CHARGE_INDICATION(new Doc() //
				.option(ChargeIndication.STANDING.value, ChargeIndication.STANDING) //
				.option(ChargeIndication.DISCHARGING.value, ChargeIndication.DISCHARGING) //
				.option(ChargeIndication.CHARGING.value, ChargeIndication.CHARGING)
		), //
		CLUSTER_1_SOH(new Doc().unit(Unit.PERCENT)), //
		CLUSTER_1_MAX_CELL_VOLTAGE_ID(new Doc().unit(Unit.NONE)), //
		CLUSTER_1_MAX_CELL_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_MIN_CELL_VOLTAGE_ID(new Doc().unit(Unit.NONE)), //
		CLUSTER_1_MIN_CELL_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_MAX_CELL_TEMPERATURE_ID(new Doc().unit(Unit.NONE)), //
		CLUSTER_1_MAX_CELL_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)), //
		CLUSTER_1_MIN_CELL_TEMPERATURE_ID(new Doc().unit(Unit.NONE)), //
		CLUSTER_1_MIN_CELL_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)), //
		SYSTEM_INSULATION(new Doc().unit(Unit.KILOOHM)),
		ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW(new Doc().level(Level.WARNING).text("Cluster 1 Cell Discharge Temperature Low Alarm Level 2")), //
		ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH(new Doc().level(Level.WARNING).text("Cluster 1 Cell Discharge Temperature High Alarm Level 2")), //
		ALARM_LEVEL_2_INSULATION_LOW(new Doc().level(Level.WARNING).text("Cluster1Insulation Low Alarm Level 2")), //
		ALARM_LEVEL_2_CELL_CHA_TEMP_LOW(new Doc().level(Level.WARNING).text("Cluster1 Cell Charge Temperature Low Alarm Level 2")), //
		ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH(new Doc().level(Level.WARNING).text("Cluster1 Cell Charge Temperature High Alarm Level 2")), //
		ALARM_LEVEL_2_DISCHA_CURRENT_HIGH(new Doc().level(Level.WARNING).text("Cluster 1 Discharge Current High Alarm Level 2")), //
		ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW(new Doc().level(Level.WARNING).text("Cluster 1 Total Voltage Low Alarm Level 2")), //
		ALARM_LEVEL_2_CELL_VOLTAGE_LOW(new Doc().level(Level.WARNING).text("Cluster 1 Cell Voltage Low Alarm Level 2")), //
		ALARM_LEVEL_2_CHA_CURRENT_HIGH(new Doc().level(Level.WARNING).text("Cluster 1 Charge Current High Alarm Level 2")), //
		ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH(new Doc().level(Level.WARNING).text("Cluster 1 Total Voltage High Alarm Level 2")), //
		ALARM_LEVEL_2_CELL_VOLTAGE_HIGH(new Doc().level(Level.WARNING).text("Cluster 1 Cell Voltage High Alarm Level 2")), //
		ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW(new Doc().level(Level.WARNING).text("Cluster 1 Cell Discharge Temperature Low Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH(new Doc().level(Level.WARNING).text("Cluster 1 Cell Discharge Temperature High Alarm Level 1")), //
		ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH(new Doc().level(Level.WARNING).text("Cluster1 Total Voltage Diff High Alarm Level 1")), //
		ALARM_LEVEL_1_INSULATION_LOW(new Doc().level(Level.WARNING).text("Cluster1 Insulation Low Alarm Level1")), //
		ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH(new Doc().level(Level.WARNING).text("Cluster 1 Cell Voltage Diff High Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH(new Doc().level(Level.WARNING).text("Cluster X Cell temperature Diff High Alarm Level 1")), //
		ALARM_LEVEL_1_SOC_LOW(new Doc().level(Level.WARNING).text("Cluster 1 SOC Low Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_CHA_TEMP_LOW(new Doc().level(Level.WARNING).text("Cluster 1 Cell Charge Temperature Low Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH(new Doc().level(Level.WARNING).text("Cluster 1 Cell Charge Temperature High Alarm Level 1")), //
		ALARM_LEVEL_1_DISCHA_CURRENT_HIGH(new Doc().level(Level.WARNING).text("Cluster 1 Discharge Current High Alarm Level 1")), //
		ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW(new Doc().level(Level.WARNING).text("Cluster 1 Total Voltage Low Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_VOLTAGE_LOW(new Doc().level(Level.WARNING).text("Cluster 1 Cell Voltage Low Alarm Level 1")), //
		ALARM_LEVEL_1_CHA_CURRENT_HIGH(new Doc().level(Level.WARNING).text("Cluster 1 Charge Current High Alarm Level 1")), //
		ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH(new Doc().level(Level.WARNING).text("Cluster 1 Total Voltage High Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_VOLTAGE_HIGH(new Doc().level(Level.WARNING).text("Cluster 1 Cell Voltage High Alarm Level 1")), //
		CLUSTER_RUN_STATE(new Doc() //
				.option(ClusterRunState.NORMAL.value, ClusterRunState.NORMAL) //
				.option(ClusterRunState.STOP_CHARGING.value, ClusterRunState.STOP_CHARGING) //
				.option(ClusterRunState.STOP_DISCHARGE.value, ClusterRunState.STOP_DISCHARGE)
				.option(ClusterRunState.STANDBY.value, ClusterRunState.STANDBY)
		),
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
		
		CLUSTER_1_BATTERY_000_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_001_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_002_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_003_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_004_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_005_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_006_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_007_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_008_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_009_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_010_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_011_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_012_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_013_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_014_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_015_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_016_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_017_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_018_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_019_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_020_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_021_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_022_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_023_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_024_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_025_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_026_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_027_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_028_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_029_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_030_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_031_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_032_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_033_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_034_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_035_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_036_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_037_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_038_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_039_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_040_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_041_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_042_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_043_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_044_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_045_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_046_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_047_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_048_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_049_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_050_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_051_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_052_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_053_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_054_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_055_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_056_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_057_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_058_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_059_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_060_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_061_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_062_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_063_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_064_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_065_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_066_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_067_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_068_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_069_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_070_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_071_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_072_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_073_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_074_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_075_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_076_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_077_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_078_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_079_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_080_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_081_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_082_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_083_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_084_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_085_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_086_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_087_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_088_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_089_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_090_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_091_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_092_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_093_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_094_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_095_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_096_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_097_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_098_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_099_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_100_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_101_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_102_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_103_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_104_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_105_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_106_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_107_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_108_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_109_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_110_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_111_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_112_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_113_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_114_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_115_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_116_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_117_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_118_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_119_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_120_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_121_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_122_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_123_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_124_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_125_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_126_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_127_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_128_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_129_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_130_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_131_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_132_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_133_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_134_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_135_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_136_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_137_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_138_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_139_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_140_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_141_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_142_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_143_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_144_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_145_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_146_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_147_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_148_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_149_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_150_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_151_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_152_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_153_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_154_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_155_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_156_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_157_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_158_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_159_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_160_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_161_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_162_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_163_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_164_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_165_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_166_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_167_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_168_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_169_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_170_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_171_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_172_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_173_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_174_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_175_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_176_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_177_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_178_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_179_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_180_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_181_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_182_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_183_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_184_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_185_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_186_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_187_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_188_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_189_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_190_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_191_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_192_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_193_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_194_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_195_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_196_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_197_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_198_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_199_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_200_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_201_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_202_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_203_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_204_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_205_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_206_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_207_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_208_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_209_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_210_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_211_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_212_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_213_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_214_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_215_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_216_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_217_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_218_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_219_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_220_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_221_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_222_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_223_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_224_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_225_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_226_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_227_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_228_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_229_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_230_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_231_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_232_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_233_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_234_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_235_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_236_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_237_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_238_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		CLUSTER_1_BATTERY_239_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),

		CLUSTER_1_BATTERY_00_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_01_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_02_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_03_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_04_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_05_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_06_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_07_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_08_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_09_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_10_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_11_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_12_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_13_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_14_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_15_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_16_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_17_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_18_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_19_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_20_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_21_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_22_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_23_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_24_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_25_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_26_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_27_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_28_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_29_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_30_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_31_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_32_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_33_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_34_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_35_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_36_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_37_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_38_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_39_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_40_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_41_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_42_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_43_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_44_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_45_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_46_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),
		CLUSTER_1_BATTERY_47_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELCIUS)),

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
	protected ModbusProtocol defineModbusProtocol(int unitId) {		
//		return Utils.createModbusProtocol(unitId);
		int statusOffset = REGISTER_BASE_ADRESS + STATUS_OFFSET;
		int alarmOffset = REGISTER_BASE_ADRESS + ALARM_OFFSET;
		int voltageOffset = REGISTER_BASE_ADRESS + VOLTAGE_OFFSET;
		int temperatureOffset = REGISTER_BASE_ADRESS + TEMPERATURE_OFFSET;

		int failureOffset = REGISTER_BASE_ADRESS + FAILURE_OFFSET;
		return 
			new ModbusProtocol(unitId, //
				new FC6WriteRegisterTask(CONTACTOR_CONTROL_ADRESS, new UnsignedWordElement(CONTACTOR_CONTROL_ADRESS)), //
				new FC3ReadRegistersTask(statusOffset , Priority.HIGH,
						m(SoltaroRack.ChannelId.CLUSTER_1_VOLTAGE, new UnsignedWordElement(statusOffset), ElementToChannelConverter.SCALE_FACTOR_2),
						m(SoltaroRack.ChannelId.CLUSTER_1_CURRENT, new UnsignedWordElement(statusOffset + 0x1), ElementToChannelConverter.SCALE_FACTOR_2),
						m(SoltaroRack.ChannelId.CLUSTER_1_CHARGE_INDICATION, new UnsignedWordElement(statusOffset + 0x2)),
						m(Bms.ChannelId.SOC, new UnsignedWordElement(statusOffset + 0x3)),
						m(SoltaroRack.ChannelId.CLUSTER_1_SOH, new UnsignedWordElement(statusOffset + 0x4)),
						m(SoltaroRack.ChannelId.CLUSTER_1_MAX_CELL_VOLTAGE_ID, new UnsignedWordElement(statusOffset + 0x5)),
						m(SoltaroRack.ChannelId.CLUSTER_1_MAX_CELL_VOLTAGE, new UnsignedWordElement(statusOffset + 0x6)),
						m(SoltaroRack.ChannelId.CLUSTER_1_MIN_CELL_VOLTAGE_ID, new UnsignedWordElement(statusOffset + 0x7)),
						m(SoltaroRack.ChannelId.CLUSTER_1_MIN_CELL_VOLTAGE, new UnsignedWordElement(statusOffset + 0x8)),
						m(SoltaroRack.ChannelId.CLUSTER_1_MAX_CELL_TEMPERATURE_ID, new UnsignedWordElement(statusOffset + 0x9)),
						m(SoltaroRack.ChannelId.CLUSTER_1_MAX_CELL_TEMPERATURE, new UnsignedWordElement(statusOffset + 0xA)),
						m(SoltaroRack.ChannelId.CLUSTER_1_MIN_CELL_TEMPERATURE_ID, new UnsignedWordElement(statusOffset + 0xB)),
						m(SoltaroRack.ChannelId.CLUSTER_1_MIN_CELL_TEMPERATURE, new UnsignedWordElement(statusOffset + 0xC)),
						new DummyRegisterElement(statusOffset + 0xD, statusOffset + 0x15),
						m(SoltaroRack.ChannelId.SYSTEM_INSULATION, new UnsignedWordElement(statusOffset + 0x16))
				),
				new FC3ReadRegistersTask(alarmOffset, Priority.LOW, //
						bm(new UnsignedWordElement(alarmOffset)) //
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
						bm(new UnsignedWordElement(alarmOffset + 0x1)) //
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
						m(SoltaroRack.ChannelId.CLUSTER_RUN_STATE, new UnsignedWordElement(alarmOffset + 0x2))
				),
				new FC3ReadRegistersTask(failureOffset, Priority.LOW, //
						bm(new UnsignedWordElement(failureOffset)) //
								.m(SoltaroRack.ChannelId.FAILURE_SAMPLING_WIRE , 0)//
								.m(SoltaroRack.ChannelId.FAILURE_CONNECTOR_WIRE , 1)//
								.m(SoltaroRack.ChannelId.FAILURE_LTC6803 , 2)//
								.m(SoltaroRack.ChannelId.FAILURE_VOLTAGE_SAMPLING , 3)//
								.m(SoltaroRack.ChannelId.FAILURE_TEMP_SAMPLING , 4)//
								.m(SoltaroRack.ChannelId.FAILURE_TEMP_SENSOR , 5)//
								.m(SoltaroRack.ChannelId.FAILURE_BALANCING_MODULE , 8)//
								.m(SoltaroRack.ChannelId.FAILURE_TEMP_SAMPLING_LINE , 9)//
								.m(SoltaroRack.ChannelId.FAILURE_INTRANET_COMMUNICATION , 10)//
								.m(SoltaroRack.ChannelId.FAILURE_EEPROM , 11)//
								.m(SoltaroRack.ChannelId.FAILURE_INITIALIZATION , 12)//
								.build() //
				),
				new FC3ReadRegistersTask(voltageOffset , Priority.LOW, //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_000_VOLTAGE, new UnsignedWordElement(voltageOffset)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_001_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x1)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_002_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x2)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_003_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x3)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_004_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x4)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_005_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x5)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_006_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x6)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_007_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x7)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_008_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x8)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_009_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x9)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_010_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xA)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_011_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xB)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_012_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xC)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_013_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xD)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_014_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xE)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_015_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xF)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_016_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x10)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_017_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x11)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_018_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x12)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_019_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x13)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_020_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x14)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_021_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x15)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_022_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x16)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_023_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x17)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_024_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x18)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_025_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x19)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_026_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x1A)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_027_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x1B)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_028_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x1C)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_029_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x1D)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_030_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x1E)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_031_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x1F)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_032_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x20)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_033_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x21)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_034_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x22)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_035_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x23)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_036_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x24)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_037_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x25)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_038_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x26)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_039_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x27)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_040_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x28)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_041_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x29)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_042_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x2A)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_043_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x2B)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_044_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x2C)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_045_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x2D)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_046_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x2E)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_047_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x2F)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_048_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x30)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_049_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x31)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_050_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x32)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_051_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x33)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_052_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x34)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_053_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x35)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_054_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x36)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_055_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x37)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_056_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x38)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_057_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x39)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_058_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x3A)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_059_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x3B)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_060_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x3C)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_061_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x3D)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_062_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x3E)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_063_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x3F)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_064_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x40)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_065_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x41)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_066_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x42)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_067_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x43)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_068_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x44)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_069_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x45)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_070_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x46)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_071_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x47)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_072_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x48)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_073_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x49)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_074_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x4A)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_075_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x4B)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_076_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x4C)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_077_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x4D)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_078_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x4E)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_079_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x4F)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_080_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x50)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_081_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x51)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_082_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x52)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_083_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x53)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_084_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x54)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_085_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x55)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_086_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x56)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_087_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x57)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_088_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x58)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_089_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x59)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_090_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x5A)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_091_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x5B)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_092_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x5C)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_093_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x5D)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_094_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x5E)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_095_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x5F)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_096_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x60)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_097_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x61)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_098_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x62)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_099_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x63)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_100_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x64)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_101_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x65)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_102_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x66)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_103_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x67)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_104_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x68)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_105_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x69)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_106_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x6A)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_107_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x6B)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_108_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x6C)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_109_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x6D)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_110_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x6E)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_111_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x6F)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_112_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x70)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_113_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x71)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_114_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x72)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_115_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x73)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_116_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x74)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_117_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x75)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_118_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x76)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_119_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x77)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_120_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x78)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_121_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x79)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_122_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x7A)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_123_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x7B)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_124_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x7C)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_125_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x7D)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_126_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x7E)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_127_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x7F)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_128_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x80)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_129_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x81)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_130_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x82)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_131_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x83)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_132_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x84)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_133_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x85)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_134_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x86)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_135_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x87)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_136_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x88)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_137_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x89)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_138_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x8A)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_139_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x8B)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_140_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x8C)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_141_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x8D)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_142_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x8E)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_143_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x8F)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_144_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x90)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_145_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x91)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_146_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x92)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_147_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x93)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_148_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x94)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_149_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x95)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_150_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x96)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_151_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x97)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_152_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x98)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_153_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x99)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_154_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x9A)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_155_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x9B)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_156_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x9C)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_157_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x9D)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_158_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x9E)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_159_VOLTAGE, new UnsignedWordElement(voltageOffset + 0x9F)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_160_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xA0)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_161_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xA1)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_162_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xA2)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_163_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xA3)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_164_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xA4)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_165_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xA5)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_166_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xA6)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_167_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xA7)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_168_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xA8)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_169_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xA9)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_170_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xAA)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_171_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xAB)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_172_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xAC)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_173_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xAD)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_174_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xAE)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_175_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xAF)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_176_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xB0)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_177_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xB1)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_178_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xB2)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_179_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xB3)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_180_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xB4)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_181_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xB5)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_182_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xB6)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_183_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xB7)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_184_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xB8)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_185_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xB9)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_186_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xBA)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_187_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xBB)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_188_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xBC)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_189_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xBD)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_190_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xBE)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_191_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xBF)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_192_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xC0)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_193_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xC1)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_194_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xC2)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_195_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xC3)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_196_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xC4)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_197_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xC5)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_198_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xC6)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_199_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xC7)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_200_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xC8)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_201_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xC9)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_202_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xCA)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_203_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xCB)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_204_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xCC)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_205_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xCD)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_206_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xCE)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_207_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xCF)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_208_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xD0)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_209_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xD1)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_210_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xD2)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_211_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xD3)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_212_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xD4)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_213_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xD5)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_214_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xD6)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_215_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xD7)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_216_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xD8)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_217_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xD9)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_218_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xDA)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_219_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xDB)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_220_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xDC)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_221_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xDD)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_222_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xDE)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_223_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xDF)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_224_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xE0)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_225_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xE1)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_226_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xE2)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_227_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xE3)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_228_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xE4)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_229_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xE5)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_230_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xE6)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_231_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xE7)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_232_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xE8)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_233_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xE9)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_234_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xEA)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_235_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xEB)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_236_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xEC)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_237_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xED)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_238_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xEE)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_239_VOLTAGE, new UnsignedWordElement(voltageOffset + 0xEF))

				),
				new FC3ReadRegistersTask(temperatureOffset  , Priority.LOW, //
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_00_TEMPERATURE, new SignedWordElement(temperatureOffset)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_01_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x1)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_02_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x2)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_03_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x3)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_04_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x4)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_05_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x5)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_06_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x6)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_07_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x7)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_08_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x8)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_09_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x9)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_10_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0xA)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_11_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0xB)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_12_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0xC)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_13_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0xD)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_14_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0xE)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_15_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0xF)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_16_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x10)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_17_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x11)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_18_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x12)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_19_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x13)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_20_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x14)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_21_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x15)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_22_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x16)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_23_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x17)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_24_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x18)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_25_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x19)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_26_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x1A)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_27_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x1B)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_28_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x1C)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_29_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x1D)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_30_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x1E)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_31_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x1F)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_32_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x20)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_33_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x21)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_34_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x22)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_35_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x23)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_36_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x24)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_37_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x25)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_38_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x26)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_39_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x27)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_40_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x28)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_41_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x29)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_42_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x2A)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_43_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x2B)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_44_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x2C)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_45_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x2D)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_46_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x2E)),
						m(SoltaroRack.ChannelId.CLUSTER_1_BATTERY_47_TEMPERATURE, new UnsignedWordElement(temperatureOffset + 0x2F))
					)
			);
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value().asString();
	}


	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
	
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			checkSystemState();
			break;
		}
	}

	private void checkSystemState() {
		Integer SYSTEM_ON = 1;
		
		IntegerReadChannel contactorControl = this.channel(ChannelId.BMS_CONTACTOR_CONTROL);
		Value<Integer> value = contactorControl.value();
		
		if(!value.asOptional().isPresent()) {
			return;
		}
			try {
	
			if(contactorControl.value().asEnum() == ContactorControl.CONNECTION_INITIATING);
			Optional<Integer> state = contactorControl.value() .asOptional();
			
			if (state.isPresent() && state   .get() == ContactorControl.CUT_OFF.value) {
				contactorControl.setNextValue(SYSTEM_ON);
				return;
			}
			if (state.isPresent() && state.get() == ContactorControl.CONNECTION_INITIATING.value) { 
				// do nothing and wait until system is in normal operating mode
				return;
			}
			if (state.isPresent() && state.get() == ContactorControl.ON_GRID.value) { 
				if (checkForFault()) {
					handleFaults();
				} else {
					doNormalProcessing();
				}
			}	
			
		} catch (InvalidValueException e) {
			log.error(e.getMessage());
		}
		
		
	}

	private void handleFaults() {
		// TODO switch off and on?!
	}

	private boolean checkForFault() {
		Optional<Integer> state = getState().getNextValue().asOptional();
		return ( state.isPresent() && state.get() != 0 );
	}

	private void doNormalProcessing() {
		System.out.println("Hello, i am working in normal mode!");
	}
}
