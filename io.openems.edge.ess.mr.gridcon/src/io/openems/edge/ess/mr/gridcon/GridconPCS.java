package io.openems.edge.ess.mr.gridcon;

import java.time.LocalDateTime;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

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
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.mr.gridcon.enums.CCUState;
import io.openems.edge.ess.mr.gridcon.enums.ErrorCode;
import io.openems.edge.ess.mr.gridcon.enums.GridConChannelId;
import io.openems.edge.ess.mr.gridcon.enums.PCSControlWordBitPosition;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.meter.api.SymmetricMeter;

/**
 * This class handles the communication between ems and a gridcon.
 */
@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.MR.Gridcon", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
)
public class GridconPCS extends AbstractOpenemsModbusComponent
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(GridconPCS.class);

	protected static final float MAX_POWER_W = 125 * 1000;
	protected static final float MAX_CHARGE_W = 86 * 1000;
	protected static final float MAX_DISCHARGE_W = 86 * 1000;

	static final int MAX_APPARENT_POWER = (int) MAX_POWER_W; // TODO Checkif correct
	BitSet commandControlWord = new BitSet(32);
	LocalDateTime timestampMrGridconWasSwitchedOff;

	@Reference
	private Power power;

	@Reference
	protected ConfigurationAdmin cm;

	ChannelAddress inputNAProtection1 = null;
	ChannelAddress inputNAProtection2 = null;
	ChannelAddress inputSyncDeviceBridge = null;
	ChannelAddress outputSyncDeviceBridge = null;
	ChannelAddress outputMRHardReset = null;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	Battery batteryStringA;
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	Battery batteryStringB;
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	Battery batteryStringC;
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	SymmetricMeter gridMeter;
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	DigitalInput inputNAProtection1Component;
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	DigitalInput inputNAProtection2Component;
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	DigitalInput inputSyncDeviceBridgeComponent;
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	DigitalOutput outputSyncDeviceBridgeComponent;
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	DigitalOutput outputMRHardResetComponent;
	
	int minSoCA;
	int minSoCB;
	int minSoCC;

	public GridconPCS() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private DigitalInput inputComponent = null;

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		// update filter for 'batteryStringA'
		if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "BatteryStringA", config.battery_string_A_id())) {
			return;
		}
		// update filter for 'batteryStringB'
		if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "BatteryStringB", config.battery_string_B_id())) {
			return;
		}
		// update filter for 'batteryStringC'
		if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "BatteryStringC", config.battery_string_C_id())) {
			return;
		}
		// update filter for 'Grid-Meter'
		if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "GridMeter", config.meter())) {
			return;
		}
		// update filter for 'inputNAProtection1'
		this.inputNAProtection1 = ChannelAddress.fromString(config.inputNAProtection1());
		if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "inputNAProtection1Component",
				this.inputNAProtection1.getComponentId())) {
			return;
		}
		// update filter for 'inputNAProtection2'
		this.inputNAProtection2 = ChannelAddress.fromString(config.inputNAProtection1());
		if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "inputNAProtection2Component",
				this.inputNAProtection2.getComponentId())) {
			return;
		}
		// update filter for 'inputSyncDeviceBridge'
		this.inputSyncDeviceBridge = ChannelAddress.fromString(config.inputSyncDeviceBridge());
		if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "inputSyncDeviceBridgeComponent",
				this.inputSyncDeviceBridge.getComponentId())) {
			return;
		}
		// update filter for 'outputSyncDeviceBridgeComponent'
		this.outputSyncDeviceBridge = ChannelAddress.fromString(config.outputSyncDeviceBridge());
		if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "outputSyncDeviceBridgeComponent",
				this.outputSyncDeviceBridge.getComponentId())) {
			return;
		}
		// update filter for 'outputMRHardReset'
		this.outputMRHardReset = ChannelAddress.fromString(config.outputMRHardReset());
		if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "outputMRHardResetComponent",
				this.outputMRHardReset.getComponentId())) {
			return;
		}
		
		minSoCA = config.minSoCA();
		minSoCB = config.minSoCB();
		minSoCC = config.minSoCC();
		

		WriteChannel<Integer> commandControlWordChannel = this.channel(GridConChannelId.COMMAND_CONTROL_WORD);
		commandControlWordChannel.onSetNextWrite(value -> {
			if (value != null) {
					Integer ctrlWord = value;
					mapBitToChannel(ctrlWord, PCSControlWordBitPosition.PLAY, GridConChannelId.COMMAND_CONTROL_WORD_PLAY);
					mapBitToChannel(ctrlWord, PCSControlWordBitPosition.ACKNOWLEDGE, GridConChannelId.COMMAND_CONTROL_WORD_ACKNOWLEDGE);
					mapBitToChannel(ctrlWord, PCSControlWordBitPosition.STOP, GridConChannelId.COMMAND_CONTROL_WORD_STOP);
					mapBitToChannel(ctrlWord, PCSControlWordBitPosition.READY, GridConChannelId.COMMAND_CONTROL_WORD_READY);
			};
		});

		this.channel(GridConChannelId.MIRROR_COMMAND_CONTROL_WORD).onUpdate(value -> {
			if (value != null) {
				@SuppressWarnings("unchecked")
				Optional<Long> ctrlWordOpt = (Optional<Long>) value.asOptional();
				if (ctrlWordOpt.isPresent()) {
					Long ctrlWord = ctrlWordOpt.get();
					mapBitToChannel(ctrlWord, PCSControlWordBitPosition.PLAY, GridConChannelId.MIRROR_COMMAND_CONTROL_WORD_PLAY);
					mapBitToChannel(ctrlWord, PCSControlWordBitPosition.ACKNOWLEDGE, GridConChannelId.MIRROR_COMMAND_CONTROL_WORD_ACKNOWLEDGE);
					mapBitToChannel(ctrlWord, PCSControlWordBitPosition.STOP, GridConChannelId.MIRROR_COMMAND_CONTROL_WORD_STOP);
					mapBitToChannel(ctrlWord, PCSControlWordBitPosition.READY, GridConChannelId.MIRROR_COMMAND_CONTROL_WORD_READY);
				}
			};
		});

		/*
		 * Initialize Power
		 */
//		int max = 5000;
//		int min = -5000;
//		
//		this.getPower().addSimpleConstraint(this, ConstraintType.STATIC, Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, max);
//		this.getPower().addSimpleConstraint(this, ConstraintType.STATIC, Phase.ALL, Pwr.ACTIVE, Relationship.GREATER_OR_EQUALS, min);
//		
		// Max Apparent
		// TODO adjust apparent power from modbus element
//		this.maxApparentPowerConstraint = new CircleConstraint(this, MAX_APPARENT_POWER);

		super.activate(context, config.service_pid(), config.id(), config.enabled(), config.unit_id(), this.cm,
				"Modbus", config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private GridMode getOnOffGrid() {
		BooleanReadChannel inputNAProtection1 = this.inputNAProtection1Component
				.channel(this.inputNAProtection1.getChannelId());
		BooleanReadChannel inputNAProtection2 = this.inputNAProtection1Component
				.channel(this.inputNAProtection1.getChannelId());

		Optional<Boolean> isInputNAProtection1 = inputNAProtection1.value().asOptional();
		Optional<Boolean> isInputNAProtection2 = inputNAProtection2.value().asOptional();

		SymmetricEss.GridMode gridMode;
		if (!isInputNAProtection1.isPresent() || !isInputNAProtection2.isPresent()) {
			gridMode = SymmetricEss.GridMode.UNDEFINED;
		} else {
			if (isInputNAProtection1.get() && isInputNAProtection2.get()) {
				gridMode = SymmetricEss.GridMode.ON_GRID;
			} else {
				gridMode = SymmetricEss.GridMode.OFF_GRID;
			}
		}
		this.getGridMode().setNextValue(gridMode);
		return gridMode;
	}

	private void handleStateMachine() {
		this.prepareGeneralCommands();

		GridMode gridMode = this.getOnOffGrid();
		switch (gridMode) {
		case ON_GRID:
			log.info("handleOnGridState");
			this.handleOnGridState();
			break;
		case OFF_GRID:
			log.info("handleOffGridState");
			this.handleOffGridState();
			break;
		case UNDEFINED:
			break;
		}

		this.writeGeneralCommands();
	}

	private void prepareGeneralCommands() {
		commandControlWord.set(PCSControlWordBitPosition.PLAY.getBitPosition(), false);
		commandControlWord.set(PCSControlWordBitPosition.READY.getBitPosition(), false);
		commandControlWord.set(PCSControlWordBitPosition.ACKNOWLEDGE.getBitPosition(), false);
		commandControlWord.set(PCSControlWordBitPosition.STOP.getBitPosition(), false);

		commandControlWord.set(PCSControlWordBitPosition.SYNC_APPROVAL.getBitPosition(), true);
		commandControlWord.set(PCSControlWordBitPosition.MODE_SELECTION.getBitPosition(), true);
		commandControlWord.set(PCSControlWordBitPosition.ACTIVATE_SHORT_CIRCUIT_HANDLING.getBitPosition(), true);

		commandControlWord.set(PCSControlWordBitPosition.DISABLE_IPU_1.getBitPosition(), true);
		commandControlWord.set(PCSControlWordBitPosition.DISABLE_IPU_2.getBitPosition(), true);
		commandControlWord.set(PCSControlWordBitPosition.DISABLE_IPU_3.getBitPosition(), true);
		commandControlWord.set(PCSControlWordBitPosition.DISABLE_IPU_4.getBitPosition(), false);

		writeValueToChannel(GridConChannelId.COMMAND_ERROR_CODE_FEEDBACK, 0);
		writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_Q_REF, 0);
		writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_P_REF, 0);
		/**
		 * Always write values for frequency and voltage to gridcon, because in case of
		 * blackstart mode if we write '0' to gridcon the systems tries to regulate
		 * frequency and voltage to zero which would be bad for Mr. Gridcon's health
		 */
		writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_U0, 1.0f);
		writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_F0, 1.035f);
		writeDateAndTime();

		writeCCUControlParameters(PControlMode.ACTIVE_POWER_CONTROL);
		writeIPUParameters(1f, 1f, 1f, MAX_DISCHARGE_W, MAX_DISCHARGE_W, MAX_DISCHARGE_W, MAX_CHARGE_W, MAX_CHARGE_W,
				MAX_CHARGE_W);

