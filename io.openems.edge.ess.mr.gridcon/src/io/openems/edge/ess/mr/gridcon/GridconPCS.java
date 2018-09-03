package io.openems.edge.ess.mr.gridcon;

import java.nio.ByteOrder;
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
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.OptionsEnum;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

import io.openems.edge.ess.power.api.CircleConstraint;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.MR.Gridcon", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
) //
public class GridconPCS extends AbstractOpenemsModbusComponent
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(GridconPCS.class);

	protected static final float MAX_POWER_KW = 125 * 1000;
	
	enum PCSControlWordBits {
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
		DISABLE_IPU_4(28),
		DISABLE_IPU_3(29),
		DISABLE_IPU_2(30),
		DISABLE_IPU_1(31),
		;
		
		PCSControlWordBits(int value) {
			this.bitPosition = value;
		}
		
		private int bitPosition;

		public int getBitPosition() {
			return bitPosition;
		}

		public int getBitMask() {
			return (int) Math.pow(2, bitPosition);
		}
	}
	
	static final int MAX_APPARENT_POWER = (int) MAX_POWER_KW; // TODO Checkif correct
	private CircleConstraint maxApparentPowerConstraint = null;

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
		
		
		super.activate(context, config.service_pid(), config.id(), config.enabled(), config.unit_id(), this.cm, "Modbus",
				config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}
	
	private void handleStateMachine() {
		// TODO
		// see Software manual chapter 5.1
		
		writeDateAndTime();
		if (isIdle()) {
			startSystem();
		} else if (isError()) {
			doErrorHandling();
		}
	}

	


	private void writeDateAndTime() {
		LocalDateTime time = LocalDateTime.now();
		byte dayOfWeek = (byte) time.getDayOfWeek().ordinal();
		byte day = (byte) time.getDayOfMonth();
		byte month = (byte) time.getMonth().getValue();
		byte year = (byte) (time.getYear() - 2000);

		Integer dateInteger = convertToInteger(BitSet.valueOf( new byte[]{ day, dayOfWeek, year, month }));
		
		
		byte seconds = (byte) time.getSecond();
		byte minutes = (byte) time.getMinute();
		byte hours = (byte) time.getHour();

		Integer timeInteger = convertToInteger(BitSet.valueOf( new byte[]{ seconds, 0, hours, minutes }));
		
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

	private boolean isIdle() {
		BooleanReadChannel stateIdleChannel = this.channel(ChannelId.PCS_CCU_STATE_IDLE);
		Optional<Boolean> valueOpt = stateIdleChannel.getNextValue().asOptional();
		return valueOpt.isPresent() && valueOpt.get();
	}

	private void startSystem() {
		// TODO
		log.info("Try to start system");
		/* 
		 * Coming from state idle
		 * first write 800V to IPU4 voltage setpoint, set "73" to DCDC String Control Mode of IPU4
		 * and "1" to  Weight String A, B, C ==> i.e. all 3 IPUs are weighted equally
		 * write -86000 to Pmax discharge Iref String A, B, C, write 86000 to Pmax Charge DCDC Str Mode of IPU 1, 2, 3
		 * set P Control mode to "Act Pow Ctrl" (hex 4000 = mode power limiter, 0 = disabled, hex 3F80 = active power control) 
		 *  and Mode Sel to "Current Control" s--> ee pic start0.png in doc folder
		 * 
		 * enable "Sync Approval" and "Ena IPU 4" and PLAY command -> system should change state to "RUN"
		 *  --> see pic start1.png
		 *  
		 * after that enable IPU 1, 2, 3, if they have reached state "RUN" (=14) 
		 * power can be set (from 0..1 (1 = max system power = 125 kW) , i.e. 0,05 is equal to  6.250 W
		 * same for reactive power
		 * see pic start2.png
		 * 
		 * "Normal mode" is reached now  
		 */
		
		
		//int controlWordMask = 0xFFFFFFFF;
		
		//  enable "Sync Approval" and "Ena IPU 4, 3, 2, 1" and PLAY command -> system should change state to "RUN" 
		BitSet bitSet = new BitSet(32);
		bitSet.set( PCSControlWordBits.PLAY.bitPosition, true);
		bitSet.set( PCSControlWordBits.SYNC_APPROVAL.bitPosition, true);
		bitSet.set( PCSControlWordBits.MODE_SELECTION.bitPosition, true);
		
		bitSet.set( PCSControlWordBits.DISABLE_IPU_1.bitPosition, false);
		bitSet.set( PCSControlWordBits.DISABLE_IPU_2.bitPosition, false);
		bitSet.set( PCSControlWordBits.DISABLE_IPU_3.bitPosition, false);
		bitSet.set( PCSControlWordBits.DISABLE_IPU_4.bitPosition, false);
		
		IntegerWriteChannel controlWordChannel = this.channel(ChannelId.PCS_COMMAND_CONTROL_WORD);
		try {
			Integer value = convertToInteger(bitSet);
			controlWordChannel.setNextWriteValue(value);
		} catch (OpenemsException e) {
			e.printStackTrace();
		}		

		writeIPUParameters();	
	}
	
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
			
			float PmaxCharge = 86000;
			float PmaxDischarge = -86000;
			
			((FloatWriteChannel) this.channel(ChannelId.PCS_CONTROL_IPU_1_PARAMETERS_P_MAX_DISCHARGE)).setNextWriteValue(PmaxDischarge);
			((FloatWriteChannel) this.channel(ChannelId.PCS_CONTROL_IPU_2_PARAMETERS_P_MAX_DISCHARGE)).setNextWriteValue(PmaxDischarge);
			((FloatWriteChannel) this.channel(ChannelId.PCS_CONTROL_IPU_3_PARAMETERS_P_MAX_DISCHARGE)).setNextWriteValue(PmaxDischarge);
			
			((FloatWriteChannel) this.channel(ChannelId.PCS_CONTROL_IPU_1_PARAMETERS_P_MAX_CHARGE)).setNextWriteValue(PmaxCharge);
			((FloatWriteChannel) this.channel(ChannelId.PCS_CONTROL_IPU_2_PARAMETERS_P_MAX_CHARGE)).setNextWriteValue(PmaxCharge);
			((FloatWriteChannel) this.channel(ChannelId.PCS_CONTROL_IPU_3_PARAMETERS_P_MAX_CHARGE)).setNextWriteValue(PmaxCharge);
			
			((IntegerWriteChannel) this.channel(ChannelId.PCS_CONTROL_PARAMETER_P_CONTROL_MODE)).setNextWriteValue(0x3F80);
			
			
			
			
		} catch (OpenemsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}


	private boolean isDCDCConverterStarted() {
		// TODO not completely correct, should work, bur for correct working the Status MCU should be read, status should be 14 = RUN
		BooleanReadChannel stateChannel = this.channel(ChannelId.PCS_CCU_STATE_RUN);
		Optional<Boolean> valueOpt = stateChannel.getNextValue().asOptional();
		return valueOpt.isPresent() && valueOpt.get();
	}

	private void doErrorHandling() {
		// TODO		
		// try to find out what kind of error it is, 
		// disable IPUs, stopping system, then acknowledge errors, wait some seconds
		// if no errors are shown, then try to start system
//		stopSystem();
//		acknowledgeErrors();
	}
	
	@Override
	public String debugLog() {
		return "Current state: " + this.channel(ChannelId.SYSTEM_CURRENT_STATE).value().asOptionString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {		
		FloatWriteChannel channelPRef = this.channel(ChannelId.PCS_COMMAND_CONTROL_PARAMETER_P_REF);
		FloatWriteChannel channelQRef = this.channel(ChannelId.PCS_COMMAND_CONTROL_PARAMETER_Q_REF);
		
		// !! signum		
		float activePowerFactor = - activePower / MAX_POWER_KW;
		float reactivePowerFactor = - reactivePower / MAX_POWER_KW;
		try {
			channelPRef.setNextWriteValue(activePowerFactor);
			channelQRef.setNextWriteValue(reactivePowerFactor);
		} catch (OpenemsException e) {
			log.error("problem occurred while trying to set active/reactive power" + e.getMessage());
		}		
	}

	@Override
	public int getPowerPrecision() {
		return (int) (MAX_POWER_KW * 0.01);
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
	public enum CurrentState implements OptionsEnum {  // see Software manual chapter 5.1
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
		DISABLED(1, "Disabled"), //TODO Check values!!!
		ACTIVE_POWER_CONTROL(2, "Active Power Control Mode"),
		POWER_LIMITER(4, "Power Limiter Mode");
		
		int value;
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
	
	public enum Command implements OptionsEnum {  // see manual(Betriebsanleitung Feldbus Konfiguration (Anybus-Modul)) page 15
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
		SYSTEM_CURRENT_STATE(new Doc().options(CurrentState.values())), //
		SYSTEM_CURRENT_PARAMETER_SET(new Doc()), //
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
		
		ACF_VOLTAGE_RMS_L12(new Doc().unit(Unit.VOLT)),
		ACF_VOLTAGE_RMS_L23(new Doc().unit(Unit.VOLT)),
		ACF_VOLTAGE_RMS_L31(new Doc().unit(Unit.VOLT)),
		ACF_RELATIVE_THD_FACTOR(new Doc().unit(Unit.PERCENT)),
		ACF_FREQUENCY(new Doc().unit(Unit.HERTZ)),
		ACF_CURRENT_RMS_L1(new Doc().unit(Unit.AMPERE)),
		ACF_CURRENT_RMS_L2(new Doc().unit(Unit.AMPERE)),
		ACF_CURRENT_RMS_L3(new Doc().unit(Unit.AMPERE)),
		ACF_ABSOLUTE_THD_FACTOR(new Doc().unit(Unit.AMPERE)),
		ACF_DISTORSION_POWER(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)),
		
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
//		CCU_STATE(new Doc() //TODO so gehts nicht, es können mehrere Werte gesetzt sein 
//				.option(1, "IDLE") //
//				.option(2, "Pre-Charge") //
//				.option(4, "Stop Pre-Charge") //
//				.option(8, "READY") //
//				.option(16, "PAUSE") // ,
//				.option(32, "RUN") //
//				.option(64, "Error") //
//				.option(128, "Voltage ramping up") //
//				.option(256, "Overload") //
//				.option(512, "Short circuit detected") //
//				.option(1024, "Derating power") //
//				.option(2048, "Derating harmonics") //
//				.option(4096, "SIA active")), //
		CCU_ERROR_CODE(new Doc().unit(Unit.NONE)),
		CCU_VOLTAGE_U12(new Doc().unit(Unit.VOLT)),
		CCU_VOLTAGE_U23(new Doc().unit(Unit.VOLT)),
		CCU_VOLTAGE_U31(new Doc().unit(Unit.VOLT)),
		CCU_CURRENT_IL1(new Doc().unit(Unit.AMPERE)),
		CCU_CURRENT_IL2(new Doc().unit(Unit.AMPERE)),
		CCU_CURRENT_IL3(new Doc().unit(Unit.AMPERE)),
		CCU_POWER_P(new Doc().unit(Unit.WATT)),
		CCU_POWER_Q(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)),
		CCU_FREQUENCY(new Doc().unit(Unit.HERTZ)),
		PCS_COMMAND_CONTROL_WORD(new Doc().unit(Unit.NONE)),
		PCS_COMMAND_CONTROL_WORD_PLAY(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_READY(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ACKNOWLEDGE(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_STOP(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_BLACKSTART_APPROVAL(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_SYNC_APPROVAL(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ACTIVATE_SHORT_CIRCUIT_HANDLING(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_MODE_SELECTION(new Doc().unit(Unit.ON_OFF)), //0=voltage control, 1=current control
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
		PCS_CONTROL_IPU_1_PARAMETERS_P_MAX_CHARGE(new Doc().unit(Unit.WATT)),
		;

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
						m(GridconPCS.ChannelId.SYSTEM_CURRENT_STATE, new UnsignedWordElement(528)), //
						m(GridconPCS.ChannelId.SYSTEM_CURRENT_PARAMETER_SET, new UnsignedWordElement(529)), // TODO parameter set and utilization are separated in half bytes, how to handle?
						bm(new UnsignedWordElement(530)) //
						.m(GridconPCS.ChannelId.SYSTEM_SERVICE_MODE, 1) //
						.m(GridconPCS.ChannelId.SYSTEM_REMOTE_MODE, 2) //
						.m(GridconPCS.ChannelId.SYSTEM_MEASUREMENTS_LIFEBIT, 6) //
						.m(GridconPCS.ChannelId.SYSTEM_CCU_LIFEBIT, 7) //						
						.build(), //
						m(GridconPCS.ChannelId.SYSTEM_NUMBER_ERROR_WARNINGS, new UnsignedWordElement(531)) //
				)
				, new FC16WriteRegistersTask(560,
						m(GridconPCS.ChannelId.SYSTEM_COMMAND, new UnsignedWordElement(560)), //
						m(GridconPCS.ChannelId.SYSTEM_PARAMETER_SET, new UnsignedWordElement(561)), //
						bm(new UnsignedWordElement(562)) //
						.m(GridconPCS.ChannelId.SYSTEM_FIELDBUS_DEVICE_LIFEBIT, 7) //						
						.build() //
				)
				, new FC3ReadRegistersTask(592, Priority.LOW, //)
						m(GridconPCS.ChannelId.SYSTEM_ERROR_CODE, new UnsignedDoublewordElement(592)) //
				)
				, new FC16WriteRegistersTask(624,
						m(GridconPCS.ChannelId.SYSTEM_ERROR_ACKNOWLEDGE, new UnsignedDoublewordElement(624))
				)
				, new FC3ReadRegistersTask(1808, Priority.LOW, //)
						m(GridconPCS.ChannelId.ACF_VOLTAGE_RMS_L12, new FloatDoublewordElement(1808)), //
						m(GridconPCS.ChannelId.ACF_VOLTAGE_RMS_L23, new FloatDoublewordElement(1810)), //
						m(GridconPCS.ChannelId.ACF_VOLTAGE_RMS_L31, new FloatDoublewordElement(1812)), //
						m(GridconPCS.ChannelId.ACF_RELATIVE_THD_FACTOR, new FloatDoublewordElement(1814)), //
						m(GridconPCS.ChannelId.ACF_FREQUENCY, new FloatDoublewordElement(1816)) //
				)
				, new FC3ReadRegistersTask(1904, Priority.LOW, //)
						m(GridconPCS.ChannelId.ACF_CURRENT_RMS_L1, new FloatDoublewordElement(1904)), //
						m(GridconPCS.ChannelId.ACF_CURRENT_RMS_L2, new FloatDoublewordElement(1906)), //
						m(GridconPCS.ChannelId.ACF_CURRENT_RMS_L3, new FloatDoublewordElement(1908)), //
						m(GridconPCS.ChannelId.ACF_ABSOLUTE_THD_FACTOR, new FloatDoublewordElement(1910)) //
				)
				, new FC3ReadRegistersTask(2064, Priority.LOW, //)
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(2064)), //
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(2066)), //
						m(GridconPCS.ChannelId.ACF_DISTORSION_POWER, new FloatDoublewordElement(2068)) //
				)	
				, new FC3ReadRegistersTask(32528, Priority.LOW, //)						
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
						m(GridconPCS.ChannelId.CCU_ERROR_CODE, new UnsignedDoublewordElement(32530)), //
						m(GridconPCS.ChannelId.CCU_VOLTAGE_U12, new FloatDoublewordElement(32532)), //
						m(GridconPCS.ChannelId.CCU_VOLTAGE_U23, new FloatDoublewordElement(32534)), //
						m(GridconPCS.ChannelId.CCU_VOLTAGE_U31, new FloatDoublewordElement(32536)), //
						m(GridconPCS.ChannelId.CCU_CURRENT_IL1, new FloatDoublewordElement(32538)), //
						m(GridconPCS.ChannelId.CCU_CURRENT_IL2, new FloatDoublewordElement(32540)), //
						m(GridconPCS.ChannelId.CCU_CURRENT_IL3, new FloatDoublewordElement(32542)), //
						m(GridconPCS.ChannelId.CCU_POWER_P, new FloatDoublewordElement(32544)), //
						m(GridconPCS.ChannelId.CCU_POWER_Q, new FloatDoublewordElement(32546)), //
						m(GridconPCS.ChannelId.CCU_FREQUENCY, new FloatDoublewordElement(32548)) //
				)	
				, new FC16WriteRegistersTask(32560,
						m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_WORD, new UnsignedDoublewordElement(32560).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_ERROR_CODE_FALLBACK, new UnsignedDoublewordElement(32562).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_PARAMETER_U0, new FloatDoublewordElement(32564).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_PARAMETER_F0, new FloatDoublewordElement(32566).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_PARAMETER_Q_REF, new FloatDoublewordElement(32568).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_PARAMETER_P_REF, new FloatDoublewordElement(32570).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_TIME_SYNC_DATE, new UnsignedDoublewordElement(32572).wordOrder(WordOrder.LSWMSW)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_TIME_SYNC_TIME, new UnsignedDoublewordElement(32574).wordOrder(WordOrder.LSWMSW)) //
				)
				, new FC16WriteRegistersTask(32592,
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
				)
				, new FC16WriteRegistersTask(32624,						
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_1_PARAMETERS_DC_VOLTAGE_SETPOINT, new FloatDoublewordElement(32624)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_1_PARAMETERS_DC_CURRENT_SETPOINT, new FloatDoublewordElement(32626)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_1_PARAMETERS_U0_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32628)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_1_PARAMETERS_F0_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32630)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_1_PARAMETERS_Q_REF_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32632)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_1_PARAMETERS_P_REF_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32634)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_1_PARAMETERS_P_MAX_DISCHARGE, new FloatDoublewordElement(32636)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_1_PARAMETERS_P_MAX_CHARGE, new FloatDoublewordElement(32638)) //
				)
				, new FC16WriteRegistersTask(32656,						
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_2_PARAMETERS_DC_VOLTAGE_SETPOINT, new FloatDoublewordElement(32656)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_2_PARAMETERS_DC_CURRENT_SETPOINT, new FloatDoublewordElement(32658)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_2_PARAMETERS_U0_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32660)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_2_PARAMETERS_F0_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32662)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_2_PARAMETERS_Q_REF_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32664)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_2_PARAMETERS_P_REF_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32666)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_2_PARAMETERS_P_MAX_DISCHARGE, new FloatDoublewordElement(32668)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_2_PARAMETERS_P_MAX_CHARGE, new FloatDoublewordElement(32670)) //
				)
				, new FC16WriteRegistersTask(32688,						
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_3_PARAMETERS_DC_VOLTAGE_SETPOINT, new FloatDoublewordElement(32688)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_3_PARAMETERS_DC_CURRENT_SETPOINT, new FloatDoublewordElement(32690)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_3_PARAMETERS_U0_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32692)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_3_PARAMETERS_F0_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32694)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_3_PARAMETERS_Q_REF_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32696)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_3_PARAMETERS_P_REF_OFFSET_TO_CCU_VALUE, new FloatDoublewordElement(32698)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_3_PARAMETERS_P_MAX_DISCHARGE, new FloatDoublewordElement(32700)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_3_PARAMETERS_P_MAX_CHARGE, new FloatDoublewordElement(32702)) //
				)
				, new FC16WriteRegistersTask(32720,
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_DC_VOLTAGE_SETPOINT, new FloatDoublewordElement(32720)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_A, new FloatDoublewordElement(32722)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_B, new FloatDoublewordElement(32724)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_C, new FloatDoublewordElement(32726)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_I_REF_STRING_A, new FloatDoublewordElement(32728)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_I_REF_STRING_B, new FloatDoublewordElement(32730)), //
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_I_REF_STRING_C, new FloatDoublewordElement(32732)), //	
						m(GridconPCS.ChannelId.PCS_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_DC_DC_STRING_CONTROL_MODE, new FloatDoublewordElement(32734)) //
				)
				
			);
	}
}
