package io.openems.edge.evcs.compleo.eco20;

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

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ElementToChannelScaleFactorConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.timer.Timer;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.core.timer.TimerManager;
import io.openems.edge.evcs.api.ChargeStateHandler;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.EvcsUtils;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.api.WriteHandler;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
	name = "Evcs.Compleo.Eco20", //
	immediate = true, //
	configurationPolicy = ConfigurationPolicy.REQUIRE)
@EventTopics({ //
	EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
	EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class CompleoEco20Impl extends AbstractOpenemsModbusComponent
	implements CompleoEco20, ManagedEvcs, Evcs, ModbusComponent, TimedataProvider, EventHandler, OpenemsComponent {

    private static final int PILOTSIGNAL_DEACTIVATION_TIME = 10; // s

    private final Logger log = LoggerFactory.getLogger(CompleoEco20Impl.class);

    private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);

    /**
     * Processes the controller's writes to this evcs component.
     */
    private final WriteHandler writeHandler = new WriteHandler(this);

    private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
	    Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

    private Config config;
    private Timer startStopTimer;
    private Timer pilotSignalDeactivationTimer;

    private Status lastEvcsStatus = Status.ERROR;
    private boolean isCharging = false;
    private boolean isChargingOld = false;
    private boolean stopRequested = true;
    private boolean oldStopRequested = true;
    private Long energyAtStartOfSession = null;

    @Reference
    private EvcsPower evcsPower;

    @Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
    private volatile Timedata timedata = null;

    @Reference
    protected ConfigurationAdmin cm;

    @Reference
    private TimerManager timerManager;

    @Override
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
	super.setModbus(modbus);
    }

    public CompleoEco20Impl() {
	super(//
		OpenemsComponent.ChannelId.values(), //
		ModbusComponent.ChannelId.values(), //
		Evcs.ChannelId.values(), //
		ManagedEvcs.ChannelId.values(), //
		CompleoEco20.ChannelId.values() //
	);
    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsException {
	this.config = config;
	this.isCharging = false;
	this.stopRequested = true;
	this.oldStopRequested = true;
	this.startStopTimer = this.timerManager.getTimerByTime(this.channel(CompleoEco20.ChannelId.START_STOP_TIMER),
		config.commandStartStopDelay() + (int) (30.0 * Math.random()));
	this.pilotSignalDeactivationTimer = this.timerManager.getTimerByTime(
		this.channel(CompleoEco20.ChannelId.PILOT_SIGNAL_DEACTIVATION_TIMER), PILOTSIGNAL_DEACTIVATION_TIME);
	if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
		"Modbus", config.modbus_id())) {
	    return;
	}
	Evcs.addCalculatePowerLimitListeners(this);
	this.energyAtStartOfSession = null;
	this.setPowerLimits();
	this._setIsClustered(false);
	this._setSetEnergyLimit(null);
	var minHwPower = config.minHwCurrent() * Evcs.DEFAULT_VOLTAGE * 3 / 1000;
	var maxHwPower = config.maxHwCurrent() * Evcs.DEFAULT_VOLTAGE * 3 / 1000;
	this._setMinimumPower(minHwPower);
	this._setMaximumPower(maxHwPower);
	this._setFixedMinimumHardwarePower(minHwPower);
	this._setFixedMaximumHardwarePower(maxHwPower);

	this.installListener();
	try {
	    this.enableCharging();
	} catch (OpenemsNamedException e) {
	    this.logWarn(this.log, "Could not start charging station.");
	    e.printStackTrace();
	}
    }

    @Override
    @Deactivate
    protected void deactivate() {
	try {
	    this.disableCharging();
	} catch (OpenemsNamedException e) {
	    this.logWarn(this.log, "Could not stop charging station.");
	    e.printStackTrace();
	}
	super.deactivate();
    }

    private void enableCharging() throws OpenemsNamedException {
	this.setEnableCharging(true);
    }

    private void disableCharging() throws OpenemsNamedException {
	this.setEnableCharging(false);
    }

    private void setPowerLimits() {
	this._setChargingType(ChargingType.AC);
	this._setPowerPrecision(230);
	// TODO update phases continouosly from meter
	var phases = 3;
	this._setPhases(phases);

	this._setFixedMinimumHardwarePower(EvcsUtils.currentToPower(this.config.minHwCurrent() / 1000, phases));
	this._setFixedMaximumHardwarePower(EvcsUtils.currentToPower(this.config.maxHwCurrent() / 1000, phases));
    }

    private void installListener() {
	this.getCableCurrentLimitChannel().onUpdate(newValue -> {
	    if (!newValue.isDefined() || newValue.get() < this.config.minHwCurrent() / 1000) {
		return;
	    }
	    if (newValue.get() * 1000 < this.config.maxHwCurrent()) {
		this._setMaximumPower(newValue.get() * this.getPhases().getValue() * 230);
	    }
	});

	this.getActiveConsumptionEnergyChannel().onUpdate(newValue -> {
	    if (!newValue.isDefined()) {
		this._setActiveConsumptionEnergy(null);
		return;
	    }
	    this._setActiveConsumptionEnergy(newValue.get());
	});
	if (!this.config.hasIntegratedMeter()) {
	    this.getChargePowerChannel().onUpdate(chargePower -> {
		var currentInMilliampere = EvcsUtils.powerToCurrentInMilliampere(chargePower.orElse(0), 3);
		this.channel(Evcs.ChannelId.CURRENT_L1).setNextValue(currentInMilliampere);
		this.channel(Evcs.ChannelId.CURRENT_L2).setNextValue(currentInMilliampere);
		this.channel(Evcs.ChannelId.CURRENT_L3).setNextValue(currentInMilliampere);

	    });
	}
    }

    private void setCurrentFromPower(boolean stopRequested, int power) throws OpenemsNamedException {

	// convert power to ampere
	var phases = this.getPhases().getValue();
	var currentInMilliAmpere = EvcsUtils.powerToCurrentInMilliampere(power, phases);
	if (stopRequested) {
	    currentInMilliAmpere = 0;
	    this.disableCharging();
	} else {
	    this.enableCharging();
	}
	this.setDefaultChargingCurrent(currentInMilliAmpere);
    }

    @Override
    public void handleEvent(Event event) {
	if (!this.isEnabled()) {
	    return;
	}
	switch (event.getTopic()) {
	case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
	    this.updateCommunicationState();
	    this.updatePowerAndEnergySimulation();

	    this.evcsNotStartingWorkaround();
	    break;
	case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
	    this.writeHandler.run();
	    break;
	}

    }

    /**
     * Corsa E does not start charging after stopRequested = true ... stopRequested
     * = false cyclce. This workaround disables pilotsignal for some time. This
     * forces Corsa E to wake up and start charging
     */
    private void evcsNotStartingWorkaround() {
	if (!this.config.restartPilotSignal()) {
	    return;
	}
	try {
	    // detect if charging station should come up again
	    if (this.oldStopRequested && !this.stopRequested) {
		// disable pilotsignal
		this.setModifyChargingStationAvailability(false);
		this.pilotSignalDeactivationTimer.reset();
	    }
	    if (this.pilotSignalDeactivationTimer.checkAndReset()) {
		// activate pilotsignal
		this.setModifyChargingStationAvailability(true);
	    }
	    this.oldStopRequested = this.stopRequested;
	} catch (Exception e) {
	    this.logError(this.log, "Ex in evcsNotStartingWorkaround: " + e.getMessage());
	}
    }

    private void updateCommunicationState() {
	var stateOpt = this.getModbusCommunicationFailed();
	if (stateOpt.isDefined()) {
	    if (stateOpt.get().booleanValue()) {
		this.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED).setNextValue(Level.FAULT);
	    } else {
		this.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED).setNextValue(Level.OK);
	    }
	}
    }

    private void updatePowerAndEnergySimulation() {
	if (this.config.hasIntegratedMeter()) {
	    return;
	}

	// NOTE this is just a power and energy SIMULATION as we do not have a meter
	if (this.isCharging) {
	    if (!this.isChargingOld) {
		this.energyAtStartOfSession = this.getActiveConsumptionEnergy().orElse(0L);
	    }
	    int current = this.getSetChargingCurrent().orElse(0);
	    var phases = this.getPhases().getValue();
	    var power = EvcsUtils.currentInMilliampereToPower(current, phases);
	    this._setChargePower(power);
	    this.calculateConsumptionEnergy.update(power);
	    this._setEnergySession((int) (this.getActiveConsumptionEnergy().orElse(0L) - this.energyAtStartOfSession));

	} else {
	    this._setChargePower(0);
	    this.calculateConsumptionEnergy.update(0);
	}
	this.isChargingOld = this.isCharging;
    }

    private ElementToChannelConverter statusElementToChannelConverter() {
	return new ElementToChannelConverter(value -> {
	    var intValue = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, value);
	    if (intValue != null) {
		// see Table 8-3 of attached documentation
		switch (intValue) {
		case 65: // A - Device is in status A, no vehicle connected.
		    this.isCharging = false;
		    this.lastEvcsStatus = Status.NOT_READY_FOR_CHARGING;
		    return Status.NOT_READY_FOR_CHARGING;
		case 66: // B - Device is in status B, vehicle connected, no charging process.
		    if (this.isCharging) {
			this.isCharging = false;
			this.lastEvcsStatus = Status.CHARGING_FINISHED;
			return this.lastEvcsStatus;
		    }
		    if (this.lastEvcsStatus == Status.CHARGING_FINISHED) {
			return this.lastEvcsStatus;
		    }
		    this.isCharging = false;
		    return Status.READY_FOR_CHARGING;
		case 67: // C - Device is in status C, charging process can take place.
		case 68: // D - Device is in status D, charging process can take place.
		    this.isCharging = true;
		    this.lastEvcsStatus = Status.CHARGING;
		    return Status.CHARGING;
		case 69: // E - Device is in status E, error or charging station not ready.
		case 70: // F - Device is in status F, charging station not available for charging
		    // processes.
		    this.isCharging = false;
		    this.lastEvcsStatus = Status.ERROR;
		    return Status.ERROR;
		}
	    }
	    this.isCharging = false;
	    this.lastEvcsStatus = Status.UNDEFINED;
	    return Status.UNDEFINED;
	});
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

	AbstractModbusElement<?> activePower;
	AbstractModbusElement<?> sessionEnergy;
	AbstractModbusElement<?> totalEnergy;

	AbstractModbusElement<?> currentL1;
	AbstractModbusElement<?> currentL2;
	AbstractModbusElement<?> currentL3;

	ElementToChannelConverter integratedMeterConverter;
	if (this.config.hasIntegratedMeter()) {
	    integratedMeterConverter = ElementToChannelConverter.SCALE_FACTOR_MINUS_3;

	    currentL1 = this.m(Evcs.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(114), integratedMeterConverter);
	    currentL2 = this.m(Evcs.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(116), integratedMeterConverter);
	    currentL3 = this.m(Evcs.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(118), integratedMeterConverter);
	    activePower = this.m(Evcs.ChannelId.CHARGE_POWER, new SignedDoublewordElement(120), //
		    ElementToChannelConverter.SCALE_FACTOR_MINUS_3);
	    totalEnergy = this.m(Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(128), //
		    ElementToChannelConverter.SCALE_FACTOR_MINUS_3);
	    sessionEnergy = this.m(Evcs.ChannelId.ENERGY_SESSION, new UnsignedDoublewordElement(132), //
		    ElementToChannelConverter.SCALE_FACTOR_MINUS_3);

	} else {

	    currentL1 = new DummyRegisterElement(114, 115);
	    currentL2 = new DummyRegisterElement(116, 117);
	    currentL3 = new DummyRegisterElement(118, 119);
	    activePower = new DummyRegisterElement(120, 121);
	    totalEnergy = new DummyRegisterElement(128, 129);
	    sessionEnergy = new DummyRegisterElement(132, 133);
	    integratedMeterConverter = ElementToChannelConverter.DIRECT_1_TO_1;

	}

	return new ModbusProtocol(this,
		// see "Installing and starting up the EV Charge Control charging controller
		// user manual", phoenix contact, 2020-09-17
		// chaper 9.2 table 9.2

		new FC4ReadInputRegistersTask(100, Priority.LOW, //
			this.m(Evcs.ChannelId.STATUS, new UnsignedWordElement(100),
				this.statusElementToChannelConverter()), //
			// Current carrying capacity of charging cable (Proximity) --> One of the
			// limiting factors for charge power
			this.m(CompleoEco20.ChannelId.CABLE_CURRENT_LIMIT, new UnsignedWordElement(101)), //
			new DummyRegisterElement(102, 104), //
			this.m(CompleoEco20.ChannelId.FIRMWARE_VERSION, new UnsignedDoublewordElement(105)), //
			this.m(ManagedEvcs.ChannelId.SET_DISPLAY_TEXT, new UnsignedWordElement(107)), //
			this.m(CompleoEco20.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(108), //
				integratedMeterConverter), //
			this.m(CompleoEco20.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(110), //
				integratedMeterConverter), //
			this.m(CompleoEco20.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(112), //
				integratedMeterConverter), //
			currentL1, //
			currentL2, //
			currentL3, //
			activePower, //
			this.m(CompleoEco20.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(122), //
				integratedMeterConverter), //
			this.m(CompleoEco20.ChannelId.APPARENT_POWER, new SignedDoublewordElement(124), //
				integratedMeterConverter), //
			new DummyRegisterElement(126, 127), // power factor, does not work propoerly
			totalEnergy, //
			new DummyRegisterElement(130, 131), // max power during measurement
			sessionEnergy, //
			this.m(CompleoEco20.ChannelId.FREQUENCY, new UnsignedDoublewordElement(134), //
				ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
			this.m(CompleoEco20.ChannelId.MAX_CURRENT_L1, new UnsignedDoublewordElement(136)), //
			this.m(CompleoEco20.ChannelId.MAX_CURRENT_L2, new UnsignedDoublewordElement(138)), //
			this.m(CompleoEco20.ChannelId.MAX_CURRENT_L3, new UnsignedDoublewordElement(140)) //

		), //

		new FC3ReadRegistersTask(300, Priority.HIGH, //
			this.m(CompleoEco20.ChannelId.SET_CHARGING_CURRENT, new UnsignedWordElement(300), //
				new ElementToChannelScaleFactorConverter(this.config.model().scaleFactor))), //
		new FC3ReadRegistersTask(528, Priority.LOW,
			this.m(CompleoEco20.ChannelId.DEFAULT_CHARGING_CURRENT, new UnsignedWordElement(528), //
				new ElementToChannelScaleFactorConverter(this.config.model().scaleFactor))),
		new FC5WriteCoilTask(400, //
			this.m(CompleoEco20.ChannelId.ENABLE_CHARGING, new CoilElement(400))), //
		new FC5WriteCoilTask(402, //
			this.m(CompleoEco20.ChannelId.MODIFY_CHARGING_STATION_AVAILABILTY, new CoilElement(402))), //
		// requires at least Wallbe FW version 1.12
		new FC6WriteRegisterTask(528, //
			this.m(CompleoEco20.ChannelId.DEFAULT_CHARGING_CURRENT, new UnsignedWordElement(528), //
				new ElementToChannelScaleFactorConverter(this.config.model().scaleFactor))) //
	); //

	// FW Phonix Contact: V1.27 -> FIRMWARE_VERSION = 774977330
	// FW Compleo : SL-01.04.21 -> FIRMWARE_VERSION = 1280520237

    }

    @Override
    public EvcsPower getEvcsPower() {
	return this.evcsPower;
    }

    @Override
    public Timedata getTimedata() {
	return this.timedata;
    }

    @Override
    public String debugLog() {
	return this.getState() + "," + this.getStatus() + "," + this.getChargePower();
    }

    @Override
    public int getConfiguredMinimumHardwarePower() {
	return EvcsUtils.currentInMilliampereToPower(this.config.minHwCurrent(), 1);
    }

    @Override
    public int getConfiguredMaximumHardwarePower() {
	return EvcsUtils.currentInMilliampereToPower(this.config.maxHwCurrent(), 3);
    }

    @Override
    public boolean getConfiguredDebugMode() {
	return this.config.debugMode();
    }

    @Override
    public boolean applyChargePowerLimit(int power) throws Exception {
	int minPower = this.getMinimumHardwarePower()
		.orElse(this.getFixedMinimumHardwarePower().orElse(Evcs.DEFAULT_MINIMUM_HARDWARE_POWER));
	int maxPower = this.getMaximumHardwarePower()
		.orElse(this.getFixedMaximumHardwarePower().orElse(Evcs.DEFAULT_MAXIMUM_HARDWARE_POWER));

	// NOTE: if something confuses you here, have a more detailed look at the
	// hardware powers (they could have been changed via Modbus)

	var allowedPower = TypeUtils.fitWithin(minPower, maxPower, power);
	this._setSetChargePowerLimit(power);
 
	// reduce start/stop cycles to protect the car
	if (power < minPower) {
	    if (!this.stopRequested) {
		// hasChanged
		this.stopRequested = true;
		this.startStopTimer.reset();
	    }
	} else if (this.stopRequested) {
	    if (this.startStopTimer.check()) {
		this.stopRequested = false;
	    }
	}
	try {
	    this.setCurrentFromPower(this.stopRequested, allowedPower);
	    return true;
	} catch (OpenemsNamedException e) {
	    throw new OpenemsException("Could not apply charge power to " + this.id());
	}

    }

    @Override
    public boolean pauseChargeProcess() throws Exception {
	return this.applyChargePowerLimit(0);
    }

    @Override
    public boolean applyDisplayText(String text) throws OpenemsException {
	return false;
    }

    @Override
    public int getWriteInterval() {
	return 0;
    }

    @Override
    public int getMinimumTimeTillChargingLimitTaken() {
	// TODO Needs to be tested
	return 10;
    }

    @Override
    public ChargeStateHandler getChargeStateHandler() {
	return this.chargeStateHandler;
    }

    @Override
    public void logDebug(String message) {
	if (this.config.debugMode()) {
	    this.logInfo(this.log, message);
	}
    }

}