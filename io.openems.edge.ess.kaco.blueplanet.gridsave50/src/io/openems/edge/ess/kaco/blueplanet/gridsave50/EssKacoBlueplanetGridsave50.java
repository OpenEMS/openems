package io.openems.edge.ess.kaco.blueplanet.gridsave50;

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
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
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
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.OptionsEnum;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.CircleConstraint;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.Kaco.BlueplanetGridsave50", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
) //
public class EssKacoBlueplanetGridsave50 extends AbstractOpenemsModbusComponent
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(EssKacoBlueplanetGridsave50.class);

	protected static final int MAX_APPARENT_POWER = 52000;

	private CircleConstraint maxApparentPowerConstraint = null;
	private int watchdogInterval = 0;
	private int maxApparentPower = 0;
	private int maxApparentPowerUnscaled = 0;
	private int maxApparentPowerScaleFactor = 0;

	@Reference
	private Power power;
	private Battery battery;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setBattery(Battery battery) {
		this.battery = battery;
	}

	public EssKacoBlueplanetGridsave50() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	private void refreshPower() {
		maxApparentPower = maxApparentPowerUnscaled * maxApparentPowerScaleFactor;
		if (maxApparentPower > 0) {
			this.maxApparentPowerConstraint.setRadius(maxApparentPower);
			this.channel(SymmetricEss.ChannelId.MAX_ACTIVE_POWER).setNextValue(maxApparentPower); // TODO check if right, should be, cause max_act = max_app if cos phi = 1
		}
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		int UNIT_ID = 1; // TODO ?
		super.activate(context, config.service_pid(), config.id(), config.enabled(), UNIT_ID, this.cm, "Modbus",
				config.modbus_id());
		// update filter for 'battery'
		if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "battery", config.battery_id())) {
			return;
		}

		watchdogInterval = config.watchdoginterval();
		
		doChannelMapping();
		initializePower();
	}

	private void initializePower() {
		// TODO adjust apparent power from modbus element
		this.maxApparentPowerConstraint = new CircleConstraint(this, MAX_APPARENT_POWER);

		this.channel(ChannelId.W_MAX).onChange(value -> {
			// TODO unchecked cast
			@SuppressWarnings("unchecked")
			Optional<Integer> valueOpt = (Optional<Integer>) value.asOptional();
			if (!valueOpt.isPresent()) {
				return;
			}
			maxApparentPowerUnscaled = TypeUtils.getAsType(OpenemsType.INTEGER, value);
			refreshPower();
		});
		this.channel(ChannelId.W_MAX_SF).onChange(value -> {
//			TODO unchecked cast
			@SuppressWarnings("unchecked")
			Optional<Integer> valueOpt = (Optional<Integer>) value.asOptional();
			if (!valueOpt.isPresent()) {
				return;
			}
			Integer i = TypeUtils.getAsType(OpenemsType.INTEGER, value);
			maxApparentPowerScaleFactor = (int) Math.pow(10, i);
			refreshPower();
		});
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
		if (!isSystemInGridmode()) { // necessary? or should we set these values always?
			return;
		}

		// according to manual active power has to be set in % of maximum active power
		// with scale factor see page 10
		// WSetPct = (WSet_Watt * 100) / ( W_Max_unscaled * 10^W_Max_SF * 10^WSetPct_SF)

		IntegerWriteChannel wSetPctChannel = this.channel(ChannelId.W_SET_PCT);
		IntegerReadChannel wSetPct_SFChannel = this.channel(ChannelId.W_SET_PCT_SF);

		Optional<Integer> wSetPctOpt = wSetPct_SFChannel.value().asOptional();
		if (wSetPctOpt.isPresent()) {

			int scalefactor = wSetPctOpt.get();

			int max = maxApparentPower; // TODO ensure that this value is not null
			if (max == 0) {
				max = MAX_APPARENT_POWER;
			}

			int WSetPct = (int) ((activePower * 100) / (max * Math.pow(10, scalefactor)));

			try {
				wSetPctChannel.setNextWriteValue(WSetPct);
			} catch (OpenemsException e) {
				log.error("problem occurred while trying so set active power" + e.getMessage());
			}
		}
	}

	private void handleStateMachine() {
		IntegerReadChannel currentStateChannel = this.channel(ChannelId.CURRENT_STATE);
		Optional<Enum<?>> currentStateOpt = currentStateChannel.value().asEnumOptional();
		if (!currentStateOpt.isPresent()) {
			return;
		}

		CurrentState currentState = (CurrentState) currentStateOpt.get();

		switch (currentState) {
		case OFF:
			doOffHandling();
			break;
		case STANDBY:
			doStandbyHandling();
			break;
		case ERROR:
			doErrorHandling();
			break;
		case GRID_CONNECTED:
			doGridConnectedHandling();
			break;
		case PRECHARGE:
		case NO_ERROR_PENDING:
		case SHUTTING_DOWN:
		case STARTING:
		case THROTTLED:
			// Do nothing because these states are only temporarily reached
			break;
		}
	}

	private void doStandbyHandling() {
		setWatchdog();
		setBatteryRanges();
		startGridMode();
	}

	private void doOffHandling() {
		setWatchdog();
		startSystem();
		setBatteryRanges();
	}

	private void doGridConnectedHandling() {
		setWatchdog();
		setBatteryRanges();
	}

	private void setBatteryRanges() {
		if (battery == null) {
			return;
		}

		int disMinV = battery.getDischargeMinVoltage().value().orElse(0);
		int chaMaxV = battery.getChargeMaxVoltage().value().orElse(0);
		int disMaxA = battery.getDischargeMaxCurrent().value().orElse(0);
		int chaMaxA = battery.getChargeMaxCurrent().value().orElse(0);

		if (disMinV == 0 || chaMaxV == 0) {
			return; // according to setup manual 64202.DisMinV and 64202.ChaMaxV must not be zero
		}

		IntegerWriteChannel disMinVChannel = this.channel(ChannelId.DIS_MIN_V);
		IntegerWriteChannel disMaxAChannel = this.channel(ChannelId.DIS_MAX_A);
		IntegerWriteChannel chaMaxVChannel = this.channel(ChannelId.CHA_MAX_V);
		IntegerWriteChannel chaMaxAChannel = this.channel(ChannelId.CHA_MAX_A);
		IntegerWriteChannel enLimitChannel = this.channel(ChannelId.EN_LIMIT);

		try {
			log.info(" ===============  BATTERY RANGES  ========================");
			log.info("DIS MIN V: " + disMinV);
			log.info("DIS MAX A: " + disMaxA);
			log.info("CHA MAX V: " + chaMaxV);
			log.info("CHA MAX A: " + chaMaxA);
			
			disMinVChannel.setNextWriteValue(disMinV);
			chaMaxVChannel.setNextWriteValue(chaMaxV);
			disMaxAChannel.setNextWriteValue(disMaxA);
			chaMaxAChannel.setNextWriteValue(chaMaxA);

			enLimitChannel.setNextWriteValue(1);
		} catch (OpenemsException e) {
			log.error("Error during setBatteryRanges, " + e.getMessage());
		}
	}

	private void doErrorHandling() {
		// find out the reason what is wrong an react
		// for a first try, switch system off, it will be restarted
		setWatchdog();
		stopSystem();
	}

	@Override
	public String debugLog() {
		return "State:" + this.channel(ChannelId.CURRENT_STATE).value().asOptionString() // 
				+ ",L:" + this.channel(SymmetricEss.ChannelId.ACTIVE_POWER).value().asString() // 
				;
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	private boolean isSystemInGridmode() {
		IntegerReadChannel currentStateChannel = this.channel(ChannelId.CURRENT_STATE);
		Optional<Enum<?>> currentStateOpt = currentStateChannel.value().asEnumOptional();
		return currentStateOpt.isPresent() && currentStateOpt.get() == CurrentState.GRID_CONNECTED;
	}

	@Override
	public int getPowerPrecision() {
		IntegerReadChannel wSetPct_SFChannel = this.channel(ChannelId.W_SET_PCT_SF);
		Optional<Integer> wSetPctOpt = wSetPct_SFChannel.value().asOptional();
		int scalefactor = wSetPctOpt.orElse(0);
		return (int) (MAX_APPARENT_POWER * 0.01 * Math.pow(10, scalefactor));
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			handleStateMachine();
			break;
		}
	}

	private void startGridMode() {
		IntegerWriteChannel requestedState = this.channel(ChannelId.REQUESTED_STATE);
		try {
			requestedState.setNextWriteValue(RequestedState.GRID_CONNECTED.value);
		} catch (OpenemsException e) {
			log.error("problem occurred while trying to start grid mode" + e.getMessage());
		}
	}

	private void startSystem() {
		IntegerWriteChannel requestedState = this.channel(ChannelId.REQUESTED_STATE);
		try {
			requestedState.setNextWriteValue(RequestedState.STANDBY.value);
		} catch (OpenemsException e) {
			log.error("problem occurred while trying to start inverter" + e.getMessage());
		}
	}

	private void stopSystem() {
		IntegerWriteChannel requestedState = this.channel(ChannelId.REQUESTED_STATE);
		try {
			requestedState.setNextWriteValue(RequestedState.OFF.value);
		} catch (OpenemsException e) {
			log.error("problem occurred while trying to stop system" + e.getMessage());
		}
	}

	private void setWatchdog() {
		// according to 3.5.2.2 in the manual write watchdog register
		IntegerWriteChannel watchdogChannel = this.channel(ChannelId.WATCHDOG);
		try {
			watchdogChannel.setNextWriteValue(watchdogInterval);
		} catch (OpenemsException e) {
			log.error("Watchdog timer could not be written!" + e.getMessage());
		}
	}
	
	/**
	 *    writes current channel values to corresponding values of the channels given from interfaces 
	 */
	private void doChannelMapping() {
		this.channel(ChannelId.CURRENT_STATE).onChange(value -> {
			Optional<Enum<?>> stateOpt = value.asEnumOptional();
			if (!stateOpt.isPresent()) {
				this.channel(SymmetricEss.ChannelId.GRID_MODE).setNextValue(SymmetricEss.GridMode.UNDEFINED.ordinal());
				return;
			}
			CurrentState state = (CurrentState) stateOpt.get();
			switch (state) {
			case GRID_CONNECTED:
				this.channel(SymmetricEss.ChannelId.GRID_MODE).setNextValue(SymmetricEss.GridMode.ON_GRID.ordinal());
				break;
			case ERROR:
			case NO_ERROR_PENDING:
			case OFF:
			case PRECHARGE:
			case SHUTTING_DOWN:
			case STANDBY:
			case STARTING:
			case THROTTLED:
				this.channel(SymmetricEss.ChannelId.GRID_MODE).setNextValue(SymmetricEss.GridMode.OFF_GRID.ordinal());
			}
		});
		
		this.battery.getSoc().onChange(value -> {
			this.getSoc().setNextValue(value.get());
			this.channel(ChannelId.BAT_SOC).setNextValue(value.get());
			this.channel(SymmetricEss.ChannelId.SOC).setNextValue(value.get());
		});
	}

	public enum CurrentState implements OptionsEnum {
		OFF(1, "Off"), // directly addressable
		STANDBY(8, "Standby"), // directly addressable
		GRID_CONNECTED(11, "Grid connected"), // directly addressable
		ERROR(7, "Error"), // can be reached from every state, not directly addressable
		PRECHARGE(9, "Precharge"), // State when system goes from OFF to STANDBY, not directly addressable
		STARTING(3, "Starting"), // State from STANDBY to GRID_CONNECTED, not directly addressable
		SHUTTING_DOWN(6, "Shutting down"), // State when system goes from GRID_CONNECTED to STANDBY, not directly
											// addressable
		NO_ERROR_PENDING(12, "No error pending"), // State when system goes from ERROR to OFF, not directly addressable
		THROTTLED(5, "Throttled"); // State that can occur when system is GRID_CONNECTED, not directly addressable

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

	public enum RequestedState implements OptionsEnum {
		// directly addressable states
		OFF(1, "Off"), STANDBY(8, "Standby"), GRID_CONNECTED(11, "Grid connected");

		int value;
		String option;

		private RequestedState(int value, String option) {
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

	public enum ErrorCode implements OptionsEnum {
		WAITING_FOR_FEED_IN(1, "Self-test: Grid parameters and generator voltage are being checked"),
		BATTERY_VOLTAGE_TOO_LOW(2, "Battery Voltage too low! Transition from or to 'Standby'"),
		YIELD_COUNTER_FOR_DAILY(4, "Yield counter for daily and annual yields are displayed"),
		SELF_TEST_IN_PROGR_CHECK(8,
				"Self test in progr. Check the shutdown of the power electronics as well as the shutdown of the grid relay before the charge process."),
		TEMPERATURE_IN_UNIT_TOO(10,
				"Temperature in unit too high In the event of overheating, the device shuts down. Possible causes: ambient temperature too high, fan covered, device fault."),
		POWER_LIMITATION_IF_THE(11,
				"Power limitation: If the generator power is too high, the device limits itself to the maximum power (e.g. around noon if the generator capacity is too large). "),
		POWADORPROTECT_DISCONNECTION(17,
				"Powador-protect disconnection The activated grid and system protection has been tripped."),
		RESID_CURRENT_SHUTDOWN(18,
				"Resid. current shutdown Residual current was detected. The feed-in was interrupted."),
		GENERATOR_INSULATION(19,
				"Generator insulation fault Insulation fault Insulation resistance from DC-/DC + to PE too low"),
		ACTIVE_RAMP_LIMITATION(20,
				"Active ramp limitation The result when the power is increased with a ramp is country-specific."),
		VOLTAGE_TRANS_FAULT_CURRENT(30,
				"Voltage trans. fault Current and voltage measurement in the device are not plausible."),
		SELF_TEST_ERROR_THE_INTERNAL(32,
				"Self test error The internal grid separation relay test has failed. Notify your authorised electrician if the fault occurs repeatedly!"),
		DC_FEEDIN_ERROR_THE_DC(33,
				"DC feed-in error The DC feed-in has exceeded the permitted value. This DC feed-in can be caused in the device by grid conditions and may not necessarily indicate a fault."),
		INTERNAL_COMMUNICATION(34,
				"Internal communication error A communication error has occurred in the internal data transmission. "),
		PROTECTION_SHUTDOWN_SW(35,
				"Protection shutdown SW Protective shutdown of the software (AC overvoltage, AC overcurrent, DC link overvoltage, DC overvoltage, DC overtemperature). "),
		PROTECTION_SHUTDOWN_HW(36,
				"Protection shutdown HW Protective shutdown of the software (AC overvoltage, AC overcurrent, DC link overvoltage, DC overvoltage, DC overtemperature). "),
		ERROR_GENERATOR_VOLTAGE(38, "Error: Generator Voltage too high Error: Battery overvoltage"),
		LINE_FAILURE_UNDERVOLTAGE_1(41,
				"Line failure undervoltage L1 The voltage of a grid phase is too low; the grid cannot be fed into. The phase experiencing failure is displayed."),
		LINE_FAILURE_OVERVOLTAGE_1(42,
				"Line failure overvoltage L1 The voltage of a grid phase is too low; the grid cannot be fed into. The phase experiencing failure is displayed."),
		LINE_FAILURE_UNDERVOLTAGE_2(43,
				"Line failure undervoltage L2 The voltage of a grid phase is too low; the grid cannot be fed into. The phase experiencing failure is displayed."),
		LINE_FAILURE_OVERVOLTAGE_2(44,
				"Line failure overvoltage L2 The voltage of a grid phase is too low; the grid cannot be fed into. The phase experiencing failure is displayed."),
		LINE_FAILURE_UNDERVOLTAGE_3(45,
				"Line failure undervoltage L3 The voltage of a grid phase is too low; the grid cannot be fed into. The phase experiencing failure is displayed."),
		LINE_FAILURE_OVERVOLTAGE_3(46,
				"Line failure overvoltage L3 The voltage of a grid phase is too low; the grid cannot be fed into. The phase experiencing failure is displayed."),
		GRID_FAILURE_PHASETOPHASE(47, "Grid failure phase-to-phase voltage"),
		LINE_FAILURE_UNDERFREQ(48,
				"Line failure: underfreq. Grid frequency is too low. This fault may be gridrelated."),
		LINE_FAILURE_OVERFREQ(49, "Line failure: overfreq. Grid frequency is too high. This fault may be gridrelated."),
		LINE_FAILURE_AVERAGE(50,
				"Line failure: average voltage The grid voltage measurement according to EN 50160 has exceeded the maximum permitted limit value. This fault may be grid-related."),
		WAITING_FOR_REACTIVATION(57,
				"Waiting for reactivation Waiting time of the device following an error. The devices switches on after a countryspecific waiting period."),
		CONTROL_BOARD_OVERTEMP(58,
				"Control board overtemp. The temperature inside the unit was too high. The device shuts down to avoid hardware damage. "),
		SELF_TEST_ERROR_A_FAULT(59,
				"Self test error A fault occurred during a self-test. Contact a qualified electrician."),
		GENERATOR_VOLTAGE_TOO(60, "Generator voltage too high Battery voltage too high"),
		EXTERNAL_LIMIT_X_THE(61,
				"External limit x% The grid operator has activated the external PowerControl limit. The inverter limits the power."),
		P_F_FREQUENCYDEPENDENT(63,
				"P(f)/frequency-dependent power reduction: When certain country settings are activated, the frequency-dependent power reduction is activated."),
		OUTPUT_CURRENT_LIMITING(64,
				"Output current limiting: The AC current is limited once the specified maximum value has been reached."),
		FAULT_AT_POWER_SECTION(67,
				"Fault at power section 1 There is a fault in the power section. Contact a qualified electrician."),
		FAN_1_ERROR_THE_FAN_IS(70,
				"Fan 1 error The fan is malfunctioning. Replace defective fan See Maintenance and troubleshooting chapter."),
		STANDALONE_GRID_ERR_STANDALONE(73, "Standalone grid err. Standalone mode was detected."),
		EXTERNAL_IDLE_POWER_REQUIREMENT(74,
				"External idle power requirement The grid operator limits the feed-in power of the device via the transmitted reactive power factor."),
		INSULATION_MEASUREMENT(79, "Insulation measurement PV generator's insulation is being measured"),
		INSULATION_MEAS_NOT_POSSIBLE(80,
				"Insulation meas. not possible The insulation measurement cannot be performed because the generator voltage is too volatile. - "),
		PROTECTION_SHUTDOWN_LINE_1(81,
				"Protection shutdown line volt. L1 Overvoltage has been detected on a conductor. An internal protective mechanism has disconnected the device to protect it against damage. In case of repeated occurrence: Contact a qualified electrician."),
		PROTECTION_SHUTDOWN_LINE_2(82,
				"Protection shutdown line volt. L2 Overvoltage has been detected on a conductor. An internal protective mechanism has disconnected the device to protect it against damage. In case of repeated occurrence: Contact a qualified electrician."),
		PROTECTION_SHUTDOWN_LINE_3(83,
				"Protection shutdown line volt. L3 Overvoltage has been detected on a conductor. An internal protective mechanism has disconnected the device to protect it against damage. In case of repeated occurrence: Contact a qualified electrician."),
		PROTECTION_SHUTDOWN_UNDERVOLT(84,
				"Protection shutdown undervolt. DC link A voltage deviation has been found in the DC link. An internal protective mechanism has disconnected the device to protect it against damage. In a TN-C-S grid, the PE must be connected to the device and at the same time the PEN bridge in the device must be removed. In case of repeated occurrence: Contact a qualified electrician."),
		PROTECT_SHUTDOWN_OVERVOLT(85, "Protect. shutdown overvolt. DC link"),
		PROTECT_SHUTDOWN_DC_LINK(86, "Protect. shutdown DC link asymmetry"),
		PROTECT_SHUTDOWN_OVERCURRENT_1(87, "Protect. shutdown overcurrent L1"),
		PROTECT_SHUTDOWN_OVERCURRENT_2(88, "Protect. shutdown overcurrent L2"),
		PROTECT_SHUTDOWN_OVERCURRENT_3(89, "Protect. shutdown overcurrent L3"),
		BUFFER_1_SELF_TEST_ERROR(93,
				"Buffer 1 self test error The control board is defective. Please inform your electrician/system manufacturer's service department."),
		SELF_TEST_ERROR_BUFFER(94,
				"Self test error buffer 2 The control board is defective. Notify authorised electrician / KACO Service!"),
		RELAY_1_SELF_TEST_ERROR(95, "Relay 1 self test error The power section is defective. Notify KACO Service"),
		RELAY_2_SELF_TEST_ERROR(96,
				"Relay 2 self test error The power section is defective. Please inform your electrician/system manufacturer's service department."),
		PROTECTION_SHUTDOWN_OVERCURRENT(97,
				"Protection shutdown overcurrent HW Too much power has been fed into the grid. Complete disconnection of the device. Please inform your electrician/system manufacturer's service department."),
		PROTECT_SHUTDOWN_HW_GATE(98,
				"Protect. shutdown HW gate driver An internal protective mechanism has disconnected the device to protect it against damage. Complete disconnection of the device. Please inform your electrician/system manufacturer's service department."),
		PROTECT_SHUTDOWN_HW_BUFFER(99,
				"Protect. shutdown HW buffer free An internal protective mechanism has disconnected the device to protect it against damage. Complete disconnection of the device. Please inform your electrician/system manufacturer's service department."),
		PROTECT_SHUTDOWN_HW_OVERHEATING(100,
				"Protect. shutdown HW overheating The device has been switched off because the temperatures in the housing were too high. Check to make sure that the fans are working. Replace fan if necessary."),
		PLAUSIBILITY_FAULT_AFI(104,
				"Plausibility fault AFI module The unit has shut down because of implausible internal measured values. Please inform your system manufacturer's service department!"),
		PLAUSIBILITY_FAULT_RELAY(105,
				"Plausibility fault relay The unit has shut down because of implausible internal measured values. Please inform your system manufacturer's service department!"),
		PLAUSIBILITY_ERROR_DCDC(106, "Plausibility error DCDC converter "),
		CHECK_SURGE_PROTECTION(107,
				"Check surge protection device Surge protection device (if present in the device) has tripped and must be reset if appropriate."),
		EXTERNAL_COMMUNICATION(196, "External communication error - - "),
		SYMMETRY_ERROR_PARALLEL(197,
				"Symmetry error parallel connection Circuit currents too high for two or more parallel connected bidirectional feed-in inverters. Synchronise intermediate circuit of the parallel connected devices and synchronise the symmetry."),
		BATTERY_DISCONNECTED(198,
				"Battery disconnected Connection to the battery disconnected. Check connection. The battery voltage may be outside the parameterised battery limits."),
		WAITING_FOR_FAULT_ACKNOWLEDGEMENT(215,
				"Waiting for fault acknowledgement Waiting for fault acknowledgement by EMS"),
		PRECHARGE_UNIT_FAULT(218, "Precharge unit fault Precharge unit: Group fault for precharge unit"),
		READY_FOR_PRECHARGING(219, "Ready for precharging Precharge unit: Ready for precharging"),
		PRECHARGE_PRECHARGE_UNIT(220, "Precharge Precharge unit: Precharge process being carried out"),
		WAIT_FOR_COOLDOWN_TIME(221,
				"Wait for cooldown time Precharge unit: Precharge resistance requires time to cool down");

		private final int value;
		private final String option;

		private ErrorCode(int value, String option) {
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

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/*
		 * SUNSPEC_103
		 */
		VENDOR_OPERATING_STATE(new Doc().options(ErrorCode.values())), // see error codes in user manual "10.10
																		// Troubleshooting" (page 48)
		/*
		 * SUNSPEC_121
		 */
		W_MAX(new Doc().unit(Unit.WATT)), //
		W_MAX_SF(new Doc().unit(Unit.NONE)), //
		/*
		 * SUNSPEC_64201
		 */

		REQUESTED_STATE(new Doc().options(RequestedState.values())), //
		CURRENT_STATE(new Doc().options(CurrentState.values())), //
		WATCHDOG(new Doc().unit(Unit.SECONDS)), //
		W_SET_PCT(new Doc().unit(Unit.PERCENT)), //
		W_SET_PCT_SF(new Doc().unit(Unit.NONE)), //

		/*
		 * SUNSPEC_64202
		 */
		V_SF(new Doc().unit(Unit.NONE)), //
		A_SF(new Doc().unit(Unit.NONE)), //
		DIS_MIN_V(new Doc().unit(Unit.VOLT)), //
		DIS_MAX_A(new Doc().unit(Unit.AMPERE)), //
//		DIS_CUTOFF_A(new Doc().text("Disconnect if discharge current lower than DisCutoffA")), // TODO scale factor
		CHA_MAX_V(new Doc().unit(Unit.VOLT)), //
		CHA_MAX_A(new Doc().unit(Unit.AMPERE)), //
//		CHA_CUTOFF_A(new Doc().text("Disconnect if charge current lower than ChaCuttoffA")), // TODO scale factor
		EN_LIMIT(new Doc().text("new battery limits are activated when EnLimit is 1")), //

		/*
		 * SUNSPEC_64203
		 */
		SOC_SF(new Doc().unit(Unit.NONE)), //
		SOH_SF(new Doc().unit(Unit.NONE)), //
		TEMP_SF(new Doc().unit(Unit.NONE)), //
		BAT_SOC(new Doc().unit(Unit.PERCENT)), //
		BAT_SOH(new Doc().unit(Unit.PERCENT)), //
		BAT_TEMP(new Doc().unit(Unit.PERCENT)), //
		/*
		 * SUNSPEC_64302
		 */
		COMMAND_ID_REQ(new Doc().unit(Unit.NONE)), //
		REQ_PARAM_0(new Doc().unit(Unit.NONE)), //
		COMMAND_ID_REQ_ENA(new Doc().unit(Unit.NONE)), //
		COMMAND_ID_RES(new Doc().unit(Unit.NONE)), //
		RETURN_CODE(new Doc().unit(Unit.NONE)), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

//	private final static int SUNSPEC_1 = 40003 - 1; // According to setup process pdf currently not used...
	private final static int SUNSPEC_103 = 40071 - 1;
	private final static int SUNSPEC_121 = 40213 - 1;
	private final static int SUNSPEC_64201 = 40823 - 1;
	private final static int SUNSPEC_64202 = 40877 - 1;
	private final static int SUNSPEC_64203 = 40893 - 1;
	private final static int SUNSPEC_64302 = 40931 - 1;

	@Override
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		return new ModbusProtocol(unitId, //
				new FC3ReadRegistersTask(SUNSPEC_103 + 39,Priority.LOW, //
						m(EssKacoBlueplanetGridsave50.ChannelId.VENDOR_OPERATING_STATE, new SignedWordElement(SUNSPEC_103 + 39))), //				
				new FC3ReadRegistersTask(SUNSPEC_64201 + 35, Priority.HIGH,
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedWordElement(SUNSPEC_64201 + 35),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new SignedWordElement(SUNSPEC_64201 + 36),
								ElementToChannelConverter.SCALE_FACTOR_1)), //
				new FC3ReadRegistersTask(SUNSPEC_121 + 2, Priority.LOW,
						m(EssKacoBlueplanetGridsave50.ChannelId.W_MAX, new UnsignedWordElement(SUNSPEC_121 + 2)), //
						new DummyRegisterElement(SUNSPEC_121 + 3, SUNSPEC_121 + 21), //
						m(EssKacoBlueplanetGridsave50.ChannelId.W_MAX_SF, new SignedWordElement(SUNSPEC_121 + 22))), //
				new FC16WriteRegistersTask(SUNSPEC_64201 + 4,
						m(EssKacoBlueplanetGridsave50.ChannelId.REQUESTED_STATE,
								new UnsignedWordElement(SUNSPEC_64201 + 4))), //
				new FC3ReadRegistersTask(SUNSPEC_64201 + 5, Priority.LOW, //
						m(EssKacoBlueplanetGridsave50.ChannelId.CURRENT_STATE,
								new UnsignedWordElement(SUNSPEC_64201 + 5))), //
				new FC16WriteRegistersTask(SUNSPEC_64201 + 8, //
						m(EssKacoBlueplanetGridsave50.ChannelId.WATCHDOG, new UnsignedWordElement(SUNSPEC_64201 + 8))), //
				new FC16WriteRegistersTask(SUNSPEC_64201 + 9, //
						m(EssKacoBlueplanetGridsave50.ChannelId.W_SET_PCT, new SignedWordElement(SUNSPEC_64201 + 9))), //
				new FC3ReadRegistersTask(SUNSPEC_64201 + 46, Priority.LOW, //
						m(EssKacoBlueplanetGridsave50.ChannelId.W_SET_PCT_SF, new SignedWordElement(SUNSPEC_64201 + 46))), //
				new FC3ReadRegistersTask(SUNSPEC_64202 + 6, Priority.LOW, //
						m(EssKacoBlueplanetGridsave50.ChannelId.V_SF, new SignedWordElement(SUNSPEC_64202 + 6)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.A_SF, new SignedWordElement(SUNSPEC_64202 + 7))), //
				new FC16WriteRegistersTask(SUNSPEC_64202 + 8,
						m(EssKacoBlueplanetGridsave50.ChannelId.DIS_MIN_V, new UnsignedWordElement(SUNSPEC_64202 + 8),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(EssKacoBlueplanetGridsave50.ChannelId.DIS_MAX_A, new UnsignedWordElement(SUNSPEC_64202 + 9),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(SUNSPEC_64202 + 10),
						m(EssKacoBlueplanetGridsave50.ChannelId.CHA_MAX_V, new UnsignedWordElement(SUNSPEC_64202 + 11),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(EssKacoBlueplanetGridsave50.ChannelId.CHA_MAX_A, new UnsignedWordElement(SUNSPEC_64202 + 12),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(SUNSPEC_64202 + 13, SUNSPEC_64202 + 14), //
						m(EssKacoBlueplanetGridsave50.ChannelId.EN_LIMIT, new UnsignedWordElement(SUNSPEC_64202 + 15))), //
				new FC3ReadRegistersTask(SUNSPEC_64203 + 5, Priority.LOW, //
						m(EssKacoBlueplanetGridsave50.ChannelId.SOC_SF, new SignedWordElement(SUNSPEC_64203 + 5)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.SOH_SF, new SignedWordElement(SUNSPEC_64203 + 6)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.TEMP_SF, new SignedWordElement(SUNSPEC_64203 + 7))), //
				new FC16WriteRegistersTask(SUNSPEC_64203 + 16, //
						m(EssKacoBlueplanetGridsave50.ChannelId.BAT_SOC, new UnsignedWordElement(SUNSPEC_64203 + 16)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.BAT_SOH, new UnsignedWordElement(SUNSPEC_64203 + 17)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.BAT_TEMP, new SignedWordElement(SUNSPEC_64203 + 18))), //
				new FC16WriteRegistersTask(SUNSPEC_64302 + 12, //
						m(EssKacoBlueplanetGridsave50.ChannelId.COMMAND_ID_REQ, //
								new SignedWordElement(SUNSPEC_64302 + 12)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.REQ_PARAM_0,
								new UnsignedDoublewordElement(SUNSPEC_64302 + 13))), //
				new FC16WriteRegistersTask(SUNSPEC_64302 + 29,
						m(EssKacoBlueplanetGridsave50.ChannelId.COMMAND_ID_REQ_ENA,
								new UnsignedWordElement(SUNSPEC_64302 + 29))), //
				new FC3ReadRegistersTask(SUNSPEC_64302 + 30, Priority.LOW, //
						m(EssKacoBlueplanetGridsave50.ChannelId.COMMAND_ID_RES,
								new SignedWordElement(SUNSPEC_64302 + 30)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.RETURN_CODE,
								new SignedWordElement(SUNSPEC_64302 + 31)))); //
	}
}
