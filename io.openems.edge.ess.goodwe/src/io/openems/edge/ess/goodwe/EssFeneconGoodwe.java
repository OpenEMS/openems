package io.openems.edge.ess.goodwe;

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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Goodwe", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE) //

public class EssFeneconGoodwe extends AbstractOpenemsModbusComponent
		implements SymmetricEss, ManagedSymmetricEss, OpenemsComponent {

	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(EssFeneconGoodwe.class);
	public static final int DEFAULT_UNIT_ID = 1;
	private boolean readonly = false;

	private Battery battery;
	
	//private InverterState inverterState;
	//
	//public enum InverterState {
	//	ON,
	//	OFF
	//}

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Power power;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setBattery(Battery battery) {
		this.battery = battery;
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus); // Bridge Modbus
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), DEFAULT_UNIT_ID, this.cm, "Modbus",
				config.modbus_id());
		
		this.readonly = config.readonly();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public EssFeneconGoodwe() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				GoodweChannelId.values() // //
		);
		this.channel(SymmetricEss.ChannelId.MAX_APPARENT_POWER).setNextValue(GoodweChannelId.TOTAL_APPARENT_POWER);
	}



	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this,
				new FC16WriteRegistersTask(0000,
						m(GoodweChannelId.LOWEST_FEEDING_VOLTAGE_PV, new UnsignedWordElement(0x0000)),
						m(GoodweChannelId.RECONNECT_TIME, new UnsignedWordElement(0x0001)),
						m(GoodweChannelId.HIGH_LIMIT_GRID_VOLTAGE, new UnsignedWordElement(0x0002)),
						m(GoodweChannelId.LOW_LIMIT_GRID_VOLTAGE, new UnsignedWordElement(0x0003)),
						m(GoodweChannelId.LOW_LIMIT_GRID_FREQUENCY, new UnsignedWordElement(0x0004)),
						m(GoodweChannelId.HIGH_LIMIT_GRID_FREQUENCY, new UnsignedWordElement(0x0005)),
						new DummyRegisterElement(0x0006, 0x000F),
						m(GoodweChannelId.RTC_YEAR_MONTH, new UnsignedWordElement(0x0010)),
						m(GoodweChannelId.RTC_DATE_HOUR, new UnsignedWordElement(0x0011)),
						m(GoodweChannelId.RTC_MINUTE_SECOND, new UnsignedWordElement(0x0012)),
						new DummyRegisterElement(0x0013, 0x009F),

						m(GoodweChannelId.RANGE_REAL_POWER_ADJUST, new UnsignedWordElement(0x0100)),
						m(GoodweChannelId.RANGE_REACTIVE_POWER_ADJUST, new UnsignedWordElement(0x0101)),
						new DummyRegisterElement(0x0102, 0x019F)),

//						m(GoodweChannelId.SERIAL_NUMBER_INVERTER, new UnsignedWordElement(0x0200)), // ASCII
//						m(GoodweChannelId.SERIAL_NUMBER_INVERTER, new UnsignedWordElement(0x0201)), //
//						m(GoodweChannelId.SERIAL_NUMBER_INVERTER, new UnsignedWordElement(0x0202)), //
//						m(GoodweChannelId.SERIAL_NUMBER_INVERTER, new UnsignedWordElement(0x0203)), //
//						m(GoodweChannelId.SERIAL_NUMBER_INVERTER, new UnsignedWordElement(0x0204)), //
//						m(GoodweChannelId.SERIAL_NUMBER_INVERTER, new UnsignedWordElement(0x0205)), //
//						m(GoodweChannelId.SERIAL_NUMBER_INVERTER, new UnsignedWordElement(0x0206)), //
//						m(GoodweChannelId.SERIAL_NUMBER_INVERTER, new UnsignedWordElement(0x0207))), //

				new FC3ReadRegistersTask(0000, Priority.LOW,
						m(GoodweChannelId.LOWEST_FEEDING_VOLTAGE_PV, new UnsignedWordElement(0x0000)),
						m(GoodweChannelId.RECONNECT_TIME, new UnsignedWordElement(0x0001)),
						m(GoodweChannelId.HIGH_LIMIT_GRID_VOLTAGE, new UnsignedWordElement(0x0002)),
						m(GoodweChannelId.LOW_LIMIT_GRID_VOLTAGE, new UnsignedWordElement(0x0003)),
						m(GoodweChannelId.LOW_LIMIT_GRID_FREQUENCY, new UnsignedWordElement(0x0004)),
						m(GoodweChannelId.HIGH_LIMIT_GRID_FREQUENCY, new UnsignedWordElement(0x0005)),
						new DummyRegisterElement(0x0006, 0x000F),

						m(GoodweChannelId.RTC_YEAR_MONTH, new UnsignedWordElement(0x0010)),
						m(GoodweChannelId.RTC_DATE_HOUR, new UnsignedWordElement(0x0011)),
						m(GoodweChannelId.RTC_MINUTE_SECOND, new UnsignedWordElement(0x0012)),
						new DummyRegisterElement(0x0013, 0x009F),

						m(GoodweChannelId.RANGE_REAL_POWER_ADJUST, new UnsignedWordElement(0x0100)),
						m(GoodweChannelId.RANGE_REACTIVE_POWER_ADJUST, new UnsignedWordElement(0x0101)),
						new DummyRegisterElement(0x0102, 0x01FF)),

//						m(GoodweChannelId.SERIAL_NUMBER_INVERTER, new UnsignedWordElement(0x0200)), // ASCII
//						m(GoodweChannelId.SERIAL_NUMBER_INVERTER, new UnsignedWordElement(0x0201)), //
//						m(GoodweChannelId.SERIAL_NUMBER_INVERTER, new UnsignedWordElement(0x0202)), //
//						m(GoodweChannelId.SERIAL_NUMBER_INVERTER, new UnsignedWordElement(0x0203)), //
//						m(GoodweChannelId.SERIAL_NUMBER_INVERTER, new UnsignedWordElement(0x0204)), //
//						m(GoodweChannelId.SERIAL_NUMBER_INVERTER, new UnsignedWordElement(0x0205)), //
//						m(GoodweChannelId.SERIAL_NUMBER_INVERTER, new UnsignedWordElement(0x0206)), //
//						m(GoodweChannelId.SERIAL_NUMBER_INVERTER, new UnsignedWordElement(0x0207)), //
//						m(GoodweChannelId.NOM_VPV, new UnsignedWordElement(0x0208)), // should i add the R/W
//																								// read values here?
//						m(GoodweChannelId.NOM_VPV, new UnsignedWordElement(0x0209)), //
//
//						m(GoodweChannelId.FIRMWARE_VERSION, new UnsignedWordElement(0x020A)), //
//						m(GoodweChannelId.FIRMWARE_VERSION, new UnsignedWordElement(0x020B)), //
//						m(GoodweChannelId.FIRMWARE_VERSION, new UnsignedWordElement(0x020C))), //
//
//				new FC16WriteRegistersTask(0x0210,
//						m(GoodweChannelId.MODEL_NAME_INVERTER, new UnsignedWordElement(0x0210)), //
//						m(GoodweChannelId.MODEL_NAME_INVERTER, new UnsignedWordElement(0x0211)), //
//						m(GoodweChannelId.MODEL_NAME_INVERTER, new UnsignedWordElement(0x0212)), //
//						m(GoodweChannelId.MODEL_NAME_INVERTER, new UnsignedWordElement(0x0213)), //
//						m(GoodweChannelId.MODEL_NAME_INVERTER, new UnsignedWordElement(0x0214))), //

				//new FC3ReadRegistersTask(0x0210, Priority.LOW,
