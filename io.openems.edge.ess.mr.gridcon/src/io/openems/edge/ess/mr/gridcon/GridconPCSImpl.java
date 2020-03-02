package io.openems.edge.ess.mr.gridcon;

import java.nio.ByteOrder;
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
import io.openems.edge.ess.mr.gridcon.enums.CCUState;
import io.openems.edge.ess.mr.gridcon.enums.GridConChannelId;
import io.openems.edge.ess.mr.gridcon.enums.InverterCount;
import io.openems.edge.ess.mr.gridcon.enums.Mode;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;
import io.openems.edge.ess.mr.gridcon.enums.StatusIPUStateMachine;
import io.openems.edge.ess.mr.gridcon.writewords.CcuParameters1;
import io.openems.edge.ess.mr.gridcon.writewords.CcuParameters2;
import io.openems.edge.ess.mr.gridcon.writewords.Commands;
import io.openems.edge.ess.mr.gridcon.writewords.DcDcParameter;
import io.openems.edge.ess.mr.gridcon.writewords.IpuParameter;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "MR.Gridcon", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
				property = { EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE //
		}) //
public class GridconPCSImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, GridconPCS, EventHandler {

	public static final float DC_LINK_VOLTAGE_TOLERANCE_VOLT = 20;

	private final Logger log = LoggerFactory.getLogger(GridconPCSImpl.class);

	private String modbus_id;
	

