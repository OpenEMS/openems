package io.openems.edge.ess.kaco.blueplanet.gridsave50;

import java.time.Duration;
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
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.Kaco.BlueplanetGridsave50", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { 	EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		}		
) //
public class EssKacoBlueplanetGridsave50 extends AbstractOpenemsModbusComponent
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(EssKacoBlueplanetGridsave50.class);

	public static final int DEFAULT_UNIT_ID = 1;
	protected static final int MAX_APPARENT_POWER = 52000;

	private int watchdogInterval = 0;
	private int maxApparentPower = 0;
	private int maxApparentPowerUnscaled = 0;
	private int maxApparentPowerScaleFactor = 0;

	/*
	 * Is Power allowed? This is set to false on error or if the inverter is not
	 * fully initialized.
	 */
	private boolean isActivePowerAllowed = true;

	@Reference
	private Power power;

	@Reference
	protected ConfigurationAdmin cm;

	private Battery battery;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setBattery(Battery battery) {
		this.battery = battery;
	}

	public EssKacoBlueplanetGridsave50() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled(), DEFAULT_UNIT_ID, this.cm, "Modbus",
				config.modbus_id()); //
		// update filter for 'battery'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "battery", config.battery_id())) {
			return;
		}

		watchdogInterval = config.watchdoginterval();
		doChannelMapping();
		initializePower();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void initializePower() {
		this.isActivePowerAllowed = true;

		this.channel(ChannelId.W_MAX).onChange(value -> {
			@SuppressWarnings("unchecked")
			Optional<Integer> valueOpt = (Optional<Integer>) value.asOptional();
			if (!valueOpt.isPresent()) {
				return;
			}
			maxApparentPowerUnscaled = TypeUtils.getAsType(OpenemsType.INTEGER, value);
			refreshPower();
		});
		this.channel(ChannelId.W_MAX_SF).onChange(value -> {
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

	private void refreshPower() {
		maxApparentPower = maxApparentPowerUnscaled * maxApparentPowerScaleFactor;
		if (maxApparentPower > 0) {
			this.getMaxApparentPower().setNextValue(maxApparentPower);
		}
	}

	@Override
	public Constraint[] getStaticConstraints() {
		if (this.isActivePowerAllowed) {
			return new Constraint[] { 
				this.createPowerConstraint("Reactive power is not allowed", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0)
			};
		} else {
			return new Constraint[] { 
					this.createPowerConstraint("KACO inverter not ready", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0),
					this.createPowerConstraint("Reactive power is not allowed", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0)
			};
		}
	}
	
	@Override
	public void applyPower(int activePower, int reactivePower) {

		IntegerWriteChannel wSetPctChannel = this.channel(ChannelId.W_SET_PCT);
		IntegerReadChannel wSetPct_SFChannel = this.channel(ChannelId.W_SET_PCT_SF);

		Optional<Integer> wSetPctSFOpt = wSetPct_SFChannel.value().asOptional();
		if (wSetPctSFOpt.isPresent()) {

			int scalefactor = wSetPctSFOpt.get();

			int max = maxApparentPower;
			if (max == 0) {
				max = MAX_APPARENT_POWER;
			}

			/**
			 * according to manual active power has to be set in % of maximum active power
			 * with scale factor see page 10 
			 * WSetPct = (WSet_Watt * 100) / ( W_Max_unscaled * 10^W_Max_SF * 10^WSetPct_SF)
			 */
			int WSetPct = (int) ((activePower * 100) / (max * Math.pow(10, scalefactor)));

			try {
				wSetPctChannel.setNextWriteValue(WSetPct);
			} catch (OpenemsException e) {
				log.error("EssKacoBlueplanetGridsave50.applyPower(): Problem occurred while trying so set active power"
						+ e.getMessage());
			}
		}
	}

	private void handleStateMachine() {
		// by default: block Power
		this.isActivePowerAllowed = false;

		// do always
		setBatteryRanges();
		setWatchdog();

		IntegerReadChannel currentStateChannel = this.channel(ChannelId.CURRENT_STATE);
		CurrentState currentState = currentStateChannel.value().asEnum();
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
		case NO_ERROR_PENDING:
			doErrorHandling();
		case PRECHARGE:
		case SHUTTING_DOWN:
		case STARTING:
		case THROTTLED:
		case CURRENTLY_UNKNOWN:
		case UNDEFINED:
			// Do nothing because these states are only temporarily reached
			break;
		}
	}

	private void doStandbyHandling() {
		this.isActivePowerAllowed = false;
		startGridMode();
	}

	private void doOffHandling() {
		this.isActivePowerAllowed = false;
		startSystem();
	}

	private void doGridConnectedHandling() {		
		// If the battery system is not ready yet set power to zero to avoid damaging or improper system states
		if (!battery.getReadyForWorking().value().orElse(false)) {
			this.isActivePowerAllowed = false;
		} else {
			this.isActivePowerAllowed = true;
		}
		
	}

	private void doErrorHandling() {
		// find out the reason what is wrong an react
		// for a first try, switch system off, it will be restarted
		stopSystem();
	}

	private void setBatteryRanges() {
		if (battery == null) {
			return;
		}

		// Read some Channels from Battery
		int disMinV = battery.getDischargeMinVoltage().value().orElse(0);
		int chaMaxV = battery.getChargeMaxVoltage().value().orElse(0);
		int disMaxA = battery.getDischargeMaxCurrent().value().orElse(0);
		int chaMaxA = battery.getChargeMaxCurrent().value().orElse(0);
		int batSoC = battery.getSoc().value().orElse(0);
		int batSoH = battery.getSoh().value().orElse(0);
		int batTemp = battery.getMaxCellTemperature().value().orElse(0);

		// Update Power Constraints
		// TODO: The actual AC allowed charge and discharge should come from the KACO
		// Blueplanet instead of calculating it from DC parameters.
		final double EFFICIENCY_FACTOR = 0.9;
		this.getAllowedCharge().setNextValue(chaMaxA * chaMaxV * -1 * EFFICIENCY_FACTOR);
		this.getAllowedDischarge().setNextValue(disMaxA * disMinV * EFFICIENCY_FACTOR);

		if (disMinV == 0 || chaMaxV == 0) {
			return; // according to setup manual 64202.DisMinV and 64202.ChaMaxV must not be zero
		}

		// Set Battery values to inverter
		try {
			this.getDischargeMinVoltageChannel().setNextWriteValue(disMinV);
			this.getChargeMaxVoltageChannel().setNextWriteValue(chaMaxV);
			this.getDischargeMaxAmpereChannel().setNextWriteValue(disMaxA);
			this.getChargeMaxAmpereChannel().setNextWriteValue(chaMaxA);
			this.getEnLimitChannel().setNextWriteValue(1);

			// battery stats to display on inverter
			this.getBatterySocChannel().setNextWriteValue(batSoC);
			this.getBatterySohChannel().setNextWriteValue(batSoH);
			this.getBatteryTempChannel().setNextWriteValue(batTemp);
		} catch (OpenemsException e) {
			log.error("Error during setBatteryRanges, " + e.getMessage());
		}
	}

	@Override
	public String debugLog() {
		return "State:" + this.channel(ChannelId.CURRENT_STATE).value().asOptionString() //
				+ ",L:" + this.channel(SymmetricEss.ChannelId.ACTIVE_POWER).value().asString(); //
	}

	@Override
	public Power getPower() {
		return this.power;
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
			calculateEnergy();
			handleStateMachine();
			break;
		}
	}

	// These variables are used to calculate the energy 
	LocalDateTime lastPowerValuesTimestamp = null;
	double lastCurrentValue = 0;
	double lastVoltageValue = 0;
	double lastActivePowerValue = 0;	
	double accumulatedChargeEnergy = 0;
	double accumulatedDischargeEnergy = 0;
	
	/*
	 * This calculates charge/discharge energy using voltage value given from the connected battery and current value from the inverter 
	 * */
	private void calculateEnergy() {
		if (this.lastPowerValuesTimestamp != null) {						
			
			long passedTimeInMilliSeconds = Duration.between(this.lastPowerValuesTimestamp, LocalDateTime.now()).toMillis();
			this.lastPowerValuesTimestamp = LocalDateTime.now();
			
			double lastPowerValue = this.lastCurrentValue * this.lastVoltageValue; 
			double energy = lastPowerValue  * ( ((double) passedTimeInMilliSeconds) / 1000.0) / 3600.0; // calculate energy in watt hours
			
			if (this.lastActivePowerValue < 0) {
				this.accumulatedChargeEnergy = this.accumulatedChargeEnergy + energy;
				this.getActiveChargeEnergy().setNextValue(accumulatedChargeEnergy);
			} else if (this.lastActivePowerValue > 0) {
				this.accumulatedDischargeEnergy = this.accumulatedDischargeEnergy + energy;
				this.getActiveDischargeEnergy().setNextValue(accumulatedDischargeEnergy);
			}
			
			log.debug("accumulated charge energy :" + accumulatedChargeEnergy);
			log.debug("accumulated discharge energy :" + accumulatedDischargeEnergy);
			
		} else {
			this.lastPowerValuesTimestamp = LocalDateTime.now();			
		}
		
		this.lastActivePowerValue = this.getActivePower().value().orElse(0);
				
		IntegerReadChannel lastCurrentValueChannel = this.channel(ChannelId.DC_CURRENT);
		this.lastCurrentValue = lastCurrentValueChannel.value().orElse(0) / 1000.0;

		this.lastVoltageValue = this.battery.getVoltage().value().orElse(0);
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
	 * writes current channel values to corresponding values of the channels given
	 * from interfaces
	 */
	private void doChannelMapping() {
		this.battery.getSoc().onChange(value -> {
			this.getSoc().setNextValue(value.get());
			this.channel(ChannelId.BAT_SOC).setNextValue(value.get());
			this.channel(SymmetricEss.ChannelId.SOC).setNextValue(value.get());
		});

		this.battery.getSoh().onChange(value -> {
			this.channel(ChannelId.BAT_SOH).setNextValue(value.get());
		});

		this.battery.getMaxCellTemperature().onChange(value -> {
			this.channel(ChannelId.BAT_TEMP).setNextValue(value.get());
		});
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/*
		 * DEBUG
		 */
		DEBUG_REQUESTED_STATE(new Doc()),
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
		AC_ENERGY(new Doc().unit(Unit.WATT_HOURS)), //
		AC_ENERGY_SF(new Doc().unit(Unit.NONE)), //
		DC_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
		DC_CURRENT_SF(new Doc().unit(Unit.NONE)), //	-2	
		DC_VOLTAGE(new Doc().unit(Unit.VOLT)), //
		DC_VOLTAGE_SF(new Doc().unit(Unit.NONE)), // -1
		DC_POWER(new Doc().unit(Unit.WATT)), //
		DC_POWER_SF(new Doc().unit(Unit.NONE)), // 1
		/*
		 * SUNSPEC_64201
		 */
		@SuppressWarnings("unchecked")
		REQUESTED_STATE(new Doc().options(RequestedState.values()) //
				// on each setNextWrite to the channel -> store the value in the DEBUG-channel
				.onInit(channel -> { //
					((WriteChannel<Integer>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_REQUESTED_STATE).setNextValue(value);
					});
				})),
		CURRENT_STATE(new Doc().options(CurrentState.values())), //
		WATCHDOG(new Doc().unit(Unit.SECONDS)), //
		W_SET_PCT(new Doc().unit(Unit.PERCENT)), //
		W_SET_PCT_SF(new Doc().unit(Unit.NONE)), //

		/*
		 * SUNSPEC_64202
		 */
		V_SF(new Doc().unit(Unit.NONE)), //
		A_SF(new Doc().unit(Unit.NONE)), //
		DEBUG_DIS_MIN_V(new Doc().unit(Unit.VOLT)), //
		@SuppressWarnings("unchecked")
		DIS_MIN_V(new Doc().unit(Unit.VOLT) //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Integer>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_DIS_MIN_V).setNextValue(value);
					});
				})), //
		DEBUG_DIS_MAX_A(new Doc().unit(Unit.AMPERE)), //
		@SuppressWarnings("unchecked")
		DIS_MAX_A(new Doc().unit(Unit.AMPERE) //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Integer>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_DIS_MAX_A).setNextValue(value);
					});
				})), //
