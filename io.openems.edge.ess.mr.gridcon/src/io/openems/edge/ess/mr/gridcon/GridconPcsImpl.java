package io.openems.edge.ess.mr.gridcon;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT;

import java.nio.ByteOrder;

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
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
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
import io.openems.edge.ess.mr.gridcon.enums.BalancingMode;
import io.openems.edge.ess.mr.gridcon.enums.CcuState;
import io.openems.edge.ess.mr.gridcon.enums.FundamentalFrequencyMode;
import io.openems.edge.ess.mr.gridcon.enums.GridConChannelId;
import io.openems.edge.ess.mr.gridcon.enums.HarmonicCompensationMode;
import io.openems.edge.ess.mr.gridcon.enums.InverterCount;
import io.openems.edge.ess.mr.gridcon.enums.Mode;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;
import io.openems.edge.ess.mr.gridcon.enums.StatusIpuStateMachine;
import io.openems.edge.ess.mr.gridcon.writewords.CcuParameters1;
import io.openems.edge.ess.mr.gridcon.writewords.CcuParameters2;
import io.openems.edge.ess.mr.gridcon.writewords.Commands;
import io.openems.edge.ess.mr.gridcon.writewords.CosPhiParameters;
import io.openems.edge.ess.mr.gridcon.writewords.DcDcParameter;
import io.openems.edge.ess.mr.gridcon.writewords.IpuParameter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "MR.Gridcon", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE //
})
public class GridconPcsImpl extends AbstractOpenemsModbusComponent
		implements ModbusComponent, OpenemsComponent, GridconPcs, EventHandler {

	private static final int START_ADDRESS_GRID_MEASUREMENTS = 33456;

	private static final int START_ADDRESS_DCDC_MEASUREMENTS = 33488;

	private static final int START_ADDRESS_DCDC_STATE_WITH_TWO_IPUS = 33232;
	private static final int START_ADDRESS_DCDC_STATE_WITH_ONE_IPU = 33200;
	private static final int START_ADDRESS_DCDC_STATE_WITH_THREE_IPUS = 33264;

	private static final String NAME_PART_INVERTER_2 = "INVERTER2";

	private static final String NAME_PART_INVERTER_3 = "INVERTER3";

	public static final float DC_LINK_VOLTAGE_TOLERANCE_VOLT = 20;

	public static final double EFFICIENCY_LOSS_FACTOR = 0.07;
	public static final double EFFICIENCY_LOSS_DISCHARGE_FACTOR = EFFICIENCY_LOSS_FACTOR;
	public static final double EFFICIENCY_LOSS_CHARGE_FACTOR = EFFICIENCY_LOSS_FACTOR;

	private final Logger log = LoggerFactory.getLogger(GridconPcsImpl.class);
	private final Commands commands;
	private final CcuParameters1 ccuParameters1;
	private final CcuParameters2 ccuParameters2;
	private final IpuParameter ipu1Parameter;
	private final IpuParameter ipu2Parameter;
	private final IpuParameter ipu3Parameter;
	private final DcDcParameter dcDcParameter;
	private final CosPhiParameters cosPhiParameters;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager componentManager;

	private InverterCount inverterCount;

	private int activePowerPreset;

	private double efficiencyLossDischargeFactor;
	private double efficiencyLossChargeFactor;

	public GridconPcsImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				GridConChannelId.values() //
		);
		this.commands = new Commands();
		this.ccuParameters1 = new CcuParameters1();
		this.ccuParameters2 = new CcuParameters2();
		this.ipu1Parameter = new IpuParameter();
		this.ipu2Parameter = new IpuParameter();
		this.ipu3Parameter = new IpuParameter();
		this.dcDcParameter = new DcDcParameter();
		this.cosPhiParameters = new CosPhiParameters();
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.inverterCount = config.inverterCount();
		this.efficiencyLossChargeFactor = config.efficiencyLossChargeFactor();
		this.efficiencyLossDischargeFactor = config.efficiencyLossDischargeFactor();

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.unit_id(), this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}

		this.setBalancingMode(config.balancing_mode());
		this.setFundamentalFrequencyMode(config.fundamental_frequency_mode());
		this.setHarmonicCompensationMode(config.harmonic_compensation_mode());
		this.cosPhiParameters.setCosPhiSetPoint1(config.cos_phi_setpoint_1());
		this.cosPhiParameters.setCosPhiSetPoint2(config.cos_phi_setpoint_2());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public float getMaxApparentPower() {
		return this.inverterCount.getMaxApparentPower();
	}

	@Override
	public void doWriteTasks() throws OpenemsNamedException {
		this.writeCommands();
		this.writeCcuParameters1();
		this.writeCcuParameters2();
		this.writeCosPhiParameters();
		this.writeDcDcControlCommandWord();
		this.writeIpuInverter1ControlCommand();
		this.writeIpuInverter2ControlCommand();
		this.writeIpuInverter3ControlCommand();
	}

	@Override
	public void handleEvent(Event event) {

		if (!isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE:
			try {
				// Ensure that all values are set before writing is executed
				this.doWriteTasks();
			} catch (OpenemsNamedException e) {
				this.log.error("Error in doWriteTasks()", e);
			}
			break;
		}
	}

	@Override
	public String debugLog() {
		CcuState state = ((EnumReadChannel) this.channel(GridConChannelId.CCU_STATE)).value().asEnum();
		IntegerReadChannel errorCountChannel = this.channel(GridConChannelId.CCU_ERROR_COUNT);
		int errorCount = errorCountChannel.value().orElse(-1);
		return "Gridcon CCU state: " + state + "; Error count: " + errorCount + "; Active Power: "
				+ this.getActivePower();

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

		this.activePowerPreset = activePower;

		float activePowerFactor = (-1) * activePower / maxApparentPower;
		float reactivePowerFactor = (-1) * reactivePower / maxApparentPower;

		this.commands.setParameterPref(activePowerFactor);
		this.commands.setParameterQref(reactivePowerFactor);
	}

	protected void writeCommands() throws IllegalArgumentException, OpenemsNamedException {

		Commands c = this.commands;

		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_PLAY, c.getPlayBit());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_READY, c.getReadyAndStopBit2nd());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ACKNOWLEDGE, c.getAcknowledgeBit());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_STOP, c.getStopBit1st());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_BLACKSTART_APPROVAL, c.isBlackstartApproval());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_SYNC_APPROVAL, c.isSyncApproval());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ACTIVATE_SHORT_CIRCUIT_HANDLING,
				c.isShortCircuitHandling());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_MODE_SELECTION, c.getMode().value);
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_TRIGGER_SIA, c.isTriggerSia());

		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_FUNDAMENTAL_FREQUENCY_MODE_BIT_1,
				c.getFundamentalFrequencyMode().isBit1());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_FUNDAMENTAL_FREQUENCY_MODE_BIT_2,
				c.getFundamentalFrequencyMode().isBit2());

		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_BALANCING_MODE_BIT_1,
				c.getBalancingMode().isBit1());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_BALANCING_MODE_BIT_2,
				c.getBalancingMode().isBit2());

		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_HARMONIC_COMPENSATION_MODE_BIT_1,
				c.getHarmonicCompensationMode().isBit1());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_HARMONIC_COMPENSATION_MODE_BIT_2,
				c.getHarmonicCompensationMode().isBit2());

		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_1, c.isEnableIpu1());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_2, c.isEnableIpu2());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_3, c.isEnableIpu3());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_4, c.isEnableIpu4());

		this.writeValueToChannel(GridConChannelId.COMMAND_ERROR_CODE_FEEDBACK, c.getErrorCodeFeedback());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_U0, c.getParameterU0());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_F0, c.getParameterF0());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_Q_REF, c.getParameterQref());
		this.writeValueToChannel(GridConChannelId.COMMAND_CONTROL_PARAMETER_P_REF, c.getParameterPref());

		this.writeValueToChannel(GridConChannelId.COMMAND_TIME_SYNC_DATE, c.getSyncDate());
		this.writeValueToChannel(GridConChannelId.COMMAND_TIME_SYNC_TIME, c.getSyncTime());
	}

	protected void writeCcuParameters1() throws IllegalArgumentException, OpenemsNamedException {
		CcuParameters1 ccpw = this.ccuParameters1;

		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_MAIN_LOWER, ccpw.getuByQDroopMainLower());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_MAIN_UPPER, ccpw.getuByQDroopMainUpper());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN, ccpw.getuByQDroopT1Main());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_MAIN_LOWER, ccpw.getfByPDroopMainLower());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_MAIN_UPPER, ccpw.getfByPDroopMainUpper());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_T1_MAIN, ccpw.getfByPDroopT1Main());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_Q_U_DROOP_MAIN_LOWER, ccpw.getqByUDroopMainLower());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_Q_U_DROOP_MAIN_UPPER, ccpw.getqByUDroopMainUpper());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_Q_U_DEAD_BAND_LOWER, ccpw.getqByUDeadBandLower());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_Q_U_DEAD_BAND_UPPER, ccpw.getqByUDeadBandUpper());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_Q_LIMIT, ccpw.getqLimit());
	}

	protected void writeCcuParameters2() throws IllegalArgumentException, OpenemsNamedException {
		CcuParameters2 ccpw = this.ccuParameters2;

		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_F_DROOP_MAIN_LOWER, ccpw.getpByFDroopMainLower());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_F_DROOP_MAIN_UPPER, ccpw.getpByFDroopMainUpper());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_F_DEAD_BAND_LOWER, ccpw.getpByFDeadBandLower());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_F_DEAD_BAND_UPPER, ccpw.getpByFDeadBandUpper());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_U_DROOP_LOWER, ccpw.getpByUDroopLower());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_U_DROOP_UPPER, ccpw.getpByUDroopUpper());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_U_DEAD_BAND_LOWER, ccpw.getpByUDeadBandLower());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_U_DEAD_BAND_UPPER, ccpw.getpByUDeadBandUpper());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_U_MAX_CHARGE, ccpw.getpByUMaxCharge());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_U_MAX_DISCHARGE, ccpw.getpByUMaxDischarge());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_MODE, ccpw.getpControlMode().getValue()); //
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_TWO, ccpw.getpControlLimTwo());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_ONE, ccpw.getpControlLimOne());
	}

	protected void writeCosPhiParameters() throws IllegalArgumentException, OpenemsNamedException {
		CosPhiParameters cpp = this.cosPhiParameters;

		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_COS_PHI_SETPOINT_1, cpp.getCosPhiSetPoint1());
		this.writeValueToChannel(GridConChannelId.CONTROL_PARAMETER_COS_PHI_SETPOINT_2, cpp.getCosPhiSetPoint2());
	}

	protected void writeIpuInverter1ControlCommand() throws IllegalArgumentException, OpenemsNamedException {
		IpuParameter iicw = this.ipu1Parameter;

		this.writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_DC_VOLTAGE_SETPOINT, iicw.getDcVoltageSetpoint());
		this.writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_DC_CURRENT_SETPOINT, iicw.getDcCurrentSetpoint());
		this.writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_U0_OFFSET_TO_CCU_VALUE, iicw.getU0OffsetToCcu());
		this.writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_F0_OFFSET_TO_CCU_VALUE, iicw.getF0OffsetToCcu());
		this.writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
				iicw.getqRefOffsetToCcu());
		this.writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
				iicw.getpRefOffsetToCcu());
		this.writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_P_MAX_DISCHARGE, iicw.getpMaxDischarge());
		this.writeValueToChannel(GridConChannelId.INVERTER_1_CONTROL_P_MAX_CHARGE, iicw.getpMaxCharge());
	}

	protected void writeIpuInverter2ControlCommand() throws IllegalArgumentException, OpenemsNamedException {
		IpuParameter iicw = this.ipu2Parameter;

		this.writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_DC_VOLTAGE_SETPOINT, iicw.getDcVoltageSetpoint());
		this.writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_DC_CURRENT_SETPOINT, iicw.getDcCurrentSetpoint());
		this.writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_U0_OFFSET_TO_CCU_VALUE, iicw.getU0OffsetToCcu());
		this.writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_F0_OFFSET_TO_CCU_VALUE, iicw.getF0OffsetToCcu());
		this.writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
				iicw.getqRefOffsetToCcu());
		this.writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
				iicw.getpRefOffsetToCcu());
		this.writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_P_MAX_DISCHARGE, iicw.getpMaxDischarge());
		this.writeValueToChannel(GridConChannelId.INVERTER_2_CONTROL_P_MAX_CHARGE, iicw.getpMaxCharge());
	}

	protected void writeIpuInverter3ControlCommand() throws IllegalArgumentException, OpenemsNamedException {
		IpuParameter iicw = this.ipu3Parameter;

		this.writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_DC_VOLTAGE_SETPOINT, iicw.getDcVoltageSetpoint());
		this.writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_DC_CURRENT_SETPOINT, iicw.getDcCurrentSetpoint());
		this.writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_U0_OFFSET_TO_CCU_VALUE, iicw.getU0OffsetToCcu());
		this.writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_F0_OFFSET_TO_CCU_VALUE, iicw.getF0OffsetToCcu());
		this.writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_Q_REF_OFFSET_TO_CCU_VALUE,
				iicw.getqRefOffsetToCcu());
		this.writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_P_REF_OFFSET_TO_CCU_VALUE,
				iicw.getpRefOffsetToCcu());
		this.writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_P_MAX_DISCHARGE, iicw.getpMaxDischarge());
		this.writeValueToChannel(GridConChannelId.INVERTER_3_CONTROL_P_MAX_CHARGE, iicw.getpMaxCharge());
	}

	protected void writeDcDcControlCommandWord() throws IllegalArgumentException, OpenemsNamedException {
		DcDcParameter dcc = this.dcDcParameter;

		System.out.println("DC DC control command:\n" + dcc.toString());

		if (dcc.getStringControlMode() == 0) {
			// weighting is never allowed to be '0', but it's working according to the tool
			// throw new OpenemsException("Calculated weight of '0' -> not allowed!");
			this.log.error("Calculated weight of '0' -> not allowed!");
		}

		this.writeValueToChannel(GridConChannelId.DCDC_CONTROL_DC_VOLTAGE_SETPOINT, dcc.getDcVoltageSetpoint()); //
		this.writeValueToChannel(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_A, dcc.getWeightStringA()); //
		this.writeValueToChannel(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_B, dcc.getWeightStringB()); //
		this.writeValueToChannel(GridConChannelId.DCDC_CONTROL_WEIGHT_STRING_C, dcc.getWeightStringC()); //
		this.writeValueToChannel(GridConChannelId.DCDC_CONTROL_I_REF_STRING_A, dcc.getiRefStringA()); //
		this.writeValueToChannel(GridConChannelId.DCDC_CONTROL_I_REF_STRING_B, dcc.getiRefStringB()); //
		this.writeValueToChannel(GridConChannelId.DCDC_CONTROL_I_REF_STRING_C, dcc.getiRefStringC()); //

		// Write values into mirror debug values for monitoring them
		GridconPcsImpl.this.channel(GridConChannelId.DCDC_CONTROL_I_REF_STRING_A_DEBUG)
				.setNextValue((int) (dcc.getiRefStringA() * 1000));
		GridconPcsImpl.this.channel(GridConChannelId.DCDC_CONTROL_I_REF_STRING_B_DEBUG)
				.setNextValue((int) (dcc.getiRefStringB() * 1000));
		GridconPcsImpl.this.channel(GridConChannelId.DCDC_CONTROL_I_REF_STRING_C_DEBUG)
				.setNextValue((int) (dcc.getiRefStringC() * 1000));

		this.writeValueToChannel(GridConChannelId.DCDC_CONTROL_STRING_CONTROL_MODE, dcc.getStringControlMode()); //
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
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
								.bit(9, GridConChannelId.COMMAND_CONTROL_WORD_FUNDAMENTAL_FREQUENCY_MODE_BIT_1) //
								.bit(10, GridConChannelId.COMMAND_CONTROL_WORD_FUNDAMENTAL_FREQUENCY_MODE_BIT_2) //
								.bit(11, GridConChannelId.COMMAND_CONTROL_WORD_BALANCING_MODE_BIT_1) //
								.bit(12, GridConChannelId.COMMAND_CONTROL_WORD_BALANCING_MODE_BIT_2) //
								.bit(13, GridConChannelId.COMMAND_CONTROL_WORD_HARMONIC_COMPENSATION_MODE_BIT_1) //
								.bit(14, GridConChannelId.COMMAND_CONTROL_WORD_HARMONIC_COMPENSATION_MODE_BIT_2) //
						), //
						m(new BitsWordElement(Commands.COMMANDS_ADRESS + 1, this) //
								.bit(12, GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_4) //
								.bit(13, GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_3) //
								.bit(14, GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_2) //
								.bit(15, GridConChannelId.COMMAND_CONTROL_WORD_ENABLE_IPU_1) //
						), //
						m(GridConChannelId.COMMAND_ERROR_CODE_FEEDBACK,
								new UnsignedDoublewordElement(Commands.COMMANDS_ADRESS + 2)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_U0,
								new FloatDoublewordElement(Commands.COMMANDS_ADRESS + 4).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_F0,
								new FloatDoublewordElement(Commands.COMMANDS_ADRESS + 6).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_Q_REF,
								new FloatDoublewordElement(Commands.COMMANDS_ADRESS + 8).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_CONTROL_PARAMETER_P_REF,
								new FloatDoublewordElement(Commands.COMMANDS_ADRESS + 10).wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_TIME_SYNC_DATE,
								new UnsignedDoublewordElement(Commands.COMMANDS_ADRESS + 12)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.COMMAND_TIME_SYNC_TIME,
								new UnsignedDoublewordElement(Commands.COMMANDS_ADRESS + 14)
										.wordOrder(WordOrder.LSWMSW)) //
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
								.bit(9, GridConChannelId.COMMAND_CONTROL_WORD_FUNDAMENTAL_FREQUENCY_MODE_BIT_1) //
								.bit(10, GridConChannelId.COMMAND_CONTROL_WORD_FUNDAMENTAL_FREQUENCY_MODE_BIT_2) //
								.bit(11, GridConChannelId.COMMAND_CONTROL_WORD_BALANCING_MODE_BIT_1) //
								.bit(12, GridConChannelId.COMMAND_CONTROL_WORD_BALANCING_MODE_BIT_2) //
								.bit(13, GridConChannelId.COMMAND_CONTROL_WORD_HARMONIC_COMPENSATION_MODE_BIT_1) //
								.bit(14, GridConChannelId.COMMAND_CONTROL_WORD_HARMONIC_COMPENSATION_MODE_BIT_2) //

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
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 0)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_MAIN_UPPER,
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 2)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN,
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 4)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_MAIN_LOWER,
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 6)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_MAIN_UPPER,
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 8)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_T1_MAIN,
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 10)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_U_DROOP_MAIN_LOWER,
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 12)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_U_DROOP_MAIN_UPPER,
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 14)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_U_DEAD_BAND_LOWER,
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 16)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_U_DEAD_BAND_UPPER,
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 18)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_Q_LIMIT,
								new FloatDoublewordElement(CcuParameters1.CCU_PARAMETERS_1_ADRESS + 20)
										.wordOrder(WordOrder.LSWMSW)) //
				),
				/*
				 * CCU Control Parameters 2
				 */
				new FC16WriteRegistersTask(CcuParameters2.CCU_PARAMETERS_2_ADRESS, //
						m(GridConChannelId.CONTROL_PARAMETER_P_F_DROOP_MAIN_LOWER,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 0)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_F_DROOP_MAIN_UPPER,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 2)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_F_DEAD_BAND_LOWER,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 4)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_F_DEAD_BAND_UPPER,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 6)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_DROOP_LOWER,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 8)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_DROOP_UPPER,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 10)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_DEAD_BAND_LOWER,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 12)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_DEAD_BAND_UPPER,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 14)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_MAX_CHARGE,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 16)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_U_MAX_DISCHARGE,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 18)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_MODE,
								new UnsignedDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 20)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_TWO,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 22)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_P_CONTROL_LIM_ONE,
								new FloatDoublewordElement(CcuParameters2.CCU_PARAMETERS_2_ADRESS + 24)
										.wordOrder(WordOrder.LSWMSW)) //
				),

				/*
				 * Cos Phi Parameters
				 */
				new FC16WriteRegistersTask(CosPhiParameters.COS_PHI_ADDRESS, //
						m(GridConChannelId.CONTROL_PARAMETER_COS_PHI_SETPOINT_1,
								new FloatDoublewordElement(CosPhiParameters.COS_PHI_ADDRESS + 0)
										.wordOrder(WordOrder.LSWMSW)), //
						m(GridConChannelId.CONTROL_PARAMETER_COS_PHI_SETPOINT_2,
								new FloatDoublewordElement(CosPhiParameters.COS_PHI_ADDRESS + 2)
										.wordOrder(WordOrder.LSWMSW)) //
				)
		// , /*
		// * Control Parameters Mirror
		// */
		// new FC3ReadRegistersTask(32912, Priority.LOW,
		// m(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_MAIN,
		// new FloatDoublewordElement(32912).wordOrder(WordOrder.LSWMSW)), //
		// m(GridConChannelId.CONTROL_PARAMETER_U_Q_DROOP_T1_MAIN,
		// new FloatDoublewordElement(32914).wordOrder(WordOrder.LSWMSW)), //
		// m(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_MAIN,
		// new FloatDoublewordElement(32916).wordOrder(WordOrder.LSWMSW)), //
		// m(GridConChannelId.CONTROL_PARAMETER_F_P_DROOP_T1_MAIN,
		// new FloatDoublewordElement(32918).wordOrder(WordOrder.LSWMSW)), //
		// m(GridConChannelId.CONTROL_PARAMETER_Q_U_DROOP_MAIN,
		// new FloatDoublewordElement(32920).wordOrder(WordOrder.LSWMSW)), //
		// m(GridConChannelId.CONTROL_PARAMETER_Q_U_DEAD_BAND,
		// new FloatDoublewordElement(32922).wordOrder(WordOrder.LSWMSW)), //
		// m(GridConChannelId.CONTROL_PARAMETER_Q_LIMIT,
		// new FloatDoublewordElement(32924).wordOrder(WordOrder.LSWMSW)), //
		// m(GridConChannelId.CONTROL_PARAMETER_P_F_DROOP_MAIN,
		// new FloatDoublewordElement(32926).wordOrder(WordOrder.LSWMSW)), //
		// m(GridConChannelId.CONTROL_PARAMETER_P_F_DEAD_BAND,
		// new FloatDoublewordElement(32928).wordOrder(WordOrder.LSWMSW)), //
		// m(GridConChannelId.CONTROL_PARAMETER_P_U_DROOP,
		// new FloatDoublewordElement(32930).wordOrder(WordOrder.LSWMSW)) //
		// )
		);

		if (inverterCount > 0) {
			/*
			 * At least 1 Inverter -> Add IPU 1
			 */
			result.addTasks(//
					/*
					 * IPU 1 StateObject
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
									new FloatDoublewordElement(33178).wordOrder(WordOrder.LSWMSW), INVERT), //
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
					 * IPU 2 StateObject
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
									new FloatDoublewordElement(33210).wordOrder(WordOrder.LSWMSW), INVERT), //
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
					 * IPU 3 StateObject
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
									new FloatDoublewordElement(33242).wordOrder(WordOrder.LSWMSW), INVERT), //
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
			int startAddressIpuControl = DcDcParameter.DC_DC_ADRESS;

			int startAddressDcDcState = START_ADDRESS_DCDC_STATE_WITH_THREE_IPUS;
			int startAddressDcdcMeasurements = START_ADDRESS_DCDC_MEASUREMENTS;
			int startAddressGridMeasurements = START_ADDRESS_GRID_MEASUREMENTS;
			switch (this.inverterCount) {
			case ONE:
				startAddressDcDcState = START_ADDRESS_DCDC_STATE_WITH_ONE_IPU;
				break;
			case TWO:
				startAddressDcDcState = START_ADDRESS_DCDC_STATE_WITH_TWO_IPUS;
				break;
			case THREE:
				// default
				break;
			}

			result.addTasks(new FC3ReadRegistersTask(startAddressGridMeasurements, Priority.HIGH,
					m(GridConChannelId.GRID_MEASUREMENT_I_L1,
							new FloatDoublewordElement(startAddressGridMeasurements).wordOrder(WordOrder.LSWMSW)), //
					m(GridConChannelId.GRID_MEASUREMENT_I_L2,
							new FloatDoublewordElement(startAddressGridMeasurements + 2).wordOrder(WordOrder.LSWMSW)), //
					m(GridConChannelId.GRID_MEASUREMENT_I_L3,
							new FloatDoublewordElement(startAddressGridMeasurements + 4).wordOrder(WordOrder.LSWMSW)), //
					m(GridConChannelId.GRID_MEASUREMENT_I_LN,
							new FloatDoublewordElement(startAddressGridMeasurements + 6).wordOrder(WordOrder.LSWMSW)), //
					m(GridConChannelId.GRID_MEASUREMENT_P_L1,
							new FloatDoublewordElement(startAddressGridMeasurements + 8).wordOrder(WordOrder.LSWMSW)), //
					m(GridConChannelId.GRID_MEASUREMENT_P_L2,
							new FloatDoublewordElement(startAddressGridMeasurements + 10).wordOrder(WordOrder.LSWMSW)), //
					m(GridConChannelId.GRID_MEASUREMENT_P_L3,
							new FloatDoublewordElement(startAddressGridMeasurements + 12).wordOrder(WordOrder.LSWMSW)), //
					m(GridConChannelId.GRID_MEASUREMENT_P_SUM,
							new FloatDoublewordElement(startAddressGridMeasurements + 14).wordOrder(WordOrder.LSWMSW)), //
					m(GridConChannelId.GRID_MEASUREMENT_Q_L1,
							new FloatDoublewordElement(startAddressGridMeasurements + 16).wordOrder(WordOrder.LSWMSW)), //
					m(GridConChannelId.GRID_MEASUREMENT_Q_L2,
							new FloatDoublewordElement(startAddressGridMeasurements + 18).wordOrder(WordOrder.LSWMSW)), //
					m(GridConChannelId.GRID_MEASUREMENT_Q_L3,
							new FloatDoublewordElement(startAddressGridMeasurements + 20).wordOrder(WordOrder.LSWMSW)), //
					m(GridConChannelId.GRID_MEASUREMENT_Q_SUM,
							new FloatDoublewordElement(startAddressGridMeasurements + 22).wordOrder(WordOrder.LSWMSW)) //
			));

			result.addTasks(//
					/*
					 * set
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
									new FloatDoublewordElement(startAddressIpuControl + 8).wordOrder(WordOrder.LSWMSW). //
											onUpdateCallback(val -> {
												if (val == null) {
													return;
												}
												GridconPcsImpl.this
														.channel(GridConChannelId.DCDC_CONTROL_I_REF_STRING_A_DEBUG)
														.setNextValue((int) (val * 1000));
											})), //
							m(GridConChannelId.DCDC_CONTROL_I_REF_STRING_B,
									new FloatDoublewordElement(startAddressIpuControl + 10).wordOrder(WordOrder.LSWMSW). //
											onUpdateCallback(val -> {
												if (val == null) {
													return;
												}
												GridconPcsImpl.this
														.channel(GridConChannelId.DCDC_CONTROL_I_REF_STRING_B_DEBUG)
														.setNextValue((int) (val * 1000));
											})), //
							m(GridConChannelId.DCDC_CONTROL_I_REF_STRING_C,
									new FloatDoublewordElement(startAddressIpuControl + 12).wordOrder(WordOrder.LSWMSW). //
											onUpdateCallback(val -> {
												if (val == null) {
													return;
												}
												GridconPcsImpl.this
														.channel(GridConChannelId.DCDC_CONTROL_I_REF_STRING_C_DEBUG)
														.setNextValue((int) (val * 1000));
											})), //
							m(GridConChannelId.DCDC_CONTROL_STRING_CONTROL_MODE,
									new UnsignedDoublewordElement(startAddressIpuControl + 14)
											.wordOrder(WordOrder.LSWMSW)) //
					),
					/*
					 * DCDC Control Mirror
					 */
					new FC3ReadRegistersTask(startAddressIpuControl, Priority.LOW,
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
									new UnsignedDoublewordElement(startAddressIpuControl + 14)
											.wordOrder(WordOrder.LSWMSW)) //
					),
					/*
					 * DCDC StateObject
					 */
					new FC3ReadRegistersTask(startAddressDcDcState, Priority.LOW, // // IPU 4 state
							m(GridConChannelId.DCDC_STATUS_STATE_MACHINE,
									new UnsignedWordElement(startAddressDcDcState)), //
							m(GridConChannelId.DCDC_STATUS_MCU, new UnsignedWordElement(startAddressDcDcState + 1)), //
							m(GridConChannelId.DCDC_STATUS_FILTER_CURRENT,
									new FloatDoublewordElement(startAddressDcDcState + 2).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_DC_LINK_POSITIVE_VOLTAGE,
									new FloatDoublewordElement(startAddressDcDcState + 4).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_DC_LINK_NEGATIVE_VOLTAGE,
									new FloatDoublewordElement(startAddressDcDcState + 6).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_DC_LINK_CURRENT,
									new FloatDoublewordElement(startAddressDcDcState + 8).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_DC_LINK_ACTIVE_POWER,
									new FloatDoublewordElement(startAddressDcDcState + 10).wordOrder(WordOrder.LSWMSW),
									INVERT), //
							m(GridConChannelId.DCDC_STATUS_DC_LINK_UTILIZATION,
									new FloatDoublewordElement(startAddressDcDcState + 12).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_FAN_SPEED_MAX,
									new UnsignedDoublewordElement(startAddressDcDcState + 14)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_FAN_SPEED_MIN,
									new UnsignedDoublewordElement(startAddressDcDcState + 16)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_TEMPERATURE_IGBT_MAX,
									new FloatDoublewordElement(startAddressDcDcState + 18).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_TEMPERATURE_MCU_BOARD,
									new FloatDoublewordElement(startAddressDcDcState + 20).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_TEMPERATURE_GRID_CHOKE,
									new FloatDoublewordElement(startAddressDcDcState + 22).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_TEMPERATURE_INVERTER_CHOKE,
									new FloatDoublewordElement(startAddressDcDcState + 24).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_RESERVE_1,
									new FloatDoublewordElement(startAddressDcDcState + 26).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_RESERVE_2,
									new FloatDoublewordElement(startAddressDcDcState + 28).wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_STATUS_RESERVE_3,
									new FloatDoublewordElement(startAddressDcDcState + 30).wordOrder(WordOrder.LSWMSW)) //
					),
					/*
					 * DCDC Measurements
					 */
					new FC3ReadRegistersTask(startAddressDcdcMeasurements, Priority.LOW, // IPU 4 measurements
							m(GridConChannelId.DCDC_MEASUREMENTS_VOLTAGE_STRING_A,
									new FloatDoublewordElement(startAddressDcdcMeasurements)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_VOLTAGE_STRING_B,
									new FloatDoublewordElement(startAddressDcdcMeasurements + 2)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_VOLTAGE_STRING_C,
									new FloatDoublewordElement(startAddressDcdcMeasurements + 4)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_CURRENT_STRING_A,
									new FloatDoublewordElement(startAddressDcdcMeasurements + 6)
											.wordOrder(WordOrder.LSWMSW). //
											onUpdateCallback(val -> {
												if (val == null) {
													return;
												}
												GridconPcsImpl.this.channel(
														GridConChannelId.DCDC_MEASUREMENTS_CURRENT_STRING_A_DEBUG)
														.setNextValue((int) (val * 1000));
											})), //
							m(GridConChannelId.DCDC_MEASUREMENTS_CURRENT_STRING_B,
									new FloatDoublewordElement(startAddressDcdcMeasurements + 8)
											.wordOrder(WordOrder.LSWMSW). //
											onUpdateCallback(val -> {
												if (val == null) {
													return;
												}
												GridconPcsImpl.this.channel(
														GridConChannelId.DCDC_MEASUREMENTS_CURRENT_STRING_B_DEBUG)
														.setNextValue((int) (val * 1000));
											})), //
							m(GridConChannelId.DCDC_MEASUREMENTS_CURRENT_STRING_C,
									new FloatDoublewordElement(startAddressDcdcMeasurements + 10)
											.wordOrder(WordOrder.LSWMSW). //
											onUpdateCallback(val -> {
												if (val == null) {
													return;
												}
												GridconPcsImpl.this.channel(
														GridConChannelId.DCDC_MEASUREMENTS_CURRENT_STRING_C_DEBUG)
														.setNextValue((int) (val * 1000));
											})), //
							m(GridConChannelId.DCDC_MEASUREMENTS_POWER_STRING_A,
									new FloatDoublewordElement(startAddressDcdcMeasurements + 12)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_POWER_STRING_B,
									new FloatDoublewordElement(startAddressDcdcMeasurements + 14)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_POWER_STRING_C,
									new FloatDoublewordElement(startAddressDcdcMeasurements + 16)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_UTILIZATION_STRING_A,
									new FloatDoublewordElement(startAddressDcdcMeasurements + 18)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_UTILIZATION_STRING_B,
									new FloatDoublewordElement(startAddressDcdcMeasurements + 20)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_UTILIZATION_STRING_C,
									new FloatDoublewordElement(startAddressDcdcMeasurements + 22)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_ACCUMULATED_SUM_DC_CURRENT,
									new FloatDoublewordElement(startAddressDcdcMeasurements + 24)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_ACCUMULATED_DC_UTILIZATION,
									new FloatDoublewordElement(startAddressDcdcMeasurements + 26)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_RESERVE_1,
									new FloatDoublewordElement(startAddressDcdcMeasurements + 28)
											.wordOrder(WordOrder.LSWMSW)), //
							m(GridConChannelId.DCDC_MEASUREMENTS_RESERVE_2,
									new FloatDoublewordElement(startAddressDcdcMeasurements + 30)
											.wordOrder(WordOrder.LSWMSW)) //
					));
		}
		return result;
	}

	@Override
	public void setErrorCodeFeedback(int errorCodeFeedback) {
		this.commands.setErrorCodeFeedback(errorCodeFeedback);
	}

	@Override
	public int getErrorCode() {
		return this.getInteger(GridConChannelId.CCU_ERROR_CODE);
	}

	private float getActivePowerInverter1() {
		FloatReadChannel c = this.channel(GridConChannelId.INVERTER_1_STATUS_DC_LINK_ACTIVE_POWER);
		return c.getNextValue().orElse(0f);
	}

	private float getActivePowerInverter2() {
		FloatReadChannel c = this.channel(GridConChannelId.INVERTER_2_STATUS_DC_LINK_ACTIVE_POWER);
		return c.getNextValue().orElse(0f);
	}

	private float getActivePowerInverter3() {
		FloatReadChannel c = this.channel(GridConChannelId.INVERTER_3_STATUS_DC_LINK_ACTIVE_POWER);
		return c.getNextValue().orElse(0f);
	}

	// TODO Check sign, round!?
	// TODO OR get CCU-Power * Max Power?!
	@Override
	public float getActivePower() {
		return this.getActivePowerInverter1() + this.getActivePowerInverter2() + this.getActivePowerInverter3();
	}

	@Override
	public float getReactivePower() { // TODO check if this is correct
		FloatReadChannel c = this.channel(GridConChannelId.CCU_POWER_Q);
		return c.getNextValue().orElse(0f) * this.getMaxApparentPower();
	}

	@Override
	public float getDcLinkPositiveVoltage() {
		FloatReadChannel c = this.channel(GridConChannelId.DCDC_STATUS_DC_LINK_POSITIVE_VOLTAGE);
		return c.value().orElse(0f);
	}

	@Override
	public boolean isCommunicationBroken() {
		return this.getModbusCommunicationFailed().get() == Boolean.TRUE;
	}

	@Override
	public void setEnableIpu1(boolean enabled) {
		switch (this.inverterCount) {
		case ONE:
			this.commands.setEnableIpu1(enabled);
			break;
		case TWO:
			this.commands.setEnableIpu1(enabled);
			break;
		case THREE:
			this.commands.setEnableIpu1(enabled);
			break;
		}
	}

	@Override
	public void setEnableIpu2(boolean enabled) {
		switch (this.inverterCount) {
		case ONE:
			System.out.println("Not allowed, there is only one inverters!");
			break;
		case TWO:
			this.commands.setEnableIpu2(enabled);
			break;
		case THREE:
			this.commands.setEnableIpu2(enabled);
			break;
		}
	}

	@Override
	public void setEnableIpu3(boolean enabled) {
		switch (this.inverterCount) {
		case ONE:
			System.out.println("Not allowed, there are only two inverters!");
			break;
		case TWO:
			System.out.println("Not allowed, there are only two inverters!");
			break;
		case THREE:
			this.commands.setEnableIpu3(enabled);
			break;
		}
	}

	@Override
	public void setBalancingMode(BalancingMode balancingMode) {
		this.commands.setBalancingMode(balancingMode);
	}

	@Override
	public void setMode(Mode mode) {
		this.commands.setMode(mode);
	}

	@Override
	public void setU0(float onGridVoltageFactor) {
		this.commands.setParameterU0(onGridVoltageFactor);
	}

	@Override
	public void setF0(float onGridFrequencyFactor) {
		this.commands.setParameterF0(onGridFrequencyFactor);
	}

	@Override
	public void setPControlMode(PControlMode pControlMode) {
		this.ccuParameters2.setpControlMode(pControlMode);
	}

	@Override
	public void setQLimit(float qLimit) {
		this.ccuParameters1.setqLimit(qLimit);
	}

	@Override
	public void setPMaxChargeIpu1(float maxPower) {
		this.ipu1Parameter.setpMaxCharge(maxPower);
	}

	@Override
	public void setPMaxDischargeIpu1(float maxPower) {
		this.ipu1Parameter.setpMaxDischarge(maxPower);
	}

	@Override
	public void setPMaxChargeIpu2(float maxPower) {
		this.ipu2Parameter.setpMaxCharge(maxPower);
	}

	@Override
	public void setPMaxDischargeIpu2(float maxPower) {
		this.ipu2Parameter.setpMaxDischarge(maxPower);
	}

	@Override
	public void setPMaxChargeIpu3(float maxPower) {
		this.ipu3Parameter.setpMaxCharge(maxPower);
	}

	@Override
	public void setPMaxDischargeIpu3(float maxPower) {
		this.ipu3Parameter.setpMaxDischarge(maxPower);
	}

	@Override
	public void setDcLinkVoltage(float dcLinkVoltageSetpoint) {
		this.dcDcParameter.setDcVoltageSetpoint(dcLinkVoltageSetpoint);
	}

	@Override
	public void setWeightStringA(Float weight) {
		this.dcDcParameter.setWeightStringA(weight);
	}

	@Override
	public void setWeightStringB(Float weight) {
		this.dcDcParameter.setWeightStringB(weight);
	}

	@Override
	public void setWeightStringC(Float weight) {
		this.dcDcParameter.setWeightStringC(weight);
	}

	@Override
	public void setStringControlMode(int stringControlMode) {
		this.dcDcParameter.setStringControlMode(stringControlMode);
	}

	@Override
	public void setIRefStringA(Float current) {
		this.dcDcParameter.setiRefStringA(current);
	}

	@Override
	public void setIRefStringB(Float current) {
		this.dcDcParameter.setiRefStringB(current);
	}

	@Override
	public void setIRefStringC(Float current) {
		this.dcDcParameter.setiRefStringC(current);
	}

	@Override
	public void enableDcDc() {
		switch (this.inverterCount) {
		case ONE:
			this.commands.setEnableIpu2(true);
			break;
		case TWO:
			this.commands.setEnableIpu3(true);
			break;
		case THREE:
			this.commands.setEnableIpu4(true);
			break;
		}
	}

	@Override
	public void disableDcDc() {
		switch (this.inverterCount) {
		case ONE:
			this.commands.setEnableIpu2(false);
			break;
		case TWO:
			this.commands.setEnableIpu3(false);
			break;
		case THREE:
			this.commands.setEnableIpu4(false);
			break;
		}
	}

	@Override
	public int getErrorCount() {
		return this.getInteger(GridConChannelId.CCU_ERROR_COUNT);
	}

	@Override
	public void setSyncDate(int date) {
		this.commands.setSyncDate(date);
	}

	@Override
	public void setSyncTime(int time) {
		this.commands.setSyncTime(time);
	}

	private <T> void writeValueToChannel(GridConChannelId channelId, T value)
			throws IllegalArgumentException, OpenemsNamedException {
		((WriteChannel<?>) channel(channelId)).setNextWriteValueFromObject(value);
	}

	private int getInteger(GridConChannelId id) {
		IntegerReadChannel c = this.channel(id);
		return c.value().orElse(Integer.MIN_VALUE);
	}

	@Override
	public boolean isStopped() {
		return this.getCcuState() == CcuState.SYNC_TO_V || this.getCcuState() == CcuState.IDLE_CURRENTLY_NOT_WORKING;
	}

	@Override
	public boolean isRunning() {
		return this.getCcuState() == CcuState.RUN || this.getCcuState() == CcuState.COMPENSATOR;
	}

	@Override
	public boolean isError() {
		return this.getCcuState() == CcuState.ERROR || this.isCommunicationBroken();
	}

	private CcuState getCcuState() {
		CcuState state = ((EnumReadChannel) this.channel(GridConChannelId.CCU_STATE)).value().asEnum();
		return state;
	}

	@Override
	public void setStop(boolean stop) {
		this.commands.setStopBit1st(stop);
		this.commands.setReadyAndStopBit2nd(stop);
		if (stop) { // only one command should be executed!
			System.out.println("only one command should be executed!");
			this.setPlay(false);
			this.setAcknowledge(false);
		}
	}

	@Override
	public void setAcknowledge(boolean acknowledge) {
		this.commands.setAcknowledgeBit(acknowledge);
		if (acknowledge) { // only one command should be executed!
			System.out.println("only one command should be executed!");
			this.setStop(false);
			this.setPlay(false);
		}
	}

	@Override
	public void setPlay(boolean play) {
		this.commands.setPlayBit(play);
		if (play) { // only one command should be executed!
			System.out.println("only one command should be executed!");
			this.setStop(false);
			this.setAcknowledge(false);
		}
	}

	@Override
	public boolean isDcDcStarted() {
		StatusIpuStateMachine state = ((EnumReadChannel) this.channel(GridConChannelId.DCDC_STATUS_STATE_MACHINE))
				.value().asEnum();
		return state == StatusIpuStateMachine.RUN;
	}

	@Override
	public boolean isIpusStarted(boolean enableIpu1, boolean enableIpu2, boolean enableIpu3) {
		boolean ret = true;

		if (enableIpu1) {
			ret = ret && this.isIpuRunning(GridConChannelId.INVERTER_1_STATUS_STATE_MACHINE);
		}

		if (enableIpu2) {
			ret = ret && this.isIpuRunning(GridConChannelId.INVERTER_2_STATUS_STATE_MACHINE);
		}

		if (enableIpu3) {
			ret = ret && this.isIpuRunning(GridConChannelId.INVERTER_3_STATUS_STATE_MACHINE);
		}

		return ret;
	}

	private boolean isIpuRunning(GridConChannelId id) {
		StatusIpuStateMachine state = ((EnumReadChannel) this.channel(id)).value().asEnum();
		return (state == StatusIpuStateMachine.RUN);
	}

	@Override
	public float getActivePowerPreset() {
		return this.activePowerPreset;
	}

	@Override
	public double getEfficiencyLossChargeFactor() {
		return this.efficiencyLossChargeFactor;
	}

	@Override
	public double getEfficiencyLossDischargeFactor() {
		return this.efficiencyLossDischargeFactor;
	}

	@Override
	public boolean isUndefined() {
		// TODO Check all relevant channels
		// Discussion -> What channels should be defined! All?
		// currently every used read only channel is checked
		boolean undefined = false;

		GridConChannelId[] ids = GridConChannelId.values();

		for (io.openems.edge.common.channel.ChannelId id : ids) {
			Channel<?> c = channel(id);

			if (c == null || c instanceof WriteChannel<?>) {
				break;
			}

			if (this.inverterCount.getCount() < 3
					&& id.id().toUpperCase().contains(GridconPcsImpl.NAME_PART_INVERTER_3)) { // skip inverter_3
				break;
			}

			if (this.inverterCount.getCount() < 2
					&& id.id().toUpperCase().contains(GridconPcsImpl.NAME_PART_INVERTER_2)) { // skip inverter_2
				break;
			}

			if (!c.value().isDefined()) {
				System.out.println("Value in Channel " + id.id() + " is not defined!");
				undefined = true;
				break;
			}
		}
		return undefined;
	}

	@Override
	public void setFundamentalFrequencyMode(FundamentalFrequencyMode fundamentalFrequencyMode) {
		this.commands.setFundamentalFrequencyMode(fundamentalFrequencyMode);
	}

	@Override
	public void setHarmonicCompensationMode(HarmonicCompensationMode harmonicCompensationMode) {
		this.commands.setHarmonicCompensationMode(harmonicCompensationMode);
	}

	@Override
	public float getCurrentL1Grid() {
		FloatReadChannel c = this.channel(GridConChannelId.GRID_MEASUREMENT_I_L1);
		return c.getNextValue().orElse(0f) * this.inverterCount.getCount() * NOMINAL_CURRENT_PER_UNIT;
	}

	@Override
	public float getCurrentL2Grid() {
		FloatReadChannel c = this.channel(GridConChannelId.GRID_MEASUREMENT_I_L2);
		return c.getNextValue().orElse(0f) * this.inverterCount.getCount() * NOMINAL_CURRENT_PER_UNIT;
	}

	@Override
	public float getCurrentL3Grid() {
		FloatReadChannel c = this.channel(GridConChannelId.GRID_MEASUREMENT_I_L3);
		return c.getNextValue().orElse(0f) * this.inverterCount.getCount() * NOMINAL_CURRENT_PER_UNIT;
	}

	@Override
	public float getCurrentLNGrid() {
		FloatReadChannel c = this.channel(GridConChannelId.GRID_MEASUREMENT_I_LN);
		return c.getNextValue().orElse(0f) * this.inverterCount.getCount() * NOMINAL_CURRENT_PER_UNIT;
	}

	@Override
	public float getActivePowerL1Grid() {
		FloatReadChannel c = this.channel(GridConChannelId.GRID_MEASUREMENT_P_L1);
		return c.getNextValue().orElse(0f) * this.inverterCount.getCount() * NOMINAL_POWER_PER_UNIT;
	}

	@Override
	public float getActivePowerL2Grid() {
		FloatReadChannel c = this.channel(GridConChannelId.GRID_MEASUREMENT_P_L2);
		return c.getNextValue().orElse(0f) * this.inverterCount.getCount() * NOMINAL_POWER_PER_UNIT;
	}

	@Override
	public float getActivePowerL3Grid() {
		FloatReadChannel c = this.channel(GridConChannelId.GRID_MEASUREMENT_P_L3);
		return c.getNextValue().orElse(0f) * this.inverterCount.getCount() * NOMINAL_POWER_PER_UNIT;
	}

	@Override
	public float getActivePowerSumGrid() {
		FloatReadChannel c = this.channel(GridConChannelId.GRID_MEASUREMENT_P_SUM);
		return c.getNextValue().orElse(0f) * this.inverterCount.getCount() * NOMINAL_POWER_PER_UNIT;
	}

	@Override
	public float getReactivePowerL1Grid() {
		FloatReadChannel c = this.channel(GridConChannelId.GRID_MEASUREMENT_Q_L1);
		return c.getNextValue().orElse(0f) * this.inverterCount.getCount() * NOMINAL_POWER_PER_UNIT;
	}

	@Override
	public float getReactivePowerL2Grid() {
		FloatReadChannel c = this.channel(GridConChannelId.GRID_MEASUREMENT_Q_L2);
		return c.getNextValue().orElse(0f) * this.inverterCount.getCount() * NOMINAL_POWER_PER_UNIT;
	}

	@Override
	public float getReactivePowerL3Grid() {
		FloatReadChannel c = this.channel(GridConChannelId.GRID_MEASUREMENT_Q_L3);
		return c.getNextValue().orElse(0f) * this.inverterCount.getCount() * NOMINAL_POWER_PER_UNIT;
	}

	@Override
	public float getReactivePowerSumGrid() {
		FloatReadChannel c = this.channel(GridConChannelId.GRID_MEASUREMENT_Q_SUM);
		return c.getNextValue().orElse(0f) * this.inverterCount.getCount() * NOMINAL_POWER_PER_UNIT;
	}

	@Override
	public float getApparentPowerL1Grid() {
		return (float) Math.sqrt(this.getActivePowerL1Grid() * this.getActivePowerL1Grid()
				+ this.getReactivePowerL1Grid() * this.getReactivePowerL1Grid());
	}

	@Override
	public float getApparentPowerL2Grid() {
		return (float) Math.sqrt(this.getActivePowerL2Grid() * this.getActivePowerL2Grid()
				+ this.getReactivePowerL2Grid() * this.getReactivePowerL2Grid());
	}

	@Override
	public float getApparentPowerL3Grid() {
		return (float) Math.sqrt(this.getActivePowerL3Grid() * this.getActivePowerL3Grid()
				+ this.getReactivePowerL3Grid() * this.getReactivePowerL3Grid());
	}

	@Override
	public float getApparentPowerSumGrid() {
		return (float) Math.sqrt(this.getActivePowerSumGrid() * this.getActivePowerSumGrid()
				+ this.getReactivePowerSumGrid() * this.getReactivePowerSumGrid());
	}

	@Override
	public void setCosPhiSetPoint1(float cosPhiSetPoint1) {
		this.cosPhiParameters.setCosPhiSetPoint1(cosPhiSetPoint1);
	}

	@Override
	public void setCosPhiSetPoint2(float cosPhiSetPoint2) {
		this.cosPhiParameters.setCosPhiSetPoint2(cosPhiSetPoint2);
	}
}
