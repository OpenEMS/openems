package io.openems.edge.evse.chargepoint.hardybarth.cph2;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.evcs.api.Evcs.evaluatePhaseCountFromCurrent;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.Instant;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.Phase;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.chargepoint.hardybarth.common.DeviceRole;
import io.openems.edge.evse.chargepoint.hardybarth.common.HardyBarth;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.HardyBarth", //
		immediate = true, //
		configurationPolicy = REQUIRE)
@EventTopics({ //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
})
public class EvseChargePointHardyBarthImpl extends AbstractOpenemsComponent implements EvseChargePointHardyBarth,
		HardyBarth, OpenemsComponent, EvseChargePoint, ElectricityMeter, TimedataProvider, EventHandler {

	private static final int MIN_PHASE_SWITCH_DELAY = 60;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	@Reference
	private HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition;

	@Reference
	private OpenemsEdgeOem oem;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	private Config config = null;
	private EvseHandler handler;
	private Instant lastPhaseSwitchCommand = null;

	public EvseChargePointHardyBarthImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EvseChargePointHardyBarth.ChannelId.values(), //
				HardyBarth.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values());

		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		if (!this.isEnabled()) {
			return;
		}

		this.handler = new EvseHandler(this, config.ip(), this.oem.getHardyBarthApiToken(), config.phaseRotation(),
				config.logVerbosity(), this::logInfo, this.httpBridgeFactory, this.httpBridgeCycleServiceDefinition, //
				communicationFailed -> doNothing());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.handler.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		this.handler.handleAfterProcessImageEvent(event);
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		if (this.isReadOnly()) {
			return ChargePointAbilities.create()//
					.build();
		}

		final var isEvConnected = this.isEvConnected();

		var phase = this.getActivePhase();
		if (phase == null) {
			phase = Phase.SingleOrThreePhase.THREE_PHASE;
		}
		final var applySetPoint = this.getApplySetPoint(phase);

		return ChargePointAbilities.create() //
				.setApplySetPoint(applySetPoint) //
				.setIsEvConnected(isEvConnected) //
				.setIsReadyForCharging(this.getIsReadyForCharging()) //
				.setPhaseSwitch(this.getPhaseSwitchAbility()) //
				.build();
	}

	@Override
	public void apply(ChargePointActions actions) {
		if (this.config.readOnly()) {
			return;
		}

		final var phaseSwitch = actions.phaseSwitch();
		this.applyPhaseSwitch(phaseSwitch);
		if (phaseSwitch != null) {
			return;
		}
		this.applySetPoint(actions);
	}

	private void applySetPoint(ChargePointActions actions) {
		if (this.isCurrentlySwitching()) {
			return;
		}
		var current = actions.getApplySetPointInAmpere().value();
		this.handler.setTarget(current);
	}

	private void applyPhaseSwitch(ApplyPhaseSwitch phaseSwitch) {
		if (phaseSwitch == null) {
			return;
		}
		if (!this.canExecutePhaseSwitch(phaseSwitch.direction())) {
			return;
		}
		this.lastPhaseSwitchCommand = Instant.now();
		this.handler.triggerPhaseSwitch(phaseSwitch.direction());
	}

	private boolean canExecutePhaseSwitch(ApplyPhaseSwitch.PhaseSwitchDirection direction) {
		if (direction == null || !this.isChargingForPhaseSwitch() || this.isCurrentlySwitching()) {
			return false;
		}
		final var ability = this.getPhaseSwitchAbility();
		return ability != null && ability.direction() == direction;
	}

	private ApplySetPoint.Ability.Ampere getApplySetPoint(Phase.SingleOrThreePhase phase) {
		return new ApplySetPoint.Ability.Ampere(phase, 6, 16);
	}

	/**
	 * Checks if the CP is currently switching.
	 * 
	 * @return is currently switching
	 */
	public boolean isCurrentlySwitching() {
		Value<String> status = this.getSaliaPhaseSwitchingStatus();

		final var recentlySwitched = this.lastPhaseSwitchCommand != null //
				&& this.lastPhaseSwitchCommand.isAfter(Instant.now().minusSeconds(MIN_PHASE_SWITCH_DELAY)); //
		final var statusSwitching = status != null //
				&& status.isDefined() //
				&& status.get() != null //
				&& !status.get().trim().isEmpty() //
				&& !"idle".equals(status.get()); //

		return recentlySwitched || statusSwitching;
	}

	private ApplyPhaseSwitch getPhaseSwitchAbility() {
		if (!this.hasPhaseSwitchingApi() || this.getDeviceRoleChannel().value().asEnum() != DeviceRole.SLAVE
				|| !this.isEvConnected()) {
			return null;
		}

		final var activePhase = this.getActivePhase();
		if (activePhase == null) {
			return null;
		}

		final var applySetPoint = this.getApplySetPoint(this.getActivePhase());

		final var oppositePhase = activePhase == Phase.SingleOrThreePhase.SINGLE_PHASE
				? Phase.SingleOrThreePhase.THREE_PHASE
				: Phase.SingleOrThreePhase.SINGLE_PHASE;
		final var direction = activePhase == Phase.SingleOrThreePhase.SINGLE_PHASE ? PhaseSwitchDirection.TO_THREE_PHASE
				: PhaseSwitchDirection.TO_SINGLE_PHASE;
		final var oppositePhaseApplySetPoint = new ApplySetPoint.Ability.Watt(oppositePhase,
				ApplySetPoint.convertAmpereToWatt(oppositePhase, applySetPoint.min()),
				ApplySetPoint.convertAmpereToWatt(oppositePhase, applySetPoint.max()));
		return new ApplyPhaseSwitch(direction, new ApplyPhaseSwitch.PhaseSwitchAbility.ManualWithoutZeroSetPoint(),
				oppositePhaseApplySetPoint);
	}

	private boolean isEvConnected() {
		return switch (this.getChargePointStatus()) {
		case B, C, D -> true;
		case A, E, F, UNDEFINED -> false;
		};
	}

	private boolean isChargingForPhaseSwitch() {
		return switch (this.getChargePointStatus()) {
		case C, D -> true;
		case A, B, E, F, UNDEFINED -> false;
		};
	}

	private Phase.SingleOrThreePhase getActivePhase() {
		final var phaseByActualPhase = this.getActivePhaseByActualPhase();
		if (phaseByActualPhase != null) {
			return phaseByActualPhase;
		}

		final var phaseByPhaseSwitching = this.getActivePhaseByPhaseSwitching();
		if (phaseByPhaseSwitching != null) {
			return phaseByPhaseSwitching;
		}

		return this.getActivePhaseByCurrent();
	}

	private Phase.SingleOrThreePhase getActivePhaseByPhaseSwitching() {
		final var phaseSwitchingActual = this.getSaliaPhaseSwitchingActual();
		if (phaseSwitchingActual != null) {
			switch (phaseSwitchingActual.trim()) {
			case "1":
				return Phase.SingleOrThreePhase.SINGLE_PHASE;
			case "3":
				return Phase.SingleOrThreePhase.THREE_PHASE;
			default:
				break;
			}
		}
		return null;
	}

	private Phase.SingleOrThreePhase getActivePhaseByActualPhase() {
		final var phaseActual = this.getRawPhaseActual().get();
		if (phaseActual != null) {
			switch (phaseActual) {
			case 1:
				return Phase.SingleOrThreePhase.SINGLE_PHASE;
			case 3:
				return Phase.SingleOrThreePhase.THREE_PHASE;
			default:
				break;
			}
		}
		return null;
	}

	private Phase.SingleOrThreePhase getActivePhaseByCurrent() {
		final var phaseCount = evaluatePhaseCountFromCurrent(//
				this.getCurrentL1().orElse(0), //
				this.getCurrentL2().orElse(0), //
				this.getCurrentL3().orElse(0));
		if (phaseCount != null && phaseCount == 1) {
			return Phase.SingleOrThreePhase.SINGLE_PHASE;
		}
		if (phaseCount != null) {
			return Phase.SingleOrThreePhase.THREE_PHASE;
		}
		return null;
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
	public String debugLog() {
		return this.handler.debugLog();
	}
}