//		DIS_CUTOFF_A(new Doc().text("Disconnect if discharge current lower than DisCutoffA")), // TODO scale factor
		DEBUG_CHA_MAX_V(new Doc().unit(Unit.VOLT)), //
		@SuppressWarnings("unchecked")
		CHA_MAX_V(new Doc().unit(Unit.VOLT) //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Integer>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_CHA_MAX_V).setNextValue(value);
					});
				})),
		DEBUG_CHA_MAX_A(new Doc().unit(Unit.AMPERE)), //
		@SuppressWarnings("unchecked")
		CHA_MAX_A(new Doc().unit(Unit.AMPERE) //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Integer>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_CHA_MAX_A).setNextValue(value);
					});
				})),
//		CHA_CUTOFF_A(new Doc().text("Disconnect if charge current lower than ChaCuttoffA")), // TODO scale factor
		DEBUG_EN_LIMIT(new Doc()), //
		@SuppressWarnings("unchecked")
		EN_LIMIT(new Doc().text("new battery limits are activated when EnLimit is 1") //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Integer>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_EN_LIMIT).setNextValue(value);
					});
				})),

		/*
		 * SUNSPEC_64203
		 */
		SOC_SF(new Doc().unit(Unit.NONE)), //
		SOH_SF(new Doc().unit(Unit.NONE)), //
		TEMP_SF(new Doc().unit(Unit.NONE)), //
		BAT_SOC(new Doc().unit(Unit.PERCENT)), //
		BAT_SOH(new Doc().unit(Unit.PERCENT)), //
		BAT_TEMP(new Doc().unit(Unit.DEGREE_CELSIUS)), //
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
	/*
	 * private final static int SUNSPEC_103 = 40071; private final static int
	 * SUNSPEC_121 = 40213; private final static int SUNSPEC_64201 = 40823; private
	 * final static int SUNSPEC_64202 = 40877; private final static int
	 * SUNSPEC_64203 = 40893; private final static int SUNSPEC_64302 = 40931;
	 */

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(SUNSPEC_103 + 24, Priority.LOW, //
						m(EssKacoBlueplanetGridsave50.ChannelId.AC_ENERGY, new UnsignedDoublewordElement(SUNSPEC_103 + 24)),
						m(EssKacoBlueplanetGridsave50.ChannelId.AC_ENERGY_SF, new SignedWordElement(SUNSPEC_103 + 26)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.DC_CURRENT, new UnsignedWordElement(SUNSPEC_103 + 27), ElementToChannelConverter.SCALE_FACTOR_1),
						m(EssKacoBlueplanetGridsave50.ChannelId.DC_CURRENT_SF, new SignedWordElement(SUNSPEC_103 + 28)),
						m(EssKacoBlueplanetGridsave50.ChannelId.DC_VOLTAGE, new UnsignedWordElement(SUNSPEC_103 + 29), ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(EssKacoBlueplanetGridsave50.ChannelId.DC_VOLTAGE_SF, new SignedWordElement(SUNSPEC_103 + 30)),
						m(EssKacoBlueplanetGridsave50.ChannelId.DC_POWER, new SignedWordElement(SUNSPEC_103 + 31), ElementToChannelConverter.SCALE_FACTOR_1),
						m(EssKacoBlueplanetGridsave50.ChannelId.DC_POWER_SF, new SignedWordElement(SUNSPEC_103 + 32))), //
				new FC3ReadRegistersTask(SUNSPEC_103 + 39, Priority.LOW, //
						m(EssKacoBlueplanetGridsave50.ChannelId.VENDOR_OPERATING_STATE,
								new SignedWordElement(SUNSPEC_103 + 39))), //				
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
						m(EssKacoBlueplanetGridsave50.ChannelId.W_SET_PCT_SF,
								new SignedWordElement(SUNSPEC_64201 + 46))), //
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

	private IntegerWriteChannel getDischargeMinVoltageChannel() {
		return this.channel(ChannelId.DIS_MIN_V);
	}

	private IntegerWriteChannel getDischargeMaxAmpereChannel() {
		return this.channel(ChannelId.DIS_MAX_A);
	}

	private IntegerWriteChannel getChargeMaxVoltageChannel() {
		return this.channel(ChannelId.CHA_MAX_V);
	}

	private IntegerWriteChannel getChargeMaxAmpereChannel() {
		return this.channel(ChannelId.CHA_MAX_A);
	}

	private IntegerWriteChannel getEnLimitChannel() {
		return this.channel(ChannelId.EN_LIMIT);
	}

	private IntegerWriteChannel getBatterySocChannel() {
		return this.channel(ChannelId.BAT_SOC);
	}

	private IntegerWriteChannel getBatterySohChannel() {
		return this.channel(ChannelId.BAT_SOH);
	}

	private IntegerWriteChannel getBatteryTempChannel() {
		return this.channel(ChannelId.BAT_TEMP);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable() {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(), //
				SymmetricEss.getModbusSlaveNatureTable(), //
				ManagedSymmetricEss.getModbusSlaveNatureTable() //
		);
	}

}
