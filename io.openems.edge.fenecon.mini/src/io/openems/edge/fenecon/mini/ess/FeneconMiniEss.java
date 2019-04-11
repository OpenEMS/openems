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
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.SinglePhase;
import io.openems.edge.ess.api.SinglePhaseEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.fenecon.mini.FeneconMiniConstants;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Fenecon.Mini.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class FeneconMiniEss extends AbstractOpenemsModbusComponent
		implements SinglePhaseEss, AsymmetricEss, SymmetricEss, OpenemsComponent, ModbusSlave {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private SinglePhase phase;

	public FeneconMiniEss() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				SinglePhaseEss.ChannelId.values(), //
				EssChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled(), FeneconMiniConstants.UNIT_ID, this.cm, "Modbus",
				config.modbus_id());
		this.phase = config.phase();
		SinglePhaseEss.initializeCopyPhaseChannel(this, config.phase());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public SinglePhase getPhase() {
		return this.phase;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(100, Priority.LOW, //
						m(EssChannelId.SYSTEM_STATE, new UnsignedWordElement(100)), //
						m(EssChannelId.CONTROL_MODE, new UnsignedWordElement(101)), //
						new DummyRegisterElement(102, 103), //
						m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY, new UnsignedDoublewordElement(104)), //
						m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, new UnsignedDoublewordElement(106)), //
						m(EssChannelId.BATTERY_GROUP_STATE, new UnsignedWordElement(108)), //
						new DummyRegisterElement(109), //
						m(EssChannelId.BATTERY_VOLTAGE, new UnsignedWordElement(110)), //
						m(EssChannelId.BATTERY_CURRENT, new SignedWordElement(111)), //
						m(EssChannelId.BATTERY_POWER, new SignedWordElement(112))), //
				new FC3ReadRegistersTask(2007, Priority.HIGH, //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L1, new UnsignedWordElement(2007),
								UNSIGNED_POWER_CONVERTER)), //
				new FC3ReadRegistersTask(2107, Priority.HIGH, //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L2, new UnsignedWordElement(2107),
								UNSIGNED_POWER_CONVERTER)), //
				new FC3ReadRegistersTask(2207, Priority.HIGH, //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L3, new UnsignedWordElement(2207),
								UNSIGNED_POWER_CONVERTER)), //
				new FC3ReadRegistersTask(3000, Priority.LOW, //
						m(EssChannelId.BECU1_CHARGE_CURRENT, new UnsignedWordElement(3000)), //
						m(EssChannelId.BECU1_DISCHARGE_CURRENT, new UnsignedWordElement(3001)), //
						m(EssChannelId.BECU1_VOLT, new UnsignedWordElement(3002)), //
						m(EssChannelId.BECU1_CURRENT, new UnsignedWordElement(3003)), //
						m(EssChannelId.BECU1_SOC, new UnsignedWordElement(3004))), //
				new FC3ReadRegistersTask(3005, Priority.LOW, //
						bm(new UnsignedWordElement(3005))//
								.m(EssChannelId.STATE_1, 0)//
								.m(EssChannelId.STATE_2, 1)//
								.m(EssChannelId.STATE_3, 2)//
								.m(EssChannelId.STATE_4, 3)//
								.m(EssChannelId.STATE_5, 4)//
								.m(EssChannelId.STATE_6, 5)//
								.m(EssChannelId.STATE_7, 6)//
								.m(EssChannelId.STATE_8, 7)//
								.m(EssChannelId.STATE_9, 8)//
								.m(EssChannelId.STATE_10, 9)//
								.m(EssChannelId.STATE_11, 10)//
								.m(EssChannelId.STATE_12, 11)//
								.m(EssChannelId.STATE_13, 12)//
								.m(EssChannelId.STATE_14, 13)//
								.m(EssChannelId.STATE_15, 14)//
								.m(EssChannelId.STATE_16, 15)//
								.build(), //
						bm(new UnsignedWordElement(3006))//
								.m(EssChannelId.STATE_17, 0)//
								.m(EssChannelId.STATE_18, 1)//
								.m(EssChannelId.STATE_19, 2)//
								.m(EssChannelId.STATE_20, 4)//
								.m(EssChannelId.STATE_21, 5)//
								.m(EssChannelId.STATE_22, 6)//
								.m(EssChannelId.STATE_23, 7)//
								.m(EssChannelId.STATE_24, 8)//
								.m(EssChannelId.STATE_25, 9)//
								.m(EssChannelId.STATE_26, 10)//
								.m(EssChannelId.STATE_27, 11)//
								.m(EssChannelId.STATE_28, 12)//
								.m(EssChannelId.STATE_29, 13)//
								.m(EssChannelId.STATE_30, 14)//
								.m(EssChannelId.STATE_31, 15)//
								.build(), //
						bm(new UnsignedWordElement(3007))//
								.m(EssChannelId.STATE_32, 0)//
								.m(EssChannelId.STATE_33, 1)//
								.m(EssChannelId.STATE_34, 2)//
								.m(EssChannelId.STATE_35, 3)//
								.m(EssChannelId.STATE_36, 4)//
								.m(EssChannelId.STATE_37, 5)//
								.m(EssChannelId.STATE_38, 6)//
								.m(EssChannelId.STATE_39, 7)//
								.m(EssChannelId.STATE_40, 8)//
								.m(EssChannelId.STATE_41, 9)//
								.m(EssChannelId.STATE_42, 10)//
								.m(EssChannelId.STATE_43, 13)//
								.m(EssChannelId.STATE_44, 14)//
								.m(EssChannelId.STATE_45, 15)//
								.build(), //
						bm(new UnsignedWordElement(3008))//
								.m(EssChannelId.STATE_46, 0)//
								.m(EssChannelId.STATE_47, 1)//
								.m(EssChannelId.STATE_48, 2)//
								.m(EssChannelId.STATE_49, 9)//
								.m(EssChannelId.STATE_50, 10)//
								.m(EssChannelId.STATE_51, 12)//
								.m(EssChannelId.STATE_52, 13)//
								.build(), //
						m(EssChannelId.BECU1_VERSION, new UnsignedWordElement(3009)), //
						new DummyRegisterElement(3010, 3011), //
						m(EssChannelId.BECU1_MIN_VOLT_NO, new UnsignedWordElement(3012)), //
						m(EssChannelId.BECU1_MIN_VOLT, new UnsignedWordElement(3013)), //
						m(EssChannelId.BECU1_MAX_VOLT_NO, new UnsignedWordElement(3014)), //
						m(EssChannelId.BECU1_MAX_VOLT, new UnsignedWordElement(3015)), // ^
						m(EssChannelId.BECU1_MIN_TEMP_NO, new UnsignedWordElement(3016)), //
						m(EssChannelId.BECU1_MIN_TEMP, new UnsignedWordElement(3017)), //
						m(EssChannelId.BECU1_MAX_TEMP_NO, new UnsignedWordElement(3018)), //
						m(EssChannelId.BECU1_MAX_TEMP, new UnsignedWordElement(3019))), //

				new FC3ReadRegistersTask(3200, Priority.LOW, //
						m(EssChannelId.BECU2_CHARGE_CURRENT, new UnsignedWordElement(3200)), //
						m(EssChannelId.BECU2_DISCHARGE_CURRENT, new UnsignedWordElement(3201)), //
						m(EssChannelId.BECU2_VOLT, new UnsignedWordElement(3202)), //
						m(EssChannelId.BECU2_CURRENT, new UnsignedWordElement(3203)), //
						m(EssChannelId.BECU2_SOC, new UnsignedWordElement(3204))), //
				new FC3ReadRegistersTask(3205, Priority.LOW, //
						bm(new UnsignedWordElement(3205))//
								.m(EssChannelId.STATE_53, 0)//
								.m(EssChannelId.STATE_54, 1)//
								.m(EssChannelId.STATE_55, 2)//
								.m(EssChannelId.STATE_56, 3)//
								.m(EssChannelId.STATE_57, 4)//
								.m(EssChannelId.STATE_58, 5)//
								.m(EssChannelId.STATE_59, 6)//
								.m(EssChannelId.STATE_60, 7)//
								.m(EssChannelId.STATE_61, 8)//
								.m(EssChannelId.STATE_62, 9)//
								.m(EssChannelId.STATE_63, 10)//
								.m(EssChannelId.STATE_64, 11)//
								.m(EssChannelId.STATE_65, 12)//
								.m(EssChannelId.STATE_66, 13)//
								.m(EssChannelId.STATE_67, 14)//
								.m(EssChannelId.STATE_68, 15)//
								.build(), //
						bm(new UnsignedWordElement(3206))//
								.m(EssChannelId.STATE_69, 0)//
								.m(EssChannelId.STATE_70, 1)//
								.m(EssChannelId.STATE_71, 2)//
								.m(EssChannelId.STATE_72, 4)//
								.m(EssChannelId.STATE_73, 5)//
								.m(EssChannelId.STATE_74, 6)//
								.m(EssChannelId.STATE_75, 7)//
								.m(EssChannelId.STATE_76, 8)//
								.m(EssChannelId.STATE_77, 9)//
								.m(EssChannelId.STATE_78, 10)//
								.m(EssChannelId.STATE_79, 11)//
								.m(EssChannelId.STATE_80, 12)//
								.m(EssChannelId.STATE_81, 13)//
								.m(EssChannelId.STATE_82, 14)//
								.m(EssChannelId.STATE_83, 15)//
								.build(), //
						bm(new UnsignedWordElement(3207))//
								.m(EssChannelId.STATE_84, 0)//
								.m(EssChannelId.STATE_85, 1)//
								.m(EssChannelId.STATE_86, 2)//
								.m(EssChannelId.STATE_87, 3)//
								.m(EssChannelId.STATE_88, 4)//
								.m(EssChannelId.STATE_89, 5)//
								.m(EssChannelId.STATE_90, 6)//
								.m(EssChannelId.STATE_91, 7)//
								.m(EssChannelId.STATE_92, 8)//
								.m(EssChannelId.STATE_93, 9)//
								.m(EssChannelId.STATE_94, 10)//
								.m(EssChannelId.STATE_95, 13)//
								.m(EssChannelId.STATE_96, 14)//
								.m(EssChannelId.STATE_97, 15)//
								.build(), //
						bm(new UnsignedWordElement(3208))//
								.m(EssChannelId.STATE_98, 0)//
								.m(EssChannelId.STATE_99, 1)//
								.m(EssChannelId.STATE_100, 2)//
								.m(EssChannelId.STATE_101, 9)//
								.m(EssChannelId.STATE_102, 10)//
								.m(EssChannelId.STATE_103, 12)//
								.m(EssChannelId.STATE_104, 13)//
								.build(), //
						m(EssChannelId.BECU2_VERSION, new UnsignedWordElement(3209)), //
						new DummyRegisterElement(3210, 3211), //
						m(EssChannelId.BECU2_MIN_VOLT_NO, new UnsignedWordElement(3212)), //
						m(EssChannelId.BECU2_MIN_VOLT, new UnsignedWordElement(3213)), //
						m(EssChannelId.BECU2_MAX_VOLT_NO, new UnsignedWordElement(3214)), //
						m(EssChannelId.BECU2_MAX_VOLT, new UnsignedWordElement(3215)), // ^
						m(EssChannelId.BECU2_MIN_TEMP_NO, new UnsignedWordElement(3216)), //
						m(EssChannelId.BECU2_MIN_TEMP, new UnsignedWordElement(3217)), //
						m(EssChannelId.BECU2_MAX_TEMP_NO, new UnsignedWordElement(3218)), //
						m(EssChannelId.BECU2_MAX_TEMP, new UnsignedWordElement(3219))), //
				new FC3ReadRegistersTask(4000, Priority.LOW, //
						m(EssChannelId.SYSTEM_WORK_STATE, new UnsignedDoublewordElement(4000)), //
						m(EssChannelId.SYSTEM_WORK_MODE_STATE, new UnsignedDoublewordElement(4002))), //
				new FC3ReadRegistersTask(4800, Priority.LOW, //
						m(EssChannelId.BECU_NUM, new UnsignedWordElement(4800)), //
						// TODO BECU_WORK_STATE has been implemented with both registers(4801 and 4807)
						m(EssChannelId.BECU_WORK_STATE, new UnsignedWordElement(4801)), //
						new DummyRegisterElement(4802), //
						m(EssChannelId.BECU_CHARGE_CURRENT, new UnsignedWordElement(4803)), //
						m(EssChannelId.BECU_DISCHARGE_CURRENT, new UnsignedWordElement(4804)), //
						m(EssChannelId.BECU_VOLT, new UnsignedWordElement(4805)), //
						m(EssChannelId.BECU_CURRENT, new UnsignedWordElement(4806))), //
				new FC3ReadRegistersTask(4808, Priority.LOW, //
						bm(new UnsignedWordElement(4808))//
								.m(EssChannelId.STATE_105, 0)//
								.m(EssChannelId.STATE_106, 1)//
								.m(EssChannelId.STATE_107, 2)//
								.m(EssChannelId.STATE_108, 3)//
								.m(EssChannelId.STATE_109, 4)//
								.m(EssChannelId.STATE_110, 9)//
								.build(), //
						bm(new UnsignedWordElement(4809))//
								.m(EssChannelId.STATE_111, 0)//
								.m(EssChannelId.STATE_112, 1)//
								.build(), //
						bm(new UnsignedWordElement(4810))//
								.m(EssChannelId.STATE_113, 0)//
								.m(EssChannelId.STATE_114, 1)//
								.m(EssChannelId.STATE_115, 2)//
								.m(EssChannelId.STATE_116, 3)//
								.m(EssChannelId.STATE_117, 4)//
								.m(EssChannelId.STATE_118, 5)//
								.m(EssChannelId.STATE_119, 6)//
								.m(EssChannelId.STATE_120, 7)//
								.m(EssChannelId.STATE_121, 8)//
								.m(EssChannelId.STATE_122, 9)//
								.m(EssChannelId.STATE_123, 10)//
								.m(EssChannelId.STATE_124, 11)//
								.m(EssChannelId.STATE_125, 12)//
								.m(EssChannelId.STATE_126, 13)//
								.m(EssChannelId.STATE_127, 14)//
								.m(EssChannelId.STATE_128, 15)//
								.build(), //
						bm(new UnsignedWordElement(4811))//
								.m(EssChannelId.STATE_129, 0)//
								.m(EssChannelId.STATE_130, 1)//
								.m(EssChannelId.STATE_131, 2)//
								.m(EssChannelId.STATE_132, 3)//
								.m(EssChannelId.STATE_133, 4)//
								.m(EssChannelId.STATE_134, 5)//
								.m(EssChannelId.STATE_135, 6)//
								.m(EssChannelId.STATE_136, 7)//
								.m(EssChannelId.STATE_137, 8)//
								.m(EssChannelId.STATE_138, 9)//
								.m(EssChannelId.STATE_139, 10)//
								.m(EssChannelId.STATE_140, 11)//
								.m(EssChannelId.STATE_141, 12)//
								.m(EssChannelId.STATE_142, 13)//
								.m(EssChannelId.STATE_143, 14)//
								.build(),

						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(4812))//
				), //

				new FC3ReadRegistersTask(30166, Priority.LOW, //
						m(SymmetricEss.ChannelId.GRID_MODE, new UnsignedWordElement(30166))), //
				new FC16WriteRegistersTask(9014, //
						m(EssChannelId.RTC_YEAR, new UnsignedWordElement(9014)), //
						m(EssChannelId.RTC_MONTH, new UnsignedWordElement(9015)), //
						m(EssChannelId.RTC_DAY, new UnsignedWordElement(9016)), //
						m(EssChannelId.RTC_HOUR, new UnsignedWordElement(9017)), //
						m(EssChannelId.RTC_MINUTE, new UnsignedWordElement(9018)), //
						m(EssChannelId.RTC_SECOND, new UnsignedWordElement(9019))), //
				new FC16WriteRegistersTask(30558, //
						m(EssChannelId.SETUP_MODE, new UnsignedWordElement(30558))), //
				new FC16WriteRegistersTask(30559, //
						m(EssChannelId.PCS_MODE, new UnsignedWordElement(30559))), //
				new FC16WriteRegistersTask(30157, //
						m(EssChannelId.SETUP_MODE, new UnsignedWordElement(30157)), //
						m(EssChannelId.PCS_MODE, new UnsignedWordElement(30158))));//
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value().asString() //
				+ "|L:" + this.getActivePower().value().asString(); //
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable() {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(), //
				SymmetricEss.getModbusSlaveNatureTable(), //
				AsymmetricEss.getModbusSlaveNatureTable(), //
				ModbusSlaveNatureTable.of(FeneconMiniEss.class, 300) //
						.build());
	}

	private final static ElementToChannelConverter UNSIGNED_POWER_CONVERTER = new ElementToChannelConverter( //
			// element -> channel
			value -> {
				if (value == null) {
					return null;
				}
				int intValue = (Integer) value;
				if (intValue == 0) {
					return 0; // ignore '0'
				}
				return intValue - 10_000; // apply delta of 10_000
			}, //
				// channel -> element
			value -> value);
}