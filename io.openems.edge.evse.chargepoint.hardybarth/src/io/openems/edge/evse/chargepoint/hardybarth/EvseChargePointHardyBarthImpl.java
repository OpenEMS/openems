package io.openems.edge.evse.chargepoint.hardybarth;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.evcs.api.Evcs.evaluatePhaseCountFromCurrent;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

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
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.Phase;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplySetPoint;
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

		final var isEvConnected = switch (this.getChargePointStatus()) {
		case UNDEFINED, A, E, F -> false;
		case B, C, D -> true;
		};
		int calculatedPhaseCount = this.getPhaseCount();
		evaluatePhaseCountFromCurrent(//
				this.getCurrentL1().orElse(0), //
				this.getCurrentL2().orElse(0), //
				this.getCurrentL3().orElse(0));
		Phase.SingleOrThreePhase phaseCount;
		if (calculatedPhaseCount == 1) {
			phaseCount = Phase.SingleOrThreePhase.SINGLE_PHASE;
		} else {
			phaseCount = Phase.SingleOrThreePhase.THREE_PHASE;
		}
		return ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Ampere(phaseCount, 6, 16)) //
				.setIsEvConnected(isEvConnected) //
				.setIsReadyForCharging(this.getIsReadyForCharging()) //
				.build();
	}

	@Override
	public void apply(ChargePointActions actions) {
		var current = actions.getApplySetPointInAmpere().value();
		this.handler.setTarget(current);
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
