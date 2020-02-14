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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
//import io.openems.edge.ess.api.ManagedSymmetricEss;
//import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.mr.gridcon.enums.CCUState;
import io.openems.edge.ess.mr.gridcon.enums.GridConChannelId;
import io.openems.edge.ess.mr.gridcon.enums.InverterCount;
import io.openems.edge.ess.mr.gridcon.enums.Mode;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;
import io.openems.edge.ess.mr.gridcon.writewords.CcuControlParametersWord;
import io.openems.edge.ess.mr.gridcon.writewords.CommandControlWord;
import io.openems.edge.ess.mr.gridcon.writewords.DcdcControlCommandWord;
import io.openems.edge.ess.mr.gridcon.writewords.IpuInverterControlWord;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "MR.Gridcon", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
				property = { EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE //
		}) //
public class GridconPCSImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, GridconPCS, EventHandler {

	public static final float DC_LINK_VOLTAGE_SETPOINT = 800f;
	public static final float DC_LINK_VOLTAGE_TOLERANCE_VOLT = 20;

	public static final int MAX_POWER_PER_INVERTER = 42_000;

	public static final float ON_GRID_FREQUENCY_FACTOR = 1.035f;
	public static final float ON_GRID_VOLTAGE_FACTOR = 0.97f;

	private final Logger log = LoggerFactory.getLogger(GridconPCSImpl.class);

	private InverterCount inverterCount;
	private String modbus_id;

	CommandControlWord commandControlWord;
	IpuInverterControlWord controlWordIPU1;
	IpuInverterControlWord controlWordIPU2;
	IpuInverterControlWord controlWordIPU3;
	CcuControlParametersWord ccuControlParametersWord;
	DcdcControlCommandWord dcdcControlCommandWord;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager componentManager;