//						m(GoodweChannelId.MODEL_NAME_INVERTER, new UnsignedWordElement(0x0210)), //
//						m(GoodweChannelId.MODEL_NAME_INVERTER, new UnsignedWordElement(0x0211)), //
//						m(GoodweChannelId.MODEL_NAME_INVERTER, new UnsignedWordElement(0x0212)), //
//						m(GoodweChannelId.MODEL_NAME_INVERTER, new UnsignedWordElement(0x0213)), //
//						m(GoodweChannelId.MODEL_NAME_INVERTER, new UnsignedWordElement(0x0214)), //
//
//						m(GoodweChannelId.DSP_INTERNAL_FIRMWARE_VERSION, new UnsignedWordElement(0x0215)), //
//						m(GoodweChannelId.DSP_INTERNAL_FIRMWARE_VERSION, new UnsignedWordElement(0x0216)), //
//						m(GoodweChannelId.DSP_INTERNAL_FIRMWARE_VERSION, new UnsignedWordElement(0x0217)), //
//						m(GoodweChannelId.DSP_INTERNAL_FIRMWARE_VERSION, new UnsignedWordElement(0x0218)), //
//						m(GoodweChannelId.DSP_INTERNAL_FIRMWARE_VERSION, new UnsignedWordElement(0x0219)), //
//						m(GoodweChannelId.DSP_INTERNAL_FIRMWARE_VERSION, new UnsignedWordElement(0x021A)), //
//
//						m(GoodweChannelId.ARM_INTERNAL_FIRMWARE_VERSION, new UnsignedWordElement(0x021B)), //
//						m(GoodweChannelId.ARM_INTERNAL_FIRMWARE_VERSION, new UnsignedWordElement(0x021C)), //
//						m(GoodweChannelId.ARM_INTERNAL_FIRMWARE_VERSION, new UnsignedWordElement(0x021D)), //
//						m(GoodweChannelId.ARM_INTERNAL_FIRMWARE_VERSION, new UnsignedWordElement(0x021E)), //
//						m(GoodweChannelId.ARM_INTERNAL_FIRMWARE_VERSION, new UnsignedWordElement(0x021F)), //
//						m(GoodweChannelId.ARM_INTERNAL_FIRMWARE_VERSION, new UnsignedWordElement(0x0220)), //
//
//						m(GoodweChannelId.MANUFACTURE_INFO, new UnsignedWordElement(0x0221)), //
//						m(GoodweChannelId.MANUFACTURE_INFO, new UnsignedWordElement(0x0222)), //
//						m(GoodweChannelId.MANUFACTURE_INFO, new UnsignedWordElement(0x0223)), //
//						m(GoodweChannelId.MANUFACTURE_INFO, new UnsignedWordElement(0x0224)), //
//						m(GoodweChannelId.MANUFACTURE_INFO, new UnsignedWordElement(0x0225)), //
//						m(GoodweChannelId.MANUFACTURE_INFO, new UnsignedWordElement(0x0226)), //
//						m(GoodweChannelId.MANUFACTURE_INFO, new UnsignedWordElement(0x0227)), //
//						m(GoodweChannelId.MANUFACTURE_INFO, new UnsignedWordElement(0x0228)) //
					new FC3ReadRegistersTask(0x0229, Priority.LOW,
						m(GoodweChannelId.FIRMWARE_VERSION_HEXA, new UnsignedWordElement(0x0229)), //
						m(GoodweChannelId.ARM_UPDATA_RESULT, new UnsignedWordElement(0x022A)), //
						m(GoodweChannelId.DSP_UPDATA_RESULT, new UnsignedWordElement(0x022B)), //
						new DummyRegisterElement(0x022C, 0x04FF)),

				// Hybrid Inverter
				new FC3ReadRegistersTask(0x0210, Priority.HIGH,
						m(GoodweChannelId.VPV1, new UnsignedWordElement(0x0500),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelId.IPV1, new UnsignedWordElement(0x0501),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelId.PV1_MODE, new UnsignedWordElement(0x0502)),

						m(GoodweChannelId.VPV2, new UnsignedWordElement(0x0503),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelId.IPV2, new UnsignedWordElement(0x0504),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelId.PV2_MODE, new UnsignedWordElement(0x0505)),
						m(GoodweChannelId.VBATTERY1, new UnsignedWordElement(0x0506),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelId.TBD1, new UnsignedWordElement(0x0507)),
						m(GoodweChannelId.BMS_STATUS, new UnsignedWordElement(0x0508)),
						m(GoodweChannelId.BMS_PACK_TEMPERATURE, new UnsignedWordElement(0x0509),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelId.IBATTERY1, new UnsignedWordElement(0x050A),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelId.BMS_CHARGE_IMAX, new UnsignedWordElement(0x050B)),
						m(GoodweChannelId.BMS_DISCHARGE_IMAX, new UnsignedWordElement(0x050C)),
						// m(GoodweChannelId.BMS_ERROR_CODE, new
						// UnsignedWordElement(0x050D)),
						m(new BitsWordElement(0x050D, this) //
								.bit(0, GoodweChannelId.STATE_0) //
								.bit(1, GoodweChannelId.STATE_1) //
								.bit(2, GoodweChannelId.STATE_2) //
								.bit(3, GoodweChannelId.STATE_3) //
								.bit(4, GoodweChannelId.STATE_4) //
								.bit(5, GoodweChannelId.STATE_5) //
								.bit(6, GoodweChannelId.STATE_6) //
								.bit(7, GoodweChannelId.STATE_7) //
								.bit(8, GoodweChannelId.STATE_8) //
								.bit(9, GoodweChannelId.STATE_9) //
								.bit(10, GoodweChannelId.STATE_10) //
								.bit(11, GoodweChannelId.STATE_11) //
								.bit(12, GoodweChannelId.STATE_12) //
								.bit(13, GoodweChannelId.STATE_13) //
						), m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(0x050E)),
						m(GoodweChannelId.INVERTER_WARNING_CODE, new UnsignedWordElement(0x050F)),
						m(GoodweChannelId.TBD2, new UnsignedWordElement(0x0510)),
						m(GoodweChannelId.BMS_SOH, new UnsignedWordElement(0x0511)),
						m(GoodweChannelId.BATTERY_MODE, new UnsignedWordElement(0x0512)),
