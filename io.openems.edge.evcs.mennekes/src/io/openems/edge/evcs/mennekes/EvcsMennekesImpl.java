package io.openems.edge.evcs.mennekes;

import static io.openems.edge.bridge.modbus.api.ModbusUtils.readElementOnce;
import static io.openems.edge.bridge.modbus.api.ModbusUtils.FunctionCode.FC3;
import static io.openems.edge.evcs.api.Evcs.calculateUsedPhasesFromCurrent;
import static io.openems.edge.evcs.api.PhaseRotation.mapLongToPhaseRotatedActivePowerChannel;
import static io.openems.edge.evcs.api.PhaseRotation.mapLongToPhaseRotatedCurrentChannel;
import static io.openems.edge.evcs.api.PhaseRotation.Phase.L1;
import static io.openems.edge.evcs.api.PhaseRotation.Phase.L2;
import static io.openems.edge.evcs.api.PhaseRotation.Phase.L3;
import static io.openems.edge.meter.api.ElectricityMeter.calculateSumActivePowerFromPhases;
import static io.openems.edge.meter.api.ElectricityMeter.calculateSumCurrentFromPhases;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
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
import io.openems.common.types.MeterType;
import io.openems.common.types.SemanticVersion;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evcs.api.ChargeStateHandler;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.PhaseRotation;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.api.WriteHandler;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Mennekes", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class EvcsMennekesImpl extends AbstractOpenemsModbusComponent
		implements Evcs, ElectricityMeter, ManagedEvcs, OpenemsComponent, ModbusComponent, EventHandler, EvcsMennekes {

	private final Logger log = LoggerFactory.getLogger(EvcsMennekesImpl.class);

	// TODO: Add functionality to distinguish between firmware version. For firmware
	// version >= 5.22 there are several new registers. Currently it is programmed
	// for firmware version 5.14.
	// private boolean softwareVersionSmallerThan_5_22 = true;

	private Config config;

	@Reference
	private EvcsPower evcsPower;

	@Reference
	protected ConfigurationAdmin cm;

	/**
	 * Handles charge states.
	 */
	private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);

	/**
	 * Processes the controller's writes to this evcs component.
	 */
	private final WriteHandler writeHandler = new WriteHandler(this);

	public EvcsMennekesImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvcsMennekes.ChannelId.values());

		calculateUsedPhasesFromCurrent(this);
		calculateSumCurrentFromPhases(this);
		calculateSumActivePowerFromPhases(this);

	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		/*
		 * Calculates the maximum and minimum hardware power dynamically by listening on
		 * the fixed hardware limit and the phases used for charging
		 */
		Evcs.addCalculatePowerLimitListeners(this);
		this.applyConfig(config);

		this.detectSoftwareVersion();
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		this.applyConfig(config);
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	private void applyConfig(Config config) {
		this.config = config;
		this._setChargingType(ChargingType.AC);
		this._setFixedMaximumHardwarePower(this.getConfiguredMaximumHardwarePower());
		this._setFixedMinimumHardwarePower(this.getConfiguredMinimumHardwarePower());
		this._setPowerPrecision(230);
		this._setPhases(3);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE -> {
			if (!this.isReadOnly()) {
				this.writeHandler.run();
			}
			break;
		}
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		// TODO: Distinguish between firmware version. For firmware version >= 5.22
		// there are several new registers, e.g. Power is given by the charger since
		// firmware 5.22
		var modbusProtocol = new ModbusProtocol(this,
				new FC3ReadRegistersTask(104, Priority.HIGH,
						m(EvcsMennekes.ChannelId.OCPP_CP_STATUS, new UnsignedWordElement(104))),
				new FC3ReadRegistersTask(111, Priority.LOW, //
						m(new BitsWordElement(111, this)) //
								.bit(0, EvcsMennekes.ChannelId.ERR_ACTUATOR_UNLOCKED_WHILE_CHARGING) //
								.bit(1, EvcsMennekes.ChannelId.ERR_TILT_PREVENT_CHARGING_UNTIL_REBOOT) //
								.bit(2, EvcsMennekes.ChannelId.ERR_PIC24) //
								.bit(3, EvcsMennekes.ChannelId.ERR_USB_STICK_HANDLING) //
								.bit(4, EvcsMennekes.ChannelId.ERR_INCORRECT_PHASE_INSTALLATION) //
								.bit(5, EvcsMennekes.ChannelId.ERR_NO_POWER),
						m(new BitsWordElement(112, this)) //
								.bit(0, EvcsMennekes.ChannelId.ERR_RCMB_TRIGGERED) //
								.bit(1, EvcsMennekes.ChannelId.ERR_VEHICLE_STATE_E) //
								.bit(2, EvcsMennekes.ChannelId.ERR_MODE3_DIODE_CHECK) //
								.bit(3, EvcsMennekes.ChannelId.ERR_MCB_TYPE2_TRIGGERED) //
								.bit(4, EvcsMennekes.ChannelId.ERR_MCB_SCHUKO_TRIGGERED) //
								.bit(5, EvcsMennekes.ChannelId.ERR_RCD_TRIGGERED) //
								.bit(6, EvcsMennekes.ChannelId.ERR_CONTACTOR_WELD) //
								.bit(7, EvcsMennekes.ChannelId.ERR_BACKEND_DISCONNECTED) //
								.bit(8, EvcsMennekes.ChannelId.ERR_ACTUATOR_LOCKING_FAILED) //
								.bit(9, EvcsMennekes.ChannelId.ERR_ACTUATOR_LOCKING_WITHOUT_PLUG_FAILED) //
								.bit(10, EvcsMennekes.ChannelId.ERR_ACTUATOR_STUCK) //
								.bit(11, EvcsMennekes.ChannelId.ERR_ACTUATOR_DETECTION_FAILED) //
								.bit(12, EvcsMennekes.ChannelId.ERR_FW_UPDATE_RUNNING) //
								.bit(13, EvcsMennekes.ChannelId.ERR_TILT) //
								.bit(14, EvcsMennekes.ChannelId.ERR_WRONG_CP_PR_WIRING) //
								.bit(15, EvcsMennekes.ChannelId.ERR_TYPE2_OVERLOAD_THR_2)),
				new FC3ReadRegistersTask(122, Priority.HIGH,
						m(EvcsMennekes.ChannelId.VEHICLE_STATE, new UnsignedWordElement(122))),
				new FC3ReadRegistersTask(131, Priority.LOW,
						m(EvcsMennekes.ChannelId.SAFE_CURRENT, new UnsignedWordElement(131))),
				new FC3ReadRegistersTask(200, Priority.HIGH,
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(200))),
				new FC3ReadRegistersTask(206, Priority.HIGH, //
						m(new UnsignedDoublewordElement(206)).build() //
								.onUpdateCallback(mapLongToPhaseRotatedActivePowerChannel(this, L1)), //
						m(new UnsignedDoublewordElement(208)).build() //
								.onUpdateCallback(mapLongToPhaseRotatedActivePowerChannel(this, L2)), //
						m(new UnsignedDoublewordElement(210)).build() //
								.onUpdateCallback(mapLongToPhaseRotatedActivePowerChannel(this, L3)), //
						m(new UnsignedDoublewordElement(212)).build() //
								.onUpdateCallback(mapLongToPhaseRotatedCurrentChannel(this, L1)), //
						m(new UnsignedDoublewordElement(214)).build() //
								.onUpdateCallback(mapLongToPhaseRotatedCurrentChannel(this, L2)), //
						m(new UnsignedDoublewordElement(216)).build() //
								.onUpdateCallback(mapLongToPhaseRotatedCurrentChannel(this, L3))), //
				// TODO Voltages are missing
				new FC3ReadRegistersTask(1000, Priority.LOW,
						m(EvcsMennekes.ChannelId.EMS_CURRENT_LIMIT, new UnsignedWordElement(1000))),
				new FC16WriteRegistersTask(1000,
						m(EvcsMennekes.ChannelId.APPLY_CURRENT_LIMIT, new UnsignedWordElement(1000))));

		// Calculates required Channels from other existing Channels.
		this.addStatusListener();

		return modbusProtocol;
	}

	private void addStatusListener() {
		this.channel(EvcsMennekes.ChannelId.OCPP_CP_STATUS).onSetNextValue(s -> {
			var currentStatus = Status.UNDEFINED;
			MennekesOcppState rawState = s.asEnum();
			/**
			 * Maps the raw state into a {@link Status}.
			 */
			currentStatus = switch (rawState) {
			case CHARGING, FINISHING -> Status.CHARGING;
			case FAULTED -> Status.ERROR;
			case PREPARING -> Status.READY_FOR_CHARGING;
			case RESERVED -> Status.NOT_READY_FOR_CHARGING;
			case AVAILABLE, SUSPENDEDEV, SUSPENDEDEVSE -> Status.CHARGING_REJECTED;
			case OCCUPIED -> this.getActivePower().orElse(0) > 0 ? Status.CHARGING : Status.CHARGING_REJECTED;
			case UNAVAILABLE -> Status.ERROR;
			case UNDEFINED -> currentStatus;
			};
			this._setStatus(currentStatus);
		});
	}

	@Override
	public boolean isReadOnly() {
		return this.config.readOnly();
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return Math.round(this.config.minHwCurrent() / 1000f) * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return Math.round(this.config.maxHwCurrent() / 1000f) * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
	}

	@Override
	public boolean applyDisplayText(String text) throws OpenemsException {
		return false;
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		return 30;
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

	@Override
	public String debugLog() {
		return "Limit:" + this.getSetChargePowerLimit().orElse(null) + "|" + this.getStatus().getName();
	}

	public boolean isCharging() {
		return this.getActivePower().orElse(0) > 0;
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws Exception {
		if (this.isReadOnly()) {
			return false;
		}
		var phases = this.getPhasesAsInt();
		var current = Math.round(power / phases / Evcs.DEFAULT_VOLTAGE);

		/*
		 * Limits the charging value because Mennekes knows only values between 6 and 32
		 * A
		 */
		current = Math.min(current, Evcs.DEFAULT_MAXIMUM_HARDWARE_CURRENT / 1000);

		if (current < Evcs.DEFAULT_MINIMUM_HARDWARE_CURRENT / 1000) {
			current = 0;
		}

		this.setApplyCurrentLimit(current);
		return true;
	}

	@Override
	public boolean pauseChargeProcess() throws Exception {
		return this.applyChargePowerLimit(0);
	}

	@Override
	public MeterType getMeterType() {
		if (this.config.readOnly()) {
			return MeterType.CONSUMPTION_METERED;
		} else {
			return MeterType.MANAGED_CONSUMPTION_METERED;
		}
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

	private void detectSoftwareVersion() throws OpenemsException {
		readElementOnce(FC3, this.getModbusProtocol(), ModbusUtils::retryOnNull,
				new UnsignedDoublewordElement(100).wordOrder(WordOrder.MSWLSW)) //
				.thenAccept(registerValue -> {
					this.channel(EvcsMennekes.ChannelId.RAW_FIRMWARE_VERSION).setNextValue(registerValue);
					if (registerValue == null) {
						return;
					}

					final var firmwareVersion = parseSoftwareVersion(registerValue.intValue());
					this.channel(EvcsMennekes.ChannelId.FIRMWARE_VERSION.id()).setNextValue(firmwareVersion);
					final var outdated = !SemanticVersion.fromString(firmwareVersion)
							.isAtLeast(SemanticVersion.fromString("5.22"));
					this.getFirmwareOutdatedChannel().setNextValue(outdated);

					if (!outdated) {
						this.getModbusProtocol().addTasks(//
								new FC3ReadRegistersTask(705, Priority.HIGH,
										m(Evcs.ChannelId.ENERGY_SESSION, new UnsignedWordElement(705))), //
								new FC3ReadRegistersTask(706, Priority.LOW,
										m(EvcsMennekes.ChannelId.MAX_CURRENT_EV, new UnsignedWordElement(706)),
										new DummyRegisterElement(707, 708),
										m(EvcsMennekes.ChannelId.CHARGE_DURATION, new UnsignedWordElement(709)),
										new DummyRegisterElement(710, 711), //
										m(EvcsMennekes.ChannelId.MIN_CURRENT_LIMIT, new UnsignedWordElement(712))));
					}
				});
	}

	protected static String parseSoftwareVersion(int registerValue) {

		byte[] bytes = new byte[4];
		bytes[0] = (byte) ((registerValue >> 24) & 0xFF);
		bytes[1] = (byte) ((registerValue >> 16) & 0xFF);
		bytes[2] = (byte) ((registerValue >> 8) & 0xFF);
		bytes[3] = (byte) (registerValue & 0xFF);

		// Convert bytes to a string
		StringBuilder firmwareVersionBuilder = new StringBuilder();
		for (byte b : bytes) {
			if (b != 0) {
				firmwareVersionBuilder.append((char) b);
			}
		}

		final var firmwareVersion = firmwareVersionBuilder.toString();
		return firmwareVersion;
	}
}
