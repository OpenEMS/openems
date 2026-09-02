package io.openems.edge.evse.chargepoint.hardybarth.ecb1;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.evcs.api.Evcs.evaluatePhaseCountFromCurrent;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.Phase;
import io.openems.edge.evcs.hardybarth.ecb1.Ecb1Handler;
import io.openems.edge.evcs.hardybarth.ecb1.Ecb1Parent;
import io.openems.edge.evcs.hardybarth.ecb1.EvcsHardyBarthEcb1;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.HardyBarth.cPH1", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
public class EvseChargePointHardyBarthEcb1Impl extends AbstractOpenemsComponent
		implements EvseChargePointHardyBarthEcb1, Ecb1Parent, OpenemsComponent, EvseChargePoint, ElectricityMeter {

	@Reference
	private BridgeHttpFactory httpBridgeFactory;

	@Reference
	private HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition;

	private Config config;
	private Ecb1Handler handler;

	public EvseChargePointHardyBarthEcb1Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				EvcsHardyBarthEcb1.ChannelId.values() //
		);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!this.isEnabled()) {
			return;
		}

		this.handler = new Ecb1Handler(this, config.ip(), config.chargeControlId(), config.meterId(),
				this.httpBridgeFactory, this.httpBridgeCycleServiceDefinition);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		if (this.handler != null) {
			this.handler.deactivate();
		}
	}

	@Override
	public void onChargeControlStatus(String state, Integer stateId, Boolean connected) {
		final var isReady = state != null && !state.isEmpty() //
				&& (state.charAt(0) == 'B' || state.charAt(0) == 'C' || state.charAt(0) == 'D');
		setValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, isReady);
	}

	@Override
	public void onCommunicationFailed(boolean failed) {
		// No dedicated communication-failed channel in EVSE
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		if (this.isReadOnly()) {
			return ChargePointAbilities.create().build();
		}

		final var connected = this.<Boolean>channel(EvcsHardyBarthEcb1.ChannelId.RAW_CONNECTED).value().orElse(false);
		final var phaseCount = evaluatePhaseCountFromCurrent(//
				this.getCurrentL1().orElse(0), //
				this.getCurrentL2().orElse(0), //
				this.getCurrentL3().orElse(0));
		final var phase = phaseCount != null && phaseCount == 1 //
				? Phase.SingleOrThreePhase.SINGLE_PHASE //
				: Phase.SingleOrThreePhase.THREE_PHASE;

		return ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Ampere(phase, //
						this.config.minHwCurrent() / 1000, //
						this.config.maxHwCurrent() / 1000)) //
				.setIsEvConnected(connected) //
				.setIsReadyForCharging(this.getIsReadyForCharging()) //
				.build();
	}

	@Override
	public void apply(ChargePointActions actions) {
		this.handler.setTarget(actions.getApplySetPointInAmpere().value());
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

	@Override
	public boolean isReadOnly() {
		return this.config.readOnly();
	}
}
