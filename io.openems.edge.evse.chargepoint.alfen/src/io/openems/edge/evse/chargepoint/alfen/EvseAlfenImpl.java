package io.openems.edge.evse.chargepoint.alfen;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.chain;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.evcs.api.Evcs.evaluatePhaseCountFromCurrent;
import static io.openems.edge.evse.api.common.ApplySetPoint.MIN_CURRENT;
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
import io.openems.common.types.Tuple2;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
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
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
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

	public static final int MAX_CURRENT = 32_000;

	/**
	 * Chargers with a Reallin power meter (produced after 2021) report NaN for
	 * unavailable registers (see Alfen "Modbus for ACE" Appendix E). Without this
	 * converter the NaN would silently end up as '0' in the Channel.
	 */
	private static final ElementToChannelConverter IGNORE_NAN = new ElementToChannelConverter(//
			value -> switch (value) {
			case Float f -> f.isNaN() ? null : f;
			case Double d -> d.isNaN() ? null : d;
			case null, default -> value;
			}, //
			value -> value);

	/**
	 * Ignores NaN like {@link #IGNORE_NAN}, then converts [A] to [mA], [V] to [mV]
	 * or [Hz] to [mHz].
	 */
	private static final ElementToChannelConverter IGNORE_NAN_AND_SCALE_FACTOR_3 = chain(IGNORE_NAN, SCALE_FACTOR_3);

	/** Parses the raw String of register 1201 to a {@link Mode3State}. */
	private static final ElementToChannelConverter TO_MODE_3_STATE = new ElementToChannelConverter(
			value -> Mode3State.fromString(value instanceof String s ? s : null).getValue());

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

	public EvseAlfenImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				EvseAlfen.ChannelId.values() //
		);
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
		super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		final var pr = this.getPhaseRotation();
		var modbusProtocol = new ModbusProtocol(this, //
				// Complete socket measurement registers (PDF section 3.4)
				new FC3ReadRegistersTask(300, Priority.HIGH, //
						m(EvseAlfen.ChannelId.METER_STATE, new UnsignedWordElement(300)), //
						m(EvseAlfen.ChannelId.METER_LAST_VALUE_TIMESTAMP, new UnsignedQuadruplewordElement(301)), //
						m(EvseAlfen.ChannelId.METER_TYPE, new UnsignedWordElement(305)), //
						m(pr.channelVoltageL1(), new FloatDoublewordElement(306), IGNORE_NAN_AND_SCALE_FACTOR_3), //
						m(pr.channelVoltageL2(), new FloatDoublewordElement(308), IGNORE_NAN_AND_SCALE_FACTOR_3), //
						m(pr.channelVoltageL3(), new FloatDoublewordElement(310), IGNORE_NAN_AND_SCALE_FACTOR_3), //
						m(EvseAlfen.ChannelId.VOLTAGE_L1_L2, new FloatDoublewordElement(312), IGNORE_NAN), //
						m(EvseAlfen.ChannelId.VOLTAGE_L2_L3, new FloatDoublewordElement(314), IGNORE_NAN), //
						m(EvseAlfen.ChannelId.VOLTAGE_L3_L1, new FloatDoublewordElement(316), IGNORE_NAN), //
						m(EvseAlfen.ChannelId.CURRENT_N, new FloatDoublewordElement(318), IGNORE_NAN), //
						m(pr.channelCurrentL1(), new FloatDoublewordElement(320), IGNORE_NAN_AND_SCALE_FACTOR_3), //
						m(pr.channelCurrentL2(), new FloatDoublewordElement(322), IGNORE_NAN_AND_SCALE_FACTOR_3), //
						m(pr.channelCurrentL3(), new FloatDoublewordElement(324), IGNORE_NAN_AND_SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.CURRENT, new FloatDoublewordElement(326),
								IGNORE_NAN_AND_SCALE_FACTOR_3), //
						m(EvseAlfen.ChannelId.POWER_FACTOR_L1, new FloatDoublewordElement(328), IGNORE_NAN), //
						m(EvseAlfen.ChannelId.POWER_FACTOR_L2, new FloatDoublewordElement(330), IGNORE_NAN), //
						m(EvseAlfen.ChannelId.POWER_FACTOR_L3, new FloatDoublewordElement(332), IGNORE_NAN), //
						m(EvseAlfen.ChannelId.POWER_FACTOR_SUM, new FloatDoublewordElement(334), IGNORE_NAN), //
						m(ElectricityMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(336),
								IGNORE_NAN_AND_SCALE_FACTOR_3), //
						m(pr.channelActivePowerL1(), new FloatDoublewordElement(338), IGNORE_NAN), //
						m(pr.channelActivePowerL2(), new FloatDoublewordElement(340), IGNORE_NAN), //
						m(pr.channelActivePowerL3(), new FloatDoublewordElement(342), IGNORE_NAN), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(344), IGNORE_NAN), //
						m(EvseAlfen.ChannelId.APPARENT_POWER_L1, new FloatDoublewordElement(346), IGNORE_NAN), //
						m(EvseAlfen.ChannelId.APPARENT_POWER_L2, new FloatDoublewordElement(348), IGNORE_NAN), //
						m(EvseAlfen.ChannelId.APPARENT_POWER_L3, new FloatDoublewordElement(350), IGNORE_NAN), //
						m(EvseAlfen.ChannelId.APPARENT_POWER_SUM, new FloatDoublewordElement(352), IGNORE_NAN), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(354), IGNORE_NAN), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(356), IGNORE_NAN), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(358), IGNORE_NAN), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(360), IGNORE_NAN)), //

				new FC3ReadRegistersTask(362, Priority.LOW, //
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1,
								new FloatQuadruplewordElement(362), IGNORE_NAN), //
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2,
								new FloatQuadruplewordElement(366), IGNORE_NAN), //
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3,
								new FloatQuadruplewordElement(370), IGNORE_NAN), //
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
								new FloatQuadruplewordElement(374), IGNORE_NAN), //
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1,
								new FloatQuadruplewordElement(378), IGNORE_NAN), //
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2,
								new FloatQuadruplewordElement(382), IGNORE_NAN), //
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3,
								new FloatQuadruplewordElement(386), IGNORE_NAN), //
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, //
								new FloatQuadruplewordElement(390), IGNORE_NAN), //
						m(EvseAlfen.ChannelId.APPARENT_ENERGY_L1, new FloatQuadruplewordElement(394), IGNORE_NAN), //
						m(EvseAlfen.ChannelId.APPARENT_ENERGY_L2, new FloatQuadruplewordElement(398), IGNORE_NAN), //
						m(EvseAlfen.ChannelId.APPARENT_ENERGY_L3, new FloatQuadruplewordElement(402), IGNORE_NAN), //
						m(EvseAlfen.ChannelId.APPARENT_ENERGY_SUM, new FloatQuadruplewordElement(406), IGNORE_NAN), //
						m(EvseAlfen.ChannelId.REACTIVE_ENERGY_L1, new FloatQuadruplewordElement(410), IGNORE_NAN), //
						m(EvseAlfen.ChannelId.REACTIVE_ENERGY_L2, new FloatQuadruplewordElement(414), IGNORE_NAN), //
						m(EvseAlfen.ChannelId.REACTIVE_ENERGY_L3, new FloatQuadruplewordElement(418), IGNORE_NAN), //
						m(EvseAlfen.ChannelId.REACTIVE_ENERGY_SUM, new FloatQuadruplewordElement(422), IGNORE_NAN)), //

				new FC3ReadRegistersTask(1200, Priority.HIGH, //
						m(EvseAlfen.ChannelId.AVAILABILITY, new UnsignedWordElement(1200)), //
						m(EvseAlfen.ChannelId.MODE_3_STATE, new StringWordElement(1201, 5), TO_MODE_3_STATE), //
						m(EvseAlfen.ChannelId.ACTUAL_APPLIED_MAX_CURRENT, new FloatDoublewordElement(1206),
								IGNORE_NAN), //
						m(EvseAlfen.ChannelId.MODBUS_SLAVE_MAX_CURRENT_VALID_TIME, new UnsignedDoublewordElement(1208)), //
						m(EvseAlfen.ChannelId.SET_CURRENT, new FloatDoublewordElement(1210), IGNORE_NAN), //
						m(EvseAlfen.ChannelId.ACTIVE_LOAD_BALANCING_SAFE_CURRENT, new FloatDoublewordElement(1212),
								IGNORE_NAN), //
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
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			final Mode3State mode3State = this.getMode3StateChannel().getNextValue().asEnum();
			setValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, mode3State.isReadyForCharging);
		}
		}
	}

	private SingleOrThreePhase getPhases() {
		// A single-phase wired charge point can never charge three-phased
		if (this.config.wiring() == SINGLE_PHASE) {
			return SINGLE_PHASE;
		}
		/*
		 * Prefer the configured phase mode from register 1215 ("Charge using 1 or 3
		 * phases"). While charging is paused no current flows, so the activity
		 * heuristic below would wrongly report THREE_PHASE right after a switch to
		 * single-phase. Only 1 and 3 are valid; unavailable registers are filled with
		 * 0xFFFF (see Alfen "Modbus Slave TCP/IP" 1.2).
		 */
		final var configuredPhases = switch (this.getSetPhases().orElse(0)) {
		case 1 -> SINGLE_PHASE;
		case 3 -> THREE_PHASE;
		default -> null; // register not available
		};
		if (configuredPhases != null) {
			return configuredPhases;
		}
		// Fall back to the phases that actually carry current
		final var phaseCount = evaluatePhaseCountFromCurrent(//
				this.getCurrentL1().get(), //
				this.getCurrentL2().get(), //
				this.getCurrentL3().get());
		return phaseCount == null || phaseCount == 3 //
				? THREE_PHASE // no current at all: assume the default wiring
				: SINGLE_PHASE;
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		if (this.config.readOnly()) {
			return ChargePointAbilities.create().build();
		}

		final var phases = this.getPhases();

		return ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(phases, MIN_CURRENT, MAX_CURRENT)) //
				.setPhaseSwitchManual(this.getPhaseSwitchAbility(phases)) //
				.setIsEvConnected(this.getMode3State().isEvConnected) //
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
		if (this.config.wiring() == SINGLE_PHASE) {
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
