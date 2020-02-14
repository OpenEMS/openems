package io.openems.edge.ess.mr.gridcon.battery;

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
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Soltaro.Gridcon", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
)
public class SoltaroGridcon extends AbstractOpenemsModbusComponent
		implements Battery, OpenemsComponent, ModbusSlave, SoltaroBattery {

	// Default values for the battery ranges
	public static final int DISCHARGE_MIN_V = 696;
	public static final int CHARGE_MAX_V = 854;
	public static final int DISCHARGE_MAX_A = 0;
	public static final int CHARGE_MAX_A = 0;

	protected final static int SYSTEM_ON = 1;
	protected final static int SYSTEM_OFF = 0;

	private final Logger log = LoggerFactory.getLogger(SoltaroGridcon.class);

	private String modbusBridgeId;

	@Reference
	protected ConfigurationAdmin cm;

	public SoltaroGridcon() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				SoltaroGridcon.ChannelId.values() //
		);
		this.channel(Battery.ChannelId.CHARGE_MAX_CURRENT).setNextValue(SoltaroGridcon.CHARGE_MAX_A);
		this.channel(Battery.ChannelId.CHARGE_MAX_VOLTAGE).setNextValue(SoltaroGridcon.CHARGE_MAX_V);
		this.channel(Battery.ChannelId.DISCHARGE_MAX_CURRENT).setNextValue(SoltaroGridcon.DISCHARGE_MAX_A);
		this.channel(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE).setNextValue(SoltaroGridcon.DISCHARGE_MIN_V);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
		this.modbusBridgeId = config.modbus_id();
		this.getCapacity().setNextValue(config.capacity());
		initializeCallbacks();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void initializeCallbacks() {
		this.channel(ChannelId.BMS_CONTACTOR_CONTROL).onChange((oldValue, newValue) -> {
			ContactorControl cc = newValue.asEnum();

			switch (cc) {
			case CONNECTION_INITIATING:
				this.channel(Battery.ChannelId.READY_FOR_WORKING).setNextValue(false);
				break;
			case CUT_OFF:
				this.channel(Battery.ChannelId.READY_FOR_WORKING).setNextValue(false);
				break;
			case ON_GRID:
				this.channel(Battery.ChannelId.READY_FOR_WORKING).setNextValue(true);
				break;
			default:
				break;
			}
		});
	}


	private boolean isAlarmLevel2Error() {
		return (readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_HIGH)
				|| readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH)
				|| readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_CHA_CURRENT_HIGH)
				|| readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_LOW)
				|| readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW)
				|| readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_DISCHA_CURRENT_HIGH)
				|| readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH)
				|| readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_LOW)
				|| readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_INSULATION_LOW)
				|| readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH)
				|| readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW));
	}

	private boolean isSystemRunning() {
		EnumReadChannel contactorControlChannel = this.channel(ChannelId.BMS_CONTACTOR_CONTROL);
		ContactorControl cc = contactorControlChannel.value().asEnum();
		return cc == ContactorControl.ON_GRID;
	}

	private boolean isSystemStopped() {
		EnumReadChannel contactorControlChannel = this.channel(ChannelId.BMS_CONTACTOR_CONTROL);
		ContactorControl cc = contactorControlChannel.value().asEnum();
		return cc == ContactorControl.CUT_OFF;
	}


	private boolean readValueFromBooleanChannel(ChannelId channelId) {
		StateChannel r = this.channel(channelId);
		Optional<Boolean> bOpt = r.value().asOptional();
		return bOpt.isPresent() && bOpt.get();
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
		
		EnumWriteChannel contactorControlChannel = this.channel(ChannelId.BMS_CONTACTOR_CONTROL);

		Optional<Integer> contactorControlOpt = contactorControlChannel.value().asOptional();
		// To avoid hardware damages do not send start command if system has already
		// started
		if (contactorControlOpt.isPresent() && contactorControlOpt.get() == ContactorControl.ON_GRID.getValue()) {
			return;
		}

		try {
			contactorControlChannel.setNextWriteValue(SYSTEM_ON);
		} catch (OpenemsNamedException e) {
			log.error("Error while trying to start system\n" + e.getMessage());
		}
	}

	private void stopSystem() {
		EnumWriteChannel contactorControlChannel = this.channel(ChannelId.BMS_CONTACTOR_CONTROL);

		Optional<Integer> contactorControlOpt = contactorControlChannel.value().asOptional();
		// To avoid hardware damages do not send stop command if system has already
		// stopped
		if (contactorControlOpt.isPresent() && contactorControlOpt.get() == ContactorControl.CUT_OFF.getValue()) {
			return;
		}

		try {
			contactorControlChannel.setNextWriteValue(SYSTEM_OFF);
		} catch (OpenemsNamedException e) {
			log.error("Error while trying to stop system\n" + e.getMessage());
		}
	}
	
	@Override
	public void start() {
		startSystem();
	}

	public void stop() {
		stopSystem();		
	}

	@Override
	public boolean isRunning() {
		return isSystemRunning();
	}

	@Override
	public boolean isStopped() {
		return isSystemStopped();
	}

	@Override
	public boolean isError() {
		return isAlarmLevel2Error();
	}
	
	@Override
	public float getSoCX() {
		return this.getSoc().value().orElse(0);
	}

	@Override
	public float getMinimalCellVoltage() {
		return ((float) getMinCellVoltage().value().orElse(0)) / 1000;
	}

	@Override
	public float getMaximalCellVoltage() {
		return ((float) getMaxCellVoltage().value().orElse(0)) / 1000;
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// IntegerWriteChannels
		CELL_VOLTAGE_PROTECT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		CELL_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE)),

		// EnumReadChannels
		CLUSTER_RUN_STATE(Doc.of(ClusterRunState.values())), //
		CLUSTER_1_CHARGE_INDICATION(Doc.of(ChargeIndication.values())), //

		// EnumWriteChannels
		BMS_CONTACTOR_CONTROL(Doc.of(ContactorControl.values()) //
				.accessMode(AccessMode.READ_WRITE)), //

		// IntegerReadChannels
		SYSTEM_OVER_VOLTAGE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		SYSTEM_UNDER_VOLTAGE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_MAX_CELL_VOLTAGE_ID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		CLUSTER_1_MIN_CELL_VOLTAGE_ID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		CLUSTER_1_MAX_CELL_TEMPERATURE_ID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		CLUSTER_1_MIN_CELL_TEMPERATURE_ID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		SYSTEM_INSULATION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOOHM)), //
		SYSTEM_ACCEPT_MAX_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		SYSTEM_ACCEPT_MAX_DISCHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		CLUSTER_1_BATTERY_000_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_001_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_002_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_003_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_004_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_005_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_006_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_007_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_008_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_009_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_010_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_011_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_012_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_013_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_014_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_015_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_016_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_017_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_018_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_019_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_020_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_021_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_022_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_023_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_024_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_025_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_026_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_027_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_028_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_029_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_030_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_031_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_032_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_033_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_034_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_035_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_036_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_037_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_038_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_039_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_040_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_041_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_042_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_043_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_044_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_045_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_046_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_047_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_048_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_049_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_050_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_051_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_052_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_053_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_054_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_055_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_056_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_057_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_058_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_059_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_060_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_061_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_062_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_063_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_064_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_065_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_066_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_067_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_068_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_069_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_070_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_071_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_072_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_073_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_074_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_075_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_076_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_077_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_078_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_079_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_080_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_081_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_082_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_083_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_084_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_085_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_086_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_087_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_088_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_089_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_090_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_091_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_092_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_093_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_094_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_095_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_096_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_097_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_098_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_099_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_100_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_101_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_102_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_103_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_104_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_105_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_106_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_107_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_108_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_109_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_110_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_111_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_112_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_113_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_114_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_115_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_116_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_117_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_118_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_119_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_120_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_121_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_122_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_123_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_124_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_125_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_126_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_127_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_128_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_129_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_130_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_131_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_132_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_133_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_134_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_135_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_136_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_137_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_138_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_139_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_140_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_141_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_142_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_143_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_144_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_145_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_146_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_147_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_148_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_149_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_150_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_151_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_152_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_153_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_154_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_155_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_156_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_157_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_158_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_159_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_160_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_161_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_162_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_163_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_164_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_165_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_166_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_167_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_168_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_169_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_170_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_171_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_172_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_173_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_174_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_175_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_176_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_177_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_178_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_179_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_180_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_181_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_182_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_183_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_184_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_185_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_186_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_187_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_188_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_189_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_190_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_191_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_192_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_193_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_194_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_195_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_196_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_197_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_198_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_199_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_200_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_201_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_202_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_203_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_204_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_205_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_206_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_207_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_208_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_209_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_210_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_211_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_212_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_213_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_214_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_215_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_216_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_217_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_218_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_219_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_220_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_221_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_222_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_223_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_224_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_225_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_226_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_227_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_228_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_229_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_230_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_231_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_232_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_233_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_234_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_235_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_236_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_237_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_238_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_239_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //

		CLUSTER_1_BATTERY_00_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_01_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_02_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_03_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_04_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_05_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_06_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_07_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_08_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_09_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_10_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_11_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_12_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_13_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_14_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_15_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_16_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_17_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_18_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_19_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_20_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_21_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_22_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_23_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_24_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_25_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_26_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_27_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_28_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_29_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_30_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_31_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_32_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_33_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_34_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_35_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_36_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_37_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_38_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_39_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_40_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_41_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_42_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_43_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_44_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_45_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_46_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_47_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //

		// StateChannels
		ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Discharge Temperature Low Alarm Level 2")), //
		ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Discharge Temperature High Alarm Level 2")), //
		ALARM_LEVEL_2_INSULATION_LOW(Doc.of(Level.WARNING) //
				.text("Cluster1Insulation Low Alarm Level 2")), //
		ALARM_LEVEL_2_CELL_CHA_TEMP_LOW(Doc.of(Level.WARNING) //
				.text("Cluster1 Cell Charge Temperature Low Alarm Level 2")), //
		ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster1 Cell Charge Temperature High Alarm Level 2")), //
		ALARM_LEVEL_2_DISCHA_CURRENT_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Discharge Current High Alarm Level 2")), //
		ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
				.text("Cluster 1 Total Voltage Low Alarm Level 2")), //
		ALARM_LEVEL_2_CELL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Voltage Low Alarm Level 2")), //
		ALARM_LEVEL_2_CHA_CURRENT_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Charge Current High Alarm Level 2")), //
		ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Total Voltage High Alarm Level 2")), //
		ALARM_LEVEL_2_CELL_VOLTAGE_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Voltage High Alarm Level 2")), //
		ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Discharge Temperature Low Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Discharge Temperature High Alarm Level 1")), //
		ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster1 Total Voltage Diff High Alarm Level 1")), //
		ALARM_LEVEL_1_INSULATION_LOW(Doc.of(Level.WARNING) //
				.text("Cluster1 Insulation Low Alarm Level1")), //
		ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Voltage Diff High Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster X Cell temperature Diff High Alarm Level 1")), //
		ALARM_LEVEL_1_SOC_LOW(Doc.of(Level.WARNING) //
				.text("Cluster 1 SOC Low Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_CHA_TEMP_LOW(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Charge Temperature Low Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Charge Temperature High Alarm Level 1")), //
		ALARM_LEVEL_1_DISCHA_CURRENT_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Discharge Current High Alarm Level 1")), //
		ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
				.text("Cluster 1 Total Voltage Low Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Voltage Low Alarm Level 1")), //
		ALARM_LEVEL_1_CHA_CURRENT_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Charge Current High Alarm Level 1")), //
		ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Total Voltage High Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_VOLTAGE_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Voltage High Alarm Level 1")), //
		FAILURE_INITIALIZATION(Doc.of(Level.FAULT) //
				.text("Initialization failure")), //
		FAILURE_EEPROM(Doc.of(Level.FAULT) //
				.text("EEPROM fault")), //
		FAILURE_INTRANET_COMMUNICATION(Doc.of(Level.FAULT) //
				.text("Intranet communication fault")), //
		FAILURE_TEMP_SAMPLING_LINE(Doc.of(Level.FAULT) //
				.text("Temperature sampling line fault")), //
		FAILURE_BALANCING_MODULE(Doc.of(Level.FAULT) //
				.text("Balancing module fault")), //
		FAILURE_TEMP_SENSOR(Doc.of(Level.FAULT) //
				.text("Temperature sensor fault")), //
		FAILURE_TEMP_SAMPLING(Doc.of(Level.FAULT) //
				.text("Temperature sampling fault")), //
		FAILURE_VOLTAGE_SAMPLING(Doc.of(Level.FAULT) //
				.text("Voltage sampling fault")), //
		FAILURE_LTC6803(Doc.of(Level.FAULT) //
				.text("LTC6803 fault")), //
		FAILURE_CONNECTOR_WIRE(Doc.of(Level.FAULT) //
				.text("connector wire fault")), //
		FAILURE_SAMPLING_WIRE(Doc.of(Level.FAULT) //
				.text("sampling wire fault")), //
		PRECHARGE_TAKING_TOO_LONG(Doc.of(Level.FAULT) //
				.text("precharge time was too long")), //
		
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
						m(SoltaroGridcon.ChannelId.BMS_CONTACTOR_CONTROL, new UnsignedWordElement(0x2010)) //
				), //
				new FC3ReadRegistersTask(0x2010, Priority.HIGH, //
						m(SoltaroGridcon.ChannelId.BMS_CONTACTOR_CONTROL, new UnsignedWordElement(0x2010)) //
				), //
				new FC3ReadRegistersTask(0x2042, Priority.HIGH, //
						m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, new UnsignedWordElement(0x2042), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
				), //
				new FC3ReadRegistersTask(0x2046, Priority.HIGH, //
						m(SoltaroGridcon.ChannelId.CELL_VOLTAGE_PROTECT, new UnsignedWordElement(0x2046)), //
						m(SoltaroGridcon.ChannelId.CELL_VOLTAGE_RECOVER, new UnsignedWordElement(0x2047)), //
						m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(0x2048), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
				), //
				new FC6WriteRegisterTask(0x2046, //
						m(SoltaroGridcon.ChannelId.CELL_VOLTAGE_PROTECT, new UnsignedWordElement(0x2046)) //
				), //
				new FC6WriteRegisterTask(0x2047, //
						m(SoltaroGridcon.ChannelId.CELL_VOLTAGE_RECOVER, new UnsignedWordElement(0x2047)) //
				), //
				new FC3ReadRegistersTask(0x2100, Priority.LOW, //
						m(Battery.ChannelId.VOLTAGE, new UnsignedWordElement(0x2100), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(Battery.ChannelId.CURRENT, new SignedWordElement(0x2101), //
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_CHARGE_INDICATION, new UnsignedWordElement(0x2102)), //
						m(Battery.ChannelId.SOC, new UnsignedWordElement(0x2103)), //
						m(Battery.ChannelId.SOH, new UnsignedWordElement(0x2104)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_MAX_CELL_VOLTAGE_ID, new UnsignedWordElement(0x2105)), //
						m(Battery.ChannelId.MAX_CELL_VOLTAGE, new UnsignedWordElement(0x2106)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_MIN_CELL_VOLTAGE_ID, new UnsignedWordElement(0x2107)), //
						m(Battery.ChannelId.MIN_CELL_VOLTAGE, new UnsignedWordElement(0x2108)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_MAX_CELL_TEMPERATURE_ID, new UnsignedWordElement(0x2109)), //
						m(Battery.ChannelId.MAX_CELL_TEMPERATURE, new UnsignedWordElement(0x210A),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_MIN_CELL_TEMPERATURE_ID, new UnsignedWordElement(0x210B)), //
						m(Battery.ChannelId.MIN_CELL_TEMPERATURE, new UnsignedWordElement(0x210C),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(0x210D, 0x2115), //
						m(SoltaroGridcon.ChannelId.SYSTEM_INSULATION, new UnsignedWordElement(0x2116)) //
				), //
				new FC3ReadRegistersTask(0x2160, Priority.HIGH, //
						m(Battery.ChannelId.CHARGE_MAX_CURRENT, new UnsignedWordElement(0x2160), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(Battery.ChannelId.DISCHARGE_MAX_CURRENT, new UnsignedWordElement(0x2161), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
				), //
				new FC3ReadRegistersTask(0x2140, Priority.LOW, //
						m(new BitsWordElement(0x2140, this) //
								.bit(0, SoltaroGridcon.ChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_HIGH) //
								.bit(1, SoltaroGridcon.ChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH) //
								.bit(2, SoltaroGridcon.ChannelId.ALARM_LEVEL_2_CHA_CURRENT_HIGH) //
								.bit(3, SoltaroGridcon.ChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_LOW) //
								.bit(4, SoltaroGridcon.ChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW) //
								.bit(5, SoltaroGridcon.ChannelId.ALARM_LEVEL_2_DISCHA_CURRENT_HIGH) //
								.bit(6, SoltaroGridcon.ChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH) //
								.bit(7, SoltaroGridcon.ChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_LOW) //
								.bit(12, SoltaroGridcon.ChannelId.ALARM_LEVEL_2_INSULATION_LOW) //
								.bit(14, SoltaroGridcon.ChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH) //
								.bit(15, SoltaroGridcon.ChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW) //
						), //
						m(new BitsWordElement(0x2141, this) //
								.bit(0, SoltaroGridcon.ChannelId.ALARM_LEVEL_1_CELL_VOLTAGE_HIGH) //
								.bit(1, SoltaroGridcon.ChannelId.ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH) //
								.bit(2, SoltaroGridcon.ChannelId.ALARM_LEVEL_1_CHA_CURRENT_HIGH) //
								.bit(3, SoltaroGridcon.ChannelId.ALARM_LEVEL_1_CELL_VOLTAGE_LOW) //
								.bit(4, SoltaroGridcon.ChannelId.ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW) //
								.bit(5, SoltaroGridcon.ChannelId.ALARM_LEVEL_1_DISCHA_CURRENT_HIGH) //
								.bit(6, SoltaroGridcon.ChannelId.ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH) //
								.bit(7, SoltaroGridcon.ChannelId.ALARM_LEVEL_1_CELL_CHA_TEMP_LOW) //
								.bit(8, SoltaroGridcon.ChannelId.ALARM_LEVEL_1_SOC_LOW) //
								.bit(9, SoltaroGridcon.ChannelId.ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH) //
								.bit(11, SoltaroGridcon.ChannelId.ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH) //
								.bit(12, SoltaroGridcon.ChannelId.ALARM_LEVEL_1_INSULATION_LOW) //
								.bit(13, SoltaroGridcon.ChannelId.ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH) //
								.bit(14, SoltaroGridcon.ChannelId.ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH) //
								.bit(15, SoltaroGridcon.ChannelId.ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW) //
						), //
						m(SoltaroGridcon.ChannelId.CLUSTER_RUN_STATE, new UnsignedWordElement(0x2142)) //
				), //
				new FC3ReadRegistersTask(0x2185, Priority.LOW, //
						m(new BitsWordElement(0x2185, this) //
								.bit(0, SoltaroGridcon.ChannelId.FAILURE_SAMPLING_WIRE)//
								.bit(1, SoltaroGridcon.ChannelId.FAILURE_CONNECTOR_WIRE)//
								.bit(2, SoltaroGridcon.ChannelId.FAILURE_LTC6803)//
								.bit(3, SoltaroGridcon.ChannelId.FAILURE_VOLTAGE_SAMPLING)//
								.bit(4, SoltaroGridcon.ChannelId.FAILURE_TEMP_SAMPLING)//
								.bit(5, SoltaroGridcon.ChannelId.FAILURE_TEMP_SENSOR)//
								.bit(8, SoltaroGridcon.ChannelId.FAILURE_BALANCING_MODULE)//
								.bit(9, SoltaroGridcon.ChannelId.FAILURE_TEMP_SAMPLING_LINE)//
								.bit(10, SoltaroGridcon.ChannelId.FAILURE_INTRANET_COMMUNICATION)//
								.bit(11, SoltaroGridcon.ChannelId.FAILURE_EEPROM)//
								.bit(12, SoltaroGridcon.ChannelId.FAILURE_INITIALIZATION)//
						) //
				), //
				new FC3ReadRegistersTask(0x2800, Priority.LOW, //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_000_VOLTAGE, new UnsignedWordElement(0x2800)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_001_VOLTAGE, new UnsignedWordElement(0x2801)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_002_VOLTAGE, new UnsignedWordElement(0x2802)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_003_VOLTAGE, new UnsignedWordElement(0x2803)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_004_VOLTAGE, new UnsignedWordElement(0x2804)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_005_VOLTAGE, new UnsignedWordElement(0x2805)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_006_VOLTAGE, new UnsignedWordElement(0x2806)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_007_VOLTAGE, new UnsignedWordElement(0x2807)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_008_VOLTAGE, new UnsignedWordElement(0x2808)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_009_VOLTAGE, new UnsignedWordElement(0x2809)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_010_VOLTAGE, new UnsignedWordElement(0x280A)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_011_VOLTAGE, new UnsignedWordElement(0x280B)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_012_VOLTAGE, new UnsignedWordElement(0x280C)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_013_VOLTAGE, new UnsignedWordElement(0x280D)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_014_VOLTAGE, new UnsignedWordElement(0x280E)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_015_VOLTAGE, new UnsignedWordElement(0x280F)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_016_VOLTAGE, new UnsignedWordElement(0x2810)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_017_VOLTAGE, new UnsignedWordElement(0x2811)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_018_VOLTAGE, new UnsignedWordElement(0x2812)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_019_VOLTAGE, new UnsignedWordElement(0x2813)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_020_VOLTAGE, new UnsignedWordElement(0x2814)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_021_VOLTAGE, new UnsignedWordElement(0x2815)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_022_VOLTAGE, new UnsignedWordElement(0x2816)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_023_VOLTAGE, new UnsignedWordElement(0x2817)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_024_VOLTAGE, new UnsignedWordElement(0x2818)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_025_VOLTAGE, new UnsignedWordElement(0x2819)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_026_VOLTAGE, new UnsignedWordElement(0x281A)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_027_VOLTAGE, new UnsignedWordElement(0x281B)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_028_VOLTAGE, new UnsignedWordElement(0x281C)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_029_VOLTAGE, new UnsignedWordElement(0x281D)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_030_VOLTAGE, new UnsignedWordElement(0x281E)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_031_VOLTAGE, new UnsignedWordElement(0x281F)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_032_VOLTAGE, new UnsignedWordElement(0x2820)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_033_VOLTAGE, new UnsignedWordElement(0x2821)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_034_VOLTAGE, new UnsignedWordElement(0x2822)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_035_VOLTAGE, new UnsignedWordElement(0x2823)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_036_VOLTAGE, new UnsignedWordElement(0x2824)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_037_VOLTAGE, new UnsignedWordElement(0x2825)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_038_VOLTAGE, new UnsignedWordElement(0x2826)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_039_VOLTAGE, new UnsignedWordElement(0x2827)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_040_VOLTAGE, new UnsignedWordElement(0x2828)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_041_VOLTAGE, new UnsignedWordElement(0x2829)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_042_VOLTAGE, new UnsignedWordElement(0x282A)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_043_VOLTAGE, new UnsignedWordElement(0x282B)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_044_VOLTAGE, new UnsignedWordElement(0x282C)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_045_VOLTAGE, new UnsignedWordElement(0x282D)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_046_VOLTAGE, new UnsignedWordElement(0x282E)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_047_VOLTAGE, new UnsignedWordElement(0x282F)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_048_VOLTAGE, new UnsignedWordElement(0x2830)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_049_VOLTAGE, new UnsignedWordElement(0x2831)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_050_VOLTAGE, new UnsignedWordElement(0x2832)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_051_VOLTAGE, new UnsignedWordElement(0x2833)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_052_VOLTAGE, new UnsignedWordElement(0x2834)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_053_VOLTAGE, new UnsignedWordElement(0x2835)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_054_VOLTAGE, new UnsignedWordElement(0x2836)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_055_VOLTAGE, new UnsignedWordElement(0x2837)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_056_VOLTAGE, new UnsignedWordElement(0x2838)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_057_VOLTAGE, new UnsignedWordElement(0x2839)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_058_VOLTAGE, new UnsignedWordElement(0x283A)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_059_VOLTAGE, new UnsignedWordElement(0x283B)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_060_VOLTAGE, new UnsignedWordElement(0x283C)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_061_VOLTAGE, new UnsignedWordElement(0x283D)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_062_VOLTAGE, new UnsignedWordElement(0x283E)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_063_VOLTAGE, new UnsignedWordElement(0x283F)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_064_VOLTAGE, new UnsignedWordElement(0x2840)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_065_VOLTAGE, new UnsignedWordElement(0x2841)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_066_VOLTAGE, new UnsignedWordElement(0x2842)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_067_VOLTAGE, new UnsignedWordElement(0x2843)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_068_VOLTAGE, new UnsignedWordElement(0x2844)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_069_VOLTAGE, new UnsignedWordElement(0x2845)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_070_VOLTAGE, new UnsignedWordElement(0x2846)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_071_VOLTAGE, new UnsignedWordElement(0x2847)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_072_VOLTAGE, new UnsignedWordElement(0x2848)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_073_VOLTAGE, new UnsignedWordElement(0x2849)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_074_VOLTAGE, new UnsignedWordElement(0x284A)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_075_VOLTAGE, new UnsignedWordElement(0x284B)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_076_VOLTAGE, new UnsignedWordElement(0x284C)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_077_VOLTAGE, new UnsignedWordElement(0x284D)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_078_VOLTAGE, new UnsignedWordElement(0x284E)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_079_VOLTAGE, new UnsignedWordElement(0x284F)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_080_VOLTAGE, new UnsignedWordElement(0x2850)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_081_VOLTAGE, new UnsignedWordElement(0x2851)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_082_VOLTAGE, new UnsignedWordElement(0x2852)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_083_VOLTAGE, new UnsignedWordElement(0x2853)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_084_VOLTAGE, new UnsignedWordElement(0x2854)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_085_VOLTAGE, new UnsignedWordElement(0x2855)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_086_VOLTAGE, new UnsignedWordElement(0x2856)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_087_VOLTAGE, new UnsignedWordElement(0x2857)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_088_VOLTAGE, new UnsignedWordElement(0x2858)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_089_VOLTAGE, new UnsignedWordElement(0x2859)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_090_VOLTAGE, new UnsignedWordElement(0x285A)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_091_VOLTAGE, new UnsignedWordElement(0x285B)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_092_VOLTAGE, new UnsignedWordElement(0x285C)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_093_VOLTAGE, new UnsignedWordElement(0x285D)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_094_VOLTAGE, new UnsignedWordElement(0x285E)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_095_VOLTAGE, new UnsignedWordElement(0x285F)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_096_VOLTAGE, new UnsignedWordElement(0x2860)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_097_VOLTAGE, new UnsignedWordElement(0x2861)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_098_VOLTAGE, new UnsignedWordElement(0x2862)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_099_VOLTAGE, new UnsignedWordElement(0x2863)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_100_VOLTAGE, new UnsignedWordElement(0x2864)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_101_VOLTAGE, new UnsignedWordElement(0x2865)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_102_VOLTAGE, new UnsignedWordElement(0x2866)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_103_VOLTAGE, new UnsignedWordElement(0x2867)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_104_VOLTAGE, new UnsignedWordElement(0x2868)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_105_VOLTAGE, new UnsignedWordElement(0x2869)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_106_VOLTAGE, new UnsignedWordElement(0x286A)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_107_VOLTAGE, new UnsignedWordElement(0x286B)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_108_VOLTAGE, new UnsignedWordElement(0x286C)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_109_VOLTAGE, new UnsignedWordElement(0x286D)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_110_VOLTAGE, new UnsignedWordElement(0x286E)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_111_VOLTAGE, new UnsignedWordElement(0x286F)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_112_VOLTAGE, new UnsignedWordElement(0x2870)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_113_VOLTAGE, new UnsignedWordElement(0x2871)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_114_VOLTAGE, new UnsignedWordElement(0x2872)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_115_VOLTAGE, new UnsignedWordElement(0x2873)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_116_VOLTAGE, new UnsignedWordElement(0x2874)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_117_VOLTAGE, new UnsignedWordElement(0x2875)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_118_VOLTAGE, new UnsignedWordElement(0x2876)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_119_VOLTAGE, new UnsignedWordElement(0x2877)) //

				), //
				new FC3ReadRegistersTask(0x2878, Priority.LOW, //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_120_VOLTAGE, new UnsignedWordElement(0x2878)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_121_VOLTAGE, new UnsignedWordElement(0x2879)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_122_VOLTAGE, new UnsignedWordElement(0x287A)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_123_VOLTAGE, new UnsignedWordElement(0x287B)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_124_VOLTAGE, new UnsignedWordElement(0x287C)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_125_VOLTAGE, new UnsignedWordElement(0x287D)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_126_VOLTAGE, new UnsignedWordElement(0x287E)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_127_VOLTAGE, new UnsignedWordElement(0x287F)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_128_VOLTAGE, new UnsignedWordElement(0x2880)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_129_VOLTAGE, new UnsignedWordElement(0x2881)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_130_VOLTAGE, new UnsignedWordElement(0x2882)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_131_VOLTAGE, new UnsignedWordElement(0x2883)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_132_VOLTAGE, new UnsignedWordElement(0x2884)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_133_VOLTAGE, new UnsignedWordElement(0x2885)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_134_VOLTAGE, new UnsignedWordElement(0x2886)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_135_VOLTAGE, new UnsignedWordElement(0x2887)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_136_VOLTAGE, new UnsignedWordElement(0x2888)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_137_VOLTAGE, new UnsignedWordElement(0x2889)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_138_VOLTAGE, new UnsignedWordElement(0x288A)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_139_VOLTAGE, new UnsignedWordElement(0x288B)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_140_VOLTAGE, new UnsignedWordElement(0x288C)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_141_VOLTAGE, new UnsignedWordElement(0x288D)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_142_VOLTAGE, new UnsignedWordElement(0x288E)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_143_VOLTAGE, new UnsignedWordElement(0x288F)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_144_VOLTAGE, new UnsignedWordElement(0x2890)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_145_VOLTAGE, new UnsignedWordElement(0x2891)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_146_VOLTAGE, new UnsignedWordElement(0x2892)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_147_VOLTAGE, new UnsignedWordElement(0x2893)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_148_VOLTAGE, new UnsignedWordElement(0x2894)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_149_VOLTAGE, new UnsignedWordElement(0x2895)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_150_VOLTAGE, new UnsignedWordElement(0x2896)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_151_VOLTAGE, new UnsignedWordElement(0x2897)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_152_VOLTAGE, new UnsignedWordElement(0x2898)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_153_VOLTAGE, new UnsignedWordElement(0x2899)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_154_VOLTAGE, new UnsignedWordElement(0x289A)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_155_VOLTAGE, new UnsignedWordElement(0x289B)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_156_VOLTAGE, new UnsignedWordElement(0x289C)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_157_VOLTAGE, new UnsignedWordElement(0x289D)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_158_VOLTAGE, new UnsignedWordElement(0x289E)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_159_VOLTAGE, new UnsignedWordElement(0x289F)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_160_VOLTAGE, new UnsignedWordElement(0x28A0)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_161_VOLTAGE, new UnsignedWordElement(0x28A1)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_162_VOLTAGE, new UnsignedWordElement(0x28A2)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_163_VOLTAGE, new UnsignedWordElement(0x28A3)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_164_VOLTAGE, new UnsignedWordElement(0x28A4)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_165_VOLTAGE, new UnsignedWordElement(0x28A5)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_166_VOLTAGE, new UnsignedWordElement(0x28A6)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_167_VOLTAGE, new UnsignedWordElement(0x28A7)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_168_VOLTAGE, new UnsignedWordElement(0x28A8)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_169_VOLTAGE, new UnsignedWordElement(0x28A9)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_170_VOLTAGE, new UnsignedWordElement(0x28AA)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_171_VOLTAGE, new UnsignedWordElement(0x28AB)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_172_VOLTAGE, new UnsignedWordElement(0x28AC)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_173_VOLTAGE, new UnsignedWordElement(0x28AD)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_174_VOLTAGE, new UnsignedWordElement(0x28AE)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_175_VOLTAGE, new UnsignedWordElement(0x28AF)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_176_VOLTAGE, new UnsignedWordElement(0x28B0)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_177_VOLTAGE, new UnsignedWordElement(0x28B1)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_178_VOLTAGE, new UnsignedWordElement(0x28B2)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_179_VOLTAGE, new UnsignedWordElement(0x28B3)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_180_VOLTAGE, new UnsignedWordElement(0x28B4)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_181_VOLTAGE, new UnsignedWordElement(0x28B5)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_182_VOLTAGE, new UnsignedWordElement(0x28B6)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_183_VOLTAGE, new UnsignedWordElement(0x28B7)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_184_VOLTAGE, new UnsignedWordElement(0x28B8)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_185_VOLTAGE, new UnsignedWordElement(0x28B9)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_186_VOLTAGE, new UnsignedWordElement(0x28BA)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_187_VOLTAGE, new UnsignedWordElement(0x28BB)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_188_VOLTAGE, new UnsignedWordElement(0x28BC)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_189_VOLTAGE, new UnsignedWordElement(0x28BD)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_190_VOLTAGE, new UnsignedWordElement(0x28BE)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_191_VOLTAGE, new UnsignedWordElement(0x28BF)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_192_VOLTAGE, new UnsignedWordElement(0x28C0)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_193_VOLTAGE, new UnsignedWordElement(0x28C1)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_194_VOLTAGE, new UnsignedWordElement(0x28C2)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_195_VOLTAGE, new UnsignedWordElement(0x28C3)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_196_VOLTAGE, new UnsignedWordElement(0x28C4)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_197_VOLTAGE, new UnsignedWordElement(0x28C5)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_198_VOLTAGE, new UnsignedWordElement(0x28C6)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_199_VOLTAGE, new UnsignedWordElement(0x28C7)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_200_VOLTAGE, new UnsignedWordElement(0x28C8)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_201_VOLTAGE, new UnsignedWordElement(0x28C9)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_202_VOLTAGE, new UnsignedWordElement(0x28CA)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_203_VOLTAGE, new UnsignedWordElement(0x28CB)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_204_VOLTAGE, new UnsignedWordElement(0x28CC)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_205_VOLTAGE, new UnsignedWordElement(0x28CD)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_206_VOLTAGE, new UnsignedWordElement(0x28CE)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_207_VOLTAGE, new UnsignedWordElement(0x28CF)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_208_VOLTAGE, new UnsignedWordElement(0x28D0)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_209_VOLTAGE, new UnsignedWordElement(0x28D1)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_210_VOLTAGE, new UnsignedWordElement(0x28D2)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_211_VOLTAGE, new UnsignedWordElement(0x28D3)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_212_VOLTAGE, new UnsignedWordElement(0x28D4)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_213_VOLTAGE, new UnsignedWordElement(0x28D5)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_214_VOLTAGE, new UnsignedWordElement(0x28D6)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_215_VOLTAGE, new UnsignedWordElement(0x28D7)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_216_VOLTAGE, new UnsignedWordElement(0x28D8)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_217_VOLTAGE, new UnsignedWordElement(0x28D9)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_218_VOLTAGE, new UnsignedWordElement(0x28DA)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_219_VOLTAGE, new UnsignedWordElement(0x28DB)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_220_VOLTAGE, new UnsignedWordElement(0x28DC)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_221_VOLTAGE, new UnsignedWordElement(0x28DD)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_222_VOLTAGE, new UnsignedWordElement(0x28DE)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_223_VOLTAGE, new UnsignedWordElement(0x28DF)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_224_VOLTAGE, new UnsignedWordElement(0x28E0)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_225_VOLTAGE, new UnsignedWordElement(0x28E1)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_226_VOLTAGE, new UnsignedWordElement(0x28E2)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_227_VOLTAGE, new UnsignedWordElement(0x28E3)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_228_VOLTAGE, new UnsignedWordElement(0x28E4)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_229_VOLTAGE, new UnsignedWordElement(0x28E5)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_230_VOLTAGE, new UnsignedWordElement(0x28E6)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_231_VOLTAGE, new UnsignedWordElement(0x28E7)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_232_VOLTAGE, new UnsignedWordElement(0x28E8)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_233_VOLTAGE, new UnsignedWordElement(0x28E9)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_234_VOLTAGE, new UnsignedWordElement(0x28EA)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_235_VOLTAGE, new UnsignedWordElement(0x28EB)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_236_VOLTAGE, new UnsignedWordElement(0x28EC)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_237_VOLTAGE, new UnsignedWordElement(0x28ED)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_238_VOLTAGE, new UnsignedWordElement(0x28EE)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_239_VOLTAGE, new UnsignedWordElement(0x28EF)) //

				), //
				new FC3ReadRegistersTask(0x2C00, Priority.LOW, //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_00_TEMPERATURE, new UnsignedWordElement(0x2C00)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_01_TEMPERATURE, new UnsignedWordElement(0x2C01)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_02_TEMPERATURE, new UnsignedWordElement(0x2C02)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_03_TEMPERATURE, new UnsignedWordElement(0x2C03)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_04_TEMPERATURE, new UnsignedWordElement(0x2C04)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_05_TEMPERATURE, new UnsignedWordElement(0x2C05)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_06_TEMPERATURE, new UnsignedWordElement(0x2C06)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_07_TEMPERATURE, new UnsignedWordElement(0x2C07)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_08_TEMPERATURE, new UnsignedWordElement(0x2C08)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_09_TEMPERATURE, new UnsignedWordElement(0x2C09)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_10_TEMPERATURE, new UnsignedWordElement(0x2C0A)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_11_TEMPERATURE, new UnsignedWordElement(0x2C0B)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_12_TEMPERATURE, new UnsignedWordElement(0x2C0C)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_13_TEMPERATURE, new UnsignedWordElement(0x2C0D)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_14_TEMPERATURE, new UnsignedWordElement(0x2C0E)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_15_TEMPERATURE, new UnsignedWordElement(0x2C0F)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_16_TEMPERATURE, new UnsignedWordElement(0x2C10)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_17_TEMPERATURE, new UnsignedWordElement(0x2C11)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_18_TEMPERATURE, new UnsignedWordElement(0x2C12)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_19_TEMPERATURE, new UnsignedWordElement(0x2C13)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_20_TEMPERATURE, new UnsignedWordElement(0x2C14)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_21_TEMPERATURE, new UnsignedWordElement(0x2C15)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_22_TEMPERATURE, new UnsignedWordElement(0x2C16)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_23_TEMPERATURE, new UnsignedWordElement(0x2C17)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_24_TEMPERATURE, new UnsignedWordElement(0x2C18)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_25_TEMPERATURE, new UnsignedWordElement(0x2C19)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_26_TEMPERATURE, new UnsignedWordElement(0x2C1A)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_27_TEMPERATURE, new UnsignedWordElement(0x2C1B)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_28_TEMPERATURE, new UnsignedWordElement(0x2C1C)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_29_TEMPERATURE, new UnsignedWordElement(0x2C1D)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_30_TEMPERATURE, new UnsignedWordElement(0x2C1E)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_31_TEMPERATURE, new UnsignedWordElement(0x2C1F)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_32_TEMPERATURE, new UnsignedWordElement(0x2C20)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_33_TEMPERATURE, new UnsignedWordElement(0x2C21)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_34_TEMPERATURE, new UnsignedWordElement(0x2C22)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_35_TEMPERATURE, new UnsignedWordElement(0x2C23)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_36_TEMPERATURE, new UnsignedWordElement(0x2C24)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_37_TEMPERATURE, new UnsignedWordElement(0x2C25)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_38_TEMPERATURE, new UnsignedWordElement(0x2C26)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_39_TEMPERATURE, new UnsignedWordElement(0x2C27)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_40_TEMPERATURE, new UnsignedWordElement(0x2C28)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_41_TEMPERATURE, new UnsignedWordElement(0x2C29)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_42_TEMPERATURE, new UnsignedWordElement(0x2C2A)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_43_TEMPERATURE, new UnsignedWordElement(0x2C2B)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_44_TEMPERATURE, new UnsignedWordElement(0x2C2C)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_45_TEMPERATURE, new UnsignedWordElement(0x2C2D)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_46_TEMPERATURE, new UnsignedWordElement(0x2C2E)), //
						m(SoltaroGridcon.ChannelId.CLUSTER_1_BATTERY_47_TEMPERATURE, new UnsignedWordElement(0x2C2F)) //
				)//
		); //
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode));
	}

	@Override
	public float getCapacityX() {
		return getCapacity().value().orElse(0);
	}

	@Override
	public float getCurrentX() {
		return getCurrent().value().orElse(0);
	}

	@Override
	public float getVoltageX() {
		return getVoltage().value().orElse(0);
	}

	@Override
	public float getMaxChargeCurrentX() {
		return getChargeMaxCurrent().value().orElse(0);
	}

	@Override
	public float getMaxDischargeCurrentX() {
		return getDischargeMaxCurrent().value().orElse(0);
	}

	@Override
	public float getMaxChargeVoltageX() {
		return getChargeMaxVoltage().value().orElse(0);
	}

	@Override
	public float getMinDischargeVoltageX() {
		return getDischargeMinVoltage().value().orElse(0);
	}


}
