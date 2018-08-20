package io.openems.edge.fenecon.mini.ess;

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
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.fenecon.mini.FeneconMiniConstants;
import io.openems.edge.fenecon.mini.core.api.FeneconMiniCore;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Fenecon.Mini.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE //
)
public class FeneconMiniEss extends AbstractOpenemsModbusComponent implements SymmetricEss, OpenemsComponent {

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
		super.activate(context, config.service_pid(), config.id(), config.enabled(), FeneconMiniConstants.UNIT_ID, this.cm, "Modbus",
				config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		return new ModbusProtocol(unitId, //
				new FC3ReadRegistersTask(100, Priority.HIGH, //
						m(FeneconMiniCore.ChannelId.SYSTEM_STATE, new UnsignedWordElement(100)), //
						m(FeneconMiniCore.ChannelId.CONTROL_MODE, new UnsignedWordElement(101)), //
						new DummyRegisterElement(102, 103), //
						m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY, new UnsignedDoublewordElement(104)), //
						m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, new UnsignedDoublewordElement(106)), //
						m(FeneconMiniCore.ChannelId.BATTERY_GROUP_STATE, new UnsignedWordElement(108)), //
						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(109)), //
						m(FeneconMiniCore.ChannelId.BATTERY_VOLTAGE, new UnsignedWordElement(110)), //
						m(FeneconMiniCore.ChannelId.BATTERY_CURRENT, new SignedWordElement(111)), //
						m(FeneconMiniCore.ChannelId.BATTERY_POWER, new SignedWordElement(112)), //
						bm(new UnsignedWordElement(113))//
								.m(FeneconMiniCore.ChannelId.STATE_0, 0) //
								.m(FeneconMiniCore.ChannelId.STATE_1, 1) //
								.m(FeneconMiniCore.ChannelId.STATE_2, 2) //
								.m(FeneconMiniCore.ChannelId.STATE_3, 3) //
								.m(FeneconMiniCore.ChannelId.STATE_4, 4) //
								.m(FeneconMiniCore.ChannelId.STATE_5, 5) //
								.m(FeneconMiniCore.ChannelId.STATE_6, 6) //
								.build()), //
				new FC3ReadRegistersTask(114, Priority.HIGH, //
						m(FeneconMiniCore.ChannelId.PCS_OPERATION_STATE, new UnsignedWordElement(114)), //
						new DummyRegisterElement(115, 117), //
						m(FeneconMiniCore.ChannelId.CURRENT, new SignedWordElement(118)), //
						new DummyRegisterElement(119, 120), //
						m(FeneconMiniCore.ChannelId.VOLTAGE, new UnsignedWordElement(121)), //
						new DummyRegisterElement(122, 123), //
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedWordElement(124),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(125, 130), //
						m(FeneconMiniCore.ChannelId.FREQUENCY, new UnsignedWordElement(131)), //
						new DummyRegisterElement(132, 133), //
						m(FeneconMiniCore.ChannelId.PHASE_ALLOWED_APPARENT, new UnsignedWordElement(134))), //
				new FC3ReadRegistersTask(150, Priority.LOW, //
						bm(new UnsignedWordElement(150))//
								.m(FeneconMiniCore.ChannelId.STATE_7, 0)//
								.m(FeneconMiniCore.ChannelId.STATE_8, 1)//
								.m(FeneconMiniCore.ChannelId.STATE_9, 2)//
								.m(FeneconMiniCore.ChannelId.STATE_10, 3)//
								.m(FeneconMiniCore.ChannelId.STATE_11, 4)//
								.m(FeneconMiniCore.ChannelId.STATE_12, 5)//
								.m(FeneconMiniCore.ChannelId.STATE_13, 6)//
								.m(FeneconMiniCore.ChannelId.STATE_14, 7)//
								.m(FeneconMiniCore.ChannelId.STATE_15, 8)//
								.m(FeneconMiniCore.ChannelId.STATE_16, 9)//
								.m(FeneconMiniCore.ChannelId.STATE_17, 10)//
								.build(), //
						bm(new UnsignedWordElement(151))//
								.m(FeneconMiniCore.ChannelId.STATE_18, 0)//
								.build(), //

						bm(new UnsignedWordElement(152))//
								.m(FeneconMiniCore.ChannelId.STATE_19, 0)//
								.m(FeneconMiniCore.ChannelId.STATE_20, 1)//
								.m(FeneconMiniCore.ChannelId.STATE_21, 2)//
								.m(FeneconMiniCore.ChannelId.STATE_22, 3)//
								.m(FeneconMiniCore.ChannelId.STATE_23, 4)//
								.m(FeneconMiniCore.ChannelId.STATE_24, 5)//
								.m(FeneconMiniCore.ChannelId.STATE_25, 6)//
								.m(FeneconMiniCore.ChannelId.STATE_26, 7)//
								.m(FeneconMiniCore.ChannelId.STATE_28, 8)//
								.m(FeneconMiniCore.ChannelId.STATE_29, 9)//
								.m(FeneconMiniCore.ChannelId.STATE_30, 10)//
								.m(FeneconMiniCore.ChannelId.STATE_31, 11)//
								.m(FeneconMiniCore.ChannelId.STATE_32, 12)//
								.m(FeneconMiniCore.ChannelId.STATE_33, 13)//
								.m(FeneconMiniCore.ChannelId.STATE_34, 14)//
								.m(FeneconMiniCore.ChannelId.STATE_35, 15)//
								.build(), //

						bm(new UnsignedWordElement(153))//
								.m(FeneconMiniCore.ChannelId.STATE_36, 0)//
								.m(FeneconMiniCore.ChannelId.STATE_37, 1)//
								.m(FeneconMiniCore.ChannelId.STATE_38, 2)//
								.m(FeneconMiniCore.ChannelId.STATE_39, 3)//
								.m(FeneconMiniCore.ChannelId.STATE_40, 4)//
								.m(FeneconMiniCore.ChannelId.STATE_41, 5)//
								.m(FeneconMiniCore.ChannelId.STATE_42, 6)//
								.m(FeneconMiniCore.ChannelId.STATE_43, 7)//
								.m(FeneconMiniCore.ChannelId.STATE_44, 8)//
								.m(FeneconMiniCore.ChannelId.STATE_45, 9)//
								.m(FeneconMiniCore.ChannelId.STATE_46, 10)//
								.m(FeneconMiniCore.ChannelId.STATE_47, 11)//
								.m(FeneconMiniCore.ChannelId.STATE_48, 12)//
								.m(FeneconMiniCore.ChannelId.STATE_49, 13)//
								.m(FeneconMiniCore.ChannelId.STATE_50, 14)//
								.m(FeneconMiniCore.ChannelId.STATE_51, 15)//
								.build(), //
						bm(new UnsignedWordElement(154))//
								.m(FeneconMiniCore.ChannelId.STATE_52, 0)//
								.m(FeneconMiniCore.ChannelId.STATE_53, 1)//
								.m(FeneconMiniCore.ChannelId.STATE_54, 2)//
								.m(FeneconMiniCore.ChannelId.STATE_55, 3)//
								.m(FeneconMiniCore.ChannelId.STATE_56, 4)//
								.m(FeneconMiniCore.ChannelId.STATE_57, 5)//
								.m(FeneconMiniCore.ChannelId.STATE_58, 6)//
								.m(FeneconMiniCore.ChannelId.STATE_59, 7)//
								.m(FeneconMiniCore.ChannelId.STATE_60, 8)//
								.m(FeneconMiniCore.ChannelId.STATE_61, 9)//
								.m(FeneconMiniCore.ChannelId.STATE_62, 10)//
								.m(FeneconMiniCore.ChannelId.STATE_63, 11)//
								.m(FeneconMiniCore.ChannelId.STATE_64, 12)//
								.build()), //
				new FC3ReadRegistersTask(3000, Priority.HIGH, //
						m(FeneconMiniCore.ChannelId.BECU1_CHARGE_CURRENT, new UnsignedWordElement(3000)), //
						m(FeneconMiniCore.ChannelId.BECU1_DISCHARGE_CURRENT, new UnsignedWordElement(3001)), //
						m(FeneconMiniCore.ChannelId.BECU1_VOLT, new UnsignedWordElement(3002)), //
						m(FeneconMiniCore.ChannelId.BECU1_CURRENT, new UnsignedWordElement(3003)), //
						m(FeneconMiniCore.ChannelId.BECU1_SOC, new UnsignedWordElement(3004))), //
				new FC3ReadRegistersTask(3005, Priority.LOW, //
						bm(new UnsignedWordElement(3005))//
								.m(FeneconMiniCore.ChannelId.STATE_65, 0)//
								.m(FeneconMiniCore.ChannelId.STATE_66, 1)//
								.m(FeneconMiniCore.ChannelId.STATE_67, 2)//
								.m(FeneconMiniCore.ChannelId.STATE_68, 3)//
								.m(FeneconMiniCore.ChannelId.STATE_69, 4)//
								.m(FeneconMiniCore.ChannelId.STATE_70, 5)//
								.m(FeneconMiniCore.ChannelId.STATE_71, 6)//
								.m(FeneconMiniCore.ChannelId.STATE_72, 7)//
								.m(FeneconMiniCore.ChannelId.STATE_73, 8)//
								.m(FeneconMiniCore.ChannelId.STATE_74, 9)//
								.m(FeneconMiniCore.ChannelId.STATE_75, 10)//
								.m(FeneconMiniCore.ChannelId.STATE_76, 11)//
								.m(FeneconMiniCore.ChannelId.STATE_77, 12)//
								.m(FeneconMiniCore.ChannelId.STATE_78, 13)//
								.m(FeneconMiniCore.ChannelId.STATE_79, 14)//
								.m(FeneconMiniCore.ChannelId.STATE_80, 15)//
								.build(), //
						bm(new UnsignedWordElement(3006))//
								.m(FeneconMiniCore.ChannelId.STATE_81, 0)//
								.m(FeneconMiniCore.ChannelId.STATE_82, 1)//
								.m(FeneconMiniCore.ChannelId.STATE_83, 2)//
								.m(FeneconMiniCore.ChannelId.STATE_84, 4)//
								.m(FeneconMiniCore.ChannelId.STATE_85, 5)//
								.m(FeneconMiniCore.ChannelId.STATE_86, 6)//
								.m(FeneconMiniCore.ChannelId.STATE_87, 7)//
								.m(FeneconMiniCore.ChannelId.STATE_88, 8)//
								.m(FeneconMiniCore.ChannelId.STATE_89, 9)//
								.m(FeneconMiniCore.ChannelId.STATE_90, 10)//
								.m(FeneconMiniCore.ChannelId.STATE_91, 11)//
								.m(FeneconMiniCore.ChannelId.STATE_92, 12)//
								.m(FeneconMiniCore.ChannelId.STATE_93, 13)//
								.m(FeneconMiniCore.ChannelId.STATE_94, 14)//
								.m(FeneconMiniCore.ChannelId.STATE_95, 15)//
								.build(), //
						bm(new UnsignedWordElement(3007))//
								.m(FeneconMiniCore.ChannelId.STATE_96, 0)//
								.m(FeneconMiniCore.ChannelId.STATE_97, 1)//
								.m(FeneconMiniCore.ChannelId.STATE_98, 2)//
								.m(FeneconMiniCore.ChannelId.STATE_99, 3)//
								.m(FeneconMiniCore.ChannelId.STATE_100, 4)//
								.m(FeneconMiniCore.ChannelId.STATE_101, 5)//
								.m(FeneconMiniCore.ChannelId.STATE_102, 6)//
								.m(FeneconMiniCore.ChannelId.STATE_103, 7)//
								.m(FeneconMiniCore.ChannelId.STATE_104, 8)//
								.m(FeneconMiniCore.ChannelId.STATE_105, 9)//
								.m(FeneconMiniCore.ChannelId.STATE_106, 10)//
								.m(FeneconMiniCore.ChannelId.STATE_107, 13)//
								.m(FeneconMiniCore.ChannelId.STATE_108, 14)//
								.m(FeneconMiniCore.ChannelId.STATE_109, 15)//
								.build(), //
						bm(new UnsignedWordElement(3008))//
								.m(FeneconMiniCore.ChannelId.STATE_110, 0)//
								.m(FeneconMiniCore.ChannelId.STATE_111, 1)//
								.m(FeneconMiniCore.ChannelId.STATE_112, 2)//
								.m(FeneconMiniCore.ChannelId.STATE_113, 9)//
								.m(FeneconMiniCore.ChannelId.STATE_114, 10)//
								.m(FeneconMiniCore.ChannelId.STATE_115, 12)//
								.m(FeneconMiniCore.ChannelId.STATE_116, 13)//
								.build(), //
						m(FeneconMiniCore.ChannelId.BECU1_VERSION, new UnsignedWordElement(3009)), //
						new DummyRegisterElement(3010, 3011), //
						m(FeneconMiniCore.ChannelId.BECU1_MIN_VOLT_NO, new UnsignedWordElement(3012)), //
						m(FeneconMiniCore.ChannelId.BECU1_MIN_VOLT, new UnsignedWordElement(3013)), //
						m(FeneconMiniCore.ChannelId.BECU1_MAX_VOLT_NO, new UnsignedWordElement(3014)), //
						m(FeneconMiniCore.ChannelId.BECU1_MAX_VOLT, new UnsignedWordElement(3015)), // ^
						m(FeneconMiniCore.ChannelId.BECU1_MIN_TEMP_NO, new UnsignedWordElement(3016)), //
						m(FeneconMiniCore.ChannelId.BECU1_MIN_TEMP, new UnsignedWordElement(3017)), //
						m(FeneconMiniCore.ChannelId.BECU1_MAX_TEMP_NO, new UnsignedWordElement(3018)), //
						m(FeneconMiniCore.ChannelId.BECU1_MAX_TEMP, new UnsignedWordElement(3019))), //
				new FC3ReadRegistersTask(3200, Priority.HIGH, //
						m(FeneconMiniCore.ChannelId.BECU2_CHARGE_CURRENT, new UnsignedWordElement(3200)), //
						m(FeneconMiniCore.ChannelId.BECU2_DISCHARGE_CURRENT, new UnsignedWordElement(3201)), //
						m(FeneconMiniCore.ChannelId.BECU2_VOLT, new UnsignedWordElement(3202)), //
						m(FeneconMiniCore.ChannelId.BECU2_CURRENT, new UnsignedWordElement(3203)), //
						m(FeneconMiniCore.ChannelId.BECU2_SOC, new UnsignedWordElement(3204))), //
				new FC3ReadRegistersTask(3205, Priority.LOW, //
						bm(new UnsignedWordElement(3205))//
								.m(FeneconMiniCore.ChannelId.STATE_117, 0)//
								.m(FeneconMiniCore.ChannelId.STATE_118, 1)//
								.m(FeneconMiniCore.ChannelId.STATE_119, 2)//
								.m(FeneconMiniCore.ChannelId.STATE_120, 3)//
								.m(FeneconMiniCore.ChannelId.STATE_121, 4)//
								.m(FeneconMiniCore.ChannelId.STATE_122, 5)//
								.m(FeneconMiniCore.ChannelId.STATE_123, 6)//
								.m(FeneconMiniCore.ChannelId.STATE_124, 7)//
								.m(FeneconMiniCore.ChannelId.STATE_125, 8)//
								.m(FeneconMiniCore.ChannelId.STATE_126, 9)//
								.m(FeneconMiniCore.ChannelId.STATE_127, 10)//
								.m(FeneconMiniCore.ChannelId.STATE_128, 11)//
								.m(FeneconMiniCore.ChannelId.STATE_129, 12)//
								.m(FeneconMiniCore.ChannelId.STATE_130, 13)//
								.m(FeneconMiniCore.ChannelId.STATE_131, 14)//
								.m(FeneconMiniCore.ChannelId.STATE_132, 15)//
								.build(), //
						bm(new UnsignedWordElement(3206))//
								.m(FeneconMiniCore.ChannelId.STATE_133, 0)//
								.m(FeneconMiniCore.ChannelId.STATE_134, 1)//
								.m(FeneconMiniCore.ChannelId.STATE_135, 2)//
								.m(FeneconMiniCore.ChannelId.STATE_136, 4)//
								.m(FeneconMiniCore.ChannelId.STATE_137, 5)//
								.m(FeneconMiniCore.ChannelId.STATE_138, 6)//
								.m(FeneconMiniCore.ChannelId.STATE_139, 7)//
								.m(FeneconMiniCore.ChannelId.STATE_140, 8)//
								.m(FeneconMiniCore.ChannelId.STATE_141, 9)//
								.m(FeneconMiniCore.ChannelId.STATE_142, 10)//
								.m(FeneconMiniCore.ChannelId.STATE_143, 11)//
								.m(FeneconMiniCore.ChannelId.STATE_144, 12)//
								.m(FeneconMiniCore.ChannelId.STATE_145, 13)//
								.m(FeneconMiniCore.ChannelId.STATE_146, 14)//
								.m(FeneconMiniCore.ChannelId.STATE_147, 15)//
								.build(), //
						bm(new UnsignedWordElement(3207))//
								.m(FeneconMiniCore.ChannelId.STATE_148, 0)//
								.m(FeneconMiniCore.ChannelId.STATE_149, 1)//
								.m(FeneconMiniCore.ChannelId.STATE_150, 2)//
								.m(FeneconMiniCore.ChannelId.STATE_151, 3)//
								.m(FeneconMiniCore.ChannelId.STATE_152, 4)//
								.m(FeneconMiniCore.ChannelId.STATE_153, 5)//
								.m(FeneconMiniCore.ChannelId.STATE_154, 6)//
								.m(FeneconMiniCore.ChannelId.STATE_155, 7)//
								.m(FeneconMiniCore.ChannelId.STATE_156, 8)//
								.m(FeneconMiniCore.ChannelId.STATE_157, 9)//
								.m(FeneconMiniCore.ChannelId.STATE_158, 10)//
								.m(FeneconMiniCore.ChannelId.STATE_159, 13)//
								.m(FeneconMiniCore.ChannelId.STATE_160, 14)//
								.m(FeneconMiniCore.ChannelId.STATE_161, 15)//
								.build(), //
						bm(new UnsignedWordElement(3208))//
								.m(FeneconMiniCore.ChannelId.STATE_162, 0)//
								.m(FeneconMiniCore.ChannelId.STATE_163, 1)//
								.m(FeneconMiniCore.ChannelId.STATE_164, 2)//
								.m(FeneconMiniCore.ChannelId.STATE_165, 9)//
								.m(FeneconMiniCore.ChannelId.STATE_165, 10)//
								.m(FeneconMiniCore.ChannelId.STATE_166, 12)//
								.m(FeneconMiniCore.ChannelId.STATE_167, 13)//
								.build(), //
						m(FeneconMiniCore.ChannelId.BECU2_VERSION, new UnsignedWordElement(3209)), //
						new DummyRegisterElement(3210, 3211), //
						m(FeneconMiniCore.ChannelId.BECU2_MIN_VOLT_NO, new UnsignedWordElement(3212)), //
						m(FeneconMiniCore.ChannelId.BECU2_MIN_VOLT, new UnsignedWordElement(3213)), //
						m(FeneconMiniCore.ChannelId.BECU2_MAX_VOLT_NO, new UnsignedWordElement(3214)), //
						m(FeneconMiniCore.ChannelId.BECU2_MAX_VOLT, new UnsignedWordElement(3215)), // ^
						m(FeneconMiniCore.ChannelId.BECU2_MIN_TEMP_NO, new UnsignedWordElement(3216)), //
						m(FeneconMiniCore.ChannelId.BECU2_MIN_TEMP, new UnsignedWordElement(3217)), //
						m(FeneconMiniCore.ChannelId.BECU2_MAX_TEMP_NO, new UnsignedWordElement(3218)), //
						m(FeneconMiniCore.ChannelId.BECU2_MAX_TEMP, new UnsignedWordElement(3219))), //

