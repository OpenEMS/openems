package io.openems.edge.core.predictormanager;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.session.User;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;
import io.openems.edge.predictor.api.oneday.Predictor24Hours;

@Component(//
		name = "Core.PredictorManager", //
		immediate = true, //
		property = { //
				"id=" + OpenemsConstants.PREDICTOR_MANAGER_ID, //
				"enabled=true" //
		})
public class PredictorManagerImpl extends AbstractOpenemsComponent implements OpenemsComponent, JsonApi {

	@Reference
	protected ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(enabled=true)")
	private volatile List<Predictor24Hours> predictors = new CopyOnWriteArrayList<>();

	public PredictorManagerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				PredictorManager.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context) {
		super.activate(context, OpenemsConstants.PREDICTOR_MANAGER_ID, "Core.PredictorManager", true);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		switch (request.getMethod()) {
		case Get24HourPredictionRequest.METHOD:
			return this.handleGet24HourPredictionRequest(user, Get24HourPredictionRequest.from(request));
		}
		return null;
	}

	private CompletableFuture<? extends JsonrpcResponseSuccess> handleGet24HourPredictionRequest(User user,
			Get24HourPredictionRequest request) throws OpenemsNamedException {
		Prediction24Hours prediction = this.get24HoursPrediction(ChannelAddress.fromString(request.getChannel()));
		return CompletableFuture
				.completedFuture(new Get24HourPredictionResponse(request.getId(), prediction.getValues()));
	}

	/**
	 * Gets the {@link Prediction24Hours} for the given {@link ChannelAddress}.
	 * 
	 * @param channelAddress the {@link ChannelAddress}
	 * @return the {@link Prediction24Hours}
	 */
	public Prediction24Hours get24HoursPrediction(ChannelAddress channelAddress) {
		if (channelAddress.getComponentId().equals(OpenemsConstants.SUM_ID)) {
			// Prediction for Sum-Channel. Need to get predictions for each individual input
			// value.
			Sum.ChannelId channelId = Sum.ChannelId.valueOf(channelAddress.getChannelId().toUpperCase());
			return this.getPredictionSum(channelId);
		} else {
			// Non-Sum-Channel
			return this.getPredictionNotSum(channelAddress);
		}
	}

	/**
	 * Gets the {@link Prediction24Hours} for a Sum-Channel.
	 * 
	 * @param channelId the {@link Sum.ChannelId}
	 * @return the {@link Prediction24Hours}
	 */
	private Prediction24Hours getPredictionSum(Sum.ChannelId channelId) {
		switch (channelId) {
		case CONSUMPTION_ACTIVE_ENERGY:
		case CONSUMPTION_ACTIVE_POWER_L1:
		case CONSUMPTION_ACTIVE_POWER_L2:
		case CONSUMPTION_ACTIVE_POWER_L3:
		case CONSUMPTION_MAX_ACTIVE_POWER:
		case ESS_ACTIVE_CHARGE_ENERGY:
		case ESS_ACTIVE_DISCHARGE_ENERGY:
		case ESS_ACTIVE_POWER:
		case ESS_ACTIVE_POWER_L1:
		case ESS_ACTIVE_POWER_L2:
		case ESS_ACTIVE_POWER_L3:
		case ESS_CAPACITY:
		case ESS_DC_CHARGE_ENERGY:
		case ESS_DC_DISCHARGE_ENERGY:
		case ESS_DISCHARGE_POWER:
		case ESS_MAX_APPARENT_POWER:
		case ESS_REACTIVE_POWER:
		case ESS_SOC:
		case GRID_ACTIVE_POWER:
		case GRID_ACTIVE_POWER_L1:
		case GRID_ACTIVE_POWER_L2:
		case GRID_ACTIVE_POWER_L3:
		case GRID_BUY_ACTIVE_ENERGY:
		case GRID_MAX_ACTIVE_POWER:
		case GRID_MIN_ACTIVE_POWER:
		case GRID_MODE:
		case GRID_SELL_ACTIVE_ENERGY:
		case PRODUCTION_ACTIVE_ENERGY:
		case PRODUCTION_AC_ACTIVE_ENERGY:
		case PRODUCTION_AC_ACTIVE_POWER_L1:
		case PRODUCTION_AC_ACTIVE_POWER_L2:
		case PRODUCTION_AC_ACTIVE_POWER_L3:
		case PRODUCTION_DC_ACTIVE_ENERGY:
		case PRODUCTION_MAX_ACTIVE_POWER:
		case PRODUCTION_MAX_AC_ACTIVE_POWER:
		case PRODUCTION_MAX_DC_ACTUAL_POWER:
			return Prediction24Hours.EMPTY;

		case CONSUMPTION_ACTIVE_POWER:
			// TODO
			return Prediction24Hours.EMPTY;

		case PRODUCTION_DC_ACTUAL_POWER: {
			// Sum up "ActualPower" prediction of all EssDcChargers
			List<EssDcCharger> chargers = this.componentManager.getEnabledComponentsOfType(EssDcCharger.class);
			Prediction24Hours[] predictions = new Prediction24Hours[chargers.size()];
			for (int i = 0; i < chargers.size(); i++) {
				EssDcCharger charger = chargers.get(i);
				predictions[i] = this.getPredictionNotSum(
						new ChannelAddress(charger.id(), EssDcCharger.ChannelId.ACTUAL_POWER.id()));
			}
			return Prediction24Hours.sum(predictions);
		}
		case PRODUCTION_AC_ACTIVE_POWER: {
			// Sum up "ActivePower" prediction of all SymmetricMeters
			List<SymmetricMeter> meters = this.componentManager.getEnabledComponentsOfType(SymmetricMeter.class)
					.stream() //
					.filter(meter -> {
						switch (meter.getMeterType()) {
						case GRID:
						case CONSUMPTION_METERED:
						case CONSUMPTION_NOT_METERED:
							return false;
						case PRODUCTION:
						case PRODUCTION_AND_CONSUMPTION:
							// Get only Production meters
							return true;
						}
						// should never come here
						return false;
					}).collect(Collectors.toList());
			Prediction24Hours[] predictions = new Prediction24Hours[meters.size()];
			for (int i = 0; i < meters.size(); i++) {
				SymmetricMeter meter = meters.get(i);
				predictions[i] = this.getPredictionNotSum(
						new ChannelAddress(meter.id(), SymmetricMeter.ChannelId.ACTIVE_POWER.id()));
			}
			return Prediction24Hours.sum(predictions);
		}

		case PRODUCTION_ACTIVE_POWER:
			return Prediction24Hours.sum(//
					this.getPredictionSum(Sum.ChannelId.PRODUCTION_AC_ACTIVE_POWER), //
					this.getPredictionSum(Sum.ChannelId.PRODUCTION_DC_ACTUAL_POWER) //
			);
		}

		// should never come here
		return Prediction24Hours.EMPTY;
	}

	/**
	 * Gets the {@link Prediction24Hours} by the best matching
	 * {@link Predictor24Hours} for the given {@link ChannelAddress}.
	 * 
	 * @param channelAddress the {@link ChannelAddress}; never Sum-Channel
	 * @return the {@link Prediction24Hours} - all values null if no Predictor
	 *         matches the Channel-Address
	 */
	private Prediction24Hours getPredictionNotSum(ChannelAddress channelAddress) {
		Predictor24Hours predictor = this.getPredictorBestMatch(channelAddress);
		if (predictor == null) {
			return Prediction24Hours.EMPTY;
		} else {
			return predictor.get24HoursPrediction(channelAddress);
		}
	}

	/**
	 * Gets the best matching {@link Predictor24Hours} for the given
	 * {@link ChannelAddress}.
	 * 
	 * @param channelAddress the {@link ChannelAddress}
	 * @return the {@link Predictor24Hours} - or null if none matches
	 */
	private synchronized Predictor24Hours getPredictorBestMatch(ChannelAddress channelAddress) {
		int bestMatchValue = -1;
		Predictor24Hours bestPredictor = null;
		for (Predictor24Hours predictor : this.predictors) {
			for (ChannelAddress pattern : predictor.getChannelAddresses()) {
				int matchValue = ChannelAddress.match(channelAddress, pattern);
				if (matchValue == 0) {
					// Exact match
					return predictor;
				} else if (matchValue > bestMatchValue) {
					bestMatchValue = matchValue;
					bestPredictor = predictor;
				}
			}
		}
		return bestPredictor;
	}

}
