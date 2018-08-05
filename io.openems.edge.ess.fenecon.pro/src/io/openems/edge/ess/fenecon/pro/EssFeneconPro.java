package io.openems.edge.ess.fenecon.pro;

import java.time.LocalDateTime;

import org.apache.commons.math3.optim.linear.Relationship;
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
import io.openems.common.types.OpenemsType;
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
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.CircleConstraint;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.ConstraintType;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.Fenecon.Pro", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
)

public class EssFeneconPro extends AbstractOpenemsModbusComponent
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(EssFeneconPro.class);

	protected final static int MAX_APPARENT_POWER = 40000;

	private final static int UNIT_ID = 100;
	private final static int MIN_REACTIVE_POWER = -10000;
	private final static int MAX_REACTIVE_POWER = 10000;

	private String modbusBridgeId;

	@Reference
	private Power power;

	@Reference
	protected ConfigurationAdmin cm;

	public EssFeneconPro() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
		IntegerWriteChannel setActivePowerChannel = this.channel(ChannelId.SET_ACTIVE_POWERL1);
		IntegerWriteChannel setReactivePowerChannel = this.channel(ChannelId.SET_REACTIVE_POWERL1);
		try {
			setActivePowerChannel.setNextWriteValue(activePower);
		} catch (OpenemsException e) {
			log.error("Unable to set ActivePower: " + e.getMessage());
		}
		try {
			setReactivePowerChannel.setNextWriteValue(reactivePower);
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
		this.modbusBridgeId = config.modbus_id();

		/*
		 * Initialize Power
		 */
		// ReactivePower limitations
		this.addPowerConstraint(ConstraintType.STATIC, Phase.ALL, Pwr.REACTIVE, Relationship.GEQ, MIN_REACTIVE_POWER);
		this.addPowerConstraint(ConstraintType.STATIC, Phase.ALL, Pwr.REACTIVE, Relationship.LEQ, MAX_REACTIVE_POWER);
		// Allowed Apparent
		CircleConstraint allowedApparentConstraint = new CircleConstraint(this, MAX_APPARENT_POWER);
		this.channel(ChannelId.ALLOWED_APPARENT).onChange(value -> {
			allowedApparentConstraint.setRadius(TypeUtils.getAsType(OpenemsType.INTEGER, value));
		});
		// Allowed Charge
		Constraint allowedChargeConstraint = this.addPowerConstraint(ConstraintType.STATIC, Phase.ALL, Pwr.ACTIVE,
				Relationship.GEQ, 0);
		this.channel(ChannelId.ALLOWED_CHARGE).onChange(value -> {
			allowedChargeConstraint.setIntValue(TypeUtils.getAsType(OpenemsType.INTEGER, value));
		});
		// Allowed Discharge
		Constraint allowedDischargeConstraint = this.addPowerConstraint(ConstraintType.STATIC, Phase.ALL, Pwr.ACTIVE,
				Relationship.LEQ, 0);
		this.channel(ChannelId.ALLOWED_DISCHARGE).onChange(value -> {
			allowedDischargeConstraint.setIntValue(TypeUtils.getAsType(OpenemsType.INTEGER, value));
		});
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public String getModbusBridgeId() {
		return modbusBridgeId;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		return new ModbusProtocol(unitId, //
				new FC3ReadRegistersTask(100, Priority.HIGH, //
						m(EssFeneconPro.ChannelId.SYSTEM_STATE, new UnsignedWordElement(100)), //
						m(SymmetricEss.ChannelId.GRID_MODE, new UnsignedDoublewordElement(100), //
								new ElementToChannelConverter((value) -> {
									switch (TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, value)) {
									case 1:
										return SymmetricEss.GridMode.OFF_GRID.ordinal();
									case 2:
										return SymmetricEss.GridMode.ON_GRID.ordinal();
									}
									throw new IllegalArgumentException("Undefined GridMode [" + value + "]");
								})),
						m(EssFeneconPro.ChannelId.CONTROL_MODE, new UnsignedWordElement(101)), //
						m(EssFeneconPro.ChannelId.WORK_MODE, new UnsignedWordElement(102)), //
						new DummyRegisterElement(103), //
						m(EssFeneconPro.ChannelId.TOTAL_BATTERY_CHARGE_ENERGY, new UnsignedDoublewordElement(104)), //
						new DummyRegisterElement(105), //
						m(EssFeneconPro.ChannelId.TOTAL_BATTERY_DISCHARGE_ENERGY, new UnsignedDoublewordElement(106)), //
						new DummyRegisterElement(107), //
						m(EssFeneconPro.ChannelId.BATTERY_GROUP_STATE, new UnsignedWordElement(108)), //
						m(EssFeneconPro.ChannelId.BATTERY_SOC, new UnsignedWordElement(109)), //
						m(EssFeneconPro.ChannelId.BATTERY_VOLTAGE, new UnsignedWordElement(110)), //
						m(EssFeneconPro.ChannelId.BATTERY_CURRENT, new SignedWordElement(111)), //
						m(EssFeneconPro.ChannelId.BATTERY_POWER, new SignedWordElement(112)), //
						bm(new UnsignedWordElement(113))//
								.m(EssFeneconPro.ChannelId.STATE_0, 0) //
								.m(EssFeneconPro.ChannelId.STATE_1, 1) //
								.m(EssFeneconPro.ChannelId.STATE_2, 2) //
								.m(EssFeneconPro.ChannelId.STATE_3, 3) //
								.m(EssFeneconPro.ChannelId.STATE_4, 4) //
								.m(EssFeneconPro.ChannelId.STATE_5, 5) //
								.m(EssFeneconPro.ChannelId.STATE_6, 6) //
								.build(), //
						m(EssFeneconPro.ChannelId.PCS_OPERATION_STATE, new UnsignedWordElement(114)), //
						new DummyRegisterElement(115, 117), //
						m(EssFeneconPro.ChannelId.CURRENTL1, new SignedWordElement(118)), //
						m(EssFeneconPro.ChannelId.CURRENTL2, new SignedWordElement(119)), //
						m(EssFeneconPro.ChannelId.CURRENTL3, new SignedWordElement(120)), //
						m(EssFeneconPro.ChannelId.VOLTAGEL1, new UnsignedWordElement(121)), //
						m(EssFeneconPro.ChannelId.VOLTAGEL2, new UnsignedWordElement(122)), //
						m(EssFeneconPro.ChannelId.VOLTAGEL3, new UnsignedWordElement(123)), //
						m(EssFeneconPro.ChannelId.ACTIVE_POWERL1, new SignedWordElement(124)), //
						m(EssFeneconPro.ChannelId.ACTIVE_POWERL2, new SignedWordElement(125)), //
						m(EssFeneconPro.ChannelId.ACTIVE_POWERL3, new SignedWordElement(126)), //
						m(EssFeneconPro.ChannelId.REACTIVE_POWERL1, new SignedWordElement(127)), //
						m(EssFeneconPro.ChannelId.REACTIVE_POWERL2, new SignedWordElement(128)), //
						m(EssFeneconPro.ChannelId.REACTIVE_POWERL3, new SignedWordElement(129)), //
						new DummyRegisterElement(130), //
						m(EssFeneconPro.ChannelId.FREQUENCYL1, new UnsignedWordElement(131)), //
						m(EssFeneconPro.ChannelId.FREQUENCYL2, new UnsignedWordElement(132)), //
						m(EssFeneconPro.ChannelId.FREQUENCYL3, new UnsignedWordElement(133)), //
						// TODO Allowed Apparent is for one phase; multiply with 3
						m(EssFeneconPro.ChannelId.ALLOWED_APPARENT, new UnsignedWordElement(134)), //
						new DummyRegisterElement(135, 140), //
						m(EssFeneconPro.ChannelId.ALLOWED_CHARGE, new UnsignedWordElement(141)), //
						m(EssFeneconPro.ChannelId.ALLOWED_DISCHARGE, new UnsignedWordElement(142)), //
						new DummyRegisterElement(143, 149)), //
				new FC3ReadRegistersTask(150, Priority.LOW, //
						bm(new UnsignedWordElement(150))//
								.m(EssFeneconPro.ChannelId.STATE_7, 0)//
								.m(EssFeneconPro.ChannelId.STATE_8, 1)//
								.m(EssFeneconPro.ChannelId.STATE_9, 2)//
								.m(EssFeneconPro.ChannelId.STATE_10, 3)//
								.m(EssFeneconPro.ChannelId.STATE_11, 4)//
								.m(EssFeneconPro.ChannelId.STATE_12, 5)//
								.m(EssFeneconPro.ChannelId.STATE_13, 6)//
								.m(EssFeneconPro.ChannelId.STATE_14, 7)//
								.m(EssFeneconPro.ChannelId.STATE_15, 8)//
								.m(EssFeneconPro.ChannelId.STATE_16, 9)//
								.m(EssFeneconPro.ChannelId.STATE_17, 10)//
								.build(), //
						bm(new UnsignedWordElement(151))//
								.m(EssFeneconPro.ChannelId.STATE_18, 0)//
								.build(), //

						bm(new UnsignedWordElement(152))//
								.m(EssFeneconPro.ChannelId.STATE_19, 0)//
								.m(EssFeneconPro.ChannelId.STATE_20, 1)//
								.m(EssFeneconPro.ChannelId.STATE_21, 2)//
								.m(EssFeneconPro.ChannelId.STATE_22, 3)//
								.m(EssFeneconPro.ChannelId.STATE_23, 4)//
								.m(EssFeneconPro.ChannelId.STATE_24, 5)//
								.m(EssFeneconPro.ChannelId.STATE_25, 6)//
								.m(EssFeneconPro.ChannelId.STATE_26, 7)//
								.m(EssFeneconPro.ChannelId.STATE_27, 8)//
								.m(EssFeneconPro.ChannelId.STATE_28, 9)//
								.m(EssFeneconPro.ChannelId.STATE_29, 10)//
								.m(EssFeneconPro.ChannelId.STATE_30, 11)//
								.m(EssFeneconPro.ChannelId.STATE_31, 12)//
								.m(EssFeneconPro.ChannelId.STATE_32, 13)//
								.m(EssFeneconPro.ChannelId.STATE_33, 14)//
								.m(EssFeneconPro.ChannelId.STATE_34, 15)//
								.build(), //

						bm(new UnsignedWordElement(153))//
								.m(EssFeneconPro.ChannelId.STATE_35, 0)//
								.m(EssFeneconPro.ChannelId.STATE_36, 1)//
								.m(EssFeneconPro.ChannelId.STATE_37, 2)//
								.m(EssFeneconPro.ChannelId.STATE_38, 3)//
								.m(EssFeneconPro.ChannelId.STATE_39, 4)//
								.m(EssFeneconPro.ChannelId.STATE_40, 5)//
								.m(EssFeneconPro.ChannelId.STATE_41, 6)//
								.m(EssFeneconPro.ChannelId.STATE_42, 7)//
								.m(EssFeneconPro.ChannelId.STATE_43, 8)//
								.m(EssFeneconPro.ChannelId.STATE_44, 9)//
								.m(EssFeneconPro.ChannelId.STATE_45, 10)//
								.m(EssFeneconPro.ChannelId.STATE_46, 11)//
								.m(EssFeneconPro.ChannelId.STATE_47, 12)//
								.m(EssFeneconPro.ChannelId.STATE_48, 13)//
								.m(EssFeneconPro.ChannelId.STATE_49, 14)//
								.m(EssFeneconPro.ChannelId.STATE_50, 15)//
								.build(), //
						bm(new UnsignedWordElement(154))//
								.m(EssFeneconPro.ChannelId.STATE_51, 0)//
								.m(EssFeneconPro.ChannelId.STATE_52, 1)//
								.m(EssFeneconPro.ChannelId.STATE_53, 2)//
								.m(EssFeneconPro.ChannelId.STATE_54, 3)//
								.m(EssFeneconPro.ChannelId.STATE_55, 4)//
								.m(EssFeneconPro.ChannelId.STATE_56, 5)//
								.m(EssFeneconPro.ChannelId.STATE_57, 6)//
								.m(EssFeneconPro.ChannelId.STATE_58, 7)//
								.m(EssFeneconPro.ChannelId.STATE_59, 8)//
								.m(EssFeneconPro.ChannelId.STATE_60, 9)//
								.m(EssFeneconPro.ChannelId.STATE_61, 10)//
								.m(EssFeneconPro.ChannelId.STATE_62, 11)//
								.m(EssFeneconPro.ChannelId.STATE_63, 12)//
								.build(), //
						bm(new UnsignedWordElement(155))//
								.m(EssFeneconPro.ChannelId.STATE_64, 0)//
								.m(EssFeneconPro.ChannelId.STATE_65, 1)//
								.m(EssFeneconPro.ChannelId.STATE_66, 2)//
								.m(EssFeneconPro.ChannelId.STATE_67, 3)//
								.m(EssFeneconPro.ChannelId.STATE_68, 4)//
								.m(EssFeneconPro.ChannelId.STATE_69, 5)//
								.m(EssFeneconPro.ChannelId.STATE_70, 6)//
								.m(EssFeneconPro.ChannelId.STATE_71, 7)//
								.m(EssFeneconPro.ChannelId.STATE_72, 8)//
								.m(EssFeneconPro.ChannelId.STATE_73, 9)//
								.m(EssFeneconPro.ChannelId.STATE_74, 10)//
								.build(), //
						bm(new UnsignedWordElement(156))//
								.m(EssFeneconPro.ChannelId.STATE_75, 0)//
								.build(), //
						bm(new UnsignedWordElement(157))//
								.m(EssFeneconPro.ChannelId.STATE_76, 0)//
								.m(EssFeneconPro.ChannelId.STATE_77, 1)//
								.m(EssFeneconPro.ChannelId.STATE_78, 2)//
								.m(EssFeneconPro.ChannelId.STATE_79, 3)//
								.m(EssFeneconPro.ChannelId.STATE_80, 4)//
								.m(EssFeneconPro.ChannelId.STATE_81, 5)//
								.m(EssFeneconPro.ChannelId.STATE_82, 6)//
								.m(EssFeneconPro.ChannelId.STATE_83, 7)//
								.m(EssFeneconPro.ChannelId.STATE_84, 8)//
								.m(EssFeneconPro.ChannelId.STATE_85, 9)//
								.m(EssFeneconPro.ChannelId.STATE_86, 10)//
								.m(EssFeneconPro.ChannelId.STATE_87, 11)//
								.m(EssFeneconPro.ChannelId.STATE_88, 12)//
								.m(EssFeneconPro.ChannelId.STATE_89, 13)//
								.m(EssFeneconPro.ChannelId.STATE_90, 14)//
								.m(EssFeneconPro.ChannelId.STATE_91, 15)//
								.build(), //
						bm(new UnsignedWordElement(158))//
								.m(EssFeneconPro.ChannelId.STATE_92, 0)//
								.m(EssFeneconPro.ChannelId.STATE_93, 1)//
								.m(EssFeneconPro.ChannelId.STATE_94, 2)//
								.m(EssFeneconPro.ChannelId.STATE_95, 3)//
								.m(EssFeneconPro.ChannelId.STATE_96, 4)//
								.m(EssFeneconPro.ChannelId.STATE_97, 5)//
								.m(EssFeneconPro.ChannelId.STATE_98, 6)//
								.m(EssFeneconPro.ChannelId.STATE_99, 7)//
								.m(EssFeneconPro.ChannelId.STATE_100, 8)//
								.m(EssFeneconPro.ChannelId.STATE_101, 9)//
								.m(EssFeneconPro.ChannelId.STATE_102, 10)//
								.m(EssFeneconPro.ChannelId.STATE_103, 11)//
								.m(EssFeneconPro.ChannelId.STATE_104, 12)//
								.m(EssFeneconPro.ChannelId.STATE_105, 13)//
								.m(EssFeneconPro.ChannelId.STATE_106, 14)//
								.m(EssFeneconPro.ChannelId.STATE_107, 15)//
								.build(), //
						bm(new UnsignedWordElement(159))//
								.m(EssFeneconPro.ChannelId.STATE_108, 0)//
								.m(EssFeneconPro.ChannelId.STATE_109, 1)//
								.m(EssFeneconPro.ChannelId.STATE_110, 2)//
								.m(EssFeneconPro.ChannelId.STATE_111, 3)//
								.m(EssFeneconPro.ChannelId.STATE_112, 4)//
								.m(EssFeneconPro.ChannelId.STATE_113, 5)//
								.m(EssFeneconPro.ChannelId.STATE_114, 6)//
								.m(EssFeneconPro.ChannelId.STATE_115, 7)//
								.m(EssFeneconPro.ChannelId.STATE_116, 8)//
								.m(EssFeneconPro.ChannelId.STATE_117, 9)//
								.m(EssFeneconPro.ChannelId.STATE_118, 10)//
								.m(EssFeneconPro.ChannelId.STATE_119, 11)//
								.m(EssFeneconPro.ChannelId.STATE_120, 12)//
								.build(), //
						bm(new UnsignedWordElement(160))//
								.m(EssFeneconPro.ChannelId.STATE_121, 0)//
								.m(EssFeneconPro.ChannelId.STATE_122, 1)//
								.m(EssFeneconPro.ChannelId.STATE_123, 2)//
								.m(EssFeneconPro.ChannelId.STATE_124, 3)//
								.m(EssFeneconPro.ChannelId.STATE_125, 4)//
								.m(EssFeneconPro.ChannelId.STATE_126, 5)//
								.m(EssFeneconPro.ChannelId.STATE_127, 6)//
								.m(EssFeneconPro.ChannelId.STATE_128, 7)//
								.m(EssFeneconPro.ChannelId.STATE_129, 8)//
								.m(EssFeneconPro.ChannelId.STATE_130, 9)//
								.m(EssFeneconPro.ChannelId.STATE_131, 10)//
								.build(), //
						bm(new UnsignedWordElement(161))//
								.m(EssFeneconPro.ChannelId.STATE_132, 0)//
								.build(), //
						bm(new UnsignedWordElement(162))//
								.m(EssFeneconPro.ChannelId.STATE_133, 0)//
								.m(EssFeneconPro.ChannelId.STATE_134, 1)//
								.m(EssFeneconPro.ChannelId.STATE_135, 2)//
								.m(EssFeneconPro.ChannelId.STATE_136, 3)//
								.m(EssFeneconPro.ChannelId.STATE_137, 4)//
								.m(EssFeneconPro.ChannelId.STATE_138, 5)//
								.m(EssFeneconPro.ChannelId.STATE_139, 6)//
								.m(EssFeneconPro.ChannelId.STATE_140, 7)//
								.m(EssFeneconPro.ChannelId.STATE_141, 8)//
								.m(EssFeneconPro.ChannelId.STATE_142, 9)//
								.m(EssFeneconPro.ChannelId.STATE_143, 10)//
								.m(EssFeneconPro.ChannelId.STATE_144, 11)//
								.m(EssFeneconPro.ChannelId.STATE_145, 12)//
								.m(EssFeneconPro.ChannelId.STATE_146, 13)//
								.m(EssFeneconPro.ChannelId.STATE_147, 14)//
								.m(EssFeneconPro.ChannelId.STATE_148, 15)//
								.build(), //
						bm(new UnsignedWordElement(163))//
								.m(EssFeneconPro.ChannelId.STATE_149, 0)//
								.m(EssFeneconPro.ChannelId.STATE_150, 1)//
								.m(EssFeneconPro.ChannelId.STATE_151, 2)//
								.m(EssFeneconPro.ChannelId.STATE_152, 3)//
								.m(EssFeneconPro.ChannelId.STATE_153, 4)//
								.m(EssFeneconPro.ChannelId.STATE_154, 5)//
								.m(EssFeneconPro.ChannelId.STATE_155, 6)//
								.m(EssFeneconPro.ChannelId.STATE_156, 7)//
								.m(EssFeneconPro.ChannelId.STATE_157, 8)//
								.m(EssFeneconPro.ChannelId.STATE_158, 9)//
								.m(EssFeneconPro.ChannelId.STATE_159, 10)//
								.m(EssFeneconPro.ChannelId.STATE_160, 11)//
								.m(EssFeneconPro.ChannelId.STATE_161, 12)//
								.m(EssFeneconPro.ChannelId.STATE_162, 13)//
								.m(EssFeneconPro.ChannelId.STATE_163, 14)//
								.m(EssFeneconPro.ChannelId.STATE_164, 15)//
								.build(), //
						bm(new UnsignedWordElement(164))//
								.m(EssFeneconPro.ChannelId.STATE_165, 0)//
								.m(EssFeneconPro.ChannelId.STATE_166, 1)//
								.m(EssFeneconPro.ChannelId.STATE_167, 2)//
								.m(EssFeneconPro.ChannelId.STATE_168, 3)//
								.m(EssFeneconPro.ChannelId.STATE_169, 4)//
								.m(EssFeneconPro.ChannelId.STATE_170, 5)//
								.m(EssFeneconPro.ChannelId.STATE_171, 6)//
								.m(EssFeneconPro.ChannelId.STATE_172, 7)//
								.m(EssFeneconPro.ChannelId.STATE_173, 8)//
								.m(EssFeneconPro.ChannelId.STATE_174, 9)//
								.m(EssFeneconPro.ChannelId.STATE_175, 10)//
								.m(EssFeneconPro.ChannelId.STATE_176, 11)//
								.m(EssFeneconPro.ChannelId.STATE_177, 12)//
								.build()//
				), //
				new FC16WriteRegistersTask(200, //
						m(EssFeneconPro.ChannelId.SET_WORK_STATE, new UnsignedWordElement(200))), //
				new FC16WriteRegistersTask(206, //
						m(EssFeneconPro.ChannelId.SET_ACTIVE_POWERL1, new UnsignedWordElement(206)), //
						m(EssFeneconPro.ChannelId.SET_REACTIVE_POWERL1, new UnsignedWordElement(207)), //
						m(EssFeneconPro.ChannelId.SET_ACTIVE_POWERL2, new UnsignedWordElement(208)), //
						m(EssFeneconPro.ChannelId.SET_REACTIVE_POWERL2, new UnsignedWordElement(209)), //
						m(EssFeneconPro.ChannelId.SET_ACTIVE_POWERL3, new UnsignedWordElement(210)), //
						m(EssFeneconPro.ChannelId.SET_REACTIVE_POWERL3, new UnsignedWordElement(211))), //

				new FC3ReadRegistersTask(3020, Priority.LOW, //
						m(EssFeneconPro.ChannelId.BATTERY_VOLTAGE_SECTION1, new UnsignedWordElement(3020)), //
						m(EssFeneconPro.ChannelId.BATTERY_VOLTAGE_SECTION2, new UnsignedWordElement(3021)), //
						m(EssFeneconPro.ChannelId.BATTERY_VOLTAGE_SECTION3, new UnsignedWordElement(3022)), //
						m(EssFeneconPro.ChannelId.BATTERY_VOLTAGE_SECTION4, new UnsignedWordElement(3023)), //
						m(EssFeneconPro.ChannelId.BATTERY_VOLTAGE_SECTION5, new UnsignedWordElement(3024)), //
						m(EssFeneconPro.ChannelId.BATTERY_VOLTAGE_SECTION6, new UnsignedWordElement(3025)), //
						m(EssFeneconPro.ChannelId.BATTERY_VOLTAGE_SECTION7, new UnsignedWordElement(3026)), //
						m(EssFeneconPro.ChannelId.BATTERY_VOLTAGE_SECTION8, new UnsignedWordElement(3027)), //
						m(EssFeneconPro.ChannelId.BATTERY_VOLTAGE_SECTION9, new UnsignedWordElement(3028)), //
						m(EssFeneconPro.ChannelId.BATTERY_VOLTAGE_SECTION10, new UnsignedWordElement(3029)), //
						m(EssFeneconPro.ChannelId.BATTERY_VOLTAGE_SECTION11, new UnsignedWordElement(3030)), //
						m(EssFeneconPro.ChannelId.BATTERY_VOLTAGE_SECTION12, new UnsignedWordElement(3031)), //
						m(EssFeneconPro.ChannelId.BATTERY_VOLTAGE_SECTION13, new UnsignedWordElement(3032)), //
						m(EssFeneconPro.ChannelId.BATTERY_VOLTAGE_SECTION14, new UnsignedWordElement(3033)), //
						m(EssFeneconPro.ChannelId.BATTERY_VOLTAGE_SECTION15, new UnsignedWordElement(3034)), //
						m(EssFeneconPro.ChannelId.BATTERY_VOLTAGE_SECTION16, new UnsignedWordElement(3035)), //
						m(EssFeneconPro.ChannelId.BATTERY_TEMPERATURE_SECTION1, new UnsignedWordElement(3036)), //
						m(EssFeneconPro.ChannelId.BATTERY_TEMPERATURE_SECTION2, new UnsignedWordElement(3037)), //
						m(EssFeneconPro.ChannelId.BATTERY_TEMPERATURE_SECTION3, new UnsignedWordElement(3038)), //
						m(EssFeneconPro.ChannelId.BATTERY_TEMPERATURE_SECTION4, new UnsignedWordElement(3039)), //
						m(EssFeneconPro.ChannelId.BATTERY_TEMPERATURE_SECTION5, new UnsignedWordElement(3040)), //
						m(EssFeneconPro.ChannelId.BATTERY_TEMPERATURE_SECTION6, new UnsignedWordElement(3041)), //
						m(EssFeneconPro.ChannelId.BATTERY_TEMPERATURE_SECTION7, new UnsignedWordElement(3042)), //
						m(EssFeneconPro.ChannelId.BATTERY_TEMPERATURE_SECTION8, new UnsignedWordElement(3043)), //
						m(EssFeneconPro.ChannelId.BATTERY_TEMPERATURE_SECTION9, new UnsignedWordElement(3044)), //
						m(EssFeneconPro.ChannelId.BATTERY_TEMPERATURE_SECTION10, new UnsignedWordElement(3045)), //
						m(EssFeneconPro.ChannelId.BATTERY_TEMPERATURE_SECTION11, new UnsignedWordElement(3046)), //
						m(EssFeneconPro.ChannelId.BATTERY_TEMPERATURE_SECTION12, new UnsignedWordElement(3047)), //
						m(EssFeneconPro.ChannelId.BATTERY_TEMPERATURE_SECTION13, new UnsignedWordElement(3048)), //
						m(EssFeneconPro.ChannelId.BATTERY_TEMPERATURE_SECTION14, new UnsignedWordElement(3049)), //
						m(EssFeneconPro.ChannelId.BATTERY_TEMPERATURE_SECTION15, new UnsignedWordElement(3050)), //
						m(EssFeneconPro.ChannelId.BATTERY_TEMPERATURE_SECTION16, new UnsignedWordElement(3051))), //
				new FC16WriteRegistersTask(9014, //
						m(EssFeneconPro.ChannelId.RTC_YEAR, new UnsignedWordElement(9014)), //
						m(EssFeneconPro.ChannelId.RTC_MONTH, new UnsignedWordElement(9015)), //
						m(EssFeneconPro.ChannelId.RTC_DAY, new UnsignedWordElement(9016)), //
						m(EssFeneconPro.ChannelId.RTC_HOUR, new UnsignedWordElement(9017)), //
						m(EssFeneconPro.ChannelId.RTC_MINUTE, new UnsignedWordElement(9018)), //
						m(EssFeneconPro.ChannelId.RTC_SECOND, new UnsignedWordElement(9019))), //
				new FC16WriteRegistersTask(30558, //
						m(EssFeneconPro.ChannelId.SET_SETUP_MODE, new UnsignedWordElement(30558))), //
				new FC16WriteRegistersTask(30559, //
						m(EssFeneconPro.ChannelId.SET_PCS_MODE, new UnsignedWordElement(30559))), //
				new FC16WriteRegistersTask(30157, //
						m(EssFeneconPro.ChannelId.SETUP_MODE, new UnsignedWordElement(30157)), //
						m(EssFeneconPro.ChannelId.PCS_MODE, new UnsignedWordElement(30158)))//

		);//
	}

	@Override
	public int getPowerPrecision() {
		return 100; // the modbus field for SetActivePower has the unit 0.1 kW
	}

	private enum SetWorkState {
		STOP, STANDBY, START, LOCAL_CONTROL, REMOTE_CONTROL_OF_GRID, EMERGENCY_STOP
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		SET_WORK_STATE(new Doc() //
				.option(0, SetWorkState.LOCAL_CONTROL)//
				.option(1, SetWorkState.START) //
				.option(2, SetWorkState.REMOTE_CONTROL_OF_GRID) //
				.option(3, SetWorkState.STOP) //
				.option(4, SetWorkState.EMERGENCY_STOP)), //

		WORK_MODE(new Doc()//
				.option(2, "Economy")//
				.option(6, "Remote")//
				.option(8, "Timing")), //
		SYSTEM_STATE(new Doc() //
				.option(0, "STANDBY") //
				.option(1, "Start Off-Grid") //
				.option(2, "START") //
				.option(3, "FAULT") //
				.option(4, "Off-Grd PV")), //
		CONTROL_MODE(new Doc()//
				.option(1, "Remote")//
				.option(2, "Local")), //

		ALLOWED_CHARGE(new Doc().unit(Unit.WATT)), //
		ALLOWED_DISCHARGE(new Doc().unit(Unit.WATT)), //
		TOTAL_BATTERY_CHARGE_ENERGY(new Doc().unit(Unit.WATT_HOURS)), //
		TOTAL_BATTERY_DISCHARGE_ENERGY(new Doc().unit(Unit.WATT_HOURS)), //
		BATTERY_GROUP_STATE(new Doc()//
				.option(0, "Initial")//
				.option(1, "Stop")//
				.option(2, "Starting")//
				.option(3, "Running")//
				.option(4, "Stopping")//
				.option(5, "Fail")//
		), //
		SET_ACTIVE_POWERL1(new Doc().unit(Unit.WATT)), //
		SET_REACTIVE_POWERL1(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		SET_ACTIVE_POWERL2(new Doc().unit(Unit.WATT)), //
		SET_REACTIVE_POWERL2(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		SET_ACTIVE_POWERL3(new Doc().unit(Unit.WATT)), //
		SET_REACTIVE_POWERL3(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		BATTERY_SOC(new Doc().unit(Unit.PERCENT)), //
		BATTERY_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
		BATTERY_POWER(new Doc().unit(Unit.WATT)), //
		CURRENTL1(new Doc().unit(Unit.MILLIAMPERE)), //
		CURRENTL2(new Doc().unit(Unit.MILLIAMPERE)), //
		CURRENTL3(new Doc().unit(Unit.MILLIAMPERE)), //
		VOLTAGEL1(new Doc().unit(Unit.MILLIVOLT)), //
		VOLTAGEL2(new Doc().unit(Unit.MILLIVOLT)), //
		VOLTAGEL3(new Doc().unit(Unit.MILLIVOLT)), //
		ACTIVE_POWERL1(new Doc().unit(Unit.WATT)), //
		ACTIVE_POWERL2(new Doc().unit(Unit.WATT)), //
		ACTIVE_POWERL3(new Doc().unit(Unit.WATT)), //
		REACTIVE_POWERL1(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		REACTIVE_POWERL2(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		REACTIVE_POWERL3(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //

		FREQUENCYL1(new Doc().unit(Unit.MILLIHERTZ)), //
		FREQUENCYL2(new Doc().unit(Unit.MILLIHERTZ)), //
		FREQUENCYL3(new Doc().unit(Unit.MILLIHERTZ)), //
		ALLOWED_APPARENT(new Doc().unit(Unit.VOLT_AMPERE)), //

		BATTERY_VOLTAGE_SECTION1(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION2(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION3(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION4(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION5(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION6(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION7(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION8(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION9(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION10(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION11(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION12(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION13(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION14(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION15(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_VOLTAGE_SECTION16(new Doc().unit(Unit.MILLIVOLT)), //
		// TODO add .delta(-40L)
		BATTERY_TEMPERATURE_SECTION1(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION2(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION3(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION4(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION5(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION6(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION7(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION8(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION9(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION10(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION11(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION12(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION13(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION14(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION15(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TEMPERATURE_SECTION16(new Doc().unit(Unit.DEGREE_CELSIUS)), //

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
		STATE_177(new Doc().level(Level.FAULT).text("ExternalGridCurrentZeroDriftErrorL3")), //
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

	@Override
	public Power getPower() {
		return this.power;
	}

}
