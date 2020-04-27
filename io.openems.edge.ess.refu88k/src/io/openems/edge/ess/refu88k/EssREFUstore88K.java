package io.openems.edge.ess.refu88k;

import java.time.LocalDateTime;
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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.ess.refu88k.enums.OperatingState;
import io.openems.edge.ess.refu88k.enums.PCSSetOperation;
import io.openems.edge.ess.refu88k.enums.VArPctEna;
import io.openems.edge.ess.refu88k.enums.WMaxLimEna;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Refu.REFUstore88k", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE,//
		}

)
public class EssREFUstore88K extends AbstractOpenemsModbusComponent
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(EssREFUstore88K.class);
	private Config config;

	public static final int DEFAULT_UNIT_ID = 1;
	private int MAX_APPARENT_POWER = 0;
	protected static final double EFFICIENCY_FACTOR = 0.98;

	/*
	 * Initialize the variables isPowerAllowed and isPowerRequired
	 */
	private boolean isPowerAllowed = false;
	private boolean isPowerRequired = false;

	private LocalDateTime timeNoPower;

	@Reference
	private Power power;

	@Reference
	protected ConfigurationAdmin cm;

	private Battery battery;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setBattery(Battery battery) {
		this.battery = battery;
	}

	public EssREFUstore88K() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				REFUStore88KChannelId.values() //
		);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), DEFAULT_UNIT_ID, this.cm, "Modbus",
				config.modbus_id()); //
		this.initializeBattery(config.battery_id());
		this.config = config;
		this.channel(REFUStore88KChannelId.W_RTG).onChange((oldValue, newValue) -> {
			@SuppressWarnings("unchecked")
			Optional<Integer> valueOpt = (Optional<Integer>) newValue.asOptional();
			if (!valueOpt.isPresent()) {
				return;
			}
			MAX_APPARENT_POWER = valueOpt.get();
			IntegerWriteChannel wMaxChannel = this.channel(REFUStore88KChannelId.W_MAX);
			try {
				// Set WMax
				wMaxChannel.setNextWriteValue(MAX_APPARENT_POWER);
			} catch (OpenemsNamedException e) {
				log.error(e.getMessage());
			}
		});
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Initializes the connection to the Battery.
	 * 
	 * @param servicePid this components' Service-PID
	 * @param batteryId  the Component-ID of the Battery component
	 */
	private void initializeBattery(String batteryId) {
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Battery", batteryId)) {
			return;
		}

		this.battery.getSoc().onChange((oldValue, newValue) -> {
			this.getSoc().setNextValue(newValue.get());
			this.channel(REFUStore88KChannelId.BAT_SOC).setNextValue(newValue.get());
			this.channel(SymmetricEss.ChannelId.SOC).setNextValue(newValue.get());
		});

		this.battery.getVoltage().onChange((oldValue, newValue) -> {
			this.channel(REFUStore88KChannelId.BAT_VOLTAGE).setNextValue(newValue.get());
		});
	}

	/**
	 * 
	 * State machine for the case the inverter is configured ON!
	 * 
	 */
	private void handleStateMachine() {

		// by default: block Power
		this.isPowerAllowed = false;

		this.channel(SymmetricEss.ChannelId.MAX_APPARENT_POWER).setNextValue(MAX_APPARENT_POWER);

		EnumReadChannel operatingStateChannel = this.channel(REFUStore88KChannelId.ST);
		OperatingState operatingState = operatingStateChannel.value().asEnum();

		switch (operatingState) {
		case OFF:
			/*
			 * 1) Inverter is OFF (St = 1), because no DC voltage is applied 2) The EMS has
			 * to initiate a precharge of the DC link capacities of the inverter 3) The EMS
			 * closes the DC relais of the battery 4) The inverter control board starts
			 * (firmware booting) and enters the STANDBY state automatically
			 */
			break;
		case STANDBY:
			/*
			 * The inverter is initialised but not grid connected. The IGBT's are locked and
			 * AC relays are open.
			 */
			this.doStandbyHandling();
			break;
		case SLEEPING:
			/*
			 * Sleeping state is not used
			 */
			break;
		case STARTING:
			/*
			 * The inverter is connecting to the grid. It switches to STARTED state
			 * automatically
			 */
			break;
		case STARTED:
			/*
			 * The inverter is grid connected. AC Relays are closed. The IGBT's are locked.
			 */
			this.checkIfPowerIsAllowed();
			this.doGridConnectedHandling();
			break;
		case THROTTLED:
			/*
			 * The inverter feeds and derating is active. The IGBT's are working and AC
			 * relays are closed.
			 */
			this.checkIfPowerIsAllowed();
			this.checkTimeNoPowerRequired();
			this.doGridConnectedHandling();
			break;
		case MPPT:
			/*
			 * The inverter feeds with max possible power. The IGBT's are working and AC
			 * relays are closed.
			 */
			this.checkIfPowerIsAllowed();
			this.checkTimeNoPowerRequired();
			this.doGridConnectedHandling();
			break;
		case SHUTTING_DOWN:
			/*
			 * The inverter is shutting down. The IGBT's are locked and AC relays are open.
			 */
			break;
		case FAULT:
			/*
			 * The inverter is in fault state. The IGBT's are locked and AC relays are open.
			 */
			break;
		case UNDEFINED:
			// Do nothing because these states are only temporarily reached
			break;
		}
	}

	/**
	 * 
	 * State machine for the case the inverter is configured OFF!
	 * 
	 */

	private void offHandleStateMachine() {

		EnumReadChannel operatingStateChannel = this.channel(REFUStore88KChannelId.ST);
		OperatingState operatingState = operatingStateChannel.value().asEnum();

		switch (operatingState) {
		case OFF:
			break;
		case STANDBY:
			break;
		case SLEEPING:
			break;
		case STARTING:
			this.stopInverter();
			break;
		case STARTED:
			this.stopInverter();
			break;
		case THROTTLED:
			this.stopInverter();
			break;
		case MPPT:
			this.stopInverter();
			break;
		case SHUTTING_DOWN:
			break;
		case FAULT:
			break;
		case UNDEFINED:
			// Do nothing because these states are only temporarily reached
			break;
		}
	}

	/**
	 * 
	 * Checks all conditions if applying power is allowed!
	 * 
	 */
	private void checkIfPowerIsAllowed() {

		// If the battery system is not ready no power can be applied!
		this.isPowerAllowed = battery.getReadyForWorking().value().orElse(false);

		// Read important Channels from battery
		int optV = battery.getVoltage().value().orElse(0);
		int disMaxA = battery.getDischargeMaxCurrent().value().orElse(0);
		int chaMaxA = battery.getChargeMaxCurrent().value().orElse(0);

		// Calculate absolute Value allowedCharge and allowed Discharge from battery
		double absAllowedCharge = Math.abs((chaMaxA * optV) / (EFFICIENCY_FACTOR));
		double absAllowedDischarge = Math.abs((disMaxA * optV) * (EFFICIENCY_FACTOR));

		// Determine allowedCharge and allowedDischarge from Inverter
		if (absAllowedCharge > MAX_APPARENT_POWER) {
			this.getAllowedCharge().setNextValue(MAX_APPARENT_POWER * -1);
		} else {
			this.getAllowedCharge().setNextValue(absAllowedCharge * -1);
		}

		if (absAllowedDischarge > MAX_APPARENT_POWER) {
			this.getAllowedDischarge().setNextValue(MAX_APPARENT_POWER);
		} else {
			this.getAllowedDischarge().setNextValue(absAllowedDischarge);
		}
	}

	/**
	 * 
	 * Checks if power is required from the system!
	 * 
	 */
	private void checkIfPowerIsRequired(int activePower, int reactivePower) {
		if (activePower != 0 || reactivePower != 0) {
			this.isPowerRequired = true;
		} else {
			this.isPowerRequired = false;
		}
	}

	/**
	 * 
	 * If no power is required for the configured time the system enters STARTED
	 * state!
	 * 
	 */
	private void checkTimeNoPowerRequired() {
		if (!isPowerRequired) {
			if (timeNoPower == null) {
				timeNoPower = LocalDateTime.now();
			}
			if ((timeNoPower.plusSeconds(config.timeLimitNoPower())).isBefore(LocalDateTime.now())) {
				this.enterStartedMode();
			}
		} else {
			timeNoPower = null;
		}
	}

	/**
	 * 
	 * Do Standby operations!
	 * 
	 */
	private void doStandbyHandling() {
		this.isPowerAllowed = false;
		this.exitStandbyMode();
	}

	/**
	 * 
	 * Exit the STANDBY mode!
	 * 
	 */
	public void exitStandbyMode() {
		EnumWriteChannel pcsSetOperation = this.channel(REFUStore88KChannelId.PCS_SET_OPERATION);
		try {
			pcsSetOperation.setNextWriteValue(PCSSetOperation.EXIT_STANDBY_MODE);
		} catch (OpenemsNamedException e) {
			log.error("problem occurred while trying to exit standby mode" + e.getMessage());
		}
	}

	/**
	 * 
	 * Switch the system to THROTTLED or MPPT!
	 * 
	 */
	private void doGridConnectedHandling() {
		this.channel(SymmetricEss.ChannelId.GRID_MODE).setNextValue(GridMode.ON_GRID);
		if (this.getOperatingState().value().asEnum() == OperatingState.STARTED) {
			if (isPowerRequired && isPowerAllowed) {
				EnumWriteChannel pcsSetOperation = this.channel(REFUStore88KChannelId.PCS_SET_OPERATION);
				try {
					pcsSetOperation.setNextWriteValue(PCSSetOperation.START_PCS);
				} catch (OpenemsNamedException e) {
					log.error("problem occurred while trying to start grid mode" + e.getMessage());
				}
			}
		}
	}

	/**
	 * 
	 * Enter the STARTED mode!
	 * 
	 */
	private void enterStartedMode() {
		EnumWriteChannel pcsSetOperation = this.channel(REFUStore88KChannelId.PCS_SET_OPERATION);
		try {
			pcsSetOperation.setNextWriteValue(PCSSetOperation.STOP_PCS);
		} catch (OpenemsNamedException e) {
			log.error("problem occurred while trying to enter started mode" + e.getMessage());
		}
	}

	/**
	 * 
	 * STOP the inverter by setting the power to zero and entering the STARTED mode!
	 * 
	 */
	public void stopInverter() {

		this.isPowerAllowed = false;

		IntegerWriteChannel wMaxLimPctChannel = this.channel(REFUStore88KChannelId.W_MAX_LIM_PCT);
		EnumWriteChannel wMaxLim_EnaChannel = this.channel(REFUStore88KChannelId.W_MAX_LIM_ENA);

		IntegerWriteChannel varMaxLimPctChannel = this.channel(REFUStore88KChannelId.VAR_W_MAX_PCT);
		EnumWriteChannel varMaxLim_EnaChannel = this.channel(REFUStore88KChannelId.VAR_PCT_ENA);

		// Set Active Power to Zero
		try {
			wMaxLimPctChannel.setNextWriteValue(0);
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
		try {
			wMaxLim_EnaChannel.setNextWriteValue(WMaxLimEna.ENABLED);
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}

		// Set Reactive Power to Zero
		try {
			varMaxLimPctChannel.setNextWriteValue(0);
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
		try {
			varMaxLim_EnaChannel.setNextWriteValue(VArPctEna.ENABLED);
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}

		// Read the current values of Active and Reactive power!
		int currentActivePower = Math.abs(getActivePower().value().orElse(0));
		int currentReactivePower = Math.abs(getReactivePower().value().orElse(0));

		/*
		 * The self consumption of the inverter in status THROTTLED or MPPT is 40W.
		 * Checking if the current values are less than 50 WATT due to hysteresis!
		 */
		if (currentActivePower < 50 && currentReactivePower < 50) {
			EnumWriteChannel pcsSetOperation = this.channel(REFUStore88KChannelId.PCS_SET_OPERATION);
			try {
				pcsSetOperation.setNextWriteValue(PCSSetOperation.ENTER_STANDBY_MODE);
			} catch (OpenemsNamedException e) {
				log.error("problem occurred while trying to start grid mode" + e.getMessage());
			}
		} else {
			return;
		}

	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode) //
		);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			if (!config.inverterOff()) {
				this.handleStateMachine();
			} else {
				this.offHandleStateMachine();
			}
			break;
		}
	}

	@Override
	public Power getPower() {
		return this.power;
	}
