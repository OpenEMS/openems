package io.openems.edge.ess.fenecon.mini;

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
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
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
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.ConstraintType;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.Fenecon.Mini", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
)

public class EssFeneconMini extends AbstractOpenemsModbusComponent
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(EssFeneconMini.class);
	private String modbusBridgeId;
	private final static int UNIT_ID = 4;

	@Reference
	private Power power;

	@Reference
	protected ConfigurationAdmin cm;

	public EssFeneconMini() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
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
						m(EssFeneconMini.ChannelId.SYSTEM_STATE, new UnsignedWordElement(100)), //
						m(EssFeneconMini.ChannelId.CONTROL_MODE, new UnsignedWordElement(101)), //
						new DummyRegisterElement(102, 103), //
						m(EssFeneconMini.ChannelId.TOTAL_BATTERY_CHARGE_ENERGY, new UnsignedDoublewordElement(104)), //
						m(EssFeneconMini.ChannelId.TOTAL_BATTERY_DISCHARGE_ENERGY, new UnsignedDoublewordElement(106)), //
						new DummyRegisterElement(107), //
						m(EssFeneconMini.ChannelId.BATTERY_GROUP_STATE, new UnsignedWordElement(108)), //
						m(EssFeneconMini.ChannelId.BATTERY_SOC, new UnsignedWordElement(109)), //
						m(EssFeneconMini.ChannelId.BATTERY_VOLTAGE, new UnsignedWordElement(110)), //
						m(EssFeneconMini.ChannelId.BATTERY_CURRENT, new SignedWordElement(111)), //
						m(EssFeneconMini.ChannelId.BATTERY_POWER, new SignedWordElement(112)), //
						bm(new UnsignedWordElement(113))//
								.m(EssFeneconMini.ChannelId.STATE_0, 0) //
								.m(EssFeneconMini.ChannelId.STATE_1, 1) //
								.m(EssFeneconMini.ChannelId.STATE_2, 2) //
								.m(EssFeneconMini.ChannelId.STATE_3, 3) //
								.m(EssFeneconMini.ChannelId.STATE_4, 4) //
								.m(EssFeneconMini.ChannelId.STATE_5, 5) //
								.m(EssFeneconMini.ChannelId.STATE_6, 6) //
								.build(), //
						m(EssFeneconMini.ChannelId.PCS_OPERATION_STATE, new UnsignedWordElement(114)), //
						new DummyRegisterElement(115, 117), //
						m(EssFeneconMini.ChannelId.CURRENT, new SignedWordElement(118)), //
						new DummyRegisterElement(119, 120), //
						m(EssFeneconMini.ChannelId.VOLTAGE, new UnsignedWordElement(121)), //
						new DummyRegisterElement(122, 123), //
						m(EssFeneconMini.ChannelId.ACTIVE_POWER, new SignedWordElement(124)), //
						new DummyRegisterElement(125, 126), //
						m(EssFeneconMini.ChannelId.REACTIVE_POWER, new SignedWordElement(127)), //
						new DummyRegisterElement(128, 130), //
						m(EssFeneconMini.ChannelId.FREQUENCY, new UnsignedWordElement(131)), //
						new DummyRegisterElement(132, 133), //
						m(EssFeneconMini.ChannelId.PHASE_ALLOWED_APPARENT, new UnsignedWordElement(134)), //
						new DummyRegisterElement(135, 140), //
						m(EssFeneconMini.ChannelId.ALLOWED_CHARGE, new UnsignedWordElement(141)), //
						m(EssFeneconMini.ChannelId.ALLOWED_DISCHARGE, new UnsignedWordElement(142)), //
						new DummyRegisterElement(143, 149), //
						bm(new UnsignedWordElement(150))//
								.m(EssFeneconMini.ChannelId.STATE_7, 0)//
								.m(EssFeneconMini.ChannelId.STATE_8, 1)//
								.m(EssFeneconMini.ChannelId.STATE_9, 2)//
								.m(EssFeneconMini.ChannelId.STATE_10, 3)//
								.m(EssFeneconMini.ChannelId.STATE_11, 4)//
								.m(EssFeneconMini.ChannelId.STATE_12, 5)//
								.m(EssFeneconMini.ChannelId.STATE_13, 6)//
								.m(EssFeneconMini.ChannelId.STATE_14, 7)//
								.m(EssFeneconMini.ChannelId.STATE_15, 8)//
								.m(EssFeneconMini.ChannelId.STATE_16, 9)//
								.m(EssFeneconMini.ChannelId.STATE_17, 10)//
								.build(), //
						bm(new UnsignedWordElement(151))//
								.m(EssFeneconMini.ChannelId.STATE_18, 0)//
								.build(), //

						bm(new UnsignedWordElement(152))//
								.m(EssFeneconMini.ChannelId.STATE_19, 0)//
								.m(EssFeneconMini.ChannelId.STATE_20, 1)//
								.m(EssFeneconMini.ChannelId.STATE_21, 2)//
								.m(EssFeneconMini.ChannelId.STATE_22, 3)//
								.m(EssFeneconMini.ChannelId.STATE_23, 4)//
								.m(EssFeneconMini.ChannelId.STATE_24, 5)//
								.m(EssFeneconMini.ChannelId.STATE_25, 6)//
								.m(EssFeneconMini.ChannelId.STATE_26, 7)//
								.m(EssFeneconMini.ChannelId.STATE_28, 8)//
								.m(EssFeneconMini.ChannelId.STATE_29, 9)//
								.m(EssFeneconMini.ChannelId.STATE_30, 10)//
								.m(EssFeneconMini.ChannelId.STATE_31, 11)//
								.m(EssFeneconMini.ChannelId.STATE_32, 12)//
								.m(EssFeneconMini.ChannelId.STATE_33, 13)//
								.m(EssFeneconMini.ChannelId.STATE_34, 14)//
								.m(EssFeneconMini.ChannelId.STATE_35, 15)//
								.build())//
		);
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

		BATTERY_SOC(new Doc().unit(Unit.PERCENT)), //
		BATTERY_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
		BATTERY_POWER(new Doc().unit(Unit.WATT)), //
		CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
		VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		REACTIVE_POWER(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
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
		STATE_35(new Doc().level(Level.FAULT).text("HardwareControlCurrentProtection")),//

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