	Commands commands;
	IpuParameter controlWordIPU1;
	IpuParameter controlWordIPU2;
	IpuParameter controlWordIPU3;
	CcuParameters1 ccuParameters1;
	CcuParameters2 ccuParameters2;
	DcDcParameter dcDcParameter;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager componentManager;
//	private Config config;
	private InverterCount inverterCount;

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
				if (getCcuState() == CCUState.UNDEFINED) {
					return;
				}
					writeCommands();  
					writeCcuParameters1();
					writeCcuParameters2();
					writeDcDcControlCommandWord();
					writeIpuInverter1ControlCommand();
					writeIpuInverter2ControlCommand();
					writeIpuInverter3ControlCommand();
				
			} catch (IllegalArgumentException | OpenemsNamedException e) {
				log.error(e.getMessage());
			}
			break;
		}
	}
	
	@Override
	public String debugLog() {
		CCUState state = ((EnumReadChannel) this.channel(GridConChannelId.CCU_STATE)).value().asEnum();
		IntegerReadChannel errorCountChannel = this.channel(GridConChannelId.CCU_ERROR_COUNT); 
		int errorCount = errorCountChannel.value().orElse(-1);
		return "Gridcon ccu state: " + state + "; Error count: " + errorCount;
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

		Commands.getCommands().setParameterPref(activePowerFactor);
		Commands.getCommands().setParameterQref(reactivePowerFactor);
	}

	protected void writeCommands() throws IllegalArgumentException, OpenemsNamedException {	
		
		Commands c = Commands.getCommands();
		
		System.out.println("Write Command control word:\n" + c.toString());
		
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_PLAY, c.getPlayBit());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_READY, c.getReadyAndStopBit2nd());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ACKNOWLEDGE, c.getAcknowledgeBit());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_STOP, c.getStopBit1st());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_BLACKSTART_APPROVAL,
				c.isBlackstartApproval());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_SYNC_APPROVAL,
				c.isSyncApproval());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ACTIVATE_SHORT_CIRCUIT_HANDLING,
				c.isShortCircuitHandling());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_MODE_SELECTION,
				c.getModeSelection().value);
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_TRIGGER_SIA, c.isTriggerSia());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ACTIVATE_HARMONIC_COMPENSATION,
				c.isHarmonicCompensation());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ID_1_SD_CARD_PARAMETER_SET,
				c.isParameterSet1());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ID_2_SD_CARD_PARAMETER_SET,
				c.isParameterSet2());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ID_3_SD_CARD_PARAMETER_SET,
				c.isParameterSet3());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ID_4_SD_CARD_PARAMETER_SET,
				c.isParameterSet4());

		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_1, 
				c.isEnableIpu1());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_2,
				c.isEnableIpu2());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_3,
				c.isEnableIpu3());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_4,
				c.isEnableIpu4());

		this.writeValueToChannel(GridConChannelId.COMMAND_ERROR_CODE_FEEDBACK,
				c.getErrorCodeFeedback());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_U0, c.getParameterU0());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_F0, c.getParameterF0());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_Q_REF,
				c.getParameterQref());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_P_REF,
				c.getParameterPref());
		this.writeValueToChannel(GridConChannelId.COMMAND_TIME_SYNC_DATE, c.getSyncDate());
		this.writeValueToChannel(GridConChannelId.COMMAND_TIME_SYNC_TIME, c.getSyncTime());
	}

	protected void writeCcuParameters1() throws IllegalArgumentException, OpenemsNamedException {
		CcuParameters1 ccpw = CcuParameters1.getCcuParameters1();
		
		System.out.println("Write CCU control parameters 1 word:\n" + ccpw.toString());
		
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_MAIN_LOWER,
				ccpw.getuByQDroopMainLower());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_MAIN_UPPER,
				ccpw.getuByQDroopMainUpper());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN,
				ccpw.getuByQDroopT1Main());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_MAIN_LOWER,
				ccpw.getfByPDroopMainLower());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_MAIN_UPPER,
				ccpw.getfByPDroopMainUpper());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_T1_MAIN,
				ccpw.getfByPDroopT1Main());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_Q_U_DROOP_MAIN_LOWER,
				ccpw.getqByUDroopMainLower());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_Q_U_DROOP_MAIN_UPPER,
				ccpw.getqByUDroopMainUpper());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_Q_U_DEAD_BAND_LOWER,
				ccpw.getqByUDeadBandLower());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_Q_U_DEAD_BAND_UPPER,
				ccpw.getqByUDeadBandUpper());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_Q_LIMIT, ccpw.getqLimit());		
	}
	
	protected void writeCcuParameters2() throws IllegalArgumentException, OpenemsNamedException {
		CcuParameters2 ccpw = CcuParameters2.getCcuParameters2();
		
		System.out.println("Write CCU control parameters 2 word:\n" + ccpw.toString());
		
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_F_DROOP_MAIN_LOWER,
				ccpw.getpByFDroopMainLower());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_F_DROOP_MAIN_UPPER,
				ccpw.getpByFDroopMainUpper());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_F_DEAD_BAND_LOWER,
				ccpw.getpByFDeadBandLower());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_F_DEAD_BAND_UPPER,
				ccpw.getpByFDeadBandUpper());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_U_DROOP_LOWER, ccpw.getpByUDroopLower());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_U_DROOP_UPPER, ccpw.getpByUDroopUpper());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_U_DEAD_BAND_LOWER,
				ccpw.getpByUDeadBandLower());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_U_DEAD_BAND_UPPER,
				ccpw.getpByUDeadBandUpper());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_U_MAX_CHARGE,
				ccpw.getpByUMaxCharge());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_U_MAX_DISCHARGE,
				ccpw.getpByUMaxDischarge());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_MODE,
				ccpw.getpControlMode().getValue()); //
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_TWO,
				ccpw.getpControlLimTwo());
		writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_ONE,
				ccpw.getpControlLimOne());
	}

	protected void writeIpuInverter1ControlCommand() throws IllegalArgumentException, OpenemsNamedException {
		IpuParameter iicw = IpuParameter.getIpu1Parameter();
		
		System.out.println("IPU 1 Inverter control word:\n" + iicw.toString());
		
		writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_DC_VOLTAGE_SETPOINT,
				iicw.getDcVoltageSetpoint());
		writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_DC_CURRENT_SETPOINT,
				iicw.getDcCurrentSetpoint());
		writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_U0_OFFSET_TO_CCU_VALUE,
				iicw.getU0OffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_F0_OFFSET_TO_CCU_VALUE,
				iicw.getF0OffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
				iicw.getqRefOffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
				iicw.getpRefOffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_P_MAX_DISCHARGE, iicw.getpMaxDischarge());
		writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_P_MAX_CHARGE, iicw.getpMaxCharge());
	}

	protected void writeIpuInverter2ControlCommand() throws IllegalArgumentException, OpenemsNamedException {
		IpuParameter iicw = IpuParameter.getIpu2Parameter();
		
		System.out.println("IPU 2 Inverter control word:\n" + iicw.toString());
		
		writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_DC_VOLTAGE_SETPOINT,
				iicw.getDcVoltageSetpoint());
		writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_DC_CURRENT_SETPOINT,
				iicw.getDcCurrentSetpoint());
		writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_U0_OFFSET_TO_CCU_VALUE,
				iicw.getU0OffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_F0_OFFSET_TO_CCU_VALUE,
				iicw.getF0OffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
				iicw.getqRefOffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
				iicw.getpRefOffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_P_MAX_DISCHARGE, iicw.getpMaxDischarge());
		writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_P_MAX_CHARGE, iicw.getpMaxCharge());
	}

	protected void writeIpuInverter3ControlCommand() throws IllegalArgumentException, OpenemsNamedException {
		IpuParameter iicw = IpuParameter.getIpu3Parameter();
		
		System.out.println("IPU 3 Inverter control word:\n" + iicw.toString());
		
		writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_DC_VOLTAGE_SETPOINT,
				iicw.getDcVoltageSetpoint());
		writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_DC_CURRENT_SETPOINT,
				iicw.getDcCurrentSetpoint());
		writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_U0_OFFSET_TO_CCU_VALUE,
				iicw.getU0OffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_F0_OFFSET_TO_CCU_VALUE,
				iicw.getF0OffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
				iicw.getqRefOffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
				iicw.getpRefOffsetToCcu());
		writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_P_MAX_DISCHARGE, iicw.getpMaxDischarge());
		writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_P_MAX_CHARGE, iicw.getpMaxCharge());
	}

	protected void writeDcDcControlCommandWord() throws IllegalArgumentException, OpenemsNamedException {
		DcDcParameter dcc = DcDcParameter.getDcdcParameter();
		
		System.out.println("DC DC control command:\n" + dcc.toString());

		if (dcc.getStringControlMode() == 0) {
			//weighting is never allowed to be '0', but it's working according to the tool
			//throw new OpenemsException("Calculated weight of '0' -> not allowed!");
			log.error("Calculated weight of '0' -> not allowed!");
		}

		writeValueToChannel(GridConChannelId.DCDC_CONTROL_DC_VOLTAGE_SETPOINT, dcc.getDcVoltageSetpoint()); //
		writeValueToChannel(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, dcc.getWeightStringA()); //
		writeValueToChannel(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, dcc.getWeightStringB()); //
		writeValueToChannel(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, dcc.getWeightStringC()); //
		writeValueToChannel(GridConChannelId.DCDC_CONTROL_I_REF_STRING_A, dcc.getiRefStringA()); //
		writeValueToChannel(GridConChannelId.DCDC_CONTROL_I_REF_STRING_B, dcc.getiRefStringB()); //
		writeValueToChannel(GridConChannelId.DCDC_CONTROL_I_REF_STRING_C, dcc.getiRefStringC()); //
		writeValueToChannel(GridConChannelId.DCDC_CONTROL_STRING_CONTROL_MODE, dcc.getStringControlMode()); //
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
				new FC16WriteRegistersTask(Commands.COMMANDS_ADRESS, //
						m(new BitsWordElement(Commands.COMMANDS_ADRESS, this) //
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
						m(new BitsWordElement(Commands.COMMANDS_ADRESS + 1, this) //
								.bit(12, GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_4) //
								.bit(13, GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_3) //
								.bit(14, GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_2) //
								.bit(15, GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_1) //
						), //
						m(GridConChannelId.COMMAND_ERROR_CODE_FEEDBACK,
								new UnsignedDoublewordElement(Commands.COMMANDS_ADRESS + 2).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_U0,
								new FloatDoublewordElement(Commands.COMMANDS_ADRESS + 4).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_F0,
								new FloatDoublewordElement(Commands.COMMANDS_ADRESS + 6).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_Q_REF,
								new FloatDoublewordElement(Commands.COMMANDS_ADRESS + 8).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_P_REF,
								new FloatDoublewordElement(Commands.COMMANDS_ADRESS + 10).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_TIME_SYNC_DATE,
								new UnsignedDoublewordElement(Commands.COMMANDS_ADRESS + 12).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_TIME_SYNC_TIME,
								new UnsignedDoublewordElement(Commands.COMMANDS_ADRESS + 14).wordOrder(WordOrder.LSWMSW)) //
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
				 * CCU Control Parameters 1
				 */
				new FC16WriteRegistersTask(CcuParameters1.CCU_PARAMETERS_1_ADRESS, //
						m(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_MAIN_LOWER,
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 0).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_MAIN_UPPER,
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 2).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN,
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 4).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_MAIN_LOWER,
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 6).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_MAIN_UPPER,
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 8).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_T1_MAIN,
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 10).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_U_DROOP_MAIN_LOWER,
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 12).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_U_DROOP_MAIN_UPPER,
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 14).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_U_DEAD_BAND_LOWER,
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 16).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_U_DEAD_BAND_UPPER,
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 18).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_LIMIT,
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 20).wordOrder(WordOrder.LSWMSW)) //
				)
				,
				/*
				 * CCU Control Parameters 2
				 */
				new FC16WriteRegistersTask(CcuParameters2.CCU_PARAMETERS_2_ADRESS, //
						m(GridConChannelId.CONTROL_PARAMETER_P_F_DROOP_MAIN_LOWER,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 0).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_F_DROOP_MAIN_UPPER,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 2).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_F_DEAD_BAND_LOWER,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 4).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_F_DEAD_BAND_UPPER,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 6).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_DROOP_LOWER,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 8).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_DROOP_UPPER,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 10).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_DEAD_BAND_LOWER,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 12).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_DEAD_BAND_UPPER,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 14).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_MAX_CHARGE,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 16).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_MAX_DISCHARGE,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 18).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_MODE,
								new UnsignedDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 20).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_TWO,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 22).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_ONE,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 24).wordOrder(WordOrder.LSWMSW)) //
				)
