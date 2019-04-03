package io.openems.edge.fenecon.pro.ess;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Fenecon.Pro.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE })

public class FeneconProEss extends AbstractOpenemsModbusComponent implements SymmetricEss, AsymmetricEss,
		ManagedAsymmetricEss, ManagedSymmetricEss, OpenemsComponent, ModbusSlave, EventHandler {

	private final Logger log = LoggerFactory.getLogger(FeneconProEss.class);

	protected final static int MAX_APPARENT_POWER = 9000;
	private final static int UNIT_ID = 4;

	private String modbusBridgeId;

	@Reference
	private Power power;

	@Reference
	protected ConfigurationAdmin cm;

	public FeneconProEss() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedAsymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				ProChannelId.values() //
		);
		this.channel(SymmetricEss.ChannelId.MAX_APPARENT_POWER).setNextValue(FeneconProEss.MAX_APPARENT_POWER);
		AsymmetricEss.initializePowerSumChannels(this);
	}

	@Override
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) throws OpenemsNamedException {
		this.getSetActivePowerL1Channel().setNextWriteValue(activePowerL1);
		this.getSetActivePowerL2Channel().setNextWriteValue(activePowerL2);
		this.getSetActivePowerL3Channel().setNextWriteValue(activePowerL3);
		this.getSetReactivePowerL1Channel().setNextWriteValue(reactivePowerL1);
		this.getSetReactivePowerL2Channel().setNextWriteValue(reactivePowerL2);
		this.getSetReactivePowerL3Channel().setNextWriteValue(reactivePowerL3);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled(), UNIT_ID, this.cm, "Modbus", config.modbus_id());
		this.modbusBridgeId = config.modbus_id();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public String getModbusBridgeId() {
		return modbusBridgeId;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(100, Priority.HIGH, //
						m(ProChannelId.SYSTEM_STATE, new UnsignedWordElement(100)), //
						m(ProChannelId.CONTROL_MODE, new UnsignedWordElement(101)), //
						m(ProChannelId.WORK_MODE, new UnsignedWordElement(102)), //
						new DummyRegisterElement(103), //
						m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY, new UnsignedDoublewordElement(104)), //
						m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, new UnsignedDoublewordElement(106)), //
						m(ProChannelId.BATTERY_GROUP_STATE, new UnsignedWordElement(108)), //
						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(109)), //
						m(ProChannelId.BATTERY_VOLTAGE, new UnsignedWordElement(110),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ProChannelId.BATTERY_CURRENT, new SignedWordElement(111),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ProChannelId.BATTERY_POWER, new SignedWordElement(112)), //
						bm(new UnsignedWordElement(113))//
								.m(ProChannelId.STATE_0, 0) //
								.m(ProChannelId.STATE_1, 1) //
								.m(ProChannelId.STATE_2, 2) //
								.m(ProChannelId.STATE_3, 3) //
								.m(ProChannelId.STATE_4, 4) //
								.m(ProChannelId.STATE_5, 5) //
								.m(ProChannelId.STATE_6, 6) //
								.build(), //
						m(ProChannelId.PCS_OPERATION_STATE, new UnsignedWordElement(114)), //
						new DummyRegisterElement(115, 117), //
						m(ProChannelId.CURRENT_L1, new SignedWordElement(118),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ProChannelId.CURRENT_L2, new SignedWordElement(119),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ProChannelId.CURRENT_L3, new SignedWordElement(120),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ProChannelId.VOLTAGE_L1, new UnsignedWordElement(121),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ProChannelId.VOLTAGE_L2, new UnsignedWordElement(122),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ProChannelId.VOLTAGE_L3, new UnsignedWordElement(123),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L1, new SignedWordElement(124)), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L2, new SignedWordElement(125)), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L3, new SignedWordElement(126)), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L1, new SignedWordElement(127)), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L2, new SignedWordElement(128)), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L3, new SignedWordElement(129)), //
						new DummyRegisterElement(130), //
						m(ProChannelId.FREQUENCY_L1, new UnsignedWordElement(131),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ProChannelId.FREQUENCY_L2, new UnsignedWordElement(132),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ProChannelId.FREQUENCY_L3, new UnsignedWordElement(133),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ProChannelId.SINGLE_PHASE_ALLOWED_APPARENT, new UnsignedWordElement(134)), //
						new DummyRegisterElement(135, 140), //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, new UnsignedWordElement(141),
								ElementToChannelConverter.INVERT), //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, new UnsignedWordElement(142)), //
						new DummyRegisterElement(143, 149)), //
				new FC3ReadRegistersTask(150, Priority.LOW, //
						bm(new UnsignedWordElement(150))//
								.m(ProChannelId.STATE_7, 0)//
								.m(ProChannelId.STATE_8, 1)//
								.m(ProChannelId.STATE_9, 2)//
								.m(ProChannelId.STATE_10, 3)//
								.m(ProChannelId.STATE_11, 4)//
								.m(ProChannelId.STATE_12, 5)//
								.m(ProChannelId.STATE_13, 6)//
								.m(ProChannelId.STATE_14, 7)//
								.m(ProChannelId.STATE_15, 8)//
								.m(ProChannelId.STATE_16, 9)//
								.m(ProChannelId.STATE_17, 10)//
								.build(), //
						bm(new UnsignedWordElement(151))//
								.m(ProChannelId.STATE_18, 0)//
								.build(), //

						bm(new UnsignedWordElement(152))//
								.m(ProChannelId.STATE_19, 0)//
								.m(ProChannelId.STATE_20, 1)//
								.m(ProChannelId.STATE_21, 2)//
								.m(ProChannelId.STATE_22, 3)//
								.m(ProChannelId.STATE_23, 4)//
								.m(ProChannelId.STATE_24, 5)//
								.m(ProChannelId.STATE_25, 6)//
								.m(ProChannelId.STATE_26, 7)//
								.m(ProChannelId.STATE_27, 8)//
								.m(ProChannelId.STATE_28, 9)//
								.m(ProChannelId.STATE_29, 10)//
								.m(ProChannelId.STATE_30, 11)//
								.m(ProChannelId.STATE_31, 12)//
								.m(ProChannelId.STATE_32, 13)//
								.m(ProChannelId.STATE_33, 14)//
								.m(ProChannelId.STATE_34, 15)//
								.build(), //

						bm(new UnsignedWordElement(153))//
								.m(ProChannelId.STATE_35, 0)//
								.m(ProChannelId.STATE_36, 1)//
								.m(ProChannelId.STATE_37, 2)//
								.m(ProChannelId.STATE_38, 3)//
								.m(ProChannelId.STATE_39, 4)//
								.m(ProChannelId.STATE_40, 5)//
								.m(ProChannelId.STATE_41, 6)//
								.m(ProChannelId.STATE_42, 7)//
								.m(ProChannelId.STATE_43, 8)//
								.m(ProChannelId.STATE_44, 9)//
								.m(ProChannelId.STATE_45, 10)//
								.m(ProChannelId.STATE_46, 11)//
								.m(ProChannelId.STATE_47, 12)//
								.m(ProChannelId.STATE_48, 13)//
								.m(ProChannelId.STATE_49, 14)//
								.m(ProChannelId.STATE_50, 15)//
								.build(), //
						bm(new UnsignedWordElement(154))//
								.m(ProChannelId.STATE_51, 0)//
								.m(ProChannelId.STATE_52, 1)//
								.m(ProChannelId.STATE_53, 2)//
								.m(ProChannelId.STATE_54, 3)//
								.m(ProChannelId.STATE_55, 4)//
								.m(ProChannelId.STATE_56, 5)//
								.m(ProChannelId.STATE_57, 6)//
								.m(ProChannelId.STATE_58, 7)//
								.m(ProChannelId.STATE_59, 8)//
								.m(ProChannelId.STATE_60, 9)//
								.m(ProChannelId.STATE_61, 10)//
								.m(ProChannelId.STATE_62, 11)//
								.m(ProChannelId.STATE_63, 12)//
								.build(), //
						bm(new UnsignedWordElement(155))//
								.m(ProChannelId.STATE_64, 0)//
								.m(ProChannelId.STATE_65, 1)//
								.m(ProChannelId.STATE_66, 2)//
								.m(ProChannelId.STATE_67, 3)//
								.m(ProChannelId.STATE_68, 4)//
								.m(ProChannelId.STATE_69, 5)//
								.m(ProChannelId.STATE_70, 6)//
								.m(ProChannelId.STATE_71, 7)//
								.m(ProChannelId.STATE_72, 8)//
								.m(ProChannelId.STATE_73, 9)//
								.m(ProChannelId.STATE_74, 10)//
								.build(), //
						bm(new UnsignedWordElement(156))//
								.m(ProChannelId.STATE_75, 0)//
								.build(), //
						bm(new UnsignedWordElement(157))//
								.m(ProChannelId.STATE_76, 0)//
								.m(ProChannelId.STATE_77, 1)//
								.m(ProChannelId.STATE_78, 2)//
								.m(ProChannelId.STATE_79, 3)//
								.m(ProChannelId.STATE_80, 4)//
								.m(ProChannelId.STATE_81, 5)//
								.m(ProChannelId.STATE_82, 6)//
								.m(ProChannelId.STATE_83, 7)//
								.m(ProChannelId.STATE_84, 8)//
								.m(ProChannelId.STATE_85, 9)//
								.m(ProChannelId.STATE_86, 10)//
								.m(ProChannelId.STATE_87, 11)//
								.m(ProChannelId.STATE_88, 12)//
								.m(ProChannelId.STATE_89, 13)//
								.m(ProChannelId.STATE_90, 14)//
								.m(ProChannelId.STATE_91, 15)//
								.build(), //
						bm(new UnsignedWordElement(158))//
								.m(ProChannelId.STATE_92, 0)//
								.m(ProChannelId.STATE_93, 1)//
								.m(ProChannelId.STATE_94, 2)//
								.m(ProChannelId.STATE_95, 3)//
								.m(ProChannelId.STATE_96, 4)//
								.m(ProChannelId.STATE_97, 5)//
								.m(ProChannelId.STATE_98, 6)//
								.m(ProChannelId.STATE_99, 7)//
								.m(ProChannelId.STATE_100, 8)//
								.m(ProChannelId.STATE_101, 9)//
								.m(ProChannelId.STATE_102, 10)//
								.m(ProChannelId.STATE_103, 11)//
								.m(ProChannelId.STATE_104, 12)//
								.m(ProChannelId.STATE_105, 13)//
								.m(ProChannelId.STATE_106, 14)//
								.m(ProChannelId.STATE_107, 15)//
								.build(), //
						bm(new UnsignedWordElement(159))//
								.m(ProChannelId.STATE_108, 0)//
								.m(ProChannelId.STATE_109, 1)//
								.m(ProChannelId.STATE_110, 2)//
								.m(ProChannelId.STATE_111, 3)//
								.m(ProChannelId.STATE_112, 4)//
								.m(ProChannelId.STATE_113, 5)//
								.m(ProChannelId.STATE_114, 6)//
								.m(ProChannelId.STATE_115, 7)//
								.m(ProChannelId.STATE_116, 8)//
								.m(ProChannelId.STATE_117, 9)//
								.m(ProChannelId.STATE_118, 10)//
								.m(ProChannelId.STATE_119, 11)//
								.m(ProChannelId.STATE_120, 12)//
								.build(), //
						bm(new UnsignedWordElement(160))//
								.m(ProChannelId.STATE_121, 0)//
								.m(ProChannelId.STATE_122, 1)//
								.m(ProChannelId.STATE_123, 2)//
								.m(ProChannelId.STATE_124, 3)//
								.m(ProChannelId.STATE_125, 4)//
								.m(ProChannelId.STATE_126, 5)//
								.m(ProChannelId.STATE_127, 6)//
								.m(ProChannelId.STATE_128, 7)//
								.m(ProChannelId.STATE_129, 8)//
								.m(ProChannelId.STATE_130, 9)//
								.m(ProChannelId.STATE_131, 10)//
								.build(), //
						bm(new UnsignedWordElement(161))//
								.m(ProChannelId.STATE_132, 0)//
								.build(), //
						bm(new UnsignedWordElement(162))//
								.m(ProChannelId.STATE_133, 0)//
								.m(ProChannelId.STATE_134, 1)//
								.m(ProChannelId.STATE_135, 2)//
								.m(ProChannelId.STATE_136, 3)//
								.m(ProChannelId.STATE_137, 4)//
								.m(ProChannelId.STATE_138, 5)//
								.m(ProChannelId.STATE_139, 6)//
								.m(ProChannelId.STATE_140, 7)//
								.m(ProChannelId.STATE_141, 8)//
								.m(ProChannelId.STATE_142, 9)//
								.m(ProChannelId.STATE_143, 10)//
								.m(ProChannelId.STATE_144, 11)//
								.m(ProChannelId.STATE_145, 12)//
								.m(ProChannelId.STATE_146, 13)//
								.m(ProChannelId.STATE_147, 14)//
								.m(ProChannelId.STATE_148, 15)//
								.build(), //
						bm(new UnsignedWordElement(163))//
								.m(ProChannelId.STATE_149, 0)//
								.m(ProChannelId.STATE_150, 1)//
								.m(ProChannelId.STATE_151, 2)//
								.m(ProChannelId.STATE_152, 3)//
								.m(ProChannelId.STATE_153, 4)//
								.m(ProChannelId.STATE_154, 5)//
								.m(ProChannelId.STATE_155, 6)//
								.m(ProChannelId.STATE_156, 7)//
								.m(ProChannelId.STATE_157, 8)//
								.m(ProChannelId.STATE_158, 9)//
								.m(ProChannelId.STATE_159, 10)//
								.m(ProChannelId.STATE_160, 11)//
								.m(ProChannelId.STATE_161, 12)//
								.m(ProChannelId.STATE_162, 13)//
								.m(ProChannelId.STATE_163, 14)//
								.m(ProChannelId.STATE_164, 15)//
								.build(), //
						bm(new UnsignedWordElement(164))//
								.m(ProChannelId.STATE_165, 0)//
								.m(ProChannelId.STATE_166, 1)//
								.m(ProChannelId.STATE_167, 2)//
								.m(ProChannelId.STATE_168, 3)//
								.m(ProChannelId.STATE_169, 4)//
								.m(ProChannelId.STATE_170, 5)//
								.m(ProChannelId.STATE_171, 6)//
								.m(ProChannelId.STATE_172, 7)//
								.m(ProChannelId.STATE_173, 8)//
								.m(ProChannelId.STATE_174, 9)//
								.m(ProChannelId.STATE_175, 10)//
								.m(ProChannelId.STATE_176, 11)//
								.m(ProChannelId.STATE_177, 12)//
								.build()//
				), //
				new FC16WriteRegistersTask(200, //
						m(ProChannelId.SET_WORK_STATE, new UnsignedWordElement(200))), //
				new FC16WriteRegistersTask(206, //
						m(ProChannelId.SET_ACTIVE_POWER_L1, new SignedWordElement(206)), //
						m(ProChannelId.SET_REACTIVE_POWER_L1, new SignedWordElement(207)), //
						m(ProChannelId.SET_ACTIVE_POWER_L2, new SignedWordElement(208)), //
						m(ProChannelId.SET_REACTIVE_POWER_L2, new SignedWordElement(209)), //
						m(ProChannelId.SET_ACTIVE_POWER_L3, new SignedWordElement(210)), //
						m(ProChannelId.SET_REACTIVE_POWER_L3, new SignedWordElement(211))), //

				new FC3ReadRegistersTask(3020, Priority.LOW, //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_1, new UnsignedWordElement(3020)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_2, new UnsignedWordElement(3021)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_3, new UnsignedWordElement(3022)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_4, new UnsignedWordElement(3023)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_5, new UnsignedWordElement(3024)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_6, new UnsignedWordElement(3025)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_7, new UnsignedWordElement(3026)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_8, new UnsignedWordElement(3027)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_9, new UnsignedWordElement(3028)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_10, new UnsignedWordElement(3029)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_11, new UnsignedWordElement(3030)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_12, new UnsignedWordElement(3031)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_13, new UnsignedWordElement(3032)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_14, new UnsignedWordElement(3033)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_15, new UnsignedWordElement(3034)), //
						m(ProChannelId.BATTERY_VOLTAGE_SECTION_16, new UnsignedWordElement(3035)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_1, new UnsignedWordElement(3036)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_2, new UnsignedWordElement(3037)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_3, new UnsignedWordElement(3038)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_4, new UnsignedWordElement(3039)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_5, new UnsignedWordElement(3040)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_6, new UnsignedWordElement(3041)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_7, new UnsignedWordElement(3042)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_8, new UnsignedWordElement(3043)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_9, new UnsignedWordElement(3044)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_10, new UnsignedWordElement(3045)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_11, new UnsignedWordElement(3046)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_12, new UnsignedWordElement(3047)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_13, new UnsignedWordElement(3048)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_14, new UnsignedWordElement(3049)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_15, new UnsignedWordElement(3050)), //
						m(ProChannelId.BATTERY_TEMPERATURE_SECTION_16, new UnsignedWordElement(3051))), //
				new FC16WriteRegistersTask(9014, //
						m(ProChannelId.RTC_YEAR, new UnsignedWordElement(9014)), //
						m(ProChannelId.RTC_MONTH, new UnsignedWordElement(9015)), //
						m(ProChannelId.RTC_DAY, new UnsignedWordElement(9016)), //
						m(ProChannelId.RTC_HOUR, new UnsignedWordElement(9017)), //
						m(ProChannelId.RTC_MINUTE, new UnsignedWordElement(9018)), //
						m(ProChannelId.RTC_SECOND, new UnsignedWordElement(9019))), //
				new FC16WriteRegistersTask(30558, //
						m(ProChannelId.SETUP_MODE, new UnsignedWordElement(30558))), //
				new FC16WriteRegistersTask(30559, //
						m(ProChannelId.PCS_MODE, new UnsignedWordElement(30559))), //
				new FC3ReadRegistersTask(30157, Priority.LOW, //
						m(ProChannelId.SETUP_MODE, new UnsignedWordElement(30157)), //
						m(ProChannelId.PCS_MODE, new UnsignedWordElement(30158)))//

		);
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value().asString() //
				+ "|L:" + this.getActivePower().value().asString() //
				+ "|Allowed:" + this.getAllowedCharge().value().asStringWithoutUnit() + ";"
				+ this.getAllowedDischarge().value().asString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	private IntegerWriteChannel getSetActivePowerL1Channel() {
		return this.channel(ProChannelId.SET_ACTIVE_POWER_L1);
	}

	private IntegerWriteChannel getSetActivePowerL2Channel() {
		return this.channel(ProChannelId.SET_ACTIVE_POWER_L2);
	}

	private IntegerWriteChannel getSetActivePowerL3Channel() {
		return this.channel(ProChannelId.SET_ACTIVE_POWER_L3);
	}

	private IntegerWriteChannel getSetReactivePowerL1Channel() {
		return this.channel(ProChannelId.SET_REACTIVE_POWER_L1);
	}

	private IntegerWriteChannel getSetReactivePowerL2Channel() {
		return this.channel(ProChannelId.SET_REACTIVE_POWER_L2);
	}

	private IntegerWriteChannel getSetReactivePowerL3Channel() {
		return this.channel(ProChannelId.SET_REACTIVE_POWER_L3);
	}

	private EnumWriteChannel getPcsModeChannel() {
		return this.channel(ProChannelId.PCS_MODE);
	}

	private PcsMode getPcsMode() {
		return this.getPcsModeChannel().value().asEnum();
	}

	private EnumWriteChannel getSetupModeChannel() {
		return this.channel(ProChannelId.SETUP_MODE);
	}

	private SetupMode getSetupMode() {
		return this.getSetupModeChannel().value().asEnum();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.activateRemoteMode();
		}
	}

	/**
	 * Activates the Remote-Mode
	 */
	private void activateRemoteMode() {
		try {
			if (this.getPcsMode() != PcsMode.REMOTE) {
				// If Mode is not "Remote"
				this.logWarn(log, "PCS-Mode is not 'Remote'. It's [" + this.getPcsMode() + "]");
				if (this.getSetupMode() == SetupMode.OFF) {
					// Activate SetupMode
					this.logInfo(log, "Activating Setup-Mode");
					this.getSetupModeChannel().setNextWriteValue(SetupMode.ON);
				} else {
					// Set Mode to "Remote"
					this.logInfo(log, "Setting PCS-Mode to 'Remote'");
					this.getPcsModeChannel().setNextWriteValue(PcsMode.REMOTE);
				}
			} else {
				// If Mode is "Remote" and SetupMode is active
				if (this.getSetupMode() == SetupMode.ON) {
					// Deactivate SetupMode
					this.logInfo(log, "Deactivating Setup-Mode");
					this.getSetupModeChannel().setNextWriteValue(SetupMode.OFF);
				}
			}
		} catch (OpenemsNamedException e) {
			this.logError(log, "Unable to activate Remote-Mode: " + e.getMessage());
		}
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable() {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(), //
				SymmetricEss.getModbusSlaveNatureTable(), //
				AsymmetricEss.getModbusSlaveNatureTable(), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(), //
				ManagedAsymmetricEss.getModbusSlaveNatureTable(), //
				ModbusSlaveNatureTable.of(FeneconProEss.class, 300) //
						.build());
	}

}
