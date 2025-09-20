package io.openems.edge.controller.levl.balancing;

import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.controller.levl.common.Utils.HUNDRED_PERCENT;
import static io.openems.edge.controller.levl.common.Utils.MILLISECONDS_PER_SECOND;
import static io.openems.edge.controller.levl.common.Utils.SECONDS_PER_HOUR;
import static io.openems.edge.controller.levl.common.Utils.applyEfficiency;
import static io.openems.edge.controller.levl.common.Utils.calculateLevlBatteryPower;
import static io.openems.edge.controller.levl.common.Utils.calculatePucBatteryPower;
import static io.openems.edge.controller.levl.common.Utils.calculatePucSoc;
import static io.openems.edge.controller.levl.common.Utils.generateResponse;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;
import static java.lang.Math.round;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.time.Clock;
import java.time.Instant;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.jsonapi.Call;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.levl.common.LogVerbosity;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Levl.Ess.Balancing", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		TOPIC_CYCLE_AFTER_WRITE, //
})

public class ControllerLevlEssBalancingImpl extends AbstractOpenemsComponent
		implements Controller, OpenemsComponent, ControllerLevlEssBalancing, ComponentJsonApi, EventHandler {

	private final Logger log = LoggerFactory.getLogger(ControllerLevlEssBalancingImpl.class);
	private final Clock clock;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	protected ManagedSymmetricEss ess;

	@Reference
	protected ElectricityMeter meter;

	@Reference
	protected Cycle cycle;

	private LogVerbosity logVerbosity;
	private LevlControlRequest currentRequest;
	private LevlControlRequest nextRequest;

	protected ControllerLevlEssBalancingImpl(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerLevlEssBalancing.ChannelId.values());
		this.clock = clock;
	}

	public ControllerLevlEssBalancingImpl() {
		this(Clock.systemDefaultZone());
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "meter", config.meter_id())) {
			return;
		}
		this.logVerbosity = config.logVerbosity();

		this.initChannelValues();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		/*
		 * Check that we are On-Grid (and warn on undefined Grid-Mode)
		 */
		var gridMode = this.ess.getGridMode();
		if (gridMode.isUndefined()) {
			this.logWarn(this.log, "Grid-Mode is [UNDEFINED]");
		}
		switch (gridMode) {
		case ON_GRID:
		case UNDEFINED:
			break;
		case OFF_GRID:
			return;
		}

		/*
		 * Calculates required charge/discharge power
		 */
		var calculatedPower = this.calculateRequiredPower();

		/*
		 * set result
		 */
		this.ess.setActivePowerEqualsWithPid(calculatedPower);
		this.ess.setReactivePowerEquals(0);
	}

	private void initChannelValues() {
		this._setRealizedEnergyGrid(0L);
		this._setRealizedEnergyBattery(0L);
		this._setLastRequestRealizedEnergyGrid(0L);
		this._setLastRequestRealizedEnergyBattery(0L);
		this._setLastRequestTimestamp("1970-01-02T00:00:00Z");
		this._setLevlSoc(0L);
		this._setPucBatteryPower(0L);
		this._setEssEfficiency(100.0);
		this._setSocLowerBoundLevl(0.0);
		this._setSocUpperBoundLevl(0.0);
		this._setRemainingLevlEnergy(0L);
		this._setBuyFromGridLimit(0L);
		this._setSellToGridLimit(0L);
		this._setInfluenceSellToGrid(false);
	}

	/**
	 * Calculates the required charge/discharge power based on primary use case
	 * (puc; self consumption optimization) and levl.
	 *
	 * @return the required power for the next cycle [W]
	 * @throws OpenemsNamedException on error
	 */
	private int calculateRequiredPower() throws OpenemsNamedException {
		var cycleTimeS = this.cycle.getCycleTime() / MILLISECONDS_PER_SECOND;

		// load physical values
		var physicalSoc = this.ess.getSoc().getOrError();
		var gridPower = this.meter.getActivePower().getOrError();
		var essPower = this.ess.getActivePower().getOrError();
		var essCapacity = this.ess.getCapacity().getOrError();
		var minEssPower = this.ess.getPower().getMinPower(this.ess, ALL, ACTIVE);
		var maxEssPower = this.ess.getPower().getMaxPower(this.ess, ALL, ACTIVE);

		// levl request specific values
		var levlSocWs = this.getLevlSoc().getOrError();
		var remainingLevlEnergyWs = this.getRemainingLevlEnergy().getOrError();
		var efficiency = this.getEssEfficiency().getOrError();
		var socLowerBoundLevlPercent = this.getSocLowerBoundLevl().getOrError();
		var socUpperBoundLevlPercent = this.getSocUpperBoundLevl().getOrError();
		var buyFromGridLimit = this.getBuyFromGridLimit().getOrError();
		var sellToGridLimit = this.getSellToGridLimit().getOrError();
		var influenceSellToGrid = this.getInfluenceSellToGrid().getOrError();

		var essCapacityWs = essCapacity * SECONDS_PER_HOUR;
		var physicalSocWs = round((physicalSoc / HUNDRED_PERCENT) * essCapacityWs);

		// primary use case (puc) calculation
		var pucSocWs = calculatePucSoc(physicalSocWs, levlSocWs, essCapacityWs);
		var pucBatteryPower = calculatePucBatteryPower(this::logDebug, gridPower, essPower, pucSocWs, essCapacityWs,
				minEssPower, maxEssPower, efficiency, cycleTimeS);
		var pucGridPower = gridPower + essPower - pucBatteryPower;
		var nextPucSocWs = pucSocWs - round(applyEfficiency(pucBatteryPower, efficiency) * cycleTimeS);

		// levl calculation
		var levlBatteryPower = 0;
		if (remainingLevlEnergyWs != 0) {
			levlBatteryPower = calculateLevlBatteryPower(this::logDebug, remainingLevlEnergyWs, pucBatteryPower,
					minEssPower, maxEssPower, pucGridPower, buyFromGridLimit, sellToGridLimit, nextPucSocWs, levlSocWs,
					socLowerBoundLevlPercent, socUpperBoundLevlPercent, essCapacityWs, influenceSellToGrid, efficiency,
					cycleTimeS);
		}

		// overall calculation
		this._setPucBatteryPower(Long.valueOf(pucBatteryPower));
		var batteryPower = pucBatteryPower + levlBatteryPower;
		return batteryPower;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(LevlControlRequest.METHOD, call -> {
			return this.handleRequest(call);
		});
	}

	/**
	 * Handles an incoming levl request. Updates the levl soc based on the request
	 * levl soc.
	 * 
	 * @param call the JSON-RPC call
	 * @return a JSON-RPC response
	 * @throws OpenemsNamedException on error
	 */
	protected JsonrpcResponse handleRequest(Call<JsonrpcRequest, JsonrpcResponse> call) throws OpenemsNamedException {
		var now = Instant.now(this.clock);
		var request = LevlControlRequest.from(call.getRequest(), now);
		this.log.info("Received new levl request: {}", request);
		this.nextRequest = request;
		var realizedEnergyBatteryWs = this.getRealizedEnergyBattery().getOrError();
		var updatedLevlSoc = request.payload.levlSocWh() * SECONDS_PER_HOUR - realizedEnergyBatteryWs;
		this._setLevlSoc(updatedLevlSoc);
		this.log.info("Updated levl soc: {}", updatedLevlSoc);
		return generateResponse(call.getRequest().getId(), request.payload.levlRequestId());
	}

	private boolean isActive(LevlControlRequest request) {
		var now = Instant.now(this.clock);
		return !(request == null || now.isBefore(request.payload.start()) || now.isAfter(request.payload.deadline()));
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			if (this.isActive(this.nextRequest)) {
				if (this.currentRequest != null) {
					this.finishRequest();
				}
				this.startNextRequest();
			} else if (this.currentRequest != null && !this.isActive(this.currentRequest)) {
				this.finishRequest();
			}
		}
		case TOPIC_CYCLE_AFTER_WRITE -> {
			try {
				this.handleAfterWriteEvent();
			} catch (Exception e) {
				this.log.error("error executing after write event", e);
			}
		}
		}
	}

	/**
	 * Determines the levl soc based on the ess power for the next cycle. Updates
	 * channel and class variables for the next cycle.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	private void handleAfterWriteEvent() throws OpenemsNamedException {
		var remainingLevlEnergy = this.getRemainingLevlEnergy().getOrError();

		if (remainingLevlEnergy != 0L) {
			var pucBatteryPower = this.getPucBatteryPowerChannel().getNextValue().getOrError();
			var levlPower = 0L;
			var essNextPower = this.ess.getDebugSetActivePowerChannel().getNextValue();
			if (essNextPower.isDefined()) {
				var essPower = essNextPower.get();
				levlPower = essPower - pucBatteryPower;
			}

			var levlEnergyWs = round(levlPower * (this.cycle.getCycleTime() / MILLISECONDS_PER_SECOND));

			// remaining for the next cycle
			var newRemainingLevlEnergy = remainingLevlEnergy - levlEnergyWs;
			if (this.hasSignChanged(remainingLevlEnergy, newRemainingLevlEnergy)) {
				newRemainingLevlEnergy = 0;
			}
			this._setRemainingLevlEnergy(newRemainingLevlEnergy);

			// realized after the next cycle
			var realizedEnergyGridWs = this.getRealizedEnergyGrid().getOrError();
			this._setRealizedEnergyGrid(realizedEnergyGridWs + levlEnergyWs);

			// realized after the next cycle
			var efficiency = this.getEssEfficiency().getOrError();
			var realizedEnergyBatteryWs = this.getRealizedEnergyBattery().getOrError();
			this._setRealizedEnergyBattery(realizedEnergyBatteryWs + applyEfficiency(levlEnergyWs, efficiency));

			var levlSoc = this.getLevlSoc().getOrError();
			this._setLevlSoc(levlSoc - applyEfficiency(levlEnergyWs, efficiency));
		}
	}

	/**
	 * Sets last request realized energy of the finished request. Reset the realized
	 * energy class values for the next active request.
	 */
	private void finishRequest() {
		var realizedEnergyGridWs = this.getRealizedEnergyGridChannel().getNextValue().orElse(0L);
		var realizedEnergyBatteryWs = this.getRealizedEnergyBatteryChannel().getNextValue().orElse(0L);
		this.logDebug("finished levl request: " + this.currentRequest);
		this.logDebug("realized levl energy on grid: " + realizedEnergyGridWs);
		this.logDebug("realized levl energy in battery: " + realizedEnergyBatteryWs);

		this._setLastRequestRealizedEnergyGrid(realizedEnergyGridWs);
		this._setLastRequestRealizedEnergyBattery(realizedEnergyBatteryWs);
		this._setLastRequestTimestamp(this.currentRequest.payload.timestamp());
		this._setRemainingLevlEnergy(0L);
		this._setRealizedEnergyGrid(0L);
		this._setRealizedEnergyBattery(0L);
		this.currentRequest = null;
	}

	/**
	 * Sets the nextRequest as the current request. Updates request specific
	 * channels with request values.
	 */
	private void startNextRequest() {
		this.logDebug("starting levl request: " + this.nextRequest);
		this.currentRequest = this.nextRequest;
		this.nextRequest = null;
		this._setEssEfficiency(this.currentRequest.payload.efficiencyPercent());
		this._setSocLowerBoundLevl(this.currentRequest.payload.socLowerBoundPercent());
		this._setSocUpperBoundLevl(this.currentRequest.payload.socUpperBoundPercent());
		this._setRemainingLevlEnergy(this.currentRequest.payload.energyWs());
		this._setBuyFromGridLimit((long) this.currentRequest.payload.buyFromGridLimitW());
		this._setSellToGridLimit((long) this.currentRequest.payload.sellToGridLimitW());
		this._setInfluenceSellToGrid(this.currentRequest.payload.influenceSellToGrid());
	}

	private boolean hasSignChanged(long a, long b) {
		return a < 0 && b > 0 || a > 0 && b < 0;
	}

	private void logDebug(String message) {
		switch (this.logVerbosity) {
		case DEBUG_LOG, NONE -> FunctionUtils.doNothing();
		case TRACE -> this.log.info(message);
		}
	}
}