//		new FC3ReadRegistersTask(4000, Priority.HIGH, //
//				m(FeneconMiniCore.ChannelId.SET_WORK_STATE, new UnsignedWordElement(4000)), //
//				m(FeneconMiniCore.ChannelId.SYSTEM_WORK_MODE_STATE, new UnsignedWordElement(4000))), //

				new FC3ReadRegistersTask(4800, Priority.HIGH, //
						m(FeneconMiniCore.ChannelId.BECU_NUM, new UnsignedWordElement(4800)), //
						// TODO BECU_WORK_STATE has been implemented with both registers(4801 and 4807)
						m(FeneconMiniCore.ChannelId.BECU_WORK_STATE, new UnsignedWordElement(4801)), //
						new DummyRegisterElement(4802), //
						m(FeneconMiniCore.ChannelId.BECU_CHARGE_CURRENT, new UnsignedWordElement(4803)), //
						m(FeneconMiniCore.ChannelId.BECU_DISCHARGE_CURRENT, new UnsignedWordElement(4804)), //
						m(FeneconMiniCore.ChannelId.BECU_VOLT, new UnsignedWordElement(4805)), //
						m(FeneconMiniCore.ChannelId.BECU_CURRENT, new UnsignedWordElement(4806)), //
						m(FeneconMiniCore.ChannelId.BECU_SOC, new UnsignedWordElement(4807))), //
				new FC3ReadRegistersTask(4808, Priority.LOW, //
						bm(new UnsignedWordElement(4808))//
								.m(FeneconMiniCore.ChannelId.STATE_168, 0)//
								.m(FeneconMiniCore.ChannelId.STATE_169, 1)//
								.m(FeneconMiniCore.ChannelId.STATE_170, 2)//
								.m(FeneconMiniCore.ChannelId.STATE_171, 3)//
								.m(FeneconMiniCore.ChannelId.STATE_172, 4)//
								.m(FeneconMiniCore.ChannelId.STATE_173, 9)//
								.build(), //
						bm(new UnsignedWordElement(4809))//
								.m(FeneconMiniCore.ChannelId.STATE_174, 0)//
								.m(FeneconMiniCore.ChannelId.STATE_175, 1)//
								.build(), //
						bm(new UnsignedWordElement(4810))//
								.m(FeneconMiniCore.ChannelId.STATE_176, 0)//
								.m(FeneconMiniCore.ChannelId.STATE_177, 1)//
								.m(FeneconMiniCore.ChannelId.STATE_178, 2)//
								.m(FeneconMiniCore.ChannelId.STATE_179, 3)//
								.m(FeneconMiniCore.ChannelId.STATE_180, 4)//
								.m(FeneconMiniCore.ChannelId.STATE_181, 5)//
								.m(FeneconMiniCore.ChannelId.STATE_182, 6)//
								.m(FeneconMiniCore.ChannelId.STATE_183, 7)//
								.m(FeneconMiniCore.ChannelId.STATE_184, 8)//
								.m(FeneconMiniCore.ChannelId.STATE_185, 9)//
								.m(FeneconMiniCore.ChannelId.STATE_186, 10)//
								.m(FeneconMiniCore.ChannelId.STATE_187, 13)//
								.m(FeneconMiniCore.ChannelId.STATE_188, 14)//
								.m(FeneconMiniCore.ChannelId.STATE_189, 15)//
								.build(), //
						bm(new UnsignedWordElement(4811))//
								.m(FeneconMiniCore.ChannelId.STATE_190, 0)//
								.m(FeneconMiniCore.ChannelId.STATE_191, 1)//
								.m(FeneconMiniCore.ChannelId.STATE_192, 2)//
								.m(FeneconMiniCore.ChannelId.STATE_193, 3)//
								.m(FeneconMiniCore.ChannelId.STATE_194, 4)//
								.m(FeneconMiniCore.ChannelId.STATE_195, 5)//
								.m(FeneconMiniCore.ChannelId.STATE_196, 6)//
								.m(FeneconMiniCore.ChannelId.STATE_197, 7)//
								.m(FeneconMiniCore.ChannelId.STATE_198, 8)//
								.m(FeneconMiniCore.ChannelId.STATE_199, 9)//
								.m(FeneconMiniCore.ChannelId.STATE_200, 10)//
								.m(FeneconMiniCore.ChannelId.STATE_201, 13)//
								.m(FeneconMiniCore.ChannelId.STATE_202, 14)//
								.build()), //
				new FC3ReadRegistersTask(30166, Priority.HIGH, //
						m(SymmetricEss.ChannelId.GRID_MODE, new UnsignedWordElement(30166))), //
				new FC16WriteRegistersTask(9014, //
						m(FeneconMiniCore.ChannelId.RTC_YEAR, new UnsignedWordElement(9014)), //
						m(FeneconMiniCore.ChannelId.RTC_MONTH, new UnsignedWordElement(9015)), //
						m(FeneconMiniCore.ChannelId.RTC_DAY, new UnsignedWordElement(9016)), //
						m(FeneconMiniCore.ChannelId.RTC_HOUR, new UnsignedWordElement(9017)), //
						m(FeneconMiniCore.ChannelId.RTC_MINUTE, new UnsignedWordElement(9018)), //
						m(FeneconMiniCore.ChannelId.RTC_SECOND, new UnsignedWordElement(9019))), //
				new FC16WriteRegistersTask(30558, //
						m(FeneconMiniCore.ChannelId.SET_SETUP_MODE, new UnsignedWordElement(30558))), //
				new FC16WriteRegistersTask(30559, //
						m(FeneconMiniCore.ChannelId.SET_PCS_MODE, new UnsignedWordElement(30559))), //

				new FC16WriteRegistersTask(30157, //
						m(FeneconMiniCore.ChannelId.SETUP_MODE, new UnsignedWordElement(30157)), //
						m(FeneconMiniCore.ChannelId.PCS_MODE, new UnsignedWordElement(30158))));//
	}

}
