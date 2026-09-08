package io.openems.edge.evse.chargepoint.voltie;

import static io.openems.common.types.OpenemsType.LONG;
import static io.openems.edge.bridge.modbus.api.element.WordOrder.LSWMSW;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.evse.chargepoint.voltie.enums.LogVerbosity.WRITES;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.Duration;
import java.time.Instant;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.common.utils.IntUtils;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.bridge.modbus.api.task.Task.ExecuteState;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.chargepoint.voltie.enums.EvseState;
import io.openems.edge.evse.chargepoint.voltie.enums.LogVerbosity;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.Voltie", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
@GenerateTargetsFromReferences("Modbus")
public class EvseChargePointVoltieImpl extends AbstractOpenemsModbusComponent implements EvseChargePointVoltie,
		EvseChargePoint, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler, ModbusComponent {

	/** Minimum charge current in [A]. */
	public static final int MIN_CURRENT = 6;
	/**
	 * Maximum charge current in [A]. The current-limit register takes [mA] typed
	 * INT16, so the written value must never exceed 32767 mA.
	 */
	public static final int MAX_CURRENT = 32;
	/**
	 * Minimum firmware build for this register map. The readable status area has
	 * grown with the firmware: it ends at 0x0015 up to build 351, at 0x0018 from
	 * 352 and at 0x001A from 357. Reading a longer block than the charger serves
	 * is answered with exception 0x02, so the driver requires the build that
	 * covers the full block it reads.
	 */
	public static final int MIN_FIRMWARE = 357;

	/**
	 * Minimum interval between repeated writes of the same register, applied to
	 * the current limit (0x0014) and the phase-switch register (0x0016). A Cycle
	 * that carries a write issues a second transaction right after the block read;
	 * the charger answers it only in its next forwarding window (~470 ms
	 * measured), which is just inside the bridge's 500 ms timeout. Repeated writes
	 * are therefore rate-limited so this happens rarely. The charging-enable
	 * register (0x000C) is not repeated at all, see
	 * {@link #writeChargingEnabled(boolean)}.
	 */
	private static final Duration WRITE_INTERVAL = Duration.ofSeconds(5);
	/** Minimum interval between diagnostics for rejected control writes. */
	private static final Duration WRITE_ERROR_LOG_INTERVAL = Duration.ofMinutes(1);
	/**
	 * Warn if the communication-loss watchdog (register 0x0017) is configured
	 * below this many seconds: with the default cycle time of one second and the
	 * two register blocks, the effective poll interval per block can reach a few
	 * seconds.
	 */
	private static final int WATCHDOG_WARN_THRESHOLD = 5;
	/**
	 * Fallback for detecting a rejected phase-switch write via read-back: number
	 * of {@link #apply(ChargePointActions)} calls to wait after the first write.
	 * The primary signal is the FC6 onExecute callback; this is a generous safety
	 * net (roughly 30 s at the default cycle time, well within the
	 * PhaseSwitchHandler's 600 s timeout).
	 */
	private static final int PHASE_SWITCH_MAX_VERIFY_CYCLES = 30;

	/**
	 * Converts the session energy from [Ws] (register) to [Wh] (channel). The
	 * channel is read-only, so no channel-to-element conversion is required.
	 */
	private static final ElementToChannelConverter WATT_SECONDS_TO_WATT_HOURS = new ElementToChannelConverter(//
			value -> {
				Long ws = TypeUtils.getAsType(LONG, value);
				return ws == null ? null : ws / 3600L;
			});

	private final Logger log = LoggerFactory.getLogger(EvseChargePointVoltieImpl.class);
	// ACTIVE_PRODUCTION_ENERGY is integrated in software from ACTIVE_POWER: the
	// device's lifetime counter (register 0x2016) updates only at session end,
	// which would break live session-energy calculation in the EVSE controller.
	private final CalculateEnergyFromPower calculateTotalEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateEnergyL1 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1);
	private final CalculateEnergyFromPower calculateEnergyL2 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2);
	private final CalculateEnergyFromPower calculateEnergyL3 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3);

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	private Config config;
	private FC3ReadRegistersTask probeTask = null;
	private boolean protocolExtended = false;
	/**
	 * Set on the first successful read of the extended status block. The
	 * firmware probe alone is not enough: control must not start before the
	 * hardware current limit is known, otherwise the set-point ability would
	 * fall back to the minimum and throttle a vehicle that is already
	 * charging.
	 */
	private boolean statusBlockRead = false;
	private Integer rawEvseState = null;
	private Instant lastCurrentWrite = Instant.MIN;
	private Integer lastWrittenCurrent = null;
	private Boolean lastWrittenEnable = null;
	private boolean chargingEnableConfirmed = false;
	private Instant lastControlWriteErrorLog = Instant.MIN;
	private boolean watchdogWarningLogged = false;

	// Phase-switch state. The stop -> switch -> start orchestration is done by the
	// Evse.Controller.Single PhaseSwitchHandler; the driver only performs the
	// register write and verifies it via the onExecute callback and read-back.
	private Instant lastPhaseSwitchWrite = Instant.MIN;
	private boolean phaseSwitchWritePending = false;
	private int phaseSwitchVerifyCycles = 0;
	private boolean phaseSwitchUnavailable = false;

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))")
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public EvseChargePointVoltieImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				EvseChargePointVoltie.ChannelId.values() //
		);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		this.config = config;
		// Allow retrying phase switching after a configuration update
		this.phaseSwitchUnavailable = false;
		this.phaseSwitchWritePending = false;
		this.phaseSwitchVerifyCycles = 0;
		super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		// Firmware builds before 357 serve a shorter status area, so reading the full
		// 27-register block would always fail with exception 0x02 and mark the
		// component defective. Start with a short probe of charger-ID and firmware
		// build only; the full register map is added dynamically once the firmware
		// build is confirmed - see onFirmwareBuildUpdate(). As the only LOW task it
		// is executed every Cycle.
		this.probeTask = new FC3ReadRegistersTask(0x0000, Priority.LOW, //
				m(EvseChargePointVoltie.ChannelId.CHARGER_ID, new UnsignedWordElement(0x0000)), //
				m(EvseChargePointVoltie.ChannelId.FIRMWARE_BUILD, new UnsignedWordElement(0x0001)) //
						.onUpdateCallback(this::onFirmwareBuildUpdate));
		return new ModbusProtocol(this, this.probeTask);
	}

	private void onFirmwareBuildUpdate(Integer firmwareBuild) {
		final var outdated = firmwareBuild != null && firmwareBuild < MIN_FIRMWARE;
		setValue(this, EvseChargePointVoltie.ChannelId.FIRMWARE_OUTDATED, outdated);
		if (firmwareBuild == null || outdated || this.protocolExtended) {
			return;
		}
		this.protocolExtended = true;
		this.extendModbusProtocol();
	}

	/**
	 * Replaces the firmware probe with the full register map. Called once, after
	 * the firmware build has been confirmed to be at least {@link #MIN_FIRMWARE}.
	 */
	private synchronized void extendModbusProtocol() {
		// The charger's Linux board polls its own EVSE status about every 500 ms and
		// forwards Modbus requests only in the gap between those polls. Measured on
		// real hardware (FW 352): a request that hits a free forwarding window is
		// answered in ~20 ms, while a request issued immediately after another one
		// waits out the next window and takes 447..497 ms. The OpenEMS Modbus TCP
		// bridge timeout is hardcoded at 500 ms, so two transactions in one Cycle
		// leave almost no margin.
		//
		// Therefore BOTH read blocks are Priority.LOW: the bridge executes exactly
		// one LOW read per Cycle (round-robin), so every read lands in a fresh
		// forwarding window. At the default Cycle time of 1 s each block refreshes
		// every ~2 s. Writes are event-driven: repeated writes of the current limit
		// and the phase-switch register are rate-limited to one per 5 s, and the
		// charging-enable register is written once per state change. Only FC6 (write
		// single register) is accepted, FC16 is rejected with exception 0x01.
		final var protocol = this.getModbusProtocol();
		final var phaseRotation = this.getPhaseRotation();

		protocol.removeTask(this.probeTask);
		protocol.addTasks(//
				// Status/config block: 0x0000..0x001A (27 registers)
				new FC3ReadRegistersTask(0x0000, Priority.LOW, //
						m(EvseChargePointVoltie.ChannelId.CHARGER_ID, new UnsignedWordElement(0x0000)), //
						m(EvseChargePointVoltie.ChannelId.FIRMWARE_BUILD, new UnsignedWordElement(0x0001)) //
								.onUpdateCallback(this::onFirmwareBuildUpdate), //
						m(EvseChargePointVoltie.ChannelId.MCU_SERIAL, //
								new UnsignedQuadruplewordElement(0x0002).wordOrder(LSWMSW)), //
						m(EvseChargePointVoltie.ChannelId.POWER_BOARD_SERIAL, //
								new UnsignedQuadruplewordElement(0x0006).wordOrder(LSWMSW)), //
						m(EvseChargePointVoltie.ChannelId.EVSE_STATE, new UnsignedWordElement(0x000A)) //
								.onUpdateCallback(raw -> this.rawEvseState = raw), //
						m(EvseChargePointVoltie.ChannelId.AUTOSTART_ENABLED, new UnsignedWordElement(0x000B)), //
						m(EvseChargePointVoltie.ChannelId.CHARGING_ENABLED, new UnsignedWordElement(0x000C)), //
						m(EvseChargePointVoltie.ChannelId.CHARGING_ACTIVE, new UnsignedWordElement(0x000D)), //
						m(EvseChargePointVoltie.ChannelId.MAINS_PHASES, new UnsignedWordElement(0x000E)), //
						m(EvseChargePointVoltie.ChannelId.STORED_DLM_MODE, new UnsignedWordElement(0x000F)), //
						// 0x0010 and 0x0011 are reserved config/mode flags. They are read as part
						// of the block but must NEVER be written: writing 0x0011 force-sets the
						// control-by-Modbus bit.
						new DummyRegisterElement(0x0010, 0x0011), //
						m(EvseChargePointVoltie.ChannelId.CHARGE_STOP_REASON, new UnsignedWordElement(0x0012)), //
						m(EvseChargePointVoltie.ChannelId.AUTONOMOUS_CURRENT_LIMIT, new UnsignedWordElement(0x0013)), //
						m(EvseChargePointVoltie.ChannelId.CURRENT_LIMIT, new UnsignedWordElement(0x0014)), //
						m(EvseChargePointVoltie.ChannelId.EFFECTIVE_DLM_MODE, new UnsignedWordElement(0x0015)), //
						m(EvseChargePointVoltie.ChannelId.FORCED_SINGLE_PHASE, new UnsignedWordElement(0x0016)), //
						m(EvseChargePointVoltie.ChannelId.COMM_WATCHDOG_TIMEOUT, new UnsignedWordElement(0x0017)) //
								.onUpdateCallback(this::onWatchdogTimeoutUpdate), //
						m(EvseChargePointVoltie.ChannelId.HARDWARE_CURRENT_LIMIT, new UnsignedWordElement(0x0018)) //
								.onUpdateCallback(v -> this.statusBlockRead |= v != null), //
						m(EvseChargePointVoltie.ChannelId.CAPABILITY_FLAGS, new UnsignedWordElement(0x0019)), //
						// Phases actually under load, as counted by the firmware from 1.0 A per
						// phase; 0 while no charging is in progress
						m(EvseChargePointVoltie.ChannelId.PHASES_IN_USE, new UnsignedWordElement(0x001A))), //

				// Meter block: 0x2000..0x201D; all INT32 values are MSW-first
				new FC3ReadRegistersTask(0x2000, Priority.LOW, //
						// Phase voltages are reported in [mV]; ElectricityMeter VOLTAGE_L*
						// channels are in [mV] as well
						m(phaseRotation.channelVoltageL1(), new UnsignedDoublewordElement(0x2000)), //
						m(phaseRotation.channelVoltageL2(), new UnsignedDoublewordElement(0x2002)), //
						m(phaseRotation.channelVoltageL3(), new UnsignedDoublewordElement(0x2004)), //
						// Phase charging currents are reported in [mA]
						m(phaseRotation.channelCurrentL1(), new UnsignedDoublewordElement(0x2006)), //
						m(phaseRotation.channelCurrentL2(), new UnsignedDoublewordElement(0x2008)), //
						m(phaseRotation.channelCurrentL3(), new UnsignedDoublewordElement(0x200A)), //
						m(EvseChargePointVoltie.ChannelId.CHARGE_DURATION, new UnsignedDoublewordElement(0x200C)), //
						m(EvseChargePointVoltie.ChannelId.ENERGY_SESSION, new UnsignedDoublewordElement(0x200E),
								WATT_SECONDS_TO_WATT_HOURS), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(0x2010)), //
						m(EvseChargePointVoltie.ChannelId.AVAILABLE_CURRENT_CAPACITY,
								new UnsignedDoublewordElement(0x2012)), //
						// 0x2014 and 0x2015 carry the MCU and power-board temperatures. They are
						// not part of the customer-facing Modbus API documentation, so they are
						// read as part of the block but not exposed as Channels.
						new DummyRegisterElement(0x2014, 0x2015), //
						// Lifetime energy in [Wh]; updated by the charger only at session end
						m(EvseChargePointVoltie.ChannelId.LIFETIME_ENERGY, new UnsignedDoublewordElement(0x2016)), //
						m(phaseRotation.channelActivePowerL1(), new SignedDoublewordElement(0x2018)), //
						m(phaseRotation.channelActivePowerL2(), new SignedDoublewordElement(0x201A)), //
						m(phaseRotation.channelActivePowerL3(), new SignedDoublewordElement(0x201C))));

		// The write tasks are registered independently of the readOnly configuration.
		// This method runs exactly once and the Modbus protocol is never rebuilt on a
		// configuration update, so gating the tasks here would leave a charge-point
		// that was activated read-only permanently unable to write after readOnly is
		// switched off - silently, because apply() would still set the Channels.
		// Read-only mode is enforced in apply() and getChargePointAbilities() instead;
		// a write task whose Channel never receives a value issues no Modbus request.
		protocol.addTasks(//
				new FC6WriteRegisterTask(this::onChargingEnabledWriteExecuted, 0x000C,
						m(EvseChargePointVoltie.ChannelId.SET_CHARGING_ENABLED, new UnsignedWordElement(0x000C))),
				new FC6WriteRegisterTask(this::onControlWriteExecuted, 0x0014,
						m(EvseChargePointVoltie.ChannelId.SET_CURRENT_LIMIT, new UnsignedWordElement(0x0014))),
				new FC6WriteRegisterTask(this::onPhaseSwitchWriteExecuted, 0x0016,
						m(EvseChargePointVoltie.ChannelId.SET_FORCED_SINGLE_PHASE, new UnsignedWordElement(0x0016))));
	}

	/**
	 * Called after execution of the FC6 write to register 0x000C. In addition to
	 * the shared diagnostics it clears the write-once latch, because a write that
	 * answered a Modbus exception never reached the charger and has to be
	 * repeated. See {@link #writeChargingEnabled(boolean)}.
	 *
	 * @param state the {@link ExecuteState}
	 */
	protected void onChargingEnabledWriteExecuted(ExecuteState state) {
		if (state instanceof ExecuteState.Error) {
			this.lastWrittenEnable = null;
			this.chargingEnableConfirmed = false;
		}
		this.onControlWriteExecuted(state);
	}

	/**
	 * Called after execution of the FC6 writes to registers 0x000C and 0x0014. A
	 * rejected write answers a Modbus exception, which the bridge reports as
	 * {@link ExecuteState.Error}.
	 *
	 * @param state the {@link ExecuteState}
	 */
	protected void onControlWriteExecuted(ExecuteState state) {
		if (!(state instanceof ExecuteState.Error)) {
			return;
		}
		var now = Instant.now();
		if (Duration.between(this.lastControlWriteErrorLog, now).compareTo(WRITE_ERROR_LOG_INTERVAL) < 0) {
			return;
		}
		this.lastControlWriteErrorLog = now;
		this.logWarn(this.log, "Control write was rejected by the charge-point; "
				+ "check that Modbus control is enabled on the charger");
	}

	/**
	 * Called after execution of the FC6 write to register 0x0016. The charger
	 * rejects the write with Modbus exception 0x03 when control-by-Modbus is
	 * disabled, on relay/EEPROM error, or while a charging session is active; the
	 * bridge reports this as {@link ExecuteState.Error}. Hardware that does not
	 * support phase switching at all is already excluded by the capability
	 * bitmask, see {@link #getPhaseSwitchAbility()}.
	 *
	 * @param state the {@link ExecuteState}
	 */
	protected void onPhaseSwitchWriteExecuted(ExecuteState state) {
		if (!(state instanceof ExecuteState.Error)) {
			return;
		}
		this.logWarn(this.log, "Phase-switch write was rejected by the charge-point "
				+ "(control-by-Modbus disabled or relay/EEPROM error); "
				+ "marking phase switching as unavailable");
		this.phaseSwitchUnavailable = true;
	}

	private void onWatchdogTimeoutUpdate(Integer timeout) {
		if (this.watchdogWarningLogged || timeout == null) {
			return;
		}
		if (timeout > 0 && timeout < 255 && timeout < WATCHDOG_WARN_THRESHOLD) {
			this.watchdogWarningLogged = true;
			this.logWarn(this.log, "Voltie communication-loss watchdog is set to " + timeout
					+ " s. If Modbus polling stalls for that long, the charger reduces the allowed current to 0 A. "
					+ "Increase the watchdog in the Voltie app or reduce the OpenEMS cycle time.");
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> this.onBeforeProcessImage();
		}
	}

	private void onBeforeProcessImage() {
		// Integrate the total energy and the energy per phase from the power values
		this.calculateTotalEnergy.update(this.getActivePowerChannel().getNextValue().get());
		this.calculateEnergyL1.update(this.getActivePowerL1Channel().getNextValue().get());
		this.calculateEnergyL2.update(this.getActivePowerL2Channel().getNextValue().get());
		this.calculateEnergyL3.update(this.getActivePowerL3Channel().getNextValue().get());

		final EvseState evseState = this.getEvseStateChannel().getNextValue().asEnum();

		// IS_READY_FOR_CHARGING: vehicle connected and no error
		setValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, evseState.isEvConnected());

		// Map error states to the FAULT State-Channel; unknown non-zero states are
		// defensively treated as errors, so the raw register value is evaluated
		setValue(this, EvseChargePointVoltie.ChannelId.EVSE_FAULT, EvseState.isErrorValue(this.rawEvseState));

		// Reflect the phase-switch availability
		setValue(this, EvseChargePointVoltie.ChannelId.PHASE_SWITCH_FAILED, this.phaseSwitchUnavailable);
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		if (this.config == null || this.isReadOnly() || !this.statusBlockRead) {
			// Until the status block has been read the hardware current limit is
			// unknown, so no set-point ability is advertised: a fallback maximum
			// would command a vehicle that is already charging down to the minimum.
			return ChargePointAbilities.create().build();
		}

		final var phase = this.getEffectivePhase();
		final var maxCurrent = this.getMaxCurrent();
		final EvseState evseState = this.getEvseState();

		var abilities = ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Ampere(phase, MIN_CURRENT, maxCurrent)) //
				.setIsEvConnected(evseState.isEvConnected()) //
				.setIsReadyForCharging(this.getIsReadyForCharging());

		final var phaseSwitchDirection = this.getPhaseSwitchAbility();
		if (phaseSwitchDirection != null) {
			abilities.setPhaseSwitchManual(phaseSwitchDirection);
		}

		return abilities.build();
	}

	/**
	 * Gets the phases the EV can charge with right now.
	 *
	 * <p>
	 * The forced single-phase register decides first; otherwise the configured
	 * hardware wiring applies.
	 *
	 * @return the {@link SingleOrThreePhase}
	 */
	private SingleOrThreePhase getEffectivePhase() {
		if (this.config.wiring() == SINGLE_PHASE) {
			return SINGLE_PHASE;
		}
		if (this.getForcedSinglePhase().orElse(false)) {
			return SINGLE_PHASE;
		}
		return THREE_PHASE;
	}

	/**
	 * Gets the maximum charge current in [A].
	 *
	 * <p>
	 * The hardware maximum (register 0x0018) is the EVSE-board potentiometer
	 * limit. When the reading is unavailable or implausible, the safe minimum of
	 * 6 A is used. The cable-aware dynamic capacity (register 0x2012) is
	 * deliberately not used here, because it may reflect the currently applied
	 * software limit and would latch the maximum down; it is available as a
	 * Channel.
	 *
	 * @return the value
	 */
	private int getMaxCurrent() {
		final var hardwareLimit = this.getHardwareCurrentLimit().get(); // [mA]
		if (hardwareLimit == null || hardwareLimit < MIN_CURRENT * 1000) {
			// unavailable or implausible potentiometer reading -> fail safe
			return MIN_CURRENT;
		}
		if (hardwareLimit > MAX_CURRENT * 1000) {
			return MAX_CURRENT;
		}
		return hardwareLimit / 1000; // truncate to 1 A resolution
	}

	/**
	 * Gets the phase-switch direction to offer, or null if phase switching is not
	 * available.
	 *
	 * <p>
	 * The primary gate is bit 0 of the capability bitmask (register 0x0019), which
	 * reports whether the forced single-phase register 0x0016 is writable on this
	 * hardware. Modbus API v1.3 asks clients to read that bit once and to offer
	 * phase switching only when it is set, rather than probing with a 0x0016
	 * write: on unsupported hardware the write is rejected with exception 0x03,
	 * and a client that retries it can repeatedly interrupt charging. The bitmask
	 * is unavailable until the protocol has been extended, so the ability is not
	 * offered before the first successful block read.
	 *
	 * <p>
	 * The {@link #phaseSwitchUnavailable} latch is kept on top of that gate as a
	 * safety net for a write the charger rejects for another reason, rather than
	 * retrying: every rejected switch costs up to 600 s at 0 A while the
	 * PhaseSwitchHandler waits out its timeout. The latch is reset on a
	 * configuration update, see {@link #modified(ComponentContext, Config)}.
	 *
	 * @return the {@link PhaseSwitchDirection} or null
	 */
	private PhaseSwitchDirection getPhaseSwitchAbility() {
		if (!this.isPhaseSwitchingSupported() // not supported by this hardware
				|| this.phaseSwitchUnavailable // a previous switch was rejected
				|| this.config.wiring() == SINGLE_PHASE // single-phase wiring can never switch
				|| this.getFirmwareBuild().orElse(0) < MIN_FIRMWARE) { // requires firmware 357
			return null;
		}
		final Value<Boolean> forcedSinglePhase = this.getForcedSinglePhase();
		if (forcedSinglePhase.get() == null) {
			// still waiting for the first read
			return null;
		}
		// Keep advertising the same direction until the read-back flips; the
		// PhaseSwitchHandler detects completion via Ability.phase().
		return forcedSinglePhase.get() //
				? PhaseSwitchDirection.TO_THREE_PHASE //
				: PhaseSwitchDirection.TO_SINGLE_PHASE;
	}

	@Override
	public void apply(ChargePointActions actions) {
		if (this.config == null || this.isReadOnly()) {
			return;
		}
		if (!this.protocolExtended || !this.statusBlockRead) {
			// The write tasks and their Channel-to-Element bindings are only created in
			// extendModbusProtocol(), i.e. after the first successful read of the
			// firmware build. A Channel write issued before that is silently dropped by
			// the bridge, while the write-tracking state would already be updated -
			// which would suppress or delay the first effective write.
			return;
		}

		// The stop -> switch -> start orchestration is done by the
		// Evse.Controller.Single PhaseSwitchHandler (STOP_CHARGE at 0 A until power
		// is below 100 W, then the phase-switch action is re-sent every cycle, then
		// START_CHARGE). ChargePointActions carry the zero set-point and the
		// phase-switch action together, so both are handled in the same call.
		final var phaseSwitch = actions.phaseSwitch();
		if (phaseSwitch != null) {
			this.applyPhaseSwitch(phaseSwitch.direction());
		}

		this.applySetPoint(actions.getApplySetPointInAmpere().value());
	}

	private void applySetPoint(int currentInAmpere) {
		try {
			this.evaluateChargingEnableLatch();

			if (currentInAmpere <= 0) {
				// Stop charging via the charging-enabled register; the current limit is
				// left untouched
				if (this.getChargingEnabled().orElse(true)) {
					this.writeChargingEnabled(false);
				}
				return;
			}

			// Re-enable charging if it was disabled
			if (!this.getChargingEnabled().orElse(false)) {
				this.writeChargingEnabled(true);
			}

			// The register takes [mA] with 1 A resolution, valid 6000..32000; never
			// write above 32767 (INT16)
			final var current = IntUtils.fitWithin(MIN_CURRENT, this.getMaxCurrent(), currentInAmpere);
			final var milliAmpere = current * 1000;

			if (this.lastWrittenCurrent != null && this.lastWrittenCurrent == current
					&& this.getCurrentLimit().orElse(-1) == milliAmpere) {
				return;
			}
			// Rate-limit writes: a write adds a second transaction to the Cycle, which
			// the charger answers only in its next forwarding window
			final var now = Instant.now();
			if (Duration.between(this.lastCurrentWrite, now).compareTo(WRITE_INTERVAL) < 0) {
				return;
			}
			this.lastCurrentWrite = now;
			this.lastWrittenCurrent = current;
			this.log(WRITES, "Set current limit to " + current + " A");
			this.setCurrentLimit(milliAmpere);

		} catch (OpenemsNamedException e) {
			this.logWarn(this.log, "Failed to apply set-point [" + currentInAmpere + " A]: " + e.getMessage());
		}
	}

	/**
	 * Detects that the charger left a charging-enabled state it had already
	 * confirmed, for example after a firmware restart, which resets register
	 * 0x000C to 0. That is a new situation rather than a write that never took
	 * effect, so the write-once latch is cleared and the next
	 * {@link #applySetPoint(int)} writes the register again.
	 */
	void evaluateChargingEnableLatch() {
		final var intended = this.lastWrittenEnable;
		final var readBack = this.getChargingEnabled().get();
		if (intended == null || readBack == null) {
			return;
		}
		if (readBack.booleanValue() == intended.booleanValue()) {
			this.chargingEnableConfirmed = true;
		} else if (this.chargingEnableConfirmed) {
			this.lastWrittenEnable = null;
			this.chargingEnableConfirmed = false;
		}
	}

	/**
	 * Writes the charging-enabled register 0x000C once per intended state change.
	 *
	 * <p>
	 * Modbus API v1.3 is explicit about this register: writing 1 only permits
	 * charging, it does not force it. The firmware enables the session only when a
	 * vehicle is already connected, so with no vehicle plugged in the write is
	 * accepted without an exception but has no effect and the read-back stays 0
	 * indefinitely - a client that keeps rewriting the register until the
	 * read-back matches loops forever. The driver therefore writes the value once
	 * and then follows the outcome on the EVSE state (0x000A) and the charging
	 * flag (0x000D).
	 *
	 * <p>
	 * Two guards make sure a genuine state change is never lost: a write that
	 * answered a Modbus exception is retried, see
	 * {@link #onChargingEnabledWriteExecuted(ExecuteState)}, and a charger that
	 * leaves an already confirmed state is written again, see
	 * {@link #evaluateChargingEnableLatch()}.
	 *
	 * @param enable true to enable charging
	 * @return true if the write was applied; false if the same value has already
	 *         been written
	 * @throws OpenemsNamedException on error
	 */
	boolean writeChargingEnabled(boolean enable) throws OpenemsNamedException {
		if (this.lastWrittenEnable != null && this.lastWrittenEnable.booleanValue() == enable) {
			return false;
		}
		this.lastWrittenEnable = enable;
		this.chargingEnableConfirmed = false;
		this.log(WRITES, enable ? "Enable charging" : "Disable charging");
		this.setChargingEnabled(enable);
		return true;
	}

	private void applyPhaseSwitch(PhaseSwitchDirection direction) {
		if (this.phaseSwitchUnavailable) {
			return;
		}
		final var targetSinglePhase = switch (direction) {
		case TO_SINGLE_PHASE -> true;
		case TO_THREE_PHASE -> false;
		};

		final var readBack = this.getForcedSinglePhase().get();
		if (readBack == null) {
			// Still waiting for a read of register 0x0016 (e.g. Modbus communication
			// failure); do not count verify cycles without a valid read-back
			this.phaseSwitchVerifyCycles = 0;
			return;
		}
		if (readBack.booleanValue() == targetSinglePhase) {
			// Switch is complete. Ability.phase() flips via getEffectivePhase(), which
			// the PhaseSwitchHandler detects as completion.
			this.phaseSwitchWritePending = false;
			this.phaseSwitchVerifyCycles = 0;
			return;
		}

		// Writing register 0x0016 while a charging session is active is rejected
		// with exception 0x03. The PhaseSwitchHandler applies a zero set-point and
		// waits before sending this action; re-check to avoid rejected writes.
		if (this.getChargingActive().orElse(true)) {
			return;
		}

		// Fallback detection of a rejected write. The primary signal is the FC6
		// onExecute callback (onPhaseSwitchWriteExecuted); this covers the case that
		// no Modbus exception is received. Cycles with failed communication are not
		// counted (the read-back would be null and handled above).
		if (this.phaseSwitchWritePending
				&& !this.getModbusCommunicationFailedChannel().value().orElse(false)
				&& ++this.phaseSwitchVerifyCycles > PHASE_SWITCH_MAX_VERIFY_CYCLES) {
			this.logWarn(this.log, "Phase-switch write was not confirmed by the charge-point; "
					+ "marking phase switching as unavailable");
			this.phaseSwitchUnavailable = true;
			return;
		}

		// Rate-limit writes to keep the extra transaction per Cycle rare; the status
		// block refreshes every ~2 s, so the read-back flips a few Cycles after an
		// accepted write
		final var now = Instant.now();
		if (Duration.between(this.lastPhaseSwitchWrite, now).compareTo(WRITE_INTERVAL) < 0) {
			return;
		}
		this.lastPhaseSwitchWrite = now;
		this.phaseSwitchWritePending = true;
		try {
			this.log(WRITES, "Phase switch: set forced single-phase = " + targetSinglePhase);
			this.setForcedSinglePhase(targetSinglePhase);
		} catch (OpenemsNamedException e) {
			this.logWarn(this.log, "Phase-switch write failed: " + e.getMessage());
		}
	}

	private void log(LogVerbosity logVerbosity, String text) {
		if (this.config.logVerbosity() == logVerbosity) {
			this.logInfo(this.log, text);
		}
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public boolean isReadOnly() {
		return this.config.readOnly();
	}

	@Override
	public String debugLog() {
		var b = new StringBuilder() //
				.append("L:").append(this.getActivePower().asString());
		if (!this.isReadOnly()) {
			b //
					.append("|SetCurrent:") //
					.append(this.channel(EvseChargePointVoltie.ChannelId.DEBUG_SET_CURRENT_LIMIT).value().asString()) //
					.append("|SetEnable:") //
					.append(this.channel(EvseChargePointVoltie.ChannelId.DEBUG_SET_CHARGING_ENABLED).value()
							.asString());
		}
		return b.toString();
	}
}