//		//TODO still necessary?
		((ManagedSymmetricEss) this).getAllowedCharge().setNextValue(-MAX_APPARENT_POWER);
		((ManagedSymmetricEss) this).getAllowedDischarge().setNextValue(MAX_APPARENT_POWER);
	}

	private void writeGeneralCommands() {
		Integer value = convertToInteger(commandControlWord);
		writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD, value);
	}

	private void handleOnGridState() {
		// Always set OutputSyncDeviceBridge OFF in On-Grid state
		this.setOutputSyncDeviceBridge(false);

		// a hardware restart has been executed, 
		if (timestampMrGridconWasSwitchedOff != null) {
			log.info("timestampMrGridconWasSwitchedOff is set: " + timestampMrGridconWasSwitchedOff.toString());
			if (LocalDateTime.now().isAfter(timestampMrGridconWasSwitchedOff.plusSeconds(15))) {
				try {
					log.info("try to write to channel hardware reset, set it to 'false'");
					// after 15 seconds switch Mr. Gridcon on again!
					BooleanWriteChannel channelHardReset = outputMRHardResetComponent.channel(outputMRHardReset.getChannelId());
					channelHardReset.setNextWriteValue(false);					
				} catch (OpenemsException e) {
					log.error("Problem occurred while deactivating hardware switch!");
					e.printStackTrace();
				}
				timestampMrGridconWasSwitchedOff = null;
			}
			return;
		}
		
		switch (getCurrentState()) {
		case DERATING_HARMONICS:
			break;
		case DERATING_POWER:
			break;
		case ERROR:
			doErrorHandling();
			break;
		case IDLE:
			startSystem();
			break;
		case OVERLOAD:
			break;
		case PAUSE:
			break;
		case PRECHARGE:
			break;
		case READY:
			break;
		case RUN:
			doRunHandling();
			break;
		case SHORT_CIRCUIT_DETECTED:
			break;
		case SIA_ACTIVE:
			break;
		case STOP_PRECHARGE:
			break;
		case UNDEFINED:
			break;
		case VOLTAGE_RAMPING_UP:
			break;
		}
		
		resetErrorCodes();
	}

	private void resetErrorCodes() {
		IntegerReadChannel errorCodeChannel = this.channel(GridConChannelId.CCU_ERROR_CODE);
		Optional<Integer> errorCodeOpt = errorCodeChannel.value().asOptional();
		log.debug("in resetErrorCodes: => Errorcode: " + errorCodeOpt);
		if (errorCodeOpt.isPresent() && errorCodeOpt.get() != 0) {
			writeValueToChannel(GridConChannelId.COMMAND_ERROR_CODE_FEEDBACK, errorCodeOpt.get());
		}		
	}

	private void doRunHandling() {
		commandControlWord.set(PCSControlWordBitPosition.DISABLE_IPU_1.getBitPosition(), false);
		commandControlWord.set(PCSControlWordBitPosition.DISABLE_IPU_2.getBitPosition(), false);
		commandControlWord.set(PCSControlWordBitPosition.DISABLE_IPU_3.getBitPosition(), false);
		commandControlWord.set(PCSControlWordBitPosition.DISABLE_IPU_4.getBitPosition(), false);
	}

	private void writeCCUControlParameters(PControlMode mode) {

		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_MAIN, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_F_P_DRROP_MAIN, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_T1_MAIN, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_Q_U_DROOP_MAIN, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_Q_U_DEAD_BAND, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_Q_LIMIT, 1f); // 0 -> limits Q to zero, 1 -> to max Q
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_F_DROOP_MAIN, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_F_DEAD_BAND, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_U_DROOP, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_U_DEAD_BAND, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_U_MAX_CHARGE, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_U_MAX_DISCHARGE, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_TWO, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_ONE, 0f);
		// the only relevant parameter is 'P Control Mode' which should be set to
		// 'Active power control' in case of on grid usage
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_MODE, mode.getFloatValue()); //
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

		writeValueToChannel(GridConChannelId.COMMAND_TIME_SYNC_DATE, dateInteger);
		writeValueToChannel(GridConChannelId.COMMAND_TIME_SYNC_TIME, timeInteger);

	}

	/**
	 * This turns on the system by enabling ALL IPUs.
	 */
	private void startSystem() {
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

		// enable "Sync Approval" and "Ena IPU 4, 3, 2, 1" and PLAY command -> system
		// should change state to "RUN"
		commandControlWord.set(PCSControlWordBitPosition.PLAY.getBitPosition(), true);

		commandControlWord.set(PCSControlWordBitPosition.SYNC_APPROVAL.getBitPosition(), true);
		commandControlWord.set(PCSControlWordBitPosition.MODE_SELECTION.getBitPosition(), true);
		commandControlWord.set(PCSControlWordBitPosition.ACTIVATE_SHORT_CIRCUIT_HANDLING.getBitPosition(), true);

		commandControlWord.set(PCSControlWordBitPosition.DISABLE_IPU_1.getBitPosition(), true);
		commandControlWord.set(PCSControlWordBitPosition.DISABLE_IPU_2.getBitPosition(), true);
		commandControlWord.set(PCSControlWordBitPosition.DISABLE_IPU_3.getBitPosition(), true);
		commandControlWord.set(PCSControlWordBitPosition.DISABLE_IPU_4.getBitPosition(), false);
	}

	// TODO Shutdown system