//						m(GoodweChannelId.BMS_WARNING_CODE_H, new UnsignedWordElement(0x0513)),
//						m(GoodweChannelId.BMS_WARNING_CODE_L, new UnsignedWordElement(0x0514)),

						m(new BitsWordElement(0x0513, this) //
								.bit(0, GoodweChannelId.STATE_0) //
								.bit(1, GoodweChannelId.STATE_1) //
								.bit(2, GoodweChannelId.STATE_2) //
								.bit(3, GoodweChannelId.STATE_3) //
								.bit(4, GoodweChannelId.STATE_4) //
								.bit(5, GoodweChannelId.STATE_5) //
								.bit(6, GoodweChannelId.STATE_6) //
								.bit(7, GoodweChannelId.STATE_7) //
								.bit(8, GoodweChannelId.STATE_8) //
								.bit(9, GoodweChannelId.STATE_9) //
								.bit(10, GoodweChannelId.STATE_10) //
								.bit(11, GoodweChannelId.STATE_11) //
								.bit(12, GoodweChannelId.STATE_12) //
								.bit(13, GoodweChannelId.STATE_13) //
						),

						m(new BitsWordElement(0x0514, this) //
								.bit(0, GoodweChannelId.STATE_0) //
								.bit(1, GoodweChannelId.STATE_1) //
								.bit(2, GoodweChannelId.STATE_2) //
								.bit(3, GoodweChannelId.STATE_3) //
								.bit(4, GoodweChannelId.STATE_4) //
								.bit(5, GoodweChannelId.STATE_5) //
								.bit(6, GoodweChannelId.STATE_6) //
								.bit(7, GoodweChannelId.STATE_7) //
								.bit(8, GoodweChannelId.STATE_8) //
								.bit(9, GoodweChannelId.STATE_9) //
								.bit(10, GoodweChannelId.STATE_10) //
								.bit(11, GoodweChannelId.STATE_11) //
								.bit(12, GoodweChannelId.STATE_12) //
								.bit(13, GoodweChannelId.STATE_13) //
						),

						m(GoodweChannelId.METER_STATUS, new UnsignedWordElement(0x0515)),
						m(GoodweChannelId.VGRID, new UnsignedWordElement(0x0516),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.IGRID, new UnsignedWordElement(0x0517),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.PGRID, new UnsignedWordElement(0x0518)),
						m(GoodweChannelId.FGRID, new UnsignedWordElement(0x0519),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_MODE, new UnsignedWordElement(0x051A)),
						m(GoodweChannelId.VLOAD, new UnsignedWordElement(0x051B),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.ILOAD, new UnsignedWordElement(0x051C),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.ONGRID_LOAD_POWER, new UnsignedWordElement(0x051D)),
						m(GoodweChannelId.FLOAD, new UnsignedWordElement(0x051E),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.LOAD_MODE, new UnsignedWordElement(0x051F)),

						m(GoodweChannelId.INVERTER_WORK_MODE, new UnsignedWordElement(0x0520)),
						m(GoodweChannelId.TEMPERATURE, new UnsignedWordElement(0x0521),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						// m(GoodweChannelId.ERROR_MESSAGE_H, new
						// UnsignedWordElement(0x0522)),

						m(new BitsWordElement(0x0522, this) //
								.bit(0, GoodweChannelId.STATE_14) //
								.bit(1, GoodweChannelId.STATE_15) //
								.bit(2, GoodweChannelId.STATE_16) //
								.bit(3, GoodweChannelId.STATE_17) //
								.bit(4, GoodweChannelId.STATE_18) //
								.bit(5, GoodweChannelId.STATE_19) //
								.bit(6, GoodweChannelId.STATE_20) //
								.bit(7, GoodweChannelId.STATE_21) //
								.bit(8, GoodweChannelId.STATE_22) //
								.bit(9, GoodweChannelId.STATE_23) //
								.bit(10, GoodweChannelId.STATE_24) //
								.bit(11, GoodweChannelId.STATE_25) //
								.bit(12, GoodweChannelId.STATE_26) //
								.bit(13, GoodweChannelId.STATE_27) //
								.bit(14, GoodweChannelId.STATE_28) //
								.bit(15, GoodweChannelId.STATE_29) //
								.bit(16, GoodweChannelId.STATE_30) //
								.bit(17, GoodweChannelId.STATE_31) //
								.bit(18, GoodweChannelId.STATE_32) //
								.bit(19, GoodweChannelId.STATE_33) //
								.bit(20, GoodweChannelId.STATE_34) //
								.bit(21, GoodweChannelId.STATE_35) //
								.bit(22, GoodweChannelId.STATE_36) //
								.bit(23, GoodweChannelId.STATE_37) //
								.bit(24, GoodweChannelId.STATE_38) //
								.bit(25, GoodweChannelId.STATE_39) //
								.bit(26, GoodweChannelId.STATE_40) //
								.bit(27, GoodweChannelId.STATE_41) //
								.bit(28, GoodweChannelId.STATE_42) //
								.bit(29, GoodweChannelId.STATE_43) //
								.bit(30, GoodweChannelId.STATE_44) //
								.bit(31, GoodweChannelId.STATE_45) //

						),

						// m(GoodweChannelId.ERROR_MESSAGE_L, new
						// UnsignedWordElement(0x0523)),
						m(new BitsWordElement(0x0523, this) //
								.bit(0, GoodweChannelId.STATE_14) //
								.bit(1, GoodweChannelId.STATE_15) //
								.bit(2, GoodweChannelId.STATE_16) //
								.bit(3, GoodweChannelId.STATE_17) //
								.bit(4, GoodweChannelId.STATE_18) //
								.bit(5, GoodweChannelId.STATE_19) //
								.bit(6, GoodweChannelId.STATE_20) //
								.bit(7, GoodweChannelId.STATE_21) //
								.bit(8, GoodweChannelId.STATE_22) //
								.bit(9, GoodweChannelId.STATE_23) //
								.bit(10, GoodweChannelId.STATE_24) //
								.bit(11, GoodweChannelId.STATE_25) //
								.bit(12, GoodweChannelId.STATE_26) //
								.bit(13, GoodweChannelId.STATE_27) //
								.bit(14, GoodweChannelId.STATE_28) //
								.bit(15, GoodweChannelId.STATE_29) //
								.bit(16, GoodweChannelId.STATE_30) //
								.bit(17, GoodweChannelId.STATE_31) //
								.bit(18, GoodweChannelId.STATE_32) //
								.bit(19, GoodweChannelId.STATE_33) //
								.bit(20, GoodweChannelId.STATE_34) //
								.bit(21, GoodweChannelId.STATE_35) //
								.bit(22, GoodweChannelId.STATE_36) //
								.bit(23, GoodweChannelId.STATE_37) //
								.bit(24, GoodweChannelId.STATE_38) //
								.bit(25, GoodweChannelId.STATE_39) //
								.bit(26, GoodweChannelId.STATE_40) //
								.bit(27, GoodweChannelId.STATE_41) //
								.bit(28, GoodweChannelId.STATE_42) //
								.bit(29, GoodweChannelId.STATE_43) //
								.bit(30, GoodweChannelId.STATE_44) //
								.bit(31, GoodweChannelId.STATE_45) //

						),

						m(GoodweChannelId.E_TOTAL_H, new UnsignedWordElement(0x0524),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.E_TOTAL_L, new UnsignedWordElement(0x0525),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.H_TOTAL_H, new UnsignedWordElement(0x0526)),
						m(GoodweChannelId.H_TOTAL_L, new UnsignedWordElement(0x0527)),
						m(GoodweChannelId.E_DAY, new UnsignedWordElement(0x0528),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.E_LOAD_DAY, new UnsignedWordElement(0x0529),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.E_TOTAL_LOAD_H, new UnsignedWordElement(0x052A),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.E_TOTAL_LOAD_L, new UnsignedWordElement(0x052B),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.TOTAL_POWER, new SignedWordElement(0x052C)),
						m(GoodweChannelId.E_PV_TOTAL_H, new UnsignedWordElement(0x052D),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.E_PV_TOTAL_L, new UnsignedWordElement(0x052E),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.GRID_IN_OUT_FLAG, new UnsignedWordElement(0x052F)),
						m(GoodweChannelId.BACK_UP_LOAD_POWER, new UnsignedWordElement(0x0530)),
						m(GoodweChannelId.METER_POWER_FACTOR, new SignedWordElement(0x0531)),
//						m(GoodweChannelId.DIAG_STATUS_H, new UnsignedWordElement(0x0532)),
//						m(GoodweChannelId.DIAG_STATUS_L, new UnsignedWordElement(0x0533)),

						m(new BitsWordElement(0x0532, this) //
								.bit(0, GoodweChannelId.STATE_46) //
								.bit(1, GoodweChannelId.STATE_47) //
								.bit(2, GoodweChannelId.STATE_48) //
								.bit(3, GoodweChannelId.STATE_49) //
								.bit(4, GoodweChannelId.STATE_50) //
								.bit(5, GoodweChannelId.STATE_51) //
								.bit(6, GoodweChannelId.STATE_52) //
								.bit(7, GoodweChannelId.STATE_53) //
								.bit(8, GoodweChannelId.STATE_54) //
								.bit(9, GoodweChannelId.STATE_55) //
								.bit(10, GoodweChannelId.STATE_56) //
								.bit(11, GoodweChannelId.STATE_57) //
								.bit(12, GoodweChannelId.STATE_58) //
								.bit(13, GoodweChannelId.STATE_59) //
								.bit(14, GoodweChannelId.STATE_60) //
								.bit(15, GoodweChannelId.STATE_61) //
								.bit(16, GoodweChannelId.STATE_62) //
								.bit(17, GoodweChannelId.STATE_63) //
								.bit(18, GoodweChannelId.STATE_64) //
								.bit(19, GoodweChannelId.STATE_65) //
								.bit(20, GoodweChannelId.STATE_66) //
								.bit(21, GoodweChannelId.STATE_67) //
								.bit(22, GoodweChannelId.STATE_68) //
								.bit(23, GoodweChannelId.STATE_69) //
								.bit(24, GoodweChannelId.STATE_70) //
								.bit(25, GoodweChannelId.STATE_71) //
								.bit(26, GoodweChannelId.STATE_72) //
								.bit(27, GoodweChannelId.STATE_73) //
								.bit(28, GoodweChannelId.STATE_74) //
								.bit(29, GoodweChannelId.STATE_75) //
						),

						m(new BitsWordElement(0x0533, this) //
								.bit(0, GoodweChannelId.STATE_46) //
								.bit(1, GoodweChannelId.STATE_47) //
								.bit(2, GoodweChannelId.STATE_48) //
								.bit(3, GoodweChannelId.STATE_49) //
								.bit(4, GoodweChannelId.STATE_50) //
								.bit(5, GoodweChannelId.STATE_51) //
								.bit(6, GoodweChannelId.STATE_52) //
								.bit(7, GoodweChannelId.STATE_53) //
								.bit(8, GoodweChannelId.STATE_54) //
								.bit(9, GoodweChannelId.STATE_55) //
								.bit(10, GoodweChannelId.STATE_56) //
								.bit(11, GoodweChannelId.STATE_57) //
								.bit(12, GoodweChannelId.STATE_58) //
								.bit(13, GoodweChannelId.STATE_59) //
								.bit(14, GoodweChannelId.STATE_60) //
								.bit(15, GoodweChannelId.STATE_61) //
								.bit(16, GoodweChannelId.STATE_62) //
								.bit(17, GoodweChannelId.STATE_63) //
								.bit(18, GoodweChannelId.STATE_64) //
								.bit(19, GoodweChannelId.STATE_65) //
								.bit(20, GoodweChannelId.STATE_66) //
								.bit(21, GoodweChannelId.STATE_67) //
								.bit(22, GoodweChannelId.STATE_68) //
								.bit(23, GoodweChannelId.STATE_69) //
								.bit(24, GoodweChannelId.STATE_70) //
								.bit(25, GoodweChannelId.STATE_71) //
								.bit(26, GoodweChannelId.STATE_72) //
								.bit(27, GoodweChannelId.STATE_73) //
								.bit(28, GoodweChannelId.STATE_74) //
								.bit(29, GoodweChannelId.STATE_75) //
						),

						// m(GoodweChannelId.DRM_STATUS, new UnsignedWordElement(0x0534)),

						m(new BitsWordElement(0x0534, this) //
								.bit(0, GoodweChannelId.STATE_76) //
								.bit(1, GoodweChannelId.STATE_77) //
								.bit(2, GoodweChannelId.STATE_78) //
								.bit(3, GoodweChannelId.STATE_79) //
								.bit(4, GoodweChannelId.STATE_80) //
								.bit(5, GoodweChannelId.STATE_81) //
								.bit(6, GoodweChannelId.STATE_82) //
								.bit(7, GoodweChannelId.STATE_83) //
								.bit(8, GoodweChannelId.STATE_84) //
								.bit(15, GoodweChannelId.STATE_85) //
						),

						m(GoodweChannelId.E_TOTAL_SELL_H, new FloatDoublewordElement(0x0535)),
						m(GoodweChannelId.E_TOTAL_SELL_L, new FloatDoublewordElement(0x0536)),
						m(GoodweChannelId.E_TOTAL_BUY_H, new FloatDoublewordElement(0x0537)),
						m(GoodweChannelId.E_TOTAL_BUY_L, new FloatDoublewordElement(0x0538)),
						m(GoodweChannelId.VPV3, new UnsignedWordElement(0x0539),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.IPV3, new UnsignedWordElement(0x053A),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.PV3_MODE, new UnsignedWordElement(0x053B)),

						m(GoodweChannelId.VGRID_U0, new UnsignedWordElement(0x053C),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.IGRID_U0, new UnsignedWordElement(0x053D),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.VGRID_U0, new UnsignedWordElement(0x053E),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.IGRID_U0, new UnsignedWordElement(0x053F),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.E_BATTERY_CHARGE_H, new UnsignedWordElement(0x0540),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.E_BATTERY_CHARGE_L, new UnsignedWordElement(0x0541),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.E_BATTERY_DISCHARGE_H, new UnsignedWordElement(0x0542),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.E_BATTERY_DISCHARGE_L, new UnsignedWordElement(0x0543),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.PPV1, new UnsignedWordElement(0x0544)),
						m(GoodweChannelId.PPV2, new UnsignedWordElement(0x0545)),
						m(GoodweChannelId.PPV3, new UnsignedWordElement(0x0546)),
						m(GoodweChannelId.BATTERY_POWER, new UnsignedWordElement(0x0547)),
						m(GoodweChannelId.INT_E_TOTAL_SELL_H, new UnsignedWordElement(0x0548),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.INT_E_TOTAL_SELL_L, new UnsignedWordElement(0x0549),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),

						m(GoodweChannelId.INT_E_TOTAL_BUY_H, new UnsignedWordElement(0x054A),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.INT_E_TOTAL_BUY_L, new UnsignedWordElement(0x054B),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.E_BATTERY_CHARGE_TODAY, new UnsignedWordElement(0x054C),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.E_BATTERY_DISCHARGE_TODAY, new UnsignedWordElement(0x054D),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						new DummyRegisterElement(0x054E, 0x054F)),

				// Setting Register
				new FC16WriteRegistersTask(0x0550,
						m(GoodweChannelId.CHARGE_TIME_START, new UnsignedWordElement(0x0550)), // HM
						m(GoodweChannelId.CHARGE_TIME_END, new UnsignedWordElement(0x0551)), // HM
						m(GoodweChannelId.BATTERY_CHARGE_POWER_MAX, new UnsignedWordElement(0x0552)), //
						m(GoodweChannelId.DISCHARGER_TIME_START, new UnsignedWordElement(0x0553)), // HM
						m(GoodweChannelId.DISCHARGER_TIME_END, new UnsignedWordElement(0x0554)), // HM
						m(GoodweChannelId.BATTERY_DISCHARGE_POWER_SET, new UnsignedWordElement(0x0555)), //
						m(GoodweChannelId.BACKUP_ENABLE, new UnsignedWordElement(0x0556)), //

						m(GoodweChannelId.ENABLE_MPPT4_SHADOW, new UnsignedWordElement(0x0558)), //
						m(GoodweChannelId.FEED_POWER_ENABLE, new UnsignedWordElement(0x0559)), //

						m(GoodweChannelId.LEAD_BATTERY_CAPACITY, new UnsignedWordElement(0x055B)), //
						m(GoodweChannelId.BATTERY_CHARGE_VOLT_MAX, new UnsignedWordElement(0x055C),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelId.BATTERY_CHARGE_CURRENT_MAX, new UnsignedWordElement(0x055D),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelId.BATTERY_DISCHARGE_CURRENT_MAX, new UnsignedWordElement(0x055E),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelId.BATTERY_VOLT_UNDER_MINIMUM, new UnsignedWordElement(0x055F),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelId.BATTERY_SOC_UNDER_MINIMUM, new UnsignedWordElement(0x0560)), //
						m(GoodweChannelId.BATTERY_ACTIVE_PERIOD, new UnsignedWordElement(0x0561)), //
						m(GoodweChannelId.RP_CONTROL_PARA, new UnsignedWordElement(0x0562)), //
						m(GoodweChannelId.BATTERY_FLOAT_VOLT, new UnsignedWordElement(0x0563),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelId.BATTERY_FLOAT_CURRENT, new UnsignedWordElement(0x0564),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelId.BATTERY_TO_FLOAT_TIME, new UnsignedWordElement(0x0565)), //
						m(GoodweChannelId.BATTERY_TYPE_INDEX, new UnsignedWordElement(0x0566)), //
						m(GoodweChannelId.FEED_POWER_PARA, new UnsignedWordElement(0x0567)), //
						m(GoodweChannelId.AUTO_START_BACKUP, new UnsignedWordElement(0x0568)), //
						m(GoodweChannelId.STOP_SOC_PROTECT, new UnsignedWordElement(0x0569)), //
						m(GoodweChannelId.DC_VOLT_OUTPUT, new UnsignedWordElement(0x056A)),

						m(GoodweChannelId.BATTERY_AVERAGE_CHARGE_VOLT, new UnsignedWordElement(0x056B),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.BATTERY_AVERAGE_CHARGE_HOURS, new UnsignedWordElement(0x056C)),
						m(GoodweChannelId.WG_POWER_MODE, new UnsignedWordElement(0x056E)),
						m(GoodweChannelId.WG_POWER_SET, new UnsignedWordElement(0x056F)),
						m(GoodweChannelId.APP_MODE_INDEX, new UnsignedWordElement(0x0574)),
						m(GoodweChannelId.GRID_WAVE_CHECK_LEVEL, new UnsignedWordElement(0x0575)),
						m(GoodweChannelId.METER_CHECK_VALUE, new UnsignedWordElement(0x0576)),
						m(GoodweChannelId.RAPAID_CUT_OFF, new UnsignedWordElement(0x0577)),
						m(GoodweChannelId.GRID_VOLT_QUALITY, new UnsignedWordElement(0x0578),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.GRID_VOLT_HIGH_S2, new UnsignedWordElement(0x0579),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.GRID_VOLT_LOW_S2, new UnsignedWordElement(0x057A),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.GRID_VOLT_HIGH_S2_TIME, new UnsignedWordElement(0x057B),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_VOLT_LOW_S2_TIME, new UnsignedWordElement(0x057C),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_VOLT_HIGH_S1, new UnsignedWordElement(0x057D),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.GRID_VOLT_LOW_S1, new UnsignedWordElement(0x057E),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.GRID_VOLT_HIGH_S1_TIME, new UnsignedWordElement(0x057F),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_VOLT_LOW_S1_TIME, new UnsignedWordElement(0x0580),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_FREQUENCY_HIGH_S2, new UnsignedWordElement(0x0581),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_FREQUENCY_LOW_S2, new UnsignedWordElement(0x0582),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),

						m(GoodweChannelId.GRID_FREQUENCY_HIGH_S2_TIME, new UnsignedWordElement(0x0583),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_FREQUENCY_LOW_S2_TIME, new UnsignedWordElement(0x0584),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_FREQUENCY_HIGH_S1, new UnsignedWordElement(0x0585),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_FREQUENCY_LOW_S1, new UnsignedWordElement(0x0586),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_FREQUENCY_HIGH_S1_TIME, new UnsignedWordElement(0x0587),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_FREQUENCY_LOW_S1_TIME, new UnsignedWordElement(0x0588),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_VOLT_RECOVER_HIGH, new UnsignedWordElement(0x0589),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.GRID_VOLT_RECOVER_LOW, new UnsignedWordElement(0x058A),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.GRID_VOLT_RECOVER_TIME, new UnsignedWordElement(0x058B),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_FREQUENCY_RECOVER_HIGH, new UnsignedWordElement(0x058C),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_FREQUENCY_RECOVER_LOW, new UnsignedWordElement(0x058D),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_FREQUENCY_RECOVER_TIME, new UnsignedWordElement(0x058E),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.POINT_B_VALUE, new UnsignedWordElement(0x058F)),
						m(GoodweChannelId.POINT_C_VALUE, new UnsignedWordElement(0x0590),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_LIMIT_BY_VOLTAGE_START_VOLTAGE,
								new UnsignedWordElement(0x0591), ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.GRID_LIMIT_BY_VOLTAGE_START_PERCENT,
								new UnsignedWordElement(0x0592)),
						m(GoodweChannelId.GRID_LIMIT_BY_VOLTAGE_SLOPE, new UnsignedWordElement(0x0593),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.ACTIVE_CURVE_VOLTAGE, new UnsignedWordElement(0x0594),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.DESACTIVE_CURVE_VOLTAGE, new UnsignedWordElement(0x0595),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),

						m(GoodweChannelId.ENABLE_CURVE, new UnsignedWordElement(0x0596)),
						m(GoodweChannelId.BACKUP_START_DELAY, new UnsignedWordElement(0x0597)),
						m(GoodweChannelId.RECOVER_TIME_EE, new UnsignedWordElement(0x0598)),
						m(GoodweChannelId.SAFETY_COUNTRY, new UnsignedWordElement(0x0599)),

						m(GoodweChannelId.BMS_CURRENT_LIOMIT_COEFFICIENT, new UnsignedWordElement(0x059B)),
						m(GoodweChannelId.WMETER_CONNECT_CHECK_FLAG, new UnsignedWordElement(0x059C)),

						m(GoodweChannelId.UPS_STANDARD_VOLT_TYPE, new UnsignedWordElement(0x059E)),
						m(GoodweChannelId.BATTERY_OFFLINE_VOLT_UNDER_MIN, new UnsignedWordElement(0x05A0),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.BATTERY_OFFLINE_SOC_UNDER_MIN, new UnsignedWordElement(0x05A1)),
						m(GoodweChannelId.ONLY_NIGHT_DISCHARGE, new UnsignedWordElement(0x05A2)),
						m(GoodweChannelId.BMS_PROTOCOL_CODE, new UnsignedWordElement(0x05A3)),
						m(GoodweChannelId.HIGH_VOLTAGE_BATTERY_STRING, new UnsignedWordElement(0x05A4)),
						m(GoodweChannelId.OFFLINE_MPPTS_CAN_ENABLE, new UnsignedWordElement(0x05A5)),
						new DummyRegisterElement(0x05A6, 0x05FF)), //

				new FC3ReadRegistersTask(0x0557, Priority.LOW,
						m(GoodweChannelId.CHARGE_TIME_START, new UnsignedWordElement(0x0550)), // HM
						m(GoodweChannelId.CHARGE_TIME_END, new UnsignedWordElement(0x0551)), // HM
						m(GoodweChannelId.BATTERY_CHARGE_POWER_MAX, new UnsignedWordElement(0x0552)), //
						m(GoodweChannelId.DISCHARGER_TIME_START, new UnsignedWordElement(0x0553)), // HM
						m(GoodweChannelId.DISCHARGER_TIME_END, new UnsignedWordElement(0x0554)), // HM
						m(GoodweChannelId.BATTERY_DISCHARGE_POWER_SET, new UnsignedWordElement(0x0555)), //
						m(GoodweChannelId.BACKUP_ENABLE, new UnsignedWordElement(0x0556)), //
						m(GoodweChannelId.OFF_GRID_AUTO_CHARGE, new UnsignedWordElement(0x0557)), //
						m(GoodweChannelId.ENABLE_MPPT4_SHADOW, new UnsignedWordElement(0x0558)), //
						m(GoodweChannelId.FEED_POWER_ENABLE, new UnsignedWordElement(0x0559)), //
						m(GoodweChannelId.MANUFACTURE_CODE, new UnsignedWordElement(0x055A)), //

						m(GoodweChannelId.LEAD_BATTERY_CAPACITY, new UnsignedWordElement(0x055B)), //
						m(GoodweChannelId.BATTERY_CHARGE_VOLT_MAX, new UnsignedWordElement(0x055C),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelId.BATTERY_CHARGE_CURRENT_MAX, new UnsignedWordElement(0x055D),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelId.BATTERY_DISCHARGE_CURRENT_MAX, new UnsignedWordElement(0x055E),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelId.BATTERY_VOLT_UNDER_MINIMUM, new UnsignedWordElement(0x055F),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelId.BATTERY_SOC_UNDER_MINIMUM, new UnsignedWordElement(0x0560)), //
						m(GoodweChannelId.BATTERY_ACTIVE_PERIOD, new UnsignedWordElement(0x0561)), //
						m(GoodweChannelId.RP_CONTROL_PARA, new UnsignedWordElement(0x0562)), //
						m(GoodweChannelId.BATTERY_FLOAT_VOLT, new UnsignedWordElement(0x0563),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelId.BATTERY_FLOAT_CURRENT, new UnsignedWordElement(0x0564),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GoodweChannelId.BATTERY_TO_FLOAT_TIME, new UnsignedWordElement(0x0565)), //
						m(GoodweChannelId.BATTERY_TYPE_INDEX, new UnsignedWordElement(0x0566)), //
						m(GoodweChannelId.FEED_POWER_PARA, new UnsignedWordElement(0x0567)), //
						m(GoodweChannelId.AUTO_START_BACKUP, new UnsignedWordElement(0x0568)), //
						m(GoodweChannelId.STOP_SOC_PROTECT, new UnsignedWordElement(0x0569)), //
						m(GoodweChannelId.DC_VOLT_OUTPUT, new UnsignedWordElement(0x056A)),

						m(GoodweChannelId.BATTERY_AVERAGE_CHARGE_VOLT, new UnsignedWordElement(0x056B),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.BATTERY_AVERAGE_CHARGE_HOURS, new UnsignedWordElement(0x056C)),

						// m(GoodweChannelId.AS477_PARAMETERS, new
						// UnsignedWordElement(0x056D)), //
						m(new BitsWordElement(0x056D, this) //
								.bit(0, GoodweChannelId.STATE_86) //
								.bit(1, GoodweChannelId.STATE_87) //
								.bit(2, GoodweChannelId.STATE_88) //
								.bit(3, GoodweChannelId.STATE_89) //
								.bit(4, GoodweChannelId.STATE_90) //
								.bit(5, GoodweChannelId.STATE_91) //
								.bit(6, GoodweChannelId.STATE_92) //
								.bit(7, GoodweChannelId.STATE_93) //
								.bit(8, GoodweChannelId.STATE_94) //
								.bit(9, GoodweChannelId.STATE_95) //
								.bit(10, GoodweChannelId.STATE_96) //
								.bit(11, GoodweChannelId.STATE_97) //
								.bit(12, GoodweChannelId.STATE_98) //
								.bit(13, GoodweChannelId.STATE_99) //
								.bit(14, GoodweChannelId.STATE_100) //
								.bit(15, GoodweChannelId.STATE_101) //
						),

						m(GoodweChannelId.WG_POWER_MODE, new UnsignedWordElement(0x056E)),
						m(GoodweChannelId.WG_POWER_SET, new UnsignedWordElement(0x056F)),

						m(GoodweChannelId.RESERVED, new UnsignedWordElement(0x0570)), //
						m(GoodweChannelId.NO_GRID_CHARGE_ENABLE, new UnsignedWordElement(0x0571)), //
						m(GoodweChannelId.DISCHARGE_WITH_PV_ENABLE, new UnsignedWordElement(0x0572)), //
						m(GoodweChannelId.RESERVED2, new UnsignedWordElement(0x0573)), //

						m(GoodweChannelId.APP_MODE_INDEX, new UnsignedWordElement(0x0574)),
						m(GoodweChannelId.GRID_WAVE_CHECK_LEVEL, new UnsignedWordElement(0x0575)),
						m(GoodweChannelId.METER_CHECK_VALUE, new UnsignedWordElement(0x0576)),
						m(GoodweChannelId.RAPAID_CUT_OFF, new UnsignedWordElement(0x0577)),
						m(GoodweChannelId.GRID_VOLT_QUALITY, new UnsignedWordElement(0x0578),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.GRID_VOLT_HIGH_S2, new UnsignedWordElement(0x0579),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.GRID_VOLT_LOW_S2, new UnsignedWordElement(0x057A),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.GRID_VOLT_HIGH_S2_TIME, new UnsignedWordElement(0x057B),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_VOLT_LOW_S2_TIME, new UnsignedWordElement(0x057C),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_VOLT_HIGH_S1, new UnsignedWordElement(0x057D),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.GRID_VOLT_LOW_S1, new UnsignedWordElement(0x057E),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.GRID_VOLT_HIGH_S1_TIME, new UnsignedWordElement(0x057F),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_VOLT_LOW_S1_TIME, new UnsignedWordElement(0x0580),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_FREQUENCY_HIGH_S2, new UnsignedWordElement(0x0581),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_FREQUENCY_LOW_S2, new UnsignedWordElement(0x0582),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),

						m(GoodweChannelId.GRID_FREQUENCY_HIGH_S2_TIME, new UnsignedWordElement(0x0583),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_FREQUENCY_LOW_S2_TIME, new UnsignedWordElement(0x0584),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_FREQUENCY_HIGH_S1, new UnsignedWordElement(0x0585),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_FREQUENCY_LOW_S1, new UnsignedWordElement(0x0586),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_FREQUENCY_HIGH_S1_TIME, new UnsignedWordElement(0x0587),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_FREQUENCY_LOW_S1_TIME, new UnsignedWordElement(0x0588),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_VOLT_RECOVER_HIGH, new UnsignedWordElement(0x0589),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.GRID_VOLT_RECOVER_LOW, new UnsignedWordElement(0x058A),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.GRID_VOLT_RECOVER_TIME, new UnsignedWordElement(0x058B),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_FREQUENCY_RECOVER_HIGH, new UnsignedWordElement(0x058C),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_FREQUENCY_RECOVER_LOW, new UnsignedWordElement(0x058D),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_FREQUENCY_RECOVER_TIME, new UnsignedWordElement(0x058E),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.POINT_B_VALUE, new UnsignedWordElement(0x058F)),
						m(GoodweChannelId.POINT_C_VALUE, new UnsignedWordElement(0x0590),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.GRID_LIMIT_BY_VOLTAGE_START_VOLTAGE,
								new UnsignedWordElement(0x0591), ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.GRID_LIMIT_BY_VOLTAGE_START_PERCENT,
								new UnsignedWordElement(0x0592)),
						m(GoodweChannelId.GRID_LIMIT_BY_VOLTAGE_SLOPE, new UnsignedWordElement(0x0593),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.ACTIVE_CURVE_VOLTAGE, new UnsignedWordElement(0x0594),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.DESACTIVE_CURVE_VOLTAGE, new UnsignedWordElement(0x0595),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),

						m(GoodweChannelId.ENABLE_CURVE, new UnsignedWordElement(0x0596)),
						m(GoodweChannelId.BACKUP_START_DELAY, new UnsignedWordElement(0x0597)),
						m(GoodweChannelId.RECOVER_TIME_EE, new UnsignedWordElement(0x0598)),
						m(GoodweChannelId.SAFETY_COUNTRY, new UnsignedWordElement(0x0599)),
						m(GoodweChannelId.ISO_LIMIT, new UnsignedWordElement(0x059A)), //
						m(GoodweChannelId.BMS_CURRENT_LIOMIT_COEFFICIENT, new UnsignedWordElement(0x059B)),
						m(GoodweChannelId.WMETER_CONNECT_CHECK_FLAG, new UnsignedWordElement(0x059C)),
						m(GoodweChannelId.METER_CONNECT_STATUS, new UnsignedWordElement(0x059D)), //
						m(GoodweChannelId.UPS_STANDARD_VOLT_TYPE, new UnsignedWordElement(0x059E)),
						m(GoodweChannelId.FUNCTION_STATUS, new UnsignedWordElement(0x059F)),

						m(new BitsWordElement(0x059F, this) //
								.bit(0, GoodweChannelId.STATE_102) //
								.bit(1, GoodweChannelId.STATE_103) //
								.bit(2, GoodweChannelId.STATE_104) //
								.bit(3, GoodweChannelId.STATE_105) //
								.bit(4, GoodweChannelId.STATE_106) //
								.bit(5, GoodweChannelId.STATE_107) //
								.bit(6, GoodweChannelId.STATE_108) //
								.bit(7, GoodweChannelId.STATE_109) //
								.bit(8, GoodweChannelId.STATE_110) //
								.bit(9, GoodweChannelId.STATE_111) //
								.bit(10, GoodweChannelId.STATE_112) //
								.bit(11, GoodweChannelId.STATE_113) //
								.bit(12, GoodweChannelId.STATE_114) //
								.bit(13, GoodweChannelId.STATE_115) //
								.bit(14, GoodweChannelId.STATE_116) //
								.bit(15, GoodweChannelId.STATE_117) //
						),

						m(GoodweChannelId.BATTERY_OFFLINE_VOLT_UNDER_MIN, new UnsignedWordElement(0x05A0),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.BATTERY_OFFLINE_SOC_UNDER_MIN, new UnsignedWordElement(0x05A1)),
						m(GoodweChannelId.ONLY_NIGHT_DISCHARGE, new UnsignedWordElement(0x05A2)),
						m(GoodweChannelId.BMS_PROTOCOL_CODE, new UnsignedWordElement(0x05A3)),
						m(GoodweChannelId.HIGH_VOLTAGE_BATTERY_STRING, new UnsignedWordElement(0x05A4)),
						m(GoodweChannelId.OFFLINE_MPPTS_CAN_ENABLE, new UnsignedWordElement(0x05A5)),
						new DummyRegisterElement(0x05A6, 0x05FF)), //

				// Meter Data Address
				new FC3ReadRegistersTask(0x6000, Priority.LOW,
						m(GoodweChannelId.ACR_METER_TYPE, new UnsignedWordElement(0x6000)),
						m(GoodweChannelId.METER_STATUS2, new UnsignedWordElement(0x6001))),

				new FC3ReadRegistersTask(0x6002, Priority.HIGH,
						m(GoodweChannelId.PHASE_A_VOLTAGE, new UnsignedWordElement(0x6002),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.PHASE_B_VOLTAGE, new UnsignedWordElement(0x6003),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.PHASE_C_VOLTAGE, new UnsignedWordElement(0x6004),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.PHASE_A_CURRENT, new UnsignedWordElement(0x6005),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.PHASE_B_CURRENT, new UnsignedWordElement(0x6006),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.PHASE_C_CURRENT, new UnsignedWordElement(0x6007),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.PHASE_A_ACTIVE_POWER, new SignedWordElement(0x6008)),
						m(GoodweChannelId.PHASE_B_ACTIVE_POWER, new SignedWordElement(0x6009)),
						m(GoodweChannelId.PHASE_C_ACTIVE_POWER, new SignedWordElement(0x600A)),
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedWordElement(0x600B)),

						m(GoodweChannelId.PHASE_A_REACTIVE_POWER, new SignedWordElement(0x600C)),
						m(GoodweChannelId.PHASE_B_REACTIVE_POWER, new SignedWordElement(0x600D)),
						m(GoodweChannelId.PHASE_C_REACTIVE_POWER, new SignedWordElement(0x600E)),
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new SignedWordElement(0x600F)),
						m(GoodweChannelId.PHASE_A_APPARENT_POWER, new SignedWordElement(0x6010)),
						m(GoodweChannelId.PHASE_B_APPARENT_POWER, new SignedWordElement(0x6011)),
						m(SymmetricEss.ChannelId.MAX_APPARENT_POWER, new SignedWordElement(0x6012)),
						m(GoodweChannelId.TOTAL_APPARENT_POWER, new SignedWordElement(0x6013)),
						m(GoodweChannelId.PHASE_A_POWER_FACTOR, new SignedWordElement(0x6014)),
						m(GoodweChannelId.PHASE_B_POWER_FACTOR, new SignedWordElement(0x6015)),
						m(GoodweChannelId.PHASE_C_POWER_FACTOR, new SignedWordElement(0x6016)),
						m(GoodweChannelId.TOTAL_POWER_FACTOR, new SignedWordElement(0x6017)),
						m(GoodweChannelId.FREQUENCY, new UnsignedWordElement(0x6018),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.E_TOTAL_SELL_H2, new FloatDoublewordElement(0x6019)),

						m(GoodweChannelId.E_TOTAL_SELL_L2, new FloatDoublewordElement(0x601A)),
						m(GoodweChannelId.E_TOTAL_BUY_H2, new FloatDoublewordElement(0x601B)),
						m(GoodweChannelId.E_TOTAL_BUY_L2, new FloatDoublewordElement(0x601C)),
						new DummyRegisterElement(0x601D, 0x70FF)), // need to confirm

				new FC3ReadRegistersTask(0x7100, Priority.HIGH,
						m(GoodweChannelId.VAC1, new UnsignedWordElement(0x7100),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.FAC1, new UnsignedWordElement(0x7101),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.PACL, new UnsignedWordElement(0x7102)),
						m(GoodweChannelId.WORK_MODE2, new UnsignedWordElement(0x7103)),
						m(GoodweChannelId.ERROR_MESSAGE_H2, new UnsignedWordElement(0x7104)),
						m(GoodweChannelId.ERROR_MESSAGE_L2, new UnsignedWordElement(0x7105)),
						m(GoodweChannelId.LINE1_AVERAGE_FAULT_VALUE, new UnsignedWordElement(0x7106),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.LINE1_AVERAGE_FAULT_TIME, new UnsignedWordElement(0x7107)),

						m(GoodweChannelId.LINE1_V_HIGH_FAULT_VALUE, new UnsignedWordElement(0x7108),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.LINE1_V_HIGH_FAULT_TIME, new UnsignedWordElement(0x7109)), // ms
						m(GoodweChannelId.LINE1_V_LOW_FAULT_VALUE, new UnsignedWordElement(0x710A),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.LINE1_V_LOW_FAULT_TIME, new UnsignedWordElement(0x710B)), // ms
						m(GoodweChannelId.LINE1_F_HIGH_FAULT_VALUE_COM, new UnsignedWordElement(0x710C),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.LINE1_F_HIGH_FAULT_TIME_COM, new UnsignedWordElement(0x710D)), // ms
						m(GoodweChannelId.LINE1_F_LOW_FAULT_VALUE_COM, new UnsignedWordElement(0x710E),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.LINE1_F_LOW_FAULT_TIME_COM, new UnsignedWordElement(0x710F)), // ms
						m(GoodweChannelId.LINE1_F_HIGH_FAULT_VALUE, new UnsignedWordElement(0x7110),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.LINE1_F_HIGH_FAULT_TIME, new UnsignedWordElement(0x7111)), // ms
						m(GoodweChannelId.LINE1_F_LOW_FAULT_VALUE, new UnsignedWordElement(0x7112),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.LINE1_F_LOW_FAULT_TIME, new UnsignedWordElement(0x7113)), // ms
						m(GoodweChannelId.SIM_VOLTAGE, new UnsignedWordElement(0x7114),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(GoodweChannelId.SIM_FREQUENCY, new UnsignedWordElement(0x7115),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(GoodweChannelId.TEST_RESULT, new UnsignedWordElement(0x7116)),
						m(GoodweChannelId.SELF_TEST_STEP, new UnsignedWordElement(0x7117)),
						m(GoodweChannelId.START_TEST, new UnsignedWordElement(0x7118))),

				new FC16WriteRegistersTask(0x7117,
						m(GoodweChannelId.SELF_TEST_STEP, new UnsignedWordElement(0x7117)),
						m(GoodweChannelId.START_TEST, new UnsignedWordElement(0x7118)),
						m(GoodweChannelId.SET_REMOTE_SAFETY, new UnsignedWordElement(0x7119))));
	}

	@Override
	public Power getPower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Constraint[] getStaticConstraints() throws OpenemsException {
		if (!battery.getReadyForWorking().value().orElse(false)) {
			return new Constraint[] { //
					this.createPowerConstraint("Battery is not ready", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0), //
					this.createPowerConstraint("Battery is not ready", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) //
			};
		} else {
			return Power.NO_CONSTRAINTS;
		}
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		if(this.readonly) {
			return;
		}
	}
	
	//TODO : check for allowed charge and discharge power channel id.
	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value().asString() //
				+ "|L:" + this.getActivePower().value().asString() //
				+ "|Allowed:"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).value().asStringWithoutUnit() + ";"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).value().asString() //
				+ "|" + this.getGridMode().value().asOptionString();
	}

	@Override
	public int getPowerPrecision() {
		// TODO Auto-generated method stub
		return 0;
	}

}
