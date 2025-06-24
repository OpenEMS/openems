package io.openems.edge.core.predictormanager;

import static io.openems.edge.common.channel.ChannelId.channelIdCamelToUpper;
import static io.openems.edge.predictor.api.prediction.Prediction.EMPTY_PREDICTION;
import static io.openems.edge.predictor.api.prediction.Prediction.sum;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedSet;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.ComparatorUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.prediction.Predictor;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = PredictorManager.SINGLETON_SERVICE_PID, //
		immediate = true, //
		property = { //
				"enabled=true" //
		})
public class PredictorManagerImpl extends AbstractOpenemsComponent implements PredictorManager, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(PredictorManagerImpl.class);

	@Reference
	private ConfigurationAdmin configurationAdmin;

	@Reference
	private ComponentManager componentManager;

	private List<String> configPredictorIds = emptyList();
	private final List<Predictor> rawPredictors = new ArrayList<>();
	private final AtomicReference<ImmutableSortedSet<Predictor>> rankedPredictors = new AtomicReference<>(
			ImmutableSortedSet.of());

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(enabled=true)" //
	)
	protected void bindPredictor(Predictor predictor) {
		synchronized (this.rawPredictors) {
			this.rawPredictors.add(predictor);
			this.updatePredictors();
		}
	}

	protected void unbindPredictor(Predictor predictor) {
		synchronized (this.rawPredictors) {
			this.rawPredictors.remove(predictor);
			this.updatePredictors();
		}
	}

	public PredictorManagerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				PredictorManager.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, PredictorManager.SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);

		this.setAndUpdatePredictors(config.predictor_ids());

		if (OpenemsComponent.validateSingleton(this.configurationAdmin, SINGLETON_SERVICE_PID,
				SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);

		this.setAndUpdatePredictors(config.predictor_ids());

		if (OpenemsComponent.validateSingleton(this.configurationAdmin, SINGLETON_SERVICE_PID,
				SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void setAndUpdatePredictors(String[] predictorIds) {
		synchronized (this.rawPredictors) {
			this.configPredictorIds = List.of(predictorIds);
			this.updatePredictors();
		}
	}

	private void updatePredictors() {
		var comparator = ComparatorUtils.comparatorIdList(this.configPredictorIds, Predictor::id);
		this.rankedPredictors.set(ImmutableSortedSet.copyOf(comparator, this.rawPredictors));
	}

	@Override
	public Prediction getPrediction(ChannelAddress channelAddress) {
		for (var predictor : this.rankedPredictors.get()) {
			for (var pattern : predictor.getChannelAddresses()) {
				// Skip if channel address does not match pattern (including wildcards)
				var matchValue = ChannelAddress.match(channelAddress, pattern);
				if (matchValue == -1) {
					continue;
				}

				// Attempt to get prediction from matching predictor
				var prediction = predictor.getPrediction(channelAddress);
				if (prediction == null || Prediction.EMPTY_PREDICTION.equals(prediction)) {
					break; // Try next predictor
				}
				return prediction;
			}
		}

		// No predictor matched; check if this is a Sum channel
		if (channelAddress.getComponentId().equals(Sum.SINGLETON_COMPONENT_ID)) {
			try {
				// Attempt to compute prediction by summing source channel predictions
				return this
						.getPredictionSum(Sum.ChannelId.valueOf(channelIdCamelToUpper(channelAddress.getChannelId())));
			} catch (IllegalArgumentException e) {
				this.logWarn(this.log, "Unable to find ChannelId for " + channelAddress);
				return EMPTY_PREDICTION;
			}
		}

		// No prediction available
		return EMPTY_PREDICTION;
	}

	/**
	 * Gets the {@link Prediction} for a Sum-Channel.
	 *
	 * @param channelId the {@link Sum.ChannelId}
	 * @return the {@link Prediction}
	 */
	private Prediction getPredictionSum(Sum.ChannelId channelId) {
		return switch (channelId) {
		case CONSUMPTION_ACTIVE_ENERGY, //
				CONSUMPTION_ACTIVE_POWER, CONSUMPTION_ACTIVE_POWER_L1, CONSUMPTION_ACTIVE_POWER_L2,
				CONSUMPTION_ACTIVE_POWER_L3, CONSUMPTION_MAX_ACTIVE_POWER, //

				ESS_ACTIVE_CHARGE_ENERGY, ESS_ACTIVE_DISCHARGE_ENERGY, ESS_ACTIVE_POWER, ESS_ACTIVE_POWER_L1,
				ESS_ACTIVE_POWER_L2, ESS_ACTIVE_POWER_L3, ESS_CAPACITY, ESS_DC_CHARGE_ENERGY, ESS_DC_DISCHARGE_ENERGY,
				ESS_DISCHARGE_POWER, ESS_MIN_DISCHARGE_POWER, ESS_MAX_DISCHARGE_POWER, ESS_MAX_APPARENT_POWER,
				ESS_REACTIVE_POWER, ESS_SOC, //

				GRID_ACTIVE_POWER, GRID_ACTIVE_POWER_L1, GRID_ACTIVE_POWER_L2, GRID_ACTIVE_POWER_L3, GRID_BUY_PRICE,
				GRID_BUY_ACTIVE_ENERGY, GRID_MAX_ACTIVE_POWER, GRID_MIN_ACTIVE_POWER, GRID_MODE,
				GRID_MODE_OFF_GRID_TIME, GRID_SELL_ACTIVE_ENERGY, //

				PRODUCTION_ACTIVE_ENERGY, PRODUCTION_AC_ACTIVE_ENERGY, PRODUCTION_AC_ACTIVE_POWER_L1,
				PRODUCTION_AC_ACTIVE_POWER_L2, PRODUCTION_AC_ACTIVE_POWER_L3, PRODUCTION_DC_ACTIVE_ENERGY,
				PRODUCTION_MAX_ACTIVE_POWER, //

				ESS_TO_CONSUMPTION_POWER, ESS_TO_CONSUMPTION_ENERGY, GRID_TO_CONSUMPTION_POWER,
				GRID_TO_CONSUMPTION_ENERGY, GRID_TO_ESS_POWER, GRID_TO_ESS_ENERGY, ESS_TO_GRID_ENERGY,
				PRODUCTION_TO_CONSUMPTION_POWER, PRODUCTION_TO_CONSUMPTION_ENERGY, PRODUCTION_TO_ESS_POWER,
				PRODUCTION_TO_ESS_ENERGY, PRODUCTION_TO_GRID_POWER, PRODUCTION_TO_GRID_ENERGY,

				HAS_IGNORED_COMPONENT_STATES ->
			EMPTY_PREDICTION;

		case UNMANAGED_CONSUMPTION_ACTIVE_POWER ->
			// Fallback for elder systems that only provide predictors for
			// ConsumptionActivePower by default
			this.getPrediction(new ChannelAddress("_sum", "ConsumptionActivePower"));

		case PRODUCTION_DC_ACTUAL_POWER -> {
			// Sum up "ActualPower" prediction of all EssDcChargers
			List<EssDcCharger> chargers = this.componentManager.getEnabledComponentsOfType(EssDcCharger.class);
			var predictions = new Prediction[chargers.size()];
			for (var i = 0; i < chargers.size(); i++) {
				var charger = chargers.get(i);
				predictions[i] = this
						.getPrediction(new ChannelAddress(charger.id(), EssDcCharger.ChannelId.ACTUAL_POWER.id()));
			}
			yield sum(predictions);
		}

		case PRODUCTION_AC_ACTIVE_POWER -> {
			// Sum up "ActivePower" prediction of all ElectricityMeter
			List<ElectricityMeter> meters = this.componentManager.getEnabledComponentsOfType(ElectricityMeter.class)
					.stream() //
					.filter(meter -> switch (meter.getMeterType()) {
					case GRID, CONSUMPTION_METERED, MANAGED_CONSUMPTION_METERED, CONSUMPTION_NOT_METERED //
						-> false;
					case PRODUCTION, PRODUCTION_AND_CONSUMPTION //
						-> true; // Get only Production meters
					}) //
					.toList();
			var predictions = new Prediction[meters.size()];
			for (var i = 0; i < meters.size(); i++) {
				var meter = meters.get(i);
				predictions[i] = this.getPrediction(//
						new ChannelAddress(meter.id(), ElectricityMeter.ChannelId.ACTIVE_POWER.id()));
			}
			yield sum(predictions);
		}

		case PRODUCTION_ACTIVE_POWER -> sum(//
				this.getPredictionSum(Sum.ChannelId.PRODUCTION_AC_ACTIVE_POWER), //
				this.getPredictionSum(Sum.ChannelId.PRODUCTION_DC_ACTUAL_POWER) //
			);
		};
	}
}
