package io.openems.edge.evcs.hardybarth;

import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE;
import static io.openems.edge.evcs.api.ChargingType.AC;
import static io.openems.edge.evcs.api.Phases.THREE_PHASE;
import static io.openems.edge.evse.chargepoint.hardybarth.common.AbstractHardyBarthHandler.HEART_BEAT_TIME;
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
import org.slf4j.Logger;

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.AbstractManagedEvcsComponent;
import io.openems.edge.evcs.api.DeprecatedEvcs;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.WriteHandler;
import io.openems.edge.evse.chargepoint.hardybarth.common.HardyBarth;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.HardyBarth", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_EXECUTE_WRITE, //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
})
public class EvcsHardyBarthImpl extends AbstractManagedEvcsComponent
		implements OpenemsComponent, EventHandler, HardyBarth, Evcs, ManagedEvcs, DeprecatedEvcs, ElectricityMeter {

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	@Reference
	private HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition;

	@Reference
	private EvcsPower evcsPower;

	@Reference
	private OpenemsEdgeOem oem;

	private Config config;
	private EvcsHandler handler;

	public EvcsHardyBarthImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				HardyBarth.ChannelId.values(), //
				DeprecatedEvcs.ChannelId.values() //
		);
		DeprecatedEvcs.copyToDeprecatedEvcsChannels(this);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());
		// TODO stop here if not enabled
		this._setChargingType(AC);
		this._setFixedMinimumHardwarePower(config.minHwCurrent() / 1000 * 3 * 230);
		this._setFixedMaximumHardwarePower(config.maxHwCurrent() / 1000 * 3 * 230);
		this._setPowerPrecision(230);
		this._setPhases(THREE_PHASE);

		this.handler = new EvcsHandler(this, config.ip(), this.oem.getHardyBarthApiToken(), config.phaseRotation(),
				config.logVerbosity(), this::logInfo, this.httpBridgeFactory, this.httpBridgeCycleServiceDefinition,
				this::_setChargingstationCommunicationFailed);

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.handler.deactivate();
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

	@Override
	protected WriteHandler createWriteHandler() {
		return new HardyBarthWriteHandler(this);
	}

	@Override
	public String debugLog() {
		return this.handler.debugLog();
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	@Override
	public boolean applyChargePowerLimit(int power) {
		// Convert it to ampere and apply hard limits
		int phases = this.getPhasesAsInt();
		final var current = round(power / (float) phases / 230.F);

		return this.handler.setTarget(current);
	}

	@Override
	public boolean pauseChargeProcess() {
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
	public boolean isReadOnly() {
		return this.config.readOnly();
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return switch (this.config.logVerbosity()) {
		case NONE -> false;
		case DEBUG_LOG, READS, WRITES -> true;
		};
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
		this.handler.handleAfterProcessImageEvent(event);
	}

	@Override
	public int getWriteInterval() {
		return HEART_BEAT_TIME;
	}
}