//				,								/*
//				 * Control Parameters Mirror
//				 */
//				new FC3ReadRegistersTask(32912, Priority.LOW,
//						m(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_MAIN,
//								new FloatDoublewordElement(32912).wordOrder(WordOrder.LSWMSW)), //
//						m(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN,
//								new FloatDoublewordElement(32914).wordOrder(WordOrder.LSWMSW)), //
//						m(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_MAIN,
//								new FloatDoublewordElement(32916).wordOrder(WordOrder.LSWMSW)), //
//						m(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_T1_MAIN,
//								new FloatDoublewordElement(32918).wordOrder(WordOrder.LSWMSW)), //
//						m(GridConChannelId.CONTROL_PARAMETER_Q_U_DROOP_MAIN,
//								new FloatDoublewordElement(32920).wordOrder(WordOrder.LSWMSW)), //
//						m(GridConChannelId.CONTROL_PARAMETER_Q_U_DEAD_BAND,
//								new FloatDoublewordElement(32922).wordOrder(WordOrder.LSWMSW)), //
//						m(GridConChannelId.CONTROL_PARAMETER_Q_LIMIT,
//								new FloatDoublewordElement(32924).wordOrder(WordOrder.LSWMSW)), //
//						m(GridConChannelId.CONTROL_PARAMETER_P_F_DROOP_MAIN,
//								new FloatDoublewordElement(32926).wordOrder(WordOrder.LSWMSW)), //
//						m(GridConChannelId.CONTROL_PARAMETER_P_F_DEAD_BAND,
//								new FloatDoublewordElement(32928).wordOrder(WordOrder.LSWMSW)), //
//						m(GridConChannelId.CONTROL_PARAMETER_P_U_DROOP,
//								new FloatDoublewordElement(32930).wordOrder(WordOrder.LSWMSW)) //
//				)
				);

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
			int startAddressIpuControl = DcDcParameter.DC_DC_ADRESS; // DCDC has now a fix address
			int startAddressIpuControlMirror = 33040; 
			
			int startAddressIpuState = 33264; // == THREE
			int startAddressIpuDcdc = 33584; // == THREE
			switch (this.inverterCount) {
			case ONE:
//				startAddressIpuControl = 32656;
				startAddressIpuControlMirror = 32976;
				startAddressIpuState = 33200;
				startAddressIpuDcdc = 33520;
				break;
			case TWO:
//				startAddressIpuControl = 32688;
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
set					 */
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
									new UnsignedDoublewordElement(startAddressIpuControl + 14).wordOrder(WordOrder.LSWMSW)) //
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
									new UnsignedDoublewordElement(startAddressIpuControlMirror + 14)
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
	public void setErrorCodeFeedback(int errorCodeFeedback) {
		Commands.getCommands().setErrorCodeFeedback(errorCodeFeedback);
	}

	@Override
	public int getErrorCode() {
		return getInteger(GridConChannelId.CCU_ERROR_CODE);		
	}

	
	// TODO Check sign, round!? 
