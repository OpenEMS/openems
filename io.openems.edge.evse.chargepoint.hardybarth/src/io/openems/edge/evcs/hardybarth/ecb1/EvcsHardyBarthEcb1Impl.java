package io.openems.edge.evcs.hardybarth.ecb1;

import static io.openems.edge.evcs.api.ChargingType.AC;
import static io.openems.edge.evcs.api.Phases.THREE_PHASE;
import static java.lang.Math.round;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.AbstractManagedEvcsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.HardyBarth.cPH1", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class EvcsHardyBarthEcb1Impl extends AbstractManagedEvcsComponent
		implements EvcsHardyBarthEcb1, Ecb1Parent, OpenemsComponent, EventHandler, ManagedEvcs, Evcs,
		ElectricityMeter {

	@Reference
	private BridgeHttpFactory httpBridgeFactory;

	@Reference
	private HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition;

	@Reference
	private EvcsPower evcsPower;

	private Config config;
	private Ecb1Handler handler;

	public EvcsHardyBarthEcb1Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				EvcsHardyBarthEcb1.ChannelId.values() //
		);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());

		this._setChargingType(AC);
		this._setFixedMinimumHardwarePower(round(config.minHwCurrent() / 1000f) * DEFAULT_VOLTAGE * THREE_PHASE.getValue());
		this._setFixedMaximumHardwarePower(round(config.maxHwCurrent() / 1000f) * DEFAULT_VOLTAGE * THREE_PHASE.getValue());
		this._setPowerPrecision(230);
		this._setPhases(THREE_PHASE);

		this.handler = new Ecb1Handler(this, config.ip(), config.chargeControlId(), config.meterId(),
				this.httpBridgeFactory, this.httpBridgeCycleServiceDefinition);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		if (this.handler != null) {
			this.handler.deactivate();
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		super.handleEvent(event);
	}

	@Override
	public void onChargeControlStatus(String state, Integer stateId, Boolean connected) {
		this._setStatus(toStatus(state, stateId));
	}

	@Override
	public void onCommunicationFailed(boolean failed) {
		this._setChargingstationCommunicationFailed(failed);
	}

	private static Status toStatus(String state, Integer stateId) {
		if (state == null || state.isEmpty()) {
			return Status.UNDEFINED;
		}
		return switch (state.charAt(0)) {
		case 'A' -> Status.NOT_READY_FOR_CHARGING;
		case 'B' -> stateId != null && stateId == 17 ? Status.CHARGING_REJECTED : Status.READY_FOR_CHARGING;
		case 'C', 'D' -> Status.CHARGING;
		case 'E', 'F' -> Status.ERROR;
		default -> Status.UNDEFINED;
		};
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws OpenemsException {
		var phases = this.getPhasesAsInt();
		var currentA = (int) round(power / (float) phases / 230.0f);
		var minA = this.config.minHwCurrent() / 1000;
		var maxA = this.config.maxHwCurrent() / 1000;
		currentA = Math.max(minA, Math.min(currentA, maxA));
		return this.handler.setTarget(currentA);
	}

	@Override
	public boolean pauseChargeProcess() throws OpenemsException {
		return this.handler.setTarget(0);
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
	public int getConfiguredMinimumHardwarePower() {
		return round(this.config.minHwCurrent() / 1000f) * DEFAULT_VOLTAGE * THREE_PHASE.getValue();
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return round(this.config.maxHwCurrent() / 1000f) * DEFAULT_VOLTAGE * THREE_PHASE.getValue();
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return false;
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.MANAGED_CONSUMPTION_METERED;
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return PhaseRotation.L1_L2_L3;
	}
}
