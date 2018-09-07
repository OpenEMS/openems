package io.openems.edge.ess.mr.gridcon;

import java.time.LocalDateTime;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

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
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.OptionsEnum;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.mr.gridcon.enums.CCUState;
import io.openems.edge.ess.mr.gridcon.enums.GridConChannelId;
import io.openems.edge.ess.mr.gridcon.enums.PCSControlWordBitPosition;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;
import io.openems.edge.ess.power.api.CircleConstraint;
import io.openems.edge.ess.power.api.Power;

/**
 * This class handles the communication between ems and a gridcon.
 */
@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.MR.Gridcon", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
)
public class GridconPCS extends AbstractOpenemsModbusComponent implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(GridconPCS.class);

	protected static final float MAX_POWER_W = 125 * 1000;

	static final int MAX_APPARENT_POWER = (int) MAX_POWER_W; // TODO Checkif correct
	private CircleConstraint maxApparentPowerConstraint = null; 

	@Reference
	private Power power;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	List<Battery> batteries = new CopyOnWriteArrayList<>();

	public GridconPCS() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		// update filter for 'modbus'
		if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "modbus", config.modbus_id())) {
			return;
		}

		// update filter for 'battery'
		if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "batteries", config.battery_ids())) {
			return;
		}

		/*
		 * Initialize Power
		 */
		// Max Apparent
		// TODO adjust apparent power from modbus element
		this.maxApparentPowerConstraint = new CircleConstraint(this, MAX_APPARENT_POWER);

		super.activate(context, config.service_pid(), config.id(), config.enabled(), config.unit_id(), this.cm, "Modbus", config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * This method tries to turn on the gridcon/ set it in a RUN state.
	 */
	private void handleStateMachine() {
		// TODO
		// see Software manual chapter 5.1

//		writeValuesBackInChannel(//
//				GridConChannelId.PCS_COMMAND_ERROR_CODE_FALLBACK, //
//				GridConChannelId.PCS_COMMAND_CONTROL_PARAMETER_U0, //
//				GridConChannelId.PCS_COMMAND_CONTROL_PARAMETER_F0, //
//				GridConChannelId.PCS_COMMAND_CONTROL_PARAMETER_Q_REF, //
//				GridConChannelId.PCS_COMMAND_CONTROL_PARAMETER_P_REF //
//		);
//
//		writeDateAndTime();
//		if (isIdle()) {
//			startSystem();
//		} else if (isError()) {
//			doErrorHandling();
//		}
	}

	/**
	 * In order to correctly communicate with a gridcon every bit in the protocol
	 * has to be sent. To ensure that all bits are set, this method sets the last
	 * value set in the channel as the next value to be written. If there was no
	 * last value 0 is set into the channel instead.
	 * 
	 * @param ids the channels which should retain their value
	 */
	void writeValuesBackInChannel(GridConChannelId... ids) {
		for (GridConChannelId id : ids) {
			Value<?> value = this.channel(id).getNextValue();
			Object writeValue = 0;
			if (value.asOptional().isPresent()) {
				writeValue = value.asOptional().get();
			}
			try {
				((WriteChannel<?>) this.channel(id)).setNextWriteValueFromObject(writeValue);
			} catch (OpenemsException e) {
				// TODO: errorhandling
				e.printStackTrace();
			}
		}
	}

	/**
	 * This writes the current time into the necessary channel for the
	 * communicationprotocol with the gridcon.
	 */
	private void writeDateAndTime() {
		LocalDateTime time = LocalDateTime.now();
		byte dayOfWeek = (byte) time.getDayOfWeek().ordinal();
		byte day = (byte) time.getDayOfMonth();
		byte month = (byte) time.getMonth().getValue();
		byte year = (byte) (time.getYear() - 2000); // 0 == year 1900 in the protocol

		Integer dateInteger = convertToInteger(BitSet.valueOf(new byte[] { day, dayOfWeek, year, month }));

		byte seconds = (byte) time.getSecond();
		byte minutes = (byte) time.getMinute();
		byte hours = (byte) time.getHour();

		// second byte is unused
		Integer timeInteger = convertToInteger(BitSet.valueOf(new byte[] { seconds, 0, hours, minutes }));

		IntegerWriteChannel dateChannel = this.channel(GridConChannelId.PCS_COMMAND_TIME_SYNC_DATE);
		IntegerWriteChannel timeChannel = this.channel(GridConChannelId.PCS_COMMAND_TIME_SYNC_TIME);

		try {
			dateChannel.setNextWriteValue(dateInteger);
			timeChannel.setNextWriteValue(timeInteger);
		} catch (OpenemsException e) {
			e.printStackTrace();
		}
	}

	private boolean isError() {
		BooleanReadChannel stateErrorChannel = this.channel(GridConChannelId.PCS_CCU_STATE_ERROR);
		Optional<Boolean> valueOpt = stateErrorChannel.getNextValue().asOptional();
		return valueOpt.isPresent() && valueOpt.get();
	}

	/**
	 * Checks if the system is currently in the IDLE state. Note a READY, ERROR or
	 * PAUSE state will return also false.
	 * 
	 * @return true if this system is idle.
	 */
	private boolean isIdle() {
		BooleanReadChannel stateIdleChannel = this.channel(GridConChannelId.PCS_CCU_STATE_IDLE);
		Optional<Boolean> valueOpt = stateIdleChannel.getNextValue().asOptional();
		return valueOpt.isPresent() && valueOpt.get();
	}

	/**
	 * This turns on the system by enabling ALL IPUs.
	 */
	private void startSystem() {
		// TODO
		log.info("Try to start system");
		/*
		 * Coming from state idle first write 800V to IPU4 voltage setpoint, set "73" to
		 * DCDC String Control Mode of IPU4 and "1" to Weight String A, B, C ==> i.e.
		 * all 3 IPUs are weighted equally write -86000 to Pmax discharge Iref String A,
		 * B, C, write 86000 to Pmax Charge DCDC Str Mode of IPU 1, 2, 3 set P Control
		 * mode to "Act Pow Ctrl" (hex 4000 = mode power limiter, 0 = disabled, hex 3F80
		 * = active power control) and Mode Sel to "Current Control" s--> ee pic
		 * start0.png in doc folder
		 * 
		 * enable "Sync Approval" and "Ena IPU 4" and PLAY command -> system should
		 * change state to "RUN" --> see pic start1.png
		 * 
		 * after that enable IPU 1, 2, 3, if they have reached state "RUN" (=14) power
		 * can be set (from 0..1 (1 = max system power = 125 kW) , i.e. 0,05 is equal to
		 * 6.250 W same for reactive power see pic start2.png
		 * 
		 * "Normal mode" is reached now
		 */

		// int controlWordMask = 0xFFFFFFFF;

		// enable "Sync Approval" and "Ena IPU 4, 3, 2, 1" and PLAY command -> system
		// should change state to "RUN"
		BitSet bitSet = new BitSet(32);
		bitSet.set(PCSControlWordBitPosition.PLAY.getBitPosition(), true);
		bitSet.set(PCSControlWordBitPosition.SYNC_APPROVAL.getBitPosition(), true);
		bitSet.set(PCSControlWordBitPosition.MODE_SELECTION.getBitPosition(), true);

		bitSet.set(PCSControlWordBitPosition.DISABLE_IPU_1.getBitPosition(), false);
		bitSet.set(PCSControlWordBitPosition.DISABLE_IPU_2.getBitPosition(), false);
		bitSet.set(PCSControlWordBitPosition.DISABLE_IPU_3.getBitPosition(), false);
		bitSet.set(PCSControlWordBitPosition.DISABLE_IPU_4.getBitPosition(), false);

		IntegerWriteChannel controlWordChannel = this.channel(GridConChannelId.PCS_COMMAND_CONTROL_WORD);
		try {
			Integer value = convertToInteger(bitSet);
			controlWordChannel.setNextWriteValue(value);
		} catch (OpenemsException e) {
			e.printStackTrace();
		}

		writeIPUParameters();
	}

	private void stopSystem() {
		// TODO
		log.info("Try to stop system");

		// disable "Sync Approval" and "Ena IPU 4, 3, 2, 1" and add STOP command ->
		// system
		// should change state to "IDLE"
		BitSet bitSet = new BitSet(32);
		bitSet.set(PCSControlWordBitPosition.PLAY.getBitPosition(), false);
		bitSet.set(PCSControlWordBitPosition.STOP.getBitPosition(), true);
		bitSet.set(PCSControlWordBitPosition.SYNC_APPROVAL.getBitPosition(), false);
		bitSet.set(PCSControlWordBitPosition.BLACKSTART_APPROVAL.getBitPosition(), false);
		bitSet.set(PCSControlWordBitPosition.MODE_SELECTION.getBitPosition(), true);

		bitSet.set(PCSControlWordBitPosition.DISABLE_IPU_1.getBitPosition(), true);
		bitSet.set(PCSControlWordBitPosition.DISABLE_IPU_2.getBitPosition(), true);
		bitSet.set(PCSControlWordBitPosition.DISABLE_IPU_3.getBitPosition(), true);
		bitSet.set(PCSControlWordBitPosition.DISABLE_IPU_4.getBitPosition(), true);

		IntegerWriteChannel controlWordChannel = this.channel(GridConChannelId.PCS_COMMAND_CONTROL_WORD);
		try {
			Integer value = convertToInteger(bitSet);
			controlWordChannel.setNextWriteValue(value);
		} catch (OpenemsException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This converts a Bitset to its decimal value. Only works as long as the value
	 * of the Bitset does not exceed the range of an Integer.
	 * 
	 * @param bitSet The Bitset which should be converted
	 * @return The converted Integer
	 */
	private Integer convertToInteger(BitSet bitSet) {
		long[] l = bitSet.toLongArray();

		if (l.length == 0) {
			return 0;
		}
		return (int) l[0];
	}

	private void writeIPUParameters() {
		try {
			((FloatWriteChannel) this.channel(GridConChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_DC_VOLTAGE_SETPOINT)).setNextWriteValue(800f);
			((FloatWriteChannel) this.channel(GridConChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_A)).setNextWriteValue(1f);
			((FloatWriteChannel) this.channel(GridConChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_B)).setNextWriteValue(1f);
			((FloatWriteChannel) this.channel(GridConChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_C)).setNextWriteValue(1f);
			((FloatWriteChannel) this.channel(GridConChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_DC_DC_STRING_CONTROL_MODE)).setNextWriteValue(73f);

			float pMaxCharge = 86000;
			float pMaxDischarge = -86000;

			((FloatWriteChannel) this.channel(GridConChannelId.PCS_CONTROL_IPU_1_PARAMETERS_P_MAX_DISCHARGE)).setNextWriteValue(pMaxDischarge);
			((FloatWriteChannel) this.channel(GridConChannelId.PCS_CONTROL_IPU_2_PARAMETERS_P_MAX_DISCHARGE)).setNextWriteValue(pMaxDischarge);
			((FloatWriteChannel) this.channel(GridConChannelId.PCS_CONTROL_IPU_3_PARAMETERS_P_MAX_DISCHARGE)).setNextWriteValue(pMaxDischarge);

			((FloatWriteChannel) this.channel(GridConChannelId.PCS_CONTROL_IPU_1_PARAMETERS_P_MAX_CHARGE)).setNextWriteValue(pMaxCharge);
			((FloatWriteChannel) this.channel(GridConChannelId.PCS_CONTROL_IPU_2_PARAMETERS_P_MAX_CHARGE)).setNextWriteValue(pMaxCharge);
			((FloatWriteChannel) this.channel(GridConChannelId.PCS_CONTROL_IPU_3_PARAMETERS_P_MAX_CHARGE)).setNextWriteValue(pMaxCharge);

			((IntegerWriteChannel) this.channel(GridConChannelId.PCS_CONTROL_PARAMETER_P_CONTROL_MODE)).setNextWriteValue(PControlMode.ACTIVE_POWER_CONTROL.getValue());

		} catch (OpenemsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void doErrorHandling() {
		// TODO
		// try to find out what kind of error it is,
		// disable IPUs, stopping system, then acknowledge errors, wait some seconds
		// if no errors are shown, then try to start system
//		stopSystem();
		acknowledgeErrors();
	}

	/**
	 * This sends an ACKNOWLEDGE message. This does not fix the error. If the error
	 * was fixed previously the system should continue operating normally. If not a
	 * manual restart may be necessary.
	 */
	private void acknowledgeErrors() {
		BitSet bitSet = new BitSet(32);
		bitSet.set(PCSControlWordBitPosition.ACKNOWLEDGE.getBitPosition(), true);

		// TODO: writebackintochannel method better?
		bitSet.set(PCSControlWordBitPosition.DISABLE_IPU_1.getBitPosition(), false);
		bitSet.set(PCSControlWordBitPosition.DISABLE_IPU_2.getBitPosition(), false);
		bitSet.set(PCSControlWordBitPosition.DISABLE_IPU_3.getBitPosition(), false);
		bitSet.set(PCSControlWordBitPosition.DISABLE_IPU_4.getBitPosition(), false);

		bitSet.set(PCSControlWordBitPosition.SYNC_APPROVAL.getBitPosition(), true);
		bitSet.set(PCSControlWordBitPosition.MODE_SELECTION.getBitPosition(), true);

		IntegerWriteChannel controlWordChannel = this.channel(GridConChannelId.PCS_COMMAND_CONTROL_WORD);
		try {
			Integer value = convertToInteger(bitSet);
			controlWordChannel.setNextWriteValue(value);
		} catch (OpenemsException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String debugLog() {
		return "Current state: " + getCurrentState().toString();
	}

	private CCUState getCurrentState() { // TODO
		if (((BooleanReadChannel) this.channel(GridConChannelId.PCS_CCU_STATE_IDLE)).value().get()) {
			return CCUState.IDLE;
		}

		if (((BooleanReadChannel) this.channel(GridConChannelId.PCS_CCU_STATE_PRECHARGE)).value().get()) {
			return CCUState.PRECHARGE;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.PCS_CCU_STATE_STOP_PRECHARGE)).value().get()) {
			return CCUState.STOP_PRECHARGE;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.PCS_CCU_STATE_READY)).value().get()) {
			return CCUState.READY;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.PCS_CCU_STATE_PAUSE)).value().get()) {
			return CCUState.PAUSE;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.PCS_CCU_STATE_RUN)).value().get()) {
			return CCUState.RUN;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.PCS_CCU_STATE_ERROR)).value().get()) {
			return CCUState.ERROR;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.PCS_CCU_STATE_VOLTAGE_RAMPING_UP)).value().get()) {
			return CCUState.VOLTAGE_RAMPING_UP;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.PCS_CCU_STATE_OVERLOAD)).value().get()) {
			return CCUState.OVERLOAD;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.PCS_CCU_STATE_SHORT_CIRCUIT_DETECTED)).value().get()) {
			return CCUState.SHORT_CIRCUIT_DETECTED;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.PCS_CCU_STATE_DERATING_POWER)).value().get()) {
			return CCUState.DERATING_POWER;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.PCS_CCU_STATE_DERATING_HARMONICS)).value().get()) {
			return CCUState.DERATING_HARMONICS;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.PCS_CCU_STATE_SIA_ACTIVE)).value().get()) {
			return CCUState.SIA_ACTIVE;
		}

		return CCUState.UNDEFINED;
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
		if (isIdle()) {
			return;
		}
		FloatWriteChannel channelPRef = this.channel(GridConChannelId.PCS_COMMAND_CONTROL_PARAMETER_P_REF);
		FloatWriteChannel channelQRef = this.channel(GridConChannelId.PCS_COMMAND_CONTROL_PARAMETER_Q_REF);
		writeValuesBackInChannel(GridConChannelId.PCS_COMMAND_CONTROL_WORD);
		/*
		 * !! signum, MR calculates negative values as discharge, positive as charge.
		 * Gridcon sets the (dis)charge according to a percentage of the MAX_POWER. So
		 * 0.1 => 10% of max power. Values should never take values lower than 0 or
		 * higher than 1.
		 */
		// TODO: round to a set number of decimals?
		float activePowerFactor = -activePower / MAX_POWER_W;
		float reactivePowerFactor = -reactivePower / MAX_POWER_W;
		try {
			channelPRef.setNextWriteValue(activePowerFactor);
			channelQRef.setNextWriteValue(reactivePowerFactor);
		} catch (OpenemsException e) {
			log.error("problem occurred while trying to set active/reactive power" + e.getMessage());
		}
	}

	@Override
	public int getPowerPrecision() {
		return 100;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			handleStateMachine();
			break;
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		return new ModbusProtocol(unitId, //
				new FC3ReadRegistersTask(2064, Priority.LOW, // )
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(2064)), //
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(2066)) //
				), new FC3ReadRegistersTask(32528, Priority.LOW, // )
						bm(new UnsignedDoublewordElement(32528)) //
								.m(GridConChannelId.PCS_CCU_STATE_IDLE, 0) //
								.m(GridConChannelId.PCS_CCU_STATE_PRECHARGE, 1) //
								.m(GridConChannelId.PCS_CCU_STATE_STOP_PRECHARGE, 2) //
								.m(GridConChannelId.PCS_CCU_STATE_READY, 3) //
								.m(GridConChannelId.PCS_CCU_STATE_PAUSE, 4) //
								.m(GridConChannelId.PCS_CCU_STATE_RUN, 5) //
								.m(GridConChannelId.PCS_CCU_STATE_ERROR, 6) //
								.m(GridConChannelId.PCS_CCU_STATE_VOLTAGE_RAMPING_UP, 7) //
								.m(GridConChannelId.PCS_CCU_STATE_OVERLOAD, 8) //
								.m(GridConChannelId.PCS_CCU_STATE_SHORT_CIRCUIT_DETECTED, 9) //
								.m(GridConChannelId.PCS_CCU_STATE_DERATING_POWER, 10) //
								.m(GridConChannelId.PCS_CCU_STATE_DERATING_HARMONICS, 11) //
								.m(GridConChannelId.PCS_CCU_STATE_SIA_ACTIVE, 12) //
								.build().wordOrder(WordOrder.LSWMSW), //
						m(GridConChannelId.PCS_CCU_ERROR_CODE, new UnsignedDoublewordElement(32530).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CCU_VOLTAGE_U12, new FloatDoublewordElement(32532).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CCU_VOLTAGE_U23, new FloatDoublewordElement(32534).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CCU_VOLTAGE_U31, new FloatDoublewordElement(32536).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CCU_CURRENT_IL1, new FloatDoublewordElement(32538).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CCU_CURRENT_IL2, new FloatDoublewordElement(32540).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CCU_CURRENT_IL3, new FloatDoublewordElement(32542).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CCU_POWER_P, new FloatDoublewordElement(32544).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CCU_POWER_Q, new FloatDoublewordElement(32546).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CCU_FREQUENCY, new FloatDoublewordElement(32548).wordOrder(WordOrder.LSWMSW)) //
				), new FC3ReadRegistersTask(33168, Priority.LOW, //
						byteMap(new UnsignedDoublewordElement(33168)) //
								.mapByte(GridConChannelId.PCS_IPU_1_STATUS_STATUS_STATE_MACHINE, 0) //
								.mapByte(GridConChannelId.PCS_IPU_1_STATUS_STATUS_MCU, 1) //
								.build().wordOrder(WordOrder.LSWMSW), //
						m(GridConChannelId.PCS_IPU_1_STATUS_FILTER_CURRENT, new FloatDoublewordElement(33170).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_STATUS_DC_LINK_POSITIVE_VOLTAGE, new FloatDoublewordElement(33172).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_STATUS_DC_LINK_NEGATIVE_VOLTAGE, new FloatDoublewordElement(33174).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_STATUS_DC_LINK_CURRENT, new FloatDoublewordElement(33176).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_STATUS_DC_LINK_ACTIVE_POWER, new FloatDoublewordElement(33178).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_STATUS_DC_LINK_UTILIZATION, new FloatDoublewordElement(33180).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_STATUS_FAN_SPEED_MAX, new UnsignedDoublewordElement(33182).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_STATUS_FAN_SPEED_MIN, new UnsignedDoublewordElement(33184).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_STATUS_TEMPERATURE_IGBT_MAX, new FloatDoublewordElement(33186).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_STATUS_TEMPERATURE_MCU_BOARD, new FloatDoublewordElement(33188).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_STATUS_TEMPERATURE_GRID_CHOKE, new FloatDoublewordElement(33190).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_STATUS_TEMPERATURE_INVERTER_CHOKE, new FloatDoublewordElement(33192).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_STATUS_RESERVE_1, new FloatDoublewordElement(33194).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_STATUS_RESERVE_2, new FloatDoublewordElement(33196).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_STATUS_RESERVE_3, new UnsignedDoublewordElement(33198).wordOrder(WordOrder.LSWMSW)) //
				), new FC3ReadRegistersTask(33200, Priority.LOW, //
						byteMap(new UnsignedDoublewordElement(33200)) //
								.mapByte(GridConChannelId.PCS_IPU_2_STATUS_STATUS_STATE_MACHINE, 0) //
								.mapByte(GridConChannelId.PCS_IPU_2_STATUS_STATUS_MCU, 1) //
								.build().wordOrder(WordOrder.LSWMSW), //
						m(GridConChannelId.PCS_IPU_2_STATUS_FILTER_CURRENT, new FloatDoublewordElement(33202).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_STATUS_DC_LINK_POSITIVE_VOLTAGE, new FloatDoublewordElement(33204).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_STATUS_DC_LINK_NEGATIVE_VOLTAGE, new FloatDoublewordElement(33206).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_STATUS_DC_LINK_CURRENT, new FloatDoublewordElement(33208).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_STATUS_DC_LINK_ACTIVE_POWER, new FloatDoublewordElement(33210).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_STATUS_DC_LINK_UTILIZATION, new FloatDoublewordElement(33212).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_STATUS_FAN_SPEED_MAX, new UnsignedDoublewordElement(33214).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_STATUS_FAN_SPEED_MIN, new UnsignedDoublewordElement(33216).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_STATUS_TEMPERATURE_IGBT_MAX, new FloatDoublewordElement(33218).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_STATUS_TEMPERATURE_MCU_BOARD, new FloatDoublewordElement(33220).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_STATUS_TEMPERATURE_GRID_CHOKE, new FloatDoublewordElement(33222).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_STATUS_TEMPERATURE_INVERTER_CHOKE, new FloatDoublewordElement(33224).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_STATUS_RESERVE_1, new FloatDoublewordElement(33226).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_STATUS_RESERVE_2, new FloatDoublewordElement(33228).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_STATUS_RESERVE_3, new UnsignedDoublewordElement(33230).wordOrder(WordOrder.LSWMSW)) //
				), new FC3ReadRegistersTask(33232, Priority.LOW, //
						byteMap(new UnsignedDoublewordElement(33232)) //
								.mapByte(GridConChannelId.PCS_IPU_3_STATUS_STATUS_STATE_MACHINE, 0) //
								.mapByte(GridConChannelId.PCS_IPU_3_STATUS_STATUS_MCU, 1) //
								.build().wordOrder(WordOrder.LSWMSW), //
						m(GridConChannelId.PCS_IPU_3_STATUS_FILTER_CURRENT, new FloatDoublewordElement(33234).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_STATUS_DC_LINK_POSITIVE_VOLTAGE, new FloatDoublewordElement(33236).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_STATUS_DC_LINK_NEGATIVE_VOLTAGE, new FloatDoublewordElement(33238).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_STATUS_DC_LINK_CURRENT, new FloatDoublewordElement(33240).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_STATUS_DC_LINK_ACTIVE_POWER, new FloatDoublewordElement(33242).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_STATUS_DC_LINK_UTILIZATION, new FloatDoublewordElement(33244).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_STATUS_FAN_SPEED_MAX, new UnsignedDoublewordElement(33246).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_STATUS_FAN_SPEED_MIN, new UnsignedDoublewordElement(33248).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_STATUS_TEMPERATURE_IGBT_MAX, new FloatDoublewordElement(33250).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_STATUS_TEMPERATURE_MCU_BOARD, new FloatDoublewordElement(33252).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_STATUS_TEMPERATURE_GRID_CHOKE, new FloatDoublewordElement(33254).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_STATUS_TEMPERATURE_INVERTER_CHOKE, new FloatDoublewordElement(33256).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_STATUS_RESERVE_1, new FloatDoublewordElement(33258).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_STATUS_RESERVE_2, new FloatDoublewordElement(33260).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_STATUS_RESERVE_3, new UnsignedDoublewordElement(33262).wordOrder(WordOrder.LSWMSW)) //
				), new FC3ReadRegistersTask(33264, Priority.LOW, //
						byteMap(new UnsignedDoublewordElement(33264)) //
								.mapByte(GridConChannelId.PCS_IPU_4_STATUS_STATUS_STATE_MACHINE, 0) //
								.mapByte(GridConChannelId.PCS_IPU_4_STATUS_STATUS_MCU, 1) //
								.build().wordOrder(WordOrder.LSWMSW), //
						m(GridConChannelId.PCS_IPU_4_STATUS_FILTER_CURRENT, new FloatDoublewordElement(33266).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_STATUS_DC_LINK_POSITIVE_VOLTAGE, new FloatDoublewordElement(33268).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_STATUS_DC_LINK_NEGATIVE_VOLTAGE, new FloatDoublewordElement(33270).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_STATUS_DC_LINK_CURRENT, new FloatDoublewordElement(33272).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_STATUS_DC_LINK_ACTIVE_POWER, new FloatDoublewordElement(33274).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_STATUS_DC_LINK_UTILIZATION, new FloatDoublewordElement(33276).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_STATUS_FAN_SPEED_MAX, new UnsignedDoublewordElement(33278).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_STATUS_FAN_SPEED_MIN, new UnsignedDoublewordElement(33280).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_STATUS_TEMPERATURE_IGBT_MAX, new FloatDoublewordElement(33282).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_STATUS_TEMPERATURE_MCU_BOARD, new FloatDoublewordElement(33284).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_STATUS_TEMPERATURE_GRID_CHOKE, new FloatDoublewordElement(33286).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_STATUS_TEMPERATURE_INVERTER_CHOKE, new FloatDoublewordElement(33288).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_STATUS_RESERVE_1, new FloatDoublewordElement(33290).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_STATUS_RESERVE_2, new FloatDoublewordElement(33292).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_STATUS_RESERVE_3, new UnsignedDoublewordElement(33294).wordOrder(WordOrder.LSWMSW)) // TODO: is this float?
				), new FC3ReadRegistersTask(33488, Priority.LOW, //
						m(GridConChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_VOLTAGE_STRING_A, new FloatDoublewordElement(33488).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_VOLTAGE_STRING_B, new FloatDoublewordElement(33490).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_VOLTAGE_STRING_C, new FloatDoublewordElement(33492).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_CURRENT_STRING_A, new FloatDoublewordElement(33494).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_CURRENT_STRING_B, new FloatDoublewordElement(33496).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_CURRENT_STRING_C, new FloatDoublewordElement(33498).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_POWER_STRING_A, new FloatDoublewordElement(33500).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_POWER_STRING_B, new FloatDoublewordElement(33502).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_POWER_STRING_C, new FloatDoublewordElement(33504).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_UTILIZATION_STRING_A, new FloatDoublewordElement(33506).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_UTILIZATION_STRING_B, new FloatDoublewordElement(33508).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_UTILIZATION_STRING_C, new FloatDoublewordElement(33510).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_ACCUMULATED_SUM_DC_CURRENT, new FloatDoublewordElement(33512).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_ACCUMULATED_DC_UTILIZATION, new FloatDoublewordElement(33514).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_RESERVE_1, new FloatDoublewordElement(33516).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_RESERVE_2, new FloatDoublewordElement(33518).wordOrder(WordOrder.LSWMSW)) //
				), new FC3ReadRegistersTask(33520, Priority.LOW, //
						m(GridConChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_VOLTAGE_STRING_A, new FloatDoublewordElement(33520).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_VOLTAGE_STRING_B, new FloatDoublewordElement(33522).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_VOLTAGE_STRING_C, new FloatDoublewordElement(33524).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_CURRENT_STRING_A, new FloatDoublewordElement(33526).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_CURRENT_STRING_B, new FloatDoublewordElement(33528).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_CURRENT_STRING_C, new FloatDoublewordElement(33530).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_POWER_STRING_A, new FloatDoublewordElement(33532).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_POWER_STRING_B, new FloatDoublewordElement(33534).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_POWER_STRING_C, new FloatDoublewordElement(33536).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_UTILIZATION_STRING_A, new FloatDoublewordElement(33538).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_UTILIZATION_STRING_B, new FloatDoublewordElement(33540).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_UTILIZATION_STRING_C, new FloatDoublewordElement(33542).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_ACCUMULATED_SUM_DC_CURRENT, new FloatDoublewordElement(33544).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_ACCUMULATED_DC_UTILIZATION, new FloatDoublewordElement(33546).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_RESERVE_1, new FloatDoublewordElement(33548).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_RESERVE_2, new FloatDoublewordElement(33550).wordOrder(WordOrder.LSWMSW)) //
				), new FC3ReadRegistersTask(33552, Priority.LOW, //
						m(GridConChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_VOLTAGE_STRING_A, new FloatDoublewordElement(33552).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_VOLTAGE_STRING_B, new FloatDoublewordElement(33554).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_VOLTAGE_STRING_C, new FloatDoublewordElement(33556).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_CURRENT_STRING_A, new FloatDoublewordElement(33558).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_CURRENT_STRING_B, new FloatDoublewordElement(33560).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_CURRENT_STRING_C, new FloatDoublewordElement(33562).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_POWER_STRING_A, new FloatDoublewordElement(33564).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_POWER_STRING_B, new FloatDoublewordElement(33566).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_POWER_STRING_C, new FloatDoublewordElement(33568).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_UTILIZATION_STRING_A, new FloatDoublewordElement(33570).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_UTILIZATION_STRING_B, new FloatDoublewordElement(33572).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_UTILIZATION_STRING_C, new FloatDoublewordElement(33574).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_ACCUMULATED_SUM_DC_CURRENT, new FloatDoublewordElement(33576).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_ACCUMULATED_DC_UTILIZATION, new FloatDoublewordElement(33578).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_RESERVE_1, new FloatDoublewordElement(33580).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_RESERVE_2, new FloatDoublewordElement(33582).wordOrder(WordOrder.LSWMSW)) //
				), new FC3ReadRegistersTask(33584, Priority.LOW, //
						m(GridConChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_VOLTAGE_STRING_A, new FloatDoublewordElement(33584).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_VOLTAGE_STRING_B, new FloatDoublewordElement(33586).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_VOLTAGE_STRING_C, new FloatDoublewordElement(33588).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_CURRENT_STRING_A, new FloatDoublewordElement(33590).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_CURRENT_STRING_B, new FloatDoublewordElement(33592).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_CURRENT_STRING_C, new FloatDoublewordElement(33594).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_POWER_STRING_A, new FloatDoublewordElement(33596).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_POWER_STRING_B, new FloatDoublewordElement(33598).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_POWER_STRING_C, new FloatDoublewordElement(33600).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_UTILIZATION_STRING_A, new FloatDoublewordElement(33602).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_UTILIZATION_STRING_B, new FloatDoublewordElement(33604).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_UTILIZATION_STRING_C, new FloatDoublewordElement(33606).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_ACCUMULATED_SUM_DC_CURRENT, new FloatDoublewordElement(33608).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_ACCUMULATED_DC_UTILIZATION, new FloatDoublewordElement(33610).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_RESERVE_1, new FloatDoublewordElement(33612).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_RESERVE_2, new FloatDoublewordElement(33614).wordOrder(WordOrder.LSWMSW)) //
				), new FC16WriteRegistersTask(32560, //
						m(GridConChannelId.PCS_COMMAND_CONTROL_WORD, new UnsignedDoublewordElement(32560).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_COMMAND_ERROR_CODE_FALLBACK, new UnsignedDoublewordElement(32562).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_COMMAND_CONTROL_PARAMETER_U0, new FloatDoublewordElement(32564).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_COMMAND_CONTROL_PARAMETER_F0, new FloatDoublewordElement(32566).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_COMMAND_CONTROL_PARAMETER_Q_REF, new FloatDoublewordElement(32568).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_COMMAND_CONTROL_PARAMETER_P_REF, new FloatDoublewordElement(32570).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_COMMAND_TIME_SYNC_DATE, new UnsignedDoublewordElement(32572).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_COMMAND_TIME_SYNC_TIME, new UnsignedDoublewordElement(32574).wordOrder(WordOrder.LSWMSW)) //
				), new FC3ReadRegistersTask(32560, Priority.LOW, //
						m(GridConChannelId.PCS_COMMAND_CONTROL_WORD, new UnsignedDoublewordElement(32560).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_COMMAND_ERROR_CODE_FALLBACK, new UnsignedDoublewordElement(32562).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_COMMAND_CONTROL_PARAMETER_U0, new FloatDoublewordElement(32564).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_COMMAND_CONTROL_PARAMETER_F0, new FloatDoublewordElement(32566).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_COMMAND_CONTROL_PARAMETER_Q_REF, new FloatDoublewordElement(32568).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_COMMAND_CONTROL_PARAMETER_P_REF, new FloatDoublewordElement(32570).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_COMMAND_TIME_SYNC_DATE, new UnsignedDoublewordElement(32572).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_COMMAND_TIME_SYNC_TIME, new UnsignedDoublewordElement(32574).wordOrder(WordOrder.LSWMSW)) //
				), new FC16WriteRegistersTask(32592, //
						m(GridConChannelId.PCS_CONTROL_PARAMETER_U_Q_DROOP_MAIN, new FloatDoublewordElement(32592).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN, new FloatDoublewordElement(32594).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_PARAMETER_F_P_DRROP_MAIN, new FloatDoublewordElement(32596).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_PARAMETER_F_P_DROOP_T1_MAIN, new FloatDoublewordElement(32598).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_PARAMETER_Q_U_DROOP_MAIN, new FloatDoublewordElement(32600).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_PARAMETER_Q_U_DEAD_BAND, new FloatDoublewordElement(32602).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_PARAMETER_Q_LIMIT, new FloatDoublewordElement(32604).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_PARAMETER_P_F_DROOP_MAIN, new FloatDoublewordElement(32606).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_PARAMETER_P_F_DEAD_BAND, new FloatDoublewordElement(32608).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_PARAMETER_P_U_DROOP, new FloatDoublewordElement(32610).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_PARAMETER_P_U_DEAD_BAND, new FloatDoublewordElement(32612).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_PARAMETER_P_U_MAX_CHARGE, new FloatDoublewordElement(32614).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_PARAMETER_P_U_MAX_DISCHARGE, new FloatDoublewordElement(32616).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_PARAMETER_P_CONTROL_MODE, new FloatDoublewordElement(32618).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_PARAMETER_P_CONTROL_LIM_TWO, new FloatDoublewordElement(32620).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_PARAMETER_P_CONTROL_LIM_ONE, new FloatDoublewordElement(32622).wordOrder(WordOrder.LSWMSW)) //
				), new FC16WriteRegistersTask(32624, //
						m(GridConChannelId.PCS_CONTROL_IPU_1_PARAMETERS_DC_VOLTAGE_SETPOINT, new FloatDoublewordElement(32624).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_1_PARAMETERS_DC_CURRENT_SETPOINT, new FloatDoublewordElement(32626).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_1_PARAMETERS_U0_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32628).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_1_PARAMETERS_F0_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32630).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_1_PARAMETERS_Q_REF_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32632).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_1_PARAMETERS_P_REF_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32634).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_1_PARAMETERS_P_MAX_DISCHARGE, new FloatDoublewordElement(32636).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_1_PARAMETERS_P_MAX_CHARGE, new FloatDoublewordElement(32638).wordOrder(WordOrder.LSWMSW)) //
				), new FC16WriteRegistersTask(32656, //
						m(GridConChannelId.PCS_CONTROL_IPU_2_PARAMETERS_DC_VOLTAGE_SETPOINT, new FloatDoublewordElement(32656).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_2_PARAMETERS_DC_CURRENT_SETPOINT, new FloatDoublewordElement(32658).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_2_PARAMETERS_U0_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32660).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_2_PARAMETERS_F0_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32662).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_2_PARAMETERS_Q_REF_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32664).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_2_PARAMETERS_P_REF_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32666).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_2_PARAMETERS_P_MAX_DISCHARGE, new FloatDoublewordElement(32668).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_2_PARAMETERS_P_MAX_CHARGE, new FloatDoublewordElement(32670).wordOrder(WordOrder.LSWMSW)) //
				), new FC16WriteRegistersTask(32688, //
						m(GridConChannelId.PCS_CONTROL_IPU_3_PARAMETERS_DC_VOLTAGE_SETPOINT, new FloatDoublewordElement(32688).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_3_PARAMETERS_DC_CURRENT_SETPOINT, new FloatDoublewordElement(32690).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_3_PARAMETERS_U0_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32692).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_3_PARAMETERS_F0_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32694).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_3_PARAMETERS_Q_REF_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32696).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_3_PARAMETERS_P_REF_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32698).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_3_PARAMETERS_P_MAX_DISCHARGE, new FloatDoublewordElement(32700).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_3_PARAMETERS_P_MAX_CHARGE, new FloatDoublewordElement(32702).wordOrder(WordOrder.LSWMSW)) //
				),
				new FC16WriteRegistersTask(32720, m(GridConChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_DC_VOLTAGE_SETPOINT, new FloatDoublewordElement(32720).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_A, new FloatDoublewordElement(32722).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_B, new FloatDoublewordElement(32724).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_C, new FloatDoublewordElement(32726).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_I_REF_STRING_A, new FloatDoublewordElement(32728).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_I_REF_STRING_B, new FloatDoublewordElement(32730).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_I_REF_STRING_C, new FloatDoublewordElement(32732).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_DC_DC_STRING_CONTROL_MODE, new FloatDoublewordElement(32734).wordOrder(WordOrder.LSWMSW)) //
				));
	}
}