//	@Override
	public float getActivePowerInverter1() {
		return getFloat(GridConChannelId.INVERTER_1_STATUS_DC_LINK_ACTIVE_POWER);
	}
	
//	@Override
	public float getActivePowerInverter2() {
		return getFloat(GridConChannelId.INVERTER_2_STATUS_DC_LINK_ACTIVE_POWER);
	}

//	@Override
	public float getActivePowerInverter3() {
		return getFloat(GridConChannelId.INVERTER_3_STATUS_DC_LINK_ACTIVE_POWER);
	}

	@Override
	public float getActivePower() {
		return getActivePowerInverter1() + getActivePowerInverter2() + getActivePowerInverter3(); 
	}
	
	@Override
	public float getDcLinkPositiveVoltage() {
		return getFloat(GridConChannelId.DCDC_STATUS_DC_LINK_POSITIVE_VOLTAGE);
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
		switch (inverterCount) {
		case ONE:
			Commands.getCommands().setEnableIpu1(enabled);
			break;
		case TWO:
			Commands.getCommands().setEnableIpu1(enabled);
			break;
		case THREE:
			Commands.getCommands().setEnableIpu1(enabled);
			break;
		}
	}

	@Override
	public void setEnableIPU2(boolean enabled) {
		switch (inverterCount) {
		case ONE:
			System.out.println("Not allowed, there is only one inverters!");			
			break;
		case TWO:
			Commands.getCommands().setEnableIpu2(enabled);
			break;
		case THREE:
			Commands.getCommands().setEnableIpu2(enabled);
			break;
		}
	}

	@Override
	public void setEnableIPU3(boolean enabled) {
		switch (inverterCount) {
		case ONE:
			System.out.println("Not allowed, there are only two inverters!");			
			break;
		case TWO:
			System.out.println("Not allowed, there are only two inverters!");
			break;
		case THREE:
			Commands.getCommands().setEnableIpu3(enabled);
			break;
		}
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

		Commands.getCommands().setParameterSet1(set1);
		Commands.getCommands().setParameterSet2(set2);
		Commands.getCommands().setParameterSet3(set3);
		Commands.getCommands().setParameterSet4(set4);
	}

	@Override
	public void setModeSelection(Mode modeSelection) {
		Commands.getCommands().setModeSelection(modeSelection);
	}
	
	@Override
	public void setSyncApproval(boolean b) {
		Commands.getCommands().setSyncApproval(b);
	}

	@Override
	public void setBlackStartApproval(boolean b) {
		Commands.getCommands().setBlackstartApproval(b);
	}

	@Override
	public void setU0(float onGridVoltageFactor) {
		Commands.getCommands().setParameterU0(onGridVoltageFactor);
	}

	@Override
	public void setF0(float onGridFrequencyFactor) {
		Commands.getCommands().setParameterF0(onGridFrequencyFactor);
	}

	@Override
	public void setPControlMode(PControlMode pControlMode) {
		CcuParameters2.getCcuParameters2().setpControlMode(pControlMode);
	}

	@Override
	public void setQLimit(float qLimit) {
		CcuParameters1.getCcuParameters1().setqLimit(qLimit);
	}

	@Override
	public void setPMaxChargeIPU1(float maxPower) {
		IpuParameter.getIpu1Parameter().setpMaxCharge(maxPower);
	}

	@Override
	public void setPMaxDischargeIPU1(float maxPower) {
		IpuParameter.getIpu1Parameter().setpMaxDischarge(maxPower);
	}

	@Override
	public void setPMaxChargeIPU2(float maxPower) {
		IpuParameter.getIpu2Parameter().setpMaxCharge(maxPower);
	}

	@Override
	public void setPMaxDischargeIPU2(float maxPower) {
		IpuParameter.getIpu2Parameter().setpMaxDischarge(maxPower);
	}

	@Override
	public void setPMaxChargeIPU3(float maxPower) {
		IpuParameter.getIpu3Parameter().setpMaxCharge(maxPower);
	}

	@Override
	public void setPMaxDischargeIPU3(float maxPower) {
		IpuParameter.getIpu3Parameter().setpMaxDischarge(maxPower);
	}

	@Override
	public void setDcLinkVoltage(float dcLinkVoltageSetpoint) {
		DcDcParameter.getDcdcParameter().setDcVoltageSetpoint(dcLinkVoltageSetpoint);
	}

	@Override
	public void setWeightStringA(Float weight) {
		DcDcParameter.getDcdcParameter().setWeightStringA(weight);
	}

	@Override
	public void setWeightStringB(Float weight) {
		DcDcParameter.getDcdcParameter().setWeightStringB(weight);
	}

	@Override
	public void setWeightStringC(Float weight) {
		DcDcParameter.getDcdcParameter().setWeightStringC(weight);
	}

	@Override
	public void setStringControlMode(int stringControlMode) {
		DcDcParameter.getDcdcParameter().setStringControlMode(stringControlMode);
	}

	@Override
	public void enableDCDC() {
		switch (inverterCount) {
		case ONE:
			Commands.getCommands().setEnableIpu2(true);
			break;
		case TWO:
			Commands.getCommands().setEnableIpu3(true);
			break;
		case THREE:
			Commands.getCommands().setEnableIpu4(true);
			break;
		}
	}
	
	@Override
	public void disableDCDC() {
		switch (inverterCount) {
		case ONE:
			Commands.getCommands().setEnableIpu2(false);
			break;
		case TWO:
			Commands.getCommands().setEnableIpu3(false);
			break;
		case THREE:
			Commands.getCommands().setEnableIpu4(false);
			break;
		}
	}

	@Override
	public int getErrorCount() {
		return getInteger(GridConChannelId.CCU_ERROR_COUNT);
	}

	@Override
	public void setSyncDate(int date) {
		Commands.getCommands().setSyncDate(date);
	}

	@Override
	public void setSyncTime(int time) {
		Commands.getCommands().setSyncTime(time);
	}


	private <T> void writeValueToChannel(GridConChannelId channelId, T value)
			throws IllegalArgumentException, OpenemsNamedException {
		((WriteChannel<?>) channel(channelId)).setNextWriteValueFromObject(value);
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
	public void setStop(boolean stop) {
		Commands.getCommands().setStopBit1st(stop);
		Commands.getCommands().setReadyAndStopBit2nd(stop);
		if (stop) { // only one command should be executed!
			System.out.println("only one command should be executed!");
			setPlay(false);
			setAcknowledge(false);
		}
	}

	@Override
	public void setAcknowledge(boolean acknowledge) {
		Commands.getCommands().setAcknowledgeBit(acknowledge);
		if (acknowledge) { // only one command should be executed!
			System.out.println("only one command should be executed!");
			setStop(false);
			setPlay(false);
		}
	}

	@Override
	public void setPlay(boolean play) {
		Commands.getCommands().setPlayBit(play);
		if (play) { // only one command should be executed!
			System.out.println("only one command should be executed!");
			setStop(false);
			setAcknowledge(false);
		}
	}

	@Override
	public boolean isDcDcStarted() {
		StatusIPUStateMachine state = ((EnumReadChannel) this.channel(GridConChannelId.DCDC_STATUS_STATE_MACHINE)).value().asEnum();
		return state == StatusIPUStateMachine.RUN;
	}

	@Override
	public boolean isIpusStarted(boolean enableIPU1, boolean enableIPU2, boolean enableIPU3) {
		boolean ret = true;
		
		if (enableIPU1) {			
			ret = ret && isIpuRunning(GridConChannelId.INVERTER_1_STATUS_STATE_MACHINE);
		}
		
		if (enableIPU2) {
			ret = ret && isIpuRunning(GridConChannelId.INVERTER_2_STATUS_STATE_MACHINE);
		}
		
		if (enableIPU3) {
			ret = ret && isIpuRunning(GridConChannelId.INVERTER_3_STATUS_STATE_MACHINE);
		}
		
		return ret;
	}

	private boolean isIpuRunning(GridConChannelId id) {
		StatusIPUStateMachine state = ((EnumReadChannel) this.channel(id)).value().asEnum();
		return (state == StatusIPUStateMachine.RUN);
	}
}
