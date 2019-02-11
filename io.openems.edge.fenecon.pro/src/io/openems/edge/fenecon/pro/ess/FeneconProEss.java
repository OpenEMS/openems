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

import io.openems.common.exceptions.OpenemsException;
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
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.GridMode;
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
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));

		AsymmetricEss.initializePowerSumChannels(this);
	}

	@Override
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) {
		try {
			this.getSetActivePowerL1Channel().setNextWriteValue(activePowerL1);
			this.getSetActivePowerL2Channel().setNextWriteValue(activePowerL2);
			this.getSetActivePowerL3Channel().setNextWriteValue(activePowerL3);
		} catch (OpenemsException e) {
			log.error("Unable to set ActivePower: " + e.getMessage());
		}
		try {
			this.getSetReactivePowerL1Channel().setNextWriteValue(reactivePowerL1);
			this.getSetReactivePowerL2Channel().setNextWriteValue(reactivePowerL2);
			this.getSetReactivePowerL3Channel().setNextWriteValue(reactivePowerL3);
		} catch (OpenemsException e) {
			log.error("Unable to set ReactivePower: " + e.getMessage());
		}
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled(), UNIT_ID, this.cm, "Modbus",
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

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(100, Priority.HIGH, //
						m(FeneconProEss.ChannelId.SYSTEM_STATE, new UnsignedWordElement(100)), //
						m(FeneconProEss.ChannelId.CONTROL_MODE, new UnsignedWordElement(101)), //
						m(FeneconProEss.ChannelId.WORK_MODE, new UnsignedWordElement(102)), //
						new DummyRegisterElement(103), //
						m(FeneconProEss.ChannelId.TOTAL_BATTERY_CHARGE_ENERGY, new UnsignedDoublewordElement(104)), //
						m(FeneconProEss.ChannelId.TOTAL_BATTERY_DISCHARGE_ENERGY, new UnsignedDoublewordElement(106)), //
						m(FeneconProEss.ChannelId.BATTERY_GROUP_STATE, new UnsignedWordElement(108)), //
						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(109)), //
						m(FeneconProEss.ChannelId.BATTERY_VOLTAGE, new UnsignedWordElement(110),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(FeneconProEss.ChannelId.BATTERY_CURRENT, new SignedWordElement(111),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(FeneconProEss.ChannelId.BATTERY_POWER, new SignedWordElement(112)), //
						bm(new UnsignedWordElement(113))//
								.m(FeneconProEss.ChannelId.STATE_0, 0) //
								.m(FeneconProEss.ChannelId.STATE_1, 1) //
								.m(FeneconProEss.ChannelId.STATE_2, 2) //
								.m(FeneconProEss.ChannelId.STATE_3, 3) //
								.m(FeneconProEss.ChannelId.STATE_4, 4) //
								.m(FeneconProEss.ChannelId.STATE_5, 5) //
								.m(FeneconProEss.ChannelId.STATE_6, 6) //
								.build(), //
						m(FeneconProEss.ChannelId.PCS_OPERATION_STATE, new UnsignedWordElement(114)), //
						new DummyRegisterElement(115, 117), //
						m(FeneconProEss.ChannelId.CURRENT_L1, new SignedWordElement(118),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(FeneconProEss.ChannelId.CURRENT_L2, new SignedWordElement(119),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(FeneconProEss.ChannelId.CURRENT_L3, new SignedWordElement(120),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(FeneconProEss.ChannelId.VOLTAGE_L1, new UnsignedWordElement(121),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(FeneconProEss.ChannelId.VOLTAGE_L2, new UnsignedWordElement(122),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(FeneconProEss.ChannelId.VOLTAGE_L3, new UnsignedWordElement(123),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L1, new SignedWordElement(124)), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L2, new SignedWordElement(125)), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L3, new SignedWordElement(126)), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L1, new SignedWordElement(127)), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L2, new SignedWordElement(128)), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L3, new SignedWordElement(129)), //
						new DummyRegisterElement(130), //
						m(FeneconProEss.ChannelId.FREQUENCY_L1, new UnsignedWordElement(131),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(FeneconProEss.ChannelId.FREQUENCY_L2, new UnsignedWordElement(132),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(FeneconProEss.ChannelId.FREQUENCY_L3, new UnsignedWordElement(133),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(FeneconProEss.ChannelId.SINGLE_PHASE_ALLOWED_APPARENT, new UnsignedWordElement(134)), //
						new DummyRegisterElement(135, 140), //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, new UnsignedWordElement(141),
								ElementToChannelConverter.INVERT), //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, new UnsignedWordElement(142)), //
						new DummyRegisterElement(143, 149)), //
				new FC3ReadRegistersTask(150, Priority.LOW, //
						bm(new UnsignedWordElement(150))//
								.m(FeneconProEss.ChannelId.STATE_7, 0)//
								.m(FeneconProEss.ChannelId.STATE_8, 1)//
								.m(FeneconProEss.ChannelId.STATE_9, 2)//
								.m(FeneconProEss.ChannelId.STATE_10, 3)//
								.m(FeneconProEss.ChannelId.STATE_11, 4)//
								.m(FeneconProEss.ChannelId.STATE_12, 5)//
								.m(FeneconProEss.ChannelId.STATE_13, 6)//
								.m(FeneconProEss.ChannelId.STATE_14, 7)//
								.m(FeneconProEss.ChannelId.STATE_15, 8)//
								.m(FeneconProEss.ChannelId.STATE_16, 9)//
								.m(FeneconProEss.ChannelId.STATE_17, 10)//
								.build(), //
						bm(new UnsignedWordElement(151))//
								.m(FeneconProEss.ChannelId.STATE_18, 0)//
								.build(), //

						bm(new UnsignedWordElement(152))//
								.m(FeneconProEss.ChannelId.STATE_19, 0)//
								.m(FeneconProEss.ChannelId.STATE_20, 1)//
								.m(FeneconProEss.ChannelId.STATE_21, 2)//
								.m(FeneconProEss.ChannelId.STATE_22, 3)//
								.m(FeneconProEss.ChannelId.STATE_23, 4)//
								.m(FeneconProEss.ChannelId.STATE_24, 5)//
								.m(FeneconProEss.ChannelId.STATE_25, 6)//
								.m(FeneconProEss.ChannelId.STATE_26, 7)//
								.m(FeneconProEss.ChannelId.STATE_27, 8)//
								.m(FeneconProEss.ChannelId.STATE_28, 9)//
								.m(FeneconProEss.ChannelId.STATE_29, 10)//
								.m(FeneconProEss.ChannelId.STATE_30, 11)//
								.m(FeneconProEss.ChannelId.STATE_31, 12)//
								.m(FeneconProEss.ChannelId.STATE_32, 13)//
								.m(FeneconProEss.ChannelId.STATE_33, 14)//
								.m(FeneconProEss.ChannelId.STATE_34, 15)//
								.build(), //

						bm(new UnsignedWordElement(153))//
								.m(FeneconProEss.ChannelId.STATE_35, 0)//
								.m(FeneconProEss.ChannelId.STATE_36, 1)//
								.m(FeneconProEss.ChannelId.STATE_37, 2)//
								.m(FeneconProEss.ChannelId.STATE_38, 3)//
								.m(FeneconProEss.ChannelId.STATE_39, 4)//
								.m(FeneconProEss.ChannelId.STATE_40, 5)//
								.m(FeneconProEss.ChannelId.STATE_41, 6)//
								.m(FeneconProEss.ChannelId.STATE_42, 7)//
								.m(FeneconProEss.ChannelId.STATE_43, 8)//
								.m(FeneconProEss.ChannelId.STATE_44, 9)//
								.m(FeneconProEss.ChannelId.STATE_45, 10)//
								.m(FeneconProEss.ChannelId.STATE_46, 11)//
								.m(FeneconProEss.ChannelId.STATE_47, 12)//
								.m(FeneconProEss.ChannelId.STATE_48, 13)//
								.m(FeneconProEss.ChannelId.STATE_49, 14)//
								.m(FeneconProEss.ChannelId.STATE_50, 15)//
								.build(), //
						bm(new UnsignedWordElement(154))//
								.m(FeneconProEss.ChannelId.STATE_51, 0)//
								.m(FeneconProEss.ChannelId.STATE_52, 1)//
								.m(FeneconProEss.ChannelId.STATE_53, 2)//
								.m(FeneconProEss.ChannelId.STATE_54, 3)//
								.m(FeneconProEss.ChannelId.STATE_55, 4)//
								.m(FeneconProEss.ChannelId.STATE_56, 5)//
								.m(FeneconProEss.ChannelId.STATE_57, 6)//
								.m(FeneconProEss.ChannelId.STATE_58, 7)//
								.m(FeneconProEss.ChannelId.STATE_59, 8)//
								.m(FeneconProEss.ChannelId.STATE_60, 9)//
								.m(FeneconProEss.ChannelId.STATE_61, 10)//
								.m(FeneconProEss.ChannelId.STATE_62, 11)//
								.m(FeneconProEss.ChannelId.STATE_63, 12)//
								.build(), //
						bm(new UnsignedWordElement(155))//
								.m(FeneconProEss.ChannelId.STATE_64, 0)//
								.m(FeneconProEss.ChannelId.STATE_65, 1)//
								.m(FeneconProEss.ChannelId.STATE_66, 2)//
								.m(FeneconProEss.ChannelId.STATE_67, 3)//
								.m(FeneconProEss.ChannelId.STATE_68, 4)//
								.m(FeneconProEss.ChannelId.STATE_69, 5)//
								.m(FeneconProEss.ChannelId.STATE_70, 6)//
								.m(FeneconProEss.ChannelId.STATE_71, 7)//
								.m(FeneconProEss.ChannelId.STATE_72, 8)//
								.m(FeneconProEss.ChannelId.STATE_73, 9)//
								.m(FeneconProEss.ChannelId.STATE_74, 10)//
								.build(), //
						bm(new UnsignedWordElement(156))//
								.m(FeneconProEss.ChannelId.STATE_75, 0)//
								.build(), //
						bm(new UnsignedWordElement(157))//
								.m(FeneconProEss.ChannelId.STATE_76, 0)//
								.m(FeneconProEss.ChannelId.STATE_77, 1)//
								.m(FeneconProEss.ChannelId.STATE_78, 2)//
								.m(FeneconProEss.ChannelId.STATE_79, 3)//
								.m(FeneconProEss.ChannelId.STATE_80, 4)//
								.m(FeneconProEss.ChannelId.STATE_81, 5)//
								.m(FeneconProEss.ChannelId.STATE_82, 6)//
								.m(FeneconProEss.ChannelId.STATE_83, 7)//
								.m(FeneconProEss.ChannelId.STATE_84, 8)//
								.m(FeneconProEss.ChannelId.STATE_85, 9)//
								.m(FeneconProEss.ChannelId.STATE_86, 10)//
								.m(FeneconProEss.ChannelId.STATE_87, 11)//
								.m(FeneconProEss.ChannelId.STATE_88, 12)//
								.m(FeneconProEss.ChannelId.STATE_89, 13)//
								.m(FeneconProEss.ChannelId.STATE_90, 14)//
								.m(FeneconProEss.ChannelId.STATE_91, 15)//
								.build(), //
						bm(new UnsignedWordElement(158))//
								.m(FeneconProEss.ChannelId.STATE_92, 0)//
								.m(FeneconProEss.ChannelId.STATE_93, 1)//
								.m(FeneconProEss.ChannelId.STATE_94, 2)//
								.m(FeneconProEss.ChannelId.STATE_95, 3)//
								.m(FeneconProEss.ChannelId.STATE_96, 4)//
								.m(FeneconProEss.ChannelId.STATE_97, 5)//
								.m(FeneconProEss.ChannelId.STATE_98, 6)//
								.m(FeneconProEss.ChannelId.STATE_99, 7)//
								.m(FeneconProEss.ChannelId.STATE_100, 8)//
								.m(FeneconProEss.ChannelId.STATE_101, 9)//
								.m(FeneconProEss.ChannelId.STATE_102, 10)//
								.m(FeneconProEss.ChannelId.STATE_103, 11)//
								.m(FeneconProEss.ChannelId.STATE_104, 12)//
								.m(FeneconProEss.ChannelId.STATE_105, 13)//
								.m(FeneconProEss.ChannelId.STATE_106, 14)//
								.m(FeneconProEss.ChannelId.STATE_107, 15)//
								.build(), //
						bm(new UnsignedWordElement(159))//
								.m(FeneconProEss.ChannelId.STATE_108, 0)//
								.m(FeneconProEss.ChannelId.STATE_109, 1)//
								.m(FeneconProEss.ChannelId.STATE_110, 2)//
								.m(FeneconProEss.ChannelId.STATE_111, 3)//
								.m(FeneconProEss.ChannelId.STATE_112, 4)//
								.m(FeneconProEss.ChannelId.STATE_113, 5)//
								.m(FeneconProEss.ChannelId.STATE_114, 6)//
								.m(FeneconProEss.ChannelId.STATE_115, 7)//
								.m(FeneconProEss.ChannelId.STATE_116, 8)//
								.m(FeneconProEss.ChannelId.STATE_117, 9)//
								.m(FeneconProEss.ChannelId.STATE_118, 10)//
								.m(FeneconProEss.ChannelId.STATE_119, 11)//
								.m(FeneconProEss.ChannelId.STATE_120, 12)//
								.build(), //
						bm(new UnsignedWordElement(160))//
								.m(FeneconProEss.ChannelId.STATE_121, 0)//
								.m(FeneconProEss.ChannelId.STATE_122, 1)//
								.m(FeneconProEss.ChannelId.STATE_123, 2)//
								.m(FeneconProEss.ChannelId.STATE_124, 3)//
								.m(FeneconProEss.ChannelId.STATE_125, 4)//
								.m(FeneconProEss.ChannelId.STATE_126, 5)//
								.m(FeneconProEss.ChannelId.STATE_127, 6)//
								.m(FeneconProEss.ChannelId.STATE_128, 7)//
								.m(FeneconProEss.ChannelId.STATE_129, 8)//
								.m(FeneconProEss.ChannelId.STATE_130, 9)//
								.m(FeneconProEss.ChannelId.STATE_131, 10)//
								.build(), //
						bm(new UnsignedWordElement(161))//
								.m(FeneconProEss.ChannelId.STATE_132, 0)//
								.build(), //
						bm(new UnsignedWordElement(162))//
								.m(FeneconProEss.ChannelId.STATE_133, 0)//
								.m(FeneconProEss.ChannelId.STATE_134, 1)//
								.m(FeneconProEss.ChannelId.STATE_135, 2)//
								.m(FeneconProEss.ChannelId.STATE_136, 3)//
								.m(FeneconProEss.ChannelId.STATE_137, 4)//
								.m(FeneconProEss.ChannelId.STATE_138, 5)//
								.m(FeneconProEss.ChannelId.STATE_139, 6)//
								.m(FeneconProEss.ChannelId.STATE_140, 7)//
								.m(FeneconProEss.ChannelId.STATE_141, 8)//
								.m(FeneconProEss.ChannelId.STATE_142, 9)//
								.m(FeneconProEss.ChannelId.STATE_143, 10)//
								.m(FeneconProEss.ChannelId.STATE_144, 11)//
								.m(FeneconProEss.ChannelId.STATE_145, 12)//
								.m(FeneconProEss.ChannelId.STATE_146, 13)//
								.m(FeneconProEss.ChannelId.STATE_147, 14)//
								.m(FeneconProEss.ChannelId.STATE_148, 15)//
								.build(), //
						bm(new UnsignedWordElement(163))//
								.m(FeneconProEss.ChannelId.STATE_149, 0)//
								.m(FeneconProEss.ChannelId.STATE_150, 1)//
								.m(FeneconProEss.ChannelId.STATE_151, 2)//
								.m(FeneconProEss.ChannelId.STATE_152, 3)//
								.m(FeneconProEss.ChannelId.STATE_153, 4)//
								.m(FeneconProEss.ChannelId.STATE_154, 5)//
								.m(FeneconProEss.ChannelId.STATE_155, 6)//
								.m(FeneconProEss.ChannelId.STATE_156, 7)//
								.m(FeneconProEss.ChannelId.STATE_157, 8)//
								.m(FeneconProEss.ChannelId.STATE_158, 9)//
								.m(FeneconProEss.ChannelId.STATE_159, 10)//
								.m(FeneconProEss.ChannelId.STATE_160, 11)//
								.m(FeneconProEss.ChannelId.STATE_161, 12)//
								.m(FeneconProEss.ChannelId.STATE_162, 13)//
								.m(FeneconProEss.ChannelId.STATE_163, 14)//
								.m(FeneconProEss.ChannelId.STATE_164, 15)//
								.build(), //
						bm(new UnsignedWordElement(164))//
								.m(FeneconProEss.ChannelId.STATE_165, 0)//
								.m(FeneconProEss.ChannelId.STATE_166, 1)//
								.m(FeneconProEss.ChannelId.STATE_167, 2)//
								.m(FeneconProEss.ChannelId.STATE_168, 3)//
								.m(FeneconProEss.ChannelId.STATE_169, 4)//
								.m(FeneconProEss.ChannelId.STATE_170, 5)//
								.m(FeneconProEss.ChannelId.STATE_171, 6)//
								.m(FeneconProEss.ChannelId.STATE_172, 7)//
								.m(FeneconProEss.ChannelId.STATE_173, 8)//
								.m(FeneconProEss.ChannelId.STATE_174, 9)//
								.m(FeneconProEss.ChannelId.STATE_175, 10)//
								.m(FeneconProEss.ChannelId.STATE_176, 11)//
								.m(FeneconProEss.ChannelId.STATE_177, 12)//
								.build()//
				), //
				new FC16WriteRegistersTask(200, //
						m(FeneconProEss.ChannelId.SET_WORK_STATE, new UnsignedWordElement(200))), //
				new FC16WriteRegistersTask(206, //
						m(FeneconProEss.ChannelId.SET_ACTIVE_POWER_L1, new SignedWordElement(206)), //
						m(FeneconProEss.ChannelId.SET_REACTIVE_POWER_L1, new SignedWordElement(207)), //
						m(FeneconProEss.ChannelId.SET_ACTIVE_POWER_L2, new SignedWordElement(208)), //
						m(FeneconProEss.ChannelId.SET_REACTIVE_POWER_L2, new SignedWordElement(209)), //
						m(FeneconProEss.ChannelId.SET_ACTIVE_POWER_L3, new SignedWordElement(210)), //
						m(FeneconProEss.ChannelId.SET_REACTIVE_POWER_L3, new SignedWordElement(211))), //

				new FC3ReadRegistersTask(3020, Priority.LOW, //
						m(FeneconProEss.ChannelId.BATTERY_VOLTAGE_SECTION_1, new UnsignedWordElement(3020)), //
						m(FeneconProEss.ChannelId.BATTERY_VOLTAGE_SECTION_2, new UnsignedWordElement(3021)), //
						m(FeneconProEss.ChannelId.BATTERY_VOLTAGE_SECTION_3, new UnsignedWordElement(3022)), //
						m(FeneconProEss.ChannelId.BATTERY_VOLTAGE_SECTION_4, new UnsignedWordElement(3023)), //
						m(FeneconProEss.ChannelId.BATTERY_VOLTAGE_SECTION_5, new UnsignedWordElement(3024)), //
						m(FeneconProEss.ChannelId.BATTERY_VOLTAGE_SECTION_6, new UnsignedWordElement(3025)), //
						m(FeneconProEss.ChannelId.BATTERY_VOLTAGE_SECTION_7, new UnsignedWordElement(3026)), //
						m(FeneconProEss.ChannelId.BATTERY_VOLTAGE_SECTION_8, new UnsignedWordElement(3027)), //
						m(FeneconProEss.ChannelId.BATTERY_VOLTAGE_SECTION_9, new UnsignedWordElement(3028)), //
						m(FeneconProEss.ChannelId.BATTERY_VOLTAGE_SECTION_10, new UnsignedWordElement(3029)), //
						m(FeneconProEss.ChannelId.BATTERY_VOLTAGE_SECTION_11, new UnsignedWordElement(3030)), //
						m(FeneconProEss.ChannelId.BATTERY_VOLTAGE_SECTION_12, new UnsignedWordElement(3031)), //
						m(FeneconProEss.ChannelId.BATTERY_VOLTAGE_SECTION_13, new UnsignedWordElement(3032)), //
						m(FeneconProEss.ChannelId.BATTERY_VOLTAGE_SECTION_14, new UnsignedWordElement(3033)), //
						m(FeneconProEss.ChannelId.BATTERY_VOLTAGE_SECTION_15, new UnsignedWordElement(3034)), //
						m(FeneconProEss.ChannelId.BATTERY_VOLTAGE_SECTION_16, new UnsignedWordElement(3035)), //
						m(FeneconProEss.ChannelId.BATTERY_TEMPERATURE_SECTION_1, new UnsignedWordElement(3036)), //
						m(FeneconProEss.ChannelId.BATTERY_TEMPERATURE_SECTION_2, new UnsignedWordElement(3037)), //
						m(FeneconProEss.ChannelId.BATTERY_TEMPERATURE_SECTION_3, new UnsignedWordElement(3038)), //
						m(FeneconProEss.ChannelId.BATTERY_TEMPERATURE_SECTION_4, new UnsignedWordElement(3039)), //
						m(FeneconProEss.ChannelId.BATTERY_TEMPERATURE_SECTION_5, new UnsignedWordElement(3040)), //
						m(FeneconProEss.ChannelId.BATTERY_TEMPERATURE_SECTION_6, new UnsignedWordElement(3041)), //
						m(FeneconProEss.ChannelId.BATTERY_TEMPERATURE_SECTION_7, new UnsignedWordElement(3042)), //
						m(FeneconProEss.ChannelId.BATTERY_TEMPERATURE_SECTION_8, new UnsignedWordElement(3043)), //
						m(FeneconProEss.ChannelId.BATTERY_TEMPERATURE_SECTION_9, new UnsignedWordElement(3044)), //
						m(FeneconProEss.ChannelId.BATTERY_TEMPERATURE_SECTION_10, new UnsignedWordElement(3045)), //
						m(FeneconProEss.ChannelId.BATTERY_TEMPERATURE_SECTION_11, new UnsignedWordElement(3046)), //
						m(FeneconProEss.ChannelId.BATTERY_TEMPERATURE_SECTION_12, new UnsignedWordElement(3047)), //
						m(FeneconProEss.ChannelId.BATTERY_TEMPERATURE_SECTION_13, new UnsignedWordElement(3048)), //
						m(FeneconProEss.ChannelId.BATTERY_TEMPERATURE_SECTION_14, new UnsignedWordElement(3049)), //
						m(FeneconProEss.ChannelId.BATTERY_TEMPERATURE_SECTION_15, new UnsignedWordElement(3050)), //
						m(FeneconProEss.ChannelId.BATTERY_TEMPERATURE_SECTION_16, new UnsignedWordElement(3051))), //
				new FC16WriteRegistersTask(9014, //
						m(FeneconProEss.ChannelId.RTC_YEAR, new UnsignedWordElement(9014)), //
						m(FeneconProEss.ChannelId.RTC_MONTH, new UnsignedWordElement(9015)), //
						m(FeneconProEss.ChannelId.RTC_DAY, new UnsignedWordElement(9016)), //
						m(FeneconProEss.ChannelId.RTC_HOUR, new UnsignedWordElement(9017)), //
						m(FeneconProEss.ChannelId.RTC_MINUTE, new UnsignedWordElement(9018)), //
						m(FeneconProEss.ChannelId.RTC_SECOND, new UnsignedWordElement(9019))), //
				new FC16WriteRegistersTask(30558, //
						m(FeneconProEss.ChannelId.SETUP_MODE, new UnsignedWordElement(30558))), //
				new FC16WriteRegistersTask(30559, //
						m(FeneconProEss.ChannelId.PCS_MODE, new UnsignedWordElement(30559))), //
				new FC3ReadRegistersTask(30157, Priority.LOW, //
						m(FeneconProEss.ChannelId.SETUP_MODE, new UnsignedWordElement(30157)), //
						m(FeneconProEss.ChannelId.PCS_MODE, new UnsignedWordElement(30158)))//

		);
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value().asString() //
				+ "|L:" + this.getActivePower().value().asString() //
				+ "|Allowed:" + this.getAllowedCharge().value().asStringWithoutUnit() + ";"
				+ this.getAllowedDischarge().value().asString();
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		SET_WORK_STATE(new Doc().options(SetWorkState.values())), //
		WORK_MODE(new Doc().options(WorkMode.values())), //
		@SuppressWarnings("unchecked")
		SYSTEM_STATE(new Doc() //
				.options(SystemState.values()) //
				.onInit(channel -> { //
					// on each update set Grid-Mode channel
					((Channel<Integer>) channel).onChange(value -> {
						SystemState systemState = value.asEnum();
						Channel<Integer> gridMode = channel.getComponent().channel(SymmetricEss.ChannelId.GRID_MODE);
						switch (systemState) {
						case STANDBY:
						case START:
						case FAULT:
							gridMode.setNextValue(GridMode.ON_GRID);
							break;
						case START_OFF_GRID:
						case OFF_GRID_PV:
							gridMode.setNextValue(GridMode.OFF_GRID);
							break;
						case UNDEFINED:
							gridMode.setNextValue(GridMode.UNDEFINED);
							break;
						}
					});
				})), //
		CONTROL_MODE(new Doc().options(ControlMode.values())),
		TOTAL_BATTERY_CHARGE_ENERGY(new Doc().unit(Unit.WATT_HOURS)), //
		TOTAL_BATTERY_DISCHARGE_ENERGY(new Doc().unit(Unit.WATT_HOURS)), //
		BATTERY_GROUP_STATE(new Doc().options(BatteryGroupState.values())), //
		BATTERY_POWER(new Doc().unit(Unit.WATT)), //
		BATTERY_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
		CURRENT_L1(new Doc().unit(Unit.MILLIAMPERE)), //
		CURRENT_L2(new Doc().unit(Unit.MILLIAMPERE)), //
		CURRENT_L3(new Doc().unit(Unit.MILLIAMPERE)), //
		VOLTAGE_L1(new Doc().unit(Unit.MILLIVOLT)), //
		VOLTAGE_L2(new Doc().unit(Unit.MILLIVOLT)), //
		VOLTAGE_L3(new Doc().unit(Unit.MILLIVOLT)), //
		FREQUENCY_L1(new Doc().unit(Unit.MILLIHERTZ)), //
		FREQUENCY_L2(new Doc().unit(Unit.MILLIHERTZ)), //
		FREQUENCY_L3(new Doc().unit(Unit.MILLIHERTZ)), //
		@SuppressWarnings("unchecked")
		SINGLE_PHASE_ALLOWED_APPARENT(new Doc().unit(Unit.VOLT_AMPERE) //
				.onInit(channel -> { //
					// on each update -> update MaxApparentPower to 3 x Single Phase Apparent Power
					((Channel<Integer>) channel).onChange(value -> {
						channel.getComponent().channel(SymmetricEss.ChannelId.MAX_APPARENT_POWER)
								.setNextValue(value.orElse(0) * 3);
					});
				})), //
		SET_ACTIVE_POWER_L1(new Doc().unit(Unit.WATT)), //
		SET_ACTIVE_POWER_L2(new Doc().unit(Unit.WATT)), //
		SET_ACTIVE_POWER_L3(new Doc().unit(Unit.WATT)), //
		SET_REACTIVE_POWER_L1(new Doc().unit(Unit.VOLT_AMPERE)), //
		SET_REACTIVE_POWER_L2(new Doc().unit(Unit.VOLT_AMPERE)), //
		SET_REACTIVE_POWER_L3(new Doc().unit(Unit.VOLT_AMPERE)), //

		BATTERY_VOLTAGE_SECTION_1(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION_2(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION_3(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION_4(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION_5(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION_6(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION_7(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION_8(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION_9(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION_10(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION_11(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION_12(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION_13(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION_14(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION_15(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION_16(new Doc().unit(Unit.MILLIVOLT)), //
		// TODO add .delta(-40L)
		BATTERY_TEMPERATURE_SECTION_1(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION_2(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION_3(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION_4(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION_5(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION_6(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION_7(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION_8(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION_9(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION_10(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION_11(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION_12(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION_13(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION_14(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION_15(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION_16(new Doc().unit(Unit.DEGREE_CELSIUS)), //

		PCS_OPERATION_STATE(new Doc().options(PcsOperationState.values())), //
		RTC_YEAR(new Doc().text("Year")), //
		RTC_MONTH(new Doc().text("Month")), //
		RTC_DAY(new Doc().text("Day")), //
		RTC_HOUR(new Doc().text("Hour")), //
		RTC_MINUTE(new Doc().text("Minute")), //
		RTC_SECOND(new Doc().text("Second")), //
		SETUP_MODE(new Doc().options(SetupMode.values())), //
		PCS_MODE(new Doc().options(PcsMode.values())), //

		STATE_0(new Doc().level(Level.WARNING).text("FailTheSystemShouldBeStopped")), //
		STATE_1(new Doc().level(Level.WARNING).text("CommonLowVoltageAlarm")), //
		STATE_2(new Doc().level(Level.WARNING).text("CommonHighVoltageAlarm")), //
		STATE_3(new Doc().level(Level.WARNING).text("ChargingOverCurrentAlarm")), //
		STATE_4(new Doc().level(Level.WARNING).text("DischargingOverCurrentAlarm")), //
		STATE_5(new Doc().level(Level.WARNING).text("OverTemperatureAlarm")), //
		STATE_6(new Doc().level(Level.WARNING).text("InteralCommunicationAbnormal")), //
		STATE_7(new Doc().level(Level.WARNING).text("GridUndervoltageL1")), //
		STATE_8(new Doc().level(Level.WARNING).text("GridOvervoltageL1")), //
		STATE_9(new Doc().level(Level.WARNING).text("GridUnderFrequencyL1")), //
		STATE_10(new Doc().level(Level.WARNING).text("GridOverFrequencyL1")), //
		STATE_11(new Doc().level(Level.WARNING).text("GridPowerSupplyOffL1")), //
		STATE_12(new Doc().level(Level.WARNING).text("GridConditionUnmeetL1")), //
		STATE_13(new Doc().level(Level.WARNING).text("DCUnderVoltageL1")), //
		STATE_14(new Doc().level(Level.WARNING).text("InputOverResistanceL1")), //
		STATE_15(new Doc().level(Level.WARNING).text("CombinationErrorL1")), //
		STATE_16(new Doc().level(Level.WARNING).text("CommWithInverterErrorL1")), //
		STATE_17(new Doc().level(Level.WARNING).text("TmeErrorL1")), //
		STATE_18(new Doc().level(Level.WARNING).text("PcsAlarm2L1")), //
		STATE_19(new Doc().level(Level.FAULT).text("ControlCurrentOverload100PercentL1")), //
		STATE_20(new Doc().level(Level.FAULT).text("ControlCurrentOverload110PercentL1")), //
		STATE_21(new Doc().level(Level.FAULT).text("ControlCurrentOverload150PercentL1")), //
		STATE_22(new Doc().level(Level.FAULT).text("ControlCurrentOverload200PercentL1")), //
		STATE_23(new Doc().level(Level.FAULT).text("ControlCurrentOverload210PercentL1")), //
		STATE_24(new Doc().level(Level.FAULT).text("ControlCurrentOverload300PercentL1")), //
		STATE_25(new Doc().level(Level.FAULT).text("ControlTransientLoad300PercentL1")), //
		STATE_26(new Doc().level(Level.FAULT).text("GridOverCurrentL1")), //
		STATE_27(new Doc().level(Level.FAULT).text("LockingWaveformTooManyTimesL1")), //
		STATE_28(new Doc().level(Level.FAULT).text("InverterVoltageZeroDriftErrorL1")), //
		STATE_29(new Doc().level(Level.FAULT).text("GridVoltageZeroDriftErrorL1")), //
		STATE_30(new Doc().level(Level.FAULT).text("ControlCurrentZeroDriftErrorL1")), //
		STATE_31(new Doc().level(Level.FAULT).text("InverterCurrentZeroDriftErrorL1")), //
		STATE_32(new Doc().level(Level.FAULT).text("GridCurrentZeroDriftErrorL1")), //
		STATE_33(new Doc().level(Level.FAULT).text("PDPProtectionL1")), //
		STATE_34(new Doc().level(Level.FAULT).text("HardwareControlCurrentProtectionL1")), //
		STATE_35(new Doc().level(Level.FAULT).text("HardwareACVoltageProtectionL1")), //
		STATE_36(new Doc().level(Level.FAULT).text("HardwareDCCurrentProtectionL1")), //
		STATE_37(new Doc().level(Level.FAULT).text("HardwareTemperatureProtectionL1")), //
		STATE_38(new Doc().level(Level.FAULT).text("NoCapturingSignalL1")), //
		STATE_39(new Doc().level(Level.FAULT).text("DCOvervoltageL1")), //
		STATE_40(new Doc().level(Level.FAULT).text("DCDisconnectedL1")), //
		STATE_41(new Doc().level(Level.FAULT).text("InverterUndervoltageL1")), //
		STATE_42(new Doc().level(Level.FAULT).text("InverterOvervoltageL1")), //
		STATE_43(new Doc().level(Level.FAULT).text("CurrentSensorFailL1")), //
		STATE_44(new Doc().level(Level.FAULT).text("VoltageSensorFailL1")), //
		STATE_45(new Doc().level(Level.FAULT).text("PowerUncontrollableL1")), //
		STATE_46(new Doc().level(Level.FAULT).text("CurrentUncontrollableL1")), //
		STATE_47(new Doc().level(Level.FAULT).text("FanErrorL1")), //
		STATE_48(new Doc().level(Level.FAULT).text("PhaseLackL1")), //
		STATE_49(new Doc().level(Level.FAULT).text("InverterRelayFaultL1")), //
		STATE_50(new Doc().level(Level.FAULT).text("GridRelayFaultL1")), //
		STATE_51(new Doc().level(Level.FAULT).text("ControlPanelOvertempL1")), //
		STATE_52(new Doc().level(Level.FAULT).text("PowerPanelOvertempL1")), //
		STATE_53(new Doc().level(Level.FAULT).text("DCInputOvercurrentL1")), //
		STATE_54(new Doc().level(Level.FAULT).text("CapacitorOvertempL1")), //
		STATE_55(new Doc().level(Level.FAULT).text("RadiatorOvertempL1")), //
		STATE_56(new Doc().level(Level.FAULT).text("TransformerOvertempL1")), //
		STATE_57(new Doc().level(Level.FAULT).text("CombinationCommErrorL1")), //
		STATE_58(new Doc().level(Level.FAULT).text("EEPROMErrorL1")), //
		STATE_59(new Doc().level(Level.FAULT).text("LoadCurrentZeroDriftErrorL1")), //
		STATE_60(new Doc().level(Level.FAULT).text("CurrentLimitRErrorL1")), //
		STATE_61(new Doc().level(Level.FAULT).text("PhaseSyncErrorL1")), //
		STATE_62(new Doc().level(Level.FAULT).text("ExternalPVCurrentZeroDriftErrorL1")), //
		STATE_63(new Doc().level(Level.FAULT).text("ExternalGridCurrentZeroDriftErrorL1")), //
		STATE_64(new Doc().level(Level.WARNING).text("GridUndervoltageL2")), //
		STATE_65(new Doc().level(Level.WARNING).text("GridOvervoltageL2")), //
		STATE_66(new Doc().level(Level.WARNING).text("GridUnderFrequencyL2")), //
		STATE_67(new Doc().level(Level.WARNING).text("GridOverFrequencyL2")), //
		STATE_68(new Doc().level(Level.WARNING).text("GridPowerSupplyOffL2")), //
		STATE_69(new Doc().level(Level.WARNING).text("GridConditionUnmeetL2")), //
		STATE_70(new Doc().level(Level.WARNING).text("DCUnderVoltageL2")), //
		STATE_71(new Doc().level(Level.WARNING).text("InputOverResistanceL2")), //
		STATE_72(new Doc().level(Level.WARNING).text("CombinationErrorL2")), //
		STATE_73(new Doc().level(Level.WARNING).text("CommWithInverterErrorL2")), //
		STATE_74(new Doc().level(Level.WARNING).text("TmeErrorL2")), //
		STATE_75(new Doc().level(Level.WARNING).text("PcsAlarm2L2")), //
		STATE_76(new Doc().level(Level.FAULT).text("ControlCurrentOverload100PercentL2")), //
		STATE_77(new Doc().level(Level.FAULT).text("ControlCurrentOverload110PercentL2")), //
		STATE_78(new Doc().level(Level.FAULT).text("ControlCurrentOverload150PercentL2")), //
		STATE_79(new Doc().level(Level.FAULT).text("ControlCurrentOverload200PercentL2")), //
		STATE_80(new Doc().level(Level.FAULT).text("ControlCurrentOverload210PercentL2")), //
		STATE_81(new Doc().level(Level.FAULT).text("ControlCurrentOverload300PercentL2")), //
		STATE_82(new Doc().level(Level.FAULT).text("ControlTransientLoad300PercentL2")), //
		STATE_83(new Doc().level(Level.FAULT).text("GridOverCurrentL2")), //
		STATE_84(new Doc().level(Level.FAULT).text("LockingWaveformTooManyTimesL2")), //
		STATE_85(new Doc().level(Level.FAULT).text("InverterVoltageZeroDriftErrorL2")), //
		STATE_86(new Doc().level(Level.FAULT).text("GridVoltageZeroDriftErrorL2")), //
		STATE_87(new Doc().level(Level.FAULT).text("ControlCurrentZeroDriftErrorL2")), //
		STATE_88(new Doc().level(Level.FAULT).text("InverterCurrentZeroDriftErrorL2")), //
		STATE_89(new Doc().level(Level.FAULT).text("GridCurrentZeroDriftErrorL2")), //
		STATE_90(new Doc().level(Level.FAULT).text("PDPProtectionL2")), //
		STATE_91(new Doc().level(Level.FAULT).text("HardwareControlCurrentProtectionL2")), //
		STATE_92(new Doc().level(Level.FAULT).text("HardwareACVoltageProtectionL2")), //
		STATE_93(new Doc().level(Level.FAULT).text("HardwareDCCurrentProtectionL2")), //
		STATE_94(new Doc().level(Level.FAULT).text("HardwareTemperatureProtectionL2")), //
		STATE_95(new Doc().level(Level.FAULT).text("NoCapturingSignalL2")), //
		STATE_96(new Doc().level(Level.FAULT).text("DCOvervoltageL2")), //
		STATE_97(new Doc().level(Level.FAULT).text("DCDisconnectedL2")), //
		STATE_98(new Doc().level(Level.FAULT).text("InverterUndervoltageL2")), //
		STATE_99(new Doc().level(Level.FAULT).text("InverterOvervoltageL2")), //
		STATE_100(new Doc().level(Level.FAULT).text("CurrentSensorFailL2")), //
		STATE_101(new Doc().level(Level.FAULT).text("VoltageSensorFailL2")), //
		STATE_102(new Doc().level(Level.FAULT).text("PowerUncontrollableL2")), //
		STATE_103(new Doc().level(Level.FAULT).text("CurrentUncontrollableL2")), //
		STATE_104(new Doc().level(Level.FAULT).text("FanErrorL2")), //
		STATE_105(new Doc().level(Level.FAULT).text("PhaseLackL2")), //
		STATE_106(new Doc().level(Level.FAULT).text("InverterRelayFaultL2")), //
		STATE_107(new Doc().level(Level.FAULT).text("GridRelayFaultL2")), //
		STATE_108(new Doc().level(Level.FAULT).text("ControlPanelOvertempL2")), //
		STATE_109(new Doc().level(Level.FAULT).text("PowerPanelOvertempL2")), //
		STATE_110(new Doc().level(Level.FAULT).text("DCInputOvercurrentL2")), //
		STATE_111(new Doc().level(Level.FAULT).text("CapacitorOvertempL2")), //
		STATE_112(new Doc().level(Level.FAULT).text("RadiatorOvertempL2")), //
		STATE_113(new Doc().level(Level.FAULT).text("TransformerOvertempL2")), //
		STATE_114(new Doc().level(Level.FAULT).text("CombinationCommErrorL2")), //
		STATE_115(new Doc().level(Level.FAULT).text("EEPROMErrorL2")), //
		STATE_116(new Doc().level(Level.FAULT).text("LoadCurrentZeroDriftErrorL2")), //
		STATE_117(new Doc().level(Level.FAULT).text("CurrentLimitRErrorL2")), //
		STATE_118(new Doc().level(Level.FAULT).text("PhaseSyncErrorL2")), //
		STATE_119(new Doc().level(Level.FAULT).text("ExternalPVCurrentZeroDriftErrorL2")), //
		STATE_120(new Doc().level(Level.FAULT).text("ExternalGridCurrentZeroDriftErrorL2")), //
		STATE_121(new Doc().level(Level.WARNING).text("GridUndervoltageL3")), //
		STATE_122(new Doc().level(Level.WARNING).text("GridOvervoltageL3")), //
		STATE_123(new Doc().level(Level.WARNING).text("GridUnderFrequencyL3")), //
		STATE_124(new Doc().level(Level.WARNING).text("GridOverFrequencyL3")), //
		STATE_125(new Doc().level(Level.WARNING).text("GridPowerSupplyOffL3")), //
		STATE_126(new Doc().level(Level.WARNING).text("GridConditionUnmeetL3")), //
		STATE_127(new Doc().level(Level.WARNING).text("DCUnderVoltageL3")), //
		STATE_128(new Doc().level(Level.WARNING).text("InputOverResistanceL3")), //
		STATE_129(new Doc().level(Level.WARNING).text("CombinationErrorL3")), //
		STATE_130(new Doc().level(Level.WARNING).text("CommWithInverterErrorL3")), //
		STATE_131(new Doc().level(Level.WARNING).text("TmeErrorL3")), //
		STATE_132(new Doc().level(Level.WARNING).text("PcsAlarm2L3")), //
		STATE_133(new Doc().level(Level.FAULT).text("ControlCurrentOverload100PercentL3")), //
		STATE_134(new Doc().level(Level.FAULT).text("ControlCurrentOverload110PercentL3")), //
		STATE_135(new Doc().level(Level.FAULT).text("ControlCurrentOverload150PercentL3")), //
		STATE_136(new Doc().level(Level.FAULT).text("ControlCurrentOverload200PercentL3")), //
		STATE_137(new Doc().level(Level.FAULT).text("ControlCurrentOverload210PercentL3")), //
		STATE_138(new Doc().level(Level.FAULT).text("ControlCurrentOverload300PercentL3")), //
		STATE_139(new Doc().level(Level.FAULT).text("ControlTransientLoad300PercentL3")), //
		STATE_140(new Doc().level(Level.FAULT).text("GridOverCurrentL3")), //
		STATE_141(new Doc().level(Level.FAULT).text("LockingWaveformTooManyTimesL3")), //
		STATE_142(new Doc().level(Level.FAULT).text("InverterVoltageZeroDriftErrorL3")), //
		STATE_143(new Doc().level(Level.FAULT).text("GridVoltageZeroDriftErrorL3")), //
		STATE_144(new Doc().level(Level.FAULT).text("ControlCurrentZeroDriftErrorL3")), //
		STATE_145(new Doc().level(Level.FAULT).text("InverterCurrentZeroDriftErrorL3")), //
		STATE_146(new Doc().level(Level.FAULT).text("GridCurrentZeroDriftErrorL3")), //
		STATE_147(new Doc().level(Level.FAULT).text("PDPProtectionL3")), //
		STATE_148(new Doc().level(Level.FAULT).text("HardwareControlCurrentProtectionL3")), //
		STATE_149(new Doc().level(Level.FAULT).text("HardwareACVoltageProtectionL3")), //
		STATE_150(new Doc().level(Level.FAULT).text("HardwareDCCurrentProtectionL3")), //
		STATE_151(new Doc().level(Level.FAULT).text("HardwareTemperatureProtectionL3")), //
		STATE_152(new Doc().level(Level.FAULT).text("NoCapturingSignalL3")), //
		STATE_153(new Doc().level(Level.FAULT).text("DCOvervoltageL3")), //
		STATE_154(new Doc().level(Level.FAULT).text("DCDisconnectedL3")), //
		STATE_155(new Doc().level(Level.FAULT).text("InverterUndervoltageL3")), //
		STATE_156(new Doc().level(Level.FAULT).text("InverterOvervoltageL3")), //
		STATE_157(new Doc().level(Level.FAULT).text("CurrentSensorFailL3")), //
		STATE_158(new Doc().level(Level.FAULT).text("VoltageSensorFailL3")), //
		STATE_159(new Doc().level(Level.FAULT).text("PowerUncontrollableL3")), //
		STATE_160(new Doc().level(Level.FAULT).text("CurrentUncontrollableL3")), //
		STATE_161(new Doc().level(Level.FAULT).text("FanErrorL3")), //
		STATE_162(new Doc().level(Level.FAULT).text("PhaseLackL3")), //
		STATE_163(new Doc().level(Level.FAULT).text("InverterRelayFaultL3")), //
		STATE_164(new Doc().level(Level.FAULT).text("GridRelayFaultL3")), //
		STATE_165(new Doc().level(Level.FAULT).text("ControlPanelOvertempL3")), //
		STATE_166(new Doc().level(Level.FAULT).text("PowerPanelOvertempL3")), //
		STATE_167(new Doc().level(Level.FAULT).text("DCInputOvercurrentL3")), //
		STATE_168(new Doc().level(Level.FAULT).text("CapacitorOvertempL3")), //
		STATE_169(new Doc().level(Level.FAULT).text("RadiatorOvertempL3")), //
		STATE_170(new Doc().level(Level.FAULT).text("TransformerOvertempL3")), //
		STATE_171(new Doc().level(Level.FAULT).text("CombinationCommErrorL3")), //
		STATE_172(new Doc().level(Level.FAULT).text("EEPROMErrorL3")), //
		STATE_173(new Doc().level(Level.FAULT).text("LoadCurrentZeroDriftErrorL3")), //
		STATE_174(new Doc().level(Level.FAULT).text("CurrentLimitRErrorL3")), //
		STATE_175(new Doc().level(Level.FAULT).text("PhaseSyncErrorL3")), //
		STATE_176(new Doc().level(Level.FAULT).text("ExternalPVCurrentZeroDriftErrorL3")), //
		STATE_177(new Doc().level(Level.FAULT).text("ExternalGridCurrentZeroDriftErrorL3")) //
		; //

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
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	private IntegerWriteChannel getSetActivePowerL1Channel() {
		return this.channel(FeneconProEss.ChannelId.SET_ACTIVE_POWER_L1);
	}

	private IntegerWriteChannel getSetActivePowerL2Channel() {
		return this.channel(FeneconProEss.ChannelId.SET_ACTIVE_POWER_L2);
	}

	private IntegerWriteChannel getSetActivePowerL3Channel() {
		return this.channel(FeneconProEss.ChannelId.SET_ACTIVE_POWER_L3);
	}

	private IntegerWriteChannel getSetReactivePowerL1Channel() {
		return this.channel(FeneconProEss.ChannelId.SET_REACTIVE_POWER_L1);
	}

	private IntegerWriteChannel getSetReactivePowerL2Channel() {
		return this.channel(FeneconProEss.ChannelId.SET_REACTIVE_POWER_L2);
	}

	private IntegerWriteChannel getSetReactivePowerL3Channel() {
		return this.channel(FeneconProEss.ChannelId.SET_REACTIVE_POWER_L3);
	}

	private IntegerWriteChannel getPcsModeChannel() {
		return this.channel(FeneconProEss.ChannelId.PCS_MODE);
	}

	private PcsMode getPcsMode() {
		return this.getPcsModeChannel().value().asEnum();
	}

	private IntegerWriteChannel getSetupModeChannel() {
		return this.channel(FeneconProEss.ChannelId.SETUP_MODE);
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
		} catch (OpenemsException e) {
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
