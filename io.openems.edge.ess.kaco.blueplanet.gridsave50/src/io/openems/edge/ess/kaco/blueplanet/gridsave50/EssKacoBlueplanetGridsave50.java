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

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
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
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.Kaco.BlueplanetGridsave50", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		}) //
// TODO: drop this Component in favour of KACO blueplanet Battery-Inverter implemention + Generic ESS.
public class EssKacoBlueplanetGridsave50 extends AbstractOpenemsModbusComponent
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent, TimedataProvider, EventHandler, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(EssKacoBlueplanetGridsave50.class);

	public static final int DEFAULT_UNIT_ID = 1;
	protected static final int MAX_APPARENT_POWER = 52000;

	private final CalculateEnergyFromPower calculateChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);

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

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private Battery battery;
	private Version version = Version.VERSION_5_34;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setBattery(Battery battery) {
		this.battery = battery;
	}

	public EssKacoBlueplanetGridsave50() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				ChannelId.values() //
		);
		this._setGridMode(GridMode.ON_GRID);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.version = config.sw_version();
		if (super.activate(context, config.id(), config.alias(), config.enabled(), DEFAULT_UNIT_ID, this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
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

		this.channel(ChannelId.W_MAX).onChange((oldValue, newValue) -> {
			@SuppressWarnings("unchecked")
			Optional<Integer> valueOpt = (Optional<Integer>) newValue.asOptional();
			if (!valueOpt.isPresent()) {
				return;
			}
			maxApparentPowerUnscaled = TypeUtils.getAsType(OpenemsType.INTEGER, newValue);
			refreshPower();
		});
		this.channel(ChannelId.W_MAX_SF).onChange((oldValue, newValue) -> {
			@SuppressWarnings("unchecked")
			Optional<Integer> valueOpt = (Optional<Integer>) newValue.asOptional();
			if (!valueOpt.isPresent()) {
				return;
			}
			Integer i = TypeUtils.getAsType(OpenemsType.INTEGER, newValue);
			maxApparentPowerScaleFactor = (int) Math.pow(10, i);
			refreshPower();
		});
	}

	private void refreshPower() {
		this.maxApparentPower = this.maxApparentPowerUnscaled * this.maxApparentPowerScaleFactor;
		if (this.maxApparentPower > 0) {
			this._setMaxApparentPower(this.maxApparentPower);
		}
	}

	@Override
	public Constraint[] getStaticConstraints() throws OpenemsException {
		if (this.isActivePowerAllowed) {
			return new Constraint[] { this.createPowerConstraint("Reactive power is not allowed", Phase.ALL,
					Pwr.REACTIVE, Relationship.EQUALS, 0) };
		} else {
			return new Constraint[] {
					this.createPowerConstraint("KACO inverter not ready", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS,
							0),
					this.createPowerConstraint("Reactive power is not allowed", Phase.ALL, Pwr.REACTIVE,
							Relationship.EQUALS, 0) };
		}
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsException {
		// TODO reactive power
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
			 * with scale factor see page 10 WSetPct = (WSet_Watt * 100) / ( W_Max_unscaled
			 * * 10^W_Max_SF * 10^WSetPct_SF)
			 */
			int WSetPct = (int) ((activePower * 100) / (max * Math.pow(10, scalefactor)));

			try {
				wSetPctChannel.setNextWriteValue(WSetPct);
			} catch (OpenemsNamedException e) {
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

		EnumReadChannel currentStateChannel = this.channel(ChannelId.CURRENT_STATE);
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
		case THROTTLED: // if inverter is throttled, maybe full power is not reachable, but the device
						// is working
			doGridConnectedHandling();
			break;
		case NO_ERROR_PENDING:
			doErrorHandling();
		case PRECHARGE:
		case SHUTTING_DOWN:
		case STARTING:
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
		// If the battery system is not ready yet set power to zero to avoid damaging or
		// improper system states
		if (battery.getStartStop() != StartStop.START) {
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
		int disMinV = battery.getDischargeMinVoltage().orElse(0);
		int chaMaxV = battery.getChargeMaxVoltage().orElse(0);
		int disMaxA = battery.getDischargeMaxCurrent().orElse(0);
		int chaMaxA = battery.getChargeMaxCurrent().orElse(0);
		int batSoC = battery.getSoc().orElse(0);
		int batSoH = battery.getSoh().orElse(0);
		int batTemp = battery.getMaxCellTemperature().orElse(0);

		// Update Power Constraints
		// TODO: The actual AC allowed charge and discharge should come from the KACO
		// Blueplanet instead of calculating it from DC parameters.
		final double EFFICIENCY_FACTOR = 0.9;

		// FIXME
		// allowedCharge += battery.getVoltage().value().orElse(0) *
		// battery.getChargeMaxCurrent().value().orElse(0) * -1;
		// allowedDischarge += battery.getVoltage().value().orElse(0) *
		// battery.getDischargeMaxCurrent().value().orElse(0);

		this._setAllowedChargePower((int) (chaMaxA * chaMaxV * -1 * EFFICIENCY_FACTOR));
		this._setAllowedDischargePower((int) (disMaxA * disMinV * EFFICIENCY_FACTOR));

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

			this._setCapacity(battery.getCapacity().get());
		} catch (OpenemsNamedException e) {
			log.error("Error during setBatteryRanges, " + e.getMessage());
		}
	}

	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Allowed:" //
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).value().asStringWithoutUnit() + ";" //
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).value().asString() //
				+ "|" + this.channel(ChannelId.CURRENT_STATE).value().asOptionString();
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
			handleStateMachine();
			this.calculateEnergy();
			break;
		}
	}

	private void startGridMode() {
		EnumWriteChannel requestedState = this.channel(ChannelId.REQUESTED_STATE);
		try {
			requestedState.setNextWriteValue(RequestedState.GRID_CONNECTED.getValue());
		} catch (OpenemsNamedException e) {
			log.error("problem occurred while trying to start grid mode" + e.getMessage());
		}
	}

	private void startSystem() {
		EnumWriteChannel requestedState = this.channel(ChannelId.REQUESTED_STATE);
		try {
			requestedState.setNextWriteValue(RequestedState.STANDBY.getValue());
		} catch (OpenemsNamedException e) {
			log.error("problem occurred while trying to start inverter" + e.getMessage());
		}
	}

	private void stopSystem() {
		EnumWriteChannel requestedState = this.channel(ChannelId.REQUESTED_STATE);
		try {
			requestedState.setNextWriteValue(RequestedState.OFF.getValue());
		} catch (OpenemsNamedException e) {
			log.error("problem occurred while trying to stop system" + e.getMessage());
		}
	}

	private void setWatchdog() {
		// according to 3.5.2.2 in the manual write watchdog register
		IntegerWriteChannel watchdogChannel = this.channel(ChannelId.WATCHDOG);
		try {
			watchdogChannel.setNextWriteValue(watchdogInterval);
		} catch (OpenemsNamedException e) {
			log.error("Watchdog timer could not be written!" + e.getMessage());
		}
	}

	/**
	 * writes current channel values to corresponding values of the channels given
	 * from interfaces
	 */
	private void doChannelMapping() {
		this.battery.getSocChannel().onChange((oldValue, newValue) -> {
			this._setSoc(newValue.get());
			this.channel(ChannelId.BAT_SOC).setNextValue(newValue.get());
		});

		this.battery.getSohChannel().onChange((oldValue, newValue) -> {
			this.channel(ChannelId.BAT_SOH).setNextValue(newValue.get());
		});

		this.battery.getMaxCellTemperatureChannel().onChange((oldValue, newValue) -> {
			this.channel(ChannelId.BAT_TEMP).setNextValue(newValue.get());
		});

		this.battery.getMinCellVoltageChannel().onChange((oldValue, newValue) -> {
			this._setMinCellVoltage(newValue.get());
		});

		this.battery.getMaxCellVoltageChannel().onChange((oldValue, newValue) -> {
			this._setMaxCellVoltage(newValue.get());
		});

		this.battery.getMinCellTemperatureChannel().onChange((oldValue, newValue) -> {
			this._setMinCellTemperature(newValue.get());
		});

		this.battery.getMaxCellTemperatureChannel().onChange((oldValue, newValue) -> {
			this._setMaxCellTemperature(newValue.get());
		});
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/*
		 * DEBUG
		 */
		DEBUG_REQUESTED_STATE(Doc.of(OpenemsType.INTEGER)),
		/*
		 * SUNSPEC_103
		 */
		// see error codes in user manual "10.10 Troubleshooting" (page 48)
		VENDOR_OPERATING_STATE(Doc.of(ErrorCode.values())),
		/*
		 * SUNSPEC_121
		 */
		W_MAX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		W_MAX_SF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		AC_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		AC_ENERGY_SF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		DC_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		DC_CURRENT_SF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), // -2
		DC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		DC_VOLTAGE_SF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), // -1
		DC_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		DC_POWER_SF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), // 1
		/*
		 * SUNSPEC_64201
		 */
		REQUESTED_STATE(Doc.of(RequestedState.values()) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onInit(new EnumWriteChannel.MirrorToDebugChannel(ChannelId.DEBUG_REQUESTED_STATE))),
		CURRENT_STATE(Doc.of(CurrentState.values())), //
		WATCHDOG(Doc.of(OpenemsType.INTEGER).unit(Unit.SECONDS) //
				.accessMode(AccessMode.WRITE_ONLY)),
		W_SET_PCT(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT) //
				.accessMode(AccessMode.WRITE_ONLY)),

		W_SET_PCT_SF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		/*
		 * SUNSPEC_64202
		 */
		V_SF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		A_SF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		DEBUG_DIS_MIN_V(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		DIS_MIN_V(new IntegerDoc() //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.WRITE_ONLY)
				.onInit(new IntegerWriteChannel.MirrorToDebugChannel(ChannelId.DEBUG_DIS_MIN_V))),

		DEBUG_DIS_MAX_A(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		DIS_MAX_A(new IntegerDoc() //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.WRITE_ONLY)
				.onInit(new IntegerWriteChannel.MirrorToDebugChannel(ChannelId.DEBUG_DIS_MAX_A))),
		// DIS_CUTOFF_A(Doc.of(OpenemsType.INTEGER) //
		// .text("Disconnect if discharge current lower than DisCutoffA")),
		// TODO scale factor
		DEBUG_CHA_MAX_V(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		CHA_MAX_V(new IntegerDoc() //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.WRITE_ONLY)
				.onInit(new IntegerWriteChannel.MirrorToDebugChannel(ChannelId.DEBUG_CHA_MAX_V))),
		DEBUG_CHA_MAX_A(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		CHA_MAX_A(new IntegerDoc() //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.WRITE_ONLY)
				.onInit(new IntegerWriteChannel.MirrorToDebugChannel(ChannelId.DEBUG_CHA_MAX_A))),
		// CHA_CUTOFF_A(Doc.of(OpenemsType.INTEGER) //
		// .text("Disconnect if charge current lower than ChaCuttoffA")),
		// TODO scale factor
		DEBUG_EN_LIMIT(Doc.of(OpenemsType.INTEGER)), //
		EN_LIMIT(new IntegerDoc() //
				.text("new battery limits are activated when EnLimit is 1") //
				.accessMode(AccessMode.WRITE_ONLY)
				.onInit(new IntegerWriteChannel.MirrorToDebugChannel(ChannelId.DEBUG_EN_LIMIT))),
		/*
		 * SUNSPEC_64203
		 */
		SOC_SF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		SOH_SF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		TEMP_SF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		BAT_SOC(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT) //
				.accessMode(AccessMode.WRITE_ONLY)),
		BAT_SOH(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT) //
				.accessMode(AccessMode.WRITE_ONLY)),
		BAT_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS) //
				.accessMode(AccessMode.WRITE_ONLY)),
		/*
		 * SUNSPEC_64302
		 */
		COMMAND_ID_REQ(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE) //
				.accessMode(AccessMode.WRITE_ONLY)),
		REQ_PARAM_0(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE) //
				.accessMode(AccessMode.WRITE_ONLY)),
		COMMAND_ID_REQ_ENA(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE) //
				.accessMode(AccessMode.WRITE_ONLY)),
		COMMAND_ID_RES(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		RETURN_CODE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	static enum Version {
		VERSION_5_34(40070, 40212, 40822, 40876, 40892, 40930), //
		VERSION_5_56(40070, 40212, 40888, 40942, 40958, 40996);
//		VERSION_5_56(40070, 40212, 41050, 41104, 41120, 41136);

		private Version(int sunSpec_103, int sunSpec_121, int sunSpec_64201, int sunSpec_64202, int sunSpec_64203,
				int sunSpec_64302) {
			this.sunSpec_103 = sunSpec_103;
			this.sunSpec_121 = sunSpec_121;
			this.sunSpec_64201 = sunSpec_64201;
			this.sunSpec_64202 = sunSpec_64202;
			this.sunSpec_64203 = sunSpec_64203;
			this.sunSpec_64302 = sunSpec_64302;
		}

		int sunSpec_103;
		int sunSpec_121;
		int sunSpec_64201;
		int sunSpec_64202;
		int sunSpec_64203;
		int sunSpec_64302;

	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

		int SUNSPEC_103 = version.sunSpec_103;
		int SUNSPEC_121 = version.sunSpec_121;
		int SUNSPEC_64201 = version.sunSpec_64201;
		int SUNSPEC_64202 = version.sunSpec_64202;
		int SUNSPEC_64203 = version.sunSpec_64203;
		int SUNSPEC_64302 = version.sunSpec_64302;

		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(SUNSPEC_103 + 24, Priority.LOW, //
						m(EssKacoBlueplanetGridsave50.ChannelId.AC_ENERGY,
								new UnsignedDoublewordElement(SUNSPEC_103 + 24)),
						m(EssKacoBlueplanetGridsave50.ChannelId.AC_ENERGY_SF, new SignedWordElement(SUNSPEC_103 + 26)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.DC_CURRENT, new UnsignedWordElement(SUNSPEC_103 + 27),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(EssKacoBlueplanetGridsave50.ChannelId.DC_CURRENT_SF, new SignedWordElement(SUNSPEC_103 + 28)),
						m(EssKacoBlueplanetGridsave50.ChannelId.DC_VOLTAGE, new UnsignedWordElement(SUNSPEC_103 + 29),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(EssKacoBlueplanetGridsave50.ChannelId.DC_VOLTAGE_SF, new SignedWordElement(SUNSPEC_103 + 30)),
						m(EssKacoBlueplanetGridsave50.ChannelId.DC_POWER, new SignedWordElement(SUNSPEC_103 + 31),
								ElementToChannelConverter.SCALE_FACTOR_1),
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
				new FC3ReadRegistersTask(SUNSPEC_64201 + 5, Priority.HIGH, //
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
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode) //
		);
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		Integer activePower = this.getActivePower().get();
		if (activePower == null) {
			// Not available
			this.calculateChargeEnergy.update(null);
			this.calculateDischargeEnergy.update(null);
		} else if (activePower > 0) {
			// Buy-From-Grid
			this.calculateChargeEnergy.update(0);
			this.calculateDischargeEnergy.update(activePower);
		} else {
			// Sell-To-Grid
			this.calculateChargeEnergy.update(activePower * -1);
			this.calculateDischargeEnergy.update(0);
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
