package io.openems.edge.fenecon.mini.ess;

import java.time.LocalDateTime;

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
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.fenecon.mini.FeneconMiniConstants;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Fenecon.Mini.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE //
)
public class FeneconMiniEss extends AbstractOpenemsModbusComponent
		implements SymmetricEss, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(FeneconMiniEss.class);

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public FeneconMiniEss() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled(), FeneconMiniConstants.UNIT_ID,
				this.cm, "Modbus", config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		return new ModbusProtocol(unitId, //
				new FC3ReadRegistersTask(100, Priority.HIGH, //
						m(FeneconMiniEss.ChannelId.SYSTEM_STATE, new UnsignedWordElement(100)), //
						m(FeneconMiniEss.ChannelId.CONTROL_MODE, new UnsignedWordElement(101)), //
						new DummyRegisterElement(102, 103), //
						m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY, new UnsignedDoublewordElement(104)), //
						m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, new UnsignedDoublewordElement(106)), //
						m(FeneconMiniEss.ChannelId.BATTERY_GROUP_STATE, new UnsignedWordElement(108)), //
						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(109)), //
						m(FeneconMiniEss.ChannelId.BATTERY_VOLTAGE, new UnsignedWordElement(110)), //
						m(FeneconMiniEss.ChannelId.BATTERY_CURRENT, new SignedWordElement(111)), //
						m(FeneconMiniEss.ChannelId.BATTERY_POWER, new SignedWordElement(112)), //
						bm(new UnsignedWordElement(113))//
								.m(FeneconMiniEss.ChannelId.STATE_0, 0) //
								.m(FeneconMiniEss.ChannelId.STATE_1, 1) //
								.m(FeneconMiniEss.ChannelId.STATE_2, 2) //
								.m(FeneconMiniEss.ChannelId.STATE_3, 3) //
								.m(FeneconMiniEss.ChannelId.STATE_4, 4) //
								.m(FeneconMiniEss.ChannelId.STATE_5, 5) //
								.m(FeneconMiniEss.ChannelId.STATE_6, 6) //
								.build()), //
				new FC3ReadRegistersTask(114, Priority.HIGH, //
						m(FeneconMiniEss.ChannelId.PCS_OPERATION_STATE, new UnsignedWordElement(114)), //
						new DummyRegisterElement(115, 117), //
						m(FeneconMiniEss.ChannelId.CURRENT, new SignedWordElement(118)), //
						new DummyRegisterElement(119, 120), //
						m(FeneconMiniEss.ChannelId.VOLTAGE, new UnsignedWordElement(121)), //
						new DummyRegisterElement(122, 123), //
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedWordElement(124),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(125, 130), //
						m(FeneconMiniEss.ChannelId.FREQUENCY, new UnsignedWordElement(131)), //
						new DummyRegisterElement(132, 133), //
						m(FeneconMiniEss.ChannelId.PHASE_ALLOWED_APPARENT, new UnsignedWordElement(134))), //
				new FC3ReadRegistersTask(150, Priority.LOW, //
						bm(new UnsignedWordElement(150))//
								.m(FeneconMiniEss.ChannelId.STATE_7, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_8, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_9, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_10, 3)//
								.m(FeneconMiniEss.ChannelId.STATE_11, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_12, 5)//
								.m(FeneconMiniEss.ChannelId.STATE_13, 6)//
								.m(FeneconMiniEss.ChannelId.STATE_14, 7)//
								.m(FeneconMiniEss.ChannelId.STATE_15, 8)//
								.m(FeneconMiniEss.ChannelId.STATE_16, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_17, 10)//
								.build(), //
						bm(new UnsignedWordElement(151))//
								.m(FeneconMiniEss.ChannelId.STATE_18, 0)//
								.build(), //

						bm(new UnsignedWordElement(152))//
								.m(FeneconMiniEss.ChannelId.STATE_19, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_20, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_21, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_22, 3)//
								.m(FeneconMiniEss.ChannelId.STATE_23, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_24, 5)//
								.m(FeneconMiniEss.ChannelId.STATE_25, 6)//
								.m(FeneconMiniEss.ChannelId.STATE_26, 7)//
								.m(FeneconMiniEss.ChannelId.STATE_28, 8)//
								.m(FeneconMiniEss.ChannelId.STATE_29, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_30, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_31, 11)//
								.m(FeneconMiniEss.ChannelId.STATE_32, 12)//
								.m(FeneconMiniEss.ChannelId.STATE_33, 13)//
								.m(FeneconMiniEss.ChannelId.STATE_34, 14)//
								.m(FeneconMiniEss.ChannelId.STATE_35, 15)//
								.build(), //

						bm(new UnsignedWordElement(153))//
								.m(FeneconMiniEss.ChannelId.STATE_36, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_37, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_38, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_39, 3)//
								.m(FeneconMiniEss.ChannelId.STATE_40, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_41, 5)//
								.m(FeneconMiniEss.ChannelId.STATE_42, 6)//
								.m(FeneconMiniEss.ChannelId.STATE_43, 7)//
								.m(FeneconMiniEss.ChannelId.STATE_44, 8)//
								.m(FeneconMiniEss.ChannelId.STATE_45, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_46, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_47, 11)//
								.m(FeneconMiniEss.ChannelId.STATE_48, 12)//
								.m(FeneconMiniEss.ChannelId.STATE_49, 13)//
								.m(FeneconMiniEss.ChannelId.STATE_50, 14)//
								.m(FeneconMiniEss.ChannelId.STATE_51, 15)//
								.build(), //
						bm(new UnsignedWordElement(154))//
								.m(FeneconMiniEss.ChannelId.STATE_52, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_53, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_54, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_55, 3)//
								.m(FeneconMiniEss.ChannelId.STATE_56, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_57, 5)//
								.m(FeneconMiniEss.ChannelId.STATE_58, 6)//
								.m(FeneconMiniEss.ChannelId.STATE_59, 7)//
								.m(FeneconMiniEss.ChannelId.STATE_60, 8)//
								.m(FeneconMiniEss.ChannelId.STATE_61, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_62, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_63, 11)//
								.m(FeneconMiniEss.ChannelId.STATE_64, 12)//
								.build()), //
				new FC3ReadRegistersTask(3000, Priority.HIGH, //
						m(FeneconMiniEss.ChannelId.BECU1_CHARGE_CURRENT, new UnsignedWordElement(3000)), //
						m(FeneconMiniEss.ChannelId.BECU1_DISCHARGE_CURRENT, new UnsignedWordElement(3001)), //
						m(FeneconMiniEss.ChannelId.BECU1_VOLT, new UnsignedWordElement(3002)), //
						m(FeneconMiniEss.ChannelId.BECU1_CURRENT, new UnsignedWordElement(3003)), //
						m(FeneconMiniEss.ChannelId.BECU1_SOC, new UnsignedWordElement(3004))), //
				new FC3ReadRegistersTask(3005, Priority.LOW, //
						bm(new UnsignedWordElement(3005))//
								.m(FeneconMiniEss.ChannelId.STATE_65, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_66, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_67, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_68, 3)//
								.m(FeneconMiniEss.ChannelId.STATE_69, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_70, 5)//
								.m(FeneconMiniEss.ChannelId.STATE_71, 6)//
								.m(FeneconMiniEss.ChannelId.STATE_72, 7)//
								.m(FeneconMiniEss.ChannelId.STATE_73, 8)//
								.m(FeneconMiniEss.ChannelId.STATE_74, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_75, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_76, 11)//
								.m(FeneconMiniEss.ChannelId.STATE_77, 12)//
								.m(FeneconMiniEss.ChannelId.STATE_78, 13)//
								.m(FeneconMiniEss.ChannelId.STATE_79, 14)//
								.m(FeneconMiniEss.ChannelId.STATE_80, 15)//
								.build(), //
						bm(new UnsignedWordElement(3006))//
								.m(FeneconMiniEss.ChannelId.STATE_81, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_82, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_83, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_84, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_85, 5)//
								.m(FeneconMiniEss.ChannelId.STATE_86, 6)//
								.m(FeneconMiniEss.ChannelId.STATE_87, 7)//
								.m(FeneconMiniEss.ChannelId.STATE_88, 8)//
								.m(FeneconMiniEss.ChannelId.STATE_89, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_90, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_91, 11)//
								.m(FeneconMiniEss.ChannelId.STATE_92, 12)//
								.m(FeneconMiniEss.ChannelId.STATE_93, 13)//
								.m(FeneconMiniEss.ChannelId.STATE_94, 14)//
								.m(FeneconMiniEss.ChannelId.STATE_95, 15)//
								.build(), //
						bm(new UnsignedWordElement(3007))//
								.m(FeneconMiniEss.ChannelId.STATE_96, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_97, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_98, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_99, 3)//
								.m(FeneconMiniEss.ChannelId.STATE_100, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_101, 5)//
								.m(FeneconMiniEss.ChannelId.STATE_102, 6)//
								.m(FeneconMiniEss.ChannelId.STATE_103, 7)//
								.m(FeneconMiniEss.ChannelId.STATE_104, 8)//
								.m(FeneconMiniEss.ChannelId.STATE_105, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_106, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_107, 13)//
								.m(FeneconMiniEss.ChannelId.STATE_108, 14)//
								.m(FeneconMiniEss.ChannelId.STATE_109, 15)//
								.build(), //
						bm(new UnsignedWordElement(3008))//
								.m(FeneconMiniEss.ChannelId.STATE_110, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_111, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_112, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_113, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_114, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_115, 12)//
								.m(FeneconMiniEss.ChannelId.STATE_116, 13)//
								.build(), //
						m(FeneconMiniEss.ChannelId.BECU1_VERSION, new UnsignedWordElement(3009)), //
						new DummyRegisterElement(3010, 3011), //
						m(FeneconMiniEss.ChannelId.BECU1_MIN_VOLT_NO, new UnsignedWordElement(3012)), //
						m(FeneconMiniEss.ChannelId.BECU1_MIN_VOLT, new UnsignedWordElement(3013)), //
						m(FeneconMiniEss.ChannelId.BECU1_MAX_VOLT_NO, new UnsignedWordElement(3014)), //
						m(FeneconMiniEss.ChannelId.BECU1_MAX_VOLT, new UnsignedWordElement(3015)), // ^
						m(FeneconMiniEss.ChannelId.BECU1_MIN_TEMP_NO, new UnsignedWordElement(3016)), //
						m(FeneconMiniEss.ChannelId.BECU1_MIN_TEMP, new UnsignedWordElement(3017)), //
						m(FeneconMiniEss.ChannelId.BECU1_MAX_TEMP_NO, new UnsignedWordElement(3018)), //
						m(FeneconMiniEss.ChannelId.BECU1_MAX_TEMP, new UnsignedWordElement(3019))), //
				new FC3ReadRegistersTask(3200, Priority.HIGH, //
						m(FeneconMiniEss.ChannelId.BECU2_CHARGE_CURRENT, new UnsignedWordElement(3200)), //
						m(FeneconMiniEss.ChannelId.BECU2_DISCHARGE_CURRENT, new UnsignedWordElement(3201)), //
						m(FeneconMiniEss.ChannelId.BECU2_VOLT, new UnsignedWordElement(3202)), //
						m(FeneconMiniEss.ChannelId.BECU2_CURRENT, new UnsignedWordElement(3203)), //
						m(FeneconMiniEss.ChannelId.BECU2_SOC, new UnsignedWordElement(3204))), //
				new FC3ReadRegistersTask(3205, Priority.LOW, //
						bm(new UnsignedWordElement(3205))//
								.m(FeneconMiniEss.ChannelId.STATE_117, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_118, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_119, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_120, 3)//
								.m(FeneconMiniEss.ChannelId.STATE_121, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_122, 5)//
								.m(FeneconMiniEss.ChannelId.STATE_123, 6)//
								.m(FeneconMiniEss.ChannelId.STATE_124, 7)//
								.m(FeneconMiniEss.ChannelId.STATE_125, 8)//
								.m(FeneconMiniEss.ChannelId.STATE_126, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_127, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_128, 11)//
								.m(FeneconMiniEss.ChannelId.STATE_129, 12)//
								.m(FeneconMiniEss.ChannelId.STATE_130, 13)//
								.m(FeneconMiniEss.ChannelId.STATE_131, 14)//
								.m(FeneconMiniEss.ChannelId.STATE_132, 15)//
								.build(), //
						bm(new UnsignedWordElement(3206))//
								.m(FeneconMiniEss.ChannelId.STATE_133, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_134, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_135, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_136, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_137, 5)//
								.m(FeneconMiniEss.ChannelId.STATE_138, 6)//
								.m(FeneconMiniEss.ChannelId.STATE_139, 7)//
								.m(FeneconMiniEss.ChannelId.STATE_140, 8)//
								.m(FeneconMiniEss.ChannelId.STATE_141, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_142, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_143, 11)//
								.m(FeneconMiniEss.ChannelId.STATE_144, 12)//
								.m(FeneconMiniEss.ChannelId.STATE_145, 13)//
								.m(FeneconMiniEss.ChannelId.STATE_146, 14)//
								.m(FeneconMiniEss.ChannelId.STATE_147, 15)//
								.build(), //
						bm(new UnsignedWordElement(3207))//
								.m(FeneconMiniEss.ChannelId.STATE_148, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_149, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_150, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_151, 3)//
								.m(FeneconMiniEss.ChannelId.STATE_152, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_153, 5)//
								.m(FeneconMiniEss.ChannelId.STATE_154, 6)//
								.m(FeneconMiniEss.ChannelId.STATE_155, 7)//
								.m(FeneconMiniEss.ChannelId.STATE_156, 8)//
								.m(FeneconMiniEss.ChannelId.STATE_157, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_158, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_159, 13)//
								.m(FeneconMiniEss.ChannelId.STATE_160, 14)//
								.m(FeneconMiniEss.ChannelId.STATE_161, 15)//
								.build(), //
						bm(new UnsignedWordElement(3208))//
								.m(FeneconMiniEss.ChannelId.STATE_162, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_163, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_164, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_165, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_165, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_166, 12)//
								.m(FeneconMiniEss.ChannelId.STATE_167, 13)//
								.build(), //
						m(FeneconMiniEss.ChannelId.BECU2_VERSION, new UnsignedWordElement(3209)), //
						new DummyRegisterElement(3210, 3211), //
						m(FeneconMiniEss.ChannelId.BECU2_MIN_VOLT_NO, new UnsignedWordElement(3212)), //
						m(FeneconMiniEss.ChannelId.BECU2_MIN_VOLT, new UnsignedWordElement(3213)), //
						m(FeneconMiniEss.ChannelId.BECU2_MAX_VOLT_NO, new UnsignedWordElement(3214)), //
						m(FeneconMiniEss.ChannelId.BECU2_MAX_VOLT, new UnsignedWordElement(3215)), // ^
						m(FeneconMiniEss.ChannelId.BECU2_MIN_TEMP_NO, new UnsignedWordElement(3216)), //
						m(FeneconMiniEss.ChannelId.BECU2_MIN_TEMP, new UnsignedWordElement(3217)), //
						m(FeneconMiniEss.ChannelId.BECU2_MAX_TEMP_NO, new UnsignedWordElement(3218)), //
						m(FeneconMiniEss.ChannelId.BECU2_MAX_TEMP, new UnsignedWordElement(3219))), //

