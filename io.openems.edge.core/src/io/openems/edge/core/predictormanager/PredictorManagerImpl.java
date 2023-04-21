package io.openems.edge.core.predictormanager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.user.User;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;
import io.openems.edge.predictor.api.oneday.Predictor24Hours;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = PredictorManager.SINGLETON_SERVICE_PID, //
		immediate = true, //
		property = { //
				"enabled=true" //
		})
public class PredictorManagerImpl extends AbstractOpenemsComponent
		implements PredictorManager, OpenemsComponent, JsonApi {

	@Reference
	private ConfigurationAdmin cm;

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
	private void activate(ComponentContext context) {
		super.activate(context, PredictorManager.SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);

		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);

		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		switch (request.getMethod()) {
		case Get24HoursPredictionRequest.METHOD:
			return this.handleGet24HoursPredictionRequest(user, Get24HoursPredictionRequest.from(request));
		}
		return null;
	}

	private CompletableFuture<? extends JsonrpcResponseSuccess> handleGet24HoursPredictionRequest(User user,
			Get24HoursPredictionRequest request) throws OpenemsNamedException {
		final Map<ChannelAddress, Prediction24Hours> predictions = new HashMap<>();
		for (ChannelAddress channel : request.getChannels()) {
			predictions.put(channel, this.get24HoursPrediction(channel));
		}
		return CompletableFuture.completedFuture(new Get24HoursPredictionResponse(request.getId(), predictions));
	}

	@Override
	public Prediction24Hours get24HoursPrediction(ChannelAddress channelAddress) {
		var predictor = this.getPredictorBestMatch(channelAddress);
		if (predictor != null) {
			return predictor.get24HoursPrediction(channelAddress);
		}
		// No explicit predictor found
		if (channelAddress.getComponentId().equals(Sum.SINGLETON_COMPONENT_ID)) {
			// This is a Sum-Channel. Try to get predictions for each source channel.
			var channelId = Sum.ChannelId.valueOf(
					io.openems.edge.common.channel.ChannelId.channelIdCamelToUpper(channelAddress.getChannelId()));
			return this.getPredictionSum(channelId);
		} else {
			return Prediction24Hours.EMPTY;
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
		case HAS_IGNORED_COMPONENT_STATES:
			return Prediction24Hours.EMPTY;

		case CONSUMPTION_ACTIVE_POWER:
			// TODO
			return Prediction24Hours.EMPTY;

		case PRODUCTION_DC_ACTUAL_POWER: {
			// Sum up "ActualPower" prediction of all EssDcChargers
			List<EssDcCharger> chargers = this.componentManager.getEnabledComponentsOfType(EssDcCharger.class);
			var predictions = new Prediction24Hours[chargers.size()];
			for (var i = 0; i < chargers.size(); i++) {
				var charger = chargers.get(i);
				predictions[i] = this.get24HoursPrediction(
						new ChannelAddress(charger.id(), EssDcCharger.ChannelId.ACTUAL_POWER.id()));
			}
			return Prediction24Hours.sum(predictions);
		}
		case PRODUCTION_AC_ACTIVE_POWER: {
			// Sum up "ActivePower" prediction of all ElectricityMeter
			List<ElectricityMeter> meters = this.componentManager.getEnabledComponentsOfType(ElectricityMeter.class)
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
			var predictions = new Prediction24Hours[meters.size()];
			for (var i = 0; i < meters.size(); i++) {
				var meter = meters.get(i);
				predictions[i] = this.get24HoursPrediction(
						new ChannelAddress(meter.id(), ElectricityMeter.ChannelId.ACTIVE_POWER.id()));
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
	 * Gets the best matching {@link Predictor24Hours} for the given
	 * {@link ChannelAddress}.
	 *
	 * @param channelAddress the {@link ChannelAddress}
	 * @return the {@link Predictor24Hours} - or null if none matches
	 */
	private synchronized Predictor24Hours getPredictorBestMatch(ChannelAddress channelAddress) {
		var bestMatchValue = -1;
		Predictor24Hours bestPredictor = null;
		for (Predictor24Hours predictor : this.predictors) {
			for (ChannelAddress pattern : predictor.getChannelAddresses()) {
				var matchValue = ChannelAddress.match(channelAddress, pattern);
				if (matchValue == 0) {
					// Exact match
					return predictor;
				}
				if (matchValue > bestMatchValue) {
					bestMatchValue = matchValue;
					bestPredictor = predictor;
				}
			}
		}
		return bestPredictor;
	}

}
