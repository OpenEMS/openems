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

import io.openems.common.exceptions.InvalidValueException;
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
@EventTopics({ EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS, EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE, })


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
	protected long realizedEnergyGridWs;
	protected long realizedEnergyBatteryWs;

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
		var calculatedPower = calculateRequiredPower();

		/*
		 * set result
		 */
		this.ess.setActivePowerEqualsWithPid(calculatedPower);
		this.ess.setReactivePowerEquals(0);
	}
	
	
	private void initChannelValues() {
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
	 * Calculates required charge/discharge power.
	 *
	 * @param essPower           the charge/discharge power of the
	 *                           {@link ManagedSymmetricEss}
	 * @param gridPower          the buy-from-grid/sell-to grid power
	 * @param targetGridSetpoint the configured targetGridSetpoint
	 * @return the required power
	 * @throws InvalidValueException
	 */
	protected int calculateRequiredPower() throws OpenemsNamedException {
		double cycleTimeS = this.cycle.getCycleTime() / 1000.0;

		// load physical values
		int physicalSoc = this.ess.getSoc().getOrError();
		int gridPower = this.meter.getActivePower().getOrError();
		int essPower = this.ess.getActivePower().getOrError();
		int essCapacity = this.ess.getCapacity().getOrError();
		
		long levlSocWs = this.getLevlSoc().getOrError();
		long remainingLevlEnergyWs = this.getRemainingLevlEnergy().getOrError();
		double efficiency = this.getEfficiency().getOrError();
		double socLowerBoundLevlPercent = this.getSocLowerBoundLevl().getOrError();
		double socUpperBoundLevlPercent = this.getSocUpperBoundLevl().getOrError();
		long buyFromGridLimit = this.getBuyFromGridLimit().getOrError();
		long sellToGridLimit = this.getSellToGridLimit().getOrError();
		boolean influenceSellToGrid = this.getInfluenceSellToGrid().getOrError();

		int minEssPower = this.ess.getPower().getMinPower(this.ess, Phase.ALL, Pwr.ACTIVE);
		int maxEssPower = this.ess.getPower().getMaxPower(this.ess, Phase.ALL, Pwr.ACTIVE);

		long essCapacityWs = essCapacity * 3600;
		long physicalSocWs = Math.round((physicalSoc / 100.0) * essCapacityWs);

		// primary use case (puc) calculation
		long pucSocWs = calculatePucSoc(levlSocWs, physicalSocWs);
		int pucBatteryPower = calculatePucBatteryPower(cycleTimeS, gridPower, essPower,
				essCapacityWs, pucSocWs, minEssPower, maxEssPower, efficiency);
		int pucGridPower = gridPower + essPower - pucBatteryPower;
		long nextPucSocWs = pucSocWs + Math.round(pucBatteryPower * cycleTimeS);

		// levl calculation
		int levlPowerW = 0;
		if (remainingLevlEnergyWs != 0) {
			levlPowerW = this.calculateLevlPowerW(remainingLevlEnergyWs, pucBatteryPower, minEssPower, maxEssPower, pucGridPower, buyFromGridLimit, sellToGridLimit,
					nextPucSocWs, levlSocWs, socLowerBoundLevlPercent, socUpperBoundLevlPercent, essCapacityWs, influenceSellToGrid, efficiency, cycleTimeS);
		}

		// overall calculation
		this._setPucBatteryPower(Long.valueOf(pucBatteryPower));
		long batteryPowerW = pucBatteryPower + levlPowerW;
		return (int) batteryPowerW;
	}
	
	protected long calculatePucSoc(long levlSocWs, long physicalSocWs) {
		var pucSoc = physicalSocWs - levlSocWs;
		
		if (pucSoc < 0) {
			return 0;
		}
		return pucSoc;
	}

	/**
	 * Calculates the power of the primary use case (puc)
	 * 
	 * @param cycleTimeS
	 * @param gridPower
	 * @param essPower
	 * @param essCapacityWs
	 * @param pucSocWs
	 * @param minEssPower
	 * @param maxEssPower
	 * @return
	 */
	protected int calculatePucBatteryPower(double cycleTimeS, int gridPower, int essPower,
			long essCapacityWs, long pucSocWs, int minEssPower, int maxEssPower, double efficiency) {
		// calculate pucPower without any limits
		int pucBatteryPower = gridPower + essPower;

		// apply ess power limits
		pucBatteryPower = Math.max(Math.min(pucBatteryPower, maxEssPower), minEssPower);

		// apply soc bounds
		pucBatteryPower = applyPucSocBounds(cycleTimeS, essCapacityWs, pucSocWs, pucBatteryPower, efficiency);
		return pucBatteryPower;
	}

	/**
	 * Checks and corrects the pucPower if it would exceed the upper or lower limits
	 * of the SoC.
	 * 
	 * @param cycleTimeSec
	 * @param essCapacityWs
	 * @param pucSocWs
	 * @param pucPower
	 * @return the restricted pucPower
	 */
	protected int applyPucSocBounds(double cycleTimeS, long essCapacityWs, long pucSocWs, int pucPower, double efficiency) {
		long dischargeEnergyLowerBoundWs = pucSocWs - essCapacityWs;
		long dischargeEnergyUpperBoundWs = pucSocWs;
				
		long powerLowerBound = Efficiency.unapply(Math.round(dischargeEnergyLowerBoundWs / cycleTimeS),
				efficiency);
		long powerUpperBound = Efficiency.unapply(Math.round(dischargeEnergyUpperBoundWs / cycleTimeS),
				efficiency);
				
		if (powerLowerBound > 0) {
			powerLowerBound = 0;
		}
		if (powerUpperBound < 0) {
			powerUpperBound = 0;
		}

		return (int) Math.max(Math.min(pucPower, powerUpperBound), powerLowerBound);
	}

	private int calculateLevlPowerW(long remainingLevlEnergyWs, int pucBatteryPower, int minEssPower, int maxEssPower, int pucGridPower, long buyFromGridLimit, long sellToGridLimit,
			long nextPucSocWs, long levlSocWs, double socLowerBoundLevlPercent, double socUpperBoundLevlPercent, long essCapacityWs, boolean influenceSellToGrid, double efficiency, double cycleTimeS) {
		long levlPower = Math.round(remainingLevlEnergyWs / (double) cycleTimeS);

		levlPower = this.applyBatteryPowerLimitsToLevlPower(levlPower, pucBatteryPower, minEssPower, maxEssPower);
		levlPower = this.applySocBoundariesToLevlPower(levlPower, nextPucSocWs, levlSocWs, socLowerBoundLevlPercent, socUpperBoundLevlPercent, essCapacityWs, efficiency, cycleTimeS);
		levlPower = this.applyGridPowerLimitsToLevlPower(levlPower, pucGridPower, buyFromGridLimit, sellToGridLimit);
		levlPower = this.applyInfluenceSellToGridConstraint(levlPower, pucGridPower, influenceSellToGrid);

		return (int) levlPower;
	}

	protected long applyBatteryPowerLimitsToLevlPower(long levlPower, int pucBatteryPower, int minEssPower,
			int maxEssPower) {
		long levlPowerLowerBound = Long.valueOf(minEssPower) - pucBatteryPower;
		long levlPowerUpperBound = Long.valueOf(maxEssPower) - pucBatteryPower;
		return Math.max(Math.min(levlPower, levlPowerUpperBound), levlPowerLowerBound);
	}

	protected long applySocBoundariesToLevlPower(long levlPower, long nextPucSocWs, long levlSocWs, double socLowerBoundLevlPercent, double socUpperBoundLevlPercent, long essCapacityWs,
			double efficiency, double cycleTimeS) {
		long levlSocLowerBoundWs = Math.round(socLowerBoundLevlPercent / 100.0 * essCapacityWs) - nextPucSocWs;
		long levlSocUpperBoundWs = Math.round(socUpperBoundLevlPercent / 100.0 * essCapacityWs) - nextPucSocWs;
		
		if (levlSocLowerBoundWs > 0) {
			levlSocLowerBoundWs = 0;
		}
		if (levlSocUpperBoundWs < 0) {
			levlSocUpperBoundWs = 0;
		}

		long levlDischargeEnergyLowerBoundWs = -(levlSocUpperBoundWs - levlSocWs);
		long levlDischargeEnergyUpperBoundWs = -(levlSocLowerBoundWs - levlSocWs);

		long levlPowerLowerBound = Efficiency.unapply(Math.round(levlDischargeEnergyLowerBoundWs / cycleTimeS),
				efficiency);
		long levlPowerUpperBound = Efficiency.unapply(Math.round(levlDischargeEnergyUpperBoundWs / cycleTimeS),
				efficiency);

		return Math.max(Math.min(levlPower, levlPowerUpperBound), levlPowerLowerBound);
	}

	protected long applyGridPowerLimitsToLevlPower(long levlPower, int pucGridPower, long buyFromGridLimit, long sellToGridLimit) {
		long levlPowerLowerBound = -(buyFromGridLimit - pucGridPower);
		long levlPowerUpperBound = -(sellToGridLimit - pucGridPower);
		return Math.max(Math.min(levlPower, levlPowerUpperBound), levlPowerLowerBound);
	}

	public long applyInfluenceSellToGridConstraint(long levlPower, int pucGridPower, boolean influenceSellToGrid) {
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
			return handleRequest(call);
		});
	}

	/**
	 * @param call
	 * @return
	 * @throws OpenemsNamedException
	 */
	protected JsonrpcResponse handleRequest(Call<JsonrpcRequest, JsonrpcResponse> call) throws OpenemsNamedException {
		var request = LevlControlRequest.from(call.getRequest());
		this.log.info("Received new levl request: {}", request);
		this.nextRequest = request;
		var nextLevlSoc = request.levlSocWh * 3600 - this.realizedEnergyBatteryWs;
		this._setLevlSoc(nextLevlSoc);
		this.log.info("Updated levl soc: {}", nextLevlSoc);
		return JsonrpcResponseSuccess
				.from(this.generateResponse(call.getRequest().getId(), request.levlRequestId));
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
					startNextRequest();
				} else if (currentRequest != null && !isActive(this.currentRequest)) {
					this.finishRequest();
				}
			}
			case EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE -> {
				try {
					handleAfterWriteEvent();
				} catch (Exception e) {
					this.log.error("error executing after write event", e);
				}
			}
		}
	}
	
	private void handleAfterWriteEvent() throws OpenemsNamedException  {
		if (this.currentRequest != null) {
			var pucBatteryPower = this.getPucBatteryPowerChannel().getNextValue().getOrError();
			var remainingLevlEnergy = this.getRemainingLevlEnergy().getOrError();
			var efficiency = this.getEfficiency().getOrError();
			var levlSoc = this.getLevlSoc().getOrError();
			
			long levlPower = 0;
			var essNextPower = this.ess.getDebugSetActivePowerChannel().getNextValue();
			if (essNextPower.isDefined()) {
				var essPower = essNextPower.get();
				levlPower = essPower - pucBatteryPower;
			}
			long levlEnergyWs = Math.round(levlPower * this.cycle.getCycleTime() / 1000.0);
			// remaining for the NEXT calculation cycle
			this._setRemainingLevlEnergy(remainingLevlEnergy - levlEnergyWs);
			// realized AFTER the next cycle (next second)
			this.realizedEnergyGridWs += levlEnergyWs;
			this.log.info("this cycle realized levl energy on grid: {}", levlEnergyWs);
			this.realizedEnergyBatteryWs += Efficiency.apply(levlEnergyWs, efficiency);
			this._setLevlSoc(levlSoc - Efficiency.apply(levlEnergyWs, efficiency));
		}
	}

	private void finishRequest() {
		this.log.info("finished levl request: {}", this.currentRequest);
		this._setLastRequestRealizedEnergyGrid(this.realizedEnergyGridWs);
		this._setLastRequestTimestamp(this.currentRequest.timestamp);
		this.log.info("realized levl energy on grid: {}", this.realizedEnergyGridWs);
		this.log.info("realized levl energy in battery: {}", this.realizedEnergyBatteryWs);
		this.realizedEnergyGridWs = 0;
		this.realizedEnergyBatteryWs = 0;
		this.currentRequest = null;
	}
	
	private void startNextRequest() {
		this.log.info("starting levl request: {}", this.currentRequest);
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

}