	public GridconPCSImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				GridConChannelId.values() //
		);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.inverterCount = config.inverterCount();
		this.modbus_id = config.modbus_id();

		// Call parent activate()
		super.activate(context, config.id(), config.alias(), config.enabled(), config.unit_id(), this.cm, "Modbus",
				config.modbus_id());

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public float getMaxApparentPower() {
		return inverterCount.getMaxApparentPower();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE:
			try {
				//nur ausführen wenn schon mal was gelesen wurde....
				if (getCcuState() == CCUState.UNDEFINED) {
					return;
				}
				if (this.commandControlWord != null ) {
					writeCommandWontrolWord();  
				}
				if (this.ccuControlParametersWord != null) {
					writeCcuControlParametersWord();
				}
				if (this.dcdcControlCommandWord != null) {
					writeDcDcControlCommandWord();
				}
				if (this.controlWordIPU1 != null) {
					writeIpuInverter1ControlCommand();
				}
				if (this.controlWordIPU2 != null) {
					writeIpuInverter2ControlCommand();
				}
				if (this.controlWordIPU3 != null) {
					writeIpuInverter3ControlCommand();
				}
				
			} catch (IllegalArgumentException | OpenemsNamedException e) {
				log.error(e.getMessage());
			}
			break;
		}
	}
	
	@Override
	public String debugLog() {
		CCUState state = ((EnumReadChannel) this.channel(GridConChannelId.CCU_STATE)).value().asEnum();
		int errorCount = (int) this.channel(GridConChannelId.CCU_ERROR_COUNT).value().get();
		return "Here is Gridcon component with ccu state: " + state + "; Error count: " + errorCount;
		// "SoC:" + this.getSoc().value().asString() //
//				+ "|L:" + this.getActivePower().value().asString() //
//				+ "|Allowed:"
//				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).value().asStringWithoutUnit() + ";"
//				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).value().asString() //
//				+ "|" + this.getGridMode().value().asOptionString();
	}

	public boolean isIdle() {
		return isStopped();
	}

	@Override
	public boolean isStopped() {
		return getCcuState() == CCUState.SYNC_TO_V || getCcuState() == CCUState.IDLE_CURRENTLY_NOT_WORKING;
	}
	
	@Override
	public boolean isRunning() {
		return getCcuState() == CCUState.COMPENSATOR;
	}

	@Override
	public boolean isError() {
		return getCcuState() == CCUState.ERROR;
	}

	private CCUState getCcuState() {
		CCUState state = ((EnumReadChannel) this.channel(GridConChannelId.CCU_STATE)).value().asEnum();
		return state;
	}

	@Override
	public void setPower(int activePower, int reactivePower) {
		float maxApparentPower = this.getMaxApparentPower();
		/*
		 * !! signum, MR calculates negative values as discharge, positive as charge.
		 * Gridcon sets the (dis)charge according to a percentage of the
		 * MAX_APPARENT_POWER. So 0.1 => 10% of max power. Values should never take
		 * values lower than -1 or higher than 1.
		 */
		float activePowerFactor = (-1) * activePower / maxApparentPower;
		float reactivePowerFactor = (-1) * reactivePower / maxApparentPower;

		this.getCommandControlWord().setParameterPref(activePowerFactor);
		this.getCommandControlWord().setParameterQref(reactivePowerFactor);
	}

	protected void writeCommandWontrolWord() throws IllegalArgumentException, OpenemsNamedException {		
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_PLAY, commandControlWord.isPlay());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_READY, commandControlWord.isReady());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ACKNOWLEDGE, commandControlWord.isAcknowledge());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_STOP, this.commandControlWord.isStop());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_BLACKSTART_APPROVAL,
				commandControlWord.isBlackstartApproval());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_SYNC_APPROVAL,
				commandControlWord.isSyncApproval());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ACTIVATE_SHORT_CIRCUIT_HANDLING,
				commandControlWord.isShortCircuitHandling());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_MODE_SELECTION,
				commandControlWord.getModeSelection().value);
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_TRIGGER_SIA, commandControlWord.isTriggerSia());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ACTIVATE_HARMONIC_COMPENSATION,
				commandControlWord.isHarmonicCompensation());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ID_1_SD_CARD_PARAMETER_SET,
				commandControlWord.isParameterSet1());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ID_2_SD_CARD_PARAMETER_SET,
				commandControlWord.isParameterSet2());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ID_3_SD_CARD_PARAMETER_SET,
				commandControlWord.isParameterSet3());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ID_4_SD_CARD_PARAMETER_SET,
				commandControlWord.isParameterSet4());

		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_1, //TODO rename channel names
				commandControlWord.isEnableIpu1());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_2,
				commandControlWord.isEnableIpu2());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_3,
				commandControlWord.isEnableIpu3());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_4,
				commandControlWord.isEnableIpu4());

		this.writeValueToChannel(GridConChannelId.COMMAND_ERROR_CODE_FEEDBACK,
				commandControlWord.getErrorCodeFeedback());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_U0, commandControlWord.getParameterU0());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_F0, commandControlWord.getParameterF0());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_Q_REF,
				commandControlWord.getParameterQref());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_P_REF,
				commandControlWord.getParameterPref());

		// TODO remove from there here only write control word
		int date = this.convertToInteger(this.generateDate());
		this.writeValueToChannel(GridConChannelId.COMMAND_TIME_SYNC_DATE, date);
		int time = this.convertToInteger(this.generateTime());
		this.writeValueToChannel(GridConChannelId.COMMAND_TIME_SYNC_TIME, time);
	}

	private <T> void writeValueToChannel(GridConChannelId channelId, T value)
			throws IllegalArgumentException, OpenemsNamedException {
		((WriteChannel<?>) channel(channelId)).setNextWriteValueFromObject(value);
	}

	private BitSet generateDate() {
		LocalDateTime time = LocalDateTime.now();
		byte dayOfWeek = (byte) time.getDayOfWeek().ordinal();
		byte day = (byte) time.getDayOfMonth();
		byte month = (byte) time.getMonth().getValue();
		byte year = (byte) (time.getYear() - 2000); // 0 == year 2000 in the protocol

		return BitSet.valueOf(new byte[] { day, dayOfWeek, year, month });
	}

	private BitSet generateTime() {
		LocalDateTime time = LocalDateTime.now();
		byte seconds = (byte) time.getSecond();
		byte minutes = (byte) time.getMinute();
		byte hours = (byte) time.getHour();

		// second byte is unused
		return BitSet.valueOf(new byte[] { seconds, 0, hours, minutes });
	}

	private int convertToInteger(BitSet bitSet) {
		long[] l = bitSet.toLongArray();
		if (l.length == 0) {
			return 0;
		}
		return (int) l[0];
	}

	protected void writeCcuControlParametersWord() throws IllegalArgumentException, OpenemsNamedException {
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_MAIN,
				ccuControlParametersWord.getuByQDroopMain());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN,
				ccuControlParametersWord.getuByQDroopT1Main());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_MAIN,
				ccuControlParametersWord.getfByPDroopMain());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_T1_MAIN,
				ccuControlParametersWord.getfByPDroopT1Main());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_Q_U_DROOP_MAIN,
				ccuControlParametersWord.getqByUDroopMain());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_Q_U_DEAD_BAND,
				ccuControlParametersWord.getqByUDeadBand());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_Q_LIMIT, ccuControlParametersWord.getqLimit());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_F_DROOP_MAIN,
				ccuControlParametersWord.getpByFDroopMain());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_F_DEAD_BAND,
				ccuControlParametersWord.getpByFDeadBand());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_U_DROOP, ccuControlParametersWord.getpByUDroop());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_U_DEAD_BAND,
				ccuControlParametersWord.getpByUDeadBand());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_U_MAX_CHARGE,
				ccuControlParametersWord.getpByUMaxCharge());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_U_MAX_DISCHARGE,
				ccuControlParametersWord.getpByUMaxDischarge());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_MODE,
				ccuControlParametersWord.getpControlMode().getFloatValue()); //
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_TWO,
				ccuControlParametersWord.getpControlLimTwo());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_ONE,
				ccuControlParametersWord.getpControlLimOne());
	}

	protected void writeIpuInverter1ControlCommand() throws IllegalArgumentException, OpenemsNamedException {
		writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_DC_VOLTAGE_SETPOINT,
				controlWordIPU1.getDcVoltageSetpoint());
		writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_DC_CURRENT_SETPOINT,
				controlWordIPU1.getDcCurrentSetpoint());
		writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_U0_OFFSET_TO_CCU_VALUE,
				controlWordIPU1.getU0OffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_F0_OFFSET_TO_CCU_VALUE,
				controlWordIPU1.getF0OffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
				controlWordIPU1.getqRefOffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
				controlWordIPU1.getpRefOffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_P_MAX_DISCHARGE, controlWordIPU1.getpMaxDischarge());
		writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_P_MAX_CHARGE, controlWordIPU1.getpMaxCharge());
	}

	protected void writeIpuInverter2ControlCommand() throws IllegalArgumentException, OpenemsNamedException {
		writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_DC_VOLTAGE_SETPOINT,
				controlWordIPU2.getDcVoltageSetpoint());
		writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_DC_CURRENT_SETPOINT,
				controlWordIPU2.getDcCurrentSetpoint());
		writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_U0_OFFSET_TO_CCU_VALUE,
				controlWordIPU2.getU0OffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_F0_OFFSET_TO_CCU_VALUE,
				controlWordIPU2.getF0OffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
				controlWordIPU2.getqRefOffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
				controlWordIPU2.getpRefOffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_P_MAX_DISCHARGE, controlWordIPU2.getpMaxDischarge());
		writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_P_MAX_CHARGE, controlWordIPU2.getpMaxCharge());
	}

	protected void writeIpuInverter3ControlCommand() throws IllegalArgumentException, OpenemsNamedException {
		writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_DC_VOLTAGE_SETPOINT,
				controlWordIPU3.getDcVoltageSetpoint());
		writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_DC_CURRENT_SETPOINT,
				controlWordIPU3.getDcCurrentSetpoint());
		writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_U0_OFFSET_TO_CCU_VALUE,
				controlWordIPU3.getU0OffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_F0_OFFSET_TO_CCU_VALUE,
				controlWordIPU3.getF0OffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
				controlWordIPU3.getqRefOffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
				controlWordIPU3.getpRefOffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_P_MAX_DISCHARGE, controlWordIPU3.getpMaxDischarge());
		writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_P_MAX_CHARGE, controlWordIPU3.getpMaxCharge());
	}

	protected void writeDcDcControlCommandWord() throws IllegalArgumentException, OpenemsNamedException {
//weighting is never allowed to be '0'
		if (dcdcControlCommandWord.getStringControlMode() == 0) {
			throw new OpenemsException("Calculated weight of '0' -> not allowed!");
		}

		writeValueToChannel(GridConChannelId.DCDC_CONTROL_DC_VOLTAGE_SETPOINT, dcdcControlCommandWord.getDcVoltageSetpoint()); //
		writeValueToChannel(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, dcdcControlCommandWord.getWeightStringA()); //
		writeValueToChannel(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, dcdcControlCommandWord.getWeightStringB()); //
		writeValueToChannel(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, dcdcControlCommandWord.getWeightStringC()); //
		writeValueToChannel(GridConChannelId.DCDC_CONTROL_I_REF_STRING_A, dcdcControlCommandWord.getiRefStringA()); //
		writeValueToChannel(GridConChannelId.DCDC_CONTROL_I_REF_STRING_B, dcdcControlCommandWord.getiRefStringB()); //
		writeValueToChannel(GridConChannelId.DCDC_CONTROL_I_REF_STRING_C, dcdcControlCommandWord.getiRefStringC()); //
		writeValueToChannel(GridConChannelId.DCDC_CONTROL_STRING_CONTROL_MODE, dcdcControlCommandWord.getStringControlMode()); //
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		int inverterCount = this.inverterCount.getCount();

		ModbusProtocol result = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(32528, Priority.HIGH, //
						m(GridConChannelId.CCU_STATE, new UnsignedWordElement(32528)), //
						m(GridConChannelId.CCU_ERROR_COUNT,
								new UnsignedWordElement(32529).byteOrder(ByteOrder.LITTLE_ENDIAN)), //
						m(GridConChannelId.CCU_ERROR_CODE,
								new UnsignedDoublewordElement(32530).wordOrder(WordOrder.LSWMSW)), //
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
						m(GridConChannelId.CCU_POWER_P, new FloatDoublewordElement(32544).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CCU_POWER_Q, new FloatDoublewordElement(32546).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CCU_FREQUENCY, new FloatDoublewordElement(32548).wordOrder(WordOrder.LSWMSW)) //
				),
				/*
				 * Commands
				 */
				new FC16WriteRegistersTask(32560, //
						m(new BitsWordElement(32560, this) //
								.bit(0, GridConChannelId.COMMAND_CONTROL_WORD_STOP) //
								.bit(1, GridConChannelId.COMMAND_CONTROL_WORD_PLAY) //
								.bit(2, GridConChannelId.COMMAND_CONTROL_WORD_READY) //
								.bit(3, GridConChannelId.COMMAND_CONTROL_WORD_ACKNOWLEDGE) //

								.bit(4, GridConChannelId.COMMAND_CONTROL_WORD_BLACKSTART_APPROVAL) //
								.bit(5, GridConChannelId.COMMAND_CONTROL_WORD_SYNC_APPROVAL) //
								.bit(6, GridConChannelId.COMMAND_CONTROL_WORD_ACTIVATE_SHORT_CIRCUIT_HANDLING) //
								.bit(7, GridConChannelId.COMMAND_CONTROL_WORD_MODE_SELECTION) //

								.bit(8, GridConChannelId.COMMAND_CONTROL_WORD_TRIGGER_SIA) //
								.bit(9, GridConChannelId.COMMAND_CONTROL_WORD_ACTIVATE_HARMONIC_COMPENSATION) //
								.bit(10, GridConChannelId.COMMAND_CONTROL_WORD_ID_1_SD_CARD_PARAMETER_SET) //
								.bit(11, GridConChannelId.COMMAND_CONTROL_WORD_ID_2_SD_CARD_PARAMETER_SET) //

								.bit(12, GridConChannelId.COMMAND_CONTROL_WORD_ID_3_SD_CARD_PARAMETER_SET) //
								.bit(13, GridConChannelId.COMMAND_CONTROL_WORD_ID_4_SD_CARD_PARAMETER_SET) //
						), //
						m(new BitsWordElement(32561, this) //
								.bit(12, GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_4) //
								.bit(13, GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_3) //
								.bit(14, GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_2) //
								.bit(15, GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_1) //
						), //
						m(GridConChannelId.COMMAND_ERROR_CODE_FEEDBACK,
								new UnsignedDoublewordElement(32562).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_U0,
								new FloatDoublewordElement(32564).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_F0,
								new FloatDoublewordElement(32566).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_Q_REF,
								new FloatDoublewordElement(32568).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_P_REF,
								new FloatDoublewordElement(32570).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_TIME_SYNC_DATE,
								new UnsignedDoublewordElement(32572).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_TIME_SYNC_TIME,
								new UnsignedDoublewordElement(32574).wordOrder(WordOrder.LSWMSW)) //
				),
				/*
				 * Commands Mirror
				 */
				new FC3ReadRegistersTask(32880, Priority.LOW, //
						m(new BitsWordElement(32880, this) //
								.bit(12, GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_4) //
								.bit(13, GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_3) //
								.bit(14, GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_2) //
								.bit(15, GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_1) //
						), //
						m(new BitsWordElement(32881, this) //
								.bit(0, GridConChannelId.COMMAND_CONTROL_WORD_STOP) //
								.bit(1, GridConChannelId.COMMAND_CONTROL_WORD_PLAY) //
								.bit(2, GridConChannelId.COMMAND_CONTROL_WORD_READY) //
								.bit(3, GridConChannelId.COMMAND_CONTROL_WORD_ACKNOWLEDGE) //
								.bit(4, GridConChannelId.COMMAND_CONTROL_WORD_BLACKSTART_APPROVAL) //
								.bit(5, GridConChannelId.COMMAND_CONTROL_WORD_SYNC_APPROVAL) //
								.bit(6, GridConChannelId.COMMAND_CONTROL_WORD_ACTIVATE_SHORT_CIRCUIT_HANDLING) //
								.bit(7, GridConChannelId.COMMAND_CONTROL_WORD_MODE_SELECTION) //
								.bit(8, GridConChannelId.COMMAND_CONTROL_WORD_TRIGGER_SIA) //
								.bit(9, GridConChannelId.COMMAND_CONTROL_WORD_ACTIVATE_HARMONIC_COMPENSATION) //
								.bit(10, GridConChannelId.COMMAND_CONTROL_WORD_ID_1_SD_CARD_PARAMETER_SET) //
								.bit(11, GridConChannelId.COMMAND_CONTROL_WORD_ID_2_SD_CARD_PARAMETER_SET) //
								.bit(12, GridConChannelId.COMMAND_CONTROL_WORD_ID_3_SD_CARD_PARAMETER_SET) //
								.bit(13, GridConChannelId.COMMAND_CONTROL_WORD_ID_4_SD_CARD_PARAMETER_SET) //
						), //
						m(GridConChannelId.COMMAND_ERROR_CODE_FEEDBACK,
								new UnsignedDoublewordElement(32882).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_U0,
								new FloatDoublewordElement(32884).wordOrder(WordOrder.LSWMSW)),
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_F0,
								new FloatDoublewordElement(32886).wordOrder(WordOrder.LSWMSW)),
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_Q_REF,
								new FloatDoublewordElement(32888).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_P_REF,
								new FloatDoublewordElement(32890).wordOrder(WordOrder.LSWMSW)) //
				),
				/*
				 * Control Parameters
				 */
				new FC16WriteRegistersTask(32592, //
						m(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_MAIN,
								new FloatDoublewordElement(32592).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN,
								new FloatDoublewordElement(32594).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_MAIN,
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
				),
				/*
				 * Control Parameters Mirror
				 */
				new FC3ReadRegistersTask(32912, Priority.LOW,
						m(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_MAIN,
								new FloatDoublewordElement(32912).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN,
								new FloatDoublewordElement(32914).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_MAIN,
								new FloatDoublewordElement(32916).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_T1_MAIN,
								new FloatDoublewordElement(32918).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_U_DROOP_MAIN,
								new FloatDoublewordElement(32920).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_U_DEAD_BAND,
								new FloatDoublewordElement(32922).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_LIMIT,
								new FloatDoublewordElement(32924).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_F_DROOP_MAIN,
								new FloatDoublewordElement(32926).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_F_DEAD_BAND,
								new FloatDoublewordElement(32928).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_DROOP,
								new FloatDoublewordElement(32930).wordOrder(WordOrder.LSWMSW)) //
				));

		if (inverterCount > 0) {
			/*
			 * At least 1 Inverter -> Add IPU 1
			 */
			result.addTasks(//
					/*
					 * IPU 1 State
					 */
					new FC3ReadRegistersTask(33168, Priority.LOW, //
							m(GridConChannelId.INVERTER_1_STATUS_STATE_MACHINE, new UnsignedWordElement(33168)), //
							m(GridConChannelId.INVERTER_1_STATUS_MCU, new UnsignedWordElement(33169)), //
							m(GridConChannelId.INVERTER_1_STATUS_FILTER_CURRENT,
									new FloatDoublewordElement(33170).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_DC_LINK_POSITIVE_VOLTAGE,
									new FloatDoublewordElement(33172).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_DC_LINK_NEGATIVE_VOLTAGE,
									new FloatDoublewordElement(33174).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_DC_LINK_CURRENT,
									new FloatDoublewordElement(33176).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_DC_LINK_ACTIVE_POWER,
									new FloatDoublewordElement(33178).wordOrder(WordOrder.LSWMSW),
									ElementToChannelConverter.INVERT), //
							m(GridConChannelId.INVERTER_1_STATUS_DC_LINK_UTILIZATION,
									new FloatDoublewordElement(33180).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_FAN_SPEED_MAX,
									new UnsignedDoublewordElement(33182).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_FAN_SPEED_MIN,
									new UnsignedDoublewordElement(33184).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_TEMPERATURE_IGBT_MAX,
									new FloatDoublewordElement(33186).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_TEMPERATURE_MCU_BOARD,
									new FloatDoublewordElement(33188).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_TEMPERATURE_GRID_CHOKE,
									new FloatDoublewordElement(33190).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_TEMPERATURE_INVERTER_CHOKE,
									new FloatDoublewordElement(33192).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_RESERVE_1,
									new FloatDoublewordElement(33194).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_RESERVE_2,
									new FloatDoublewordElement(33196).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_STATUS_RESERVE_3,
									new FloatDoublewordElement(33198).wordOrder(WordOrder.LSWMSW)) //
					),
					/*
					 * IPU 1 Control Parameters
					 */
					new FC16WriteRegistersTask(32624, //
							m(GridConChannelId.INVERTER_1_CONTROL_DC_VOLTAGE_SETPOINT,
									new FloatDoublewordElement(32624).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_DC_CURRENT_SETPOINT,
									new FloatDoublewordElement(32626).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_U0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32628).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_F0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32630).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32632).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32634).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_P_MAX_DISCHARGE,
									new FloatDoublewordElement(32636).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_P_MAX_CHARGE,
									new FloatDoublewordElement(32638).wordOrder(WordOrder.LSWMSW)) //
					),
					/*
					 * IPU 1 Mirror Control
					 */
					new FC3ReadRegistersTask(32944, Priority.LOW,
							m(GridConChannelId.INVERTER_1_CONTROL_DC_VOLTAGE_SETPOINT,
									new FloatDoublewordElement(32944).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_DC_CURRENT_SETPOINT,
									new FloatDoublewordElement(32946).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_U0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32948).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_F0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32950).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32952).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_1_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32954).wordOrder(WordOrder.LSWMSW)) //
					));
		}

		if (inverterCount > 1) {
			/*
			 * At least 2 Inverters -> Add IPU 2
			 */
			result.addTasks(//
					/*
					 * IPU 2 State
					 */
					new FC3ReadRegistersTask(33200, Priority.LOW, //
							m(GridConChannelId.INVERTER_2_STATUS_STATE_MACHINE, new UnsignedWordElement(33200)), //
							m(GridConChannelId.INVERTER_2_STATUS_MCU, new UnsignedWordElement(33201)), //
							m(GridConChannelId.INVERTER_2_STATUS_FILTER_CURRENT,
									new FloatDoublewordElement(33202).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_DC_LINK_POSITIVE_VOLTAGE,
									new FloatDoublewordElement(33204).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_DC_LINK_NEGATIVE_VOLTAGE,
									new FloatDoublewordElement(33206).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_DC_LINK_CURRENT,
									new FloatDoublewordElement(33208).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_DC_LINK_ACTIVE_POWER,
									new FloatDoublewordElement(33210).wordOrder(WordOrder.LSWMSW),
									ElementToChannelConverter.INVERT), //
							m(GridConChannelId.INVERTER_2_STATUS_DC_LINK_UTILIZATION,
									new FloatDoublewordElement(33212).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_FAN_SPEED_MAX,
									new UnsignedDoublewordElement(33214).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_FAN_SPEED_MIN,
									new UnsignedDoublewordElement(33216).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_TEMPERATURE_IGBT_MAX,
									new FloatDoublewordElement(33218).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_TEMPERATURE_MCU_BOARD,
									new FloatDoublewordElement(33220).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_TEMPERATURE_GRID_CHOKE,
									new FloatDoublewordElement(33222).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_TEMPERATURE_INVERTER_CHOKE,
									new FloatDoublewordElement(33224).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_RESERVE_1,
									new FloatDoublewordElement(33226).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_RESERVE_2,
									new FloatDoublewordElement(33228).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_STATUS_RESERVE_3,
									new FloatDoublewordElement(33230).wordOrder(WordOrder.LSWMSW)) //
					),
					/*
					 * IPU 2 Control Parameters
					 */
					new FC16WriteRegistersTask(32656, //
							m(GridConChannelId.INVERTER_2_CONTROL_DC_VOLTAGE_SETPOINT,
									new FloatDoublewordElement(32656).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_DC_CURRENT_SETPOINT,
									new FloatDoublewordElement(32658).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_U0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32660).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_F0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32662).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32664).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32666).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_P_MAX_DISCHARGE,
									new FloatDoublewordElement(32668).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_P_MAX_CHARGE,
									new FloatDoublewordElement(32670).wordOrder(WordOrder.LSWMSW)) //
					),
					/*
					 * IPU 2 Mirror Control
					 */
					new FC3ReadRegistersTask(32976, Priority.LOW,
							m(GridConChannelId.INVERTER_2_CONTROL_DC_VOLTAGE_SETPOINT,
									new FloatDoublewordElement(32976).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_DC_CURRENT_SETPOINT,
									new FloatDoublewordElement(32978).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_U0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32980).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_F0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32982).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32984).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_2_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32986).wordOrder(WordOrder.LSWMSW)) //
					));
		}
		if (inverterCount > 2) {
			/*
			 * 3 Inverters -> Add IPU 3
			 */
			result.addTasks(//
					/*
					 * IPU 3 State
					 */
					new FC3ReadRegistersTask(33232, Priority.LOW, //
							m(GridConChannelId.INVERTER_3_STATUS_STATE_MACHINE, new UnsignedWordElement(33232)), //
							m(GridConChannelId.INVERTER_3_STATUS_MCU, new UnsignedWordElement(33233)), //
							m(GridConChannelId.INVERTER_3_STATUS_FILTER_CURRENT,
									new FloatDoublewordElement(33234).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_DC_LINK_POSITIVE_VOLTAGE,
									new FloatDoublewordElement(33236).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_DC_LINK_NEGATIVE_VOLTAGE,
									new FloatDoublewordElement(33238).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_DC_LINK_CURRENT,
									new FloatDoublewordElement(33240).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_DC_LINK_ACTIVE_POWER,
									new FloatDoublewordElement(33242).wordOrder(WordOrder.LSWMSW),
									ElementToChannelConverter.INVERT), //
							m(GridConChannelId.INVERTER_3_STATUS_DC_LINK_UTILIZATION,
									new FloatDoublewordElement(33244).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_FAN_SPEED_MAX,
									new UnsignedDoublewordElement(33246).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_FAN_SPEED_MIN,
									new UnsignedDoublewordElement(33248).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_TEMPERATURE_IGBT_MAX,
									new FloatDoublewordElement(33250).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_TEMPERATURE_MCU_BOARD,
									new FloatDoublewordElement(33252).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_TEMPERATURE_GRID_CHOKE,
									new FloatDoublewordElement(33254).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_TEMPERATURE_INVERTER_CHOKE,
									new FloatDoublewordElement(33256).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_RESERVE_1,
									new FloatDoublewordElement(33258).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_RESERVE_2,
									new FloatDoublewordElement(33260).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_STATUS_RESERVE_3,
									new FloatDoublewordElement(33262).wordOrder(WordOrder.LSWMSW)) //
					),
					/*
					 * IPU 3 Control Parameters
					 */
					new FC16WriteRegistersTask(32688, //
							m(GridConChannelId.INVERTER_3_CONTROL_DC_VOLTAGE_SETPOINT,
									new FloatDoublewordElement(32688).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_DC_CURRENT_SETPOINT,
									new FloatDoublewordElement(32690).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_U0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32692).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_F0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32694).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32696).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(32698).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_P_MAX_DISCHARGE,
									new FloatDoublewordElement(32700).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_P_MAX_CHARGE,
									new FloatDoublewordElement(32702).wordOrder(WordOrder.LSWMSW)) //
					),
					/*
					 * IPU 3 Mirror Control
					 */
					new FC3ReadRegistersTask(33008, Priority.LOW,
							m(GridConChannelId.INVERTER_3_CONTROL_DC_VOLTAGE_SETPOINT,
									new FloatDoublewordElement(33008).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_DC_CURRENT_SETPOINT,
									new FloatDoublewordElement(33010).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_U0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(33012).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_F0_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(33014).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(33016).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.INVERTER_3_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
									new FloatDoublewordElement(33018).wordOrder(WordOrder.LSWMSW)) //
					));
		}

		{
			/*
			 * DCDC
			 * 
			 * if one inverter is used, dc dc converter is ipu2 ...
			 */
			int startAddressIpuControl = 32720; // == THREE
			int startAddressIpuControlMirror = 33040; // == THREE
			int startAddressIpuState = 33264; // == THREE

			int startAddressIpuDcdc = 33584; // == THREE
			switch (this.inverterCount) {
			case ONE:
				startAddressIpuControl = 32656;
				startAddressIpuControlMirror = 32976;
				startAddressIpuState = 33200;
				startAddressIpuDcdc = 33520;
				break;
			case TWO:
				startAddressIpuControl = 32688;
				startAddressIpuControlMirror = 33008;
				startAddressIpuState = 33232;
				startAddressIpuDcdc = 33552;
				break;
			case THREE:
				// default
				break;
			}

			result.addTasks(//
					/*
					 * DCDC Control
					 */
					new FC16WriteRegistersTask(startAddressIpuControl, //
							m(GridConChannelId.DCDC_CONTROL_DC_VOLTAGE_SETPOINT,
									new FloatDoublewordElement(startAddressIpuControl).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A,
									new FloatDoublewordElement(startAddressIpuControl + 2).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B,
									new FloatDoublewordElement(startAddressIpuControl + 4).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C,
									new FloatDoublewordElement(startAddressIpuControl + 6).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_I_REF_STRING_A,
									new FloatDoublewordElement(startAddressIpuControl + 8).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_I_REF_STRING_B,
									new FloatDoublewordElement(startAddressIpuControl + 10)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_I_REF_STRING_C,
									new FloatDoublewordElement(startAddressIpuControl + 12)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_STRING_CONTROL_MODE,
									new FloatDoublewordElement(startAddressIpuControl + 14).wordOrder(WordOrder.LSWMSW)) //
					),
					/*
					 * DCDC Control Mirror
					 */
					new FC3ReadRegistersTask(startAddressIpuControlMirror, Priority.LOW,
							m(GridConChannelId.DCDC_CONTROL_DC_VOLTAGE_SETPOINT,
									new FloatDoublewordElement(startAddressIpuControlMirror)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A,
									new FloatDoublewordElement(startAddressIpuControlMirror + 2)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B,
									new FloatDoublewordElement(startAddressIpuControlMirror + 4)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C,
									new FloatDoublewordElement(startAddressIpuControlMirror + 6)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_I_REF_STRING_A,
									new FloatDoublewordElement(startAddressIpuControlMirror + 8)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_I_REF_STRING_B,
									new FloatDoublewordElement(startAddressIpuControlMirror + 10)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_I_REF_STRING_C,
									new FloatDoublewordElement(startAddressIpuControlMirror + 12)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_CONTROL_STRING_CONTROL_MODE,
									new FloatDoublewordElement(startAddressIpuControlMirror + 14)
											.wordOrder(WordOrder.LSWMSW)) //
					),
					/*
					 * DCDC State
					 */
					new FC3ReadRegistersTask(startAddressIpuState, Priority.LOW, // // IPU 4 state
							m(GridConChannelId.DCDC_STATUS_STATE_MACHINE,
									new UnsignedWordElement(startAddressIpuState)), //
							m(GridConChannelId.DCDC_STATUS_MCU, new UnsignedWordElement(startAddressIpuState + 1)), //
							m(GridConChannelId.DCDC_STATUS_FILTER_CURRENT,
									new FloatDoublewordElement(startAddressIpuState + 2).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_DC_LINK_POSITIVE_VOLTAGE,
									new FloatDoublewordElement(startAddressIpuState + 4).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_DC_LINK_NEGATIVE_VOLTAGE,
									new FloatDoublewordElement(startAddressIpuState + 6).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_DC_LINK_CURRENT,
									new FloatDoublewordElement(startAddressIpuState + 8).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_DC_LINK_ACTIVE_POWER,
									new FloatDoublewordElement(startAddressIpuState + 10).wordOrder(WordOrder.LSWMSW),
									ElementToChannelConverter.INVERT), //
							m(GridConChannelId.DCDC_STATUS_DC_LINK_UTILIZATION,
									new FloatDoublewordElement(startAddressIpuState + 12).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_FAN_SPEED_MAX,
									new UnsignedDoublewordElement(startAddressIpuState + 14)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_FAN_SPEED_MIN,
									new UnsignedDoublewordElement(startAddressIpuState + 16)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_TEMPERATURE_IGBT_MAX,
									new FloatDoublewordElement(startAddressIpuState + 18).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_TEMPERATURE_MCU_BOARD,
									new FloatDoublewordElement(startAddressIpuState + 20).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_TEMPERATURE_GRID_CHOKE,
									new FloatDoublewordElement(startAddressIpuState + 22).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_TEMPERATURE_INVERTER_CHOKE,
									new FloatDoublewordElement(startAddressIpuState + 24).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_RESERVE_1,
									new FloatDoublewordElement(startAddressIpuState + 26).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_RESERVE_2,
									new FloatDoublewordElement(startAddressIpuState + 28).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_RESERVE_3,
									new FloatDoublewordElement(startAddressIpuState + 30).wordOrder(WordOrder.LSWMSW)) //
					),
					/*
					 * DCDC Measurements
					 */
					new FC3ReadRegistersTask(startAddressIpuDcdc, Priority.LOW, // IPU 4 measurements
							m(GridConChannelId.DCDC_MEASUREMENTS_VOLTAGE_STRING_A,
									new FloatDoublewordElement(startAddressIpuDcdc).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_VOLTAGE_STRING_B,
									new FloatDoublewordElement(startAddressIpuDcdc + 2).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_VOLTAGE_STRING_C,
									new FloatDoublewordElement(startAddressIpuDcdc + 4).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_CURRENT_STRING_A,
									new FloatDoublewordElement(startAddressIpuDcdc + 6).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_CURRENT_STRING_B,
									new FloatDoublewordElement(startAddressIpuDcdc + 8).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_CURRENT_STRING_C,
									new FloatDoublewordElement(startAddressIpuDcdc + 10).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_POWER_STRING_A,
									new FloatDoublewordElement(startAddressIpuDcdc + 12).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_POWER_STRING_B,
									new FloatDoublewordElement(startAddressIpuDcdc + 14).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_POWER_STRING_C,
									new FloatDoublewordElement(startAddressIpuDcdc + 16).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_UTILIZATION_STRING_A,
									new FloatDoublewordElement(startAddressIpuDcdc + 18).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_UTILIZATION_STRING_B,
									new FloatDoublewordElement(startAddressIpuDcdc + 20).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_UTILIZATION_STRING_C,
									new FloatDoublewordElement(startAddressIpuDcdc + 22).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_ACCUMULATED_SUM_DC_CURRENT,
									new FloatDoublewordElement(startAddressIpuDcdc + 24).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_ACCUMULATED_DC_UTILIZATION,
									new FloatDoublewordElement(startAddressIpuDcdc + 26).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_RESERVE_1,
									new FloatDoublewordElement(startAddressIpuDcdc + 28).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_RESERVE_2,
									new FloatDoublewordElement(startAddressIpuDcdc + 30).wordOrder(WordOrder.LSWMSW)) //
					));
		}
		return result;
	}

	@Override
	public void stop() {
		this.getCommandControlWord().setStop(true);
	}

	@Override
	public void acknowledgeErrors() {
		this.getCommandControlWord().setAcknowledge(true);
	}

	@Override
	public void setErrorCodeFeedback(int errorCodeFeedback) {
		this.getCommandControlWord().setErrorCodeFeedback(errorCodeFeedback);
	}

	@Override
	public int getErrorCode() {
		return getInteger(GridConChannelId.CCU_ERROR_CODE);		
	}

	@Override
	public float getActivePowerInverter1() {
		return getFloat(GridConChannelId.INVERTER_1_STATUS_DC_LINK_ACTIVE_POWER);
	}

	@Override
	public float getActivePowerInverter2() {
		return getFloat(GridConChannelId.INVERTER_2_STATUS_DC_LINK_ACTIVE_POWER);
	}

	@Override
	public float getActivePowerInverter3() {
		return getFloat(GridConChannelId.INVERTER_3_STATUS_DC_LINK_ACTIVE_POWER);
	}

	@Override
	public float getDcLinkPositiveVoltage() {
		return getFloat(GridConChannelId.DCDC_STATUS_DC_LINK_POSITIVE_VOLTAGE);
	}

	private float getFloat(GridConChannelId id) {
		FloatReadChannel c = this.channel(id);
		return c.value().orElse(0f);
	}
	
	private int getInteger(GridConChannelId id) {
		IntegerReadChannel c = this.channel(id);
		return c.value().orElse(Integer.MIN_VALUE);
	}

	@Override
	public boolean isCommunicationBroken() {

		String modbusId = this.modbus_id;
		ComponentManager manager = this.componentManager;
		AbstractModbusBridge modbusBridge = null;
		try {
			modbusBridge = manager.getComponent(modbusId);
		} catch (OpenemsNamedException e) {
			log.debug("Cannot get modbus component");
		}
		if (modbusBridge == null) {
			return true;
		}

		Channel<Boolean> slaveCommunicationFailedChannel = modbusBridge.getSlaveCommunicationFailedChannel();
		Optional<Boolean> communicationFailedOpt = slaveCommunicationFailedChannel.value().asOptional();

		// If the channel value is present and it is set then the communication is
		// broken
		if (communicationFailedOpt.isPresent() && communicationFailedOpt.get()) {
			return true;
		}

		return false;
	}

	@Override
	public void setEnableIPU1(boolean enabled) {
		getCommandControlWord().setEnableIpu1(enabled);
	}

	@Override
	public void setEnableIPU2(boolean enabled) {
		getCommandControlWord().setEnableIpu2(enabled);
	}

	@Override
	public void setEnableIPU3(boolean enabled) {
		getCommandControlWord().setEnableIpu3(enabled);
	}

	@Override
	public void setEnableIPU4(boolean enabled) {
		getCommandControlWord().setEnableIpu4(enabled);
	}
	
	@Override
	public void setParameterSet(ParameterSet set) {
		boolean set1 = false;
		boolean set2 = false;
		boolean set3 = false;
		boolean set4 = false;
		switch (set) {
		case SET_1:
			set1 = true;
			break;
		case SET_2:
			set2 = true;
			break;
		case SET_3:
			set3 = true;
			break;
		case SET_4:
			set4 = true;
			break;
		}

		getCommandControlWord().setParameterSet1(set1);
		getCommandControlWord().setParameterSet2(set2);
		getCommandControlWord().setParameterSet3(set3);
		getCommandControlWord().setParameterSet4(set4);
	}

	@Override
	public void setModeSelection(Mode modeSelection) {
		getCommandControlWord().setModeSelection(modeSelection);
	}

	@Override
	public void setPlay(boolean b) {
		getCommandControlWord().setPlay(b);
	}

	@Override
	public void setSyncApproval(boolean b) {
		getCommandControlWord().setSyncApproval(b);
	}

	@Override
	public void setBlackStartApproval(boolean b) {
		getCommandControlWord().setBlackstartApproval(b);
	}

	@Override
	public void setShortCircuitHAndling(boolean b) {
		getCommandControlWord().setShortCircuitHandling(b);
	}

	@Override
	public void setU0(float onGridVoltageFactor) {
		getCommandControlWord().setParameterU0(onGridVoltageFactor);
	}

	@Override
	public void setF0(float onGridFrequencyFactor) {
		getCommandControlWord().setParameterF0(onGridFrequencyFactor);
	}

	@Override
	public void setPControlMode(PControlMode pControlMode) {
		getCcuControlParametersWord().setpControlMode(pControlMode);
	}

	@Override
	public void setQLimit(float qLimit) {
		getCcuControlParametersWord().setqLimit(qLimit);
	}

	@Override
	public void setPMaxChargeIPU1(float maxPower) {
		getControlWordIPU1().setpMaxCharge(maxPower);
	}

	@Override
	public void setPMaxDischargeIPU1(float maxPower) {
		getControlWordIPU1().setpMaxDischarge(maxPower);
	}

	@Override
	public void setPMaxChargeIPU2(float maxPower) {
		getControlWordIPU2().setpMaxCharge(maxPower);
	}

	@Override
	public void setPMaxDischargeIPU2(float maxPower) {
		getControlWordIPU2().setpMaxDischarge(maxPower);
	}

	@Override
	public void setPMaxChargeIPU3(float maxPower) {
		getControlWordIPU3().setpMaxCharge(maxPower);
	}

	@Override
	public void setPMaxDischargeIPU3(float maxPower) {
		getControlWordIPU3().setpMaxDischarge(maxPower);
	}

	@Override
	public void setDcLinkVoltage(float dcLinkVoltageSetpoint) {
		getDcdcControlCommandWord().setDcVoltageSetpoint(dcLinkVoltageSetpoint);
	}

	@Override
	public void setWeightStringA(Float weight) {
		getDcdcControlCommandWord().setWeightStringA(weight);
	}

	@Override
	public void setWeightStringB(Float weight) {
		getDcdcControlCommandWord().setWeightStringB(weight);
	}

	@Override
	public void setWeightStringC(Float weight) {
		getDcdcControlCommandWord().setWeightStringC(weight);
	}

	@Override
	public void setStringControlMode(int stringControlMode) {
		getDcdcControlCommandWord().setStringControlMode(stringControlMode);
	}

	@Override
	public void enableDCDC() {
		switch (inverterCount) {
		case ONE:
			getCommandControlWord().setEnableIpu2(true);
			break;
		case TWO:
			getCommandControlWord().setEnableIpu3(true);
			break;
		case THREE:
			getCommandControlWord().setEnableIpu4(true);
			break;
		}
	}
	
	private CommandControlWord getCommandControlWord() {
		if (this.commandControlWord == null) {
			this.commandControlWord = new CommandControlWord();
		}
		return this.commandControlWord;
	}
	
	private CcuControlParametersWord getCcuControlParametersWord() {
		if (this.ccuControlParametersWord == null) {
			this.ccuControlParametersWord = new CcuControlParametersWord();
		}
		return this.ccuControlParametersWord;
	}
	
	private IpuInverterControlWord getControlWordIPU1() {
		if (this.controlWordIPU1 == null) {
			this.controlWordIPU1 = new IpuInverterControlWord();
		}
		return this.controlWordIPU1;
	}
	
	private IpuInverterControlWord getControlWordIPU2() {
		if (this.controlWordIPU2 == null) {
			this.controlWordIPU2 = new IpuInverterControlWord();
		}
		return this.controlWordIPU2;
	}
	
	private IpuInverterControlWord getControlWordIPU3() {
		if (this.controlWordIPU3 == null) {
			this.controlWordIPU3 = new IpuInverterControlWord();
		}
		return this.controlWordIPU3;
	}
	
	private DcdcControlCommandWord getDcdcControlCommandWord() {
		if (this.dcdcControlCommandWord == null) {
			this.dcdcControlCommandWord = new DcdcControlCommandWord();
		}
		return this.dcdcControlCommandWord;
	}

	@Override
	public Integer getErrorCount() {
		return getInteger(GridConChannelId.CCU_ERROR_COUNT);
	}

}
