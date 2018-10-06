package io.openems.edge.ess.refu;

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
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.channel.merger.ChannelMergerSumInteger;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Refu.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)

public class RefuEss extends AbstractOpenemsModbusComponent
		implements SymmetricEss, AsymmetricEss, ManagedAsymmetricEss, ManagedSymmetricEss, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(RefuEss.class);

	// TODO
	protected final static int MAX_APPARENT_POWER = 9000;
	private final static int UNIT_ID = 1;

	@Reference
	private Power power;

	@Reference
	protected ConfigurationAdmin cm;

	public RefuEss() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
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
		super.activate(context, config.service_pid(), config.id(), config.enabled(), UNIT_ID, this.cm, "Modbus",
				config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
	return  new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(0x100, Priority.HIGH, //
						m(RefuEss.ChannelId.SYSTEM_STATE, new UnsignedWordElement(0x100)), //
						bm(new UnsignedWordElement(0x101))//
								.m(RefuEss.ChannelId.STATE_0, 0) //
								.m(RefuEss.ChannelId.STATE_1, 1) //
								.m(RefuEss.ChannelId.STATE_2, 2) //
								.m(RefuEss.ChannelId.STATE_3, 3) //
								.m(RefuEss.ChannelId.STATE_4, 4) //
								.m(RefuEss.ChannelId.STATE_5, 5) //
								.m(RefuEss.ChannelId.STATE_6, 6) //
								.m(RefuEss.ChannelId.STATE_7, 7) //
								.m(RefuEss.ChannelId.STATE_8, 8) //
								.m(RefuEss.ChannelId.STATE_9, 9) //
								.m(RefuEss.ChannelId.STATE_10, 10) //
								.build(), //
						m(RefuEss.ChannelId.COMMUNICATION_INFORMATIONS, new UnsignedWordElement(0x102)), //
						m(RefuEss.ChannelId.INVERTER_STATUS, new UnsignedWordElement(0x103)), //
						bm(new UnsignedWordElement(0x104))//
								.m(RefuEss.ChannelId.STATE_11, 0)//
								.build(),
						m(RefuEss.ChannelId.DCDC_STATUS, new UnsignedWordElement(0x105)), //
						bm(new UnsignedWordElement(0x106))//
								.m(RefuEss.ChannelId.STATE_12, 0)//
								.build(),
						m(RefuEss.ChannelId.BATTERY_CURRENT_PCS, new SignedWordElement(0x107),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(RefuEss.ChannelId.BATTERY_VOLTAGE_PCS, new SignedWordElement(0x108),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(RefuEss.ChannelId.CURRENT, new SignedWordElement(0x109),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(RefuEss.ChannelId.CURRENT_L1, new SignedWordElement(0x10A),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(RefuEss.ChannelId.CURRENT_L2, new SignedWordElement(0x10B),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(RefuEss.ChannelId.CURRENT_L3, new SignedWordElement(0x10C),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedWordElement(0x10D),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L1, new SignedWordElement(0x10E),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L2, new SignedWordElement(0x10F),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L3, new SignedWordElement(0x110),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new SignedWordElement(0x111),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L1, new SignedWordElement(0x112),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L2, new SignedWordElement(0x113),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L3, new SignedWordElement(0x114),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(RefuEss.ChannelId.COS_PHI_3P, new SignedWordElement(0x115)),
						m(RefuEss.ChannelId.COS_PHI_L1, new SignedWordElement(0x116)),
						m(RefuEss.ChannelId.COS_PHI_L2, new SignedWordElement(0x117)),
						m(RefuEss.ChannelId.COS_PHI_L3, new SignedWordElement(0x118))),

				new FC4ReadInputRegistersTask(0x11A, Priority.LOW, //

						m(RefuEss.ChannelId.PCS_ALLOWED_CHARGE, new SignedWordElement(0x11A)), //
						m(RefuEss.ChannelId.PCS_ALLOWED_DISCHARGE, new SignedWordElement(0x11B)), //
						m(RefuEss.ChannelId.BATTERY_STATE, new UnsignedWordElement(0x11C)), //
						m(RefuEss.ChannelId.BATTERY_MODE, new UnsignedWordElement(0x11D)), //
						m(RefuEss.ChannelId.BATTERY_VOLTAGE, new SignedWordElement(0x11E)), //
						m(RefuEss.ChannelId.BATTERY_CURRENT, new SignedWordElement(0x11F)), //
						m(RefuEss.ChannelId.BATTERY_POWER, new SignedWordElement(0x120)), //
						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(0x121)), //
						m(RefuEss.ChannelId.ALLOWED_CHARGE_CURRENT, new UnsignedWordElement(0x122),
								ElementToChannelConverter.SCALE_FACTOR_2_AND_INVERT), // need to check
						m(RefuEss.ChannelId.ALLOWED_DISCHARGE_CURRENT, new UnsignedWordElement(0x123),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, new UnsignedWordElement(0x124),
								ElementToChannelConverter.SCALE_FACTOR_2_AND_INVERT), //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, new UnsignedWordElement(0x125),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(RefuEss.ChannelId.BATTERY_CHARGE_ENERGY,
								new SignedDoublewordElement(0x126).wordOrder(WordOrder.LSWMSW)), // 
						m(RefuEss.ChannelId.BATTERY_DISCHARGE_ENERGY,
								new SignedDoublewordElement(0x128).wordOrder(WordOrder.LSWMSW)), // 

						m(RefuEss.ChannelId.BATTERY_OPERATION_STATUS, new UnsignedWordElement(0x12A)), //

						m(RefuEss.ChannelId.BATTERY_HIGHEST_VOLTAGE, new UnsignedWordElement(0x12B)), //
						m(RefuEss.ChannelId.BATTERY_LOWEST_VOLTAGE, new UnsignedWordElement(0x12C)), //
						m(RefuEss.ChannelId.BATTERY_HIGHEST_TEMPERATURE, new SignedWordElement(0x12D)), //
						m(RefuEss.ChannelId.BATTERY_LOWEST_TEMPERATURE, new SignedWordElement(0x12E)), //
						m(RefuEss.ChannelId.BATTERY_STOP_REQUEST, new UnsignedWordElement(0x12F)), //

						bm(new UnsignedWordElement(0x130))//
								.m(RefuEss.ChannelId.STATE_13, 0) //
								.m(RefuEss.ChannelId.STATE_14, 1) //
								.m(RefuEss.ChannelId.STATE_15, 2) //
								.m(RefuEss.ChannelId.STATE_16, 3) //
								.m(RefuEss.ChannelId.STATE_17, 4) //
								.m(RefuEss.ChannelId.STATE_18, 5) //
								.m(RefuEss.ChannelId.STATE_19, 6) //
								.m(RefuEss.ChannelId.STATE_20, 7) //
								.m(RefuEss.ChannelId.STATE_21, 8) //
								.m(RefuEss.ChannelId.STATE_22, 9) //
								.m(RefuEss.ChannelId.STATE_23, 10) //
								.m(RefuEss.ChannelId.STATE_24, 11) //
								.m(RefuEss.ChannelId.STATE_25, 12) //
								.m(RefuEss.ChannelId.STATE_26, 13) //
								.m(RefuEss.ChannelId.STATE_27, 14) //
								.build(), //

						bm(new UnsignedWordElement(0x131))//
								.m(RefuEss.ChannelId.STATE_28, 0) //
								.m(RefuEss.ChannelId.STATE_29, 1) //
								.m(RefuEss.ChannelId.STATE_30, 5) //
								.m(RefuEss.ChannelId.STATE_31, 7) //
								.build(),

						bm(new UnsignedWordElement(0x132))//
								.m(RefuEss.ChannelId.STATE_32, 0) //
								.m(RefuEss.ChannelId.STATE_33, 1) //
								.m(RefuEss.ChannelId.STATE_34, 2) //
								.m(RefuEss.ChannelId.STATE_35, 3) //
								.build(),

						bm(new UnsignedWordElement(0x133))//
								.m(RefuEss.ChannelId.STATE_36, 0) //
								.m(RefuEss.ChannelId.STATE_37, 1) //
								.m(RefuEss.ChannelId.STATE_38, 2) //
								.m(RefuEss.ChannelId.STATE_39, 3) //
								.build(),

						new DummyRegisterElement(0x134), //

						bm(new UnsignedWordElement(0x135))//
								.m(RefuEss.ChannelId.STATE_40, 0) //
								.m(RefuEss.ChannelId.STATE_41, 1) //
								.m(RefuEss.ChannelId.STATE_42, 2) //
								.m(RefuEss.ChannelId.STATE_43, 3) //
								.build(),

						bm(new UnsignedWordElement(0x136))//
								.m(RefuEss.ChannelId.STATE_44, 0) //
								.m(RefuEss.ChannelId.STATE_45, 1) //
								.m(RefuEss.ChannelId.STATE_46, 2) //
								.m(RefuEss.ChannelId.STATE_47, 3) //
								.build(),

						bm(new UnsignedWordElement(0x137))//
								.m(RefuEss.ChannelId.STATE_48, 0) //
								.m(RefuEss.ChannelId.STATE_49, 1) //
								.m(RefuEss.ChannelId.STATE_50, 2) //
								.m(RefuEss.ChannelId.STATE_51, 3) //
								.m(RefuEss.ChannelId.STATE_52, 4) //
								.m(RefuEss.ChannelId.STATE_53, 5) //
								.m(RefuEss.ChannelId.STATE_54, 10) //
								.m(RefuEss.ChannelId.STATE_55, 11) //
								.m(RefuEss.ChannelId.STATE_56, 12) //
								.m(RefuEss.ChannelId.STATE_57, 13) //
								.build(),

						bm(new UnsignedWordElement(0x138))//
								.m(RefuEss.ChannelId.STATE_58, 0) //
								.m(RefuEss.ChannelId.STATE_59, 1) //
								.m(RefuEss.ChannelId.STATE_60, 2) //
								.m(RefuEss.ChannelId.STATE_61, 3) //
								.build(),

						bm(new UnsignedWordElement(0x139))//
								.m(RefuEss.ChannelId.STATE_62, 0) //
								.m(RefuEss.ChannelId.STATE_63, 1) //
								.m(RefuEss.ChannelId.STATE_64, 2) //
								.m(RefuEss.ChannelId.STATE_65, 3) //
								.build(),

						bm(new UnsignedWordElement(0x13A))//
								.m(RefuEss.ChannelId.STATE_66, 0) //
								.m(RefuEss.ChannelId.STATE_67, 1) //
								.m(RefuEss.ChannelId.STATE_68, 2) //
								.m(RefuEss.ChannelId.STATE_69, 3) //
								.build(),

						bm(new UnsignedWordElement(0x13B))//
								.m(RefuEss.ChannelId.STATE_70, 0) //
								.m(RefuEss.ChannelId.STATE_71, 1) //
								.m(RefuEss.ChannelId.STATE_72, 2) //
								.m(RefuEss.ChannelId.STATE_73, 3) //
								.build(),

						bm(new UnsignedWordElement(0x13C))//
								.m(RefuEss.ChannelId.STATE_74, 0) //
								.m(RefuEss.ChannelId.STATE_75, 1) //
								.m(RefuEss.ChannelId.STATE_76, 2) //
								.m(RefuEss.ChannelId.STATE_77, 3) //
								.build(),

						bm(new UnsignedWordElement(0x13D))//
								.m(RefuEss.ChannelId.STATE_78, 0) //
								.build(),

						bm(new UnsignedWordElement(0x13E))//
								.m(RefuEss.ChannelId.STATE_79, 0) //
								.build(),

						bm(new UnsignedWordElement(0x13F))//
								.m(RefuEss.ChannelId.STATE_80, 2) //
								.m(RefuEss.ChannelId.STATE_81, 3) //
								.m(RefuEss.ChannelId.STATE_82, 4) //
								.m(RefuEss.ChannelId.STATE_83, 6) //
								.m(RefuEss.ChannelId.STATE_84, 9) //
								.m(RefuEss.ChannelId.STATE_85, 10) //
								.m(RefuEss.ChannelId.STATE_86, 11) //
								.m(RefuEss.ChannelId.STATE_87, 12) //
								.m(RefuEss.ChannelId.STATE_88, 13) //
								.m(RefuEss.ChannelId.STATE_89, 14) //
								.m(RefuEss.ChannelId.STATE_90, 15) //
								.build(),
						
						bm(new UnsignedWordElement(0x140))//
								.m(RefuEss.ChannelId.STATE_91, 2) //
								.m(RefuEss.ChannelId.STATE_92, 3) //
								.m(RefuEss.ChannelId.STATE_93, 7) //
								.m(RefuEss.ChannelId.STATE_94, 8) //
								.m(RefuEss.ChannelId.STATE_95, 10) //
								.m(RefuEss.ChannelId.STATE_96, 11) //
								.m(RefuEss.ChannelId.STATE_97, 12) //
								.m(RefuEss.ChannelId.STATE_98, 13) //
								.m(RefuEss.ChannelId.STATE_99, 14) //
								.build(),
						// TODO maybe normal integer
						bm(new UnsignedWordElement(0x141))//
								.m(RefuEss.ChannelId.STATE_100, 0) //
								.build(),

						bm(new UnsignedWordElement(0x142))//
								.m(RefuEss.ChannelId.STATE_101, 0) //
								.build(),

						bm(new UnsignedWordElement(0x143))//
								.m(RefuEss.ChannelId.STATE_102, 0) //
								.build(),

						bm(new UnsignedWordElement(0x144))//
								.m(RefuEss.ChannelId.STATE_103, 0) //
								.build(),

						bm(new UnsignedWordElement(0x145))//
								.m(RefuEss.ChannelId.STATE_104, 0) //
								.build(),

						bm(new UnsignedWordElement(0x146))//
								.m(RefuEss.ChannelId.STATE_105, 0) //
								.build(),

						bm(new UnsignedWordElement(0x147))//
								.m(RefuEss.ChannelId.STATE_106, 0) //
								.build(),

						bm(new UnsignedWordElement(0x148))//
								.m(RefuEss.ChannelId.STATE_107, 0) //
								.build(),

						bm(new UnsignedWordElement(0x149))//
								.m(RefuEss.ChannelId.STATE_108, 0) //
								.build(),

						bm(new UnsignedWordElement(0x14a))//
								.m(RefuEss.ChannelId.STATE_109, 0) //
								.build(),

						bm(new UnsignedWordElement(0x14b))//
								.m(RefuEss.ChannelId.STATE_110, 0) //
								.build(),

						bm(new UnsignedWordElement(0x14c))//
								.m(RefuEss.ChannelId.STATE_111, 0) //
								.build(),

						bm(new UnsignedWordElement(0x14d))//
								.m(RefuEss.ChannelId.STATE_112, 0) //
								.build(),

						bm(new UnsignedWordElement(0x14e))//
								.m(RefuEss.ChannelId.STATE_113, 0) //
								.build(),

						bm(new UnsignedWordElement(0x14f))//
								.m(RefuEss.ChannelId.STATE_114, 0) //
								.build(),

						bm(new UnsignedWordElement(0x150))//
								.m(RefuEss.ChannelId.STATE_115, 0) //
								.build(),

						bm(new UnsignedWordElement(0x151))//
								.m(RefuEss.ChannelId.STATE_116, 0) //
								.build(),

						bm(new UnsignedWordElement(0x152))//
								.m(RefuEss.ChannelId.STATE_117, 0) //
								.build(),

						bm(new UnsignedWordElement(0x153))//
								.m(RefuEss.ChannelId.STATE_118, 0) //
								.build(),

						bm(new UnsignedWordElement(0x154))//
								.m(RefuEss.ChannelId.STATE_119, 0) //
								.build(),

						bm(new UnsignedWordElement(0x155))//
								.m(RefuEss.ChannelId.STATE_120, 0) //
								.build() //

				),

				new FC16WriteRegistersTask(0x200, //
						m(RefuEss.ChannelId.SET_WORK_STATE, new UnsignedWordElement(0x200)),
						m(RefuEss.ChannelId.SET_SYSTEM_ERROR_RESET, new UnsignedWordElement(0x201)),
						m(RefuEss.ChannelId.SET_OPERATION_MODE, new UnsignedWordElement(0x202))), //
				new FC16WriteRegistersTask(0x203, //
						m(RefuEss.ChannelId.SET_ACTIVE_POWER, new SignedWordElement(0x203),
								ElementToChannelConverter.SCALE_FACTOR_2)),
				new FC16WriteRegistersTask(0x204, //
						m(RefuEss.ChannelId.SET_ACTIVE_POWER_L1, new SignedWordElement(0x204),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(RefuEss.ChannelId.SET_ACTIVE_POWER_L2, new SignedWordElement(0x205),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(RefuEss.ChannelId.SET_ACTIVE_POWER_L3, new SignedWordElement(0x206),
								ElementToChannelConverter.SCALE_FACTOR_2)), //
				new FC16WriteRegistersTask(0x207, //
						m(RefuEss.ChannelId.SET_REACTIVE_POWER, new SignedWordElement(0x207),
								ElementToChannelConverter.SCALE_FACTOR_2)),
				new FC16WriteRegistersTask(0x208, //
						m(RefuEss.ChannelId.SET_REACTIVE_POWER_L1, new SignedWordElement(0x208),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(RefuEss.ChannelId.SET_REACTIVE_POWER_L2, new SignedWordElement(0x209),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(RefuEss.ChannelId.SET_REACTIVE_POWER_L3, new SignedWordElement(0x20A),
								ElementToChannelConverter.SCALE_FACTOR_2)));//

//		new ChannelMergerSumInteger( //
//				/* target */ this.getActivePower(), //
//				/* sources */ (Channel<Integer>[]) new Channel<?>[] { //
//						this.getActivePowerL1(), //
//						this.getActivePowerL2(), //
//						this.getActivePowerL3() //
//				});
//		new ChannelMergerSumInteger( //
//				/* target */ this.getReactivePower(), //
//				/* sources */ (Channel<Integer>[]) new Channel<?>[] { //
//						this.getReactivePowerL1(), //
//						this.getReactivePowerL2(), //
//						this.getReactivePowerL3() //
//				});
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value().asString() //
				+ "|L:" + this.getActivePower().value().asString() //
				+ "|Allowed:" + this.getAllowedCharge().value().asStringWithoutUnit() + ";"
				+ this.getAllowedDischarge().value().asString();
	}

	private enum SetWorkState {
		STOP, START
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		SYSTEM_STATE(new Doc() //
				.option(0, "STOP") //
				.option(1, "Init") //
				.option(2, "Pre-operation") //
				.option(3, "STANDBY") //
				.option(4, "START") //
				.option(4, "FAULT")), //

		COMMUNICATION_INFORMATIONS(new Doc()//
				.option(1, "Gateway Initialized")//
				.option(2, "Modbus Slave Status")//
				.option(4, "Modbus Master Status")//
				.option(8, "CAN Timeout")//
				.option(16, "First Communication Ok")), //

		INVERTER_STATUS(new Doc()//
				.option(1, "Ready to Power on")//
				.option(2, "Ready for Operating")//
				.option(4, "Enabled")//
				.option(8, "Fault")//
				.option(256, "Warning")//
				.option(512, "Voltage/Current mode").option(1024, "Power mode")//
				.option(2048, "AC relays close")//
				.option(4096, "DC relays 1 close")//
				.option(8192, "DC relays 2 close")//
				.option(16384, "Mains OK")), //

		SET_WORK_STATE(new Doc() //
				.option(0, SetWorkState.STOP)//
				.option(1, SetWorkState.START)), //

		DCDC_STATUS(new Doc() //
				.option(1, "Ready to Power on")//
				.option(2, "Ready for Operating")//
				.option(4, "Enabled")//
				.option(8, "DCDC Fault")//
				.option(128, "DCDC Warning")//
				.option(256, "Voltage/Current mode").option(512, "Power mode")), //

		BATTERY_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
		BATTERY_POWER(new Doc().unit(Unit.WATT)), //
		BATTERY_VOLTAGE_PCS(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_CURRENT_PCS(new Doc().unit(Unit.MILLIAMPERE)), //
		PCS_ALLOWED_CHARGE(new Doc().unit(Unit.KILOWATT)), //
		PCS_ALLOWED_DISCHARGE(new Doc().unit(Unit.KILOWATT)), //
		ALLOWED_CHARGE_CURRENT(new Doc().unit(Unit.MILLIAMPERE)),
		ALLOWED_DISCHARGE_CURRENT(new Doc().unit(Unit.MILLIAMPERE)),

		

		BATTERY_CHARGE_ENERGY(new Doc().unit(Unit.KILOWATT_HOURS)),
		BATTERY_DISCHARGE_ENERGY(new Doc().unit(Unit.KILOWATT_HOURS)),
		BATTERY_HIGHEST_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), BATTERY_LOWEST_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		BATTERY_HIGHEST_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELSIUS)),
		BATTERY_LOWEST_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELSIUS)), BATTERY_STOP_REQUEST(new Doc()),
		CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
		CURRENT_L1(new Doc().unit(Unit.MILLIAMPERE)), //
		CURRENT_L2(new Doc().unit(Unit.MILLIAMPERE)), //
		CURRENT_L3(new Doc().unit(Unit.MILLIAMPERE)), //
		SET_ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		SET_ACTIVE_POWER_L1(new Doc().unit(Unit.WATT)), //
		SET_ACTIVE_POWER_L2(new Doc().unit(Unit.WATT)), //
		SET_ACTIVE_POWER_L3(new Doc().unit(Unit.WATT)), //
		SET_REACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		SET_REACTIVE_POWER_L1(new Doc().unit(Unit.VOLT_AMPERE)), //
		SET_REACTIVE_POWER_L2(new Doc().unit(Unit.VOLT_AMPERE)), //
		SET_REACTIVE_POWER_L3(new Doc().unit(Unit.VOLT_AMPERE)), //

		COS_PHI_3P(new Doc()), COS_PHI_L1(new Doc()), COS_PHI_L2(new Doc()),
		COS_PHI_L3(new Doc().unit(null)),

		BATTERY_OPERATION_STATUS(new Doc() //
				.option(1, "Battery group 1 operating")//
				.option(2, "Battery group 2 operating")//
				.option(4, "Battery group 3 operating")//
				.option(8, "Battery group 4 operating")), //

		BATTERY_STATE(new Doc() //
				.option(0, "Initial")//
				.option(1, "STOP")//
				.option(2, "Starting")//
				.option(3, "START")//
				.option(4, "Stopping")//
				.option(5, "Fault")), //

		BATTERY_MODE(new Doc() //
				.option(0, "Normal Mode")), //

		SET_SYSTEM_ERROR_RESET(new Doc()//
				.option(0, "OFF")//
				.option(1, "ON")), //

		SET_OPERATION_MODE(new Doc()//
				.option(0, "P/Q Set point")//
				.option(1, "IAC / cosphi set point")), //

		STATE_0(new Doc().level(Level.FAULT).text("BMSInError")), //
		STATE_1(new Doc().level(Level.FAULT).text("BMSInErrorSecond")), //
		STATE_2(new Doc().level(Level.FAULT).text("BMSUndervoltage")), //
		STATE_3(new Doc().level(Level.FAULT).text("BMSOvercurrent")), //
		STATE_4(new Doc().level(Level.FAULT).text("ErrorBMSLimitsNotInitialized")), //
		STATE_5(new Doc().level(Level.FAULT).text("ConnectError")), //
		STATE_6(new Doc().level(Level.FAULT).text("OvervoltageWarning")), //
		STATE_7(new Doc().level(Level.FAULT).text("UndervoltageWarning")), //
		STATE_8(new Doc().level(Level.FAULT).text("OvercurrentWarning")), //
		STATE_9(new Doc().level(Level.FAULT).text("BMSReady")), //
		STATE_10(new Doc().level(Level.FAULT).text("TREXReady")), //

		STATE_11(new Doc().level(Level.WARNING).text("ErrorCode")), //

		STATE_12(new Doc().level(Level.WARNING).text("DCDCError")), //

		STATE_13(new Doc().level(Level.WARNING).text("NormalChargingOverCurrent")), //
		STATE_14(new Doc().level(Level.WARNING).text("CharginigCurrentOverLimit")), //
		STATE_15(new Doc().level(Level.WARNING).text("DischargingCurrentOverLimit")), //
		STATE_16(new Doc().level(Level.WARNING).text("NormalHighVoltage")), //
		STATE_17(new Doc().level(Level.WARNING).text("NormalLowVoltage")), //
		STATE_18(new Doc().level(Level.WARNING).text("AbnormalVoltageVariation")), //
		STATE_19(new Doc().level(Level.WARNING).text("NormalHighTemperature")), //
		STATE_20(new Doc().level(Level.WARNING).text("NormalLowTemperature")), //
		STATE_21(new Doc().level(Level.WARNING).text("AbnormalTemperatureVariation")), //
		STATE_22(new Doc().level(Level.WARNING).text("SeriousHighVoltage")), //
		STATE_23(new Doc().level(Level.WARNING).text("SeriousLowVoltage")), //
		STATE_24(new Doc().level(Level.WARNING).text("SeriousLowTemperature")), //
		STATE_25(new Doc().level(Level.WARNING).text("ChargingSeriousOverCurrent")), //
		STATE_26(new Doc().level(Level.WARNING).text("DischargingSeriousOverCurrent")), //
		STATE_27(new Doc().level(Level.WARNING).text("AbnormalCapacityAlarm")), //

		STATE_28(new Doc().level(Level.WARNING).text("EEPROMParameterFailure")), //
		STATE_29(new Doc().level(Level.WARNING).text("SwitchOfInsideCombinedCabinet")), //
		STATE_30(new Doc().level(Level.WARNING).text("ShouldNotBeConnectedToGridDueToTheDCSideCondition")), //
		STATE_31(new Doc().level(Level.WARNING).text("EmergencyStopRequireFromSystemController")), //

		STATE_32(new Doc().level(Level.WARNING).text("BatteryGroup1EnableAndNotConnectedToGrid")), //
		STATE_33(new Doc().level(Level.WARNING).text("BatteryGroup2EnableAndNotConnectedToGrid")), //
		STATE_34(new Doc().level(Level.WARNING).text("BatteryGroup3EnableAndNotConnectedToGrid")), //
		STATE_35(new Doc().level(Level.WARNING).text("BatteryGroup4EnableAndNotConnectedToGrid")), //

		STATE_36(new Doc().level(Level.WARNING).text("TheIsolationSwitchOfBatteryGroup1Open")), //
		STATE_37(new Doc().level(Level.WARNING).text("TheIsolationSwitchOfBatteryGroup2Open")), //
		STATE_38(new Doc().level(Level.WARNING).text("TheIsolationSwitchOfBatteryGroup3Open")), //
		STATE_39(new Doc().level(Level.WARNING).text("TheIsolationSwitchOfBatteryGroup4Open")), //

		STATE_40(new Doc().level(Level.WARNING).text("BalancingSamplingFailureOfBatteryGroup1")), //
		STATE_41(new Doc().level(Level.WARNING).text("BalancingSamplingFailureOfBatteryGroup2")), //
		STATE_42(new Doc().level(Level.WARNING).text("BalancingSamplingFailureOfBatteryGroup3")), //
		STATE_43(new Doc().level(Level.WARNING).text("BalancingSamplingFailureOfBatteryGroup4")), //

		STATE_44(new Doc().level(Level.WARNING).text("BalancingControlFailureOfBatteryGroup1")), //
		STATE_45(new Doc().level(Level.WARNING).text("BalancingControlFailureOfBatteryGroup2")), //
		STATE_46(new Doc().level(Level.WARNING).text("BalancingControlFailureOfBatteryGroup3")), //
		STATE_47(new Doc().level(Level.WARNING).text("BalancingControlFailureOfBatteryGroup4")), //

		STATE_48(new Doc().level(Level.FAULT).text("NoEnableBateryGroupOrUsableBatteryGroup")), //
		STATE_49(new Doc().level(Level.FAULT).text("NormalLeakageOfBatteryGroup")), //
		STATE_50(new Doc().level(Level.FAULT).text("SeriousLeakageOfBatteryGroup")), //
		STATE_51(new Doc().level(Level.FAULT).text("BatteryStartFailure")), //
		STATE_52(new Doc().level(Level.FAULT).text("BatteryStopFailure")), //
		STATE_53(new Doc().level(Level.FAULT).text("InterruptionOfCANCommunicationBetweenBatteryGroupAndController")), //
		STATE_54(new Doc().level(Level.FAULT).text("EmergencyStopAbnormalOfAuxiliaryCollector")), //
		STATE_55(new Doc().level(Level.FAULT).text("LeakageSelfDetectionOnNegative")), //
		STATE_56(new Doc().level(Level.FAULT).text("LeakageSelfDetectionOnPositive")), //
		STATE_57(new Doc().level(Level.FAULT).text("SelfDetectionFailureOnBattery")), //

		STATE_58(new Doc().level(Level.FAULT).text("CANCommunicationInterruptionBetweenBatteryGroupAndGroup1")), //
		STATE_59(new Doc().level(Level.FAULT).text("CANCommunicationInterruptionBetweenBatteryGroupAndGroup2")), //
		STATE_60(new Doc().level(Level.FAULT).text("CANCommunicationInterruptionBetweenBatteryGroupAndGroup3")), //
		STATE_61(new Doc().level(Level.FAULT).text("CANCommunicationInterruptionBetweenBatteryGroupAndGroup4")), //

		STATE_62(new Doc().level(Level.FAULT).text("MainContractorAbnormalInBatterySelfDetectGroup1")), //
		STATE_63(new Doc().level(Level.FAULT).text("MainContractorAbnormalInBatterySelfDetectGroup2")), //
		STATE_64(new Doc().level(Level.FAULT).text("MainContractorAbnormalInBatterySelfDetectGroup3")), //
		STATE_65(new Doc().level(Level.FAULT).text("MainContractorAbnormalInBatterySelfDetectGroup4")), //

		STATE_66(new Doc().level(Level.FAULT).text("PreChargeContractorAbnormalOnBatterySelfDetectGroup1")), //
		STATE_67(new Doc().level(Level.FAULT).text("PreChargeContractorAbnormalOnBatterySelfDetectGroup2")), //
		STATE_68(new Doc().level(Level.FAULT).text("PreChargeContractorAbnormalOnBatterySelfDetectGroup3")), //
		STATE_69(new Doc().level(Level.FAULT).text("PreChargeContractorAbnormalOnBatterySelfDetectGroup4")), //

		STATE_70(new Doc().level(Level.FAULT).text("MainContactFailureOnBatteryControlGroup1")), //
		STATE_71(new Doc().level(Level.FAULT).text("MainContactFailureOnBatteryControlGroup2")), //
		STATE_72(new Doc().level(Level.FAULT).text("MainContactFailureOnBatteryControlGroup3")), //
		STATE_73(new Doc().level(Level.FAULT).text("MainContactFailureOnBatteryControlGroup4")), //

		STATE_74(new Doc().level(Level.FAULT).text("PreChargeFailureOnBatteryControlGroup1")), //
		STATE_75(new Doc().level(Level.FAULT).text("PreChargeFailureOnBatteryControlGroup2")), //
		STATE_76(new Doc().level(Level.FAULT).text("PreChargeFailureOnBatteryControlGroup3")), //
		STATE_77(new Doc().level(Level.FAULT).text("PreChargeFailureOnBatteryControlGroup4")), //

		STATE_78(new Doc().level(Level.FAULT).text("BatteryFault7")), //
		STATE_79(new Doc().level(Level.FAULT).text("BatteryFault8")), //

		STATE_80(new Doc().level(Level.FAULT).text("SamplingCircuitAbnormalForBMU")), //
		STATE_81(new Doc().level(Level.FAULT).text("PowerCableDisconnectFailure")), //
		STATE_82(new Doc().level(Level.FAULT).text("SamplingCircuitDisconnectFailure")), //
		STATE_83(new Doc().level(Level.FAULT).text("CANDisconnectForMasterAndSlave")), //
		STATE_84(new Doc().level(Level.FAULT).text("SammplingCircuitFailure")), //
		STATE_85(new Doc().level(Level.FAULT).text("SingleBatteryFailure")), //
		STATE_86(new Doc().level(Level.FAULT).text("CircuitDetectionAbnormalForMainContactor")), //
		STATE_87(new Doc().level(Level.FAULT).text("CircuitDetectionAbnormalForMainContactorSecond")), //
		STATE_88(new Doc().level(Level.FAULT).text("CircuitDetectionAbnormalForFancontactor")), //
		STATE_89(new Doc().level(Level.FAULT).text("BMUPowerContactorCircuitDetectionAbnormal")), //
		STATE_90(new Doc().level(Level.FAULT).text("CentralContactorCircuitDetectionAbnormal")), // 3

		STATE_91(new Doc().level(Level.FAULT).text("SeriousTemperatureFault")), //
		STATE_92(new Doc().level(Level.FAULT).text("CommunicationFaultForSystemController")), //
		STATE_93(new Doc().level(Level.FAULT).text("FrogAlarm")), //
		STATE_94(new Doc().level(Level.FAULT).text("FuseFault")), //
		STATE_95(new Doc().level(Level.FAULT).text("NormalLeakage")), //
		STATE_96(new Doc().level(Level.FAULT).text("SeriousLeakage")), //
		STATE_97(new Doc().level(Level.FAULT).text("CANDisconnectionBetweenBatteryGroupAndBatteryStack")), //
		STATE_98(new Doc().level(Level.FAULT).text("CentralContactorCircuitOpen")), //
		STATE_99(new Doc().level(Level.FAULT).text("BMUPowerContactorOpen")), //

		STATE_100(new Doc().level(Level.FAULT).text("BatteryFault11")), //
		STATE_101(new Doc().level(Level.FAULT).text("BatteryFault12")), //
		STATE_102(new Doc().level(Level.FAULT).text("BatteryFault13")), //
		STATE_103(new Doc().level(Level.FAULT).text("BatteryFault14")), //

		STATE_104(new Doc().level(Level.FAULT).text("BatteryGroupControlStatus")), //

		STATE_105(new Doc().level(Level.WARNING).text("ErrorLog1")), //
		STATE_106(new Doc().level(Level.WARNING).text("ErrorLog2")), //
		STATE_107(new Doc().level(Level.WARNING).text("ErrorLog3")), //
		STATE_108(new Doc().level(Level.WARNING).text("ErrorLog4")), //
		STATE_109(new Doc().level(Level.WARNING).text("ErrorLog5")), //
		STATE_110(new Doc().level(Level.WARNING).text("ErrorLog6")), //
		STATE_111(new Doc().level(Level.WARNING).text("ErrorLog7")), //
		STATE_112(new Doc().level(Level.WARNING).text("ErrorLog8")), //
		STATE_113(new Doc().level(Level.WARNING).text("ErrorLog9")), //
		STATE_114(new Doc().level(Level.WARNING).text("ErrorLog10")), //
		STATE_115(new Doc().level(Level.WARNING).text("ErrorLog11")), //
		STATE_116(new Doc().level(Level.WARNING).text("ErrorLog12")), //
		STATE_117(new Doc().level(Level.WARNING).text("ErrorLog13")), //
		STATE_118(new Doc().level(Level.WARNING).text("ErrorLog14")), //
		STATE_119(new Doc().level(Level.WARNING).text("ErrorLog15")), //
		STATE_120(new Doc().level(Level.WARNING).text("ErrorLog16")), //

		; //
		
		// TODO
				/*
				 * this.power = new SymmetricPowerImpl(100000, setActivePower, setReactivePower, getParent().getBridge());
				this.allowedChargeLimit = new PGreaterEqualLimitation(power);
				this.allowedChargeLimit.setP(this.allowedCharge.valueOptional().orElse(0L));
				this.batFullLimit = new NoPBetweenLimitation(power);
				this.power.addStaticLimitation(batFullLimit);
				this.allowedCharge.addChangeListener(new ChannelChangeListener() {

					@Override
					public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
						allowedChargeLimit.setP(allowedCharge.valueOptional().orElse(0L));
						if (allowedCharge.isValuePresent()) {
							if (allowedCharge.getValue() > -100) {
								batFullLimit.setP(0L, 5000L);
							} else {
								batFullLimit.setP(null, null);
							}
						}
					}
				});
				this.power.addStaticLimitation(this.allowedChargeLimit);
				this.allowedDischargeLimit = new PSmallerEqualLimitation(power);
				this.allowedDischargeLimit.setP(this.allowedDischarge.valueOptional().orElse(0L));
				this.batEmptyLimit = new NoPBetweenLimitation(power);
				this.power.addStaticLimitation(batEmptyLimit);
				this.allowedDischarge.addChangeListener(new ChannelChangeListener() {

					@Override
					public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
						allowedDischargeLimit.setP(allowedDischarge.valueOptional().orElse(0L));
						if (allowedDischarge.isValuePresent()) {
							if(allowedDischarge.getValue() < 100) {
								batEmptyLimit.setP(-5000L, 0L);
							}else {
								batEmptyLimit.setP(null, null);
							}
						}
					}
				});
				return protocol;
				 */

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	private IntegerWriteChannel getSetActivePowerChannel() {
		return this.channel(RefuEss.ChannelId.SET_ACTIVE_POWER);
	}

	private IntegerWriteChannel getSetActivePowerL1Channel() {
		return this.channel(RefuEss.ChannelId.SET_ACTIVE_POWER_L1);
	}

	private IntegerWriteChannel getSetActivePowerL2Channel() {
		return this.channel(RefuEss.ChannelId.SET_ACTIVE_POWER_L2);
	}

	private IntegerWriteChannel getSetActivePowerL3Channel() {
		return this.channel(RefuEss.ChannelId.SET_ACTIVE_POWER_L3);
	}

	private IntegerWriteChannel getSetReactivePowerChannel() {
		return this.channel(RefuEss.ChannelId.SET_REACTIVE_POWER);
	}

	private IntegerWriteChannel getSetReactivePowerL1Channel() {
		return this.channel(RefuEss.ChannelId.SET_REACTIVE_POWER_L1);
	}

	private IntegerWriteChannel getSetReactivePowerL2Channel() {
		return this.channel(RefuEss.ChannelId.SET_REACTIVE_POWER_L2);
	}

	private IntegerWriteChannel getSetReactivePowerL3Channel() {
		return this.channel(RefuEss.ChannelId.SET_REACTIVE_POWER_L3);
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

}