//	private void stopSystem() {
//		log.info("Try to stop system");
//
//		// disable "Sync Approval" and "Ena IPU 4, 3, 2, 1" and add STOP command ->
//		// system should change state to "IDLE"
//		commandControlWord.set(PCSControlWordBitPosition.STOP.getBitPosition(), true);
//		commandControlWord.set(PCSControlWordBitPosition.SYNC_APPROVAL.getBitPosition(), false);
//		commandControlWord.set(PCSControlWordBitPosition.BLACKSTART_APPROVAL.getBitPosition(), false);
//		commandControlWord.set(PCSControlWordBitPosition.MODE_SELECTION.getBitPosition(), true);
//
//		commandControlWord.set(PCSControlWordBitPosition.DISABLE_IPU_1.getBitPosition(), true);
//		commandControlWord.set(PCSControlWordBitPosition.DISABLE_IPU_2.getBitPosition(), true);
//		commandControlWord.set(PCSControlWordBitPosition.DISABLE_IPU_3.getBitPosition(), true);
//		commandControlWord.set(PCSControlWordBitPosition.DISABLE_IPU_4.getBitPosition(), true);
//	}

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

	/**
	 * Writes parameters to all 4 IPUs !! Max charge/discharge power for IPUs always
	 * in absolute values !!
	 */
	private void writeIPUParameters(float weightA, float weightB, float weightC, float pMaxDischargeIPU1,
			float pMaxDischargeIPU2, float pMaxDischargeIPU3, float pMaxChargeIPU1, float pMaxChargeIPU2,
			float pMaxChargeIPU3) {

		writeValueToChannel(GridConChannelId.CONTROL_IPU_1_PARAMETERS_DC_VOLTAGE_SETPOINT, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_1_PARAMETERS_DC_CURRENT_SETPOINT, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_1_PARAMETERS_U0_OFFSET_TO_CCU_VALUE, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_1_PARAMETERS_F0_OFFSET_TO_CCU_VALUE, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_1_PARAMETERS_Q_REF_OFFSET_TO_CCU_VALUE, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_1_PARAMETERS_P_REF_OFFSET_TO_CCU_VALUE, 0f);

		writeValueToChannel(GridConChannelId.CONTROL_IPU_2_PARAMETERS_DC_VOLTAGE_SETPOINT, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_2_PARAMETERS_DC_CURRENT_SETPOINT, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_2_PARAMETERS_U0_OFFSET_TO_CCU_VALUE, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_2_PARAMETERS_F0_OFFSET_TO_CCU_VALUE, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_2_PARAMETERS_Q_REF_OFFSET_TO_CCU_VALUE, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_2_PARAMETERS_P_REF_OFFSET_TO_CCU_VALUE, 0f);

		writeValueToChannel(GridConChannelId.CONTROL_IPU_3_PARAMETERS_DC_VOLTAGE_SETPOINT, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_3_PARAMETERS_DC_CURRENT_SETPOINT, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_3_PARAMETERS_U0_OFFSET_TO_CCU_VALUE, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_3_PARAMETERS_F0_OFFSET_TO_CCU_VALUE, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_3_PARAMETERS_Q_REF_OFFSET_TO_CCU_VALUE, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_3_PARAMETERS_P_REF_OFFSET_TO_CCU_VALUE, 0f);

		writeValueToChannel(GridConChannelId.CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_DC_VOLTAGE_SETPOINT, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_A, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_B, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_C, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_I_REF_STRING_A, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_I_REF_STRING_B, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_I_REF_STRING_C, 0f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_DC_DC_STRING_CONTROL_MODE,
				0f); //

		// The value of 800 Volt is given by MR as a good reference value
		writeValueToChannel(GridConChannelId.CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_DC_VOLTAGE_SETPOINT, 800f);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_A, weightA);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_B, weightB);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_C, weightC);
		// The value '73' implies that all 3 strings are in weighting mode
		writeValueToChannel(GridConChannelId.CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_DC_DC_STRING_CONTROL_MODE,
				73f); //

		// Gridcon needs negative values for discharge values
		writeValueToChannel(GridConChannelId.CONTROL_IPU_1_PARAMETERS_P_MAX_DISCHARGE, -pMaxDischargeIPU1);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_2_PARAMETERS_P_MAX_DISCHARGE, -pMaxDischargeIPU2);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_3_PARAMETERS_P_MAX_DISCHARGE, -pMaxDischargeIPU3);
		// Gridcon needs positive values for charge values
		writeValueToChannel(GridConChannelId.CONTROL_IPU_1_PARAMETERS_P_MAX_CHARGE, pMaxChargeIPU1);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_2_PARAMETERS_P_MAX_CHARGE, pMaxChargeIPU2);
		writeValueToChannel(GridConChannelId.CONTROL_IPU_3_PARAMETERS_P_MAX_CHARGE, pMaxChargeIPU3);
	}

	private void doErrorHandling() {
		if (isHardwareTrip()) {
			doHardRestart();
		} else {
			log.info("try to acknowledge errors");
			acknowledgeErrors();
		}
	}

	private void doHardRestart() {	
		try {				
			log.info("in doHardRestart");
				if (timestampMrGridconWasSwitchedOff == null) {
					log.info("timestampMrGridconWasSwitchedOff was not set yet! try to write 'true' to channelHardReset!");
					BooleanWriteChannel channelHardReset = outputMRHardResetComponent.channel(outputMRHardReset.getChannelId());
					channelHardReset.setNextWriteValue(true);
					timestampMrGridconWasSwitchedOff = LocalDateTime.now();
				}
		} catch (OpenemsException e) {
			log.error("Problem occurred while activating hardware switch to restart Mr. Gridcon!");
			e.printStackTrace();
		}
		
	}

	private boolean isHardwareTrip() {
		log.info("in isHardwareTrip");
		IntegerReadChannel errorCodeChannel = this.channel(GridConChannelId.CCU_ERROR_CODE); 
		Optional<Integer> errorCodeOpt = errorCodeChannel.value().asOptional();		
		if (errorCodeOpt.isPresent()) {			
			int code = errorCodeOpt.get();
			log.info("Error code is present --> " + code);
			int mainCode = ( (code >> 24) & 255) ;
			int bit = ( (code >> 16) & 255) ;
			int b = ( (code >> 8) & 255) ;			
			ErrorCode errorCode = ErrorCode.getErrorCodeFromCode(code);
			log.info("main code: " + mainCode + "; bit: " + bit + "; b: " + b + "; ==> Errorcode: " + errorCode.text);
			return errorCode.needsHardReset;
		} 
		return false;
	}

	LocalDateTime lastTimeAcknowledgeCommandoWasSent;
	long ACKNOWLEDGE_TIME_SECONDS = 5;

	/**
	 * This sends an ACKNOWLEDGE message. This does not fix the error. If the error
	 * was fixed previously the system should continue operating normally. If not a
	 * manual restart may be necessary.
	 */
	private void acknowledgeErrors() {
		if (lastTimeAcknowledgeCommandoWasSent == null || LocalDateTime.now()
				.isAfter(lastTimeAcknowledgeCommandoWasSent.plusSeconds(ACKNOWLEDGE_TIME_SECONDS))) {
			commandControlWord.set(PCSControlWordBitPosition.ACKNOWLEDGE.getBitPosition(), true);
			lastTimeAcknowledgeCommandoWasSent = LocalDateTime.now();
		}
	}

	@Override
	public String debugLog() {
		return "State:" + this.getCurrentState().toString() + "," + "L:"
				+ this.channel(SymmetricEss.ChannelId.ACTIVE_POWER).value().asString() //
				+ "," + this.getOnOffGrid().name();
	}

	private CCUState getCurrentState() {
		if (((BooleanReadChannel) this.channel(GridConChannelId.CCU_STATE_IDLE)).value().asOptional()
				.orElse(false)) {
			return CCUState.IDLE;
		}

		if (((BooleanReadChannel) this.channel(GridConChannelId.CCU_STATE_PRECHARGE)).value().asOptional()
				.orElse(false)) {
			return CCUState.PRECHARGE;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.CCU_STATE_STOP_PRECHARGE)).value().asOptional()
				.orElse(false)) {
			return CCUState.STOP_PRECHARGE;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.CCU_STATE_READY)).value().asOptional()
				.orElse(false)) {
			return CCUState.READY;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.CCU_STATE_PAUSE)).value().asOptional()
				.orElse(false)) {
			return CCUState.PAUSE;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.CCU_STATE_RUN)).value().asOptional()
				.orElse(false)) {
			return CCUState.RUN;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.CCU_STATE_ERROR)).value().asOptional()
				.orElse(false)) {
			return CCUState.ERROR;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.CCU_STATE_VOLTAGE_RAMPING_UP)).value().asOptional()
				.orElse(false)) {
			return CCUState.VOLTAGE_RAMPING_UP;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.CCU_STATE_OVERLOAD)).value().asOptional()
				.orElse(false)) {
			return CCUState.OVERLOAD;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.CCU_STATE_SHORT_CIRCUIT_DETECTED)).value()
				.asOptional().orElse(false)) {
			return CCUState.SHORT_CIRCUIT_DETECTED;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.CCU_STATE_DERATING_POWER)).value().asOptional()
				.orElse(false)) {
			return CCUState.DERATING_POWER;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.CCU_STATE_DERATING_HARMONICS)).value().asOptional()
				.orElse(false)) {
			return CCUState.DERATING_HARMONICS;
		}
		if (((BooleanReadChannel) this.channel(GridConChannelId.CCU_STATE_SIA_ACTIVE)).value().asOptional()
				.orElse(false)) {
			return CCUState.SIA_ACTIVE;
		}

		return CCUState.UNDEFINED;
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public Constraint[] getStaticConstraints() {
		if (getCurrentState() != CCUState.RUN || this.getOnOffGrid() != GridMode.ON_GRID) {
			return new Constraint[] {
					this.createPowerConstraint("Inverter not ready", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0),
					this.createPowerConstraint("Inverter not ready", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) };
		} else {
//			return Power.NO_CONSTRAINTS;
			// calculate max charge and discharge power
			int currentMaxChargeBatteryA_W = batteryStringA.getVoltage().value().orElse(0)
					* batteryStringA.getChargeMaxCurrent().value().orElse(0);
			int maxChargeA_W = Math.min(currentMaxChargeBatteryA_W, batteryStringA.getMaxPower().value().orElse(0));

			int currentMaxChargeBatteryB_W = batteryStringB.getVoltage().value().orElse(0)
					* batteryStringB.getChargeMaxCurrent().value().orElse(0);
			int maxChargeB_W = Math.min(currentMaxChargeBatteryB_W, batteryStringB.getMaxPower().value().orElse(0));

			int currentMaxChargeBatteryC_W = batteryStringC.getVoltage().value().orElse(0)
					* batteryStringC.getChargeMaxCurrent().value().orElse(0);
			int maxChargeC_W = Math.min(currentMaxChargeBatteryC_W, batteryStringC.getMaxPower().value().orElse(0));

			int maxCharge_W = (maxChargeA_W + maxChargeB_W + maxChargeC_W);
			maxCharge_W = (-1) * Math.min(maxCharge_W, (int) MAX_CHARGE_W);

			int currentMaxDischargeBatteryA_W = batteryStringA.getVoltage().value().orElse(0)
					* batteryStringA.getDischargeMaxCurrent().value().orElse(0);
			int maxDischargeA_W = Math.min(currentMaxDischargeBatteryA_W,
					batteryStringA.getMaxPower().value().orElse(0));

			int currentMaxDischargeBatteryB_W = batteryStringB.getVoltage().value().orElse(0)
					* batteryStringB.getDischargeMaxCurrent().value().orElse(0);
			int maxDischargeB_W = Math.min(currentMaxDischargeBatteryB_W,
					batteryStringB.getMaxPower().value().orElse(0));

			int currentMaxDischargeBatteryC_W = batteryStringC.getVoltage().value().orElse(0)
					* batteryStringC.getDischargeMaxCurrent().value().orElse(0);
			int maxDischargeC_W = Math.min(currentMaxDischargeBatteryC_W,
					batteryStringC.getMaxPower().value().orElse(0));

			int maxDischarge_W = (maxDischargeA_W + maxDischargeB_W + maxDischargeC_W);
			maxDischarge_W = Math.min(maxDischarge_W, (int) MAX_DISCHARGE_W);

			log.info("getStaticConstraints() maxCharge:" + maxCharge_W + "; maxDischarge:" + maxDischarge_W);

			return new Constraint[] {
					this.createPowerConstraint("GridCon PCS calculated max charge power", Phase.ALL, Pwr.ACTIVE,
							Relationship.GREATER_OR_EQUALS, maxCharge_W),
					this.createPowerConstraint("GridCon PCS calculated max discharge power", Phase.ALL, Pwr.ACTIVE,
							Relationship.LESS_OR_EQUALS, maxDischarge_W),
					this.createPowerConstraint("GridCon PCS", Phase.ALL, Pwr.REACTIVE, Relationship.LESS_OR_EQUALS,
							MAX_APPARENT_POWER) };
		}
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
		
		doStringWeighting(activePower, reactivePower);
		/*
		 * !! signum, MR calculates negative values as discharge, positive as charge.
		 * Gridcon sets the (dis)charge according to a percentage of the MAX_POWER. So
		 * 0.1 => 10% of max power. Values should never take values lower than -1 or
		 * higher than 1.
		 */
		float activePowerFactor = -activePower / MAX_POWER_W;
		float reactivePowerFactor = -reactivePower / MAX_POWER_W;

		writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_P_REF, activePowerFactor);
		writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_Q_REF, reactivePowerFactor);
	}

	private void doStringWeighting(int activePower, int reactivePower) {
		// weight according to battery ranges		
		// weight considering SoC of the batteries...
		
		
		int weightA = 0;
		int weightB = 0;
		int weightC = 0;

		// weight strings according to max allowed current
		// use values for discharging
		if (activePower > 0) {
			weightA = batteryStringA.getDischargeMaxCurrent().value().asOptional().orElse(0);
			weightB = batteryStringB.getDischargeMaxCurrent().value().asOptional().orElse(0);
			weightC = batteryStringC.getDischargeMaxCurrent().value().asOptional().orElse(0);
			
			//if minSoc is reached, do not allow further discharging
			if (batteryStringA.getSoc().value().asOptional().orElse(0) <= minSoCA) {
				weightA = 0;
			}
			if (batteryStringB.getSoc().value().asOptional().orElse(0) <= minSoCB) {
				weightB = 0;
			}
			if (batteryStringC.getSoc().value().asOptional().orElse(0) <= minSoCC) {
				weightC = 0;
			}
			
		} else { // use values for charging
			weightA = batteryStringA.getChargeMaxCurrent().value().asOptional().orElse(0);
			weightB = batteryStringB.getChargeMaxCurrent().value().asOptional().orElse(0);
			weightC = batteryStringC.getChargeMaxCurrent().value().asOptional().orElse(0);
		}

		// TODO discuss if this is correct!
		int maxChargePower1 = batteryStringA.getChargeMaxCurrent().value().asOptional().orElse(0)
				* batteryStringA.getChargeMaxVoltage().value().asOptional().orElse(0);
		int maxChargePower2 = batteryStringB.getChargeMaxCurrent().value().asOptional().orElse(0)
				* batteryStringB.getChargeMaxVoltage().value().asOptional().orElse(0);
		int maxChargePower3 = batteryStringC.getChargeMaxCurrent().value().asOptional().orElse(0)
				* batteryStringC.getChargeMaxVoltage().value().asOptional().orElse(0);

		int maxDischargePower1 = batteryStringA.getDischargeMaxCurrent().value().asOptional().orElse(0)
				* batteryStringA.getDischargeMinVoltage().value().asOptional().orElse(0);
		int maxDischargePower2 = batteryStringB.getDischargeMaxCurrent().value().asOptional().orElse(0)
				* batteryStringB.getDischargeMinVoltage().value().asOptional().orElse(0);
		int maxDischargePower3 = batteryStringC.getDischargeMaxCurrent().value().asOptional().orElse(0)
				* batteryStringC.getDischargeMinVoltage().value().asOptional().orElse(0);

		writeIPUParameters(weightA, weightB, weightC, maxDischargePower1, maxDischargePower2, maxDischargePower3,
				maxChargePower1, maxChargePower2, maxChargePower3);
	}

	/** Writes the given value into the channel */
	void writeValueToChannel(GridConChannelId channelId, Object value) {
		try {
			((WriteChannel<?>) this.channel(channelId)).setNextWriteValueFromObject(value);
		} catch (OpenemsException e) {
			e.printStackTrace();
			log.error("Problem occurred during writing '" + value + "' to channel " + channelId.name());
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
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			handleStateMachine();
			calculateSoC();
			break;
		}
	}

	private void handleOffGridState() {
		// Always set OutputSyncDeviceBridge ON in Off-Grid state
		log.info("Set K3 ON");
		this.setOutputSyncDeviceBridge(true);
		// TODO check if OutputSyncDeviceBridge was actually set to ON via
		// inputSyncDeviceBridgeComponent. On Error switch off the MR.

		// Measured by Grid-Meter, grid Values
		int gridFreq = this.gridMeter.getFrequency().value().orElse(-1);
		int gridVolt = this.gridMeter.getVoltage().value().orElse(-1);

		log.info("GridFreq: " + gridFreq + ", GridVolt: " + gridVolt);

		// Always set Voltage Control Mode + Blackstart Approval
		commandControlWord.set(PCSControlWordBitPosition.BLACKSTART_APPROVAL.getBitPosition(), true);
		commandControlWord.set(PCSControlWordBitPosition.MODE_SELECTION.getBitPosition(), false);

		if (gridFreq == 0 || gridFreq < 49_700 || gridFreq > 50_300 || //
				gridVolt == 0 || gridVolt < 215_000 || gridVolt > 245_000) {
			log.info("Off-Grid -> F/U 1");
			/*
			 * Off-Grid
			 */
			writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_U0, 1.0f);
			writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_F0, 1.0f);

		} else {
			/*
			 * Going On-Grid
			 */
			int invSetFreq = gridFreq + 20; // add 20 mHz
			int invSetVolt = gridVolt + 5_000; // add 5 V
			float invSetFreqNormalized = invSetFreq / 50_000f;
			float invSetVoltNormalized = invSetVolt / 230_000f;
			log.info("Going On-Grid -> F/U " + invSetFreq + ", " + invSetVolt + ", " + invSetFreqNormalized + ", "
					+ invSetVoltNormalized);
			writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_U0, invSetVoltNormalized);
			writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_F0, invSetFreqNormalized);
		}
	}

	private void calculateSoC() {
		double sumCapacity = 0;
		double sumCurrentCapacity = 0;
		for (Battery b : new Battery[] { batteryStringA, batteryStringB, batteryStringC }) {
			sumCapacity = sumCapacity + b.getCapacity().value().asOptional().orElse(0);
			sumCurrentCapacity = sumCurrentCapacity
					+ b.getCapacity().value().asOptional().orElse(0) * b.getSoc().value().orElse(0) / 100.0;
		}
		int soC = (int) (sumCurrentCapacity * 100 / sumCapacity);
		this.getSoc().setNextValue(soC);
	}
	
	// checks if bit at requested position is set and writes it to given channel id 
	private void mapBitToChannel(Long ctrlWord, PCSControlWordBitPosition bitPosition, GridConChannelId id) {				
		boolean val = ((ctrlWord >> bitPosition.getBitPosition()) & 1) == 1;		
		this.channel(id).setNextValue(val);
	}
	
	// checks if bit at requested position is set and writes it to given channel id 
	private void mapBitToChannel(Integer ctrlWord, PCSControlWordBitPosition bitPosition, GridConChannelId id) {				
		boolean val = ((ctrlWord >> bitPosition.getBitPosition()) & 1) == 1;		
		this.channel(id).setNextValue(val);
	}
	
	/**
	 * Handles channels that are mapping to one bit of a 
	 * modbus unsigned double word element
	 */
	public class DoubleWordErrorCodeChannelMapper {
		private final UnsignedDoublewordElement element;
		private final Map<Integer, Channel<?>> channels = new HashMap<>();

		public DoubleWordErrorCodeChannelMapper(UnsignedDoublewordElement element) {
			this.element = element;
			this.element.onUpdateCallback((value) -> {
				this.channels.forEach((bitIndex, channel) -> {
					channel.setNextValue(value << ~bitIndex < 0);
				});
			});
		}

		public DoubleWordErrorCodeChannelMapper m(io.openems.edge.common.channel.doc.ChannelId channelId, int bitIndex) {
			Channel<?> channel = channel(channelId);
			if (channel.getType() != OpenemsType.BOOLEAN) {
				throw new IllegalArgumentException(
						"Channel [" + channelId + "] must be of type [BOOLEAN] for bit-mapping.");
			}
			this.channels.put(bitIndex, channel);
			return this;
		}

		public UnsignedDoublewordElement build() {
			return this.element;
		}
	}

	/**
	 * Creates a DoubleWordErrorCodeChannelMapper that can be used with builder pattern inside the
	 * protocol definition.
	 * 
	 * @param element
	 * @return
	 */
	protected final DoubleWordErrorCodeChannelMapper map(UnsignedDoublewordElement element) {
		return new DoubleWordErrorCodeChannelMapper(element);
	}

	protected ModbusProtocol defineModbusProtocol(int unitId) {
		ModbusProtocol protocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(32528, Priority.HIGH, // CCU state
						bm(new UnsignedDoublewordElement(32528)) //
								.m(GridConChannelId.CCU_STATE_IDLE, 0) //
								.m(GridConChannelId.CCU_STATE_PRECHARGE, 1) //
								.m(GridConChannelId.CCU_STATE_STOP_PRECHARGE, 2) //
								.m(GridConChannelId.CCU_STATE_READY, 3) //
								.m(GridConChannelId.CCU_STATE_PAUSE, 4) //
								.m(GridConChannelId.CCU_STATE_RUN, 5) //
								.m(GridConChannelId.CCU_STATE_ERROR, 6) //
								.m(GridConChannelId.CCU_STATE_VOLTAGE_RAMPING_UP, 7) //
								.m(GridConChannelId.CCU_STATE_OVERLOAD, 8) //
								.m(GridConChannelId.CCU_STATE_SHORT_CIRCUIT_DETECTED, 9) //
								.m(GridConChannelId.CCU_STATE_DERATING_POWER, 10) //
								.m(GridConChannelId.CCU_STATE_DERATING_HARMONICS, 11) //
								.m(GridConChannelId.CCU_STATE_SIA_ACTIVE, 12) //
								.build().wordOrder(WordOrder.LSWMSW), //
								// TODO check ErrorCode if this is corresponding
								m(GridConChannelId.CCU_ERROR_CODE, new UnsignedDoublewordElement(32530).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CCU_VOLTAGE_U12,
								new FloatDoublewordElement(32532).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CCU_VOLTAGE_U23,
								new FloatDoublewordElement(32534).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CCU_VOLTAGE_U31,
								new FloatDoublewordElement(32536).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CCU_CURRENT_IL1,
								new FloatDoublewordElement(32538).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CCU_CURRENT_IL2,
								new FloatDoublewordElement(32540).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CCU_CURRENT_IL3,
								new FloatDoublewordElement(32542).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CCU_POWER_P,
								new FloatDoublewordElement(32544).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CCU_POWER_Q,
								new FloatDoublewordElement(32546).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CCU_FREQUENCY,
								new FloatDoublewordElement(32548).wordOrder(WordOrder.LSWMSW)) //
				), new FC3ReadRegistersTask(33168, Priority.LOW, // IPU 1 state
						byteMap(new UnsignedDoublewordElement(33168)) //
								.mapByte(GridConChannelId.IPU_1_STATUS_STATUS_STATE_MACHINE, 0) //
								.mapByte(GridConChannelId.IPU_1_STATUS_STATUS_MCU, 1) //
								.build().wordOrder(WordOrder.LSWMSW), //
						m(GridConChannelId.IPU_1_STATUS_FILTER_CURRENT,
								new FloatDoublewordElement(33170).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_STATUS_DC_LINK_POSITIVE_VOLTAGE,
								new FloatDoublewordElement(33172).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_STATUS_DC_LINK_NEGATIVE_VOLTAGE,
								new FloatDoublewordElement(33174).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_STATUS_DC_LINK_CURRENT,
								new FloatDoublewordElement(33176).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_STATUS_DC_LINK_ACTIVE_POWER,
								new FloatDoublewordElement(33178).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_STATUS_DC_LINK_UTILIZATION,
								new FloatDoublewordElement(33180).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_STATUS_FAN_SPEED_MAX,
								new UnsignedDoublewordElement(33182).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_STATUS_FAN_SPEED_MIN,
								new UnsignedDoublewordElement(33184).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_STATUS_TEMPERATURE_IGBT_MAX,
								new FloatDoublewordElement(33186).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_STATUS_TEMPERATURE_MCU_BOARD,
								new FloatDoublewordElement(33188).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_STATUS_TEMPERATURE_GRID_CHOKE,
								new FloatDoublewordElement(33190).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_STATUS_TEMPERATURE_INVERTER_CHOKE,
								new FloatDoublewordElement(33192).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_STATUS_RESERVE_1,
								new FloatDoublewordElement(33194).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_STATUS_RESERVE_2,
								new FloatDoublewordElement(33196).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_STATUS_RESERVE_3,
								new FloatDoublewordElement(33198).wordOrder(WordOrder.LSWMSW)) //
				), new FC3ReadRegistersTask(33200, Priority.LOW, // // IPU 2 state
						byteMap(new UnsignedDoublewordElement(33200)) //
								.mapByte(GridConChannelId.IPU_2_STATUS_STATUS_STATE_MACHINE, 0) //
								.mapByte(GridConChannelId.IPU_2_STATUS_STATUS_MCU, 1) //
								.build().wordOrder(WordOrder.LSWMSW), //
						m(GridConChannelId.IPU_2_STATUS_FILTER_CURRENT,
								new FloatDoublewordElement(33202).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_STATUS_DC_LINK_POSITIVE_VOLTAGE,
								new FloatDoublewordElement(33204).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_STATUS_DC_LINK_NEGATIVE_VOLTAGE,
								new FloatDoublewordElement(33206).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_STATUS_DC_LINK_CURRENT,
								new FloatDoublewordElement(33208).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_STATUS_DC_LINK_ACTIVE_POWER,
								new FloatDoublewordElement(33210).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_STATUS_DC_LINK_UTILIZATION,
								new FloatDoublewordElement(33212).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_STATUS_FAN_SPEED_MAX,
								new UnsignedDoublewordElement(33214).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_STATUS_FAN_SPEED_MIN,
								new UnsignedDoublewordElement(33216).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_STATUS_TEMPERATURE_IGBT_MAX,
								new FloatDoublewordElement(33218).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_STATUS_TEMPERATURE_MCU_BOARD,
								new FloatDoublewordElement(33220).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_STATUS_TEMPERATURE_GRID_CHOKE,
								new FloatDoublewordElement(33222).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_STATUS_TEMPERATURE_INVERTER_CHOKE,
								new FloatDoublewordElement(33224).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_STATUS_RESERVE_1,
								new FloatDoublewordElement(33226).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_STATUS_RESERVE_2,
								new FloatDoublewordElement(33228).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_STATUS_RESERVE_3,
								new FloatDoublewordElement(33230).wordOrder(WordOrder.LSWMSW)) //
				), new FC3ReadRegistersTask(33232, Priority.LOW, //
						byteMap(new UnsignedDoublewordElement(33232)) // // IPU 3 state
								.mapByte(GridConChannelId.IPU_3_STATUS_STATUS_STATE_MACHINE, 0) //
								.mapByte(GridConChannelId.IPU_3_STATUS_STATUS_MCU, 1) //
								.build().wordOrder(WordOrder.LSWMSW), //
						m(GridConChannelId.IPU_3_STATUS_FILTER_CURRENT,
								new FloatDoublewordElement(33234).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_STATUS_DC_LINK_POSITIVE_VOLTAGE,
								new FloatDoublewordElement(33236).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_STATUS_DC_LINK_NEGATIVE_VOLTAGE,
								new FloatDoublewordElement(33238).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_STATUS_DC_LINK_CURRENT,
								new FloatDoublewordElement(33240).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_STATUS_DC_LINK_ACTIVE_POWER,
								new FloatDoublewordElement(33242).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_STATUS_DC_LINK_UTILIZATION,
								new FloatDoublewordElement(33244).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_STATUS_FAN_SPEED_MAX,
								new UnsignedDoublewordElement(33246).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_STATUS_FAN_SPEED_MIN,
								new UnsignedDoublewordElement(33248).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_STATUS_TEMPERATURE_IGBT_MAX,
								new FloatDoublewordElement(33250).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_STATUS_TEMPERATURE_MCU_BOARD,
								new FloatDoublewordElement(33252).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_STATUS_TEMPERATURE_GRID_CHOKE,
								new FloatDoublewordElement(33254).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_STATUS_TEMPERATURE_INVERTER_CHOKE,
								new FloatDoublewordElement(33256).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_STATUS_RESERVE_1,
								new FloatDoublewordElement(33258).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_STATUS_RESERVE_2,
								new FloatDoublewordElement(33260).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_STATUS_RESERVE_3,
								new FloatDoublewordElement(33262).wordOrder(WordOrder.LSWMSW)) //
				), new FC3ReadRegistersTask(33264, Priority.LOW, // // IPU 4 state
						byteMap(new UnsignedDoublewordElement(33264)) //
								.mapByte(GridConChannelId.IPU_4_STATUS_STATUS_STATE_MACHINE, 0) //
								.mapByte(GridConChannelId.IPU_4_STATUS_STATUS_MCU, 1) //
								.build().wordOrder(WordOrder.LSWMSW), //
						m(GridConChannelId.IPU_4_STATUS_FILTER_CURRENT,
								new FloatDoublewordElement(33266).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_STATUS_DC_LINK_POSITIVE_VOLTAGE,
								new FloatDoublewordElement(33268).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_STATUS_DC_LINK_NEGATIVE_VOLTAGE,
								new FloatDoublewordElement(33270).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_STATUS_DC_LINK_CURRENT,
								new FloatDoublewordElement(33272).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_STATUS_DC_LINK_ACTIVE_POWER,
								new FloatDoublewordElement(33274).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_STATUS_DC_LINK_UTILIZATION,
								new FloatDoublewordElement(33276).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_STATUS_FAN_SPEED_MAX,
								new UnsignedDoublewordElement(33278).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_STATUS_FAN_SPEED_MIN,
								new UnsignedDoublewordElement(33280).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_STATUS_TEMPERATURE_IGBT_MAX,
								new FloatDoublewordElement(33282).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_STATUS_TEMPERATURE_MCU_BOARD,
								new FloatDoublewordElement(33284).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_STATUS_TEMPERATURE_GRID_CHOKE,
								new FloatDoublewordElement(33286).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_STATUS_TEMPERATURE_INVERTER_CHOKE,
								new FloatDoublewordElement(33288).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_STATUS_RESERVE_1,
								new FloatDoublewordElement(33290).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_STATUS_RESERVE_2,
								new FloatDoublewordElement(33292).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_STATUS_RESERVE_3,
								new FloatDoublewordElement(33294).wordOrder(WordOrder.LSWMSW)) // TODO: is this float?
				), new FC3ReadRegistersTask(33488, Priority.LOW, // // IPU 1 measurements
						m(GridConChannelId.IPU_1_DC_DC_MEASUREMENTS_VOLTAGE_STRING_A,
								new FloatDoublewordElement(33488).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_DC_DC_MEASUREMENTS_VOLTAGE_STRING_B,
								new FloatDoublewordElement(33490).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_DC_DC_MEASUREMENTS_VOLTAGE_STRING_C,
								new FloatDoublewordElement(33492).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_DC_DC_MEASUREMENTS_CURRENT_STRING_A,
								new FloatDoublewordElement(33494).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_DC_DC_MEASUREMENTS_CURRENT_STRING_B,
								new FloatDoublewordElement(33496).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_DC_DC_MEASUREMENTS_CURRENT_STRING_C,
								new FloatDoublewordElement(33498).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_DC_DC_MEASUREMENTS_POWER_STRING_A,
								new FloatDoublewordElement(33500).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_DC_DC_MEASUREMENTS_POWER_STRING_B,
								new FloatDoublewordElement(33502).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_DC_DC_MEASUREMENTS_POWER_STRING_C,
								new FloatDoublewordElement(33504).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_DC_DC_MEASUREMENTS_UTILIZATION_STRING_A,
								new FloatDoublewordElement(33506).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_DC_DC_MEASUREMENTS_UTILIZATION_STRING_B,
								new FloatDoublewordElement(33508).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_DC_DC_MEASUREMENTS_UTILIZATION_STRING_C,
								new FloatDoublewordElement(33510).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_DC_DC_MEASUREMENTS_ACCUMULATED_SUM_DC_CURRENT,
								new FloatDoublewordElement(33512).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_DC_DC_MEASUREMENTS_ACCUMULATED_DC_UTILIZATION,
								new FloatDoublewordElement(33514).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_DC_DC_MEASUREMENTS_RESERVE_1,
								new FloatDoublewordElement(33516).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_1_DC_DC_MEASUREMENTS_RESERVE_2,
								new FloatDoublewordElement(33518).wordOrder(WordOrder.LSWMSW)) //
				), new FC3ReadRegistersTask(33520, Priority.LOW, // IPU 2 measurements
						m(GridConChannelId.IPU_2_DC_DC_MEASUREMENTS_VOLTAGE_STRING_A,
								new FloatDoublewordElement(33520).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_DC_DC_MEASUREMENTS_VOLTAGE_STRING_B,
								new FloatDoublewordElement(33522).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_DC_DC_MEASUREMENTS_VOLTAGE_STRING_C,
								new FloatDoublewordElement(33524).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_DC_DC_MEASUREMENTS_CURRENT_STRING_A,
								new FloatDoublewordElement(33526).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_DC_DC_MEASUREMENTS_CURRENT_STRING_B,
								new FloatDoublewordElement(33528).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_DC_DC_MEASUREMENTS_CURRENT_STRING_C,
								new FloatDoublewordElement(33530).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_DC_DC_MEASUREMENTS_POWER_STRING_A,
								new FloatDoublewordElement(33532).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_DC_DC_MEASUREMENTS_POWER_STRING_B,
								new FloatDoublewordElement(33534).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_DC_DC_MEASUREMENTS_POWER_STRING_C,
								new FloatDoublewordElement(33536).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_DC_DC_MEASUREMENTS_UTILIZATION_STRING_A,
								new FloatDoublewordElement(33538).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_DC_DC_MEASUREMENTS_UTILIZATION_STRING_B,
								new FloatDoublewordElement(33540).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_DC_DC_MEASUREMENTS_UTILIZATION_STRING_C,
								new FloatDoublewordElement(33542).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_DC_DC_MEASUREMENTS_ACCUMULATED_SUM_DC_CURRENT,
								new FloatDoublewordElement(33544).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_DC_DC_MEASUREMENTS_ACCUMULATED_DC_UTILIZATION,
								new FloatDoublewordElement(33546).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_DC_DC_MEASUREMENTS_RESERVE_1,
								new FloatDoublewordElement(33548).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_2_DC_DC_MEASUREMENTS_RESERVE_2,
								new FloatDoublewordElement(33550).wordOrder(WordOrder.LSWMSW)) //
				), new FC3ReadRegistersTask(33552, Priority.LOW, // IPU 3 measurements
						m(GridConChannelId.IPU_3_DC_DC_MEASUREMENTS_VOLTAGE_STRING_A,
								new FloatDoublewordElement(33552).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_DC_DC_MEASUREMENTS_VOLTAGE_STRING_B,
								new FloatDoublewordElement(33554).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_DC_DC_MEASUREMENTS_VOLTAGE_STRING_C,
								new FloatDoublewordElement(33556).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_DC_DC_MEASUREMENTS_CURRENT_STRING_A,
								new FloatDoublewordElement(33558).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_DC_DC_MEASUREMENTS_CURRENT_STRING_B,
								new FloatDoublewordElement(33560).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_DC_DC_MEASUREMENTS_CURRENT_STRING_C,
								new FloatDoublewordElement(33562).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_DC_DC_MEASUREMENTS_POWER_STRING_A,
								new FloatDoublewordElement(33564).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_DC_DC_MEASUREMENTS_POWER_STRING_B,
								new FloatDoublewordElement(33566).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_DC_DC_MEASUREMENTS_POWER_STRING_C,
								new FloatDoublewordElement(33568).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_DC_DC_MEASUREMENTS_UTILIZATION_STRING_A,
								new FloatDoublewordElement(33570).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_DC_DC_MEASUREMENTS_UTILIZATION_STRING_B,
								new FloatDoublewordElement(33572).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_DC_DC_MEASUREMENTS_UTILIZATION_STRING_C,
								new FloatDoublewordElement(33574).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_DC_DC_MEASUREMENTS_ACCUMULATED_SUM_DC_CURRENT,
								new FloatDoublewordElement(33576).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_DC_DC_MEASUREMENTS_ACCUMULATED_DC_UTILIZATION,
								new FloatDoublewordElement(33578).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_DC_DC_MEASUREMENTS_RESERVE_1,
								new FloatDoublewordElement(33580).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_3_DC_DC_MEASUREMENTS_RESERVE_2,
								new FloatDoublewordElement(33582).wordOrder(WordOrder.LSWMSW)) //
				), new FC3ReadRegistersTask(33584, Priority.LOW, // IPU 4 measurements
						m(GridConChannelId.IPU_4_DC_DC_MEASUREMENTS_VOLTAGE_STRING_A,
								new FloatDoublewordElement(33584).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_DC_DC_MEASUREMENTS_VOLTAGE_STRING_B,
								new FloatDoublewordElement(33586).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_DC_DC_MEASUREMENTS_VOLTAGE_STRING_C,
								new FloatDoublewordElement(33588).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_DC_DC_MEASUREMENTS_CURRENT_STRING_A,
								new FloatDoublewordElement(33590).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_DC_DC_MEASUREMENTS_CURRENT_STRING_B,
								new FloatDoublewordElement(33592).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_DC_DC_MEASUREMENTS_CURRENT_STRING_C,
								new FloatDoublewordElement(33594).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_DC_DC_MEASUREMENTS_POWER_STRING_A,
								new FloatDoublewordElement(33596).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_DC_DC_MEASUREMENTS_POWER_STRING_B,
								new FloatDoublewordElement(33598).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_DC_DC_MEASUREMENTS_POWER_STRING_C,
								new FloatDoublewordElement(33600).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_DC_DC_MEASUREMENTS_UTILIZATION_STRING_A,
								new FloatDoublewordElement(33602).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_DC_DC_MEASUREMENTS_UTILIZATION_STRING_B,
								new FloatDoublewordElement(33604).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_DC_DC_MEASUREMENTS_UTILIZATION_STRING_C,
								new FloatDoublewordElement(33606).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_DC_DC_MEASUREMENTS_ACCUMULATED_SUM_DC_CURRENT,
								new FloatDoublewordElement(33608).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_DC_DC_MEASUREMENTS_ACCUMULATED_DC_UTILIZATION,
								new FloatDoublewordElement(33610).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_DC_DC_MEASUREMENTS_RESERVE_1,
								new FloatDoublewordElement(33612).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.IPU_4_DC_DC_MEASUREMENTS_RESERVE_2,
								new FloatDoublewordElement(33614).wordOrder(WordOrder.LSWMSW)) //
				), new FC16WriteRegistersTask(32560, // Commands
						m(GridConChannelId.COMMAND_CONTROL_WORD,
								new UnsignedDoublewordElement(32560).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_ERROR_CODE_FEEDBACK,
								new UnsignedDoublewordElement(32562).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_U0,
								new FloatDoublewordElement(32564).wordOrder(WordOrder.LSWMSW)), // TODO Check word order
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_F0,
								new FloatDoublewordElement(32566).wordOrder(WordOrder.LSWMSW)), // TODO Check word order
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_Q_REF,
								new FloatDoublewordElement(32568).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_P_REF,
								new FloatDoublewordElement(32570).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_TIME_SYNC_DATE,
								new UnsignedDoublewordElement(32572).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_TIME_SYNC_TIME,
								new UnsignedDoublewordElement(32574).wordOrder(WordOrder.LSWMSW)) //
				), new FC16WriteRegistersTask(32592, // Control parameters
						m(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_MAIN,
								new FloatDoublewordElement(32592).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN,
								new FloatDoublewordElement(32594).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_F_P_DRROP_MAIN,
								new FloatDoublewordElement(32596).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_T1_MAIN,
								new FloatDoublewordElement(32598).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_U_DROOP_MAIN,
								new FloatDoublewordElement(32600).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_U_DEAD_BAND,
								new FloatDoublewordElement(32602).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_LIMIT,
								new FloatDoublewordElement(32604).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_F_DROOP_MAIN,
								new FloatDoublewordElement(32606).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_F_DEAD_BAND,
								new FloatDoublewordElement(32608).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_DROOP,
								new FloatDoublewordElement(32610).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_DEAD_BAND,
								new FloatDoublewordElement(32612).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_MAX_CHARGE,
								new FloatDoublewordElement(32614).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_MAX_DISCHARGE,
								new FloatDoublewordElement(32616).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_MODE,
								new FloatDoublewordElement(32618).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_TWO,
								new FloatDoublewordElement(32620).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_ONE,
								new FloatDoublewordElement(32622).wordOrder(WordOrder.LSWMSW)) //
				), new FC16WriteRegistersTask(32624, // IPU 1 control parameters
						m(GridConChannelId.CONTROL_IPU_1_PARAMETERS_DC_VOLTAGE_SETPOINT,
								new FloatDoublewordElement(32624).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_1_PARAMETERS_DC_CURRENT_SETPOINT,
								new FloatDoublewordElement(32626).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_1_PARAMETERS_U0_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(32628).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_1_PARAMETERS_F0_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(32630).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_1_PARAMETERS_Q_REF_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(32632).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_1_PARAMETERS_P_REF_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(32634).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_1_PARAMETERS_P_MAX_DISCHARGE,
								new FloatDoublewordElement(32636).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_1_PARAMETERS_P_MAX_CHARGE,
								new FloatDoublewordElement(32638).wordOrder(WordOrder.LSWMSW)) //
				), new FC16WriteRegistersTask(32656, // IPU 2 control parameters
						m(GridConChannelId.CONTROL_IPU_2_PARAMETERS_DC_VOLTAGE_SETPOINT,
								new FloatDoublewordElement(32656).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_2_PARAMETERS_DC_CURRENT_SETPOINT,
								new FloatDoublewordElement(32658).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_2_PARAMETERS_U0_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(32660).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_2_PARAMETERS_F0_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(32662).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_2_PARAMETERS_Q_REF_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(32664).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_2_PARAMETERS_P_REF_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(32666).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_2_PARAMETERS_P_MAX_DISCHARGE,
								new FloatDoublewordElement(32668).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_2_PARAMETERS_P_MAX_CHARGE,
								new FloatDoublewordElement(32670).wordOrder(WordOrder.LSWMSW)) //
				), new FC16WriteRegistersTask(32688, // IPU 3 control parameters
						m(GridConChannelId.CONTROL_IPU_3_PARAMETERS_DC_VOLTAGE_SETPOINT,
								new FloatDoublewordElement(32688).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_3_PARAMETERS_DC_CURRENT_SETPOINT,
								new FloatDoublewordElement(32690).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_3_PARAMETERS_U0_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(32692).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_3_PARAMETERS_F0_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(32694).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_3_PARAMETERS_Q_REF_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(32696).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_3_PARAMETERS_P_REF_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(32698).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_3_PARAMETERS_P_MAX_DISCHARGE,
								new FloatDoublewordElement(32700).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_3_PARAMETERS_P_MAX_CHARGE,
								new FloatDoublewordElement(32702).wordOrder(WordOrder.LSWMSW)) //
				), new FC16WriteRegistersTask(32720, // IPU 4 control parameters
						m(GridConChannelId.CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_DC_VOLTAGE_SETPOINT,
								new FloatDoublewordElement(32720).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_A,
								new FloatDoublewordElement(32722).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_B,
								new FloatDoublewordElement(32724).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_C,
								new FloatDoublewordElement(32726).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_I_REF_STRING_A,
								new FloatDoublewordElement(32728).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_I_REF_STRING_B,
								new FloatDoublewordElement(32730).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_I_REF_STRING_C,
								new FloatDoublewordElement(32732).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_DC_DC_STRING_CONTROL_MODE,
								new FloatDoublewordElement(32734).wordOrder(WordOrder.LSWMSW)) //
				)
				// Mirror values to check
				, new FC3ReadRegistersTask(32880, Priority.LOW, // Commands
						m(GridConChannelId.MIRROR_COMMAND_CONTROL_WORD,
								new UnsignedDoublewordElement(32880).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_COMMAND_ERROR_CODE_FEEDBACK,
								new UnsignedDoublewordElement(32882).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_COMMAND_CONTROL_PARAMETER_U0,
								new FloatDoublewordElement(32884).wordOrder(WordOrder.LSWMSW)),
						m(GridConChannelId.MIRROR_COMMAND_CONTROL_PARAMETER_F0,
								new FloatDoublewordElement(32886).wordOrder(WordOrder.LSWMSW)),
						m(GridConChannelId.MIRROR_COMMAND_CONTROL_PARAMETER_Q_REF,
								new FloatDoublewordElement(32888).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_COMMAND_CONTROL_PARAMETER_P_REFERENCE,
								new FloatDoublewordElement(32890).wordOrder(WordOrder.LSWMSW)) //
				),
				new FC3ReadRegistersTask(32912, Priority.LOW,
						m(GridConChannelId.MIRROR_CONTROL_PARAMETER_U_Q_DROOP_MAIN,
								new FloatDoublewordElement(32912).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN,
								new FloatDoublewordElement(32914).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_PARAMETER_F_P_DRROP_MAIN,
								new FloatDoublewordElement(32916).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_PARAMETER_F_P_DROOP_T1_MAIN,
								new FloatDoublewordElement(32918).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_PARAMETER_Q_U_DROOP_MAIN,
								new FloatDoublewordElement(32920).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_PARAMETER_Q_U_DEAD_BAND,
								new FloatDoublewordElement(32922).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_PARAMETER_Q_LIMIT,
								new FloatDoublewordElement(32924).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_PARAMETER_P_F_DROOP_MAIN,
								new FloatDoublewordElement(32926).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_PARAMETER_P_F_DEAD_BAND,
								new FloatDoublewordElement(32928).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_PARAMETER_P_U_DROOP,
								new FloatDoublewordElement(32930).wordOrder(WordOrder.LSWMSW)) //
				),
				new FC3ReadRegistersTask(32944, Priority.LOW,
						m(GridConChannelId.MIRROR_CONTROL_IPU_1_PARAMETERS_DC_VOLTAGE_SETPOINT,
								new FloatDoublewordElement(32944).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_IPU_1_PARAMETERS_DC_CURRENT_SETPOINT,
								new FloatDoublewordElement(32946).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_IPU_1_PARAMETERS_U0_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(32948).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_IPU_1_PARAMETERS_F0_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(32950).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_IPU_1_PARAMETERS_Q_REF_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(32952).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_IPU_1_PARAMETERS_P_REF_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(32954).wordOrder(WordOrder.LSWMSW)) //
				),
				new FC3ReadRegistersTask(32976, Priority.LOW,
						m(GridConChannelId.MIRROR_CONTROL_IPU_2_PARAMETERS_DC_VOLTAGE_SETPOINT,
								new FloatDoublewordElement(32976).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_IPU_2_PARAMETERS_DC_CURRENT_SETPOINT,
								new FloatDoublewordElement(32978).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_IPU_2_PARAMETERS_U0_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(32980).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_IPU_2_PARAMETERS_F0_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(32982).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_IPU_2_PARAMETERS_Q_REF_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(32984).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_IPU_2_PARAMETERS_P_REF_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(32986).wordOrder(WordOrder.LSWMSW)) //
				),
				new FC3ReadRegistersTask(33008, Priority.LOW,
						m(GridConChannelId.MIRROR_CONTROL_IPU_3_PARAMETERS_DC_VOLTAGE_SETPOINT,
								new FloatDoublewordElement(33008).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_IPU_3_PARAMETERS_DC_CURRENT_SETPOINT,
								new FloatDoublewordElement(33010).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_IPU_3_PARAMETERS_U0_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(33012).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_IPU_3_PARAMETERS_F0_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(33014).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_IPU_3_PARAMETERS_Q_REF_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(33016).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_IPU_3_PARAMETERS_P_REF_OFFSET_TO_CCU_VALUE,
								new FloatDoublewordElement(33018).wordOrder(WordOrder.LSWMSW)) //
				),
				new FC3ReadRegistersTask(33040, Priority.LOW,
						m(GridConChannelId.MIRROR_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_DC_VOLTAGE_SETPOINT,
								new FloatDoublewordElement(33040).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_A,
								new FloatDoublewordElement(33042).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_B,
								new FloatDoublewordElement(33044).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_WEIGHT_STRING_C,
								new FloatDoublewordElement(33046).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_I_REF_STRING_A,
								new FloatDoublewordElement(33048).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.MIRROR_CONTROL_IPU_4_DC_DC_CONVERTER_PARAMETERS_I_REF_STRING_B,
								new FloatDoublewordElement(33050).wordOrder(WordOrder.LSWMSW)) //
				));

		// Calculate Total Active Power
		FloatReadChannel ap1 = this.channel(GridConChannelId.IPU_1_STATUS_DC_LINK_ACTIVE_POWER);
		FloatReadChannel ap2 = this.channel(GridConChannelId.IPU_2_STATUS_DC_LINK_ACTIVE_POWER);
		FloatReadChannel ap3 = this.channel(GridConChannelId.IPU_3_STATUS_DC_LINK_ACTIVE_POWER);
		final Consumer<Value<Float>> calculateActivePower = ignoreValue -> {
			float ipu1 = ap1.getNextValue().orElse(0f);
			float ipu2 = ap2.getNextValue().orElse(0f);
			float ipu3 = ap3.getNextValue().orElse(0f);
			this.getActivePower().setNextValue((ipu1 + ipu2 + ipu3) * -1);
		};
		ap1.onSetNextValue(calculateActivePower);
		ap2.onSetNextValue(calculateActivePower);
		ap3.onSetNextValue(calculateActivePower);

		return protocol;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return defineModbusProtocol(0);
	}

	private void setOutputSyncDeviceBridge(boolean value) {
		BooleanWriteChannel outputSyncDeviceBridge = this.outputSyncDeviceBridgeComponent
				.channel(this.outputSyncDeviceBridge.getChannelId());
		this.setOutput(outputSyncDeviceBridge, value);
	}

	/**
	 * Helper function to switch an output if it was not switched before.
	 *
	 * @param value true to switch ON, false to switch ON
	 */
	private void setOutput(BooleanWriteChannel channel, boolean value) {
		Optional<Boolean> currentValueOpt = channel.value().asOptional();
		if (!currentValueOpt.isPresent() || currentValueOpt.get() != value) {
			log.info("Set output [" + channel.address() + "] " + (value ? "ON" : "OFF") + ".");
			try {
				channel.setNextWriteValue(value);
			} catch (OpenemsException e) {
				this.logError(this.log, "Unable to set output: [" + channel.address() + "] " + e.getMessage());
			}
		}
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable() {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(), //
				SymmetricEss.getModbusSlaveNatureTable(), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(), //
				ModbusSlaveNatureTable.of(GridconPCS.class, 300) //
						.build());
	}
}
