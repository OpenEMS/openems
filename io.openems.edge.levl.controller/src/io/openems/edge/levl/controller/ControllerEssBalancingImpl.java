package io.openems.edge.levl.controller;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import org.osgi.service.cm.ConfigurationAdmin;
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
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.jsonapi.Call;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.levl.controller.common.Efficiency;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Levl.Symmetric.Balancing", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@EventTopics({ EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE, })

public class ControllerEssBalancingImpl extends AbstractOpenemsComponent
		implements Controller, OpenemsComponent, ControllerEssBalancing, ComponentJsonApi, EventHandler {

	private final Logger log = LoggerFactory.getLogger(ControllerEssBalancingImpl.class);

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	protected ManagedSymmetricEss ess;

	@Reference
	protected ElectricityMeter meter;

	@Reference
	protected Cycle cycle;

	protected static Clock clock = Clock.systemDefaultZone();

	protected LevlControlRequest currentRequest;
	protected LevlControlRequest nextRequest;

	private static final String METHOD = "sendLevlControlRequest";

	public ControllerEssBalancingImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssBalancing.ChannelId.values() //
		);
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
		this._setEfficiency(100.0);
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
	protected int calculateRequiredPower() throws OpenemsNamedException {
		var cycleTimeS = this.cycle.getCycleTime() / 1000.0;

		// load physical values
		var physicalSoc = this.ess.getSoc().getOrError();
		var gridPower = this.meter.getActivePower().getOrError();
		var essPower = this.ess.getActivePower().getOrError();
		var essCapacity = this.ess.getCapacity().getOrError();
		var minEssPower = this.ess.getPower().getMinPower(this.ess, Phase.ALL, Pwr.ACTIVE);
		var maxEssPower = this.ess.getPower().getMaxPower(this.ess, Phase.ALL, Pwr.ACTIVE);

		// levl request specific values
		var levlSocWs = this.getLevlSoc().getOrError();
		var remainingLevlEnergyWs = this.getRemainingLevlEnergy().getOrError();
		var efficiency = this.getEfficiency().getOrError();
		var socLowerBoundLevlPercent = this.getSocLowerBoundLevl().getOrError();
		var socUpperBoundLevlPercent = this.getSocUpperBoundLevl().getOrError();
		var buyFromGridLimit = this.getBuyFromGridLimit().getOrError();
		var sellToGridLimit = this.getSellToGridLimit().getOrError();
		var influenceSellToGrid = this.getInfluenceSellToGrid().getOrError();

		var essCapacityWs = essCapacity * 3600L;
		var physicalSocWs = Math.round((physicalSoc / 100.0) * essCapacityWs);

		// primary use case (puc) calculation
		var pucSocWs = this.calculatePucSoc(physicalSocWs, levlSocWs, essCapacityWs);
		var pucBatteryPower = this.calculatePucBatteryPower(gridPower, essPower, pucSocWs, essCapacityWs, minEssPower,
				maxEssPower, efficiency, cycleTimeS);
		var pucGridPower = gridPower + essPower - pucBatteryPower;
		var nextPucSocWs = pucSocWs - Math.round(Efficiency.apply(pucBatteryPower, efficiency) * cycleTimeS);

		// levl calculation
		var levlBatteryPower = 0;
		if (remainingLevlEnergyWs != 0) {
			levlBatteryPower = this.calculateLevlBatteryPower(remainingLevlEnergyWs, pucBatteryPower, minEssPower,
					maxEssPower, pucGridPower, buyFromGridLimit, sellToGridLimit, nextPucSocWs, levlSocWs,
					socLowerBoundLevlPercent, socUpperBoundLevlPercent, essCapacityWs, influenceSellToGrid, efficiency,
					cycleTimeS);
		}

		// overall calculation
		this._setPucBatteryPower(Long.valueOf(pucBatteryPower));
		var batteryPower = pucBatteryPower + levlBatteryPower;
		return batteryPower;
	}

	/**
	 * Calculates the soc of the primary use case based on physical soc and tracked
	 * levl soc.
	 * 
	 * @param physicalSocWs the physical soc [Ws]
	 * @param levlSocWs     the levl soc [Ws]
	 * @param essCapacityWs the ess capacity [Ws]
	 * 
	 * @return the soc of the primary use case
	 */
	protected long calculatePucSoc(long physicalSocWs, long levlSocWs, long essCapacityWs) {
		var pucSoc = physicalSocWs - levlSocWs;

		// handle case of pucSoc out of bounds (e.g. due to rounding)
		if (pucSoc < 0) {
			return 0;
		} else if (pucSoc > essCapacityWs) {
			return essCapacityWs;
		}
		return pucSoc;
	}

	/**
	 * Calculates the power of the primary use case, taking into account the ess
	 * power limits and the soc limits.
	 * <p>
	 * positive = discharge
	 * negative = charge
	 * </p>
	 * 
	 * @param gridPower     the active power of the meter [W]
	 * @param essPower      the active power of the ess [W]
	 * @param pucSocWs      the soc of the puc [Ws]
	 * @param essCapacityWs the ess capacity [Ws]
	 * @param minEssPower   the minimum possible power of the ess [W]
	 * @param maxEssPower   the maximum possible power of the ess [W]
	 * @param efficiency    the efficiency of the system [%]
	 * @param cycleTimeS    the configured openems cycle time [seconds]
	 * 
	 * @return the puc battery power for the next cycle [W]
	 */
	protected int calculatePucBatteryPower(int gridPower, int essPower, long pucSocWs, long essCapacityWs,
			int minEssPower, int maxEssPower, double efficiency, double cycleTimeS) {
		this.log.debug("### calculatePucBatteryPower ###");
		this.log.debug(String.format(
				"Parameters: gridPower=%d, essPower=%d, pucSocWs=%d, essCapacityWs=%d, minEssPower=%d, "
						+ "maxEssPower=%d, efficiency=%.2f, cycleTimeS=%.2f",
				gridPower, essPower, pucSocWs, essCapacityWs, minEssPower, maxEssPower, efficiency, cycleTimeS));
		// calculate pucPower without any limits
		var pucBatteryPower = gridPower + essPower;
		this.log.debug("pucBatteryPower without limits: " + pucBatteryPower);

		// apply ess power limits
		pucBatteryPower = Math.max(Math.min(pucBatteryPower, maxEssPower), minEssPower);
		this.log.debug("pucBatteryPower with ess power limits: " + pucBatteryPower);

		// apply soc bounds
		pucBatteryPower = this.applyPucSocBounds(pucBatteryPower, pucSocWs, essCapacityWs, efficiency, cycleTimeS);
		this.log.debug("pucBatteryPower with ess power and soc limits: " + pucBatteryPower);
		return pucBatteryPower;
	}

	/**
	 * Checks and corrects the puc battery power if it would exceed the upper or
	 * lower limits of the soc.
	 * 
	 * @param pucPower      the calculated pucPower [W]
	 * @param pucSocWs      the soc of the puc [Ws]
	 * @param essCapacityWs the ess capacity [Ws]
	 * @param efficiency    the efficiency of the system [%]
	 * @param cycleTimeS    the configured openems cycle time [seconds]
	 * @return the restricted pucPower [W]
	 */
	protected int applyPucSocBounds(int pucPower, long pucSocWs, long essCapacityWs, double efficiency,
			double cycleTimeS) {
		var dischargeEnergyLowerBoundWs = pucSocWs - essCapacityWs;
		var dischargeEnergyUpperBoundWs = pucSocWs;

		var powerLowerBound = Efficiency.unapply(Math.round(dischargeEnergyLowerBoundWs / cycleTimeS), efficiency);
		var powerUpperBound = Efficiency.unapply(Math.round(dischargeEnergyUpperBoundWs / cycleTimeS), efficiency);

		if (powerLowerBound > 0) {
			powerLowerBound = 0;
		}
		if (powerUpperBound < 0) {
			powerUpperBound = 0;
		}

		return (int) Math.max(Math.min(pucPower, powerUpperBound), powerLowerBound);
	}

	/**
	 * Calculates the battery power for the levl use case considering various
	 * constraints.
	 * <p>
	 * positive = discharge
	 * negative = charge
	 * </p>
	 * 
	 * @param remainingLevlEnergyWs    the remaining energy that has to be realized
	 *                                 for levl [Ws]
	 * @param pucBatteryPower          the puc battery power [W]
	 * @param minEssPower              the minimum possible power of the ess [W]
	 * @param maxEssPower              the maximum possible power of the ess [W]
	 * @param pucGridPower             the active power of the puc on the meter [W]
	 * @param buyFromGridLimit         maximum power that may be bought from the
	 *                                 grid [W]
	 * @param sellToGridLimit          maximum power that may be sold to the grid
	 *                                 [W]
	 * @param nextPucSocWs             the calculated puc soc for the next cycle
	 *                                 [Ws]
	 * @param levlSocWs                the current levl soc [Ws]
	 * @param socLowerBoundLevlPercent the lower levl soc limit [%]
	 * @param socUpperBoundLevlPercent the upper levl soc limit [%]
	 * @param essCapacityWs            the ess capacity [Ws]
	 * @param influenceSellToGrid      whether it's allowed to influence sell to
	 *                                 grid
	 * @param efficiency               the efficiency of the system [%]
	 * @param cycleTimeS               the configured openems cycle time [seconds]
	 * @return the levl battery power [W]
	 */
	protected int calculateLevlBatteryPower(long remainingLevlEnergyWs, int pucBatteryPower, int minEssPower,
			int maxEssPower, int pucGridPower, long buyFromGridLimit, long sellToGridLimit, long nextPucSocWs,
			long levlSocWs, double socLowerBoundLevlPercent, double socUpperBoundLevlPercent, long essCapacityWs,
			boolean influenceSellToGrid, double efficiency, double cycleTimeS) {

		this.log.debug("### calculateLevlBatteryPowerW ###");
		this.log.debug(String.format("Parameters: remainingLevlEnergyWs=%d, pucBatteryPower=%d, minEssPower=%d, maxEssPower=%d, "
					+ "pucGridPower=%d, buyFromGridLimit=%d, sellToGridLimit=%d, nextPucSocWs=%d, levlSocWs=%d, "
					+ "socLowerBoundLevlPercent=%.2f, socUpperBoundLevlPercent=%.2f, essCapacityWs=%d, "
					+ "influenceSellToGrid=%b, efficiency=%.2f, cycleTimeS=%.2f", remainingLevlEnergyWs,
					pucBatteryPower, minEssPower, maxEssPower, pucGridPower, buyFromGridLimit, sellToGridLimit,
					nextPucSocWs, levlSocWs, socLowerBoundLevlPercent, socUpperBoundLevlPercent, essCapacityWs,
					influenceSellToGrid, efficiency, cycleTimeS));

		var levlPower = Math.round(remainingLevlEnergyWs / (double) cycleTimeS);
		this.log.debug("Initial levlPower: " + levlPower);

		levlPower = this.applyBatteryPowerLimitsToLevlPower(levlPower, pucBatteryPower, minEssPower, maxEssPower);
		this.log.debug("LevlPower after applyBatteryPowerLimits: " + levlPower);

		levlPower = this.applySocBoundariesToLevlPower(levlPower, nextPucSocWs, levlSocWs, socLowerBoundLevlPercent,
				socUpperBoundLevlPercent, essCapacityWs, efficiency, cycleTimeS);
		this.log.debug("LevlPower after applySocBoundaries: " + levlPower);

		levlPower = this.applyGridPowerLimitsToLevlPower(levlPower, pucGridPower, buyFromGridLimit, sellToGridLimit);
		this.log.debug("LevlPower after applyGridPowerLimits: " + levlPower);

		levlPower = this.applyInfluenceSellToGridConstraint(levlPower, pucGridPower, influenceSellToGrid);
		this.log.debug("LevlPower after applyInfluenceSellToGridConstraint: " + levlPower);

		return (int) levlPower;
	}

	/**
	 * Applies battery power limits to the levl power.
	 * 
	 * @param levlPower       the levl battery power [W]
	 * @param pucBatteryPower the puc battery power [W]
	 * @param minEssPower     the minimum possible power of the ess [W]
	 * @param maxEssPower     the maximum possible power of the ess [W]
	 * @return the restricted levl battery power [W]
	 */
	protected long applyBatteryPowerLimitsToLevlPower(long levlPower, int pucBatteryPower, int minEssPower,
			int maxEssPower) {
		var levlPowerLowerBound = Long.valueOf(minEssPower) - pucBatteryPower;
		var levlPowerUpperBound = Long.valueOf(maxEssPower) - pucBatteryPower;
		return Math.max(Math.min(levlPower, levlPowerUpperBound), levlPowerLowerBound);
	}

	/**
	 * Applies upper and lower soc bounderies to the levl power.
	 * 
	 * @param levlPower                the levl battery power [W]
	 * @param nextPucSocWs             the calculated puc soc for the next cycle [Ws]
	 * @param levlSocWs                the current levl soc [Ws]
	 * @param socLowerBoundLevlPercent the lower levl soc limit [%]
	 * @param socUpperBoundLevlPercent the upper levl soc limit [%]
	 * @param essCapacityWs            the ess capacity [Ws]
	 * @param efficiency               the efficiency of the system [%]
	 * @param cycleTimeS               the configured openems cycle time [seconds]
	 * @return the restricted levl battery power [W]
	 */
	protected long applySocBoundariesToLevlPower(long levlPower, long nextPucSocWs, long levlSocWs,
			double socLowerBoundLevlPercent, double socUpperBoundLevlPercent, long essCapacityWs, double efficiency,
			double cycleTimeS) {
		var levlSocLowerBoundWs = Math.round(socLowerBoundLevlPercent / 100.0 * essCapacityWs) - nextPucSocWs;
		var levlSocUpperBoundWs = Math.round(socUpperBoundLevlPercent / 100.0 * essCapacityWs) - nextPucSocWs;

		if (levlSocLowerBoundWs > 0) {
			levlSocLowerBoundWs = 0;
		}
		if (levlSocUpperBoundWs < 0) {
			levlSocUpperBoundWs = 0;
		}

		var levlDischargeEnergyLowerBoundWs = -(levlSocUpperBoundWs - levlSocWs);
		var levlDischargeEnergyUpperBoundWs = -(levlSocLowerBoundWs - levlSocWs);

		var levlPowerLowerBound = Efficiency.unapply(Math.round(levlDischargeEnergyLowerBoundWs / cycleTimeS),
				efficiency);
		var levlPowerUpperBound = Efficiency.unapply(Math.round(levlDischargeEnergyUpperBoundWs / cycleTimeS),
				efficiency);

		return Math.max(Math.min(levlPower, levlPowerUpperBound), levlPowerLowerBound);
	}

	/**
	 * Applies grid power limits to the levl power.
	 * 
	 * @param levlPower        the levl battery power [W]
	 * @param pucGridPower     the active power of the puc on the meter [W]
	 * @param buyFromGridLimit maximum power that may be bought from the grid [W]
	 * @param sellToGridLimit  maximum power that may be sold to the grid [W]
	 * @return the restricted levl battery power [W]
	 */
	protected long applyGridPowerLimitsToLevlPower(long levlPower, int pucGridPower, long buyFromGridLimit,
			long sellToGridLimit) {
		var levlPowerLowerBound = -(buyFromGridLimit - pucGridPower);
		var levlPowerUpperBound = -(sellToGridLimit - pucGridPower);
		return Math.max(Math.min(levlPower, levlPowerUpperBound), levlPowerLowerBound);
	}

	/**
	 * Applies influence sell to grid constraint to the levl power.
	 * 
	 * @param levlPower           the levl battery power [W]
	 * @param pucGridPower        the active power of the puc on the meter [W]
	 * @param influenceSellToGrid whether it's allowed to influence sell to grid
	 * @return the restricted levl battery power [W]
	 */
	protected long applyInfluenceSellToGridConstraint(long levlPower, int pucGridPower, boolean influenceSellToGrid) {
		if (!influenceSellToGrid) {
			if (pucGridPower < 0) {
				// if primary use case sells to grid, levl isn't allowed to do anything
				levlPower = 0;
			} else {
				// if primary use case buys from grid, levl can sell maximum this amount to grid
				levlPower = Math.min(levlPower, pucGridPower);
			}
		}
		return levlPower;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(METHOD, call -> {
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
		var request = LevlControlRequest.from(call.getRequest());
		this.log.info("Received new levl request: {}", request);
		this.nextRequest = request;
		var realizedEnergyBatteryWs = this.getRealizedEnergyBattery().getOrError();
		var updatedLevlSoc = request.levlSocWh * 3600L - realizedEnergyBatteryWs;
		this._setLevlSoc(updatedLevlSoc);
		this.log.info("Updated levl soc: {}", updatedLevlSoc);
		return JsonrpcResponseSuccess.from(this.generateResponse(call.getRequest().getId(), request.levlRequestId));
	}

	private JsonObject generateResponse(UUID requestId, String levlRequestId) {
		JsonObject response = new JsonObject();
		var result = new JsonObject();
		result.addProperty("levlRequestId", levlRequestId);
		response.addProperty("id", requestId.toString());
		response.add("result", result);
		return response;
	}

	private static boolean isActive(LevlControlRequest request) {
		LocalDateTime now = LocalDateTime.now(clock);
		return !(request == null || now.isBefore(request.start) || now.isAfter(request.deadline));
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			if (isActive(this.nextRequest)) {
				if (this.currentRequest != null) {
					this.finishRequest();
				}
				this.startNextRequest();
			} else if (this.currentRequest != null && !isActive(this.currentRequest)) {
				this.finishRequest();
			}
		}
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE -> {
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

			var levlEnergyWs = Math.round(levlPower * (this.cycle.getCycleTime() / 1000.0));

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
			var efficiency = this.getEfficiency().getOrError();
			var realizedEnergyBatteryWs = this.getRealizedEnergyBattery().getOrError();
			this._setRealizedEnergyBattery(realizedEnergyBatteryWs + Efficiency.apply(levlEnergyWs, efficiency));

			var levlSoc = this.getLevlSoc().getOrError();
			this._setLevlSoc(levlSoc - Efficiency.apply(levlEnergyWs, efficiency));
		}
	}

	/**
	 * Sets last request realized energy of the finished request. Reset the realized
	 * energy class values for the next active request.
	 */
	private void finishRequest() {
		var realizedEnergyGridWs = this.getRealizedEnergyGridChannel().getNextValue().orElse(0L);
		var realizedEnergyBatteryWs = this.getRealizedEnergyBatteryChannel().getNextValue().orElse(0L);
		this.log.info("finished levl request: {}", this.currentRequest);
		this.log.info("realized levl energy on grid: {}", realizedEnergyGridWs);
		this.log.info("realized levl energy in battery: {}", realizedEnergyBatteryWs);

		this._setLastRequestRealizedEnergyGrid(realizedEnergyGridWs);
		this._setLastRequestRealizedEnergyBattery(realizedEnergyBatteryWs);
		this._setLastRequestTimestamp(this.currentRequest.timestamp);
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
		this.log.info("starting levl request: {}", this.nextRequest);
		this.currentRequest = this.nextRequest;
		this.nextRequest = null;
		this._setEfficiency(this.currentRequest.efficiencyPercent);
		this._setSocLowerBoundLevl(this.currentRequest.socLowerBoundPercent);
		this._setSocUpperBoundLevl(this.currentRequest.socUpperBoundPercent);
		this._setRemainingLevlEnergy(this.currentRequest.energyWs);
		this._setBuyFromGridLimit((long) this.currentRequest.buyFromGridLimitW);
		this._setSellToGridLimit((long) this.currentRequest.sellToGridLimitW);
		this._setInfluenceSellToGrid(this.currentRequest.influenceSellToGrid);
	}

	private boolean hasSignChanged(long a, long b) {
		return a < 0 && b > 0 || a > 0 && b < 0;
	}
}
