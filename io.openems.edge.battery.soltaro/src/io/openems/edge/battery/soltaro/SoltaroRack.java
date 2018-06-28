package io.openems.edge.battery.soltaro;

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

import io.openems.edge.battery.api.Bms;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;


/**
 * Implements the FENECONSoltaro battery system.
 */
@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Bms.Fenecon.Soltaro", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
)
public class SoltaroRack extends AbstractOpenemsModbusComponent
		implements Bms, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(AbstractOpenemsModbusComponent.class);

	private final static int UNIT_ID = 0x1;

	private String modbusBridgeId;

	@Reference
	protected ConfigurationAdmin cm;

	public SoltaroRack() {
	
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
	
	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		BMS_CONTACTOR_CONTROL(new Doc() //
				.option(ContactorControl.CUT_OFF.value, ContactorControl.CUT_OFF) //
				.option(ContactorControl.CONNECTION_INITIATING.value, ContactorControl.CONNECTION_INITIATING) //
				.option(ContactorControl.ON_GRID.value, ContactorControl.ON_GRID)
		),
		CLUSTER_1_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		CLUSTER_1_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
		CLUSTER_1_CHARGE_INDICATION(new Doc() //
				.option(ChargeIndication.STANDING.value, ChargeIndication.STANDING) //
				.option(ChargeIndication.DISCHARGING.value, ChargeIndication.DISCHARGING) //
				.option(ChargeIndication.CHARGING.value, ChargeIndication.CHARGING)
		), //
		CLUSTER_1_SOC(new Doc().unit(Unit.PERCENT)), //
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
		//TODO
		return null;
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
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			
			break;
		}
	}


}
