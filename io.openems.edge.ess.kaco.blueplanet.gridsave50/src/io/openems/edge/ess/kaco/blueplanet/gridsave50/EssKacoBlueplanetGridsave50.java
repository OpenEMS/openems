package io.openems.edge.ess.kaco.blueplanet.gridsave50;

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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Priority;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.OptionsEnum;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.Ess;
import io.openems.edge.ess.power.symmetric.SymmetricPower;
import io.openems.edge.ess.symmetric.api.SymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.Kaco.BlueplanetGridsave50", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
		)
public class EssKacoBlueplanetGridsave50 extends AbstractOpenemsModbusComponent
implements SymmetricEss, Ess, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(EssKacoBlueplanetGridsave50.class);

	private final static int UNIT_ID = 1;
	protected final static int MAX_APPARENT_POWER = 50000;

	private final SymmetricPower power;

	@Reference
	protected ConfigurationAdmin cm;

	public EssKacoBlueplanetGridsave50() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
		/*
		 * Initialize Power
		 */
		this.power = new SymmetricPower(this, EssKacoBlueplanetGridsave50.MAX_APPARENT_POWER, 1 /*
		 * TODO: POWER_PRECISION
		 */, //
		 (activePower, reactivePower) -> {
			 /*
			  * Get channels
			  */
			 IntegerWriteChannel disMinV = this.channel(ChannelId.DIS_MIN_V);
			 IntegerWriteChannel disMaxA = this.channel(ChannelId.DIS_MAX_A);
			 IntegerWriteChannel chaMaxV = this.channel(ChannelId.CHA_MAX_V);
			 IntegerWriteChannel chaMaxA = this.channel(ChannelId.CHA_MAX_A);
			 IntegerWriteChannel enLimit = this.channel(ChannelId.EN_LIMIT);
			 IntegerWriteChannel conn = this.channel(ChannelId.CONN);
			 IntegerWriteChannel wSetEna = this.channel(ChannelId.W_SET_ENA);
			 IntegerWriteChannel wSetPct = this.channel(ChannelId.W_SET_PCT);
			 IntegerReadChannel statePowerUnit = this.channel(ChannelId.STATE_POWER_UNIT);

			 /*
			  * Handle state machine
			  */
			 Value<Integer> stateValue = statePowerUnit.value();
			 if (stateValue.get() == null) {
				 return;
			 }

			 StatePowerUnit state;
			 try {
				 state = (StatePowerUnit) stateValue.asEnum();
			 } catch (InvalidValueException e1) {
				 e1.printStackTrace();
				 return;
			 }

			 switch (state) {
			 case PRECHARGE_SYSTEM_BOOT: // Transitive state -> Wait...
				 break;

			 case DISCONNECT: // DSP has no power supply -> start battery
			 case STANDBY:
				 try {
					 conn.setNextWriteValue(1 /* TODO use enum */);
				 } catch (OpenemsException e) {
					 e.printStackTrace();
				 }

			 case ACTIVE:
				 // TODO replace static value with the one from Sunspec 103 * scale factor
				 // TODO round properly
				 // the base formula is (activePower * 1000) / 52000
				 int activePowerPct = activePower / 52;

				 try {
					 disMinV.setNextWriteValue(696);
					 disMaxA.setNextWriteValue(3);
					 chaMaxV.setNextWriteValue(854);
					 chaMaxA.setNextWriteValue(3);
					 enLimit.setNextWriteValue(1);
					 wSetEna.setNextWriteValue( 1 /* TODO use enum */);
					 wSetPct.setNextWriteValue(activePowerPct);
				 } catch (OpenemsException e) {
					 e.printStackTrace();
				 }
				 break;

			 case ERROR: {
				 /*
				  * Error
				  */
				 log.warn("ERROR");
				 try {
					 // clear error
					 conn.setNextWriteValue(0);
				 } catch (OpenemsException e) {
					 e.printStackTrace();
				 }
				 break;
			 }
			 }
		 });

	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled(), UNIT_ID, this.cm, "Modbus",
				config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public enum Conn {
		DISCONNECT(0), CONNECT(1);

		int value;		
		private Conn(int value) {
			this.value = value;
		}
	}

	public enum StatePowerUnit {
		DISCONNECT(0), PRECHARGE_SYSTEM_BOOT(16), STANDBY(3), ACTIVE(1), ERROR(15);
		
		int value;
		private StatePowerUnit(int value) {
			this.value = value;
		}
	}
	
	public enum WSetEna {
		ENABLED(1), DISABLED(0);

		int value;
		private WSetEna(int value) {
			this.value = value;
		}
	}
	
	public enum ErrorCode implements OptionsEnum {
		WAITING_FOR_FEED_IN(1, "Self-test: Grid parameters and generator voltage are being checked"),
		BATTERY_VOLTAGE_TOO_LOW(2, "Battery Voltage too low! Transition from or to 'Standby'"),
		YIELD_COUNTER_FOR_DAILY(4, "Yield counter for daily and annual yields are displayed"),
		SELF_TEST_IN_PROGR_CHECK(8, "Self test in progr. Check the shutdown of the power electronics as well as the shutdown of the grid relay before the charge process."),
		TEMPERATURE_IN_UNIT_TOO(10, "Temperature in unit too high In the event of overheating, the device shuts down. Possible causes: ambient temperature too high, fan covered, device fault."),
		POWER_LIMITATION_IF_THE(11, "Power limitation: If the generator power is too high, the device limits itself to the maximum power (e.g. around noon if the generator capacity is too large). "),
		POWADORPROTECT_DISCONNECTION(17, "Powador-protect disconnection The activated grid and system protection has been tripped."),
		RESID_CURRENT_SHUTDOWN(18, "Resid. current shutdown Residual current was detected. The feed-in was interrupted."),
		GENERATOR_INSULATION(19, "Generator insulation fault Insulation fault Insulation resistance from DC-/DC + to PE too low"),
		ACTIVE_RAMP_LIMITATION(20, "Active ramp limitation The result when the power is increased with a ramp is country-specific."),
		VOLTAGE_TRANS_FAULT_CURRENT(30, "Voltage trans. fault Current and voltage measurement in the device are not plausible."),
		SELF_TEST_ERROR_THE_INTERNAL(32, "Self test error The internal grid separation relay test has failed. Notify your authorised electrician if the fault occurs repeatedly!"),
		DC_FEEDIN_ERROR_THE_DC(33, "DC feed-in error The DC feed-in has exceeded the permitted value. This DC feed-in can be caused in the device by grid conditions and may not necessarily indicate a fault."),
		INTERNAL_COMMUNICATION(34, "Internal communication error A communication error has occurred in the internal data transmission. "),
		PROTECTION_SHUTDOWN_SW(35, "Protection shutdown SW Protective shutdown of the software (AC overvoltage, AC overcurrent, DC link overvoltage, DC overvoltage, DC overtemperature). "),
		PROTECTION_SHUTDOWN_HW(36, "Protection shutdown HW Protective shutdown of the software (AC overvoltage, AC overcurrent, DC link overvoltage, DC overvoltage, DC overtemperature). "),
		ERROR_GENERATOR_VOLTAGE(38, "Error: Generator Voltage too high Error: Battery overvoltage"),
		LINE_FAILURE_UNDERVOLTAGE_1(41, "Line failure undervoltage L1 The voltage of a grid phase is too low; the grid cannot be fed into. The phase experiencing failure is displayed."),
		LINE_FAILURE_OVERVOLTAGE_1(42, "Line failure overvoltage L1 The voltage of a grid phase is too low; the grid cannot be fed into. The phase experiencing failure is displayed."),
		LINE_FAILURE_UNDERVOLTAGE_2(43, "Line failure undervoltage L2 The voltage of a grid phase is too low; the grid cannot be fed into. The phase experiencing failure is displayed."),
		LINE_FAILURE_OVERVOLTAGE_2(44, "Line failure overvoltage L2 The voltage of a grid phase is too low; the grid cannot be fed into. The phase experiencing failure is displayed."),
		LINE_FAILURE_UNDERVOLTAGE_3(45, "Line failure undervoltage L3 The voltage of a grid phase is too low; the grid cannot be fed into. The phase experiencing failure is displayed."),
		LINE_FAILURE_OVERVOLTAGE_3(46, "Line failure overvoltage L3 The voltage of a grid phase is too low; the grid cannot be fed into. The phase experiencing failure is displayed."),
		GRID_FAILURE_PHASETOPHASE(47, "Grid failure phase-to-phase voltage"),
		LINE_FAILURE_UNDERFREQ(48, "Line failure: underfreq. Grid frequency is too low. This fault may be gridrelated."),
		LINE_FAILURE_OVERFREQ(49, "Line failure: overfreq. Grid frequency is too high. This fault may be gridrelated."),
		LINE_FAILURE_AVERAGE(50, "Line failure: average voltage The grid voltage measurement according to EN 50160 has exceeded the maximum permitted limit value. This fault may be grid-related."),
		WAITING_FOR_REACTIVATION(57, "Waiting for reactivation Waiting time of the device following an error. The devices switches on after a countryspecific waiting period."),
		CONTROL_BOARD_OVERTEMP(58, "Control board overtemp. The temperature inside the unit was too high. The device shuts down to avoid hardware damage. "),
		SELF_TEST_ERROR_A_FAULT(59, "Self test error A fault occurred during a self-test. Contact a qualified electrician."),
		GENERATOR_VOLTAGE_TOO(60, "Generator voltage too high Battery voltage too high"),
		EXTERNAL_LIMIT_X_THE(61, "External limit x% The grid operator has activated the external PowerControl limit. The inverter limits the power."),
		P_F_FREQUENCYDEPENDENT(63, "P(f)/frequency-dependent power reduction: When certain country settings are activated, the frequency-dependent power reduction is activated."),
		OUTPUT_CURRENT_LIMITING(64, "Output current limiting: The AC current is limited once the specified maximum value has been reached."),
		FAULT_AT_POWER_SECTION(67, "Fault at power section 1 There is a fault in the power section. Contact a qualified electrician."),
		FAN_1_ERROR_THE_FAN_IS(70, "Fan 1 error The fan is malfunctioning. Replace defective fan See Maintenance and troubleshooting chapter."),
		STANDALONE_GRID_ERR_STANDALONE(73, "Standalone grid err. Standalone mode was detected."),
		EXTERNAL_IDLE_POWER_REQUIREMENT(74, "External idle power requirement The grid operator limits the feed-in power of the device via the transmitted reactive power factor."),
		INSULATION_MEASUREMENT(79, "Insulation measurement PV generator's insulation is being measured"),
		INSULATION_MEAS_NOT_POSSIBLE(80, "Insulation meas. not possible The insulation measurement cannot be performed because the generator voltage is too volatile. - "),
		PROTECTION_SHUTDOWN_LINE_1(81, "Protection shutdown line volt. L1 Overvoltage has been detected on a conductor. An internal protective mechanism has disconnected the device to protect it against damage. In case of repeated occurrence: Contact a qualified electrician."),
		PROTECTION_SHUTDOWN_LINE_2(82, "Protection shutdown line volt. L2 Overvoltage has been detected on a conductor. An internal protective mechanism has disconnected the device to protect it against damage. In case of repeated occurrence: Contact a qualified electrician."),
		PROTECTION_SHUTDOWN_LINE_3(83, "Protection shutdown line volt. L3 Overvoltage has been detected on a conductor. An internal protective mechanism has disconnected the device to protect it against damage. In case of repeated occurrence: Contact a qualified electrician."),
		PROTECTION_SHUTDOWN_UNDERVOLT(84, "Protection shutdown undervolt. DC link A voltage deviation has been found in the DC link. An internal protective mechanism has disconnected the device to protect it against damage. In a TN-C-S grid, the PE must be connected to the device and at the same time the PEN bridge in the device must be removed. In case of repeated occurrence: Contact a qualified electrician."),
		PROTECT_SHUTDOWN_OVERVOLT(85, "Protect. shutdown overvolt. DC link"),
		PROTECT_SHUTDOWN_DC_LINK(86, "Protect. shutdown DC link asymmetry"),
		PROTECT_SHUTDOWN_OVERCURRENT_1(87, "Protect. shutdown overcurrent L1"),
		PROTECT_SHUTDOWN_OVERCURRENT_2(88, "Protect. shutdown overcurrent L2"),
		PROTECT_SHUTDOWN_OVERCURRENT_3(89, "Protect. shutdown overcurrent L3"),
		BUFFER_1_SELF_TEST_ERROR(93, "Buffer 1 self test error The control board is defective. Please inform your electrician/system manufacturer's service department."),
		SELF_TEST_ERROR_BUFFER(94, "Self test error buffer 2 The control board is defective. Notify authorised electrician / KACO Service!"),
		RELAY_1_SELF_TEST_ERROR(95, "Relay 1 self test error The power section is defective. Notify KACO Service"),
		RELAY_2_SELF_TEST_ERROR(96, "Relay 2 self test error The power section is defective. Please inform your electrician/system manufacturer's service department."),
		PROTECTION_SHUTDOWN_OVERCURRENT(97, "Protection shutdown overcurrent HW Too much power has been fed into the grid. Complete disconnection of the device. Please inform your electrician/system manufacturer's service department."),
		PROTECT_SHUTDOWN_HW_GATE(98, "Protect. shutdown HW gate driver An internal protective mechanism has disconnected the device to protect it against damage. Complete disconnection of the device. Please inform your electrician/system manufacturer's service department."),
		PROTECT_SHUTDOWN_HW_BUFFER(99, "Protect. shutdown HW buffer free An internal protective mechanism has disconnected the device to protect it against damage. Complete disconnection of the device. Please inform your electrician/system manufacturer's service department."),
		PROTECT_SHUTDOWN_HW_OVERHEATING(100, "Protect. shutdown HW overheating The device has been switched off because the temperatures in the housing were too high. Check to make sure that the fans are working. Replace fan if necessary."),
		PLAUSIBILITY_FAULT_AFI(104, "Plausibility fault AFI module The unit has shut down because of implausible internal measured values. Please inform your system manufacturer's service department!"),
		PLAUSIBILITY_FAULT_RELAY(105, "Plausibility fault relay The unit has shut down because of implausible internal measured values. Please inform your system manufacturer's service department!"),
		PLAUSIBILITY_ERROR_DCDC(106, "Plausibility error DCDC converter "),
		CHECK_SURGE_PROTECTION(107, "Check surge protection device Surge protection device (if present in the device) has tripped and must be reset if appropriate."),
		EXTERNAL_COMMUNICATION(196, "External communication error - - "),
		SYMMETRY_ERROR_PARALLEL(197, "Symmetry error parallel connection Circuit currents too high for two or more parallel connected bidirectional feed-in inverters. Synchronise intermediate circuit of the parallel connected devices and synchronise the symmetry."),
		BATTERY_DISCONNECTED(198, "Battery disconnected Connection to the battery disconnected. Check connection. The battery voltage may be outside the parameterised battery limits."),
		WAITING_FOR_FAULT_ACKNOWLEDGEMENT(215, "Waiting for fault acknowledgement Waiting for fault acknowledgement by EMS"),
		PRECHARGE_UNIT_FAULT(218, "Precharge unit fault Precharge unit: Group fault for precharge unit"),
		READY_FOR_PRECHARGING(219, "Ready for precharging Precharge unit: Ready for precharging"),
		PRECHARGE_PRECHARGE_UNIT(220, "Precharge Precharge unit: Precharge process being carried out"),
		WAIT_FOR_COOLDOWN_TIME(221, "Wait for cooldown time Precharge unit: Precharge resistance requires time to cool down");

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
	

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/*
		 * SUNSPEC_103
		 */
		VENDOR_OPERATING_STATE(new Doc().options(ErrorCode.values())),
		
		// see error codes in user manual "10.10 Troubleshooting" (page 48)
		/*
		 * SUNSPEC_64201
		 */
		CONN(new Doc() //
				.option(Conn.DISCONNECT.value, Conn.DISCONNECT) //
				.option(Conn.CONNECT.value, Conn.CONNECT)), //
		STATE_POWER_UNIT(new Doc() //
				.option(StatePowerUnit.DISCONNECT.value, StatePowerUnit.DISCONNECT) //
				.option(StatePowerUnit.PRECHARGE_SYSTEM_BOOT.value, StatePowerUnit.PRECHARGE_SYSTEM_BOOT) //
				.option(StatePowerUnit.STANDBY.value, StatePowerUnit.STANDBY) //
				.option(StatePowerUnit.ACTIVE.value, StatePowerUnit.ACTIVE) //
				.option(StatePowerUnit.ERROR.value, StatePowerUnit.ERROR) //
				// note: 'Startup' is not handled. It is deprecated with next firmware version
				), //
		W_SET_PCT(new Doc().text("Set power output to specified level. unscaled: -100 to 100")), //
		W_SET_ENA(new Doc().text("WSet_Ena control") //
				.option(WSetEna.DISABLED.value, WSetEna.DISABLED) //
				.option(WSetEna.ENABLED.value, WSetEna.ENABLED) //
				), //
		/*
		 * SUNSPEC_64202
		 */
		V_SF(new Doc().text("scale factor for voltage")), //
		A_SF(new Doc().text("scale factor for ampere")), //
		DIS_MIN_V(new Doc().text("min. discharge voltage")), // TODO scale factor
		DIS_MAX_A(new Doc().text("max. discharge current")), // TODO scale factor
		DIS_CUTOFF_A(new Doc().text("Disconnect if discharge current lower than DisCutoffA")), // TODO scale factor
		CHA_MAX_V(new Doc().text("max. charge voltage")), // TODO scale factor
		CHA_MAX_A(new Doc().text("max. charge current")), // TODO scale factor
		CHA_CUTOFF_A(new Doc().text("Disconnect if charge current lower than ChaCuttoffA")), // TODO scale factor
		EN_LIMIT(new Doc().text("new battery limits are activated when EnLimit is 1")) //
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	private final static int SUNSPEC_103 = 40071 - 1;
	private final static int SUNSPEC_64201 = 40823 - 1;
	private final static int SUNSPEC_64202 = 40855 - 1;

	@Override
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		return new ModbusProtocol(unitId, //
				new FC16WriteRegistersTask(SUNSPEC_64201 + 4, //
						m(EssKacoBlueplanetGridsave50.ChannelId.CONN, new UnsignedWordElement(SUNSPEC_64201 + 4))),
				new FC16WriteRegistersTask(SUNSPEC_64201 + 10, //
						m(EssKacoBlueplanetGridsave50.ChannelId.W_SET_PCT, new UnsignedWordElement(SUNSPEC_64201 + 10)),
						m(EssKacoBlueplanetGridsave50.ChannelId.W_SET_ENA,
								new UnsignedWordElement(SUNSPEC_64201 + 11))),
				new FC3ReadRegistersTask(SUNSPEC_103 + 39, Priority.LOW, //
						m(EssKacoBlueplanetGridsave50.ChannelId.VENDOR_OPERATING_STATE,
								new UnsignedWordElement(SUNSPEC_103 + 39))),
				new FC3ReadRegistersTask(SUNSPEC_64201 + 4, Priority.LOW, //
						m(EssKacoBlueplanetGridsave50.ChannelId.CONN, new UnsignedWordElement(SUNSPEC_64201 + 4)),
						m(EssKacoBlueplanetGridsave50.ChannelId.STATE_POWER_UNIT,
								new UnsignedWordElement(SUNSPEC_64201 + 5))), //
				new FC3ReadRegistersTask(SUNSPEC_64202 + 6, Priority.LOW, //
						m(EssKacoBlueplanetGridsave50.ChannelId.V_SF, new UnsignedWordElement(SUNSPEC_64202 + 6)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.A_SF, new UnsignedWordElement(SUNSPEC_64202 + 7)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.DIS_MIN_V, new UnsignedWordElement(SUNSPEC_64202 + 8)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.DIS_MAX_A, new UnsignedWordElement(SUNSPEC_64202 + 9)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.DIS_CUTOFF_A,
								new UnsignedWordElement(SUNSPEC_64202 + 10)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.CHA_MAX_V, new UnsignedWordElement(SUNSPEC_64202 + 11)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.CHA_MAX_A, new UnsignedWordElement(SUNSPEC_64202 + 12)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.CHA_CUTOFF_A,
								new UnsignedWordElement(SUNSPEC_64202 + 13)), //
						new DummyRegisterElement(SUNSPEC_64202 + 14),
						m(EssKacoBlueplanetGridsave50.ChannelId.EN_LIMIT, new UnsignedWordElement(SUNSPEC_64202 + 15))) //
				);
	}

	@Override
	public String debugLog() {
		return "Conn: " + this.channel(ChannelId.CONN).value().asOptionString() + ", State: "
				+ this.channel(ChannelId.STATE_POWER_UNIT).value().asOptionString()  + ", Vendor Operating State: "
						+ this.channel(ChannelId.VENDOR_OPERATING_STATE).value().asOptionString() ;
	}

	@Override
	public SymmetricPower getPower() {
		return this.power;
	}
}
