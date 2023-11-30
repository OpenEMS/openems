package io.openems.edge.predictor.persistencemodel;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.predictor.api.oneday.AbstractPredictor24Hours;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;
import io.openems.edge.predictor.api.oneday.Predictor24Hours;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Predictor.PersistenceModel", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class PredictorPersistenceModelImpl extends AbstractPredictor24Hours
		implements Predictor24Hours, OpenemsComponent {

	/** Use that many quarters to calculate regression. */
	private static final int REGRESSION_QUERY_QUARTERS = 2 /* hours */ * 4 /* quarters */;
	/** Prediction that many quarters by regression. */
	private static final int REGRESSION_APPLY_QUARTERS = 2 /* quarters */;
	/** Use that many quarters for smoothing short-term prediction. */
	private static final int SMOOTH_QUERY_QUARTERS = 2 /* hours */ * 4 /* quarters */;
	/** Apply smooth factor on that many quarters. */
	private static final int SMOOTH_APPLY_QUARTERS = 3 /* hours */ * 4 /* quarters */;

	private static final int EXTRA_QUERY_QUARTERS = Math.max(SMOOTH_QUERY_QUARTERS, REGRESSION_QUERY_QUARTERS);

	private final Logger log = LoggerFactory.getLogger(PredictorPersistenceModelImpl.class);

	@Reference
	private Timedata timedata;

	@Reference
	private ComponentManager componentManager;

	public PredictorPersistenceModelImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				PredictorPersistenceModel.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.channelAddresses());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected Prediction24Hours createNewPrediction(ChannelAddress channelAddress) {
		var now = ZonedDateTime.now(this.componentManager.getClock());
		var fromDate = now.minus(24 * 60 + EXTRA_QUERY_QUARTERS * 15, ChronoUnit.MINUTES);

		// Query database
		final SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryResult;
		try {
			queryResult = this.timedata.queryHistoricData(null, fromDate, now, Sets.newHashSet(channelAddress),
					new Resolution(15, ChronoUnit.MINUTES));
		} catch (OpenemsNamedException e) {
			this.logError(this.log, e.getMessage());
			e.printStackTrace();
			return Prediction24Hours.EMPTY;
		}

		// Extract data
		var data = queryResult.values().stream() //
				.map(SortedMap::values) //
				// extract JsonElement values as flat stream
				.flatMap(Collection::stream) //
				// convert JsonElement to Integer
				.map(v -> {
					if (v.isJsonNull()) {
						return (Integer) null;
					}
					return v.getAsInt();
				}).toList();

		// Apply regression for ultra-short-term prediction
		final var regression = this.getRegressionPrediction(data);

		final var factor = this.getSmoothFactor(data);

		final double reduce = -1. / SMOOTH_APPLY_QUARTERS;
		final var i = new AtomicDouble(1.);

		// Prepare and return result
		var result = Streams.concat(//
				// Ultra-short term prediction (by regression)
				regression.stream(),
				// Apply factor
				data.stream() //
						.skip(EXTRA_QUERY_QUARTERS + REGRESSION_APPLY_QUARTERS) //
						.limit(SMOOTH_APPLY_QUARTERS) //
						.map(v -> v == null ? null : (int) Math.round(v * reduceFactor(factor, i.getAndAdd(reduce)))), //
				// Keep remaining
				data.stream() //
						.skip(EXTRA_QUERY_QUARTERS + REGRESSION_APPLY_QUARTERS + SMOOTH_APPLY_QUARTERS) //
		).toArray(Integer[]::new);

		return Prediction24Hours.of(channelAddress, result);
	}

	/**
	 * Generate a ultra-short-term prediction purely based on regression.
	 * 
	 * @param data the timedata
	 * @return the list of predictions
	 */
	private List<Integer> getRegressionPrediction(List<Integer> data) {
		final var regression = new SimpleRegression();
		final var counter = new AtomicInteger(0);
		data.stream() //
				.skip(data.size() - REGRESSION_QUERY_QUARTERS) //
				.forEach(v -> {
					var i = counter.incrementAndGet();
					if (v == null) {
						return;
					}
					regression.addData(i, v);
				});
		var start = counter.incrementAndGet();
		return IntStream.range(start, start + REGRESSION_APPLY_QUARTERS) //
				.mapToObj(i -> {
					var p = regression.predict(i);
					if (Double.isNaN(i)) {
						return null; // TODO use proper value
					}
					return (int) Math.round(p);
				}) //
				.toList();
	}

	/**
	 * Generate a smooth factor forshort-term prediction smoothing.
	 * 
	 * @param data the timedata
	 * @return the smooth factor
	 */
	private double getSmoothFactor(List<Integer> data) {
		var predicted = data.stream() //
				.limit(SMOOTH_QUERY_QUARTERS) //
				.filter(Objects::nonNull) //
				.mapToInt(Integer::intValue) //
				.average();
		var actual = Lists.reverse(data).stream() //
				.limit(SMOOTH_QUERY_QUARTERS) //
				.filter(Objects::nonNull) //
				.mapToInt(Integer::intValue) //
				.average();
		if (actual.isPresent() && predicted.isPresent() && predicted.getAsDouble() != 0) {
			var f = actual.getAsDouble() / predicted.getAsDouble();
			if (f <= 0) {
				return 1.; // Disallow zero or negative
			} else if (f < 0 && f > -0.1) { // Avoid small negative number
				return -0.1;
			} else if (f > 0 && f < 0.1) { // Avoid small positive number
				return 0.1;
			} else {
				return f;
			}
		} else {
			return 1.; // Avoid divide by zero
		}
	}

	/**
	 * Steadily reduces the original factor to 1.
	 * 
	 * @param originalFactor the original factor
	 * @param reduceFactor   multiply delta to 1 with this factor
	 * @return reduced original factor
	 */
	private static double reduceFactor(double originalFactor, double reduceFactor) {
		if (originalFactor > 1.) {
			return 1. + ((originalFactor - 1) * reduceFactor);
		} else {
			return 1. - ((1. - originalFactor) * reduceFactor);
		}
	}

	@Override
	protected ClockProvider getClockProvider() {
		return this.componentManager;
	}

}
