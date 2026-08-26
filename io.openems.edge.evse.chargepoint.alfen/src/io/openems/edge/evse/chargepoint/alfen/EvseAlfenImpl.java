package io.openems.edge.evse.chargepoint.alfen;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
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
import io.openems.common.types.OpenemsType;
import io.openems.common.types.Tuple2;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.FloatQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.channel.StringReadChannel;
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
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.Alfen", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
@GenerateTargetsFromReferences("Modbus")
public class EvseAlfenImpl extends AbstractOpenemsModbusComponent implements EvseAlfen, EvseChargePoint,
		ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler, ModbusComponent {

	private static final float DETECT_PHASE_ACTIVITY = 400; // mA

	private final Logger log = LoggerFactory.getLogger(EvseAlfenImpl.class);

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))")
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;
	private Tuple2<Instant, Integer> previousCurrent = null;
	private int phasePattern = 0;

	public EvseAlfenImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				EvseAlfen.ChannelId.values() //
		);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
		this.installListeners();
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		this.config = config;
		super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Installs listeners to set phases and map raw current values to rotated
	 * phases.
	 */
	private void installListeners() {
		final var phaseRotation = this.getPhaseRotation();
		final Channel<Integer> curL1 = this.channel(phaseRotation.channelCurrentL1());
		final Channel<Integer> curL2 = this.channel(phaseRotation.channelCurrentL2());
		final Channel<Integer> curL3 = this.channel(phaseRotation.channelCurrentL3());
		final Channel<Integer> volL1 = this.channel(phaseRotation.channelVoltageL1());
		final Channel<Integer> volL2 = this.channel(phaseRotation.channelVoltageL2());
		final Channel<Integer> volL3 = this.channel(phaseRotation.channelVoltageL3());

		this.channel(EvseAlfen.ChannelId.CURRENT_L1_RAW).onUpdate(newValue -> {
			int current = 0;
			var rawCurrent = asFloatOrNull(newValue);
			if (rawCurrent != null) {
				current = TypeUtils.getAsType(OpenemsType.INTEGER, rawCurrent);
				if (current > DETECT_PHASE_ACTIVITY) {
					this.phasePattern |= 0x01;
				} else {
					this.phasePattern &= ~0x01;
				}
			} else {
				this.phasePattern &= ~0x01;
			}
			curL1.setNextValue(current);
		});
		this.channel(EvseAlfen.ChannelId.CURRENT_L2_RAW).onUpdate(newValue -> {
			int current = 0;
			var rawCurrent = asFloatOrNull(newValue);
			if (rawCurrent != null) {
				current = TypeUtils.getAsType(OpenemsType.INTEGER, rawCurrent);
				if (current > DETECT_PHASE_ACTIVITY) {
					this.phasePattern |= 0x02;
				} else {
					this.phasePattern &= ~0x02;
				}
			} else {
				this.phasePattern &= ~0x02;
			}
			curL2.setNextValue(current);
		});
		this.channel(EvseAlfen.ChannelId.CURRENT_L3_RAW).onUpdate(newValue -> {
			int current = 0;
			var rawCurrent = asFloatOrNull(newValue);
			if (rawCurrent != null) {
				current = TypeUtils.getAsType(OpenemsType.INTEGER, rawCurrent);
				if (current > DETECT_PHASE_ACTIVITY) {
					this.phasePattern |= 0x04;
				} else {
					this.phasePattern &= ~0x04;
				}
			} else {
				this.phasePattern &= ~0x04;
			}
			curL3.setNextValue(current);
		});

		this.channel(EvseAlfen.ChannelId.VOLTAGE_L1_RAW).onUpdate(newValue -> {
			var voltage = asFloatOrNull(newValue);
			volL1.setNextValue(voltage != null //
					? Math.round(voltage * 1000) // Convert V to mV
					: null);
		});
		this.channel(EvseAlfen.ChannelId.VOLTAGE_L2_RAW).onUpdate(newValue -> {
			var voltage = asFloatOrNull(newValue);
			volL2.setNextValue(voltage != null //
					? Math.round(voltage * 1000) // Convert V to mV
					: null);
		});
		this.channel(EvseAlfen.ChannelId.VOLTAGE_L3_RAW).onUpdate(newValue -> {
			var voltage = asFloatOrNull(newValue);
			volL3.setNextValue(voltage != null //
					? Math.round(voltage * 1000) // Convert V to mV
					: null);
		});

		// Map charge power to active power
		this.channel(EvseAlfen.ChannelId.CHARGE_POWER).onUpdate(newValue -> {
			var power = asFloatOrNull(newValue);
			this._setActivePower(power != null ? Math.round(power) : null);
		});

		// Map energy delivered to production + consumption energy
		// "Energy Delivered" in Alfen context = energy delivered to EV = consumption
		// from grid perspective
		this.channel(EvseAlfen.ChannelId.ENERGY_DELIVERED_SUM).onUpdate(newValue -> {
			var rawEnergy = asFloatOrNull(newValue);
			var energy = rawEnergy != null ? (Long) (long) Math.round(rawEnergy) : null;
			this._setActiveProductionEnergy(energy);
			this._setActiveConsumptionEnergy(energy);
		});

		// Map per-phase charge power to ElectricityMeter active power channels
		this.channel(EvseAlfen.ChannelId.CHARGE_POWER_L1).onUpdate(newValue -> {
			var power = asFloatOrNull(newValue);
			this._setActivePowerL1(power != null ? Math.round(power) : null);
		});
		this.channel(EvseAlfen.ChannelId.CHARGE_POWER_L2).onUpdate(newValue -> {
			var power = asFloatOrNull(newValue);
			this._setActivePowerL2(power != null ? Math.round(power) : null);
		});
		this.channel(EvseAlfen.ChannelId.CHARGE_POWER_L3).onUpdate(newValue -> {
			var power = asFloatOrNull(newValue);
			this._setActivePowerL3(power != null ? Math.round(power) : null);
		});
	}

	/**
	 * Chargers with a Reallin power meter (produced after 2021) report NaN for
	 * unavailable registers (see Alfen "Modbus for ACE" Appendix E).
	 * Math.round(Float.NaN) would silently yield 0 instead of null.
	 *
	 * @param value the raw channel {@link Value}
	 * @return the float value, or null if undefined or NaN
	 */
	private static Float asFloatOrNull(Value<?> value) {
		if (!value.isDefined()) {
			return null;
		}
		var f = (Float) value.get();
		return Float.isNaN(f) ? null : f;
	}

	private SingleOrThreePhase getPhases() {
		// A single-phase wired charge point can never charge three-phased
		if (this.config.wiring() == SingleOrThreePhase.SINGLE_PHASE) {
			return SingleOrThreePhase.SINGLE_PHASE;
		}
		// Prefer the configured phase mode from register 1215 ("Charge using 1 or 3
		// phases"). While charging is paused no current flows, so the activity
		// heuristic below would wrongly report THREE_PHASE right after a switch to
		// single-phase.
		Channel<Integer> setPhases = this.channel(EvseAlfen.ChannelId.SET_PHASES);
		var configuredPhases = setPhases.value().get();
		if (configuredPhases != null) {
			// Only 1 and 3 are valid. Unavailable registers are filled with
			// 0xFFFF (see Alfen "Modbus Slave TCP/IP" 1.2), so anything else
			// falls back to the current activity heuristic below.
			if (configuredPhases == 1) {
				return SingleOrThreePhase.SINGLE_PHASE;
			}
			if (configuredPhases == 3) {
				return THREE_PHASE;
			}
		}
		var bitCount = Integer.bitCount(this.phasePattern);
		if (bitCount == 0 || bitCount == 3) {
			return THREE_PHASE;
		}
		return SingleOrThreePhase.SINGLE_PHASE;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			this.updateIsReadyForCharging();
		}
		}
	}

	private void updateIsReadyForCharging() {
		Value<?> mode3StateVal = this.channel(EvseAlfen.ChannelId.MODE_3_STATE).getNextValue();
		if (!mode3StateVal.isDefined()) {
			setValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, false);
			return;
		}

		var mode3State = (String) mode3StateVal.get();

		// Mode 3 states:
		// A = Not connected
		// B = Connected, not ready
		// C1 = Connected, ready, not charging
		// C2 = Charging
		// D1, D2 = With ventilation
		// E = Error (short circuit)
		// F = Error

		boolean isReady = mode3State.startsWith("B") || mode3State.startsWith("C") || mode3State.startsWith("D");
		setValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, isReady);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		var modbusProtocol = new ModbusProtocol(this, //
				// Complete socket measurement registers (PDF section 3.4)
				new FC3ReadRegistersTask(300, Priority.HIGH, //
						m(EvseAlfen.ChannelId.METER_STATE, new UnsignedWordElement(300)), //
						m(EvseAlfen.ChannelId.METER_LAST_VALUE_TIMESTAMP, new UnsignedQuadruplewordElement(301)), //
						m(EvseAlfen.ChannelId.METER_TYPE, new UnsignedWordElement(305)), //
						m(EvseAlfen.ChannelId.VOLTAGE_L1_RAW, new FloatDoublewordElement(306)), //
						m(EvseAlfen.ChannelId.VOLTAGE_L2_RAW, new FloatDoublewordElement(308)), //
						m(EvseAlfen.ChannelId.VOLTAGE_L3_RAW, new FloatDoublewordElement(310)), //
						m(EvseAlfen.ChannelId.VOLTAGE_L1_L2, new FloatDoublewordElement(312)), //
						m(EvseAlfen.ChannelId.VOLTAGE_L2_L3, new FloatDoublewordElement(314)), //
						m(EvseAlfen.ChannelId.VOLTAGE_L3_L1, new FloatDoublewordElement(316)), //
						m(EvseAlfen.ChannelId.CURRENT_N, new FloatDoublewordElement(318)), //
						m(EvseAlfen.ChannelId.CURRENT_L1_RAW, new FloatDoublewordElement(320), SCALE_FACTOR_3), //
						m(EvseAlfen.ChannelId.CURRENT_L2_RAW, new FloatDoublewordElement(322), SCALE_FACTOR_3), //
						m(EvseAlfen.ChannelId.CURRENT_L3_RAW, new FloatDoublewordElement(324), SCALE_FACTOR_3), //
						m(EvseAlfen.ChannelId.CURRENT_SUM, new FloatDoublewordElement(326)), //
						m(EvseAlfen.ChannelId.POWER_FACTOR_L1, new FloatDoublewordElement(328)), //
						m(EvseAlfen.ChannelId.POWER_FACTOR_L2, new FloatDoublewordElement(330)), //
						m(EvseAlfen.ChannelId.POWER_FACTOR_L3, new FloatDoublewordElement(332)), //
						m(EvseAlfen.ChannelId.POWER_FACTOR_SUM, new FloatDoublewordElement(334)), //
						m(new FloatDoublewordElement(336)) //
								.build().onUpdateCallback(value -> {
									// Convert Hz (float) to mHz (integer)
									this._setFrequency(value != null ? Math.round(value * 1000) : null);
								}), //
						m(EvseAlfen.ChannelId.CHARGE_POWER_L1, new FloatDoublewordElement(338)), //
						m(EvseAlfen.ChannelId.CHARGE_POWER_L2, new FloatDoublewordElement(340)), //
						m(EvseAlfen.ChannelId.CHARGE_POWER_L3, new FloatDoublewordElement(342)), //
						m(EvseAlfen.ChannelId.CHARGE_POWER, new FloatDoublewordElement(344)), //
						m(EvseAlfen.ChannelId.APPARENT_POWER_L1, new FloatDoublewordElement(346)), //
						m(EvseAlfen.ChannelId.APPARENT_POWER_L2, new FloatDoublewordElement(348)), //
						m(EvseAlfen.ChannelId.APPARENT_POWER_L3, new FloatDoublewordElement(350)), //
						m(EvseAlfen.ChannelId.APPARENT_POWER_SUM, new FloatDoublewordElement(352)), //
						// Map reactive power per phase to ElectricityMeter channels
						m(new FloatDoublewordElement(354)) //
								.build().onUpdateCallback(value -> {
									this._setReactivePowerL1(value != null ? Math.round(value) : null);
								}), //
						m(new FloatDoublewordElement(356)) //
								.build().onUpdateCallback(value -> {
									this._setReactivePowerL2(value != null ? Math.round(value) : null);
								}), //
						m(new FloatDoublewordElement(358)) //
								.build().onUpdateCallback(value -> {
									this._setReactivePowerL3(value != null ? Math.round(value) : null);
								}), //
						m(EvseAlfen.ChannelId.REACTIVE_POWER_SUM, new FloatDoublewordElement(360))), //

				// Complete energy registers (PDF section 3.4, register 362-425)
				new FC3ReadRegistersTask(362, Priority.LOW, //
						m(EvseAlfen.ChannelId.ENERGY_DELIVERED_L1, new FloatQuadruplewordElement(362)), //
						m(EvseAlfen.ChannelId.ENERGY_DELIVERED_L2, new FloatQuadruplewordElement(366)), //
						m(EvseAlfen.ChannelId.ENERGY_DELIVERED_L3, new FloatQuadruplewordElement(370)), //
						m(EvseAlfen.ChannelId.ENERGY_DELIVERED_SUM, new FloatQuadruplewordElement(374)), //
						m(EvseAlfen.ChannelId.ENERGY_CONSUMED_L1, new FloatQuadruplewordElement(378)), //
						m(EvseAlfen.ChannelId.ENERGY_CONSUMED_L2, new FloatQuadruplewordElement(382)), //
						m(EvseAlfen.ChannelId.ENERGY_CONSUMED_L3, new FloatQuadruplewordElement(386)), //
						m(EvseAlfen.ChannelId.ENERGY_CONSUMED_SUM, new FloatQuadruplewordElement(390)), //
						m(EvseAlfen.ChannelId.APPARENT_ENERGY_L1, new FloatQuadruplewordElement(394)), //
						m(EvseAlfen.ChannelId.APPARENT_ENERGY_L2, new FloatQuadruplewordElement(398)), //
						m(EvseAlfen.ChannelId.APPARENT_ENERGY_L3, new FloatQuadruplewordElement(402)), //
						m(EvseAlfen.ChannelId.APPARENT_ENERGY_SUM, new FloatQuadruplewordElement(406)), //
						m(EvseAlfen.ChannelId.REACTIVE_ENERGY_L1, new FloatQuadruplewordElement(410)), //
						m(EvseAlfen.ChannelId.REACTIVE_ENERGY_L2, new FloatQuadruplewordElement(414)), //
						m(EvseAlfen.ChannelId.REACTIVE_ENERGY_L3, new FloatQuadruplewordElement(418)), //
						m(EvseAlfen.ChannelId.REACTIVE_ENERGY_SUM, new FloatQuadruplewordElement(422))), //

				new FC3ReadRegistersTask(1200, Priority.HIGH, //
						m(EvseAlfen.ChannelId.AVAILABILITY, new UnsignedWordElement(1200)), //
						m(EvseAlfen.ChannelId.MODE_3_STATE, new StringWordElement(1201, 5)), //
						m(EvseAlfen.ChannelId.ACTUAL_APPLIED_MAX_CURRENT, new FloatDoublewordElement(1206)), //
						m(EvseAlfen.ChannelId.MODBUS_SLAVE_MAX_CURRENT_VALID_TIME, new UnsignedDoublewordElement(1208)), //
						m(EvseAlfen.ChannelId.SET_CURRENT, new FloatDoublewordElement(1210)), //
						m(EvseAlfen.ChannelId.ACTIVE_LOAD_BALANCING_SAFE_CURRENT, new FloatDoublewordElement(1212)), //
						m(EvseAlfen.ChannelId.MODBUS_SLAVE_RECEIVED_SETPOINT_ACCOUNTED_FOR,
								new UnsignedWordElement(1214)), //
						m(EvseAlfen.ChannelId.SET_PHASES, new UnsignedWordElement(1215))));

		if (!this.config.readOnly()) {
			modbusProtocol.addTasks(//
					new FC16WriteRegistersTask(1210, //
							m(EvseAlfen.ChannelId.SET_CURRENT, new FloatDoublewordElement(1210))), //
					new FC6WriteRegisterTask(1215, //
							m(EvseAlfen.ChannelId.SET_PHASES, new UnsignedWordElement(1215))));
		}
		return modbusProtocol;
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		if (this.config.readOnly()) {
			return ChargePointAbilities.create().build();
		}

		final var phases = this.getPhases();
		final int minCurrent = this.config.minCurrent();
		final int maxCurrent = this.config.maxCurrent();

		final StringReadChannel mode3StateChannel = this.channel(EvseAlfen.ChannelId.MODE_3_STATE);
		final var mode3State = mode3StateChannel.value().orElse("A");
		final var isEvConnected = mode3State.length() == 2; // e.g., "B1", "C2"

		return ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(phases, minCurrent, maxCurrent)) //
				.setPhaseSwitchManual(this.getPhaseSwitchAbility(phases)) //
				.setIsEvConnected(isEvConnected) //
				.setIsReadyForCharging(this.getIsReadyForCharging()) //
				.build();
	}

	/**
	 * Advertises the possible phase-switch direction, i.e. the opposite of the
	 * currently active phase mode. Returns null if phase switching is not possible
	 * at all.
	 *
	 * @param phases the currently active {@link SingleOrThreePhase}
	 * @return the {@link PhaseSwitchDirection} or null
	 */
	private PhaseSwitchDirection getPhaseSwitchAbility(SingleOrThreePhase phases) {
		if (this.config.wiring() == SingleOrThreePhase.SINGLE_PHASE) {
			// SINGLE_PHASE wiring can never do phase switching
			return null;
		}
		return phases == THREE_PHASE //
				? PhaseSwitchDirection.TO_SINGLE_PHASE //
				: PhaseSwitchDirection.TO_THREE_PHASE;
	}

	@Override
	public void apply(ChargePointActions actions) {
		if (this.config.readOnly()) {
			return;
		}
		this.applySetPoint(actions.getApplySetPointInMilliAmpere().value());

		// Apply phase switching via Alfen register 1215
		var phaseSwitch = actions.phaseSwitch();
		if (phaseSwitch != null && phaseSwitch.direction() != null) {
			try {
				this.setSetPhases(phaseSwitch.direction() == PhaseSwitchDirection.TO_SINGLE_PHASE ? 1 : 3);
			} catch (OpenemsNamedException e) {
				this.log.error("Failed to apply phase switch: " + e.getMessage());
			}
		}
	}

	private void applySetPoint(int setPointInMilliAmpere) {
		// Throttle writes to prevent overloading the charger
		final var now = Instant.now();

		// Allow immediate disable (0 mA), even if within 5-second throttle period
		final boolean isChangingToZero = setPointInMilliAmpere == 0 //
				&& this.previousCurrent != null //
				&& this.previousCurrent.b() != 0;

		if (!isChangingToZero //
				&& this.previousCurrent != null //
				&& Duration.between(this.previousCurrent.a(), now).getSeconds() < 5) {
			return;
		}

		this.previousCurrent = Tuple2.of(now, setPointInMilliAmpere);

		try {
			// Convert mA to A
			float currentInAmpere = setPointInMilliAmpere / 1000f;
			this.setSetCurrent(currentInAmpere);
		} catch (OpenemsNamedException e) {
			this.log.error("Failed to apply charge current: " + e.getMessage());
		}
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

	@Override
	public boolean isReadOnly() {
		return this.config.readOnly();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public String debugLog() {
		var b = new StringBuilder() //
				.append("L:").append(this.getActivePower().asString());
		if (!this.config.readOnly()) {
			FloatReadChannel debugChannel = this.channel(EvseAlfen.ChannelId.DEBUG_SET_CURRENT);
			b //
					.append("|SetCurrent:") //
					.append(debugChannel.value().asString());
		}
		return b.toString();
	}
}