//
//	public String getSerialNumber() {
//		return this.channel(REFUStore88KChannelId.SN).value().asString();
//	}
//
//	public Channel<Integer> getDcVoltage() {
//		return this.channel(REFUStore88KChannelId.DCV);
//	}
//
//	public Channel<Integer> getAcVoltage() {
//		return this.channel(REFUStore88KChannelId.PP_VPH_AB);
//	}
//
//	public Channel<Integer> getAcCurrent() {
//		return this.channel(REFUStore88KChannelId.A);
//	}

//	public Channel<Integer> getActivePower() {
//		return this.channel(REFUStore88KChannelId.W);
//	}
//
//	public Channel<Integer> getReactivePower() {
//		return this.channel(REFUStore88KChannelId.VA_R);
//	}
//
//	public Channel<Integer> getApparentPower() {
//		return this.channel(REFUStore88KChannelId.VA);
//	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {

		this.checkIfPowerIsRequired(activePower, reactivePower);

		int wSetPct = 0;
		int varSetPct = 0;
		int maxBatteryChargeValue = 0;
		int maxBatteryDischargeValue = 0;

		if (!this.isPowerAllowed || MAX_APPARENT_POWER == 0) {
			this.log.debug("Power is not allowed!");
			activePower = 0;
			reactivePower = 0;
		} else {
			// Calculate Active Power as a percentage of WMAX
			wSetPct = ((1000 * activePower) / MAX_APPARENT_POWER);
			// Calculate Reactive Power as a percentage of WMAX
			varSetPct = ((1000 * reactivePower) / MAX_APPARENT_POWER);

			maxBatteryChargeValue = battery.getChargeMaxCurrent().value().orElse(0);
			maxBatteryDischargeValue = battery.getDischargeMaxCurrent().value().orElse(0);
		}

		IntegerWriteChannel maxBatAChaChannel = this.channel(REFUStore88KChannelId.MAX_BAT_A_CHA);
		maxBatAChaChannel.setNextWriteValue(maxBatteryChargeValue);

		IntegerWriteChannel maxBatADischaChannel = this.channel(REFUStore88KChannelId.MAX_BAT_A_DISCHA);
		maxBatADischaChannel.setNextWriteValue(maxBatteryDischargeValue);

		IntegerWriteChannel wMaxLimPctChannel = this.channel(REFUStore88KChannelId.W_MAX_LIM_PCT);
		EnumWriteChannel wMaxLim_EnaChannel = this.channel(REFUStore88KChannelId.W_MAX_LIM_ENA);

		IntegerWriteChannel varMaxLimPctChannel = this.channel(REFUStore88KChannelId.VAR_W_MAX_PCT);
		EnumWriteChannel varMaxLim_EnaChannel = this.channel(REFUStore88KChannelId.VAR_PCT_ENA);

		wMaxLimPctChannel.setNextWriteValue(wSetPct);
		wMaxLim_EnaChannel.setNextWriteValue(WMaxLimEna.ENABLED);

		varMaxLimPctChannel.setNextWriteValue(varSetPct);
		varMaxLim_EnaChannel.setNextWriteValue(VArPctEna.ENABLED);

	}

	@Override
	public Constraint[] getStaticConstraints() throws OpenemsException {
		if (this.isPowerAllowed) {
			return Power.NO_CONSTRAINTS;
		} else {
			return new Constraint[] {
					this.createPowerConstraint("REFU inverter not ready", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS,
							0),
					this.createPowerConstraint("Reactive power is not allowed", Phase.ALL, Pwr.REACTIVE,
							Relationship.EQUALS, 0) };
		}
	}

	@Override
	public int getPowerPrecision() {
		return MAX_APPARENT_POWER / 1000;
	}

	public Channel<OperatingState> getOperatingState() {
		return this.channel(REFUStore88KChannelId.ST);
	}

	/*
	 * Supported Models First available Model = Start Address + 2 = 40002 Then 40002
	 * + Length of Model ....
	 */
	private final static int START_ADDRESS = 40000;
	private final static int SUNSPEC_1 = START_ADDRESS + 2; // Common
	private final static int SUNSPEC_103 = 40070; // Inverter (Three Phase)
	private final static int SUNSPEC_120 = 40122; // Nameplate
	private final static int SUNSPEC_121 = 40150; // Basic Settings
	private final static int SUNSPEC_123 = 40182; // Immediate Controls
	private final static int SUNSPEC_64040 = 40208; // REFU Parameter
	private final static int SUNSPEC_64041 = 40213; // REFU Parameter Value
	private final static int SUNSPEC_64800 = 40225; // MESA-PCS Extensions

	@Override
	protected ModbusProtocol defineModbusProtocol() { // Register
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(SUNSPEC_1, Priority.ONCE, //
						m(REFUStore88KChannelId.ID_1, new UnsignedWordElement(SUNSPEC_1)), // 40002
						m(REFUStore88KChannelId.L_1, new UnsignedWordElement(SUNSPEC_1 + 1)), // 40003
						m(REFUStore88KChannelId.MN, new StringWordElement(SUNSPEC_1 + 2, 16)), // 40004
						m(REFUStore88KChannelId.MD, new StringWordElement(SUNSPEC_1 + 18, 16)), // 40020
						m(REFUStore88KChannelId.OPT, new StringWordElement(SUNSPEC_1 + 34, 8)), // 40036
						m(REFUStore88KChannelId.VR, new StringWordElement(SUNSPEC_1 + 42, 8)), // 40044
						m(REFUStore88KChannelId.SN, new StringWordElement(SUNSPEC_1 + 50, 16)), // 40052
						m(REFUStore88KChannelId.DA, new UnsignedWordElement(SUNSPEC_1 + 66)), // 40068
						m(REFUStore88KChannelId.PAD_1, new UnsignedWordElement(SUNSPEC_1 + 67))), // 40069

				new FC3ReadRegistersTask(SUNSPEC_103, Priority.HIGH, //
						m(REFUStore88KChannelId.ID_103, new UnsignedWordElement(SUNSPEC_103)), // 40070
						m(REFUStore88KChannelId.L_103, new UnsignedWordElement(SUNSPEC_103 + 1)), // 40071
						m(REFUStore88KChannelId.A, new UnsignedWordElement(SUNSPEC_103 + 2), // 40072
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(REFUStore88KChannelId.APH_A, new UnsignedWordElement(SUNSPEC_103 + 3), // 40073
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(REFUStore88KChannelId.APH_B, new UnsignedWordElement(SUNSPEC_103 + 4), // 40074
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(REFUStore88KChannelId.APH_C, new UnsignedWordElement(SUNSPEC_103 + 5), // 40075
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(REFUStore88KChannelId.A_SF, new UnsignedWordElement(SUNSPEC_103 + 6)), // 40076
						m(REFUStore88KChannelId.PP_VPH_AB, new UnsignedWordElement(SUNSPEC_103 + 7), // 40077
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(REFUStore88KChannelId.PP_VPH_BC, new UnsignedWordElement(SUNSPEC_103 + 8), // 40078
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(REFUStore88KChannelId.PP_VPH_CA, new UnsignedWordElement(SUNSPEC_103 + 9), // 40079
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(REFUStore88KChannelId.PH_VPH_A, new UnsignedWordElement(SUNSPEC_103 + 10), // 40080
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(REFUStore88KChannelId.PH_VPH_B, new UnsignedWordElement(SUNSPEC_103 + 11), // 40081
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(REFUStore88KChannelId.PH_VPH_C, new UnsignedWordElement(SUNSPEC_103 + 12), // 40082
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(REFUStore88KChannelId.V_SF, new UnsignedWordElement(SUNSPEC_103 + 13)), // 40083
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedWordElement(SUNSPEC_103 + 14), // 40084 //
								ElementToChannelConverter.SCALE_FACTOR_1), // REFUStore88KChannelId.W//
						m(REFUStore88KChannelId.W_SF, new SignedWordElement(SUNSPEC_103 + 15)), // 40085
						m(REFUStore88KChannelId.HZ, new SignedWordElement(SUNSPEC_103 + 16), // 40086
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(REFUStore88KChannelId.HZ_SF, new SignedWordElement(SUNSPEC_103 + 17)), // 40087
						m(REFUStore88KChannelId.VA, new SignedWordElement(SUNSPEC_103 + 18), // 40088
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(REFUStore88KChannelId.VA_SF, new SignedWordElement(SUNSPEC_103 + 19)), // 40089
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new SignedWordElement(SUNSPEC_103 + 20), // 40090 //
								ElementToChannelConverter.SCALE_FACTOR_1), // REFUStore88KChannelId.VA_R
						m(REFUStore88KChannelId.VA_R_SF, new SignedWordElement(SUNSPEC_103 + 21)), // 40091
						new DummyRegisterElement(SUNSPEC_103 + 22, SUNSPEC_103 + 23),
						m(REFUStore88KChannelId.WH, new UnsignedDoublewordElement(SUNSPEC_103 + 24), // 40094
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(REFUStore88KChannelId.WH_SF, new UnsignedWordElement(SUNSPEC_103 + 26)), // 40096
						m(REFUStore88KChannelId.DCA, new SignedWordElement(SUNSPEC_103 + 27), // 40097
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(REFUStore88KChannelId.DCA_SF, new UnsignedWordElement(SUNSPEC_103 + 28)), // 40098
						m(REFUStore88KChannelId.DCV, new UnsignedWordElement(SUNSPEC_103 + 29), // 40099
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(REFUStore88KChannelId.DCV_SF, new UnsignedWordElement(SUNSPEC_103 + 30)), // 40100
						m(REFUStore88KChannelId.DCW, new SignedWordElement(SUNSPEC_103 + 31), // 40101
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(REFUStore88KChannelId.DCW_SF, new SignedWordElement(SUNSPEC_103 + 32)), // 40102
						m(REFUStore88KChannelId.TMP_CAB, new SignedWordElement(SUNSPEC_103 + 33), // 40103
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(REFUStore88KChannelId.TMP_SNK, new SignedWordElement(SUNSPEC_103 + 34), // 40104
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						new DummyRegisterElement(SUNSPEC_103 + 35, SUNSPEC_103 + 36),
						m(REFUStore88KChannelId.TMP_SF, new UnsignedWordElement(SUNSPEC_103 + 37)), // 40107
						m(REFUStore88KChannelId.ST, new UnsignedWordElement(SUNSPEC_103 + 38)), // 40108
						m(REFUStore88KChannelId.ST_VND, new UnsignedWordElement(SUNSPEC_103 + 39)), // 40109
						m(new BitsWordElement(SUNSPEC_103 + 40, this) //
								.bit(0, REFUStore88KChannelId.GROUND_FAULT) //
								.bit(1, REFUStore88KChannelId.DC_OVER_VOLTAGE) //
								.bit(2, REFUStore88KChannelId.AC_DISCONNECT) //
								.bit(3, REFUStore88KChannelId.DC_DISCONNECT) //
								.bit(4, REFUStore88KChannelId.GRID_DISCONNECT) //
								.bit(5, REFUStore88KChannelId.CABINET_OPEN) //
								.bit(6, REFUStore88KChannelId.MANUAL_SHUTDOWN) //
								.bit(7, REFUStore88KChannelId.OVER_TEMP) //
								.bit(8, REFUStore88KChannelId.OVER_FREQUENCY) //
								.bit(9, REFUStore88KChannelId.UNDER_FREQUENCY) //
								.bit(10, REFUStore88KChannelId.AC_OVER_VOLT) //
								.bit(11, REFUStore88KChannelId.AC_UNDER_VOLT) //
								.bit(12, REFUStore88KChannelId.BLOWN_STRING_FUSE) //
								.bit(13, REFUStore88KChannelId.UNDER_TEMP) //
								.bit(14, REFUStore88KChannelId.MEMORY_LOSS) //
								.bit(15, REFUStore88KChannelId.HW_TEST_FAILURE) //
						), //
						m(new BitsWordElement(SUNSPEC_103 + 41, this) //
								.bit(0, REFUStore88KChannelId.OTHER_ALARM) //
								.bit(1, REFUStore88KChannelId.OTHER_WARNING) //
						), //
						m(REFUStore88KChannelId.EVT_2, new UnsignedDoublewordElement(SUNSPEC_103 + 42)), // 40112
						m(REFUStore88KChannelId.EVT_VND_1, new UnsignedDoublewordElement(SUNSPEC_103 + 44)), // 40114
						m(REFUStore88KChannelId.EVT_VND_2, new UnsignedDoublewordElement(SUNSPEC_103 + 46)), // 40116
						m(REFUStore88KChannelId.EVT_VND_3, new UnsignedDoublewordElement(SUNSPEC_103 + 48)), // 40118
						m(REFUStore88KChannelId.EVT_VND_4, new UnsignedDoublewordElement(SUNSPEC_103 + 50))), // 40120

//				new FC16WriteRegistersTask(SUNSPEC_103 + 40,
//						m(new BitsWordElement(SUNSPEC_103 + 40, this) //
//								.bit(0, REFUStore88KChannelId.GROUND_FAULT) //
//								.bit(1, REFUStore88KChannelId.DC_OVER_VOLTAGE) //
//								.bit(2, REFUStore88KChannelId.AC_DISCONNECT) //
//								.bit(3, REFUStore88KChannelId.DC_DISCONNECT) //
//								.bit(4, REFUStore88KChannelId.GRID_DISCONNECT) //
//								.bit(5, REFUStore88KChannelId.CABINET_OPEN) //
//								.bit(6, REFUStore88KChannelId.MANUAL_SHUTDOWN) //
//								.bit(7, REFUStore88KChannelId.OVER_TEMP) //
//								.bit(8, REFUStore88KChannelId.OVER_FREQUENCY) //
//								.bit(9, REFUStore88KChannelId.UNDER_FREQUENCY) //
//								.bit(10, REFUStore88KChannelId.AC_OVER_VOLT) //
//								.bit(11, REFUStore88KChannelId.AC_UNDER_VOLT) //
//								.bit(12, REFUStore88KChannelId.BLOWN_STRING_FUSE) //
//								.bit(13, REFUStore88KChannelId.UNDER_TEMP) //
//								.bit(14, REFUStore88KChannelId.MEMORY_LOSS) //
//								.bit(15, REFUStore88KChannelId.HW_TEST_FAILURE) //
//								),//
//						m(new BitsWordElement(SUNSPEC_103 + 41, this) //
//								.bit(0, REFUStore88KChannelId.OTHER_ALARM) //
//								.bit(1, REFUStore88KChannelId.OTHER_WARNING) //
//								)//
//						),

				new FC3ReadRegistersTask(SUNSPEC_120, Priority.LOW, //
						m(REFUStore88KChannelId.ID_120, new UnsignedWordElement(SUNSPEC_120)), // 40122
						m(REFUStore88KChannelId.L_120, new UnsignedWordElement(SUNSPEC_120 + 1)), // 40123
						m(REFUStore88KChannelId.DER_TYP, new UnsignedWordElement(SUNSPEC_120 + 2)), // 40124
						m(REFUStore88KChannelId.W_RTG, new UnsignedWordElement(SUNSPEC_120 + 3), // 40125
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(REFUStore88KChannelId.W_RTG_SF, new UnsignedWordElement(SUNSPEC_120 + 4)), // 40126
						m(REFUStore88KChannelId.VA_RTG, new UnsignedWordElement(SUNSPEC_120 + 5), // 40127
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(REFUStore88KChannelId.VA_RTG_SF, new UnsignedWordElement(SUNSPEC_120 + 6)), // 40128
						m(REFUStore88KChannelId.VAR_RTG_Q1, new SignedWordElement(SUNSPEC_120 + 7), // 40129
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(REFUStore88KChannelId.VAR_RTG_Q2, new SignedWordElement(SUNSPEC_120 + 8), // 40130
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(REFUStore88KChannelId.VAR_RTG_Q3, new SignedWordElement(SUNSPEC_120 + 9), // 40131
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(REFUStore88KChannelId.VAR_RTG_Q4, new SignedWordElement(SUNSPEC_120 + 10), // 40132
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(REFUStore88KChannelId.VAR_RTG_SF, new SignedWordElement(SUNSPEC_120 + 11)), // 40133
						m(REFUStore88KChannelId.A_RTG, new UnsignedWordElement(SUNSPEC_120 + 12), // 40134
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(REFUStore88KChannelId.A_RTG_SF, new SignedWordElement(SUNSPEC_120 + 13)), // 40135
						m(REFUStore88KChannelId.PF_RTG_Q1, new SignedWordElement(SUNSPEC_120 + 14), // 40136
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
						m(REFUStore88KChannelId.PF_RTG_Q2, new SignedWordElement(SUNSPEC_120 + 15), // 40137
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
						m(REFUStore88KChannelId.PF_RTG_Q3, new SignedWordElement(SUNSPEC_120 + 16), // 40138
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
						m(REFUStore88KChannelId.PF_RTG_Q4, new SignedWordElement(SUNSPEC_120 + 17), // 40139
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
						m(REFUStore88KChannelId.PF_RTG_SF, new SignedWordElement(SUNSPEC_120 + 18)), // 40140
						new DummyRegisterElement(SUNSPEC_120 + 19, SUNSPEC_120 + 26),
						m(REFUStore88KChannelId.PAD_120, new SignedWordElement(SUNSPEC_120 + 27))), // 40149

				new FC3ReadRegistersTask(SUNSPEC_121, Priority.LOW, //
						m(REFUStore88KChannelId.ID_121, new UnsignedWordElement(SUNSPEC_121)), // 40150
						m(REFUStore88KChannelId.L_121, new UnsignedWordElement(SUNSPEC_121 + 1)), // 40151
						new DummyRegisterElement(SUNSPEC_121 + 2, SUNSPEC_121 + 21),
						m(REFUStore88KChannelId.W_MAX_SF, new UnsignedWordElement(SUNSPEC_121 + 22)), // 40172
						m(REFUStore88KChannelId.V_REF_SF, new UnsignedWordElement(SUNSPEC_121 + 23)), // 40173
						m(REFUStore88KChannelId.V_REF_OFS_SF, new UnsignedWordElement(SUNSPEC_121 + 24))), // 40174

				new FC16WriteRegistersTask(SUNSPEC_121 + 2, //
						m(REFUStore88KChannelId.W_MAX, new UnsignedWordElement(SUNSPEC_121 + 2), // 40152
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(REFUStore88KChannelId.V_REF, new UnsignedWordElement(SUNSPEC_121 + 3), // 40153
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(REFUStore88KChannelId.V_REF_OFS, new UnsignedWordElement(SUNSPEC_121 + 4), // 40154
								ElementToChannelConverter.SCALE_FACTOR_1)),

				new FC3ReadRegistersTask(SUNSPEC_123, Priority.LOW, //
						m(REFUStore88KChannelId.ID_123, new UnsignedWordElement(SUNSPEC_123)), // 40182
						m(REFUStore88KChannelId.L_123, new UnsignedWordElement(SUNSPEC_123 + 1)), // 40183
						new DummyRegisterElement(SUNSPEC_123 + 2, SUNSPEC_123 + 22),
						m(REFUStore88KChannelId.W_MAX_LIM_PCT_SF, new UnsignedWordElement(SUNSPEC_123 + 23)), // 40205
						m(REFUStore88KChannelId.OUT_PF_SET_SF, new UnsignedWordElement(SUNSPEC_123 + 24)), // 40206
						m(REFUStore88KChannelId.VAR_PCT_SF, new UnsignedWordElement(SUNSPEC_123 + 25))), // 40207

				new FC16WriteRegistersTask(SUNSPEC_123 + 4, //
						m(REFUStore88KChannelId.CONN, new UnsignedWordElement(SUNSPEC_123 + 4)), // 40186
						m(REFUStore88KChannelId.W_MAX_LIM_PCT, new SignedWordElement(SUNSPEC_123 + 5))), // 40187

				new FC16WriteRegistersTask(SUNSPEC_123 + 9, //
						m(REFUStore88KChannelId.W_MAX_LIM_ENA, new UnsignedWordElement(SUNSPEC_123 + 9)), // 40191
						m(REFUStore88KChannelId.OUT_PF_SET, new SignedWordElement(SUNSPEC_123 + 10), // 40192
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3)),

				new FC16WriteRegistersTask(SUNSPEC_123 + 14, //
						m(REFUStore88KChannelId.OUT_PF_SET_ENA, new UnsignedWordElement(SUNSPEC_123 + 14)), // 40196
						m(REFUStore88KChannelId.VAR_W_MAX_PCT, new SignedWordElement(SUNSPEC_123 + 15), // 40197
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),

				new FC16WriteRegistersTask(SUNSPEC_123 + 22, //
						m(REFUStore88KChannelId.VAR_PCT_ENA, new UnsignedWordElement(SUNSPEC_123 + 22))), // 40204

				new FC3ReadRegistersTask(SUNSPEC_64040, Priority.LOW, //
						m(REFUStore88KChannelId.ID_64040, new UnsignedWordElement(SUNSPEC_64040)), // 40208
						m(REFUStore88KChannelId.L_64040, new UnsignedWordElement(SUNSPEC_64040 + 1))), // 40209

				new FC16WriteRegistersTask(SUNSPEC_64040 + 2, //
						m(REFUStore88KChannelId.READ_WRITE_PARAM_ID, new UnsignedDoublewordElement(SUNSPEC_64040 + 2)), // 40210
						m(REFUStore88KChannelId.READ_WRITE_PARAM_INDEX,
								new UnsignedDoublewordElement(SUNSPEC_64040 + 4))), // 40212

				new FC3ReadRegistersTask(SUNSPEC_64041, Priority.LOW, //
						m(REFUStore88KChannelId.ID_64041, new UnsignedWordElement(SUNSPEC_64041)), // 40213
						m(REFUStore88KChannelId.L_64041, new UnsignedWordElement(SUNSPEC_64041 + 1))), // 40214

				new FC16WriteRegistersTask(SUNSPEC_64041 + 2, //
						m(REFUStore88KChannelId.READ_WRITE_PARAM_VALUE_U32,
								new UnsignedDoublewordElement(SUNSPEC_64041 + 2)), // 40215
						m(REFUStore88KChannelId.READ_WRITE_PARAM_VALUE_S32,
								new SignedDoublewordElement(SUNSPEC_64041 + 4)), // 40217
						m(REFUStore88KChannelId.READ_WRITE_PARAM_VALUE_F32,
								new SignedDoublewordElement(SUNSPEC_64041 + 6)), // 40219
						m(REFUStore88KChannelId.READ_WRITE_PARAM_VALUE_U16, new UnsignedWordElement(SUNSPEC_64041 + 8)), // 40221
						m(REFUStore88KChannelId.READ_WRITE_PARAM_VALUE_S16, new SignedWordElement(SUNSPEC_64041 + 9)), // 40222
						m(REFUStore88KChannelId.READ_WRITE_PARAM_VALUE_U8, new UnsignedWordElement(SUNSPEC_64041 + 10)), // 40223
						m(REFUStore88KChannelId.READ_WRITE_PARAM_VALUE_S8, new SignedWordElement(SUNSPEC_64041 + 11))), // 40224

				new FC16WriteRegistersTask(SUNSPEC_64800, //
						m(REFUStore88KChannelId.ID_64800, new UnsignedWordElement(SUNSPEC_64800)), // 40225
						m(REFUStore88KChannelId.L_64800, new UnsignedWordElement(SUNSPEC_64800 + 1)), // 40226
						m(REFUStore88KChannelId.LOC_REM_CTL, new SignedWordElement(SUNSPEC_64800 + 2))), // 40227

				new FC3ReadRegistersTask(SUNSPEC_64800 + 3, Priority.LOW, //
						m(REFUStore88KChannelId.PCS_HB, new SignedWordElement(SUNSPEC_64800 + 3)), // 40228
						m(REFUStore88KChannelId.CONTROLLER_HB, new SignedWordElement(SUNSPEC_64800 + 4)), // 40229
						new DummyRegisterElement(SUNSPEC_64800 + 5)),

				new FC16WriteRegistersTask(SUNSPEC_64800 + 6, //
						m(REFUStore88KChannelId.PCS_SET_OPERATION, new SignedWordElement(SUNSPEC_64800 + 6)), // 40231
						m(REFUStore88KChannelId.MAX_BAT_A_CHA, new UnsignedWordElement(SUNSPEC_64800 + 7), // 40232
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(REFUStore88KChannelId.MAX_BAT_A_DISCHA, new UnsignedWordElement(SUNSPEC_64800 + 8), // 40233
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(REFUStore88KChannelId.MAX_A, new UnsignedWordElement(SUNSPEC_64800 + 9)), // 40234
						m(REFUStore88KChannelId.MAX_A_CUR, new UnsignedWordElement(SUNSPEC_64800 + 10)), // 40235
						m(REFUStore88KChannelId.MAX_BAT_A_SF, new SignedWordElement(SUNSPEC_64800 + 11)), // 40236
						m(REFUStore88KChannelId.MAX_A_SF, new SignedWordElement(SUNSPEC_64800 + 12)), // 40237
						m(REFUStore88KChannelId.MAX_A_CUR_SF, new SignedWordElement(SUNSPEC_64800 + 13)), // 40238
						m(REFUStore88KChannelId.PADDING_1, new SignedWordElement(SUNSPEC_64800 + 14)), // 40239
						m(REFUStore88KChannelId.PADDING_2, new SignedWordElement(SUNSPEC_64800 + 15)))); // 40240

	}

	@Override
	public String debugLog() {
		return "State:" + this.channel(REFUStore88KChannelId.ST).value().asOptionString() //
				+ " | Active Power:" + this.channel(SymmetricEss.ChannelId.ACTIVE_POWER).value().asString() //
				+ " | Reactive Power:" + this.channel(SymmetricEss.ChannelId.REACTIVE_POWER).value().asString() //
				+ " | Allowed Charge:" + this.getAllowedCharge().value() //
				+ " | Allowed Discharge:" + this.getAllowedDischarge().value() //
				+ " | Allowed ChargeCurrent:" + this.battery.getChargeMaxCurrent() //
				+ " | Allowed DischargeCurrent:" + this.battery.getDischargeMaxCurrent() //
				+ " | DC Voltage:" + this.channel(REFUStore88KChannelId.DCV).value().asString() //
		;
	}
}