//		new FC3ReadRegistersTask(4000, Priority.HIGH, //
//				m(FeneconMiniCore.ChannelId.SET_WORK_STATE, new UnsignedWordElement(4000)), //
//				m(FeneconMiniCore.ChannelId.SYSTEM_WORK_MODE_STATE, new UnsignedWordElement(4000))), //

				// this.activePower = new ModbusReadLongChannel("ActivePower",
				// this).unit("W").negate()
//				.ignore(-10000l))),
				new FC3ReadRegistersTask(4004, Priority.HIGH, //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedWordElement(4004))//
				), //
				new FC3ReadRegistersTask(5003, Priority.HIGH, //
						m(FeneconMiniEss.ChannelId.SELL_TO_GRID_ENERGY, new UnsignedDoublewordElement(5003)), //
						m(FeneconMiniEss.ChannelId.BUY_FROM_GRID_ENERGY, new UnsignedDoublewordElement(5005))//
				), //

				new FC3ReadRegistersTask(4800, Priority.HIGH, //
						m(FeneconMiniEss.ChannelId.BECU_NUM, new UnsignedWordElement(4800)), //
						// TODO BECU_WORK_STATE has been implemented with both registers(4801 and 4807)
						m(FeneconMiniEss.ChannelId.BECU_WORK_STATE, new UnsignedWordElement(4801)), //
						new DummyRegisterElement(4802), //
						m(FeneconMiniEss.ChannelId.BECU_CHARGE_CURRENT, new UnsignedWordElement(4803)), //
						m(FeneconMiniEss.ChannelId.BECU_DISCHARGE_CURRENT, new UnsignedWordElement(4804)), //
						m(FeneconMiniEss.ChannelId.BECU_VOLT, new UnsignedWordElement(4805)), //
						m(FeneconMiniEss.ChannelId.BECU_CURRENT, new UnsignedWordElement(4806)), //
						m(FeneconMiniEss.ChannelId.BECU_SOC, new UnsignedWordElement(4807))), //
				new FC3ReadRegistersTask(4808, Priority.LOW, //
						bm(new UnsignedWordElement(4808))//
								.m(FeneconMiniEss.ChannelId.STATE_168, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_169, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_170, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_171, 3)//
								.m(FeneconMiniEss.ChannelId.STATE_172, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_173, 9)//
								.build(), //
						bm(new UnsignedWordElement(4809))//
								.m(FeneconMiniEss.ChannelId.STATE_174, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_175, 1)//
								.build(), //
						bm(new UnsignedWordElement(4810))//
								.m(FeneconMiniEss.ChannelId.STATE_176, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_177, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_178, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_179, 3)//
								.m(FeneconMiniEss.ChannelId.STATE_180, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_181, 5)//
								.m(FeneconMiniEss.ChannelId.STATE_182, 6)//
								.m(FeneconMiniEss.ChannelId.STATE_183, 7)//
								.m(FeneconMiniEss.ChannelId.STATE_184, 8)//
								.m(FeneconMiniEss.ChannelId.STATE_185, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_186, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_187, 13)//
								.m(FeneconMiniEss.ChannelId.STATE_188, 14)//
								.m(FeneconMiniEss.ChannelId.STATE_189, 15)//
								.build(), //
						bm(new UnsignedWordElement(4811))//
								.m(FeneconMiniEss.ChannelId.STATE_190, 0)//
								.m(FeneconMiniEss.ChannelId.STATE_191, 1)//
								.m(FeneconMiniEss.ChannelId.STATE_192, 2)//
								.m(FeneconMiniEss.ChannelId.STATE_193, 3)//
								.m(FeneconMiniEss.ChannelId.STATE_194, 4)//
								.m(FeneconMiniEss.ChannelId.STATE_195, 5)//
								.m(FeneconMiniEss.ChannelId.STATE_196, 6)//
								.m(FeneconMiniEss.ChannelId.STATE_197, 7)//
								.m(FeneconMiniEss.ChannelId.STATE_198, 8)//
								.m(FeneconMiniEss.ChannelId.STATE_199, 9)//
								.m(FeneconMiniEss.ChannelId.STATE_200, 10)//
								.m(FeneconMiniEss.ChannelId.STATE_201, 13)//
								.m(FeneconMiniEss.ChannelId.STATE_202, 14)//
								.build()), //
				new FC3ReadRegistersTask(30166, Priority.HIGH, //
						m(SymmetricEss.ChannelId.GRID_MODE, new UnsignedWordElement(30166))), //
				new FC16WriteRegistersTask(9014, //
						m(FeneconMiniEss.ChannelId.RTC_YEAR, new UnsignedWordElement(9014)), //
						m(FeneconMiniEss.ChannelId.RTC_MONTH, new UnsignedWordElement(9015)), //
						m(FeneconMiniEss.ChannelId.RTC_DAY, new UnsignedWordElement(9016)), //
						m(FeneconMiniEss.ChannelId.RTC_HOUR, new UnsignedWordElement(9017)), //
						m(FeneconMiniEss.ChannelId.RTC_MINUTE, new UnsignedWordElement(9018)), //
						m(FeneconMiniEss.ChannelId.RTC_SECOND, new UnsignedWordElement(9019))), //
				new FC16WriteRegistersTask(30558, //
						m(FeneconMiniEss.ChannelId.SET_SETUP_MODE, new UnsignedWordElement(30558))), //
				new FC16WriteRegistersTask(30559, //
						m(FeneconMiniEss.ChannelId.SET_PCS_MODE, new UnsignedWordElement(30559))), //

				new FC16WriteRegistersTask(30157, //
						m(FeneconMiniEss.ChannelId.SETUP_MODE, new UnsignedWordElement(30157)), //
						m(FeneconMiniEss.ChannelId.PCS_MODE, new UnsignedWordElement(30158))));//
	}

	enum SetWorkState {
		LOCAL_CONTROL, START, REMOTE_CONTROL_OF_GRID, STOP, EMERGENCY_STOP
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		SET_WORK_STATE(new Doc() //
				.option(0, SetWorkState.LOCAL_CONTROL)//
				.option(1, SetWorkState.START) //
				.option(2, SetWorkState.REMOTE_CONTROL_OF_GRID) //
				.option(3, SetWorkState.STOP) //
				.option(4, SetWorkState.EMERGENCY_STOP)), //
		SYSTEM_WORK_MODE_STATE(new Doc()), //
		SYSTEM_STATE(new Doc() //
				.option(0, "STANDBY") //
				.option(1, "Start Off-Grid") //
				.option(2, "START") //
				.option(3, "FAULT") //
				.option(4, "Off-Grd PV")), //
		CONTROL_MODE(new Doc()//
				.option(1, "Remote")//
				.option(2, "Local")), //

		BATTERY_GROUP_STATE(new Doc()//
				.option(0, "Initial")//
				.option(1, "Stop")//
				.option(2, "Starting")//
				.option(3, "Running")//
				.option(4, "Stopping")//
				.option(5, "Fail")//
		), //
		
		SELL_TO_GRID_ENERGY(new Doc().unit(Unit.WATT_HOURS)),//
		BUY_FROM_GRID_ENERGY(new Doc().unit(Unit.WATT_HOURS)),//
		ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		SET_ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		SET_REACTIVE_POWER(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		BATTERY_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
		BATTERY_POWER(new Doc().unit(Unit.WATT)), //
		CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
		VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		FREQUENCY(new Doc().unit(Unit.HERTZ)), //
		PHASE_ALLOWED_APPARENT(new Doc().unit(Unit.VOLT_AMPERE)), //

		PCS_OPERATION_STATE(new Doc()//
				.option(0, "Self-checking")//
				.option(1, "Standby")//
				.option(2, "Off-Grid PV")//
				.option(3, "Off-Grid")//
				.option(4, "ON_GRID")//
				.option(5, "Fail")//
				.option(6, "ByPass 1")//
				.option(7, "ByPass 2")), //
		RTC_YEAR(new Doc().text("Year")), //
		RTC_MONTH(new Doc().text("Month")), //
		RTC_DAY(new Doc().text("Day")), //
		RTC_HOUR(new Doc().text("Hour")), //
		RTC_MINUTE(new Doc().text("Minute")), //
		RTC_SECOND(new Doc().text("Second")), //
		SET_SETUP_MODE(new Doc()//
				.option(0, "OFF")//
				.option(1, "ON")), //
		SET_PCS_MODE(new Doc()//
				.option(0, "Emergency")//
				.option(1, "ConsumersPeakPattern")//
				.option(2, "Economic")//
				.option(3, "Eco")//
				.option(4, "Debug")//
				.option(5, "SmoothPv")//
				.option(6, "Remote")), //
		SETUP_MODE(new Doc()//
				.option(0, "OFF")//
				.option(1, "ON")), //
		PCS_MODE(new Doc()//
				.option(0, "Emergency")//
				.option(1, "ConsumersPeakPattern")//
				.option(2, "Economic")//
				.option(3, "Eco")//
				.option(4, "Debug")//
				.option(5, "SmoothPv")//
				.option(6, "Remote")//

		), //

		BECU_NUM(new Doc()), //
		BECU_WORK_STATE(new Doc()), //
		BECU_CHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU_DISCHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU_VOLT(new Doc().unit(Unit.VOLT)), //
		BECU_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU_SOC(new Doc().unit(Unit.PERCENT)), //

		BECU1_CHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU1_DISCHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU1_VOLT(new Doc().unit(Unit.VOLT)), //
		BECU1_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU1_SOC(new Doc().unit(Unit.PERCENT)), //

		BECU2_CHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU2_DISCHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU2_VOLT(new Doc().unit(Unit.VOLT)), //
		BECU2_CURRENT(new Doc().unit(Unit.AMPERE)), //
		BECU2_SOC(new Doc().unit(Unit.PERCENT)), //

		BECU1_VERSION(new Doc()), //
		BECU1_MIN_VOLT_NO(new Doc()), //
		BECU1_MIN_VOLT(new Doc().unit(Unit.VOLT)), //
		BECU1_MAX_VOLT_NO(new Doc()), //
		BECU1_MAX_VOLT(new Doc().unit(Unit.VOLT)), //
		BECU1_MIN_TEMP_NO(new Doc()), //
		BECU1_MIN_TEMP(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BECU1_MAX_TEMP_NO(new Doc()), //
		BECU1_MAX_TEMP(new Doc().unit(Unit.DEGREE_CELSIUS)), //

		BECU2_VERSION(new Doc()), //
		BECU2_MIN_VOLT_NO(new Doc()), //
		BECU2_MIN_VOLT(new Doc().unit(Unit.VOLT)), //
		BECU2_MAX_VOLT_NO(new Doc()), //
		BECU2_MAX_VOLT(new Doc().unit(Unit.VOLT)), //
		BECU2_MIN_TEMP_NO(new Doc()), //
		BECU2_MIN_TEMP(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BECU2_MAX_TEMP_NO(new Doc()), //
		BECU2_MAX_TEMP(new Doc().unit(Unit.DEGREE_CELSIUS)), //

		STATE_0(new Doc().level(Level.WARNING).text("FailTheSystemShouldBeStopped")), //
		STATE_1(new Doc().level(Level.WARNING).text("CommonLowVoltageAlarm")), //
		STATE_2(new Doc().level(Level.WARNING).text("CommonHighVoltageAlarm")), //
		STATE_3(new Doc().level(Level.WARNING).text("ChargingOverCurrentAlarm")), //
		STATE_4(new Doc().level(Level.WARNING).text("DischargingOverCurrentAlarm")), //
		STATE_5(new Doc().level(Level.WARNING).text("OverTemperatureAlarm")), //
		STATE_6(new Doc().level(Level.WARNING).text("InteralCommunicationAbnormal")), //
		STATE_7(new Doc().level(Level.WARNING).text("GridUndervoltage")), //
		STATE_8(new Doc().level(Level.WARNING).text("GridOvervoltage")), //
		STATE_9(new Doc().level(Level.WARNING).text("")), //
		STATE_10(new Doc().level(Level.WARNING).text("GridUnderFrequency")), //
		STATE_11(new Doc().level(Level.WARNING).text("GridOverFrequency")), //
		STATE_12(new Doc().level(Level.WARNING).text("GridPowerSupplyOff")), //
		STATE_13(new Doc().level(Level.WARNING).text("GridConditionUnmeet")), //
		STATE_14(new Doc().level(Level.WARNING).text("DCUnderVoltage")), //
		STATE_15(new Doc().level(Level.WARNING).text("InputOverResistance")), //
		STATE_16(new Doc().level(Level.WARNING).text("CombinationError")), //
		STATE_17(new Doc().level(Level.WARNING).text("CommWithInverterError")), //
		STATE_18(new Doc().level(Level.WARNING).text("TmeError")), //
		STATE_19(new Doc().level(Level.WARNING).text("PcsAlarm2")), //
		STATE_20(new Doc().level(Level.FAULT).text("ControlCurrentOverload100Percent")), //
		STATE_21(new Doc().level(Level.FAULT).text("ControlCurrentOverload110Percent")), //
		STATE_22(new Doc().level(Level.FAULT).text("ControlCurrentOverload150Percent")), //
		STATE_23(new Doc().level(Level.FAULT).text("ControlCurrentOverload200Percent")), //
		STATE_24(new Doc().level(Level.FAULT).text("ControlCurrentOverload120Percent")), //
		STATE_25(new Doc().level(Level.FAULT).text("ControlCurrentOverload300Percent")), //
		STATE_26(new Doc().level(Level.FAULT).text("ControlTransientLoad300Percent")), //
		STATE_27(new Doc().level(Level.FAULT).text("GridOverCurrent")), //
		STATE_28(new Doc().level(Level.FAULT).text("LockingWaveformTooManyTimes")), //
		STATE_29(new Doc().level(Level.FAULT).text("InverterVoltageZeroDriftError")), //
		STATE_30(new Doc().level(Level.FAULT).text("GridVoltageZeroDriftError")), //
		STATE_31(new Doc().level(Level.FAULT).text("ControlCurrentZeroDriftError")), //
		STATE_32(new Doc().level(Level.FAULT).text("InverterCurrentZeroDriftError")), //
		STATE_33(new Doc().level(Level.FAULT).text("GridCurrentZeroDriftError")), //
		STATE_34(new Doc().level(Level.FAULT).text("PDPProtection")), //
		STATE_35(new Doc().level(Level.FAULT).text("HardwareControlCurrentProtection")), //
		STATE_36(new Doc().level(Level.FAULT).text("HardwareACVoltProtection")), //
		STATE_37(new Doc().level(Level.FAULT).text("HardwareDCCurrentProtection")), //
		STATE_38(new Doc().level(Level.FAULT).text("HardwareTemperatureProtection")), //
		STATE_39(new Doc().level(Level.FAULT).text("NoCapturingSignal")), //
		STATE_40(new Doc().level(Level.FAULT).text("DCOvervoltage")), //
		STATE_41(new Doc().level(Level.FAULT).text("DCDisconnected")), //
		STATE_42(new Doc().level(Level.FAULT).text("InverterUndervoltage")), //
		STATE_43(new Doc().level(Level.FAULT).text("InverterOvervoltage")), //
		STATE_44(new Doc().level(Level.FAULT).text("CurrentSensorFail")), //
		STATE_45(new Doc().level(Level.FAULT).text("VoltageSensorFail")), //
		STATE_46(new Doc().level(Level.FAULT).text("PowerUncontrollable")), //
		STATE_47(new Doc().level(Level.FAULT).text("CurrentUncontrollable")), //
		STATE_48(new Doc().level(Level.FAULT).text("FanError")), //
		STATE_49(new Doc().level(Level.FAULT).text("PhaseLack")), //
		STATE_50(new Doc().level(Level.FAULT).text("InverterRelayFault")), //
		STATE_51(new Doc().level(Level.FAULT).text("GridRelayFault")), //
		STATE_52(new Doc().level(Level.FAULT).text("ControlPanelOvertemp")), //
		STATE_53(new Doc().level(Level.FAULT).text("PowerPanelOvertemp")), //
		STATE_54(new Doc().level(Level.FAULT).text("DCInputOvercurrent")), //
		STATE_55(new Doc().level(Level.FAULT).text("CapacitorOvertemp")), //
		STATE_56(new Doc().level(Level.FAULT).text("RadiatorOvertemp")), //
		STATE_57(new Doc().level(Level.FAULT).text("TransformerOvertemp")), //
		STATE_58(new Doc().level(Level.FAULT).text("CombinationCommError")), //
		STATE_59(new Doc().level(Level.FAULT).text("EEPROMError")), //
		STATE_60(new Doc().level(Level.FAULT).text("LoadCurrentZeroDriftError")), //
		STATE_61(new Doc().level(Level.FAULT).text("CurrentLimitRError")), //
		STATE_62(new Doc().level(Level.FAULT).text("PhaseSyncError")), //
		STATE_63(new Doc().level(Level.FAULT).text("ExternalPVCurrentZeroDriftError")), //
		STATE_64(new Doc().level(Level.FAULT).text("ExternalGridCurrentZeroDriftError")), //
		STATE_65(new Doc().level(Level.WARNING).text("BECU1GeneralChargeOverCurrentAlarm")), //
		STATE_66(new Doc().level(Level.WARNING).text("BECU1GeneralDischargeOverCurrentAlarm")), //
		STATE_67(new Doc().level(Level.WARNING).text("BECU1ChargeCurrentLimitAlarm")), //
		STATE_68(new Doc().level(Level.WARNING).text("BECU1DischargeCurrentLimitAlarm")), //
		STATE_69(new Doc().level(Level.WARNING).text("BECU1GeneralHighVoltageAlarm")), //
		STATE_70(new Doc().level(Level.WARNING).text("BECU1GeneralLowVoltageAlarm")), //
		STATE_71(new Doc().level(Level.WARNING).text("BECU1AbnormalVoltageChangeAlarm")), //
		STATE_72(new Doc().level(Level.WARNING).text("BECU1GeneralHighTemperatureAlarm")), //
		STATE_73(new Doc().level(Level.WARNING).text("BECU1GeneralLowTemperatureAlarm")), //
		STATE_74(new Doc().level(Level.WARNING).text("BECU1AbnormalTemperatureChangeAlarm")), //
		STATE_75(new Doc().level(Level.WARNING).text("BECU1SevereHighVoltageAlarm")), //
		STATE_76(new Doc().level(Level.WARNING).text("BECU1SevereLowVoltageAlarm")), //
		STATE_77(new Doc().level(Level.WARNING).text("BECU1SevereLowTemperatureAlarm")), //
		STATE_78(new Doc().level(Level.WARNING).text("BECU1SeverveChargeOverCurrentAlarm")), //
		STATE_79(new Doc().level(Level.WARNING).text("BECU1SeverveDischargeOverCurrentAlarm")), //
		STATE_80(new Doc().level(Level.WARNING).text("BECU1AbnormalCellCapacityAlarm")), //
		STATE_81(new Doc().level(Level.WARNING).text("BECU1BalancedSamplingAlarm")), //
		STATE_82(new Doc().level(Level.WARNING).text("BECU1BalancedControlAlarm")), //
		STATE_83(new Doc().level(Level.WARNING).text("BECU1HallSensorDoesNotWorkAccurately")), //
		STATE_84(new Doc().level(Level.WARNING).text("BECU1Generalleakage")), //
		STATE_85(new Doc().level(Level.WARNING).text("BECU1Severeleakage")), //
		STATE_86(new Doc().level(Level.WARNING).text("BECU1Contactor1TurnOnAbnormity")), //
		STATE_87(new Doc().level(Level.WARNING).text("BECU1Contactor1TurnOffAbnormity")), //
		STATE_88(new Doc().level(Level.WARNING).text("BECU1Contactor2TurnOnAbnormity")), //
		STATE_89(new Doc().level(Level.WARNING).text("BECU1Contactor2TurnOffAbnormity")), //
		STATE_90(new Doc().level(Level.WARNING).text("BECU1Contactor4CheckAbnormity")), //
		STATE_91(new Doc().level(Level.WARNING).text("BECU1ContactorCurrentUnsafe")), //
		STATE_92(new Doc().level(Level.WARNING).text("BECU1Contactor5CheckAbnormity")), //
		STATE_93(new Doc().level(Level.WARNING).text("BECU1HighVoltageOffset")), //
		STATE_94(new Doc().level(Level.WARNING).text("BECU1LowVoltageOffset")), //
		STATE_95(new Doc().level(Level.WARNING).text("BECU1HighTemperatureOffset")), //
		STATE_96(new Doc().level(Level.FAULT).text("BECU1DischargeSevereOvercurrent")), //
		STATE_97(new Doc().level(Level.FAULT).text("BECU1ChargeSevereOvercurrent")), //
		STATE_98(new Doc().level(Level.FAULT).text("BECU1GeneralUndervoltage")), //
		STATE_99(new Doc().level(Level.FAULT).text("BECU1SevereOvervoltage")), //
		STATE_100(new Doc().level(Level.FAULT).text("BECU1GeneralOvervoltage")), //
		STATE_101(new Doc().level(Level.FAULT).text("BECU1SevereUndervoltage")), //
		STATE_102(new Doc().level(Level.FAULT).text("BECU1InsideCANBroken")), //
		STATE_103(new Doc().level(Level.FAULT).text("BECU1GeneralUndervoltageHighCurrentDischarge")), //
		STATE_104(new Doc().level(Level.FAULT).text("BECU1BMUError")), //
		STATE_105(new Doc().level(Level.FAULT).text("BECU1CurrentSamplingInvalidation")), //
		STATE_106(new Doc().level(Level.FAULT).text("BECU1BatteryFail")), //
		STATE_107(new Doc().level(Level.FAULT).text("BECU1TemperatureSamplingBroken")), //
		STATE_108(new Doc().level(Level.FAULT).text("BECU1Contactor1TestBackIsAbnormalTurnOnAbnormity")), //
		STATE_109(new Doc().level(Level.FAULT).text("BECU1Contactor1TestBackIsAbnormalTurnOffAbnormity")), //
		STATE_110(new Doc().level(Level.FAULT).text("BECU1Contactor2TestBackIsAbnormalTurnOnAbnormity")), //
		STATE_111(new Doc().level(Level.FAULT).text("BECU1Contactor2TestBackIsAbnormalTurnOffAbnormity")), //
		STATE_112(new Doc().level(Level.FAULT).text("BECU1SevereHighTemperatureFault")), //
		STATE_113(new Doc().level(Level.FAULT).text("BECU1HallInvalidation")), //
		STATE_114(new Doc().level(Level.FAULT).text("BECU1ContactorInvalidation")), //
		STATE_115(new Doc().level(Level.FAULT).text("BECU1OutsideCANBroken")), //
		STATE_116(new Doc().level(Level.FAULT).text("BECU1CathodeContactorBroken")), //

		STATE_117(new Doc().level(Level.WARNING).text("BECU2GeneralChargeOverCurrentAlarm")), //
		STATE_118(new Doc().level(Level.WARNING).text("BECU2GeneralDischargeOverCurrentAlarm")), //
		STATE_119(new Doc().level(Level.WARNING).text("BECU2ChargeCurrentLimitAlarm")), //
		STATE_120(new Doc().level(Level.WARNING).text("BECU2DischargeCurrentLimitAlarm")), //
		STATE_121(new Doc().level(Level.WARNING).text("BECU2GeneralHighVoltageAlarm")), //
		STATE_122(new Doc().level(Level.WARNING).text("BECU2GeneralLowVoltageAlarm")), //
		STATE_123(new Doc().level(Level.WARNING).text("BECU2AbnormalVoltageChangeAlarm")), //
		STATE_124(new Doc().level(Level.WARNING).text("BECU2GeneralHighTemperatureAlarm")), //
		STATE_125(new Doc().level(Level.WARNING).text("BECU2GeneralLowTemperatureAlarm")), //
		STATE_126(new Doc().level(Level.WARNING).text("BECU2AbnormalTemperatureChangeAlarm")), //
		STATE_127(new Doc().level(Level.WARNING).text("BECU2SevereHighVoltageAlarm")), //
		STATE_128(new Doc().level(Level.WARNING).text("BECU2SevereLowVoltageAlarm")), //
		STATE_129(new Doc().level(Level.WARNING).text("BECU2SevereLowTemperatureAlarm")), //
		STATE_130(new Doc().level(Level.WARNING).text("BECU2SeverveChargeOverCurrentAlarm")), //
		STATE_131(new Doc().level(Level.WARNING).text("BECU2SeverveDischargeOverCurrentAlarm")), //
		STATE_132(new Doc().level(Level.WARNING).text("BECU2AbnormalCellCapacityAlarm")), //
		STATE_133(new Doc().level(Level.WARNING).text("BECU2BalancedSamplingAlarm")), //
		STATE_134(new Doc().level(Level.WARNING).text("BECU2BalancedControlAlarm")), //
		STATE_135(new Doc().level(Level.WARNING).text("BECU2HallSensorDoesNotWorkAccurately")), //
		STATE_136(new Doc().level(Level.WARNING).text("BECU2Generalleakage")), //
		STATE_137(new Doc().level(Level.WARNING).text("BECU2Severeleakage")), //
		STATE_138(new Doc().level(Level.WARNING).text("BECU2Contactor1TurnOnAbnormity")), //
		STATE_139(new Doc().level(Level.WARNING).text("BECU2Contactor1TurnOffAbnormity")), //
		STATE_140(new Doc().level(Level.WARNING).text("BECU2Contactor2TurnOnAbnormity")), //
		STATE_141(new Doc().level(Level.WARNING).text("BECU2Contactor2TurnOffAbnormity")), //
		STATE_142(new Doc().level(Level.WARNING).text("BECU2Contactor4CheckAbnormity")), //
		STATE_143(new Doc().level(Level.WARNING).text("BECU2ContactorCurrentUnsafe")), //
		STATE_144(new Doc().level(Level.WARNING).text("BECU2Contactor5CheckAbnormity")), //
		STATE_145(new Doc().level(Level.WARNING).text("BECU2HighVoltageOffset")), //
		STATE_146(new Doc().level(Level.WARNING).text("BECU2LowVoltageOffset")), //
		STATE_147(new Doc().level(Level.WARNING).text("BECU2HighTemperatureOffset")), //
		STATE_148(new Doc().level(Level.FAULT).text("BECU2DischargeSevereOvercurrent")), //
		STATE_149(new Doc().level(Level.FAULT).text("BECU2ChargeSevereOvercurrent")), //
		STATE_150(new Doc().level(Level.FAULT).text("BECU2GeneralUndervoltage")), //
		STATE_151(new Doc().level(Level.FAULT).text("BECU2SevereOvervoltage")), //
		STATE_152(new Doc().level(Level.FAULT).text("BECU2GeneralOvervoltage")), //
		STATE_153(new Doc().level(Level.FAULT).text("BECU2SevereUndervoltage")), //
		STATE_154(new Doc().level(Level.FAULT).text("BECU2InsideCANBroken")), //
		STATE_155(new Doc().level(Level.FAULT).text("BECU2GeneralUndervoltageHighCurrentDischarge")), //
		STATE_156(new Doc().level(Level.FAULT).text("BECU2BMUError")), //
		STATE_157(new Doc().level(Level.FAULT).text("BECU2CurrentSamplingInvalidation")), //
		STATE_158(new Doc().level(Level.FAULT).text("BECU2BatteryFail")), //
		STATE_159(new Doc().level(Level.FAULT).text("BECU2TemperatureSamplingBroken")), //
		STATE_160(new Doc().level(Level.FAULT).text("BECU2Contactor1TestBackIsAbnormalTurnOnAbnormity")), //
		STATE_161(new Doc().level(Level.FAULT).text("BECU2Contactor1TestBackIsAbnormalTurnOffAbnormity")), //
		STATE_162(new Doc().level(Level.FAULT).text("BECU2Contactor2TestBackIsAbnormalTurnOnAbnormity")), //
		STATE_163(new Doc().level(Level.FAULT).text("BECU2Contactor2TestBackIsAbnormalTurnOffAbnormity")), //
		STATE_164(new Doc().level(Level.FAULT).text("BECU2SevereHighTemperatureFault")), //
		STATE_165(new Doc().level(Level.FAULT).text("BECU2HallInvalidation")), //
		STATE_166(new Doc().level(Level.FAULT).text("BECU2ContactorInvalidation")), //
		STATE_167(new Doc().level(Level.FAULT).text("BECU2OutsideCANBroken")), //
		STATE_168(new Doc().level(Level.FAULT).text("BECU2CathodeContactorBroken")), //

		STATE_169(new Doc().level(Level.FAULT).text("NoAvailableBatteryGroup")), //
		STATE_170(new Doc().level(Level.FAULT).text("StackGeneralLeakage")), //
		STATE_171(new Doc().level(Level.FAULT).text("StackSevereLeakage")), //
		STATE_172(new Doc().level(Level.FAULT).text("StackStartingFail")), //
		STATE_173(new Doc().level(Level.FAULT).text("StackStoppingFail")), //
		STATE_174(new Doc().level(Level.FAULT).text("BatteryProtection")), //
		STATE_175(new Doc().level(Level.FAULT).text("StackAndGroup1CANCommunicationInterrupt")), //
		STATE_176(new Doc().level(Level.FAULT).text("StackAndGroup2CANCommunicationInterrupt")), //
		STATE_177(new Doc().level(Level.WARNING).text("GeneralOvercurrentAlarmAtCellStackCharge")), //
		STATE_178(new Doc().level(Level.WARNING).text("GeneralOvercurrentAlarmAtCellStackDischarge")), //
		STATE_179(new Doc().level(Level.WARNING).text("CurrentLimitAlarmAtCellStackCharge")), //
		STATE_180(new Doc().level(Level.WARNING).text("CurrentLimitAlarmAtCellStackDischarge")), //
		STATE_181(new Doc().level(Level.WARNING).text("GeneralCellStackHighVoltageAlarm")), //
		STATE_182(new Doc().level(Level.WARNING).text("GeneralCellStackLowVoltageAlarm")), //
		STATE_183(new Doc().level(Level.WARNING).text("AbnormalCellStackVoltageChangeAlarm")), //
		STATE_184(new Doc().level(Level.WARNING).text("GeneralCellStackHighTemperatureAlarm")), //
		STATE_185(new Doc().level(Level.WARNING).text("GeneralCellStackLowTemperatureAlarm")), //
		STATE_186(new Doc().level(Level.WARNING).text("AbnormalCellStackTemperatureChangeAlarm")), //
		STATE_187(new Doc().level(Level.WARNING).text("SevereCellStackHighVoltageAlarm")), //
		STATE_188(new Doc().level(Level.WARNING).text("SevereCellStackLowVoltageAlarm")), //
		STATE_189(new Doc().level(Level.WARNING).text("SevereCellStackLowTemperatureAlarm")), //
		STATE_190(new Doc().level(Level.WARNING).text("SeverveOverCurrentAlarmAtCellStackDharge")), //
		STATE_191(new Doc().level(Level.WARNING).text("SeverveOverCurrentAlarmAtCellStackDischarge")), //
		STATE_192(new Doc().level(Level.WARNING).text("AbnormalCellStackCapacityAlarm")), //
		STATE_193(new Doc().level(Level.WARNING).text("TheParameterOfEEPROMInCellStackLoseEffectiveness")), //
		STATE_194(new Doc().level(Level.WARNING).text("IsolatingSwitchInConfluenceArkBreak")), //
		STATE_195(
				new Doc().level(Level.WARNING).text("TheCommunicationBetweenCellStackAndTemperatureOfCollectorBreak")), //
		STATE_196(new Doc().level(Level.WARNING).text("TheTemperatureOfCollectorFail")), //
		STATE_197(new Doc().level(Level.WARNING).text("HallSensorDoNotWorkAccurately")), //
		STATE_198(new Doc().level(Level.WARNING).text("TheCommunicationOfPCSBreak")), //
		STATE_199(new Doc().level(Level.WARNING).text("AdvancedChargingOrMainContactorCloseAbnormally")), //
		STATE_200(new Doc().level(Level.WARNING).text("AbnormalSampledVoltage")), //
		STATE_201(new Doc().level(Level.WARNING).text("AbnormalAdvancedContactorOrAbnormalRS485GalleryOfPCS")), //
		STATE_202(new Doc().level(Level.WARNING).text("AbnormalMainContactor")), //
		STATE_203(new Doc().level(Level.WARNING).text("GeneralCellStackLeakage")), //
		STATE_204(new Doc().level(Level.WARNING).text("SevereCellStackLeakage")), //
		STATE_205(new Doc().level(Level.WARNING).text("SmokeAlarm")), //
		STATE_206(new Doc().level(Level.WARNING).text("TheCommunicationWireToAmmeterBreak")), //
		STATE_207(new Doc().level(Level.WARNING).text("TheCommunicationWireToDredBreak")//
		); //

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
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			this.defineWorkState();
			break;
		}
	}

	private LocalDateTime lastDefineWorkState = null;

	private void defineWorkState() {
		/*
		 * Set ESS in running mode
		 */
		// TODO this should be smarter: set in energy saving mode if there was no output
		// power for a while and we don't need emergency power.
		LocalDateTime now = LocalDateTime.now();
		if (lastDefineWorkState == null || now.minusMinutes(1).isAfter(this.lastDefineWorkState)) {
			this.lastDefineWorkState = now;
			IntegerWriteChannel setWorkStateChannel = this.channel(ChannelId.SET_WORK_STATE);
			try {
				int startOption = setWorkStateChannel.channelDoc().getOption(SetWorkState.START);
				setWorkStateChannel.setNextWriteValue(startOption);
			} catch (OpenemsException e) {
				logError(this.log, "Unable to start: " + e.getMessage());
			}
		}
	}

}
