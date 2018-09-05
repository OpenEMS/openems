package io.openems.edge.ess.mr.gridcon;

import java.time.LocalDateTime;
import java.util.BitSet;
import java.util.Optional;

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
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.OptionsEnum;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
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

	/**
	 * This enum signalizes the bit position of a command in the control word for a
	 * gridcon. The index is based on 0.
	 */
	enum PCSControlWordBitPosition {
		PLAY(0),
		READY(1),
		ACKNOWLEDGE(2),
		STOP(3),
		BLACKSTART_APPROVAL(4),
		SYNC_APPROVAL(5),
		ACTIVATE_SHORT_CIRCUIT_HANDLING(6),
		MODE_SELECTION(7),
		TRIGGER_SIA(8),
		ACTIVATE_HARMONIC_COMPENSATION(9),
		/**
		 * 1 is the value for disable, 0 is the value for enable
		 */
		DISABLE_IPU_4(28),
		/**
		 * 1 is the value for disable, 0 is the value for enable
		 */
		DISABLE_IPU_3(29),
		/**
		 * 1 is the value for disable, 0 is the value for enable
		 */
		DISABLE_IPU_2(30),
		/**
		 * 1 is the value for disable, 0 is the value for enable
		 */
		DISABLE_IPU_1(31),;

		PCSControlWordBitPosition(int value) {
			this.bitPosition = value;
		}

		private int bitPosition;

		public int getBitPosition() {
			return bitPosition;
		}
	}

	static final int MAX_APPARENT_POWER = (int) MAX_POWER_W; // TODO Checkif correct
	private CircleConstraint maxApparentPowerConstraint = null; // TODO set the constraint

	@Reference
	private Power power;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private Battery battery;

	public GridconPCS() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		// update filter for 'battery'
		if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "battery", config.battery_id())) {
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

		writeValuesBackInChannel(//
				ChannelId.PCS_COMMAND_ERROR_CODE_FALLBACK, //
				ChannelId.PCS_COMMAND_CONTROL_PARAMETER_U0, //
				ChannelId.PCS_COMMAND_CONTROL_PARAMETER_F0, //
				ChannelId.PCS_COMMAND_CONTROL_PARAMETER_Q_REF, //
				ChannelId.PCS_COMMAND_CONTROL_PARAMETER_P_REF //
		);

		writeDateAndTime();
		if (isIdle()) {
			startSystem();
		} else if (isError()) {
			doErrorHandling();
		}
	}

	/**
	 * In order to correctly communicate with a gridcon every bit in the protocol
	 * has to be sent. To ensure that all bits are set, this method sets the last
	 * value set in the channel as the next value to be written. If there was no
	 * last value 0 is set into the channel instead.
	 * 
	 * @param ids the channels which should retain their value
	 */
	void writeValuesBackInChannel(ChannelId... ids) {
		for (ChannelId id : ids) {
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

		IntegerWriteChannel dateChannel = this.channel(ChannelId.PCS_COMMAND_TIME_SYNC_DATE);
		IntegerWriteChannel timeChannel = this.channel(ChannelId.PCS_COMMAND_TIME_SYNC_TIME);

		try {
			dateChannel.setNextWriteValue(dateInteger);
			timeChannel.setNextWriteValue(timeInteger);
		} catch (OpenemsException e) {
			e.printStackTrace();
		}
	}

	private boolean isError() {
		BooleanReadChannel stateErrorChannel = this.channel(ChannelId.PCS_CCU_STATE_ERROR);
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
		BooleanReadChannel stateIdleChannel = this.channel(ChannelId.PCS_CCU_STATE_IDLE);
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
		bitSet.set(PCSControlWordBitPosition.PLAY.bitPosition, true);
		bitSet.set(PCSControlWordBitPosition.SYNC_APPROVAL.bitPosition, true);
		bitSet.set(PCSControlWordBitPosition.MODE_SELECTION.bitPosition, true);

		bitSet.set(PCSControlWordBitPosition.DISABLE_IPU_1.bitPosition, false);
		bitSet.set(PCSControlWordBitPosition.DISABLE_IPU_2.bitPosition, false);
		bitSet.set(PCSControlWordBitPosition.DISABLE_IPU_3.bitPosition, false);
		bitSet.set(PCSControlWordBitPosition.DISABLE_IPU_4.bitPosition, false);

		IntegerWriteChannel controlWordChannel = this.channel(ChannelId.PCS_COMMAND_CONTROL_WORD);
		try {
			Integer value = convertToInteger(bitSet);
			controlWordChannel.setNextWriteValue(value);
		} catch (OpenemsException e) {
			e.printStackTrace();
		}

		writeIPUParameters();
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
			((FloatWriteChannel) this.channel(ChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_DC_VOLTAGE_SETPOINT)).setNextWriteValue(800f);
			((FloatWriteChannel) this.channel(ChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_A)).setNextWriteValue(1f);
			((FloatWriteChannel) this.channel(ChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_B)).setNextWriteValue(1f);
			((FloatWriteChannel) this.channel(ChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_C)).setNextWriteValue(1f);
			((FloatWriteChannel) this.channel(ChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_DC_DC_STRING_CONTROL_MODE)).setNextWriteValue(73f);

			float pMaxCharge = 86000;
			float pMaxDischarge = -86000;

			((FloatWriteChannel) this.channel(ChannelId.PCS_CONTROL_IPU_1_PARAMETERS_P_MAX_DISCHARGE)).setNextWriteValue(pMaxDischarge);
			((FloatWriteChannel) this.channel(ChannelId.PCS_CONTROL_IPU_2_PARAMETERS_P_MAX_DISCHARGE)).setNextWriteValue(pMaxDischarge);
			((FloatWriteChannel) this.channel(ChannelId.PCS_CONTROL_IPU_3_PARAMETERS_P_MAX_DISCHARGE)).setNextWriteValue(pMaxDischarge);

			((FloatWriteChannel) this.channel(ChannelId.PCS_CONTROL_IPU_1_PARAMETERS_P_MAX_CHARGE)).setNextWriteValue(pMaxCharge);
			((FloatWriteChannel) this.channel(ChannelId.PCS_CONTROL_IPU_2_PARAMETERS_P_MAX_CHARGE)).setNextWriteValue(pMaxCharge);
			((FloatWriteChannel) this.channel(ChannelId.PCS_CONTROL_IPU_3_PARAMETERS_P_MAX_CHARGE)).setNextWriteValue(pMaxCharge);

			((IntegerWriteChannel) this.channel(ChannelId.PCS_CONTROL_PARAMETER_P_CONTROL_MODE)).setNextWriteValue(PControlMode.ACTIVE_POWER_CONTROL.getValue());

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
		bitSet.set(PCSControlWordBitPosition.ACKNOWLEDGE.bitPosition, true);

		// TODO: writebackintochannel method better?
		bitSet.set(PCSControlWordBitPosition.DISABLE_IPU_1.bitPosition, false);
		bitSet.set(PCSControlWordBitPosition.DISABLE_IPU_2.bitPosition, false);
		bitSet.set(PCSControlWordBitPosition.DISABLE_IPU_3.bitPosition, false);
		bitSet.set(PCSControlWordBitPosition.DISABLE_IPU_4.bitPosition, false);

		bitSet.set(PCSControlWordBitPosition.SYNC_APPROVAL.bitPosition, true);
		bitSet.set(PCSControlWordBitPosition.MODE_SELECTION.bitPosition, true);

		IntegerWriteChannel controlWordChannel = this.channel(ChannelId.PCS_COMMAND_CONTROL_WORD);
		try {
			Integer value = convertToInteger(bitSet);
			controlWordChannel.setNextWriteValue(value);
		} catch (OpenemsException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String debugLog() {
		return "Current state: " + this.channel(ChannelId.SYSTEM_STATE_STATE_MACHINE).value().asOptionString();
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
		FloatWriteChannel channelPRef = this.channel(ChannelId.PCS_COMMAND_CONTROL_PARAMETER_P_REF);
		FloatWriteChannel channelQRef = this.channel(ChannelId.PCS_COMMAND_CONTROL_PARAMETER_Q_REF);
		writeValuesBackInChannel(new ChannelId[] { ChannelId.PCS_COMMAND_CONTROL_WORD });
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
		// TODO: this is 12.5 => very bad with cast. Maybe change to 25?
		return (int) (MAX_POWER_W * 0.0001);
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

	// TODO numbers are not correctly
	public enum CurrentState implements OptionsEnum { // see Software manual chapter 5.1
		OFFLINE(1, "Offline"),
		INIT(2, "Init"),
		IDLE(3, "Idle"),
		PRECHARGE(4, "Precharge"),
		STOP_PRECHARGE(5, "Stop precharge"),
		ECO(6, "Eco"),
		PAUSE(7, "Pause"),
		RUN(8, "Run"),
		ERROR(99, "Error");

		int value;
		String option;

		private CurrentState(int value, String option) {
			this.value = value;
			this.option = option;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getOption() {
			return option;
		}
	}

	public enum PControlMode implements OptionsEnum {
		DISABLED(1, "Disabled"), // TODO Check values!!!
		ACTIVE_POWER_CONTROL(0x3F80, "Active Power Control Mode"),
		POWER_LIMITER(4, "Power Limiter Mode");

		private int value;
		String option;

		private PControlMode(int value, String option) {
			this.value = value;
			this.option = option;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getOption() {
			return option;
		}
	}

	public enum Command implements OptionsEnum { // see manual(Betriebsanleitung Feldbus Konfiguration (Anybus-Modul)) page 15
		PLAY(1, "Start active filter"),
		PAUSE(2, "Set outgoing current of ACF to zero"),
		ACKNOWLEDGE(4, "Achnowledge errors"),
		STOP(8, "Switch off");

		int value;
		String option;

		private Command(int value, String option) {
			this.value = value;
			this.option = option;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getOption() {
			return option;
		}
	}

	// TODO Is this implemented according SunSpec?
	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		SYSTEM_STATE_STATE_MACHINE(new Doc().options(CurrentState.values())),
		SYSTEM_CURRENT_PARAMETER_SET(new Doc()),
		SYSTEM_UTILIZATION(new Doc().unit(Unit.PERCENT)),
		SYSTEM_SERVICE_MODE(new Doc().unit(Unit.ON_OFF)),
		SYSTEM_REMOTE_MODE(new Doc().unit(Unit.ON_OFF)),
		SYSTEM_MEASUREMENTS_LIFEBIT(new Doc().unit(Unit.ON_OFF)),
		SYSTEM_CCU_LIFEBIT(new Doc().unit(Unit.ON_OFF)),
		SYSTEM_NUMBER_ERROR_WARNINGS(new Doc().unit(Unit.NONE)),
		SYSTEM_COMMAND(new Doc().options(Command.values())),
		SYSTEM_PARAMETER_SET(new Doc()),
		SYSTEM_FIELDBUS_DEVICE_LIFEBIT(new Doc().unit(Unit.ON_OFF)),
		SYSTEM_ERROR_CODE(new Doc().unit(Unit.NONE)),
		SYSTEM_ERROR_ACKNOWLEDGE(new Doc().unit(Unit.NONE)),

		PCS_CCU_STATE_IDLE(new Doc()),
		PCS_CCU_STATE_PRE_CHARGE(new Doc()),
		PCS_CCU_STATE_STOP_PRE_CHARGE(new Doc()),
		PCS_CCU_STATE_READY(new Doc()),
		PCS_CCU_STATE_PAUSE(new Doc()),
		PCS_CCU_STATE_RUN(new Doc()),
		PCS_CCU_STATE_ERROR(new Doc()),
		PCS_CCU_STATE_VOLTAGE_RAMPING_UP(new Doc()),
		PCS_CCU_STATE_OVERLOAD(new Doc()),
		PCS_CCU_STATE_SHORT_CIRCUIT_DETECTED(new Doc()),
		PCS_CCU_STATE_DERATING_POWER(new Doc()),
		PCS_CCU_STATE_DERATING_HARMONICS(new Doc()),
		PCS_CCU_STATE_SIA_ACTIVE(new Doc()),
		PCS_CCU_ERROR_CODE(new Doc().unit(Unit.NONE)),
		PCS_CCU_VOLTAGE_U12(new Doc().unit(Unit.VOLT)),
		PCS_CCU_VOLTAGE_U23(new Doc().unit(Unit.VOLT)),
		PCS_CCU_VOLTAGE_U31(new Doc().unit(Unit.VOLT)),
		PCS_CCU_CURRENT_IL1(new Doc().unit(Unit.AMPERE)),
		PCS_CCU_CURRENT_IL2(new Doc().unit(Unit.AMPERE)),
		PCS_CCU_CURRENT_IL3(new Doc().unit(Unit.AMPERE)),
		PCS_CCU_POWER_P(new Doc().unit(Unit.WATT)),
		PCS_CCU_POWER_Q(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)),
		PCS_CCU_FREQUENCY(new Doc().unit(Unit.HERTZ)),

		PCS_IPU_1_STATUS_STATUS(new Doc()),
		PCS_IPU_1_STATUS_FILTER_CURRENT(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_1_STATUS_DC_LINK_POSITIVE_VOLTAGE(new Doc().unit(Unit.VOLT)),
		PCS_IPU_1_STATUS_DC_LINK_NEGATIVE_VOLTAGE(new Doc().unit(Unit.VOLT)),
		PCS_IPU_1_STATUS_DC_LINK_CURRENT(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_1_STATUS_DC_LINK_ACTIVE_POWER(new Doc().unit(Unit.KILOWATT)),
		PCS_IPU_1_STATUS_DC_LINK_UTILIZATION(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_1_STATUS_FAN_SPEED_MAX(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_1_STATUS_FAN_SPEED_MIN(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_1_STATUS_TEMPERATURE_IGBT_MAX(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_1_STATUS_TEMPERATURE_MCU_BOARD(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_1_STATUS_TEMPERATURE_GRID_CHOKE(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_1_STATUS_TEMPERATURE_INVERTER_CHOKE(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_1_STATUS_RESERVE_1(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_1_STATUS_RESERVE_2(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_1_STATUS_RESERVE_3(new Doc().unit(Unit.DEGREE_CELSIUS)),

		PCS_IPU_2_STATUS_STATUS(new Doc()),
		PCS_IPU_2_STATUS_FILTER_CURRENT(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_2_STATUS_DC_LINK_POSITIVE_VOLTAGE(new Doc().unit(Unit.VOLT)),
		PCS_IPU_2_STATUS_DC_LINK_NEGATIVE_VOLTAGE(new Doc().unit(Unit.VOLT)),
		PCS_IPU_2_STATUS_DC_LINK_CURRENT(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_2_STATUS_DC_LINK_ACTIVE_POWER(new Doc().unit(Unit.KILOWATT)),
		PCS_IPU_2_STATUS_DC_LINK_UTILIZATION(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_2_STATUS_FAN_SPEED_MAX(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_2_STATUS_FAN_SPEED_MIN(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_2_STATUS_TEMPERATURE_IGBT_MAX(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_2_STATUS_TEMPERATURE_MCU_BOARD(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_2_STATUS_TEMPERATURE_GRID_CHOKE(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_2_STATUS_TEMPERATURE_INVERTER_CHOKE(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_2_STATUS_RESERVE_1(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_2_STATUS_RESERVE_2(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_2_STATUS_RESERVE_3(new Doc().unit(Unit.DEGREE_CELSIUS)),

		PCS_IPU_3_STATUS_STATUS(new Doc()),
		PCS_IPU_3_STATUS_FILTER_CURRENT(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_3_STATUS_DC_LINK_POSITIVE_VOLTAGE(new Doc().unit(Unit.VOLT)),
		PCS_IPU_3_STATUS_DC_LINK_NEGATIVE_VOLTAGE(new Doc().unit(Unit.VOLT)),
		PCS_IPU_3_STATUS_DC_LINK_CURRENT(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_3_STATUS_DC_LINK_ACTIVE_POWER(new Doc().unit(Unit.KILOWATT)),
		PCS_IPU_3_STATUS_DC_LINK_UTILIZATION(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_3_STATUS_FAN_SPEED_MAX(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_3_STATUS_FAN_SPEED_MIN(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_3_STATUS_TEMPERATURE_IGBT_MAX(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_3_STATUS_TEMPERATURE_MCU_BOARD(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_3_STATUS_TEMPERATURE_GRID_CHOKE(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_3_STATUS_TEMPERATURE_INVERTER_CHOKE(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_3_STATUS_RESERVE_1(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_3_STATUS_RESERVE_2(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_3_STATUS_RESERVE_3(new Doc().unit(Unit.DEGREE_CELSIUS)),

		PCS_IPU_4_STATUS_STATUS(new Doc()),
		PCS_IPU_4_STATUS_FILTER_CURRENT(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_4_STATUS_DC_LINK_POSITIVE_VOLTAGE(new Doc().unit(Unit.VOLT)),
		PCS_IPU_4_STATUS_DC_LINK_NEGATIVE_VOLTAGE(new Doc().unit(Unit.VOLT)),
		PCS_IPU_4_STATUS_DC_LINK_CURRENT(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_4_STATUS_DC_LINK_ACTIVE_POWER(new Doc().unit(Unit.KILOWATT)),
		PCS_IPU_4_STATUS_DC_LINK_UTILIZATION(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_4_STATUS_FAN_SPEED_MAX(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_4_STATUS_FAN_SPEED_MIN(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_4_STATUS_TEMPERATURE_IGBT_MAX(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_4_STATUS_TEMPERATURE_MCU_BOARD(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_4_STATUS_TEMPERATURE_GRID_CHOKE(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_4_STATUS_TEMPERATURE_INVERTER_CHOKE(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_4_STATUS_RESERVE_1(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_4_STATUS_RESERVE_2(new Doc().unit(Unit.DEGREE_CELSIUS)),
		PCS_IPU_4_STATUS_RESERVE_3(new Doc().unit(Unit.DEGREE_CELSIUS)),

		PCS_IPU_1_DC_DC_MEASUREMENTS_VOLTAGE_STRING_A(new Doc().unit(Unit.VOLT)),
		PCS_IPU_1_DC_DC_MEASUREMENTS_VOLTAGE_STRING_B(new Doc().unit(Unit.VOLT)),
		PCS_IPU_1_DC_DC_MEASUREMENTS_VOLTAGE_STRING_C(new Doc().unit(Unit.VOLT)),
		PCS_IPU_1_DC_DC_MEASUREMENTS_CURRENT_STRING_A(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_1_DC_DC_MEASUREMENTS_CURRENT_STRING_B(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_1_DC_DC_MEASUREMENTS_CURRENT_STRING_C(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_1_DC_DC_MEASUREMENTS_POWER_STRING_A(new Doc().unit(Unit.KILOWATT)),
		PCS_IPU_1_DC_DC_MEASUREMENTS_POWER_STRING_B(new Doc().unit(Unit.KILOWATT)),
		PCS_IPU_1_DC_DC_MEASUREMENTS_POWER_STRING_C(new Doc().unit(Unit.KILOWATT)),
		PCS_IPU_1_DC_DC_MEASUREMENTS_UTILIZATION_STRING_A(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_1_DC_DC_MEASUREMENTS_UTILIZATION_STRING_B(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_1_DC_DC_MEASUREMENTS_UTILIZATION_STRING_C(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_1_DC_DC_MEASUREMENTS_ACCUMULATED_SUM_DC_CURRENT(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_1_DC_DC_MEASUREMENTS_ACCUMULATED_DC_UTILIZATION(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_1_DC_DC_MEASUREMENTS_RESERVE_1(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_1_DC_DC_MEASUREMENTS_RESERVE_2(new Doc().unit(Unit.PERCENT)),

		PCS_IPU_2_DC_DC_MEASUREMENTS_VOLTAGE_STRING_A(new Doc().unit(Unit.VOLT)),
		PCS_IPU_2_DC_DC_MEASUREMENTS_VOLTAGE_STRING_B(new Doc().unit(Unit.VOLT)),
		PCS_IPU_2_DC_DC_MEASUREMENTS_VOLTAGE_STRING_C(new Doc().unit(Unit.VOLT)),
		PCS_IPU_2_DC_DC_MEASUREMENTS_CURRENT_STRING_A(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_2_DC_DC_MEASUREMENTS_CURRENT_STRING_B(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_2_DC_DC_MEASUREMENTS_CURRENT_STRING_C(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_2_DC_DC_MEASUREMENTS_POWER_STRING_A(new Doc().unit(Unit.KILOWATT)),
		PCS_IPU_2_DC_DC_MEASUREMENTS_POWER_STRING_B(new Doc().unit(Unit.KILOWATT)),
		PCS_IPU_2_DC_DC_MEASUREMENTS_POWER_STRING_C(new Doc().unit(Unit.KILOWATT)),
		PCS_IPU_2_DC_DC_MEASUREMENTS_UTILIZATION_STRING_A(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_2_DC_DC_MEASUREMENTS_UTILIZATION_STRING_B(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_2_DC_DC_MEASUREMENTS_UTILIZATION_STRING_C(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_2_DC_DC_MEASUREMENTS_ACCUMULATED_SUM_DC_CURRENT(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_2_DC_DC_MEASUREMENTS_ACCUMULATED_DC_UTILIZATION(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_2_DC_DC_MEASUREMENTS_RESERVE_1(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_2_DC_DC_MEASUREMENTS_RESERVE_2(new Doc().unit(Unit.PERCENT)),

		PCS_IPU_3_DC_DC_MEASUREMENTS_VOLTAGE_STRING_A(new Doc().unit(Unit.VOLT)),
		PCS_IPU_3_DC_DC_MEASUREMENTS_VOLTAGE_STRING_B(new Doc().unit(Unit.VOLT)),
		PCS_IPU_3_DC_DC_MEASUREMENTS_VOLTAGE_STRING_C(new Doc().unit(Unit.VOLT)),
		PCS_IPU_3_DC_DC_MEASUREMENTS_CURRENT_STRING_A(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_3_DC_DC_MEASUREMENTS_CURRENT_STRING_B(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_3_DC_DC_MEASUREMENTS_CURRENT_STRING_C(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_3_DC_DC_MEASUREMENTS_POWER_STRING_A(new Doc().unit(Unit.KILOWATT)),
		PCS_IPU_3_DC_DC_MEASUREMENTS_POWER_STRING_B(new Doc().unit(Unit.KILOWATT)),
		PCS_IPU_3_DC_DC_MEASUREMENTS_POWER_STRING_C(new Doc().unit(Unit.KILOWATT)),
		PCS_IPU_3_DC_DC_MEASUREMENTS_UTILIZATION_STRING_A(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_3_DC_DC_MEASUREMENTS_UTILIZATION_STRING_B(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_3_DC_DC_MEASUREMENTS_UTILIZATION_STRING_C(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_3_DC_DC_MEASUREMENTS_ACCUMULATED_SUM_DC_CURRENT(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_3_DC_DC_MEASUREMENTS_ACCUMULATED_DC_UTILIZATION(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_3_DC_DC_MEASUREMENTS_RESERVE_1(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_3_DC_DC_MEASUREMENTS_RESERVE_2(new Doc().unit(Unit.PERCENT)),

		PCS_IPU_4_DC_DC_MEASUREMENTS_VOLTAGE_STRING_A(new Doc().unit(Unit.VOLT)),
		PCS_IPU_4_DC_DC_MEASUREMENTS_VOLTAGE_STRING_B(new Doc().unit(Unit.VOLT)),
		PCS_IPU_4_DC_DC_MEASUREMENTS_VOLTAGE_STRING_C(new Doc().unit(Unit.VOLT)),
		PCS_IPU_4_DC_DC_MEASUREMENTS_CURRENT_STRING_A(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_4_DC_DC_MEASUREMENTS_CURRENT_STRING_B(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_4_DC_DC_MEASUREMENTS_CURRENT_STRING_C(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_4_DC_DC_MEASUREMENTS_POWER_STRING_A(new Doc().unit(Unit.KILOWATT)),
		PCS_IPU_4_DC_DC_MEASUREMENTS_POWER_STRING_B(new Doc().unit(Unit.KILOWATT)),
		PCS_IPU_4_DC_DC_MEASUREMENTS_POWER_STRING_C(new Doc().unit(Unit.KILOWATT)),
		PCS_IPU_4_DC_DC_MEASUREMENTS_UTILIZATION_STRING_A(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_4_DC_DC_MEASUREMENTS_UTILIZATION_STRING_B(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_4_DC_DC_MEASUREMENTS_UTILIZATION_STRING_C(new Doc().unit(Unit.PERCENT)),
		PCS_IPU_4_DC_DC_MEASUREMENTS_ACCUMULATED_SUM_DC_CURRENT(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_4_DC_DC_MEASUREMENTS_ACCUMULATED_DC_UTILIZATION(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_4_DC_DC_MEASUREMENTS_RESERVE_1(new Doc().unit(Unit.AMPERE)),
		PCS_IPU_4_DC_DC_MEASUREMENTS_RESERVE_2(new Doc().unit(Unit.PERCENT)),

		PCS_COMMAND_CONTROL_WORD(new Doc().unit(Unit.NONE)),
		PCS_COMMAND_CONTROL_WORD_PLAY(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_READY(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ACKNOWLEDGE(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_STOP(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_BLACKSTART_APPROVAL(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_SYNC_APPROVAL(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ACTIVATE_SHORT_CIRCUIT_HANDLING(new Doc().unit(Unit.ON_OFF)), //
		PCS_COMMAND_CONTROL_WORD_MODE_SELECTION(new Doc().unit(Unit.ON_OFF)), // 0=voltage control, 1=current
																				// control
		PCS_COMMAND_CONTROL_WORD_TRIGGER_SIA(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ACTIVATE_HARMONIC_COMPENSATION(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ID_1_SD_CARD_PARAMETER_SET(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ID_2_SD_CARD_PARAMETER_SET(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ID_3_SD_CARD_PARAMETER_SET(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ID_4_SD_CARD_PARAMETER_SET(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ENABLE_IPU_4(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ENABLE_IPU_3(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ENABLE_IPU_2(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ENABLE_IPU_1(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_ERROR_CODE_FALLBACK(new Doc()),
		PCS_COMMAND_CONTROL_PARAMETER_U0(new Doc()),
		PCS_COMMAND_CONTROL_PARAMETER_F0(new Doc()),
		PCS_COMMAND_CONTROL_PARAMETER_Q_REF(new Doc()),
		PCS_COMMAND_CONTROL_PARAMETER_P_REF(new Doc()),
		PCS_COMMAND_TIME_SYNC_DATE(new Doc()),
		PCS_COMMAND_TIME_SYNC_TIME(new Doc()),

		PCS_CONTROL_PARAMETER_U_Q_DROOP_MAIN(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN(new Doc().unit(Unit.SECONDS)),
		PCS_CONTROL_PARAMETER_F_P_DRROP_MAIN(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_PARAMETER_F_P_DROOP_T1_MAIN(new Doc().unit(Unit.SECONDS)),
		PCS_CONTROL_PARAMETER_Q_U_DROOP_MAIN(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_PARAMETER_Q_U_DEAD_BAND(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_PARAMETER_Q_LIMIT(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_PARAMETER_P_F_DROOP_MAIN(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_PARAMETER_P_F_DEAD_BAND(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_PARAMETER_P_U_DROOP(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_PARAMETER_P_U_DEAD_BAND(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_PARAMETER_P_U_MAX_CHARGE(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_PARAMETER_P_U_MAX_DISCHARGE(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_PARAMETER_P_CONTROL_MODE(new Doc().options(PControlMode.values())),
		PCS_CONTROL_PARAMETER_P_CONTROL_LIM_TWO(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_PARAMETER_P_CONTROL_LIM_ONE(new Doc().unit(Unit.NONE)),

		PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_DC_VOLTAGE_SETPOINT(new Doc().unit(Unit.VOLT)),
		PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_A(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_B(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_C(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_I_REF_STRING_A(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_I_REF_STRING_B(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_I_REF_STRING_C(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_DC_DC_STRING_CONTROL_MODE(new Doc().unit(Unit.NONE)),

		PCS_CONTROL_IPU_3_PARAMETERS_DC_VOLTAGE_SETPOINT(new Doc().unit(Unit.VOLT)),
		PCS_CONTROL_IPU_3_PARAMETERS_DC_CURRENT_SETPOINT(new Doc().unit(Unit.AMPERE)),
		PCS_CONTROL_IPU_3_PARAMETERS_U0_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_IPU_3_PARAMETERS_F0_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_IPU_3_PARAMETERS_Q_REF_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_IPU_3_PARAMETERS_P_REF_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_IPU_3_PARAMETERS_P_MAX_DISCHARGE(new Doc().unit(Unit.WATT)),
		PCS_CONTROL_IPU_3_PARAMETERS_P_MAX_CHARGE(new Doc().unit(Unit.WATT)),

		PCS_CONTROL_IPU_2_PARAMETERS_DC_VOLTAGE_SETPOINT(new Doc().unit(Unit.VOLT)),
		PCS_CONTROL_IPU_2_PARAMETERS_DC_CURRENT_SETPOINT(new Doc().unit(Unit.AMPERE)),
		PCS_CONTROL_IPU_2_PARAMETERS_U0_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_IPU_2_PARAMETERS_F0_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_IPU_2_PARAMETERS_Q_REF_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_IPU_2_PARAMETERS_P_REF_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_IPU_2_PARAMETERS_P_MAX_DISCHARGE(new Doc().unit(Unit.WATT)),
		PCS_CONTROL_IPU_2_PARAMETERS_P_MAX_CHARGE(new Doc().unit(Unit.WATT)),

		PCS_CONTROL_IPU_1_PARAMETERS_DC_VOLTAGE_SETPOINT(new Doc().unit(Unit.VOLT)),
		PCS_CONTROL_IPU_1_PARAMETERS_DC_CURRENT_SETPOINT(new Doc().unit(Unit.AMPERE)),
		PCS_CONTROL_IPU_1_PARAMETERS_U0_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_IPU_1_PARAMETERS_F0_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_IPU_1_PARAMETERS_Q_REF_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_IPU_1_PARAMETERS_P_REF_OFFSET_TO_CCU_VALUE(new Doc().unit(Unit.NONE)),
		PCS_CONTROL_IPU_1_PARAMETERS_P_MAX_DISCHARGE(new Doc().unit(Unit.WATT)),
		PCS_CONTROL_IPU_1_PARAMETERS_P_MAX_CHARGE(new Doc().unit(Unit.WATT)),;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		return new ModbusProtocol(unitId, //
				new FC3ReadRegistersTask(528, Priority.LOW, //
						m(GridconPCS.ChannelId.SYSTEM_STATE_STATE_MACHINE, new UnsignedWordElement(528)), //
						m(GridconPCS.ChannelId.SYSTEM_CURRENT_PARAMETER_SET, new UnsignedWordElement(529)), //
						// TODO parameter set and utilization are separated in half bytes,how to handle?
						bm(new UnsignedWordElement(530)) //
								.m(GridconPCS.ChannelId.SYSTEM_SERVICE_MODE, 1) //
								.m(GridconPCS.ChannelId.SYSTEM_REMOTE_MODE, 2) //
								.m(GridconPCS.ChannelId.SYSTEM_MEASUREMENTS_LIFEBIT, 6) //
								.m(GridconPCS.ChannelId.SYSTEM_CCU_LIFEBIT, 7) //
								.build(), //
						m(GridconPCS.ChannelId.SYSTEM_NUMBER_ERROR_WARNINGS, new UnsignedWordElement(531)) //
				), new FC16WriteRegistersTask(560, m(GridconPCS.ChannelId.SYSTEM_COMMAND, new UnsignedWordElement(560)), //
						m(GridconPCS.ChannelId.SYSTEM_PARAMETER_SET, new UnsignedWordElement(561)), //
						bm(new UnsignedWordElement(562)) //
								.m(GridconPCS.ChannelId.SYSTEM_FIELDBUS_DEVICE_LIFEBIT, 7) //
								.build() //
				), new FC3ReadRegistersTask(592, Priority.LOW, // )
						m(GridconPCS.ChannelId.SYSTEM_ERROR_CODE, new UnsignedDoublewordElement(592)) //
				), new FC16WriteRegistersTask(624, //
						m(GridconPCS.ChannelId.SYSTEM_ERROR_ACKNOWLEDGE, new UnsignedDoublewordElement(624))),
				new FC3ReadRegistersTask(2064, Priority.LOW, // )
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(2064)), //
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(2066)) //
				), new FC3ReadRegistersTask(32528, Priority.LOW, // )
						bm(new UnsignedDoublewordElement(32528)) //
								.m(GridconPCS.ChannelId.PCS_CCU_STATE_IDLE, 0) //
								.m(GridconPCS.ChannelId.PCS_CCU_STATE_PRE_CHARGE, 1) //
								.m(GridconPCS.ChannelId.PCS_CCU_STATE_STOP_PRE_CHARGE, 2) //
								.m(GridconPCS.ChannelId.PCS_CCU_STATE_READY, 3) //
								.m(GridconPCS.ChannelId.PCS_CCU_STATE_PAUSE, 4) //
								.m(GridconPCS.ChannelId.PCS_CCU_STATE_RUN, 5) //
								.m(GridconPCS.ChannelId.PCS_CCU_STATE_ERROR, 6) //
								.m(GridconPCS.ChannelId.PCS_CCU_STATE_VOLTAGE_RAMPING_UP, 7) //
								.m(GridconPCS.ChannelId.PCS_CCU_STATE_OVERLOAD, 8) //
								.m(GridconPCS.ChannelId.PCS_CCU_STATE_SHORT_CIRCUIT_DETECTED, 9) //
								.m(GridconPCS.ChannelId.PCS_CCU_STATE_DERATING_POWER, 10) //
								.m(GridconPCS.ChannelId.PCS_CCU_STATE_DERATING_HARMONICS, 11) //
								.m(GridconPCS.ChannelId.PCS_CCU_STATE_SIA_ACTIVE, 12) //
								.build().wordOrder(WordOrder.LSWMSW), //
						m(GridconPCS.ChannelId.PCS_CCU_ERROR_CODE, new UnsignedDoublewordElement(32530)), //
						m(GridconPCS.ChannelId.PCS_CCU_VOLTAGE_U12, new FloatDoublewordElement(32532)), //
						m(GridconPCS.ChannelId.PCS_CCU_VOLTAGE_U23, new FloatDoublewordElement(32534)), //
						m(GridconPCS.ChannelId.PCS_CCU_VOLTAGE_U31, new FloatDoublewordElement(32536)), //
						m(GridconPCS.ChannelId.PCS_CCU_CURRENT_IL1, new FloatDoublewordElement(32538)), //
						m(GridconPCS.ChannelId.PCS_CCU_CURRENT_IL2, new FloatDoublewordElement(32540)), //
						m(GridconPCS.ChannelId.PCS_CCU_CURRENT_IL3, new FloatDoublewordElement(32542)), //
						m(GridconPCS.ChannelId.PCS_CCU_POWER_P, new FloatDoublewordElement(32544)), //
						m(GridconPCS.ChannelId.PCS_CCU_POWER_Q, new FloatDoublewordElement(32546)), //
						m(GridconPCS.ChannelId.PCS_CCU_FREQUENCY, new FloatDoublewordElement(32548)) //
				), new FC3ReadRegistersTask(33168, Priority.LOW, //
						m(GridconPCS.ChannelId.PCS_IPU_1_STATUS_STATUS, new UnsignedDoublewordElement(33168)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_STATUS_FILTER_CURRENT, new FloatDoublewordElement(33170)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_STATUS_DC_LINK_POSITIVE_VOLTAGE, new FloatDoublewordElement(33172)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_STATUS_DC_LINK_NEGATIVE_VOLTAGE, new FloatDoublewordElement(33174)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_STATUS_DC_LINK_CURRENT, new FloatDoublewordElement(33176)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_STATUS_DC_LINK_ACTIVE_POWER, new FloatDoublewordElement(33178)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_STATUS_DC_LINK_UTILIZATION, new FloatDoublewordElement(33180)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_STATUS_FAN_SPEED_MAX, new UnsignedDoublewordElement(33182)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_STATUS_FAN_SPEED_MIN, new UnsignedDoublewordElement(33184)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_STATUS_TEMPERATURE_IGBT_MAX, new FloatDoublewordElement(33186)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_STATUS_TEMPERATURE_MCU_BOARD, new FloatDoublewordElement(33188)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_STATUS_TEMPERATURE_GRID_CHOKE, new FloatDoublewordElement(33190)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_STATUS_TEMPERATURE_INVERTER_CHOKE, new FloatDoublewordElement(33192)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_STATUS_RESERVE_1, new FloatDoublewordElement(33194)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_STATUS_RESERVE_2, new FloatDoublewordElement(33196)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_STATUS_RESERVE_3, new UnsignedDoublewordElement(33198)) //
				), new FC3ReadRegistersTask(33200, Priority.LOW, //
						m(GridconPCS.ChannelId.PCS_IPU_2_STATUS_STATUS, new UnsignedDoublewordElement(33200)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_STATUS_FILTER_CURRENT, new FloatDoublewordElement(33202)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_STATUS_DC_LINK_POSITIVE_VOLTAGE, new FloatDoublewordElement(33204)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_STATUS_DC_LINK_NEGATIVE_VOLTAGE, new FloatDoublewordElement(33206)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_STATUS_DC_LINK_CURRENT, new FloatDoublewordElement(33208)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_STATUS_DC_LINK_ACTIVE_POWER, new FloatDoublewordElement(33210)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_STATUS_DC_LINK_UTILIZATION, new FloatDoublewordElement(33212)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_STATUS_FAN_SPEED_MAX, new UnsignedDoublewordElement(33214)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_STATUS_FAN_SPEED_MIN, new UnsignedDoublewordElement(33216)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_STATUS_TEMPERATURE_IGBT_MAX, new FloatDoublewordElement(33218)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_STATUS_TEMPERATURE_MCU_BOARD, new FloatDoublewordElement(33220)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_STATUS_TEMPERATURE_GRID_CHOKE, new FloatDoublewordElement(33222)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_STATUS_TEMPERATURE_INVERTER_CHOKE, new FloatDoublewordElement(33224)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_STATUS_RESERVE_1, new FloatDoublewordElement(33226)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_STATUS_RESERVE_2, new FloatDoublewordElement(33228)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_STATUS_RESERVE_3, new UnsignedDoublewordElement(33230)) //
				), new FC3ReadRegistersTask(33232, Priority.LOW, //
						m(GridconPCS.ChannelId.PCS_IPU_3_STATUS_STATUS, new UnsignedDoublewordElement(33232)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_STATUS_FILTER_CURRENT, new FloatDoublewordElement(33234)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_STATUS_DC_LINK_POSITIVE_VOLTAGE, new FloatDoublewordElement(33236)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_STATUS_DC_LINK_NEGATIVE_VOLTAGE, new FloatDoublewordElement(33238)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_STATUS_DC_LINK_CURRENT, new FloatDoublewordElement(33240)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_STATUS_DC_LINK_ACTIVE_POWER, new FloatDoublewordElement(33242)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_STATUS_DC_LINK_UTILIZATION, new FloatDoublewordElement(33244)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_STATUS_FAN_SPEED_MAX, new UnsignedDoublewordElement(33246)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_STATUS_FAN_SPEED_MIN, new UnsignedDoublewordElement(33248)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_STATUS_TEMPERATURE_IGBT_MAX, new FloatDoublewordElement(33250)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_STATUS_TEMPERATURE_MCU_BOARD, new FloatDoublewordElement(33252)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_STATUS_TEMPERATURE_GRID_CHOKE, new FloatDoublewordElement(33254)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_STATUS_TEMPERATURE_INVERTER_CHOKE, new FloatDoublewordElement(33256)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_STATUS_RESERVE_1, new FloatDoublewordElement(33258)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_STATUS_RESERVE_2, new FloatDoublewordElement(33260)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_STATUS_RESERVE_3, new UnsignedDoublewordElement(33262)) //
				), new FC3ReadRegistersTask(33264, Priority.LOW, //
						m(GridconPCS.ChannelId.PCS_IPU_4_STATUS_STATUS, new UnsignedDoublewordElement(33264)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_STATUS_FILTER_CURRENT, new FloatDoublewordElement(33266)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_STATUS_DC_LINK_POSITIVE_VOLTAGE, new FloatDoublewordElement(33268)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_STATUS_DC_LINK_NEGATIVE_VOLTAGE, new FloatDoublewordElement(33270)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_STATUS_DC_LINK_CURRENT, new FloatDoublewordElement(33272)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_STATUS_DC_LINK_ACTIVE_POWER, new FloatDoublewordElement(33274)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_STATUS_DC_LINK_UTILIZATION, new FloatDoublewordElement(33276)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_STATUS_FAN_SPEED_MAX, new UnsignedDoublewordElement(33278)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_STATUS_FAN_SPEED_MIN, new UnsignedDoublewordElement(33280)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_STATUS_TEMPERATURE_IGBT_MAX, new FloatDoublewordElement(33282)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_STATUS_TEMPERATURE_MCU_BOARD, new FloatDoublewordElement(33284)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_STATUS_TEMPERATURE_GRID_CHOKE, new FloatDoublewordElement(33286)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_STATUS_TEMPERATURE_INVERTER_CHOKE, new FloatDoublewordElement(33288)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_STATUS_RESERVE_1, new FloatDoublewordElement(33290)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_STATUS_RESERVE_2, new FloatDoublewordElement(33292)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_STATUS_RESERVE_3, new UnsignedDoublewordElement(33294)) //
				), new FC3ReadRegistersTask(33488, Priority.LOW, //
						m(GridconPCS.ChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_VOLTAGE_STRING_A, new FloatDoublewordElement(33488)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_VOLTAGE_STRING_B, new FloatDoublewordElement(33490)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_VOLTAGE_STRING_C, new FloatDoublewordElement(33492)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_CURRENT_STRING_A, new FloatDoublewordElement(33494)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_CURRENT_STRING_B, new FloatDoublewordElement(33496)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_CURRENT_STRING_C, new FloatDoublewordElement(33498)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_POWER_STRING_A, new FloatDoublewordElement(33500)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_POWER_STRING_B, new FloatDoublewordElement(33502)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_POWER_STRING_C, new FloatDoublewordElement(33504)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_UTILIZATION_STRING_A, new FloatDoublewordElement(33506)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_UTILIZATION_STRING_B, new FloatDoublewordElement(33508)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_UTILIZATION_STRING_C, new FloatDoublewordElement(33510)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_ACCUMULATED_SUM_DC_CURRENT, new FloatDoublewordElement(33512)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_ACCUMULATED_DC_UTILIZATION, new FloatDoublewordElement(33514)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_RESERVE_1, new FloatDoublewordElement(33516)), //
						m(GridconPCS.ChannelId.PCS_IPU_1_DC_DC_MEASUREMENTS_RESERVE_2, new FloatDoublewordElement(33518)) //
				), new FC3ReadRegistersTask(33520, Priority.LOW, //
						m(GridconPCS.ChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_VOLTAGE_STRING_A, new FloatDoublewordElement(33520)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_VOLTAGE_STRING_B, new FloatDoublewordElement(33522)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_VOLTAGE_STRING_C, new FloatDoublewordElement(33524)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_CURRENT_STRING_A, new FloatDoublewordElement(33526)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_CURRENT_STRING_B, new FloatDoublewordElement(33528)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_CURRENT_STRING_C, new FloatDoublewordElement(33530)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_POWER_STRING_A, new FloatDoublewordElement(33532)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_POWER_STRING_B, new FloatDoublewordElement(33534)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_POWER_STRING_C, new FloatDoublewordElement(33536)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_UTILIZATION_STRING_A, new FloatDoublewordElement(33538)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_UTILIZATION_STRING_B, new FloatDoublewordElement(33540)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_UTILIZATION_STRING_C, new FloatDoublewordElement(33542)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_ACCUMULATED_SUM_DC_CURRENT, new FloatDoublewordElement(33544)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_ACCUMULATED_DC_UTILIZATION, new FloatDoublewordElement(33546)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_RESERVE_1, new FloatDoublewordElement(33548)), //
						m(GridconPCS.ChannelId.PCS_IPU_2_DC_DC_MEASUREMENTS_RESERVE_2, new FloatDoublewordElement(33550)) //
				), new FC3ReadRegistersTask(33552, Priority.LOW, //
						m(GridconPCS.ChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_VOLTAGE_STRING_A, new FloatDoublewordElement(33552)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_VOLTAGE_STRING_B, new FloatDoublewordElement(33554)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_VOLTAGE_STRING_C, new FloatDoublewordElement(33556)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_CURRENT_STRING_A, new FloatDoublewordElement(33558)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_CURRENT_STRING_B, new FloatDoublewordElement(33560)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_CURRENT_STRING_C, new FloatDoublewordElement(33562)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_POWER_STRING_A, new FloatDoublewordElement(33564)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_POWER_STRING_B, new FloatDoublewordElement(33566)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_POWER_STRING_C, new FloatDoublewordElement(33568)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_UTILIZATION_STRING_A, new FloatDoublewordElement(33570)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_UTILIZATION_STRING_B, new FloatDoublewordElement(33572)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_UTILIZATION_STRING_C, new FloatDoublewordElement(33574)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_ACCUMULATED_SUM_DC_CURRENT, new FloatDoublewordElement(33576)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_ACCUMULATED_DC_UTILIZATION, new FloatDoublewordElement(33578)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_RESERVE_1, new FloatDoublewordElement(33580)), //
						m(GridconPCS.ChannelId.PCS_IPU_3_DC_DC_MEASUREMENTS_RESERVE_2, new FloatDoublewordElement(33582)) //
				), new FC3ReadRegistersTask(33584, Priority.LOW, //
						m(GridconPCS.ChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_VOLTAGE_STRING_A, new FloatDoublewordElement(33584)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_VOLTAGE_STRING_B, new FloatDoublewordElement(33586)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_VOLTAGE_STRING_C, new FloatDoublewordElement(33588)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_CURRENT_STRING_A, new FloatDoublewordElement(33590)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_CURRENT_STRING_B, new FloatDoublewordElement(33592)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_CURRENT_STRING_C, new FloatDoublewordElement(33594)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_POWER_STRING_A, new FloatDoublewordElement(33596)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_POWER_STRING_B, new FloatDoublewordElement(33598)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_POWER_STRING_C, new FloatDoublewordElement(33600)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_UTILIZATION_STRING_A, new FloatDoublewordElement(33602)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_UTILIZATION_STRING_B, new FloatDoublewordElement(33604)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_UTILIZATION_STRING_C, new FloatDoublewordElement(33606)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_ACCUMULATED_SUM_DC_CURRENT, new FloatDoublewordElement(33608)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_ACCUMULATED_DC_UTILIZATION, new FloatDoublewordElement(33610)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_RESERVE_1, new FloatDoublewordElement(33612)), //
						m(GridconPCS.ChannelId.PCS_IPU_4_DC_DC_MEASUREMENTS_RESERVE_2, new FloatDoublewordElement(33614)) //
				), new FC16WriteRegistersTask(32560, //
						m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_WORD, new UnsignedDoublewordElement(32560).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_ERROR_CODE_FALLBACK, new UnsignedDoublewordElement(32562).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_PARAMETER_U0, new FloatDoublewordElement(32564).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_PARAMETER_F0, new FloatDoublewordElement(32566).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_PARAMETER_Q_REF, new FloatDoublewordElement(32568).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_PARAMETER_P_REF, new FloatDoublewordElement(32570).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_TIME_SYNC_DATE, new UnsignedDoublewordElement(32572).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_TIME_SYNC_TIME, new UnsignedDoublewordElement(32574).wordOrder(WordOrder.LSWMSW)) //
				), new FC3ReadRegistersTask(32560, Priority.LOW, //
						m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_WORD, new UnsignedDoublewordElement(32560).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_ERROR_CODE_FALLBACK, new UnsignedDoublewordElement(32562).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_PARAMETER_U0, new FloatDoublewordElement(32564).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_PARAMETER_F0, new FloatDoublewordElement(32566).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_PARAMETER_Q_REF, new FloatDoublewordElement(32568).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_PARAMETER_P_REF, new FloatDoublewordElement(32570).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_TIME_SYNC_DATE, new UnsignedDoublewordElement(32572).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_TIME_SYNC_TIME, new UnsignedDoublewordElement(32574).wordOrder(WordOrder.LSWMSW)) //
				), new FC16WriteRegistersTask(32592, //
						m(GridconPCS.ChannelId.PCS_CONTROL_PARAMETER_U_Q_DROOP_MAIN, new FloatDoublewordElement(32592)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN, new FloatDoublewordElement(32594)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_PARAMETER_F_P_DRROP_MAIN, new FloatDoublewordElement(32596)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_PARAMETER_F_P_DROOP_T1_MAIN, new FloatDoublewordElement(32598)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_PARAMETER_Q_U_DROOP_MAIN, new FloatDoublewordElement(32600)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_PARAMETER_Q_U_DEAD_BAND, new FloatDoublewordElement(32602)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_PARAMETER_Q_LIMIT, new FloatDoublewordElement(32604)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_PARAMETER_P_F_DROOP_MAIN, new FloatDoublewordElement(32606)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_PARAMETER_P_F_DEAD_BAND, new FloatDoublewordElement(32608)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_PARAMETER_P_U_DROOP, new FloatDoublewordElement(32610)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_PARAMETER_P_U_DEAD_BAND, new FloatDoublewordElement(32612)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_PARAMETER_P_U_MAX_CHARGE, new FloatDoublewordElement(32614)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_PARAMETER_P_U_MAX_DISCHARGE, new FloatDoublewordElement(32616)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_PARAMETER_P_CONTROL_MODE, new FloatDoublewordElement(32618)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_PARAMETER_P_CONTROL_LIM_TWO, new FloatDoublewordElement(32620)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_PARAMETER_P_CONTROL_LIM_ONE, new FloatDoublewordElement(32622)) //
				), new FC16WriteRegistersTask(32624, //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_1_PARAMETERS_DC_VOLTAGE_SETPOINT, new FloatDoublewordElement(32624).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_1_PARAMETERS_DC_CURRENT_SETPOINT, new FloatDoublewordElement(32626).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_1_PARAMETERS_U0_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32628).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_1_PARAMETERS_F0_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32630).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_1_PARAMETERS_Q_REF_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32632).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_1_PARAMETERS_P_REF_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32634).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_1_PARAMETERS_P_MAX_DISCHARGE, new FloatDoublewordElement(32636).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_1_PARAMETERS_P_MAX_CHARGE, new FloatDoublewordElement(32638).wordOrder(WordOrder.LSWMSW)) //
				), new FC16WriteRegistersTask(32656, //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_2_PARAMETERS_DC_VOLTAGE_SETPOINT, new FloatDoublewordElement(32656).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_2_PARAMETERS_DC_CURRENT_SETPOINT, new FloatDoublewordElement(32658).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_2_PARAMETERS_U0_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32660).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_2_PARAMETERS_F0_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32662).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_2_PARAMETERS_Q_REF_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32664).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_2_PARAMETERS_P_REF_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32666).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_2_PARAMETERS_P_MAX_DISCHARGE, new FloatDoublewordElement(32668).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_2_PARAMETERS_P_MAX_CHARGE, new FloatDoublewordElement(32670).wordOrder(WordOrder.LSWMSW)) //
				), new FC16WriteRegistersTask(32688, //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_3_PARAMETERS_DC_VOLTAGE_SETPOINT, new FloatDoublewordElement(32688).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_3_PARAMETERS_DC_CURRENT_SETPOINT, new FloatDoublewordElement(32690).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_3_PARAMETERS_U0_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32692).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_3_PARAMETERS_F0_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32694).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_3_PARAMETERS_Q_REF_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32696).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_3_PARAMETERS_P_REF_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32698).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_3_PARAMETERS_P_MAX_DISCHARGE, new FloatDoublewordElement(32700).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_3_PARAMETERS_P_MAX_CHARGE, new FloatDoublewordElement(32702).wordOrder(WordOrder.LSWMSW)) //
				),
				new FC16WriteRegistersTask(32720,
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_DC_VOLTAGE_SETPOINT, new FloatDoublewordElement(32720).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_A, new FloatDoublewordElement(32722).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_B, new FloatDoublewordElement(32724).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_C, new FloatDoublewordElement(32726).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_I_REF_STRING_A, new FloatDoublewordElement(32728).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_I_REF_STRING_B, new FloatDoublewordElement(32730).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_I_REF_STRING_C, new FloatDoublewordElement(32732).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_DC_DC_STRING_CONTROL_MODE, new FloatDoublewordElement(32734).wordOrder(WordOrder.LSWMSW)) //
				));
	}
}
